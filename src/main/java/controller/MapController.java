package controller;

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

import javafx.application.Platform;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller de la carte : relie MapView, RouteGraph et déplacements d'agents.
 * Version claire : les citoyens vont vers des vraies zones Shelter uniquement.
 */
public class MapController {

    private final MapView mapView;
    private final List<Zone> zones;
    private final List<Agent> agents;
    private final RouteGraph routeGraph;

    private Consumer<Zone> onZoneSelected;
    private Consumer<AgentMovement> onAgentArrived;
    private Zone selectedZone;

    public MapController(MapView mapView, List<Zone> zones, List<Agent> agents) {
        this.mapView = mapView;
        this.zones = zones;
        this.agents = agents;
        this.routeGraph = new RouteGraph(zones);

        mapView.setRouteGraph(routeGraph);
        mapView.setMapController(this);
        mapView.setOnZoneSelected(zone -> {
            this.selectedZone = zone;
            if (onZoneSelected != null) Platform.runLater(() -> onZoneSelected.accept(zone));
        });

        routeGraph.addArrivalListener(new RouteGraph.ArrivalListener() {
            @Override public void onAgentArrived(AgentMovement mv) { handleAgentArrived(mv); }
            @Override public void onAgentBlocked(AgentMovement mv) { handleAgentBlocked(mv); }
        });
    }

    public MapController(MapView mapView, List<Zone> zones) {
        this(mapView, zones, List.of());
    }

    public void tick(double deltaSeconds) {
        routeGraph.tick(deltaSeconds);
        routeGraph.simulateFlows();
        routeGraph.refreshAllEdges();
        mapView.refreshRouteColors();
    }

    /** Évacue un citoyen depuis sa zone la plus proche vers le refuge le plus proche via Dijkstra. */
    public AgentMovement evacuateCitizen(Citizen citizen) {
        if (citizen == null) return null;
        Zone from = findClosestZone(citizen);
        return evacuateCitizen(citizen, from);
    }

    public AgentMovement evacuateCitizen(Citizen citizen, Zone from) {
        if (citizen == null || from == null) return null;
        if (citizen.getState() == CitizenState.SAFE || citizen.getState() == CitizenState.ESCAPING) return null;

        AgentMovement mv = routeGraph.planEvacuation(citizen, from, zones);
        if (mv != null) {
            citizen.setState(CitizenState.ESCAPING);
            mapView.refreshRouteColors();
        }
        return mv;
    }

    public AgentMovement sendRescueAgent(RescueAgent agent, Zone from, Zone target) {
        if (agent == null || from == null || target == null) return null;
        AgentMovement mv = routeGraph.planRescueMission(agent, from, target);
        if (mv != null) mapView.refreshRouteColors();
        return mv;
    }

    /**
     * Évacuation de masse claire : chaque citoyen part de SA zone la plus proche
     * et reçoit un chemin vers un Shelter. Pas de déplacement aléatoire.
     */
    public void triggerMassEvacuation(List<Citizen> citizens) {
        if (citizens == null) return;
        int planned = 0;
        for (Citizen c : citizens) {
            if (c == null) continue;
            AgentMovement mv = evacuateCitizen(c);
            if (mv != null) planned++;
        }
        mapView.setGraphInfo("Évacuation vers refuges : " + planned + " citoyen(s) ont reçu un chemin Dijkstra.");
    }

    /** Bouton manuel : évacuer tous les citoyens visibles vers les refuges. */
    public void evacuateAllCitizensToShelters() {
        List<Citizen> citizens = agents.stream()
                .filter(a -> a instanceof Citizen)
                .map(a -> (Citizen) a)
                .toList();
        triggerMassEvacuation(citizens);
    }

    public EvacuationPath computeEvacuationPreview(Zone from) {
        if (from == null) return null;
        List<Zone> shelters = zones.stream()
                .filter(z -> z instanceof Shelter)
                .filter(z -> !z.isFlooded())
                .toList();
        return routeGraph.getRouter().findNearestSafe(from, shelters);
    }

