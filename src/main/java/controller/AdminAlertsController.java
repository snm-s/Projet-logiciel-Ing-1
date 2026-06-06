package controller;

import javafx.collections.ObservableList;
import model.alert.Alert;
import model.alert.AlertSystem;
import view.AdminAlertsView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AdminAlertsController — Plein pouvoir sur les alertes.
 *
 * Capacités :
 *  - Ajouter / Supprimer / Résoudre des alertes
 *  - Valider ou rejeter les suggestions des citoyens
 *  - Synchroniser AlertSystem (modèle) ↔ AdminAlertsView
 */
public class AdminAlertsController {

    private final AdminAlertsView view;
    private final AlertSystem     model;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────────────
    public AdminAlertsController(AdminAlertsView view, AlertSystem model) {
        this.view  = view;
        this.model = model;
        view.setController(this);
        syncModelToView();
    }

    // ── Actions déclenchées par la vue ────────────────────────────────────

    /** Ajoute une nouvelle alerte (depuis le formulaire admin). */
    public void addAlert(Alert alert) {
        alert.setId(nextId());
        if (isBlankTime(alert.getTime()))
            alert.setTime(LocalTime.now().format(TIME_FMT));

        model.addAlert(alert);
        view.getAllAlerts().add(alert);
        view.refreshTable();
    }

    /** Résout une alerte publiée. */
    public void resolveAlert(Alert alert) {
        if (alert == null) return;
        alert.setStatus("Résolue");
        syncStatus(alert, "Résolue");
        view.refreshTable();
    }

    /** Supprime définitivement une alerte. */
    public void deleteAlert(Alert alert) {
        if (alert == null) return;
        view.getAllAlerts().remove(alert);
        model.removeAlert(alert);
        view.refreshTable();
    }

    /** Valide une suggestion citoyen → publie l'alerte. */
    public void approveSuggestion(Alert suggestion) {
        if (suggestion == null) return;
        view.getSuggestions().remove(suggestion);
        model.approveSuggestion(suggestion);       // modifie status + origin dans le modèle
        view.getAllAlerts().add(suggestion);
        view.refreshTable();
    }

    /** Rejette une suggestion citoyen. */
    public void rejectSuggestion(Alert suggestion) {
        if (suggestion == null) return;
        view.getSuggestions().remove(suggestion);
        model.rejectSuggestion(suggestion);
        view.refreshTable();
    }

    /** Recharge la vue depuis le modèle (utile après injection externe). */
    public void reloadFromModel() {
        syncModelToView();
        view.refreshTable();
    }

    // ── Requêtes utilitaires ──────────────────────────────────────────────

    public int          countActiveAlerts()   { return model.getActiveAlerts().size(); }
    public int          countSuggestions()    { return model.countPendingSuggestions(); }
    public Alert        getLatestAlert()      { return model.getLatestAlert(); }
    public List<Alert>  getActiveAlerts()     { return model.getActiveAlerts(); }
    public List<Alert>  getAllAlerts()         { return model.getAlerts(); }

    // ── Privé ─────────────────────────────────────────────────────────────

    private void syncModelToView() {
        ObservableList<Alert> viewList = view.getAllAlerts();
        for (Alert a : model.getAlerts()) {
            if (!viewList.contains(a)) viewList.add(a);
        }
        ObservableList<Alert> suggList = view.getSuggestions();
        for (Alert s : model.getSuggestions()) {
            if (!suggList.contains(s)) suggList.add(s);
        }
    }

    private void syncStatus(Alert target, String newStatus) {
        for (Alert a : model.getAlerts()) {
            if (a == target || a.getId() == target.getId()) {
                a.setStatus(newStatus);
                break;
            }
        }
    }

    private int nextId() {
        return model.getAlerts().stream().mapToInt(Alert::getId).max().orElse(0) + 1;
    }

    private boolean isBlankTime(String t) {
        return t == null || t.isBlank() || "--:--".equals(t);
    }
}
