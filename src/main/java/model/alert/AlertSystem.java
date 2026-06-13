package model.alert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import model.enums.AlertType;

public class AlertSystem {

    private final List<Alert> alerts = new ArrayList<>();
    private final List<Alert> suggestions = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    /**
     * Returns the alerts.
     * @return the List<Alert>.
     */
    public List<Alert> getAlerts() {
        return alerts;
    }

    /**
     * Returns the active alerts.
     * @return the List<Alert>.
     */
    public List<Alert> getActiveAlerts() {
        return alerts.stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the latest alert.
     * @return the Alert.
     */
    public Alert getLatestAlert() {
        if (alerts.isEmpty()) {
            return new Alert(AlertType.INFO, "Aucune alerte", "Système", "Faible", "00:00", "Résolue");
        }

        return alerts.get(alerts.size() - 1);
    }

    /**
     * Adds alert.
     * @param alert the alert.
     */
    public void addAlert(Alert alert) {
        if (alert == null) return;

        alerts.add(alert);
        notifyListeners();
    }

    /**
     * Removes alert.
     * @param alert the alert.
     */
    public void removeAlert(Alert alert) {
        if (alert == null) return;

        alerts.remove(alert);
        notifyListeners();
    }

    /**
     * Returns the suggestions.
     * @return the List<Alert>.
     */
    public List<Alert> getSuggestions() {
        return suggestions;
    }

    /**
     * Adds suggestion.
     * @param alert the alert.
     */
    public void addSuggestion(Alert alert) {
        if (alert == null) return;

        alert.setOrigin("suggestion");
        alert.setStatus("En attente");

        suggestions.add(alert);
        notifyListeners();
    }

    /**
     * Approves suggestion.
     * @param suggestion the suggestion.
     */
    public void approveSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        suggestions.remove(suggestion);

        suggestion.setOrigin("admin");
        suggestion.setStatus("Active");

        alerts.add(suggestion);
        notifyListeners();
    }

    /**
     * Rejects suggestion.
     * @param suggestion the suggestion.
     */
    public void rejectSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        suggestions.remove(suggestion);
        notifyListeners();
    }

    /**
     * Counts the pending suggestions.
     * @return the int result.
     */
    public int countPendingSuggestions() {
        return suggestions.size();
    }

    /**
     * Adds listener.
     * @param listener the listener.
     */
    public void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes listener.
     * @param listener the listener.
     */
    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies changes.
     */
    public void notifyChanges() {
        notifyListeners();
    }

    /**
     * Notifies listeners.
     */
    private void notifyListeners() {
        List<Runnable> copy = new ArrayList<>(listeners);

        for (Runnable listener : copy) {
            listener.run();
        }
    }
}
