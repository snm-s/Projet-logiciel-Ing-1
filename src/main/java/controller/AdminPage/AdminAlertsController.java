package controller.AdminPage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Platform;
import model.alert.Alert;
import model.alert.AlertSystem;
import view.AdminAlertsView;

public class AdminAlertsController {

    private final AdminAlertsView view;
    private final AlertSystem model;
    private final Runnable alertListener;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructs a new AdminAlertsController.
     * @param view the view.
     * @param model the model.
     */
    public AdminAlertsController(AdminAlertsView view, AlertSystem model) {
        this.view = view;
        this.model = model;

        view.setController(this);

        this.alertListener = () -> Platform.runLater(this::reloadFromModel);
        this.model.addListener(alertListener);

        reloadFromModel();
    }

    /**
     * Adds alert.
     * @param alert the alert.
     */
    public void addAlert(Alert alert) {
        if (alert == null) return;

        alert.setId(nextId());

        if (isBlankTime(alert.getTime())) {
            alert.setTime(LocalTime.now().format(TIME_FMT));
        }

        alert.setOrigin("admin");

        if (alert.getStatus() == null || alert.getStatus().isBlank()) {
            alert.setStatus("Active");
        }

        model.addAlert(alert);
    }

    /**
     * Resolves alert.
     * @param alert the alert.
     */
    public void resolveAlert(Alert alert) {
        if (alert == null) return;

        alert.setStatus("Résolue");
        model.notifyChanges();
    }

    /**
     * Deletes alert.
     * @param alert the alert.
     */
    public void deleteAlert(Alert alert) {
        if (alert == null) return;

        model.removeAlert(alert);
    }

    /**
     * Approves suggestion.
     * @param suggestion the suggestion.
     */
    public void approveSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        model.approveSuggestion(suggestion);
    }

    /**
     * Rejects suggestion.
     * @param suggestion the suggestion.
     */
    public void rejectSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        model.rejectSuggestion(suggestion);
    }

    /**
     * Performs from model.
     */
    public void reloadFromModel() {
        view.setAlerts(model.getAlerts());
        view.setSuggestions(model.getSuggestions());
        view.refreshTable();
    }

    /**
     * Counts the active alerts.
     * @return the int result.
     */
    public int countActiveAlerts() {
        return model.getActiveAlerts().size();
    }

    /**
     * Counts the suggestions.
     * @return the int result.
     */
    public int countSuggestions() {
        return model.countPendingSuggestions();
    }

    /**
     * Returns the latest alert.
     * @return the Alert.
     */
    public Alert getLatestAlert() {
        return model.getLatestAlert();
    }

    /**
     * Returns the active alerts.
     * @return the List<Alert>.
     */
    public List<Alert> getActiveAlerts() {
        return model.getActiveAlerts();
    }

    /**
     * Returns the all alerts.
     * @return the List<Alert>.
     */
    public List<Alert> getAllAlerts() {
        return model.getAlerts();
    }

    /**
     * Performs dispose.
     */
    public void dispose() {
        model.removeListener(alertListener);
    }

    /**
     * Performs id.
     * @return the int result.
     */
    private int nextId() {
        return model.getAlerts().stream()
                .mapToInt(Alert::getId)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Returns whether blank time.
     * @param time the time.
     * @return the boolean result.
     */
    private boolean isBlankTime(String time) {
        return time == null || time.isBlank() || "--:--".equals(time);
    }
}
