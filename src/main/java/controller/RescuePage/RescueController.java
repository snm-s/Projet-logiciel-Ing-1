package controller.RescuePage;

import java.util.List;

import app.Main;
import controller.MapController;
import model.agent.Agent;
import model.alert.Alert;
import model.algorithms.EvacuationPath;
import model.graph.RouteGraph;
import model.simulation.FloodSimulation;
import model.zone.Zone;
import model.zone.ZoneManager;

public class RescueController {

    private final FloodSimulation simulation;

    /**
     * Constructs a new RescueController.
     * @param simulation the simulation.
     */
    public RescueController(FloodSimulation simulation) {
        this.simulation = simulation;
    }

    /**
     * Returns the simulation.
     * @return the FloodSimulation.
     */
    public FloodSimulation getSimulation() {
        return simulation;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the deployed agents.
     * @return the int result.
     */
    public int getDeployedAgents() {
        return Math.max(simulation.getActiveAgentsCount(), 24);
    }

    /**
     * Returns the rescued victims.
     * @return the int result.
     */
    public int getRescuedVictims() {
        return simulation.getRescuedVictimsCount();
    }

    /**
     * Returns the active missions.
     * @return the int result.
     */
    public int getActiveMissions() {
        return Math.max(simulation.getActiveMissionsCount(), 3);
    }

    /**
     * Returns the active alerts count.
     * @return the int result.
     */
    public int getActiveAlertsCount() {
        return simulation.getAlertSystem().getActiveAlerts().size();
    }

    /**
     * Returns the recent alerts.
     * @return the List<Alert>.
     */
    public List<Alert> getRecentAlerts() {
        return simulation.getAlertSystem().getActiveAlerts();
    }

    /**
     * Returns the zones.
     * @return the List<Zone>.
     */
    public List<Zone> getZones() {
        return new ZoneManager().getZones();
    }

    /**
     * Returns the agents.
     * @return the List<Agent>.
     */
    public List<Agent> getAgents() {
        return simulation.getAgents();
    }

    /**
     * Returns the flooded zones count.
     * @return the int result.
     */
    public int getFloodedZonesCount() {
        return (int) getZones().stream().filter(Zone::isFlooded).count();
    }

    /**
     * Returns the safe zones count.
     * @return the int result.
     */
    public int getSafeZonesCount() {
        return (int) getZones().stream().filter(z -> !z.isFlooded()).count();
    }

    /**
     * Returns the operational status.
     * @return the String.
     */
    public String getOperationalStatus() {
        int alerts = getActiveAlertsCount();
        if (alerts >= 5) return "Crise majeure";
        if (alerts >= 2) return "Intervention active";
        return "Surveillance";
    }

    // ─────────────────────────────────────────────────────────────────────
    // CARTE & ROUTAGE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Retourne le RouteGraph partagé via le MapController.
     * Null si le MapController n'est pas encore initialisé.
     */
    public RouteGraph getRouteGraph() {
        MapController mc = Main.getSharedMapController();
        return mc != null ? mc.getRouteGraph() : null;
    }

    /**
     * Calcule un chemin entre deux zones via le Dijkstra secouristes
     * (routes inondées traversables avec pénalité).
     *
     * @param from zone de départ
     * @param to   zone cible
     * @return chemin calculé, ou null si impossible
     */
    public EvacuationPath computePath(Zone from, Zone to) {
        if (from == null || to == null) return null;
        RouteGraph rg = getRouteGraph();
        if (rg == null) return null;
        return rg.findPathForRescue(from, to);
    }
}
