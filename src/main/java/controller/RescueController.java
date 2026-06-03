package controller;

import java.util.List;

import model.alert.Alert;
import model.simulation.FloodSimulation;

public class RescueController {
    private FloodSimulation simulation;

    public RescueController(FloodSimulation simulation) {
        this.simulation = simulation;
        m
    }

    public int getDeployedAgents() { return simulation.getActiveAgentsCount(); }
    public int getRescuedVictims() { return simulation.getRescuedVictimsCount(); }
    public int getActiveMissions() { return simulation.getActiveMissionsCount(); }
    public int getActiveAlertsCount() { 
    return simulation.getAlertSystem().getActiveAlerts().size(); 
}

    public List<Alert> getRecentAlerts() {
        return simulation.getAlertSystem().getActiveAlerts();
    }
}
