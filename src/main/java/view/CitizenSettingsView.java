package view;

import java.util.function.Consumer;

import app.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.agent.Agent;
import model.agent.Citizen;
import model.auth.UserService;

public class CitizenSettingsView extends BorderPane {

    private static final String WHITE = "#ffffff";
    private static final String LIGHT = "#b8c7dd";
    private static final String MUTED = "#7f91aa";
    private static final String BLUE = "#1683ff";
    private static final String BLUE_DARK = "#0e73eb";
    private static final String GREEN = "#22c55e";
    private static final String RED = "#ef4444";
    private static final String CARD_BG = "rgba(8,22,42,0.72)";

    private final Agent user;
    private final Citizen citizen;
    private final Consumer<Double> displayScaleCallback;

    private CheckBox mobilityReducedCheck;
    private CheckBox emergencyAlertsCheck;
    private CheckBox soundNotificationsCheck;
    private CheckBox routeUpdatesCheck;
    private CheckBox backgroundLocationCheck;
    private Slider displayScaleSlider;
    private Label scaleValueLabel;
    private Label statusLabel;
    private Label mobilityBadge;

    /**
     * Create the default citizen settings view.
     */
    public CitizenSettingsView() {
        this(Main.currentUser, null);
    }

    /**
     * Create the settings view prefilled for a specific agent and a display scale callback.
     *
     * @param user agent whose settings are shown
     * @param displayScaleCallback callback to adjust display scaling
     */
    public CitizenSettingsView(Agent user, Consumer<Double> displayScaleCallback) {
        this.user = user;
        this.citizen = user instanceof Citizen ? (Citizen) user : null;
        this.displayScaleCallback = displayScaleCallback;

        setStyle("-fx-background-color: transparent;");
        setPadding(new Insets(0));

        VBox page = new VBox(22);
        page.setPadding(new Insets(30));

        VBox header = new VBox(6);
        Label title = label("Paramètres", WHITE, 28, true);
        Label subtitle = label("Modifiez vos préférences puis enregistrez pour les conserver dans votre profil.", LIGHT, 14, false);
        header.getChildren().addAll(title, subtitle);

        if (citizen == null) {
            VBox errorCard = glassCard(20);
            errorCard.getChildren().addAll(
                    label("Profil non compatible", WHITE, 18, true),
                    label("Cette page est réservée aux comptes citoyens.", LIGHT, 13, false)
            );
            page.getChildren().addAll(header, errorCard);
        } else {
            page.getChildren().addAll(
                    header,
                    buildProfileCard(),
                    buildSettingsGrid(),
                    buildSaveBar()
            );
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(scroll);
    }

    /**
     * Builds profile card.
     * @return the VBox.
     */
    private VBox buildProfileCard() {
        VBox card = glassCard(20);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle circle = new Circle(34);
        circle.setFill(Color.web(BLUE));

        Label initials = label(getInitials(getFullName()), WHITE, 16, true);
        avatar.getChildren().addAll(circle, initials);

        VBox profileText = new VBox(4);
        Label name = label(getFullName(), WHITE, 20, true);
        Label email = label(nonEmpty(user.getEmail(), "Email non renseigné"), LIGHT, 13, false);

        HBox badges = new HBox(8);

        mobilityBadge = badge(
                citizen.isMobilityReduced() ? "PMR activé" : "Mobilité standard",
                citizen.isMobilityReduced() ? RED : GREEN
        );

        badges.getChildren().addAll(
                badge("Citoyen", BLUE),
                mobilityBadge
        );

        profileText.getChildren().addAll(name, email, badges);
        row.getChildren().addAll(avatar, profileText);

        card.getChildren().add(row);
        return card;
    }

    /**
     * Builds settings grid.
     * @return the HBox.
     */
    private HBox buildSettingsGrid() {
        HBox grid = new HBox(18);
        grid.setAlignment(Pos.TOP_LEFT);
    
        VBox column = new VBox(18);
        column.setAlignment(Pos.TOP_LEFT);
        column.setMaxWidth(650);
    
        column.getChildren().addAll(
                buildNotificationSection()
        );
    
        grid.getChildren().add(column);
        HBox.setHgrow(column, Priority.ALWAYS);
    
        return grid;
    }

    /**
     * Builds notification section.
     * @return the VBox.
     */
    private VBox buildNotificationSection() {
        VBox wrapper = sectionWrapper("Notifications");
        VBox card = (VBox) wrapper.getChildren().get(1);

        emergencyAlertsCheck = settingCheckBox(citizen.isEmergencyAlertsEnabled());
        soundNotificationsCheck = settingCheckBox(citizen.isSoundNotificationsEnabled());
        routeUpdatesCheck = settingCheckBox(citizen.isRouteUpdatesEnabled());
        backgroundLocationCheck = settingCheckBox(citizen.isBackgroundLocationEnabled());

        card.getChildren().addAll(
                settingRow("!", "Alertes importantes", "Notification des situations urgentes", emergencyAlertsCheck),
                settingRow("◉", "Son des notifications", "Jouer un son lors des alertes", soundNotificationsCheck),
                settingRow("↻", "Mises à jour itinéraire", "Recalcul automatique du trajet", routeUpdatesCheck),
                settingRow("⌖", "Localisation arrière-plan", "Mise à jour continue de la position", backgroundLocationCheck)
        );

        return wrapper;
    }

    /**
     * Builds save bar.
     * @return the HBox.
     */
    private HBox buildSaveBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(18));
        bar.setStyle(
                "-fx-background-color: rgba(8,22,42,0.78);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.16);" +
                        "-fx-border-radius: 18;"
        );

