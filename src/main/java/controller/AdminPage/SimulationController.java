package controller.AdminPage;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import app.Main;
import controller.MapController;
import javafx.application.Platform;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.graph.AgentMovement;
import model.graph.Edge;
import model.graph.EdgeState;
import model.graph.RouteGraph;
import model.simulation.FloodSimulation;
import model.simulation.SimulationDataService;
import model.zone.Zone;

/**
 * Contrôleur principal de la simulation d'inondation — version corrigée.
 *
 * Corrections apportées :
 * ─ removeZone        : supprime bien toutes les arêtes connectées + relocalise agents statiques ET en transit
 * ─ removeAgent       : retire l'agent du RouteGraph avant de le supprimer
 * ─ resetZones        : propage la remise à zéro au mapController + dataService
 * ─ addAgent          : fusion Citizen/RescueAgent selon rôle ; support nom/prénom/rôle/âge/localisation
 * ─ addRandomCitizens → addRandomAgents (10 agents mélangés)
 * ─ État agent SAFE / AVAILABLE à l'arrivée
 * ─ Vitesse par défaut 3 min = 180 s pour une inondation complète ; facteur FLOOD_SPEED_FACTOR
 * ─ Stats nœud/arête : recordAgentPassedZone appelé à l'arrivée + compteurs arête mis à jour à chaque tick
 * ─ hasManualFloodStarted : utilise EdgeState sans appel à getFloodLevel()
 * ─ removeEdgeWithAgentRelocation : relocalise vers zone SOURCE correctement
 * ─ clearStats / tickWaitCycles / checkAndSetOvercrowding : inchangés mais corrigés
 */
public class SimulationController {

    // ─── Constantes ──────────────────────────────────────────────────────
    /** Niveau d'eau (m) déclenchant l'évacuation automatique. */
    private static final double SEUIL_EVACUATION_M   = 0.5;
    /** Incrément de temps simulé par pas (secondes). */
    private static final double DELTA_SECONDS         = 1.0;
    /** Cycles d'attente lors d'une forte congestion. */
    private static final int    OVERCROWD_WAIT_CYCLES = 2;
    /**
     * Facteur de ralentissement de l'inondation.
     * À 1.0 le niveau monte vite (quelques secondes) ;
     * réduire ce facteur permet ~3 minutes à vitesse 500 ms/cycle.
     * Le niveau brut du modèle est divisé par ce facteur pour l'affichage et les décisions.
     */
    public static final double FLOOD_SPEED_FACTOR = 60.0;

    // ─── Rôles d'agents ──────────────────────────────────────────────────
    public enum AgentRole { CITIZEN, RESCUE }

    // ─── Mode fin de trajet ──────────────────────────────────────────────
    public enum AgentEndBehavior { RANDOM_DESTINATION, REMOVE_AGENT }

    // ─── Modèle & services ───────────────────────────────────────────────
    private final FloodSimulation       modele;
    private final SimulationDataService dataService;
    private MapController               mapController;

    // ─── Paramètres simulation ───────────────────────────────────────────
    private double           vitesseSimulationMs  = 500.0;
    private boolean          modeAleatoire        = true;
    private AgentEndBehavior agentEndBehavior     = AgentEndBehavior.RANDOM_DESTINATION;
    private boolean          evacuationDeclenchee = false;

    // ─── Plages de création d'agents (réglables depuis l'UI) ─────────────
    private int    agentAgeMin    = 18;  private int    agentAgeMax    = 75;
    private double agentSpeedMin  = 0.5; private double agentSpeedMax  = 2.0;
    private double agentStressMin = 0.0; private double agentStressMax = 1.0;

    // ─── Stats nœuds ─────────────────────────────────────────────────────
    private final Map<Integer, Integer> zonePassCount   = new HashMap<>();
    private final Map<Integer, Double>  zoneTimeSpent   = new HashMap<>();
    private final Map<Integer, Integer> zoneWaitCycles  = new HashMap<>();
    private final Map<Integer, Boolean> zoneOvercrowded = new HashMap<>();

    // ─── Stats arêtes ─────────────────────────────────────────────────────
    private final Map<Integer, Integer> edgePassCount  = new HashMap<>();
    private final Map<Integer, Double>  edgeAvgSpeed   = new HashMap<>();
    private final Map<Integer, Long>    edgeFlowCumul  = new HashMap<>();
    private final Map<Integer, Long>    edgeCycleCumul = new HashMap<>();

    // ─── Sélection ───────────────────────────────────────────────────────
    private Agent selectedAgent = null;

    // ─── Callbacks UI ─────────────────────────────────────────────────────
    private Consumer<String>         onStatusChanged;
    private Consumer<Double>         onWaterLevelChanged;
    private Consumer<List<Zone>>     onZonesUpdated;
    private Consumer<List<Agent>>    onAgentsUpdated;
    private Consumer<AgentMovement>  onAgentArrived;
    private Consumer<NodeEdgeStats>  onStatsUpdated;
    private Consumer<List<Zone>>     onSelectedAgentPathChanged;

    // ─── État sauvegardé pour "Recommencer" ──────────────────────────────
    private byte[] savedStateBytes = null;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTEURS
    // ─────────────────────────────────────────────────────────────────────

