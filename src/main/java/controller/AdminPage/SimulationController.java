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
import model.simulation.EvacuationEvent;
import model.simulation.FloodSimulation;
import model.simulation.SimulationDataService;
import model.zone.Zone;

/**
 * Contrôleur principal de la simulation d'inondation.
 *
 * Responsabilités :
 * ─ Orchestration (démarrage, pause, tick, reset)
 * ─ Modifications runtime (ajout/suppression/déplacement agents et zones,
 *   création d'arêtes) → via FloodSimulation uniquement
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
    private static final double SEUIL_EVACUATION_M    = 0.5;
    private static final double DELTA_SECONDS         = 1.0;
    private static final int    OVERCROWD_WAIT_CYCLES = 2;
    public  static final double FLOOD_SPEED_FACTOR    = 60.0;

    // ─── Rôles d'agents ──────────────────────────────────────────────────
    public enum AgentRole { CITIZEN, PMR, RESCUE }

    // ─── Modèle & services ───────────────────────────────────────────────
    private final FloodSimulation       modele;
    private final SimulationDataService dataService;   // lecture initiale + export/import fichier UNIQUEMENT
    private MapController               mapController;

    // ─── Paramètres simulation ───────────────────────────────────────────
    private double  vitesseSimulationMs  = 500.0;
    private boolean modeAleatoire        = true;
    private boolean evacuationDeclenchee = false;

    // ─── Plages de création d'agents ─────────────────────────────────────
    private int    agentAgeMin   = 18;  private int    agentAgeMax   = 75;
    private double agentSpeedMin = 0.5; private double agentSpeedMax = 2.0;

    // ─── Stats nœuds ─────────────────────────────────────────────────────
    private final Map<Integer, Integer> zonePassCount   = new HashMap<>();
    private final Map<Integer, Double>  zoneTimeSpent   = new HashMap<>();   // secondes cumulées
    private final Map<Integer, Integer> zoneWaitCycles  = new HashMap<>();
    private final Map<Integer, Boolean> zoneOvercrowded = new HashMap<>();

    // ─── Stats arêtes ────────────────────────────────────────────────────
    private final Map<Integer, Integer> edgePassCount  = new HashMap<>();
    private final Map<Integer, Double>  edgeAvgSpeed   = new HashMap<>();
    private final Map<Integer, Long>    edgeFlowCumul  = new HashMap<>();
    private final Map<Integer, Long>    edgeCycleCumul = new HashMap<>();

    // ─── Sélection ───────────────────────────────────────────────────────
    private Agent selectedAgent = null;

    // ─── Callbacks UI ────────────────────────────────────────────────────
    private Consumer<String>                    onStatusChanged;
    private Consumer<Double>                    onWaterLevelChanged;
    private Consumer<List<Zone>>                onZonesUpdated;
    private Consumer<List<Agent>>               onAgentsUpdated;
    private Consumer<AgentMovement>             onAgentArrived;
    private Consumer<NodeEdgeStats>             onStatsUpdated;
    private Consumer<List<Zone>>                onSelectedAgentPathChanged;
    private Consumer<List<EvacuationEvent>>     onHistoryUpdated;

    // ─── État sauvegardé pour "Recommencer" ──────────────────────────────
    private byte[] savedStateBytes = null;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTEURS
    // ─────────────────────────────────────────────────────────────────────

    public SimulationController(FloodSimulation simulation, SimulationDataService dataService) {
        this.modele      = simulation;
        this.dataService = dataService;
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
        modele.demarrer();
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

        if (modeAleatoire) {
            // Mode auto : monte l'eau ET avance le temps
            modele.executerPas();
        } else {
            // FIX mode manuel : avance le temps sans monter l'eau
            // (l'inondation est déclenchée manuellement par l'utilisateur)
            modele.avancerTempsSansMontee(DELTA_SECONDS);
        }

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
        notifyHistoryUpdated();

        Platform.runLater(() -> {
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(niveauScaled);
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESET
    // ─────────────────────────────────────────────────────────────────────

    public void resetSimulation() {
        if (savedStateBytes != null) {
            restoreFromBytes(savedStateBytes);
        } else {
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
        notifyHistoryUpdated();
        Platform.runLater(() -> {
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
            if (onAgentsUpdated     != null) onAgentsUpdated.accept(modele.getAgents());
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(0.0);
        });
    }

    /**
     * Remet toutes les zones à leur état initial (non inondées, non évacuées).
     * FIX : on passe une COPIE de la liste pour éviter que setZones() fasse
     *       clear() puis addAll() sur la même instance → liste vide.
     */
    public void resetAllZones() {
        modele.getZones().forEach(Zone::reset);
        modele.setZones(new ArrayList<>(modele.getZones())); // copie → notifie les observers
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
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(buildSnapshot());
            notifyStatus("État exporté : " + file.getName());
            return true;
        } catch (Exception e) {
            notifyStatus("Erreur export : " + e.getMessage());
            return false;
        }
    }

    /**
     * FIX import : après applySnapshot, reconstruit le RouteGraph depuis les
     * nouvelles zones pour que la carte reflète l'état importé.
     */
    public boolean importState(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            applySnapshot((SimulationSnapshot) ois.readObject());
            rebuildRouteGraph();
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
            new ObjectOutputStream(baos).writeObject(buildSnapshot());
            savedStateBytes = baos.toByteArray();
        } catch (Exception e) {
            System.err.println("saveInitialState: " + e.getMessage());
        }
    }

    private void restoreFromBytes(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            applySnapshot((SimulationSnapshot) ois.readObject());
            rebuildRouteGraph();
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
            modeAleatoire
        );
    }

    private void applySnapshot(SimulationSnapshot snap) {
        modele.resetSimulation();
        if (snap.zones  != null && !snap.zones.isEmpty())  modele.setZones(new ArrayList<>(snap.zones));
        if (snap.agents != null && !snap.agents.isEmpty()) modele.setAgents(new ArrayList<>(snap.agents));
        modele.setNiveauEau(snap.niveauEau);
        modele.setGravite(snap.gravite);
        modeAleatoire        = snap.modeAleatoire;
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

    /**
     * Recrée un RouteGraph propre à partir des zones courantes de FloodSimulation.
     * Appelé après import et après reset pour que la carte soit cohérente.
     */
    private void rebuildRouteGraph() {
        if (mapController == null) return;
        mapController.rebuildRouteGraph(modele.getZones());
        mapController.syncZones(modele.getZones());
        mapController.syncAgents(modele.getAgents());
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DES AGENTS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Crée et ajoute un agent avec les paramètres fournis.
     * stress : 0 = calme, 1 = stressé, -1 = aléatoire.
     */
    public Agent addAgent(AgentRole role, String firstName, String lastName,
                          int age, double speed, int stress, Zone startZone) {
        Random rng = new Random();
        AgentRole effectiveRole = role != null ? role : AgentRole.CITIZEN;

        int    effAge    = age   < 0 ? agentAgeMin   + rng.nextInt(Math.max(1, agentAgeMax   - agentAgeMin))  : age;
        double effSpeed  = speed < 0 ? agentSpeedMin + rng.nextDouble() * (agentSpeedMax - agentSpeedMin)     : speed;
        int    effStress = stress < 0 ? rng.nextInt(2) : stress;

        Zone zone = startZone != null ? startZone : pickRandomNonFloodedZone(rng);
        int  id   = modele.nextAgentId();

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
            default -> {
                Citizen c = new Citizen(id, fn, ln, node);
                c.setAge(effAge);
                c.setMaxSpeed(effSpeed);
                c.setState(effStress == 1 ? CitizenState.STRESSED : CitizenState.CALM);
                if (zone != null) c.setCurrentZone(zone);
                yield c;
            }
        };

        modele.addAgent(agent);
        propagateAgentToMap(agent);
        return agent;
    }

    public Citizen    addRandomCitizen()     { return (Citizen)     addAgent(AgentRole.CITIZEN, null, null, -1, -1, -1, null); }
    public RescueAgent addRandomRescueAgent(){ return (RescueAgent) addAgent(AgentRole.RESCUE,  null, null, -1, -1, -1, null); }

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

    public boolean removeAgent(int agentId) {
        if (mapController != null && mapController.getRouteGraph() != null)
            mapController.getRouteGraph().removeMovementsOfAgent(agentId);

        Agent toRemove = modele.getAgents().stream()
            .filter(a -> a.getId() == agentId).findFirst().orElse(null);
        if (toRemove == null) return false;

        modele.removeAgent(toRemove);

        if (mapController != null) mapController.removeAgent(agentId);

        if (selectedAgent != null && selectedAgent.getId() == agentId) {
            selectedAgent = null;
            if (onSelectedAgentPathChanged != null)
                Platform.runLater(() -> onSelectedAgentPathChanged.accept(new ArrayList<>()));
        }

        notifyAgentsUpdated();
        return true;
    }

    public void setAgents(List<Agent> agents) {
        modele.setAgents(agents);
        if (mapController != null) mapController.syncAgents(modele.getAgents());
        notifyAgentsUpdated();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PLAGES DE GÉNÉRATION D'AGENTS
    // ─────────────────────────────────────────────────────────────────────

    public void setAgentAgeRange(int min, int max)         { agentAgeMin = min;   agentAgeMax = max; }
    public void setAgentSpeedRange(double min, double max) { agentSpeedMin = min; agentSpeedMax = max; }

    public int    getAgentAgeMin()   { return agentAgeMin; }
    public int    getAgentAgeMax()   { return agentAgeMax; }
    public double getAgentSpeedMin() { return agentSpeedMin; }
    public double getAgentSpeedMax() { return agentSpeedMax; }

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
    // ─────────────────────────────────────────────────────────────────────

    public void addZone(Zone zone) {
        modele.addZone(zone);
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    public void updateZone(Zone zone) {
        modele.updateZone(zone);
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    /**
     * Crée un quartier avec des paramètres explicites (pas aléatoires).
     * Pour l'ajout depuis l'UI avec formulaire.
     */
    public Zone addNeighborhood(String name, double lat, double lng,
                                double altitude, int population, String description) {
        int id = modele.nextZoneId();
        Zone z = new model.zone.Neighborhood(id,
            name != null ? name : "Zone-" + id,
            lat, lng, altitude, population,
            description != null ? description : "");
        addZone(z);
        return z;
    }

    /**
     * Crée un refuge avec des paramètres explicites.
     */
    public Zone addShelter(String name, double lat, double lng,
                           double altitude, int capacity, String description) {
        int id = modele.nextZoneId();
        Zone z = new model.zone.Shelter(id,
            name != null ? name : "Refuge-" + id,
            lat, lng, altitude, 0,
            description != null ? description : "",
            capacity);
        addZone(z);
        return z;
    }

    /** Quartier entièrement aléatoire (ajout en masse). */
    public Zone addRandomNeighborhood() {
        Random rng = new Random();
        return addNeighborhood(null,
            45.7640 + (rng.nextDouble() - 0.5) * 0.06,
            4.8357  + (rng.nextDouble() - 0.5) * 0.08,
            0.5 + rng.nextDouble() * 3.5,
            50  + rng.nextInt(950), "Générée auto");
    }

    /** Refuge entièrement aléatoire (ajout en masse). */
    public Zone addRandomShelter() {
        Random rng = new Random();
        return addShelter(null,
            45.7640 + (rng.nextDouble() - 0.5) * 0.06,
            4.8357  + (rng.nextDouble() - 0.5) * 0.08,
            2.0 + rng.nextDouble() * 4.0,
            100 + rng.nextInt(400), "Refuge généré");
    }

    /**
     * Déplace une zone existante vers de nouvelles coordonnées.
     * Les arêtes connectées restent dans le RouteGraph mais leurs waypoints
     * sont recalculés (fallback ligne droite si OSRM indisponible).
     *
     * Le view doit appeler cette méthode lors d'un drag-and-drop de nœud.
     */
    public void moveZone(int zoneId, double newLat, double newLng) {
        Zone zone = getZoneById(zoneId);
        if (zone == null) return;

        
        zone.setLatitude(newLat);
        zone.setLongitude(newLng);

        // Mettre à jour les waypoints de toutes les arêtes connectées
        if (mapController != null && mapController.getRouteGraph() != null) {
            RouteGraph rg = mapController.getRouteGraph();
            rg.getEdgesForZone(zone).forEach(e -> e.refreshWaypoints(zone, rg.getWaypointProvider()));
        }

        modele.updateZone(zone);
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
    }

    /**
     * Crée une arête entre deux zones.
     * Réutilise la logique de RouteGraph.ensureShelterConnectivity (ajout conditionnel).
     * Le view appelle cette méthode quand l'utilisateur clique successivement sur 2 zones.
     */
    public boolean addEdgeBetween(int zoneIdA, int zoneIdB) {
        if (zoneIdA == zoneIdB) return false;
        if (mapController == null || mapController.getRouteGraph() == null) return false;

        Zone a = getZoneById(zoneIdA);
        Zone b = getZoneById(zoneIdB);
        if (a == null || b == null) return false;

        RouteGraph rg = mapController.getRouteGraph();

        // Ne pas créer de doublon
        if (rg.hasEdgeBetween(a, b)) {
            notifyStatus("Une arête existe déjà entre " + a.getName() + " et " + b.getName());
            return false;
        }

        rg.addEdge(a, b);
        notifyStatus("Arête créée : " + a.getName() + " ↔ " + b.getName());
        if (mapController != null) mapController.syncZones(modele.getZones());
        return true;
    }

    public void removeZone(int zoneId) {
        Zone toRemove = getZoneById(zoneId);
        if (toRemove == null) return;

        List<Agent> agentsToRelocate = new ArrayList<>();
        agentsToRelocate.addAll(findAgentsStaticInZone(zoneId));
        agentsToRelocate.addAll(findAgentsTransitThroughZone(zoneId));
        agentsToRelocate = agentsToRelocate.stream().distinct().collect(Collectors.toList());

        List<Zone> adjacent = findAdjacentZones(zoneId);

        if (!agentsToRelocate.isEmpty()) {
            if (adjacent.isEmpty()) {
                agentsToRelocate.forEach(modele::removeAgent);
                notifyStatus("⚠ Agents supprimés (aucune zone adjacente disponible)");
            } else {
                Zone fallback = adjacent.get(0);
                agentsToRelocate.forEach(a -> relocateAgent(a, fallback));
                checkAndSetOvercrowding(fallback, agentsToRelocate.size());
                notifyStatus("Agents relocalisés vers " + fallback.getName());
            }
        }

        cancelMovementsThrough(zoneId);

        if (mapController != null && mapController.getRouteGraph() != null) {
            RouteGraph rg = mapController.getRouteGraph();
            new ArrayList<>(rg.getEdges()).stream()
                .filter(e -> e.getFromZone().getId() == zoneId || e.getToZone().getId() == zoneId)
                .forEach(e -> rg.removeEdge(e.getId()));
        }

        modele.removeZoneById(zoneId);

        if (mapController != null) {
            mapController.syncZones(modele.getZones());
            mapController.syncAgents(modele.getAgents());
        }

        notifyAgentsUpdated();
        Platform.runLater(() -> { if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones()); });
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
    // STATISTIQUES NŒUDS / ARÊTES
    // ─────────────────────────────────────────────────────────────────────

    public void recordAgentPassedZone(int zoneId) { zonePassCount.merge(zoneId, 1, Integer::sum); }
    public void recordAgentPassedEdge(int edgeId) { edgePassCount.merge(edgeId, 1, Integer::sum); }

    /**
     * Enregistre le temps passé par un agent dans une zone.
     * Appelé dans handleAgentArrived pour alimenter computeZoneAvgSpeed.
     * FIX : zoneTimeSpent n'était jamais alimenté → stats toujours à 0.
     */
    public void recordAgentTimeInZone(int zoneId, double seconds) {
        zoneTimeSpent.merge(zoneId, seconds, Double::sum);
    }

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

    /**
     * FIX : divisait passed/time mais zoneTimeSpent n'était jamais alimenté.
     * Maintenant zoneTimeSpent est rempli via recordAgentTimeInZone().
     */
    private double computeZoneAvgSpeed(int zoneId) {
        int    passed = zonePassCount.getOrDefault(zoneId, 0);
        double time   = zoneTimeSpent.getOrDefault(zoneId, 0.0);
        return (passed > 0 && time > 0) ? passed / time : 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HISTORIQUE ÉVACUATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Retourne l'historique complet des événements d'évacuation.
     * Le view peut s'abonner via setOnHistoryUpdated pour un tableau en temps réel.
     */
    public List<EvacuationEvent> getEvacuationHistory() {
        return modele.getEvacuationHistory();
    }

    public List<EvacuationEvent> getEvacuationHistoryFor(int agentId) {
        return modele.getEvacuationHistoryFor(agentId);
    }

    public void setOnHistoryUpdated(Consumer<List<EvacuationEvent>> cb) {
        this.onHistoryUpdated = cb;
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

    public void    setVitesseSimulation(double ms) { this.vitesseSimulationMs = Math.max(50.0, ms); }
    public double  getVitesseSimulationMs()         { return vitesseSimulationMs; }
    public void    setGravite(double g)             { modele.setGravite(g); }
    public void    setNiveauEau(double nv)          { modele.setNiveauEau(nv); Platform.runLater(() -> { if (onWaterLevelChanged != null) onWaterLevelChanged.accept(nv / FLOOD_SPEED_FACTOR); }); }
    public void    setModeAleatoire(boolean b)      { this.modeAleatoire = b; }
    public boolean isModeAleatoire()                { return modeAleatoire; }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public void setOnStatusChanged(Consumer<String>                 cb) { this.onStatusChanged             = cb; }
    public void setOnWaterLevelChanged(Consumer<Double>             cb) { this.onWaterLevelChanged         = cb; }
    public void setOnZonesUpdated(Consumer<List<Zone>>              cb) { this.onZonesUpdated              = cb; }
    public void setOnAgentsUpdated(Consumer<List<Agent>>            cb) { this.onAgentsUpdated             = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement>           cb) { this.onAgentArrived              = cb; }
    public void setOnStatsUpdated(Consumer<NodeEdgeStats>           cb) { this.onStatsUpdated              = cb; }
    public void setOnSelectedAgentPathChanged(Consumer<List<Zone>>  cb) { this.onSelectedAgentPathChanged  = cb; }

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
        public final List<Zone>   zones;
        public final List<Agent>  agents;
        public final double       niveauEau;
        public final double       gravite;
        public final boolean      modeAleatoire;

        public SimulationSnapshot(List<Zone> zones, List<Agent> agents,
                                  double niveauEau, double gravite, boolean modeAleatoire) {
            this.zones         = zones;
            this.agents        = agents;
            this.niveauEau     = niveauEau;
            this.gravite       = gravite;
            this.modeAleatoire = modeAleatoire;
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

    /**
     * FIX : alimente zoneTimeSpent pour que computeZoneAvgSpeed retourne
     * des valeurs réelles plutôt que 0.
     * On estime le temps passé dans la zone de départ = temps de trajet / nb zones.
     */
    private void handleAgentArrived(AgentMovement mv) {
        Agent agent = mv.getAgent();
        Zone  dest  = mv.getDestination();
        Zone  orig  = mv.getOriginZone();

        if (dest != null) {
            recordAgentPassedZone(dest.getId());
            // Temps estimé dans la zone de destination = 1 pas de simulation
            recordAgentTimeInZone(dest.getId(), DELTA_SECONDS);
        }
        if (orig != null) {
            recordAgentPassedZone(orig.getId());
            int nbZones = mv.getPath() != null ? Math.max(1, mv.getPath().getZones().size()) : 1;
            // Répartir le temps de trajet équitablement sur les zones traversées
            // progress = 1.0, donc totalTime ≈ 1/baseSpeed secondes (approximation)
            double estimatedTravelTime = nbZones * DELTA_SECONDS;
            recordAgentTimeInZone(orig.getId(), estimatedTravelTime / nbZones);
        }

        modele.recordEvacuationArrival(agent, dest);

        if (agent instanceof Citizen c) {
            c.setState(CitizenState.SAFE);
        } else if (agent instanceof RescueAgent ra) {
            ra.setState(RescueState.DISPONIBLE);
        }

        notifyHistoryUpdated();

        if (selectedAgent != null && selectedAgent.getId() == agent.getId())
            notifySelectedAgentPath();
    }

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
            .filter(a -> { Zone z = a.getCurrentZone(); return z != null && z.getId() == zoneId; })
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

    private model.graph.Node zoneToNode(Zone zone) {
        return new model.graph.Node(zone.getLatitude(), zone.getLongitude());
    }

    private static final String[] FIRSTNAMES =
        {"Alice","Bob","Clara","David","Emma","Félix","Gina","Hugo","Inès","Jules"};
    private static final String[] LASTNAMES  =
        {"Martin","Bernard","Dubois","Thomas","Robert","Petit","Durand","Leroy","Moreau","Simon"};
    private String randomFirstName(Random rng) { return FIRSTNAMES[rng.nextInt(FIRSTNAMES.length)]; }
    private String randomLastName(Random rng)  { return LASTNAMES[rng.nextInt(LASTNAMES.length)]; }

    private void notifySelectedAgentPath() {
        if (onSelectedAgentPathChanged == null) return;
        List<Zone> path = getSelectedAgentRemainingPath();
        Platform.runLater(() -> onSelectedAgentPathChanged.accept(path));
    }

    private void notifyHistoryUpdated() {
        if (onHistoryUpdated != null) {
            List<EvacuationEvent> history = modele.getEvacuationHistory();
            Platform.runLater(() -> onHistoryUpdated.accept(history));
        }
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