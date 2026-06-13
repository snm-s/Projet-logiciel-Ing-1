package controller.AdminPage;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import app.Main;
import controller.MapController;
import javafx.application.Platform;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.PMRAgent;
import model.agent.RescueAgent;
import model.enums.CitizenState;
import model.enums.RescueState;
import model.graph.AgentMovement;
import model.graph.Edge;
import model.graph.EdgeState;
import model.graph.RouteGraph;
import model.simulation.FloodSimulation;
import model.simulation.SimulationDataService;
import model.zone.Zone;

/**
 * Contrôleur principal de la simulation d'inondation.
 *
 * Responsabilités :
 * ─ Orchestration (démarrage, pause, tick, reset)
 * ─ Modifications runtime (ajout/suppression agents et zones) → via FloodSimulation uniquement
 * ─ Statistiques nœuds/arêtes (locales au controller, non persistées)
 * ─ Export/import d'état (sérialisation snapshot)
 * ─ Callbacks UI (Platform.runLater)
 *
 * Ce controller NE DOIT PAS appeler dataService pour les modifications runtime.
 * dataService est utilisé uniquement :
 *   • Au démarrage (chargement initial via FloodSimulation)
 *   • À l'export/import explicite de fichier
 */
public class SimulationController {

    // ─── Constantes ──────────────────────────────────────────────────────
    private static final double SEUIL_EVACUATION_M   = 0.5;
    private static final double DELTA_SECONDS        = 1.0;
    private static final int    OVERCROWD_WAIT_CYCLES = 2;
    public  static final double FLOOD_SPEED_FACTOR   = 60.0;

    // ─── Rôles d'agents ──────────────────────────────────────────────────
    public enum AgentRole { CITIZEN, PMR, RESCUE }

    // ─── Mode fin de trajet ──────────────────────────────────────────────
    public enum AgentEndBehavior { RANDOM_DESTINATION, REMOVE_AGENT }

    // ─── Modèle & services ───────────────────────────────────────────────
    private final FloodSimulation       modele;
    private final SimulationDataService dataService;   // lecture initiale + export/import fichier UNIQUEMENT
    private MapController               mapController;

    // ─── Paramètres simulation ───────────────────────────────────────────
    private double           vitesseSimulationMs  = 500.0;
    private boolean          modeAleatoire        = true;
    private AgentEndBehavior agentEndBehavior     = AgentEndBehavior.RANDOM_DESTINATION;
    private boolean          evacuationDeclenchee = false;

    // ─── Plages de création d'agents ─────────────────────────────────────
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
    private Consumer<String>           onStatusChanged;
    private Consumer<Double>           onWaterLevelChanged;
    private Consumer<List<Zone>>       onZonesUpdated;
    private Consumer<List<Agent>>      onAgentsUpdated;
    private Consumer<AgentMovement>    onAgentArrived;
    private Consumer<NodeEdgeStats>    onStatsUpdated;
    private Consumer<List<Zone>>       onSelectedAgentPathChanged;

    // ─── État sauvegardé pour "Recommencer" ──────────────────────────────
    private byte[] savedStateBytes = null;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTEURS
    // ─────────────────────────────────────────────────────────────────────

