package controller;

import java.util.ArrayList;
import java.util.List;
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
    private boolean evacuationStarted = false;

    // ─────────────────────────────────────────────────────────────────────
    public MapController(MapView mapView, List<Zone> zones, List<Agent> agents) {
        this.mapView   = mapView;
        this.zones     = new ArrayList<>(zones  != null ? zones  : List.of());
        this.agents    = new ArrayList<>(agents != null ? agents : List.of());

        this.routeGraph = new RouteGraph(this.zones);
        initMapView();
    }

    public MapController(MapView mapView, List<Zone> zones) {
        this(mapView, zones, List.of());
    }

    // ─────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────

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
     * Remplace la liste de zones et reconstruit le {@link RouteGraph}.
     * Utilisé lors d'un reset ou d'un ajout/suppression de zone.
     */
    public void syncZones(List<Zone> newZones) {
        zones.clear();
        if (newZones != null) zones.addAll(newZones);

        // Reconstruire le graphe avec les nouvelles zones
        routeGraph = new RouteGraph(zones);
        mapView.setRouteGraph(routeGraph);

        // Réenregistrer l'ArrivalListener sur le nouveau graphe
        routeGraph.addArrivalListener(new RouteGraph.ArrivalListener() {
            @Override public void onAgentArrived(AgentMovement mv) { handleAgentArrived(mv); }
            @Override public void onAgentBlocked(AgentMovement mv) { handleAgentBlocked(mv); }
        });

        mapView.updateAllZones(zones);
        mapView.refreshRouteColors();
    }

    /**
     * Ajoute un agent au graphe et à la MapView (sans reconstruire toute la liste).
     */
    public void addAgent(Agent agent) {
        if (agent == null || agents.contains(agent)) return;
        agents.add(agent);
        mapView.addAgent(agent);            // repaint + snap au graphe

        // Si une alerte/évacuation est déjà lancée, tout citoyen ajouté part aussi vers son refuge.
        if (evacuationStarted && agent instanceof Citizen citizen) {
            evacuateCitizen(citizen);
        }
    }

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

    // ─────────────────────────────────────────────────────────────────────
    // TICK
    // ─────────────────────────────────────────────────────────────────────

    public void tick(double deltaSeconds) {
        routeGraph.tick(deltaSeconds);
        routeGraph.simulateFlows();
        routeGraph.refreshAllEdges();
        mapView.refreshRouteColors();
    }

    // ─────────────────────────────────────────────────────────────────────
    // ÉVACUATION
    // ─────────────────────────────────────────────────────────────────────

    /** Évacue un citoyen depuis sa zone la plus proche vers le refuge le plus proche. */
    public AgentMovement evacuateCitizen(Citizen citizen) {
        if (citizen == null) return null;
        Zone from = findClosestZone(citizen);
        return evacuateCitizen(citizen, from);
    }

    public AgentMovement evacuateCitizen(Citizen citizen, Zone from) {
        if (citizen == null || from == null) return null;
        if (citizen.getState() == CitizenState.SAFE ||
            citizen.getState() == CitizenState.ESCAPING) return null;

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

    public AgentMovement sendRescueAgent(RescueAgent agent, Zone from, Zone target) {
        if (agent == null || from == null || target == null) return null;
        AgentMovement mv = routeGraph.planRescueMission(agent, from, target);
        if (mv != null) mapView.refreshRouteColors();
        return mv;
    }

    /**
     * Évacuation de masse : chaque citoyen reçoit un chemin Dijkstra vers un refuge.
     */
    public void triggerMassEvacuation(List<Citizen> citizens) {
        evacuationStarted = true;
        if (citizens == null) return;
        int planned = 0;
        for (Citizen c : citizens) {
            if (c == null) continue;
            if (evacuateCitizen(c) != null) planned++;
        }
        mapView.setGraphInfo("Évacuation → refuges : " + planned + " citoyen(s) en route (Dijkstra).");
    }

    /** Évacue tous les citoyens de la liste interne vers les refuges. */
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
                .filter(z -> z instanceof Shelter && !z.isFlooded())
                .toList();
        return routeGraph.getRouter().findNearestSafe(from, shelters);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HANDLERS D'ÉVÉNEMENTS GRAPHE
    // ─────────────────────────────────────────────────────────────────────

    private void handleAgentArrived(AgentMovement mv) {
        Agent agent = mv.getAgent();
        Zone destination = mv.getDestinationZone();

        if (agent instanceof Citizen c && destination instanceof Shelter) {
            c.setState(CitizenState.SAFE);
            Main.getSharedSimulation().recordEvacuationArrival(agent, destination);
        }

        if (onAgentArrived != null) Platform.runLater(() -> onAgentArrived.accept(mv));

        String destName = destination == null ? "destination" : destination.getName();
        mapView.setGraphInfo("Arrivé : " + nameOf(agent) + " → " + destName);
        mapView.refreshRouteColors();
    }

    private void handleAgentBlocked(AgentMovement mv) {
        mapView.setGraphInfo("Bloqué : " + nameOf(mv.getAgent()) + " — replanification demandée.");
        mapView.refreshRouteColors();
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private Zone findClosestZone(Agent agent) {
        if (agent == null || agent.getPosition() == null || zones.isEmpty()) {
            return zones.isEmpty() ? null : zones.get(0);
        }
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
        String n = ((a.getFirstName() == null ? "" : a.getFirstName())
                  + " "
                  + (a.getLastName()  == null ? "" : a.getLastName())).trim();
        return n.isBlank() ? "Agent #" + a.getId() : n;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────

    public long countCitizensAtRisk() {
        return agents.stream()
            .filter(a -> a instanceof Citizen c && c.getState() != CitizenState.SAFE).count();
    }

    public long countCitizensSafe() {
        return agents.stream()
            .filter(a -> a instanceof Citizen c && c.getState() == CitizenState.SAFE).count();
    }

    public long countCitizensEscaping() {
        return agents.stream()
            .filter(a -> a instanceof Citizen c && c.getState() == CitizenState.ESCAPING).count();
    }

    public long countSheltersTotal()      { return zones.stream().filter(z -> z instanceof Shelter).count(); }
    public long countSheltersAccessible() { return zones.stream().filter(z -> z instanceof Shelter && !z.isFlooded()).count(); }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION / VUE
    // ─────────────────────────────────────────────────────────────────────

    public void zoomIn()                   { mapView.zoomIn(); }
    public void zoomOut()                  { mapView.zoomOut(); }
    public void setZoom(int level)         { mapView.setZoom(level); }
    public void resetView()                { mapView.resetView(); }
    public void flyTo(double lat, double lng) { mapView.flyTo(lat, lng); }
    public void panTo(double lat, double lng) { mapView.panTo(lat, lng); }
    public void highlightZone(int id)      { mapView.highlightZone(id); }
    public void focusZone(Zone zone)       { if (zone != null) { mapView.focusZone(zone); selectZone(zone); } }
    public void focusZoneById(int id)      { zones.stream().filter(z -> z.getId() == id).findFirst().ifPresent(this::focusZone); }
    public void selectZone(Zone zone)      { if (zone != null) { selectedZone = zone; mapView.selectZone(zone); } }
    public Zone getSelectedZone()          { return selectedZone; }

    public void updateZone(Zone zone, double niveauEau) { mapView.updateZoneWithWaterLevel(zone, niveauEau); }
    public void updateAllZones(List<Zone> updatedZones) { mapView.updateAllZones(updatedZones); routeGraph.refreshAllEdges(); }

    public void drawRoutes(List<int[]> routes, List<Zone> zones) { mapView.refreshRouteColors(); }

    public void setOnZoneSelected(Consumer<Zone> cb)         { this.onZoneSelected = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement> cb){ this.onAgentArrived = cb; }

    public RouteGraph  getRouteGraph() { return routeGraph; }
    public List<Zone>  getZones()      { return zones; }
    public List<Agent> getAgents()     { return agents; }
    public List<Edge>  getEdges()      { return routeGraph.getEdges(); }
    public MapView getMapView() { return mapView; }
}
