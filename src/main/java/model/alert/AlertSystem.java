package model.alert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import model.enums.AlertType;

public class AlertSystem {

    private final List<Alert> alerts = new ArrayList<>();
    private final List<Alert> suggestions = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

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
            return new Alert(AlertType.INFO, "Aucune alerte", "Système", "Faible", "00:00", "Résolue");
        }

        return alerts.get(alerts.size() - 1);
    }

    public void addAlert(Alert alert) {
        if (alert == null) return;

        alerts.add(alert);
        notifyListeners();
    }

    public void removeAlert(Alert alert) {
        if (alert == null) return;

        alerts.remove(alert);
        notifyListeners();
    }

    public List<Alert> getSuggestions() {
        return suggestions;
    }

    public void addSuggestion(Alert alert) {
        if (alert == null) return;

        alert.setOrigin("suggestion");
        alert.setStatus("En attente");

        suggestions.add(alert);
        notifyListeners();
    }

    public void approveSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        suggestions.remove(suggestion);

        suggestion.setOrigin("admin");
        suggestion.setStatus("Active");

        alerts.add(suggestion);
        notifyListeners();
    }

    public void rejectSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        suggestions.remove(suggestion);
        notifyListeners();
    }

    public int countPendingSuggestions() {
        return suggestions.size();
    }

    public void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void notifyChanges() {
        notifyListeners();
    }

    private void notifyListeners() {
        List<Runnable> copy = new ArrayList<>(listeners);

        for (Runnable listener : copy) {
            listener.run();
        }
    }
}
