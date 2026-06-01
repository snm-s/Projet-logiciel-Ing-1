package controller;

import model.alert.Alert;
import model.simulation.FloodSimulation;

public class CitizenController {
    private FloodSimulation simulation;
    private String currentCitizenName = "Jean Dupont";

    public CitizenController(FloodSimulation simulation) {
        this.simulation = simulation;
    }

    public String getCitizenName() {
        return currentCitizenName;
    }

    public int getAlertCount() {
        return simulation.getAlertSystem().getActiveAlerts().size();
    }

    public Alert getLatestCriticalAlert() {
        return simulation.getAlertSystem().getLatestAlert();
    }

 public model.graph.Route calculateEvacuationRoute(model.graph.Node citizenNode) {
    // Temporaire en attendant de brancher Dijkstra/AStar
    return new model.graph.Route();
}


}