        statusLabel = label("Aucune modification enregistrée pour le moment.", MUTED, 13, false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button resetButton = darkButton("Réinitialiser");
        resetButton.setOnAction(event -> resetValues());

        Button saveButton = blueButton("Enregistrer les modifications");
        saveButton.setOnAction(event -> saveSettings());

        bar.getChildren().addAll(statusLabel, spacer, resetButton, saveButton);
        return bar;
    }

    /**
     * Saves settings.
     */
    private void saveSettings() {
        if (citizen == null) {
            return;
        }

        citizen.setEmergencyAlertsEnabled(emergencyAlertsCheck.isSelected());
        citizen.setSoundNotificationsEnabled(soundNotificationsCheck.isSelected());
        citizen.setRouteUpdatesEnabled(routeUpdatesCheck.isSelected());
        citizen.setBackgroundLocationEnabled(backgroundLocationCheck.isSelected());

        boolean saved = UserService.updateAgent(citizen);

        if (saved) {
            Main.currentUser = citizen;
            updateMobilityBadge();

            if (displayScaleCallback != null) {
                displayScaleCallback.accept(citizen.getDisplayScale());
            }

            statusLabel.setText("Modifications enregistrées dans dataUser/users.json ✓");
            statusLabel.setTextFill(Color.web(GREEN));
        } else {
            statusLabel.setText("Impossible d’enregistrer : utilisateur introuvable dans users.json.");
            statusLabel.setTextFill(Color.web(RED));
        }
    }

    /**
     * Resets values.
     */
    private void resetValues() {
        if (citizen == null) {
            return;
        }

        emergencyAlertsCheck.setSelected(citizen.isEmergencyAlertsEnabled());
        soundNotificationsCheck.setSelected(citizen.isSoundNotificationsEnabled());
        routeUpdatesCheck.setSelected(citizen.isRouteUpdatesEnabled());
        backgroundLocationCheck.setSelected(citizen.isBackgroundLocationEnabled());

        if (displayScaleCallback != null) {
            displayScaleCallback.accept(citizen.getDisplayScale() <= 0 ? 100.0 : citizen.getDisplayScale());
        }

        statusLabel.setText("Modifications annulées.");
        statusLabel.setTextFill(Color.web(MUTED));
    }

    /**
     * Updates mobility badge.
     */
    private void updateMobilityBadge() {
        if (mobilityBadge == null || citizen == null) {
            return;
        }

        boolean isPmr = citizen.isMobilityReduced();

        mobilityBadge.setText(isPmr ? "PMR activé" : "Mobilité standard");
        mobilityBadge.setStyle(
                "-fx-background-color: " + (isPmr ? RED : GREEN) + ";" +
                        "-fx-background-radius: 999;"
        );
    }