    private void handleAgentArrived(AgentMovement mv) {
        Agent agent = mv.getAgent();
        if (agent instanceof Citizen c) c.setState(CitizenState.SAFE);
        if (onAgentArrived != null) Platform.runLater(() -> onAgentArrived.accept(mv));
        mapView.setGraphInfo("Arrivé au refuge : " + nameOf(agent) + " → " + mv.getDestinationZone().getName());
        mapView.refreshRouteColors();
    }

    private void handleAgentBlocked(AgentMovement mv) {
        mapView.setGraphInfo("Agent bloqué : " + nameOf(mv.getAgent()) + " — replanification demandée.");
        mapView.refreshRouteColors();
    }

    private Zone findClosestZone(Agent agent) {
        if (agent == null || agent.getPosition() == null || zones.isEmpty()) return zones.isEmpty() ? null : zones.get(0);
        double lat = agent.getPosition().getLat();
        double lng = agent.getPosition().getLng();
        Zone best = null;
        double bestDist = Double.MAX_VALUE;
        for (Zone z : zones) {
            double d = Math.pow(z.getLatitude() - lat, 2) + Math.pow(z.getLongitude() - lng, 2);
            if (d < bestDist) { bestDist = d; best = z; }
        }
        return best;
    }

    private String nameOf(Agent a) {
        if (a == null) return "Agent";
        String n = ((a.getFirstName() == null ? "" : a.getFirstName()) + " " + (a.getLastName() == null ? "" : a.getLastName())).trim();
        return n.isBlank() ? "Agent #" + a.getId() : n;
    }

    public long countCitizensAtRisk() {
        return agents.stream().filter(a -> a instanceof Citizen c && c.getState() != CitizenState.SAFE).count();
    }

    public long countCitizensSafe() {
        return agents.stream().filter(a -> a instanceof Citizen c && c.getState() == CitizenState.SAFE).count();
    }

    public long countCitizensEscaping() {
        return agents.stream().filter(a -> a instanceof Citizen c && c.getState() == CitizenState.ESCAPING).count();
    }

    public long countSheltersTotal() { return zones.stream().filter(z -> z instanceof Shelter).count(); }
    public long countSheltersAccessible() { return zones.stream().filter(z -> z instanceof Shelter && !z.isFlooded()).count(); }

    public void zoomIn() { mapView.zoomIn(); }
    public void zoomOut() { mapView.zoomOut(); }
    public void setZoom(int level) { mapView.setZoom(level); }
    public void resetView() { mapView.resetView(); }
    public void flyTo(double lat, double lng) { mapView.flyTo(lat, lng); }
    public void panTo(double lat, double lng) { mapView.panTo(lat, lng); }
    public void highlightZone(int id) { mapView.highlightZone(id); }
    public void focusZone(Zone zone) { if (zone != null) { mapView.focusZone(zone); selectZone(zone); } }
    public void focusZoneById(int id) { zones.stream().filter(z -> z.getId() == id).findFirst().ifPresent(this::focusZone); }
    public void selectZone(Zone zone) { if (zone != null) { selectedZone = zone; mapView.selectZone(zone); } }
    public Zone getSelectedZone() { return selectedZone; }
    public void updateZone(Zone zone, double niveauEau) { mapView.updateZoneWithWaterLevel(zone, niveauEau); }
    public void updateAllZones(List<Zone> updatedZones) { mapView.updateAllZones(updatedZones); routeGraph.refreshAllEdges(); }
    public void drawRoutes(List<int[]> routes, List<Zone> zones) { mapView.refreshRouteColors(); }
    public void setOnZoneSelected(Consumer<Zone> cb) { this.onZoneSelected = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement> cb) { this.onAgentArrived = cb; }
    public RouteGraph getRouteGraph() { return routeGraph; }
    public List<Zone> getZones() { return zones; }
    public List<Edge> getEdges() { return routeGraph.getEdges(); }
}