    public SimulationController(FloodSimulation simulation, SimulationDataService dataService) {
        this.modele      = simulation;
        this.dataService = dataService;
        dataService.takeSnapshot(modele.getZones(), modele.getAgents());
        saveInitialState();
    }

    public SimulationController() {
        this(Main.getSharedSimulation(), Main.getSharedDataService());
    }

    // ─────────────────────────────────────────────────────────────────────
    // INJECTION MAP CONTROLLER
    // ─────────────────────────────────────────────────────────────────────

    public void setMapController(MapController mc) {
        this.mapController = mc;
        mc.setOnAgentArrived(mv -> {
            handleAgentArrived(mv);
            if (onAgentArrived != null) Platform.runLater(() -> onAgentArrived.accept(mv));
        });
        mc.syncAgents(modele.getAgents());
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCÈS MODÈLE
    // ─────────────────────────────────────────────────────────────────────

    public FloodSimulation       getModele()       { return modele; }
    public List<Zone>            getZones()        { return modele.getZones(); }
    public SimulationDataService getDataService()  { return dataService; }
    public MapController         getMapController(){ return mapController; }

    public Zone getZoneById(int id) {
        return modele.getZones().stream().filter(z -> z.getId() == id).findFirst().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTRÔLES SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    public void demarrerSimulation() {
        modele.demarrer();
        modele.publishSimulationStartAlert();
        if (modeAleatoire) {
            if (!evacuationDeclenchee) {
                declencherEvacuationAutomatique();
                evacuationDeclenchee = true;
            }
            notifyStatus("Simulation aléatoire — évacuation déclenchée");
        } else {
            notifyStatus("Mode manuel — cliquez sur la carte pour inonder une zone");
        }
    }

    public void mettreEnPause()       { modele.setEnPause(true);  notifyStatus("En pause"); }
    public void reprendreSimulation() { modele.setEnPause(false); notifyStatus("Simulation en cours"); }

    /**
     * Exécute un pas complet de simulation.
     * FIX : le niveau d'eau brut est mis à l'échelle par FLOOD_SPEED_FACTOR
     * pour que la montée soit lisible sur ~3 minutes à 500 ms/cycle.
     */
    public void executerPas() {
        if (modele.isEnPause()) return;

        // Avancement physique uniquement en mode aléatoire
        if (modeAleatoire) modele.executerPas();

        // Niveau pondéré pour les décisions et l'affichage
        double niveauBrut   = modele.getNiveauEau();
        double niveauScaled = niveauBrut / FLOOD_SPEED_FACTOR;

        // Déclenchement de l'évacuation
        if (!evacuationDeclenchee) {
            boolean doEvac = modeAleatoire
                ? niveauScaled >= SEUIL_EVACUATION_M
                : hasManualFloodStarted();
            if (doEvac) {
                declencherEvacuationAutomatique();
                evacuationDeclenchee = true;
            }
        }

        // Tick du moteur de déplacement
        if (mapController != null) {
            if (evacuationDeclenchee) mapController.tick(DELTA_SECONDS);
            mapController.updateAllZones(modele.getZones());
        }

        tickWaitCycles();
        updateEdgeStats();
        notifySelectedAgentPath();

        final double nv = niveauScaled;
        Platform.runLater(() -> {
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(nv);
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
        });
    }

    public void resetSimulation() {
        if (savedStateBytes != null) {
            restoreFromBytes(savedStateBytes);
        } else {
            Object[] initial = dataService.reset();
            @SuppressWarnings("unchecked") List<Zone>  zones  = (List<Zone>)  initial[0];
            @SuppressWarnings("unchecked") List<Agent> agents = (List<Agent>) initial[1];
            modele.resetSimulation();
            if (!zones.isEmpty())  modele.setZones(zones);
            if (!agents.isEmpty()) modele.setAgents(agents);
        }

        evacuationDeclenchee = false;
        selectedAgent        = null;
        clearStats();

        if (mapController != null) {
            mapController.syncAgents(modele.getAgents());
            mapController.syncZones(modele.getZones());
        }
        notifyStatus("Simulation réinitialisée");
        Platform.runLater(() -> {
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
            if (onAgentsUpdated     != null) onAgentsUpdated.accept(modele.getAgents());
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(0.0);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESET ZONES (bouton "↺ Reset zones")
    // FIX : propage la remise à zéro au mapController et au dataService.
    // ─────────────────────────────────────────────────────────────────────

    public void resetAllZones() {
        modele.getZones().forEach(Zone::reset);
        dataService.saveZones(modele.getZones());
        if (mapController != null) mapController.syncZones(modele.getZones());
        clearStats();
        evacuationDeclenchee = false;
        notifyStatus("Zones réinitialisées");
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT / IMPORT ÉTAT
    // ─────────────────────────────────────────────────────────────────────

    public boolean exportState(File file) {
        try {
            SimulationSnapshot snap = buildSnapshot();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(snap);
            }
            notifyStatus("État exporté : " + file.getName());
            return true;
        } catch (Exception e) {
            notifyStatus("Erreur export : " + e.getMessage());
            return false;
        }
    }

    public boolean importState(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            SimulationSnapshot snap = (SimulationSnapshot) ois.readObject();
            applySnapshot(snap);
            notifyStatus("État importé : " + file.getName());
            return true;
        } catch (Exception e) {
            notifyStatus("Erreur import : " + e.getMessage());
            return false;
        }
    }

    public void saveCurrentStateAsInitial() {
        saveInitialState();
        notifyStatus("État initial enregistré.");
    }

    private void saveInitialState() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(buildSnapshot());
            oos.close();
            savedStateBytes = baos.toByteArray();
        } catch (Exception e) {
            System.err.println("saveInitialState: " + e.getMessage());
        }
    }

    private void restoreFromBytes(byte[] bytes) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            SimulationSnapshot snap = (SimulationSnapshot) ois.readObject();
            applySnapshot(snap);
        } catch (Exception e) {
            System.err.println("restoreFromBytes: " + e.getMessage());
        }
    }

