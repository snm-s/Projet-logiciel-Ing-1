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

    public AdminAlertsController(AdminAlertsView view, AlertSystem model) {
        this.view = view;
        this.model = model;

        view.setController(this);

        this.alertListener = () -> Platform.runLater(this::reloadFromModel);
        this.model.addListener(alertListener);

        reloadFromModel();
    }

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

    public void resolveAlert(Alert alert) {
        if (alert == null) return;

        alert.setStatus("Résolue");
        model.notifyChanges();
    }

    public void deleteAlert(Alert alert) {
        if (alert == null) return;

        model.removeAlert(alert);
    }

    public void approveSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        model.approveSuggestion(suggestion);
    }

    public void rejectSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        model.rejectSuggestion(suggestion);
    }

    public void reloadFromModel() {
        view.setAlerts(model.getAlerts());
        view.setSuggestions(model.getSuggestions());
        view.refreshTable();
    }

    public int countActiveAlerts() {
        return model.getActiveAlerts().size();
    }

    public int countSuggestions() {
        return model.countPendingSuggestions();
    }

    public Alert getLatestAlert() {
        return model.getLatestAlert();
    }

    public List<Alert> getActiveAlerts() {
        return model.getActiveAlerts();
    }

    public List<Alert> getAllAlerts() {
        return model.getAlerts();
    }

    public void dispose() {
        model.removeListener(alertListener);
    }

    private int nextId() {
        return model.getAlerts().stream()
                .mapToInt(Alert::getId)
                .max()
                .orElse(0) + 1;
    }

    private boolean isBlankTime(String time) {
        return time == null || time.isBlank() || "--:--".equals(time);
    }
}
