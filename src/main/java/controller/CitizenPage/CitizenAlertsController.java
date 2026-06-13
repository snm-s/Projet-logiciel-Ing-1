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

    /**
     * Constructs a new CitizenAlertsController.
     * @param view the view.
     * @param model the model.
     */
    public CitizenAlertsController(CitizenAlertsView view, AlertSystem model) {
        this.view = view;
        this.model = model;

        view.setController(this);

        this.alertListener = () -> Platform.runLater(this::reloadPublishedAlerts);
        this.model.addListener(alertListener);

        reloadPublishedAlerts();
    }

    /**
     * Submits suggestion.
     * @param suggestion the suggestion.
     */
    public void submitSuggestion(Alert suggestion) {
        if (suggestion == null) return;

        if (isBlankTime(suggestion.getTime())) {
            suggestion.setTime(LocalTime.now().format(TIME_FMT));
        }

        suggestion.setOrigin("suggestion");
        suggestion.setStatus("En attente");

        model.addSuggestion(suggestion);
    }

    /**
     * Performs published alerts.
     */
    public void reloadPublishedAlerts() {
        view.setAlerts(model.getAlerts());
    }

    /**
     * Returns the published alerts.
     * @return the List<Alert>.
     */
    public List<Alert> getPublishedAlerts() {
        return model.getAlerts();
    }

    /**
     * Returns the active alerts.
     * @return the List<Alert>.
     */
    public List<Alert> getActiveAlerts() {
        return model.getActiveAlerts();
    }

    /**
     * Returns the latest alert.
     * @return the Alert.
     */
    public Alert getLatestAlert() {
        return model.getLatestAlert();
    }

    /**
     * Returns the pending suggestions count.
     * @return the int result.
     */
    public int getPendingSuggestionsCount() {
        return model.countPendingSuggestions();
    }

    /**
     * Performs dispose.
     */
    public void dispose() {
        model.removeListener(alertListener);
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
