package controller;

import javafx.collections.ObservableList;
import model.alert.Alert;
import model.alert.AlertSystem;
import view.CitizenAlertsView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CitizenAlertsController — Droits restreints au citoyen.
 *
 * Capacités :
 *  - Consulter les alertes publiées (lecture seule)
 *  - Soumettre une suggestion (envoyée en attente de validation admin)
 *  - Aucune modification/suppression directe des alertes
 */
public class CitizenAlertsController {

    private final CitizenAlertsView view;
    private final AlertSystem       model;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────────────
    public CitizenAlertsController(CitizenAlertsView view, AlertSystem model) {
        this.view  = view;
        this.model = model;
        view.setController(this);
        syncPublishedAlerts();
    }

    // ── Action principale : soumettre une suggestion ──────────────────────

    /**
     * Soumet une suggestion citoyen.
     * Elle atterrit dans AlertSystem.suggestions (en attente de validation admin).
     * Elle n'est PAS ajoutée à la vue citoyen (pas encore publiée).
     *
     * @param suggestion l'alerte suggérée (origin = "suggestion", status = "En attente")
     */
    public void submitSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        // Horodatage auto si vide
        if (isBlankTime(suggestion.getTime()))
            suggestion.setTime(LocalTime.now().format(TIME_FMT));

        // Enregistrement dans le modèle partagé
        model.addSuggestion(suggestion);
        // La vue n'est PAS mise à jour : le citoyen ne voit pas ses suggestions en attente
    }

    // ── Mise à jour de la vue quand les alertes publiées changent ─────────

    /**
     * Recharge les alertes publiées depuis le modèle.
     * À appeler depuis l'extérieur quand l'admin publie une nouvelle alerte.
     */
    public void reloadPublishedAlerts() {
        syncPublishedAlerts();
        view.refreshTable();
    }

    // ── Requêtes lecture seule ────────────────────────────────────────────

    public List<Alert> getPublishedAlerts() { return model.getAlerts(); }
    public List<Alert> getActiveAlerts()    { return model.getActiveAlerts(); }
    public Alert       getLatestAlert()     { return model.getLatestAlert(); }

    // ── Privé ─────────────────────────────────────────────────────────────

    /**
     * Pousse les alertes publiées du modèle vers la vue citoyen (sans suggestions).
     */
    private void syncPublishedAlerts() {
        ObservableList<Alert> viewList = view.getAllAlerts();
        for (Alert a : model.getAlerts()) {
            if (!viewList.contains(a)) viewList.add(a);
        }
    }

    private boolean isBlankTime(String t) {
        return t == null || t.isBlank() || "--:--".equals(t);
    }
}
