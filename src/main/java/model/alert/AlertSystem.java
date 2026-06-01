package model.alert;

import java.util.ArrayList;
import java.util.List;

public class AlertSystem {
    private List<Alert> activeAlerts = new ArrayList<>();

    public List<Alert> getActiveAlerts() {
        return activeAlerts;
    }

    public Alert getLatestAlert() {
        if (activeAlerts.isEmpty()) {
            return new Alert(1, "Aucune alerte majeure pour le moment.", 0);
        }
        return activeAlerts.get(activeAlerts.size() - 1);
    }

    public void addAlert(Alert alert) {
        activeAlerts.add(alert);
    }
}
