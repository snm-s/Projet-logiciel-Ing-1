package controller.RescuePage;

import java.util.List;

import model.alert.Alert;
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
}
