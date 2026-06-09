package controller.CitizenPage;

import javafx.application.Platform;
import model.alert.Alert;
import model.alert.AlertSystem;
import view.CitizenAlertsView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CitizenAlertsController {

    private final CitizenAlertsView view;
    private final AlertSystem model;
    private final Runnable alertListener;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public CitizenAlertsController(CitizenAlertsView view, AlertSystem model) {
        this.view = view;
        this.model = model;

        view.setController(this);

        this.alertListener = () -> Platform.runLater(this::reloadPublishedAlerts);
        this.model.addListener(alertListener);

        reloadPublishedAlerts();
    }

    public void submitSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        if (isBlankTime(suggestion.getTime())) {
            suggestion.setTime(LocalTime.now().format(TIME_FMT));
        }

        suggestion.setOrigin("suggestion");
        suggestion.setStatus("En attente");

        model.addSuggestion(suggestion);
    }

    public void reloadPublishedAlerts() {
        view.setAlerts(model.getAlerts());
    }

    public List<Alert> getPublishedAlerts() {
        return model.getAlerts();
    }

    public List<Alert> getActiveAlerts() {
        return model.getActiveAlerts();
    }

    public Alert getLatestAlert() {
        return model.getLatestAlert();
    }

    public int getPendingSuggestionsCount() {
        return model.countPendingSuggestions();
    }

    public void dispose() {
        model.removeListener(alertListener);
    }

    private boolean isBlankTime(String time) {
        return time == null || time.isBlank() || "--:--".equals(time);
    }
}
