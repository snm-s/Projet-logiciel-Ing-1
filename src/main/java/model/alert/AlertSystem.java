package model.alert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import model.enums.AlertType;

public class AlertSystem {
    private final List<Alert> alerts     = new ArrayList<>();
    private final List<Alert> suggestions = new ArrayList<>();

    // ── Published alerts (visible to all) ─────────────────────────────────
    public List<Alert> getAlerts() { return alerts; }

    public List<Alert> getActiveAlerts() {
        return alerts.stream()
            .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
            .collect(Collectors.toList());
    }

    public Alert getLatestAlert() {
        if (alerts.isEmpty())
            return new Alert(AlertType.INFO, "Aucune alerte", "Système", "Faible", "00:00", "Résolue");
        return alerts.get(alerts.size() - 1);
    }

    public void addAlert(Alert alert) { alerts.add(alert); }

    public void removeAlert(Alert alert) { alerts.remove(alert); }

    // ── Citizen suggestions (pending admin validation) ─────────────────────
    public List<Alert> getSuggestions() { return suggestions; }

    public void addSuggestion(Alert alert) {
        alert.setOrigin("suggestion");
        alert.setStatus("En attente");
        suggestions.add(alert);
    }

    /** Admin approves a suggestion → moves it to published alerts */
    public void approveSuggestion(Alert suggestion) {
        suggestions.remove(suggestion);
        suggestion.setOrigin("admin");
        suggestion.setStatus("Active");
        alerts.add(suggestion);
    }

    /** Admin rejects a suggestion → simply removes it */
    public void rejectSuggestion(Alert suggestion) {
        suggestions.remove(suggestion);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    public int countPendingSuggestions() { return suggestions.size(); }
}