    /**
     * Performs wrapper.
     * @param titleText the titleText.
     * @return the VBox.
     */
    private VBox sectionWrapper(String titleText) {
        VBox wrapper = new VBox(10);

        Label title = label(titleText.toUpperCase(), MUTED, 12, true);
        title.setStyle("-fx-letter-spacing: 2px;");

        VBox card = glassCard(0);
        card.setSpacing(0);

        wrapper.getChildren().addAll(title, card);
        return wrapper;
    }

    /**
     * Performs row.
     * @param iconText the iconText.
     * @param title the title.
     * @param subtitle the subtitle.
     * @param checkBox the checkBox.
     * @return the HBox.
     */
    private HBox settingRow(String iconText, String title, String subtitle, CheckBox checkBox) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setMinHeight(72);
        row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.035);" +
                        "-fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-width: 0 0 1 0;"
        );

        VBox texts = new VBox(3);
        texts.getChildren().addAll(
                label(title, WHITE, 15, true),
                label(subtitle, MUTED, 12, false)
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(lineIcon(iconText), texts, spacer, checkBox);
        row.setOnMouseClicked(event -> checkBox.setSelected(!checkBox.isSelected()));

        return row;
    }

    /**
     * Performs row.
     * @param title the title.
     * @param value the value.
     * @return the HBox.
     */
    private HBox infoRow(String title, String value) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setMinHeight(66);
        row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.035);" +
                        "-fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-width: 0 0 1 0;"
        );

        VBox texts = new VBox(3);
        texts.getChildren().addAll(
                label(title, WHITE, 15, true),
                label(value, MUTED, 12, false)
        );

        row.getChildren().addAll(lineIcon("i"), texts);
        return row;
    }

    /**
     * Performs check box.
     * @param selected the selected.
     * @return the CheckBox.
     */
    private CheckBox settingCheckBox(boolean selected) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(selected);
        checkBox.setFocusTraversable(false);
        checkBox.setStyle("-fx-cursor: hand;");
        checkBox.setOnMouseClicked(event -> event.consume());
        return checkBox;
    }

    /**
     * Performs icon.
     * @param text the text.
     * @return the StackPane.
     */
    private StackPane lineIcon(String text) {
        StackPane icon = new StackPane();
        icon.setPrefSize(38, 38);
        icon.setMinSize(38, 38);
        icon.setMaxSize(38, 38);
        icon.setStyle(
                "-fx-background-color: rgba(22,131,255,0.12);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 12;"
        );

        Label symbol = label(text, WHITE, text.length() > 1 ? 13 : 17, true);
        icon.getChildren().add(symbol);

        return icon;
    }

    /**
     * Performs badge.
     * @param text the text.
     * @param color the color.
     * @return the Label.
     */
    private Label badge(String text, String color) {
        Label badge = label(text, WHITE, 11, true);
        badge.setPadding(new Insets(6, 10, 6, 10));
        badge.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 999;"
        );
        return badge;
    }

    /**
     * Performs card.
     * @param padding the padding.
     * @return the VBox.
     */
    private VBox glassCard(double padding) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(padding));
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.16);" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0, 0, 6);"
        );
        return card;
    }

    /**
     * Performs button.
     * @param text the text.
     * @return the Button.
     */
    private Button blueButton(String text) {
        Button button = new Button(text);
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setStyle(
                "-fx-background-color: linear-gradient(to right, " + BLUE_DARK + ", " + BLUE + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    /**
     * Performs button.
     * @param text the text.
     * @return the Button.
     */
    private Button darkButton(String text) {
        Button button = new Button(text);
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);" +
                        "-fx-text-fill: " + LIGHT + ";" +
                        "-fx-border-color: rgba(255,255,255,0.16);" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    /**
     * Returns the full name.
     * @return the String.
     */
    private String getFullName() {
        String firstName = nonEmpty(user.getFirstName(), "");
        String lastName = nonEmpty(user.getLastName(), "");
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "Citoyen" : fullName;
    }

    /**
     * Returns the initials.
     * @param fullName the fullName.
     * @return the String.
     */
    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "C";
        }

        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    /**
     * Performs empty.
     * @param value the value.
     * @param fallback the fallback.
     * @return the String.
     */
    private String nonEmpty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Performs label.
     * @param text the text.
     * @param color the color.
     * @param size the size.
     * @param bold the bold.
     * @return the Label.
     */
    private Label label(String text, String color, int size, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return label;
    }
}
