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

    public RescueController(FloodSimulation simulation) {
        this.simulation = simulation;
    }

    public FloodSimulation getSimulation() {
        return simulation;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────

    public int getDeployedAgents() {
        return Math.max(simulation.getActiveAgentsCount(), 24);
    }

    public int getRescuedVictims() {
        return simulation.getRescuedVictimsCount();
    }

    public int getActiveMissions() {
        return Math.max(simulation.getActiveMissionsCount(), 3);
    }

    public int getActiveAlertsCount() {
        return simulation.getAlertSystem().getActiveAlerts().size();
    }

    public List<Alert> getRecentAlerts() {
        return simulation.getAlertSystem().getActiveAlerts();
    }

    public List<Zone> getZones() {
        return new ZoneManager().getZones();
    }

    public List<Agent> getAgents() {
        return simulation.getAgents();
    }

    public int getFloodedZonesCount() {
        return (int) getZones().stream().filter(Zone::isFlooded).count();
    }

    public int getSafeZonesCount() {
        return (int) getZones().stream().filter(z -> !z.isFlooded()).count();
    }

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