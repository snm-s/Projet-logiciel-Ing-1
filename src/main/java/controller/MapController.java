package controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import app.Main;
import javafx.application.Platform;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.algorithms.EvacuationPath;
import model.enums.CitizenState;
import model.graph.AgentMovement;
import model.graph.Edge;
import model.graph.RouteGraph;
import model.zone.Shelter;
import model.zone.Zone;
import view.MapView;

/**
 * Contrôleur de la carte : relie {@link MapView}, {@link RouteGraph} et les agents.
 *
 * <h3>Nouveautés</h3>
 * <ul>
 *   <li>{@link #syncAgents(List)} — remplace la liste live d'agents et rafraîchit la MapView.</li>
 *   <li>{@link #syncZones(List)} — remplace la liste live de zones, reconstruit le graphe.</li>
 *   <li>{@link #addAgent(Agent)} / {@link #removeAgent(int)} — mutations atomiques avec repaint.</li>
 *   <li>La liste interne {@code agents} est mutable et partagée avec la MapView, ce qui garantit
 *       que tout ce qui est affiché est également accessible aux {@link Citizen} et
 *       {@link RescueAgent} qui interrogent le graphe.</li>
 * </ul>
 */
public class MapController {

    private final MapView        mapView;
    /** Liste mutable partagée entre MapController, MapView et RouteGraph. */
    private final List<Agent>    agents;
    /** Liste mutable de zones — peut être rechargée lors d'un reset. */
    private List<Zone>           zones;
    private RouteGraph           routeGraph;

    private Consumer<Zone>          onZoneSelected;
    private Consumer<AgentMovement> onAgentArrived;
    private Zone                    selectedZone;

    // Missions de secours : rescueAgentId -> citoyens bloqués pris en charge
    private final Map<Integer, List<Citizen>> rescueAssignments = new HashMap<>();
    private final Set<Integer> busyRescueAgents = new HashSet<>();

    // ─────────────────────────────────────────────────────────────────────
    /**
     * Constructs a new MapController.
     * @param mapView the mapView.
     * @param zones the zones.
     * @param agents the agents.
     */
    public MapController(MapView mapView, List<Zone> zones, List<Agent> agents) {
        this.mapView   = mapView;
        this.zones     = new ArrayList<>(zones  != null ? zones  : List.of());
        this.agents    = new ArrayList<>(agents != null ? agents : List.of());

        this.routeGraph = new RouteGraph(this.zones);
        initMapView();
    }

    /**
     * Create a MapController linking a MapView with zones and agents.
     *
     * @param mapView the view used to render the map
     * @param zones initial list of zones
     * @param agents initial list of agents
     */

    public MapController(MapView mapView, List<Zone> zones) {
        this(mapView, zones, List.of());
    }

