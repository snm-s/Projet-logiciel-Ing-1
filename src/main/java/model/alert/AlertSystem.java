package model.alert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import model.enums.AlertType;
import model.enums.AlertSeverity;
import model.observer.Observer;
import model.alert.Alert;
import model.graph.Node;

public class AlertSystem {

    private final List<Alert> alerts = new ArrayList<>();
    private final List<Alert> suggestions = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();

    public List<Alert> getAlerts() {
        return alerts;
    }

    public List<Alert> getActiveAlerts() {
        return alerts.stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());
    }

    public Alert getLatestAlert() {
        if (alerts.isEmpty()) {
            return new Alert(AlertType.INFO, "Aucune alerte", null, AlertSeverity.LOW, "00:00", "Résolue");

        }

        return alerts.get(alerts.size() - 1);
    }

    public void addAlert(Alert alert) {
        if (alert == null)
            return;

        alerts.add(alert);
        notifyListeners(alert);
    }

    public void removeAlert(Alert alert) {
        if (alert == null)
            return;

        alerts.remove(alert);
        notifyListeners(alert);
    }

    public List<Alert> getSuggestions() {
        return suggestions;
    }

    public void addSuggestion(Alert alert) {
        if (alert == null)
            return;

        alert.setOrigin("suggestion");
        alert.setStatus("En attente");

        suggestions.add(alert);
        notifyListeners(alert);
    }

    public void approveSuggestion(Alert suggestion) {
        if (suggestion == null)
            return;

        suggestions.remove(suggestion);

        suggestion.setOrigin("admin");
        suggestion.setStatus("Active");

        alerts.add(suggestion);
        notifyListeners(suggestion);
    }

    public void rejectSuggestion(Alert suggestion) {
        if (suggestion == null)
            return;

        suggestions.remove(suggestion);
        notifyListeners(suggestion);
    }

    public int countPendingSuggestions() {
        return suggestions.size();
    }

    public void addListener(Observer obs) {
        if (observers != null && !observers.contains(obs)) {
            observers.add(obs);
        }
    }

    public void removeListener(Observer obs) {
        observers.remove(obs);
    }

    public void notifyListeners(Alert alert) {
        List<Observer> copy = new ArrayList<>(observers);

        for (Observer listener : copy) {
            listener.update(alert);
        }
    }
}