    public SimulationController(FloodSimulation simulation, SimulationDataService dataService) {
        this.modele      = simulation;
        this.dataService = dataService;
        // Snapshot pris une seule fois au démarrage pour pouvoir "Recommencer"
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

    public FloodSimulation       getModele()        { return modele; }
    public List<Zone>            getZones()         { return modele.getZones(); }
    public List<Agent>           getAgents()        { return modele.getAgents(); }
    public SimulationDataService getDataService()   { return dataService; }
    public MapController         getMapController() { return mapController; }

    public Zone getZoneById(int id) {
        return modele.getZones().stream()
            .filter(z -> z.getId() == id).findFirst().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTRÔLES SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    public void demarrerSimulation() {
        modele.demarrer();                     // publie l'alerte de démarrage
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

    public void executerPas() {
        if (modele.isEnPause()) return;

        if (modeAleatoire) modele.executerPas();

        double niveauScaled = modele.getNiveauEau() / FLOOD_SPEED_FACTOR;

        if (!evacuationDeclenchee) {
            boolean doEvac = modeAleatoire
                ? niveauScaled >= SEUIL_EVACUATION_M
                : hasManualFloodStarted();
            if (doEvac) {
                declencherEvacuationAutomatique();
                evacuationDeclenchee = true;
            }
        }

        if (mapController != null) {
            if (evacuationDeclenchee) mapController.tick(DELTA_SECONDS);
            mapController.updateAllZones(modele.getZones());
        }

        tickWaitCycles();
        updateEdgeStats();
        notifySelectedAgentPath();

        Platform.runLater(() -> {
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(niveauScaled);
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESET
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Remet la simulation à l'état initial sauvegardé.
     * Utilise le snapshot en mémoire (savedStateBytes), sinon dataService.reset().
     * Ne persiste PAS dans dataService — la simulation est l'état de référence.
     */
    public void resetSimulation() {
        if (savedStateBytes != null) {
            restoreFromBytes(savedStateBytes);
        } else {
            // Fallback : relecture depuis dataService (démarrage seulement)
            Object[] initial = dataService.reset();
            @SuppressWarnings("unchecked") List<Zone>  zones  = (List<Zone>)  initial[0];
            @SuppressWarnings("unchecked") List<Agent> agents = (List<Agent>) initial[1];
            modele.resetSimulation();
            if (zones  != null && !zones.isEmpty())  modele.setZones(zones);
            if (agents != null && !agents.isEmpty()) modele.setAgents(agents);
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

    /**
     * Remet toutes les zones à leur état initial (non inondées, non évacuées).
     * Modifie FloodSimulation uniquement — pas de persistance dataService.
     */
    public void resetAllZones() {
        modele.getZones().forEach(Zone::reset);
        // Notifie les observers de FloodSimulation via setZones (recopy)
        modele.setZones(modele.getZones());
        if (mapController != null) mapController.syncZones(modele.getZones());
        clearStats();
        evacuationDeclenchee = false;
        notifyStatus("Zones réinitialisées");
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT / IMPORT ÉTAT  ← seuls endroits légitimes pour dataService I/O
    // ─────────────────────────────────────────────────────────────────────

    public boolean exportState(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(buildSnapshot());
            notifyStatus("État exporté : " + file.getName());
            return true;
        } catch (Exception e) {
            notifyStatus("Erreur export : " + e.getMessage());
            return false;
        }
    }

    public boolean importState(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            applySnapshot((SimulationSnapshot) ois.readObject());
            notifyStatus("État importé : " + file.getName());
            return true;
        } catch (Exception e) {
            notifyStatus("Erreur import : " + e.getMessage());
            return false;
        }
    }

    /** Enregistre l'état courant comme point de départ de "Recommencer". */
    public void saveCurrentStateAsInitial() {
        saveInitialState();
        notifyStatus("État initial enregistré.");
    }

    private void saveInitialState() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(buildSnapshot());
            savedStateBytes = baos.toByteArray();
        } catch (Exception e) {
            System.err.println("saveInitialState: " + e.getMessage());
        }
    }

    private void restoreFromBytes(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            applySnapshot((SimulationSnapshot) ois.readObject());
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
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(snap.niveauEau / FLOOD_SPEED_FACTOR);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DES AGENTS
    // Toutes les modifications passent par modele.addAgent / modele.removeAgent
    // → pas de dataService ici.
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Crée et ajoute un agent.
     * La construction instancie directement les classes concrètes
     * (Citizen, PMRAgent, RescueAgent) sans passer par dataService.
     *
     * @param role      type d'agent (null → CITIZEN)
     * @param firstName prénom (null → aléatoire via dataService.createRandom*)
     * @param lastName  nom   (null → aléatoire)
     * @param age       < 0 → tiré dans la plage configurée
     * @param speed     < 0 → tiré dans la plage configurée
     * @param stress    < 0 → tiré dans la plage configurée (Citizen uniquement)
     * @param startZone zone de départ (null → aléatoire parmi les zones non inondées)
     */
    public Agent addAgent(AgentRole role, String firstName, String lastName,
                          int age, double speed, int stress, Zone startZone) {
        Random rng = new Random();
        AgentRole effectiveRole = role != null ? role : AgentRole.CITIZEN;

        int    effAge    = age    < 0 ? agentAgeMin    + rng.nextInt(Math.max(1, agentAgeMax    - agentAgeMin))    : age;
        double effSpeed  = speed  < 0 ? agentSpeedMin  + rng.nextDouble() * (agentSpeedMax  - agentSpeedMin)  : speed;
        int effStress = stress < 0 ? rng.nextInt(2) : stress; //il faut que dans le view on puisse cocher stresser ou pas

        // Zone de départ : paramètre > zone non inondée aléatoire
        Zone zone = startZone != null ? startZone : pickRandomNonFloodedZone(rng);

        int id = modele.nextAgentId();

        // Node correspondant à la zone (null accepté par les constructeurs)
        model.graph.Node node = zone != null ? zoneToNode(zone) : null;

        String fn = firstName != null ? firstName : randomFirstName(rng);
        String ln = lastName  != null ? lastName  : randomLastName(rng);

        Agent agent = switch (effectiveRole) {
            case PMR -> {
                PMRAgent pmr = new PMRAgent(id, fn, ln, node);
                pmr.setAge(effAge);
                pmr.setMaxSpeed(effSpeed);
                if (zone != null) pmr.setCurrentZone(zone);
                yield pmr;
            }
            case RESCUE -> {
                RescueAgent ra = new RescueAgent(id, fn, ln, node);
                ra.setAge(effAge);
                ra.setMaxSpeed(effSpeed);
                ra.setState(RescueState.DISPONIBLE);
                if (zone != null) ra.setCurrentZone(zone);
                yield ra;
            }
            default -> {  // CITIZEN
                Citizen c = new Citizen(id, fn, ln, node);
                c.setAge(effAge);
                c.setMaxSpeed(effSpeed);
                if (effStress==1) {
                    c.setState(CitizenState.STRESSED);
                } else {
                    c.setState(CitizenState.CALM);
                }
                if (zone != null) c.setCurrentZone(zone);
                yield c;
            }
        };

        // Ajout dans FloodSimulation (notifie les observers)
        modele.addAgent(agent);
        propagateAgentToMap(agent);
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

    /** Ajoute n agents avec paramètres aléatoires (~20 % secours, ~10 % PMR). */
    public List<Agent> addRandomAgents(int count) {
        List<Agent> added = new ArrayList<>();
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            int r = rng.nextInt(10);
            AgentRole role = r == 0 ? AgentRole.RESCUE : r == 1 ? AgentRole.PMR : AgentRole.CITIZEN;
            Agent a = addAgent(role, null, null, -1, -1, -1, null);
            if (a != null) added.add(a);
        }
        return added;
    }

    /**
     * Supprime un agent de FloodSimulation ET du RouteGraph.
     * Pas de persistance dataService.
     */
    public boolean removeAgent(int agentId) {
        // 1. Annuler les mouvements actifs dans le graphe
        if (mapController != null && mapController.getRouteGraph() != null)
            mapController.getRouteGraph().removeMovementsOfAgent(agentId);

        // 2. Trouver l'agent avant suppression (pour nettoyer la sélection)
        Agent toRemove = modele.getAgents().stream()
            .filter(a -> a.getId() == agentId).findFirst().orElse(null);
        if (toRemove == null) return false;

        // 3. Supprimer de FloodSimulation (notifie les observers)
        modele.removeAgent(toRemove);

        // 4. Synchro MapController
        if (mapController != null) mapController.removeAgent(agentId);

        // 5. Nettoyer la sélection
        if (selectedAgent != null && selectedAgent.getId() == agentId) {
            selectedAgent = null;
            if (onSelectedAgentPathChanged != null)
                Platform.runLater(() -> onSelectedAgentPathChanged.accept(new ArrayList<>()));
        }

        notifyAgentsUpdated();
        return true;
    }

    /**
     * Remplace toute la liste d'agents dans FloodSimulation.
     * Utilisé lors d'un import ou d'un reset.
     */
    public void setAgents(List<Agent> agents) {
        modele.setAgents(agents);
        if (mapController != null) mapController.syncAgents(modele.getAgents());
        notifyAgentsUpdated();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PLAGES DE GÉNÉRATION D'AGENTS
    // ─────────────────────────────────────────────────────────────────────

    public void setAgentAgeRange(int min, int max)          { agentAgeMin = min;    agentAgeMax = max; }
    public void setAgentSpeedRange(double min, double max)  { agentSpeedMin = min;  agentSpeedMax = max; }
    public void setAgentStressRange(double min, double max) { agentStressMin = min; agentStressMax = max; }

    public int    getAgentAgeMin()    { return agentAgeMin; }
    public int    getAgentAgeMax()    { return agentAgeMax; }
    public double getAgentSpeedMin()  { return agentSpeedMin; }
    public double getAgentSpeedMax()  { return agentSpeedMax; }
    public double getAgentStressMin() { return agentStressMin; }
    public double getAgentStressMax() { return agentStressMax; }

    // ─────────────────────────────────────────────────────────────────────
    // SÉLECTION AGENT
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
    // GESTION DES ZONES
    // Toutes les modifications passent par FloodSimulation — pas de dataService.
    // ─────────────────────────────────────────────────────────────────────

    public void addZone(Zone zone) {
        modele.addZone(zone);                          // notifie les observers
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    public void updateZone(Zone zone) {
        modele.updateZone(zone);                       // notifie les observers
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    /**
     * Supprime une zone de FloodSimulation avec relocalisation des agents.
     * Supprime aussi les arêtes connectées du RouteGraph.
     * Pas de persistance dataService.
     */
    public void removeZone(int zoneId) {
        Zone toRemove = getZoneById(zoneId);
        if (toRemove == null) return;

        // Agents à relocaliser (statiques + en transit)
        List<Agent> agentsToRelocate = new ArrayList<>();
        agentsToRelocate.addAll(findAgentsStaticInZone(zoneId));
        agentsToRelocate.addAll(findAgentsTransitThroughZone(zoneId));
        agentsToRelocate = agentsToRelocate.stream().distinct().collect(Collectors.toList());

        List<Zone> adjacent = findAdjacentZones(zoneId);

        if (!agentsToRelocate.isEmpty()) {
            if (adjacent.isEmpty()) {
                agentsToRelocate.forEach(a -> modele.removeAgent(a));
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
            new ArrayList<>(rg.getEdges()).stream()
                .filter(e -> e.getFromZone().getId() == zoneId || e.getToZone().getId() == zoneId)
                .forEach(e -> rg.removeEdge(e.getId()));
        }

        modele.removeZoneById(zoneId);                 // notifie les observers

        if (mapController != null) {
            mapController.syncZones(modele.getZones());
            mapController.syncAgents(modele.getAgents());
        }

        notifyAgentsUpdated();
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
    // SUPPRESSION ARÊTE AVEC RELOCALISATION
    // ─────────────────────────────────────────────────────────────────────

    public void removeEdgeWithAgentRelocation(int edgeId) {
        if (mapController == null) return;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return;

        Edge edge = rg.getEdges().stream()
            .filter(e -> e.getId() == edgeId).findFirst().orElse(null);
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
        if (mapController != null) mapController.syncZones(modele.getZones());
        notifyAgentsUpdated();
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONGESTION
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
    // STATISTIQUES NŒUDS / ARÊTES  (locales au controller, non persistées)
    // ─────────────────────────────────────────────────────────────────────

    public void recordAgentPassedZone(int zoneId) { zonePassCount.merge(zoneId, 1, Integer::sum); }
    public void recordAgentPassedEdge(int edgeId) { edgePassCount.merge(edgeId, 1, Integer::sum); }

    public NodeEdgeStats getZoneStats(int zoneId) {
        Zone z = getZoneById(zoneId);
        if (z == null) return null;
        int     agents  = countAgentsInZone(zoneId);
        int     passed  = zonePassCount.getOrDefault(zoneId, 0);
        double  avgSpd  = computeZoneAvgSpeed(zoneId);
        boolean overc   = isZoneOvercrowded(zoneId);
        int     waitC   = getZoneWaitCycles(zoneId);
        String  status  = z.isFlooded() ? "Inondé" : overc ? "Forte congestion" : "Normal";
        return new NodeEdgeStats(z.getName(), "Nœud", agents, passed, avgSpd,
            overc, waitC, status, z.getMaxCapacity(), (int) z.getPopulation());
    }

    public NodeEdgeStats getEdgeStats(int edgeId) {
        if (mapController == null) return null;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return null;
        Edge edge = rg.getEdges().stream()
            .filter(e -> e.getId() == edgeId).findFirst().orElse(null);
        if (edge == null) return null;
        int current = (int) rg.getActiveMovements().stream()
            .filter(mv -> mv.getCurrentEdge() != null
                       && mv.getCurrentEdge().getId() == edgeId).count();
        boolean congested = edge.getState() == EdgeState.CONGESTED
                         || edge.getState() == EdgeState.OVERLOADED;
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
        return rg.getEdges().stream().filter(e -> e.getId() == edgeId).findFirst()
            .map(e -> Math.min(1.0, (double) e.getCurrentFlow() / Math.max(1, e.getCapacityMax())))
            .orElse(0.0);
    }

    public double getZoneDensity(int zoneId) {
        Zone z = getZoneById(zoneId);
        if (z == null) return 0.0;
        return Math.min(1.0, (double) countAgentsInZone(zoneId) / Math.max(1, z.getMaxCapacity()));
    }

    private void updateEdgeStats() {
        if (mapController == null) return;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return;
        for (Edge e : rg.getEdges()) {
            int id   = e.getId();
            int flow = e.getCurrentFlow();
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

    public int getPopulationARisque() {
        return mapController != null
            ? (int) mapController.countCitizensAtRisk()
            : (int) modele.getAgents().stream().filter(a -> a instanceof Citizen).count();
    }

    public int  getPopulationEnSecurite()     { return mapController != null ? (int) mapController.countCitizensSafe() : 0; }
    public long getNombreZonesInondees()       { return modele.getZones().stream().filter(Zone::isFlooded).count(); }
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
            safe*100/total, risk*100/total, cong*100/total, over*100/total, flood*100/total
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARAMÈTRES
    // ─────────────────────────────────────────────────────────────────────

    public void    setVitesseSimulation(double ms)           { this.vitesseSimulationMs = Math.max(50.0, ms); }
    public double  getVitesseSimulationMs()                  { return vitesseSimulationMs; }
    public void    setGravite(double g)                      { modele.setGravite(g); }
    public void    setNiveauEau(double nv)                   { modele.setNiveauEau(nv); Platform.runLater(() -> { if (onWaterLevelChanged != null) onWaterLevelChanged.accept(nv / FLOOD_SPEED_FACTOR); }); }
    public void    setModeAleatoire(boolean b)               { this.modeAleatoire = b; }
    public boolean isModeAleatoire()                         { return modeAleatoire; }
    public void    setAgentEndBehavior(AgentEndBehavior b)   { this.agentEndBehavior = b; }
    public AgentEndBehavior getAgentEndBehavior()            { return agentEndBehavior; }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public void setOnStatusChanged(Consumer<String>              cb) { this.onStatusChanged             = cb; }
    public void setOnWaterLevelChanged(Consumer<Double>          cb) { this.onWaterLevelChanged         = cb; }
    public void setOnZonesUpdated(Consumer<List<Zone>>           cb) { this.onZonesUpdated              = cb; }
    public void setOnAgentsUpdated(Consumer<List<Agent>>         cb) { this.onAgentsUpdated             = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement>        cb) { this.onAgentArrived              = cb; }
    public void setOnStatsUpdated(Consumer<NodeEdgeStats>        cb) { this.onStatsUpdated              = cb; }
    public void setOnSelectedAgentPathChanged(Consumer<List<Zone>> cb){ this.onSelectedAgentPathChanged = cb; }

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
        private static final long serialVersionUID = 3L;
        public final List<Zone>       zones;
        public final List<Agent>      agents;
        public final double           niveauEau;
        public final double           gravite;
        public final boolean          modeAleatoire;
        public final AgentEndBehavior agentEndBehavior;

        public SimulationSnapshot(List<Zone> zones, List<Agent> agents,
                                  double niveauEau, double gravite,
                                  boolean modeAleatoire, AgentEndBehavior agentEndBehavior) {
            this.zones           = zones;
            this.agents          = agents;
            this.niveauEau       = niveauEau;
            this.gravite         = gravite;
            this.modeAleatoire   = modeAleatoire;
            this.agentEndBehavior = agentEndBehavior;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVÉS — helpers
    // ─────────────────────────────────────────────────────────────────────

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

    private void handleAgentArrived(AgentMovement mv) {
        Agent agent = mv.getAgent();
        Zone  dest  = mv.getDestination();

        if (dest != null) recordAgentPassedZone(dest.getId());
        modele.recordEvacuationArrival(agent, dest);

        // État à l'arrivée — appel direct aux méthodes concrètes (pas de réflexion)
        if (agent instanceof Citizen c) {
            c.setState(CitizenState.SAFE);
        } else if (agent instanceof RescueAgent ra) {
            ra.setState(RescueState.DISPONIBLE);
        }

        if (agentEndBehavior == AgentEndBehavior.REMOVE_AGENT) {
            Platform.runLater(() -> removeAgent(agent.getId()));
        } else {
            // Nouvelle destination aléatoire non inondée
            List<Zone> candidates = modele.getZones().stream()
                .filter(z -> !z.isFlooded() && (dest == null || z.getId() != dest.getId()))
                .collect(Collectors.toList());
            if (!candidates.isEmpty() && mapController != null && dest != null) {
                Zone newDest = candidates.get(new Random().nextInt(candidates.size()));
                Platform.runLater(() ->
                    mapController.getRouteGraph().planEvacuation(agent, dest, List.of(newDest)));
            }
        }

        if (selectedAgent != null && selectedAgent.getId() == agent.getId())
            notifySelectedAgentPath();
    }

    /**
     * Propage un agent nouvellement créé au MapController.
     * Ne passe pas par dataService.
     */
    private void propagateAgentToMap(Agent agent) {
        if (mapController != null) {
            mapController.addAgent(agent);
            mapController.syncAgents(modele.getAgents());
            if (evacuationDeclenchee && agent instanceof Citizen c)
                mapController.evacuateCitizen(c);
        }
        notifyAgentsUpdated();
    }

    private void relocateAgent(Agent agent, Zone target) {
        if (mapController == null || target == null) return;
        RouteGraph rg = mapController.getRouteGraph();
        if (rg == null) return;
        rg.removeMovementsOfAgent(agent.getId());
        rg.planEvacuation(agent, target, modele.getZones());
    }

    private List<Agent> findAgentsStaticInZone(int zoneId) {
        return modele.getAgents().stream()
            .filter(a -> {
                Zone z = a.getCurrentZone();
                return z != null && z.getId() == zoneId;
            })
            .collect(Collectors.toList());
    }

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
                .filter(mv -> mv.getCurrentZone() != null
                           && mv.getCurrentZone().getId() == zoneId).count();
        }
        int stat = (int) modele.getAgents().stream()
            .filter(a -> { Zone z = a.getCurrentZone(); return z != null && z.getId() == zoneId; })
            .count();
        return Math.max(transit, stat);
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

    private Zone pickRandomNonFloodedZone(Random rng) {
        List<Zone> candidates = modele.getZones().stream()
            .filter(z -> !z.isFlooded()).collect(Collectors.toList());
        return candidates.isEmpty() ? null : candidates.get(rng.nextInt(candidates.size()));
    }

    /**
     * Crée un Node minimal à partir d'une Zone pour les constructeurs d'Agent.
     * Adaptez selon votre implémentation de Node.
     */
    private model.graph.Node zoneToNode(Zone zone) {
        return new model.graph.Node(zone.getLatitude(), zone.getLongitude());
    }

    private static final String[] FIRSTNAMES = {
        "Alice","Bob","Clara","David","Emma","Félix","Gina","Hugo","Inès","Jules"
    };
    private static final String[] LASTNAMES = {
        "Martin","Bernard","Dubois","Thomas","Robert","Petit","Durand","Leroy","Moreau","Simon"
    };
    private String randomFirstName(Random rng) { return FIRSTNAMES[rng.nextInt(FIRSTNAMES.length)]; }
    private String randomLastName(Random rng)  { return LASTNAMES[rng.nextInt(LASTNAMES.length)]; }

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

    private void clearStats() {
        zonePassCount.clear();  zoneTimeSpent.clear();
        zoneWaitCycles.clear(); zoneOvercrowded.clear();
        edgePassCount.clear();  edgeAvgSpeed.clear();
        edgeFlowCumul.clear();  edgeCycleCumul.clear();
    }
}