    // ─────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Initializes map view.
     */
    private void initMapView() {
        mapView.setRouteGraph(routeGraph);
        mapView.setMapController(this);
        mapView.setAgents(agents);           // injection de la liste LIVE

        mapView.setOnZoneSelected(zone -> {
            this.selectedZone = zone;
            if (onZoneSelected != null) Platform.runLater(() -> onZoneSelected.accept(zone));
        });

        routeGraph.addArrivalListener(new RouteGraph.ArrivalListener() {
            @Override public void onAgentArrived(AgentMovement mv) { handleAgentArrived(mv); }
            @Override public void onAgentBlocked(AgentMovement mv) { handleAgentBlocked(mv); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // SYNCHRONISATION (appelée par SimulationController)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Performs route graph.
     * @param newZones the newZones.
     */
    public void rebuildRouteGraph(List<Zone> newZones) {
        if (newZones == null) return;

        // 1. MAJ zones internes
        this.zones = new ArrayList<>(newZones);

        // 2. Rebuild complet du graphe
        this.routeGraph = new RouteGraph(this.zones);

        // 3. Rebrancher la view
        mapView.setRouteGraph(routeGraph);

        // 4. Réattacher listeners (OBLIGATOIRE)
        routeGraph.addArrivalListener(new RouteGraph.ArrivalListener() {
            @Override
            /**
             * Performs agent arrived.
             * @param mv the mv.
             */
            public void onAgentArrived(AgentMovement mv) {
                handleAgentArrived(mv);
            }

            @Override
            /**
             * Performs agent blocked.
             * @param mv the mv.
             */
            public void onAgentBlocked(AgentMovement mv) {
                handleAgentBlocked(mv);
            }
        });

        // 5. refresh visuel global
        mapView.updateAllZones(this.zones);
        mapView.refreshRouteColors();
    }

    /**
     * Rebuild the underlying RouteGraph using a new set of zones and refresh the view.
     *
     * @param newZones new list of zones to use
     */


    /**
     * Remplace la liste d'agents par la liste fournie (reset ou ajout en masse).
     * Met à jour MapView et réancre les agents sur le graphe.
     */
    public void syncAgents(List<Agent> newAgents) {
        agents.clear();
        if (newAgents != null) agents.addAll(newAgents);
        mapView.setAgents(agents);          // MapView utilise la même liste désormais
        mapView.refreshRouteColors();
    }

    /**
     * Replace the controller's agent list with the provided list and refresh the view.
     *
     * @param newAgents the new list of agents
     */

    /**
     * Remplace la liste de zones et reconstruit le {@link RouteGraph}.
     * Utilisé lors d'un reset ou d'un ajout/suppression de zone.
     */
    public void syncZones(List<Zone> newZones) {
        if (newZones == null) return;

        this.zones.clear();
        this.zones.addAll(newZones);

        mapView.updateAllZones(this.zones);
        mapView.refreshRouteColors();
    }

    /**
     * Replace the internal list of zones and refresh the view.
     *
     * @param newZones list of new zones
     */

    /**
     * Ajoute un agent au graphe et à la MapView (sans reconstruire toute la liste).
     */
    public void addAgent(Agent agent) {
        if (agent == null || agents.contains(agent)) return;
        agents.add(agent);
        mapView.addAgent(agent);            // repaint + snap au graphe

        // Si la simulation/alerte est déjà lancée, un citoyen ajouté doit aussi partir vers un refuge.
        if (agent instanceof Citizen c && Main.getSharedSimulation().hasSimulationStartAlertBeenPublished()) {
            evacuateCitizen(c);
        }
    }

    /**
     * Add an agent to the map and graph, triggering a repaint.
     *
     * @param agent the agent to add
     */

    /**
     * Supprime un agent par son identifiant.
     */
    public boolean removeAgent(int agentId) {
        boolean removed = agents.removeIf(a -> a.getId() == agentId);
    
        if (routeGraph != null) {
            routeGraph.removeMovementsOfAgent(agentId);
        }
    
        mapView.setAgents(agents);
        mapView.refreshRouteColors();
    
        return removed;
    }

    /**
     * Remove an agent by id from the controller and update the view.
     *
     * @param agentId id of the agent to remove
     * @return true if an agent was removed
     */

    // ─────────────────────────────────────────────────────────────────────
    // TICK
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Performs tick.
     * @param deltaSeconds the deltaSeconds.
     */
    public void tick(double deltaSeconds) {
        routeGraph.tick(deltaSeconds);
        routeGraph.simulateFlows();
        routeGraph.refreshAllEdges();
        dispatchRescueForBlockedCitizens();
        mapView.refreshRouteColors();
    }

    /**
     * Advance the controller by one simulation tick (forward route graph and update view).
     *
     * @param deltaSeconds tick duration in seconds
     */

    // ─────────────────────────────────────────────────────────────────────
    // ÉVACUATION
    // ─────────────────────────────────────────────────────────────────────

    /** Évacue un citoyen depuis sa zone la plus proche vers le refuge le plus proche. */
    public AgentMovement evacuateCitizen(Citizen citizen) {
        if (citizen == null) return null;
        Zone from = findClosestZone(citizen);
        return evacuateCitizen(citizen, from);
    }

    /**
     * Evacuate a citizen from their nearest zone to the nearest shelter.
     *
     * @param citizen the citizen to evacuate
     * @return created AgentMovement or null
     */

    public AgentMovement evacuateCitizen(Citizen citizen, Zone from) {
        if (citizen == null || from == null) return null;
        if (citizen.getState() == CitizenState.SAFE ||
            citizen.getState() == CitizenState.ESCAPING) return null;

        if (citizen.isMobilityReduced()) {
            if (dispatchRescueForCitizen(citizen)) {
                mapView.setGraphInfo("PMR : " + nameOf(citizen) + " pris en charge par un secouriste.");
            } else {
                mapView.setGraphInfo("PMR : " + nameOf(citizen) + " attend un secouriste disponible.");
            }
            return null;
        }

        // Keep the semantic location in sync before routing.
        citizen.setCurrentZone(from);

        AgentMovement mv = routeGraph.planEvacuation(citizen, from, zones);
        if (mv != null) {
            citizen.setState(CitizenState.ESCAPING);
            if (mv.getDestinationZone() != null) {
                Main.getSharedSimulation().recordEvacuationDeparture(citizen, from, mv.getDestinationZone());
            }
            mapView.refreshRouteColors();
        }
        return mv;
    }

    /**
     * Evacuate a citizen starting from a specific zone.
     *
     * @param citizen the citizen to evacuate
     * @param from the starting zone
     * @return created AgentMovement or null
     */

    public AgentMovement sendRescueAgent(RescueAgent agent, Zone from, Zone target) {
        if (agent == null || from == null || target == null) return null;
        AgentMovement mv = routeGraph.planRescueMission(agent, from, target);
        if (mv != null) mapView.refreshRouteColors();
        return mv;
    }

    /**
     * Send a rescue agent on a mission from one zone to a target zone.
     *
     * @param agent rescue agent
     * @param from origin zone
     * @param target destination zone
     * @return created AgentMovement or null
     */

    /**
     * Évacuation de masse : chaque citoyen reçoit un chemin Dijkstra vers un refuge.
     */
    public void triggerMassEvacuation(List<Citizen> citizens) {
        if (citizens == null) return;
        int planned = 0;
        List<Citizen> ordered = new ArrayList<>(citizens);
        ordered.sort((a, b) -> {
            boolean aPriority = a != null && a.isMobilityReduced();
            boolean bPriority = b != null && b.isMobilityReduced();
            if (aPriority != bPriority) return aPriority ? -1 : 1;
            return 0;
        });

        for (Citizen c : ordered) {
            if (c == null) continue;
            if (evacuateCitizen(c) != null) planned++;
        }
        mapView.setGraphInfo("Évacuation → refuges : " + planned + " citoyen(s) en route (Dijkstra).");
    }

    /**
     * Trigger mass evacuation for a list of citizens (plans routes for each).
     *
     * @param citizens list of citizens to evacuate
     */

    /** Évacue tous les citoyens de la liste interne vers les refuges. */
    public void evacuateAllCitizensToShelters() {
        List<Citizen> citizens = agents.stream()
                .filter(a -> a instanceof Citizen)
                .map(a -> (Citizen) a)
                .toList();
        triggerMassEvacuation(citizens);
    }

    /**
     * Evacuate all citizens currently known to the controller to shelters.
     */

    public EvacuationPath computeEvacuationPreview(Zone from) {
        if (from == null) return null;
        List<Zone> shelters = zones.stream()
                .filter(z -> z instanceof Shelter && !z.isFlooded())
                .toList();
        return routeGraph.getRouter().findNearestSafe(from, shelters);
    }

    /**
     * Compute a preview evacuation path from a given zone to the nearest safe shelter.
     *
     * @param from origin zone
     * @return EvacuationPath preview or null
     */

    // ─────────────────────────────────────────────────────────────────────
    // HANDLERS D'ÉVÉNEMENTS GRAPHE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Handles agent arrived.
     * @param mv the mv.
     */
    private void handleAgentArrived(AgentMovement mv) {
        Agent agent = mv.getAgent();
        Zone destination = mv.getDestinationZone();

        if (agent != null && destination != null) {
            agent.setCurrentZone(destination);
        }

        if (agent instanceof Citizen c && destination instanceof Shelter) {
    c.setState(CitizenState.SAFE);
    Main.getSharedSimulation().recordEvacuationArrival(agent, destination);
}

        if (agent instanceof Citizen c && destination instanceof Shelter) {
            c.setState(CitizenState.SAFE);
            Main.getSharedSimulation().recordEvacuationArrival(agent, destination);
        }

        // Un secouriste arrivé sur une zone à risque récupère les citoyens assignés
        // puis les amène virtuellement au refuge accessible le plus proche.
        if (agent instanceof RescueAgent rescue && rescueAssignments.containsKey(rescue.getId())) {
            List<Citizen> rescued = rescueAssignments.remove(rescue.getId());
            busyRescueAgents.remove(rescue.getId());
            Zone refuge = nearestShelter(destination);

            if (refuge != null) {
                for (Citizen c : rescued) {
                    c.setState(CitizenState.SAFE);
                    c.setPosition(new model.graph.Node(refuge.getLatitude(), refuge.getLongitude()));
                    Main.getSharedSimulation().recordEvacuationArrival(c, refuge);
                }
                mapView.setGraphInfo("Secours : " + nameOf(rescue) + " a transféré " + rescued.size()
                        + " citoyen(s) vers " + refuge.getName() + ".");
                // trajet retour du secouriste vers le refuge pour visualiser l'intervention
                if (destination != null) sendRescueAgent(rescue, destination, refuge);
            }
        }

        if (onAgentArrived != null) Platform.runLater(() -> onAgentArrived.accept(mv));

        String destName = destination == null ? "destination" : destination.getName();
        mapView.setGraphInfo("Arrivé : " + nameOf(agent) + " → " + destName);
        mapView.refreshRouteColors();
    }

    /**
     * Handles agent blocked.
     * @param mv the mv.
     */
    private void handleAgentBlocked(AgentMovement mv) {
        Agent a = mv.getAgent();
        if (a instanceof Citizen c) {
            c.setState(CitizenState.STRESSED);
            Main.getSharedSimulation().recordEvacuationBlocked(c, findClosestZone(c));
            dispatchRescueForCitizen(c);
        }
        mapView.setGraphInfo("Bloqué : " + nameOf(a) + " — secours demandé.");
        mapView.refreshRouteColors();
    }

    // ─────────────────────────────────────────────────────────────────────
    // SECOURS AUTOMATIQUE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Performs rescue for blocked citizens.
     */
    private void dispatchRescueForBlockedCitizens() {
        List<Citizen> priorityFirst = new ArrayList<>();
        List<Citizen> others = new ArrayList<>();

        for (Agent a : new ArrayList<>(agents)) {
            if (!(a instanceof Citizen c)) continue;
            if (c.isMobilityReduced()) priorityFirst.add(c);
            else if (c.getState() == CitizenState.STRESSED) others.add(c);
        }

        priorityFirst.forEach(this::dispatchRescueForCitizen);
        others.forEach(this::dispatchRescueForCitizen);
    }

    /**
     * Performs rescue for citizen.
     * @param citizen the citizen.
     * @return the boolean result.
     */
    private boolean dispatchRescueForCitizen(Citizen citizen) {
        if (citizen == null || citizen.getState() == CitizenState.SAFE) return false;

        // déjà assigné à une mission ?
        for (List<Citizen> list : rescueAssignments.values()) {
            if (list.stream().anyMatch(c -> c.getId() == citizen.getId())) return true;
        }

        RescueAgent rescue = nearestAvailableRescue(citizen);
        if (rescue == null) {
            mapView.setGraphInfo("Aucun secouriste disponible pour " + nameOf(citizen));
            return false;
        }

        Zone from = findClosestZone(rescue);
        Zone target = findClosestZone(citizen);
        if (from == null || target == null) return false;

        rescue.setState(model.enums.RescueState.EN_ROUTE);
        AgentMovement mission = sendRescueAgent(rescue, from, target);
        if (mission != null) {
            busyRescueAgents.add(rescue.getId());
            List<Citizen> group = rescueAssignments.computeIfAbsent(rescue.getId(), id -> new ArrayList<>());
            group.add(citizen);
            mapView.setGraphInfo("Secours envoyé : " + nameOf(rescue) + " vers " + target.getName());
            return true;
        }
        rescue.setState(model.enums.RescueState.DISPONIBLE);
        return false;
    }

    /**
     * Performs available rescue.
     * @param target the target.
     * @return the RescueAgent.
     */
    private RescueAgent nearestAvailableRescue(Agent target) {
        RescueAgent best = null;
        double bestDist = Double.MAX_VALUE;
        Zone targetZone = findClosestZone(target);
        if (targetZone == null) return null;

        for (Agent a : agents) {
            if (!(a instanceof RescueAgent r)) continue;
            if (r.getState() != model.enums.RescueState.DISPONIBLE) continue;
            if (busyRescueAgents.contains(r.getId())) continue;
            Zone z = findClosestZone(r);
            if (z == null) continue;
            double d = Math.pow(z.getLatitude() - targetZone.getLatitude(), 2)
                    + Math.pow(z.getLongitude() - targetZone.getLongitude(), 2);
            if (d < bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }

    /**
     * Performs shelter.
     * @param from the from.
     * @return the Zone.
     */
    private Zone nearestShelter(Zone from) {
        if (from == null) return zones.stream().filter(z -> z instanceof Shelter).findFirst().orElse(null);
        return zones.stream()
                .filter(z -> z instanceof Shelter && !z.isFlooded())
                .min((a, b) -> Double.compare(
                        Math.pow(a.getLatitude() - from.getLatitude(), 2) + Math.pow(a.getLongitude() - from.getLongitude(), 2),
                        Math.pow(b.getLatitude() - from.getLatitude(), 2) + Math.pow(b.getLongitude() - from.getLongitude(), 2)))
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Finds closest zone.
     * @param agent the agent.
     * @return the Zone.
     */
    private Zone findClosestZone(Agent agent) {
        if (agent == null || zones.isEmpty()) {
            return null;
        }

        if (agent.getPosition() != null) {
            double lat = agent.getPosition().getLat();
            double lng = agent.getPosition().getLng();
            Zone best = null;
            double bestDist = Double.MAX_VALUE;
            for (Zone z : zones) {
                double d = Math.pow(z.getLatitude() - lat, 2) + Math.pow(z.getLongitude() - lng, 2);
                if (d < bestDist) { bestDist = d; best = z; }
            }
            if (best != null) {
                return best;
            }
        }

        Zone currentZone = agent.getCurrentZone();
        if (currentZone != null) {
            for (Zone z : zones) {
                if (z.getId() == currentZone.getId()) {
                    return z;
                }
            }
        }

        if (agent.getPosition() == null) {
            return null;
        }
        return null;
    }

    /**
     * Performs of.
     * @param a the a.
     * @return the String.
     */
    private String nameOf(Agent a) {
        if (a == null) return "Agent";
        String n = ((a.getFirstName() == null ? "" : a.getFirstName())
                  + " "
                  + (a.getLastName()  == null ? "" : a.getLastName())).trim();
        return n.isBlank() ? "Agent #" + a.getId() : n;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Counts the citizens at risk.
     * @return the long result.
     */
    public long countCitizensAtRisk() {
        return agents.stream()
            .filter(a -> a instanceof Citizen c && c.getState() != CitizenState.SAFE).count();
    }

    /**
     * Counts the citizens safe.
     * @return the long result.
     */
    public long countCitizensSafe() {
        return agents.stream()
            .filter(a -> a instanceof Citizen c && c.getState() == CitizenState.SAFE).count();
    }

    /**
     * Counts the citizens escaping.
     * @return the long result.
     */
    public long countCitizensEscaping() {
        return agents.stream()
            .filter(a -> a instanceof Citizen c && c.getState() == CitizenState.ESCAPING).count();
    }

    /**
     * Counts the shelters total.
     * @return the long result.
     */
    public long countSheltersTotal()      { return zones.stream().filter(z -> z instanceof Shelter).count(); }
    /**
     * Counts the shelters accessible.
     * @return the long result.
     */
    public long countSheltersAccessible() { return zones.stream().filter(z -> z instanceof Shelter && !z.isFlooded()).count(); }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION / VUE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Performs in.
     */
    public void zoomIn()                   { mapView.zoomIn(); }
    /**
     * Performs out.
     */
    public void zoomOut()                  { mapView.zoomOut(); }
    /**
     * Sets the zoom.
     * @param level the level.
     */
    public void setZoom(int level)         { mapView.setZoom(level); }
    /**
     * Resets view.
     */
    public void resetView()                { mapView.resetView(); }
    /**
     * Performs to.
     * @param lat the lat.
     * @param lng the lng.
     */
    public void flyTo(double lat, double lng) { mapView.flyTo(lat, lng); }
    /**
     * Performs to.
     * @param lat the lat.
     * @param lng the lng.
     */
    public void panTo(double lat, double lng) { mapView.panTo(lat, lng); }
    /**
     * Performs zone.
     * @param id the id.
     */
    public void highlightZone(int id)      { mapView.highlightZone(id); }
    /**
     * Performs zone.
     * @param zone the zone.
     */
    public void focusZone(Zone zone)       { if (zone != null) { mapView.focusZone(zone); selectZone(zone); } }
    /**
     * Performs zone by id.
     * @param id the id.
     */
    public void focusZoneById(int id)      { zones.stream().filter(z -> z.getId() == id).findFirst().ifPresent(this::focusZone); }
    /**
     * Performs zone.
     * @param zone the zone.
     */
    public void selectZone(Zone zone)      { if (zone != null) { selectedZone = zone; mapView.selectZone(zone); } }
    /**
     * Returns the selected zone.
     * @return the Zone.
     */
    public Zone getSelectedZone()          { return selectedZone; }

    /**
     * Updates zone.
     * @param zone the zone.
     * @param niveauEau the niveauEau.
     */
    public void updateZone(Zone zone, double niveauEau) { mapView.updateZoneWithWaterLevel(zone, niveauEau); }
    /**
     * Updates all zones.
     * @param updatedZones the updatedZones.
     */
    public void updateAllZones(List<Zone> updatedZones) { mapView.updateAllZones(updatedZones); routeGraph.refreshAllEdges(); }

    /**
     * Performs routes.
     * @param routes the routes.
     * @param zones the zones.
     */
    public void drawRoutes(List<int[]> routes, List<Zone> zones) { mapView.refreshRouteColors(); }

    /**
     * Sets the on zone selected.
     * @param cb the cb.
     */
    public void setOnZoneSelected(Consumer<Zone> cb)         { this.onZoneSelected = cb; }
    /**
     * Sets the on agent arrived.
     * @param cb the cb.
     */
    public void setOnAgentArrived(Consumer<AgentMovement> cb){ this.onAgentArrived = cb; }

    /**
     * Returns the route graph.
     * @return the RouteGraph.
     */
    public RouteGraph  getRouteGraph() { return routeGraph; }
    /**
     * Returns the zones.
     * @return the List<Zone>.
     */
    public List<Zone>  getZones()      { return zones; }
    /**
     * Returns the agents.
     * @return the List<Agent>.
     */
    public List<Agent> getAgents()     { return agents; }
    /**
     * Returns the edges.
     * @return the List<Edge>.
     */
    public List<Edge>  getEdges()      { return routeGraph.getEdges(); }
    /**
     * Returns the map view.
     * @return the MapView.
     */
    public MapView getMapView() { return mapView; }
}