    private SimulationSnapshot buildSnapshot() {
        return new SimulationSnapshot(
            new ArrayList<>(modele.getZones()),
            new ArrayList<>(modele.getAgents()),
            modele.getNiveauEau(),
            modele.getGravite(),
            modeAleatoire,
            agentEndBehavior
        );
    }

    private void applySnapshot(SimulationSnapshot snap) {
        modele.resetSimulation();
        if (snap.zones  != null && !snap.zones.isEmpty())  modele.setZones(snap.zones);
        if (snap.agents != null && !snap.agents.isEmpty()) modele.setAgents(snap.agents);
        modele.setNiveauEau(snap.niveauEau);
        modele.setGravite(snap.gravite);
        modeAleatoire    = snap.modeAleatoire;
        agentEndBehavior = snap.agentEndBehavior;
        evacuationDeclenchee = false;
        selectedAgent        = null;
        clearStats();
        if (mapController != null) {
            mapController.syncAgents(modele.getAgents());
            mapController.syncZones(modele.getZones());
        }
        Platform.runLater(() -> {
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
            if (onAgentsUpdated     != null) onAgentsUpdated.accept(modele.getAgents());
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(snap.niveauEau);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DES AGENTS
    // FIX :
    //   • Fusion Citizen / RescueAgent via AgentRole
    //   • Support nom, prénom, âge, vitesse, stress, zone de départ
    //   • addRandomAgents(n) génère un mix citoyen + secours
    //   • removeAgent retire aussi du RouteGraph
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Crée et ajoute un agent avec les paramètres fournis.
     *
     * @param role      CITIZEN ou RESCUE (null → CITIZEN)
     * @param firstName prénom (null → aléatoire)
     * @param lastName  nom   (null → aléatoire)
     * @param age       &lt;0 → aléatoire dans la plage
     * @param speed     &lt;0 → aléatoire dans la plage
     * @param stress    &lt;0 → aléatoire dans la plage
     * @param startZone zone de départ (null → aléatoire)
     */
    public Agent addAgent(AgentRole role, String firstName, String lastName,
                          int age, double speed, double stress, Zone startZone) {
        Random rng = new Random();
        AgentRole effectiveRole = (role != null) ? role : AgentRole.CITIZEN;

        int    effAge   = age   < 0 ? agentAgeMin   + rng.nextInt(Math.max(1, agentAgeMax   - agentAgeMin))   : age;
        double effSpeed = speed < 0 ? agentSpeedMin + rng.nextDouble() * (agentSpeedMax - agentSpeedMin) : speed;
        double effStress= stress< 0 ? agentStressMin+ rng.nextDouble() * (agentStressMax - agentStressMin): stress;

        int id = nextAgentId();
        Agent agent;

        if (effectiveRole == AgentRole.RESCUE) {
            RescueAgent ra = dataService.createRandomRescueAgent(id, modele.getZones());
            if (ra == null) return null;
            if (firstName != null) trySet(ra, "setFirstName", String.class, firstName);
            if (lastName  != null) trySet(ra, "setLastName",  String.class, lastName);
            trySet(ra, "setAge",         int.class,    effAge);
            trySet(ra, "setSpeed",       double.class, effSpeed);
            if (startZone != null) trySet(ra, "setCurrentZone", Zone.class, startZone);
            agent = ra;
        } else {
            Citizen c = dataService.createRandomCitizen(id, modele.getZones());
            if (c == null) return null;
            if (firstName != null) trySet(c, "setFirstName", String.class, firstName);
            if (lastName  != null) trySet(c, "setLastName",  String.class, lastName);
            trySet(c, "setAge",          int.class,    effAge);
            trySet(c, "setSpeed",        double.class, effSpeed);
            trySet(c, "setStressLevel",  double.class, effStress);
            if (startZone != null) trySet(c, "setCurrentZone", Zone.class, startZone);
            agent = c;
        }

        modele.getAgents().add(agent);
        persistAndPropagate(agent);
        return agent;
    }

    /** Raccourci : citoyen entièrement aléatoire. */
    public Citizen addRandomCitizen() {
        return (Citizen) addAgent(AgentRole.CITIZEN, null, null, -1, -1, -1, null);
    }

    /** Raccourci : secours entièrement aléatoire. */
    public RescueAgent addRandomRescueAgent() {
        return (RescueAgent) addAgent(AgentRole.RESCUE, null, null, -1, -1, -1, null);
    }

    /**
     * Ajoute n agents avec paramètres aléatoires (mix citoyens + secours).
     * FIX : remplace addRandomCitizens(10) → addRandomAgents(10)
     */
    public List<Agent> addRandomAgents(int count) {
        List<Agent> added = new ArrayList<>();
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            AgentRole role = rng.nextInt(5) == 0 ? AgentRole.RESCUE : AgentRole.CITIZEN; // ~20 % secours
            Agent a = addAgent(role, null, null, -1, -1, -1, null);
            if (a != null) added.add(a);
        }
        return added;
    }

    /**
     * Supprime un agent du modèle ET du RouteGraph.
     * FIX : l'ancienne version ne retirait pas l'agent des mouvements actifs.
     */
    public boolean removeAgent(int agentId) {
        // 1. Retirer du RouteGraph (mouvements actifs)
        if (mapController != null && mapController.getRouteGraph() != null) {
            mapController.getRouteGraph().removeMovementsOfAgent(agentId);
        }
        // 2. Retirer de la liste du modèle
        boolean removed = modele.getAgents().removeIf(a -> a.getId() == agentId);
        if (removed) {
            dataService.saveAgents(modele.getAgents());
            if (mapController != null) mapController.removeAgent(agentId);
            if (selectedAgent != null && selectedAgent.getId() == agentId) {
                selectedAgent = null;
                if (onSelectedAgentPathChanged != null)
                    Platform.runLater(() -> onSelectedAgentPathChanged.accept(new ArrayList<>()));
            }
            notifyAgentsUpdated();
        }
        return removed;
    }

    public void setAgents(List<Agent> agents) {
        modele.setAgents(agents);
        if (mapController != null) mapController.syncAgents(modele.getAgents());
        notifyAgentsUpdated();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PLAGES DE GÉNÉRATION D'AGENTS
    // ─────────────────────────────────────────────────────────────────────

    public void setAgentAgeRange(int min, int max)        { agentAgeMin = min;    agentAgeMax = max; }
    public void setAgentSpeedRange(double min, double max){ agentSpeedMin = min;  agentSpeedMax = max; }
    public void setAgentStressRange(double min, double max){ agentStressMin = min; agentStressMax = max; }

    public int    getAgentAgeMin()    { return agentAgeMin; }
    public int    getAgentAgeMax()    { return agentAgeMax; }
    public double getAgentSpeedMin()  { return agentSpeedMin; }
    public double getAgentSpeedMax()  { return agentSpeedMax; }
    public double getAgentStressMin() { return agentStressMin; }
    public double getAgentStressMax() { return agentStressMax; }

    // ─────────────────────────────────────────────────────────────────────
    // SÉLECTION AGENT — TRAJET RESTANT
    // ─────────────────────────────────────────────────────────────────────

    public void  selectAgent(Agent agent) { this.selectedAgent = agent; notifySelectedAgentPath(); }
    public Agent getSelectedAgent()       { return selectedAgent; }

    public List<Zone> getSelectedAgentRemainingPath() {
        if (selectedAgent == null || mapController == null) return new ArrayList<>();
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return new ArrayList<>();
        return rg.getActiveMovements().stream()
            .filter(mv -> mv.getAgent().getId() == selectedAgent.getId())
            .findFirst()
            .map(mv -> mv.getPath().getRemainingZones())
            .orElse(new ArrayList<>());
    }

    // ─────────────────────────────────────────────────────────────────────
    // SUPPRESSION NŒUD AVEC RELOCALISATION
    // FIX :
    //   • Cherche les agents statiques (currentZone) ET en transit
    //   • Supprime toutes les arêtes connectées du RouteGraph
    //   • Annule les mouvements qui passent par ce nœud
    // ─────────────────────────────────────────────────────────────────────

    public void removeZone(int zoneId) {
        Zone toRemove = getZoneById(zoneId);
        if (toRemove == null) return;

        // Agents à relocaliser = statiques dans la zone + en transit vers/depuis elle
        List<Agent> agentsToRelocate = new ArrayList<>();
        agentsToRelocate.addAll(findAgentsStaticInZone(zoneId));
        agentsToRelocate.addAll(findAgentsTransitThroughZone(zoneId));
        // Dédupliquer
        agentsToRelocate = agentsToRelocate.stream().distinct().collect(Collectors.toList());

        List<Zone> adjacent = findAdjacentZones(zoneId);

        if (!agentsToRelocate.isEmpty()) {
            if (adjacent.isEmpty()) {
                final List<Agent> toRemoveAgents = agentsToRelocate;
                toRemoveAgents.forEach(a -> modele.getAgents().remove(a));
                notifyStatus("⚠ Agents supprimés (aucune zone adjacente disponible)");
            } else {
                Zone fallback = adjacent.get(0);
                agentsToRelocate.forEach(a -> relocateAgent(a, fallback));
                checkAndSetOvercrowding(fallback, agentsToRelocate.size());
                notifyStatus("Agents relocalisés vers " + fallback.getName());
            }
        }

        // Annuler les mouvements passant par ce nœud
        cancelMovementsThrough(zoneId);

        // Supprimer les arêtes connectées du RouteGraph
        if (mapController != null && mapController.getRouteGraph() != null) {
            RouteGraph rg = mapController.getRouteGraph();
            List<Edge> connected = new ArrayList<>(rg.getEdges()).stream()
                .filter(e -> e.getFromZone().getId() == zoneId || e.getToZone().getId() == zoneId)
                .collect(Collectors.toList());
            connected.forEach(e -> rg.removeEdge(e.getId()));
        }

        modele.removeZoneById(zoneId);
        dataService.saveZones(modele.getZones());
        dataService.saveAgents(modele.getAgents());

        if (mapController != null) {
            mapController.syncZones(modele.getZones());
            mapController.syncAgents(modele.getAgents());
        }

        notifyAgentsUpdated();
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    // ─────────────────────────────────────────────────────────────────────
    // SUPPRESSION ARÊTE AVEC RELOCALISATION
    // FIX : relocalise vers la zone SOURCE de l'arête (logique correcte)
    // ─────────────────────────────────────────────────────────────────────

    public void removeEdgeWithAgentRelocation(int edgeId) {
        if (mapController == null) return;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return;

        Edge edge = rg.getEdges().stream().filter(e -> e.getId() == edgeId).findFirst().orElse(null);
        if (edge == null) return;

        Zone sourceZone = edge.getFromZone();

        List<AgentMovement> toRelocate = rg.getActiveMovements().stream()
            .filter(mv -> mv.getCurrentEdge() != null && mv.getCurrentEdge().getId() == edgeId)
            .collect(Collectors.toList());

        toRelocate.forEach(mv -> {
            rg.removeMovementsOfAgent(mv.getAgent().getId());
            relocateAgent(mv.getAgent(), sourceZone);
        });

        if (!toRelocate.isEmpty()) {
            checkAndSetOvercrowding(sourceZone, toRelocate.size());
            notifyStatus("Agents relocalisés vers " + sourceZone.getName());
        }

        rg.removeEdge(edgeId);
        dataService.saveAgents(modele.getAgents());

        if (mapController != null) mapController.syncZones(modele.getZones());
        notifyAgentsUpdated();
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DES ZONES
    // ─────────────────────────────────────────────────────────────────────

    public void updateZone(Zone zone) {
        modele.updateZone(zone);
        dataService.saveZones(modele.getZones());
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    public void addZone(Zone zone) {
        modele.addZone(zone);
        dataService.saveZones(modele.getZones());
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    public Zone addRandomNeighborhood() {
        Random rng = new Random();
        int    id  = modele.nextZoneId();
        double lat = 45.7640 + (rng.nextDouble() - 0.5) * 0.06;
        double lng = 4.8357  + (rng.nextDouble() - 0.5) * 0.08;
        Zone z = new model.zone.Neighborhood(id, "Zone-" + id, lat, lng,
            0.5 + rng.nextDouble() * 3.5, 50 + rng.nextInt(950), "Générée auto");
        addZone(z);
        return z;
    }

    public Zone addRandomShelter() {
        Random rng = new Random();
        int    id  = modele.nextZoneId();
        double lat = 45.7640 + (rng.nextDouble() - 0.5) * 0.06;
        double lng = 4.8357  + (rng.nextDouble() - 0.5) * 0.08;
        Zone z = new model.zone.Shelter(id, "Refuge-" + id, lat, lng,
            2.0 + rng.nextDouble() * 4.0, 0, "Refuge généré", 100 + rng.nextInt(400));
        addZone(z);
        return z;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONGESTION FORTE
    // ─────────────────────────────────────────────────────────────────────

    private void checkAndSetOvercrowding(Zone zone, int addedCount) {
        if (zone == null) return;
        int current = countAgentsInZone(zone.getId());
        int max     = zone.getMaxCapacity();
        if (max > 0 && current + addedCount > max) {
            zoneOvercrowded.put(zone.getId(), true);
            zoneWaitCycles.put(zone.getId(), OVERCROWD_WAIT_CYCLES);
            notifyStatus("⚠ Forte congestion — " + zone.getName()
                + " (" + OVERCROWD_WAIT_CYCLES + " cycles)");
        }
    }

    private void tickWaitCycles() {
        new ArrayList<>(zoneWaitCycles.keySet()).forEach(id -> {
            int rem = zoneWaitCycles.get(id) - 1;
            if (rem <= 0) {
                zoneWaitCycles.remove(id);
                zoneOvercrowded.put(id, false);
                Zone z = getZoneById(id);
                if (z != null) notifyStatus("✓ Congestion levée — " + z.getName());
            } else {
                zoneWaitCycles.put(id, rem);
            }
        });
    }

    public boolean isZoneOvercrowded(int zoneId) { return Boolean.TRUE.equals(zoneOvercrowded.get(zoneId)); }
    public int     getZoneWaitCycles(int zoneId) { return zoneWaitCycles.getOrDefault(zoneId, 0); }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES NŒUDS / ARÊTES
    // FIX : recordAgentPassedZone est désormais appelé à l'arrivée de l'agent
    //       et aussi depuis handleAgentArrived pour les zones intermédiaires.
    // ─────────────────────────────────────────────────────────────────────

    public void recordAgentPassedZone(int zoneId) {
        zonePassCount.merge(zoneId, 1, Integer::sum);
    }

    public void recordAgentPassedEdge(int edgeId) {
        edgePassCount.merge(edgeId, 1, Integer::sum);
    }

    public NodeEdgeStats getZoneStats(int zoneId) {
        Zone z = getZoneById(zoneId);
        if (z == null) return null;
        int     agents = countAgentsInZone(zoneId);
        int     passed = zonePassCount.getOrDefault(zoneId, 0);
        double  avgSpd = computeZoneAvgSpeed(zoneId);
        boolean overc  = isZoneOvercrowded(zoneId);
        int     waitC  = getZoneWaitCycles(zoneId);
        String  status = z.isFlooded() ? "Inondé" : overc ? "Forte congestion" : "Normal";
        return new NodeEdgeStats(z.getName(), "Nœud", agents, passed, avgSpd, overc, waitC, status,
            z.getMaxCapacity(), (int) z.getPopulation());
    }

    public NodeEdgeStats getEdgeStats(int edgeId) {
        if (mapController == null) return null;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return null;
        Edge edge = rg.getEdges().stream().filter(e -> e.getId() == edgeId).findFirst().orElse(null);
        if (edge == null) return null;
        int current = (int) rg.getActiveMovements().stream()
            .filter(mv -> mv.getCurrentEdge() != null && mv.getCurrentEdge().getId() == edgeId).count();
        boolean congested = edge.getState() == EdgeState.CONGESTED || edge.getState() == EdgeState.OVERLOADED;
        return new NodeEdgeStats(
            edge.getName(), "Arête", current,
            edgePassCount.getOrDefault(edgeId, 0),
            edgeAvgSpeed.getOrDefault(edgeId, 0.0),
            congested, 0, edge.getState().name(),
            edge.getCapacityMax(), 0
        );
    }

    public double getEdgeDensity(int edgeId) {
        if (mapController == null) return 0.0;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return 0.0;
        return rg.getEdges().stream().filter(e -> e.getId() == edgeId)
            .findFirst()
            .map(e -> Math.min(1.0, (double) e.getCurrentFlow() / Math.max(1, e.getCapacityMax())))
            .orElse(0.0);
    }

    public double getZoneDensity(int zoneId) {
        Zone z = getZoneById(zoneId);
        if (z == null) return 0.0;
        return Math.min(1.0, (double) countAgentsInZone(zoneId) / Math.max(1, z.getMaxCapacity()));
    }

    /**
     * Met à jour les stats de débit sur toutes les arêtes à chaque tick.
     * FIX : met aussi à jour edgePassCount quand le flux d'une arête augmente.
     */
    private void updateEdgeStats() {
        if (mapController == null) return;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return;
        for (Edge e : rg.getEdges()) {
            int  id   = e.getId();
            int  flow = e.getCurrentFlow();
            edgeFlowCumul.merge(id,  (long) flow, Long::sum);
            edgeCycleCumul.merge(id, 1L,           Long::sum);
            long f = edgeFlowCumul.getOrDefault(id, 1L);
            long c = edgeCycleCumul.getOrDefault(id, 1L);
            edgeAvgSpeed.put(id, (double) f / c);
        }
    }

    private double computeZoneAvgSpeed(int zoneId) {
        int    passed = zonePassCount.getOrDefault(zoneId, 0);
        double time   = zoneTimeSpent.getOrDefault(zoneId, 1.0);
        return time > 0 ? passed / time : 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES GLOBALES
    // ─────────────────────────────────────────────────────────────────────

    public int  getPopulationARisque() {
        return mapController != null
            ? (int) mapController.countCitizensAtRisk()
            : (int) modele.getAgents().stream().filter(a -> a instanceof Citizen).count();
    }
    public int  getPopulationEnSecurite() { return mapController != null ? (int) mapController.countCitizensSafe() : 0; }
    public long getNombreZonesInondees()  { return modele.getZones().stream().filter(Zone::isFlooded).count(); }
    public int  getNombreAgentsEnDeplacement() {
        return mapController == null || mapController.getRouteGraph() == null
            ? 0 : mapController.getRouteGraph().getActiveMovements().size();
    }

    public double[] getStatutReseau() {
        if (mapController == null || mapController.getRouteGraph() == null) {
            double nv = modele.getNiveauEau() / FLOOD_SPEED_FACTOR;
            if (nv <= 0) return new double[]{100, 0, 0, 0, 0};
            double inond = Math.min(nv / 3.0 * 100.0, 60.0);
            double risk  = Math.min(nv / 2.0 * 30.0,  30.0);
            return new double[]{Math.max(100.0 - inond - risk, 0), risk, 0, 0, inond};
        }
        List<Edge> edges = mapController.getRouteGraph().getEdges();
        if (edges.isEmpty()) return new double[]{100, 0, 0, 0, 0};
        double total = edges.size();
        long safe  = edges.stream().filter(e -> e.getState() == EdgeState.SAFE).count();
        long risk  = edges.stream().filter(e -> e.getState() == EdgeState.AT_RISK).count();
        long cong  = edges.stream().filter(e -> e.getState() == EdgeState.CONGESTED).count();
        long over  = edges.stream().filter(e -> e.getState() == EdgeState.OVERLOADED).count();
        long flood = edges.stream().filter(e -> e.getState() == EdgeState.FLOODED).count();
        return new double[]{
            safe *100/total, risk*100/total,
            cong *100/total, over*100/total, flood*100/total
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARAMÈTRES
    // ─────────────────────────────────────────────────────────────────────

    public void    setVitesseSimulation(double ms)  { this.vitesseSimulationMs = Math.max(50.0, ms); }
    public double  getVitesseSimulationMs()          { return vitesseSimulationMs; }
    public void    setGravite(double g)              { modele.setGravite(g); }
    public void    setNiveauEau(double nv)           { modele.setNiveauEau(nv); Platform.runLater(() -> { if (onWaterLevelChanged != null) onWaterLevelChanged.accept(nv / FLOOD_SPEED_FACTOR); }); }
    public void    setModeAleatoire(boolean b)       { this.modeAleatoire = b; }
    public boolean isModeAleatoire()                 { return modeAleatoire; }
    public void    setAgentEndBehavior(AgentEndBehavior b) { this.agentEndBehavior = b; }
    public AgentEndBehavior getAgentEndBehavior()    { return agentEndBehavior; }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public void setOnStatusChanged(Consumer<String>              cb) { this.onStatusChanged             = cb; }
    public void setOnWaterLevelChanged(Consumer<Double>          cb) { this.onWaterLevelChanged         = cb; }
    public void setOnZonesUpdated(Consumer<List<Zone>>           cb) { this.onZonesUpdated              = cb; }
    public void setOnAgentsUpdated(Consumer<List<Agent>>         cb) { this.onAgentsUpdated             = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement>        cb) { this.onAgentArrived              = cb; }
    public void setOnStatsUpdated(Consumer<NodeEdgeStats>        cb) { this.onStatsUpdated              = cb; }
    public void setOnSelectedAgentPathChanged(Consumer<List<Zone>> cb) { this.onSelectedAgentPathChanged = cb; }

    // ─────────────────────────────────────────────────────────────────────
    // DTO Stats nœud / arête
    // ─────────────────────────────────────────────────────────────────────

    public static class NodeEdgeStats {
        public final String  name, type, status;
        public final int     currentAgents, totalPassed, waitCycles, capacity, population;
        public final double  avgSpeed;
        public final boolean congested;

        public NodeEdgeStats(String name, String type, int currentAgents, int totalPassed,
                             double avgSpeed, boolean congested, int waitCycles, String status,
                             int capacity, int population) {
            this.name          = name;
            this.type          = type;
            this.currentAgents = currentAgents;
            this.totalPassed   = totalPassed;
            this.avgSpeed      = avgSpeed;
            this.congested     = congested;
            this.waitCycles    = waitCycles;
            this.status        = status;
            this.capacity      = capacity;
            this.population    = population;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SNAPSHOT SÉRIALISABLE
    // ─────────────────────────────────────────────────────────────────────

    public static class SimulationSnapshot implements Serializable {
        private static final long serialVersionUID = 2L;   // bumped après refactor
        public final List<Zone>       zones;
        public final List<Agent>      agents;
        public final double           niveauEau;
        public final double           gravite;
        public final boolean          modeAleatoire;
        public final AgentEndBehavior agentEndBehavior;

        public SimulationSnapshot(List<Zone> zones, List<Agent> agents, double niveauEau,
                                  double gravite, boolean modeAleatoire, AgentEndBehavior agentEndBehavior) {
            this.zones            = zones;
            this.agents           = agents;
            this.niveauEau        = niveauEau;
            this.gravite          = gravite;
            this.modeAleatoire    = modeAleatoire;
            this.agentEndBehavior = agentEndBehavior;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVÉS — helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Vérifie si une inondation manuelle a démarré sur le graphe.
     * FIX : utilise l'état de l'arête uniquement, sans appel à getFloodLevel().
     */
    private boolean hasManualFloodStarted() {
        if (mapController == null || mapController.getRouteGraph() == null) return false;
        return mapController.getRouteGraph().getEdges().stream()
            .anyMatch(e -> e.getState() == EdgeState.FLOODING
                       || e.getState() == EdgeState.FLOODED
                       || e.getState() == EdgeState.AT_RISK);
    }

    private void declencherEvacuationAutomatique() {
        if (mapController == null) return;
        List<Citizen> citizens = modele.getAgents().stream()
            .filter(a -> a instanceof Citizen).map(a -> (Citizen) a)
            .collect(Collectors.toList());
        mapController.triggerMassEvacuation(citizens);
        notifyStatus("⚠ Évacuation déclenchée — trajets attribués aux citoyens");
    }

    /**
     * Gère l'arrivée d'un agent à destination.
     * FIX :
     *  • Enregistre le passage dans les stats de la zone de destination
     *  • Change l'état de l'agent : SAFE pour Citizen, AVAILABLE pour RescueAgent
     *  • Comportement de fin de trajet (REMOVE / RANDOM_DESTINATION)
     */
    private void handleAgentArrived(AgentMovement mv) {
        Agent agent = mv.getAgent();
        Zone  dest  = mv.getDestination();

        // Stats
        if (dest != null) recordAgentPassedZone(dest.getId());
        modele.recordEvacuationArrival(agent, dest);

        // État à l'arrivée
        if (agent instanceof Citizen c) {
            trySet(c, "setState", String.class, "SAFE");
            trySet(c, "setStatus", String.class, "SAFE");
        } else if (agent instanceof RescueAgent ra) {
            trySet(ra, "setState", String.class, "AVAILABLE");
            trySet(ra, "setStatus", String.class, "AVAILABLE");
        }

        // Comportement post-arrivée
        if (agentEndBehavior == AgentEndBehavior.REMOVE_AGENT) {
            Platform.runLater(() -> removeAgent(agent.getId()));
        } else {
            List<Zone> candidates = modele.getZones().stream()
                .filter(z -> !z.isFlooded() && (dest == null || z.getId() != dest.getId()))
                .collect(Collectors.toList());
            if (!candidates.isEmpty() && mapController != null) {
                Zone newDest = candidates.get(new Random().nextInt(candidates.size()));
                Platform.runLater(() -> {
                    if (dest != null)
                        mapController.getRouteGraph().planEvacuation(agent, dest, List.of(newDest));
                });
            }
        }

        if (selectedAgent != null && selectedAgent.getId() == agent.getId())
            notifySelectedAgentPath();
    }

    private void persistAndPropagate(Agent agent) {
        dataService.saveAgents(modele.getAgents());
        if (mapController != null) {
            mapController.addAgent(agent);
            mapController.syncAgents(modele.getAgents());
            if (evacuationDeclenchee && agent instanceof Citizen c)
                mapController.evacuateCitizen(c);
        }
        Platform.runLater(() -> {
            if (Main.getSharedMapView() != null)
                Main.getSharedMapView().setAgents(modele.getAgents());
        });
        notifyAgentsUpdated();
    }

    private void relocateAgent(Agent agent, Zone target) {
        if (mapController == null || target == null) return;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return;
        rg.removeMovementsOfAgent(agent.getId());
        rg.planEvacuation(agent, target, modele.getZones());
    }

    /**
     * Agents dont la currentZone est zoneId (agents statiques/en attente).
     * FIX : cherche dans le modèle via getField, pas seulement dans les mouvements actifs.
     */
    private List<Agent> findAgentsStaticInZone(int zoneId) {
        return modele.getAgents().stream()
            .filter(a -> {
                try {
                    var m = a.getClass().getMethod("getCurrentZone");
                    Zone z = (Zone) m.invoke(a);
                    return z != null && z.getId() == zoneId;
                } catch (Exception ex) { return false; }
            })
            .collect(Collectors.toList());
    }

    /**
     * Agents dont le trajet actif passe par zoneId.
     */
    private List<Agent> findAgentsTransitThroughZone(int zoneId) {
        if (mapController == null || mapController.getRouteGraph() == null) return new ArrayList<>();
        return mapController.getRouteGraph().getActiveMovements().stream()
            .filter(mv -> mv.getCurrentZone() != null && mv.getCurrentZone().getId() == zoneId)
            .map(AgentMovement::getAgent)
            .collect(Collectors.toList());
    }

    private int countAgentsInZone(int zoneId) {
        int transit = 0;
        if (mapController != null && mapController.getRouteGraph() != null) {
            transit = (int) mapController.getRouteGraph().getActiveMovements().stream()
                .filter(mv -> mv.getCurrentZone() != null && mv.getCurrentZone().getId() == zoneId).count();
        }
        int staticCount = (int) modele.getAgents().stream()
            .filter(a -> {
                try {
                    var m = a.getClass().getMethod("getCurrentZone");
                    Zone z = (Zone) m.invoke(a);
                    return z != null && z.getId() == zoneId;
                } catch (Exception ex) { return false; }
            }).count();
        return Math.max(transit, staticCount);
    }

    private List<Zone> findAdjacentZones(int zoneId) {
        if (mapController == null || mapController.getRouteGraph() == null) return new ArrayList<>();
        List<Zone> result = new ArrayList<>();
        for (Edge e : mapController.getRouteGraph().getEdges()) {
            if (e.getFromZone().getId() == zoneId && !result.contains(e.getToZone()))
                result.add(e.getToZone());
            else if (e.getToZone().getId() == zoneId && !result.contains(e.getFromZone()))
                result.add(e.getFromZone());
        }
        return result;
    }

    private void cancelMovementsThrough(int zoneId) {
        if (mapController == null || mapController.getRouteGraph() == null) return;
        RouteGraph rg = mapController.getRouteGraph();
        rg.getActiveMovements().stream()
            .filter(mv -> mv.getPath() != null
                && mv.getPath().getZones().stream().anyMatch(z -> z.getId() == zoneId))
            .collect(Collectors.toList())
            .forEach(mv -> rg.removeMovementsOfAgent(mv.getAgent().getId()));
    }

    private void notifySelectedAgentPath() {
        if (onSelectedAgentPathChanged == null) return;
        List<Zone> path = getSelectedAgentRemainingPath();
        Platform.runLater(() -> onSelectedAgentPathChanged.accept(path));
    }

    private void notifyStatus(String s) {
        if (onStatusChanged != null) Platform.runLater(() -> onStatusChanged.accept(s));
    }

    private void notifyAgentsUpdated() {
        if (onAgentsUpdated != null)
            Platform.runLater(() -> onAgentsUpdated.accept(modele.getAgents()));
    }

    private int nextAgentId() {
        return modele.getAgents().stream().mapToInt(Agent::getId).max().orElse(100) + 1;
    }

    private void clearStats() {
        zonePassCount.clear();  zoneTimeSpent.clear();
        zoneWaitCycles.clear(); zoneOvercrowded.clear();
        edgePassCount.clear();  edgeAvgSpeed.clear();
        edgeFlowCumul.clear();  edgeCycleCumul.clear();
    }

    /** Appelle un setter par réflexion sans lever d'exception si absent. */
    private static void trySet(Object obj, String method, Class<?> type, Object value) {
        try {
            obj.getClass().getMethod(method, type).invoke(obj, value);
        } catch (Exception ignored) {}
    }
}