package view;

import java.awt.Desktop;
import java.net.URI;

import app.Main;
import controller.CitizenPage.CitizenController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

public class CitizenProfileView extends BorderPane {

    private static final String BLUE = "#1683ff";
    private static final String BLUE_DARK = "#0e73eb";
    private static final String WHITE = "#ffffff";
    private static final String LIGHT = "#b8c7dd";
    private static final String MUTED = "#7f91aa";
    private static final String GREEN = "#22c55e";
    private static final String RED = "#ef4444";

    /**
     * Create a profile view for the given citizen.
     *
     * @param controller controller providing simulation data
     * @param user       agent representing the citizen shown in this view
     */
    public CitizenProfileView(CitizenController controller, Agent user) {
        setStyle("-fx-background-color: transparent;");

        VBox page = new VBox(22);
        page.setPadding(new Insets(30));

        VBox header = new VBox(6);
        Label title = label("Mon profil", WHITE, 28, true);
        Label subtitle = label("Vos informations personnelles et votre situation d’évacuation en temps réel.", LIGHT, 14, false);
        header.getChildren().addAll(title, subtitle);

        HBox top = new HBox(18);
        top.getChildren().addAll(
                buildIdentityCard(controller, user),
                buildSafetyCard(controller, user)
        );

        HBox.setHgrow(top.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);

        HBox infoSection = new HBox(18);
        infoSection.getChildren().addAll(
                buildPersonalInfoCard(controller, user),
                buildEvacuationCard(controller, user)
        );

        HBox.setHgrow(infoSection.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(infoSection.getChildren().get(1), Priority.ALWAYS);

        VBox bottomCard = buildAdviceAndEmergencyCard();

        page.getChildren().addAll(header, top, infoSection, bottomCard);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 0;"
        );

        setCenter(scroll);
    }

    /**
     * Builds identity card.
     * @param controller the controller.
     * @param user the user.
     * @return the VBox.
     */
    private VBox buildIdentityCard(CitizenController controller, Agent user) {
        VBox card = glassCard(22);
        card.setMinHeight(210);

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle circle = new Circle(42);
        circle.setFill(Color.web(controller.isCitizenInPanic(user) ? RED : BLUE));
        circle.setStyle("-fx-effect: dropshadow(gaussian, rgba(22,131,255,0.35), 20, 0, 0, 6);");

        Label avatarIcon = label(getInitials(controller.getFullName(user)), WHITE, 20, true);
        avatar.getChildren().addAll(circle, avatarIcon);

        VBox nameBox = new VBox(6);
        Label name = label(controller.getFullName(user), WHITE, 24, true);
        Label role = label("Citoyen inscrit sur la plateforme d’évacuation", LIGHT, 13, false);

        HBox badges = new HBox(8);
        badges.getChildren().addAll(
                badge("Compte actif", GREEN),
                badge(controller.getCitizenState(user), controller.isCitizenInPanic(user) ? RED : BLUE)
        );

        nameBox.getChildren().addAll(name, role, badges);
        row.getChildren().addAll(avatar, nameBox);

        VBox zoneBox = new VBox(6);
        zoneBox.setPadding(new Insets(18, 0, 0, 0));

        Label zoneTitle = label("Zone actuelle", MUTED, 12, true);
        Label zoneValue = label(controller.getDetailedPositionLabel(user), WHITE, 15, false);
        zoneValue.setWrapText(true);

        zoneBox.getChildren().addAll(zoneTitle, zoneValue);

        card.getChildren().addAll(row, zoneBox);
        return card;
    }

    /**
     * Builds safety card.
     * @param controller the controller.
     * @param user the user.
     * @return the VBox.
     */
    private VBox buildSafetyCard(CitizenController controller, Agent user) {
        VBox card = glassCard(22);
        card.setMinHeight(210);
    
        Label title = label("État de sécurité", WHITE, 18, true);
    
        HBox main = new HBox(16);
        main.setAlignment(Pos.CENTER_LEFT);
    
        StackPane indicator = new StackPane();
    
        Circle outer = new Circle(38);
        outer.setFill(controller.isCitizenInPanic(user)
                ? Color.rgb(239, 68, 68, 0.18)
                : Color.rgb(34, 197, 94, 0.18));
    
        Circle inner = new Circle(24);
        inner.setFill(Color.web(controller.isCitizenInPanic(user) ? RED : GREEN));
    
        Label icon = label(controller.isCitizenInPanic(user) ? "!" : "✓", WHITE, 22, true);
        indicator.getChildren().addAll(outer, inner, icon);
    
        VBox status = new VBox(5);
        Label state = label(controller.isCitizenInPanic(user) ? "Situation à surveiller" : "Situation stable", WHITE, 20, true);
        Label desc = label(
                controller.isCitizenInPanic(user)
                        ? "Votre état indique une situation de stress. Suivez les consignes d’évacuation."
                        : "Aucune situation critique détectée pour votre profil actuellement.",
                LIGHT,
                13,
                false
        );
        desc.setWrapText(true);
    
        status.getChildren().addAll(state, desc);
        main.getChildren().addAll(indicator, status);
    
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
                miniStat("Alertes", String.valueOf(controller.getAlertCount())),
                miniStat("Distance", controller.getDistanceLabel(user)),
                miniStat("Temps", controller.getEtaLabel(user))
        );
    
        for (javafx.scene.Node n : stats.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
    
        card.getChildren().addAll(title, main, stats);
        return card;
    }

    /**
     * Builds personal info card.
     * @param controller the controller.
     * @param user the user.
     * @return the VBox.
     */
    private VBox buildPersonalInfoCard(CitizenController controller, Agent user) {
    VBox card = glassCard(20);

    Label title = label("Informations personnelles", WHITE, 18, true);
    Label subtitle = label("Données utilisées pour vous identifier pendant la simulation.", LIGHT, 13, false);

    VBox rows = new VBox(14);
    rows.setPadding(new Insets(8, 0, 0, 0));

    CheckBox pmrCheck = new CheckBox();
    pmrCheck.setFocusTraversable(false);

    if (user instanceof Citizen citizen) {
        pmrCheck.setSelected(citizen.isMobilityReduced());
    }

    rows.getChildren().addAll(
            detailRow("Nom complet", controller.getFullName(user)),
            detailRow("Email", user != null && user.getEmail() != null ? user.getEmail() : "Non renseigné"),
            detailRow("Téléphone", user != null && user.getPhone() != null ? user.getPhone() : "Non renseigné"),
            detailRow("Ville", user != null && user.getCity() != null ? user.getCity() : "Non renseignée"),
            pmrRow("Mobilité réduite / PMR", pmrCheck)
    );

    Button saveBtn = new Button("Enregistrer");
    saveBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, " + BLUE_DARK + ", " + BLUE + ");" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
    );

    Label status = label("", GREEN, 12, true);

    saveBtn.setOnAction(e -> {
        if (user instanceof Citizen citizen) {
            citizen.setMobilityReduced(pmrCheck.isSelected());

            boolean saved = UserService.updateAgent(citizen);

            if (saved) {
                Main.currentUser = citizen;
                status.setText("Profil enregistré ✓");
                status.setTextFill(Color.web(GREEN));
            } else {
                status.setText("Erreur : impossible d’enregistrer.");
                status.setTextFill(Color.web(RED));
            }
        }
    });

    HBox saveRow = new HBox(12, saveBtn, status);
    saveRow.setAlignment(Pos.CENTER_LEFT);

    card.getChildren().addAll(title, subtitle, rows, saveRow);
    return card;
}

    /**
     * Builds evacuation card.
     * @param controller the controller.
     * @param user the user.
     * @return the VBox.
     */
    private VBox buildEvacuationCard(CitizenController controller, Agent user) {
        VBox card = glassCard(20);

        Label title = label("Plan d’évacuation", WHITE, 18, true);
        Label subtitle = label("Résumé de votre trajet vers la zone sécurisée la plus proche.", LIGHT, 13, false);

        VBox rows = new VBox(14);
        rows.setPadding(new Insets(8, 0, 0, 0));

        rows.getChildren().addAll(
                detailRow("Position", controller.getPositionLabel(user)),
                detailRow("Refuge conseillé", controller.getTargetRefugeLabel(user)),
                detailRow("Distance estimée", controller.getDistanceLabel(user)),
                detailRow("Temps estimé", controller.getEtaLabel(user)),
                detailRow("Statut du trajet", controller.getRouteStatusLabel(user))
        );

        card.getChildren().addAll(title, subtitle, rows);
        return card;
    }

    /**
     * Builds advice and emergency card.
     * @return the VBox.
     */
    private VBox buildAdviceAndEmergencyCard() {
        VBox bottomCard = glassCard(20);

        Label bottomTitle = label("Conseils et numéros d’urgence", WHITE, 18, true);
        Label advice = label(
                "Gardez votre téléphone chargé, suivez l’itinéraire conseillé et évitez les zones signalées comme inondées ou congestionnées.",
                LIGHT,
                14,
                false
        );
        advice.setWrapText(true);

        HBox adviceRow = new HBox(14);
        adviceRow.getChildren().addAll(
                smallAdvice("⌖", "Position", "Restez dans une zone sûre."),
smallAdvice("⚠", "Alertes", "Consultez les consignes officielles."),
                smallAdvice("↗", "Refuge", "Rejoignez le refuge conseillé si nécessaire.")
        );

        for (javafx.scene.Node n : adviceRow.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        VBox emergencyBox = new VBox(12);
        emergencyBox.setPadding(new Insets(16));
        emergencyBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.045);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 18;"
        );

        HBox emergencyHeader = new HBox(10);
        emergencyHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane emergencyIcon = lineIcon("!");
        Label emergencyTitle = label("Numéros utiles", WHITE, 15, true);
        Label emergencySubtitle = label("Cliquez sur un numéro pour lancer l’appel si votre appareil le permet.", LIGHT, 12, false);

        VBox emergencyTexts = new VBox(2);
        emergencyTexts.getChildren().addAll(emergencyTitle, emergencySubtitle);

        emergencyHeader.getChildren().addAll(emergencyIcon, emergencyTexts);

        HBox emergencyLinks = new HBox(10);
        emergencyLinks.setAlignment(Pos.CENTER_LEFT);
        emergencyLinks.getChildren().addAll(
                emergencyLink("112", "Urgence européenne", "tel:112"),
                emergencyLink("15", "SAMU", "tel:15"),
                emergencyLink("18", "Pompiers", "tel:18"),
                emergencyLink("17", "Police", "tel:17"),
                emergencyLink("114", "SMS urgence", "https://www.info.urgence114.fr")
        );

        emergencyBox.getChildren().addAll(emergencyHeader, emergencyLinks);

        bottomCard.getChildren().addAll(bottomTitle, advice, adviceRow, emergencyBox);
        return bottomCard;
    }

    /**
     * Performs row.
     * @param key the key.
     * @param value the value.
     * @return the HBox.
     */
    private HBox detailRow(String key, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.045);" +
                        "-fx-background-radius: 12;"
        );

        Label k = label(key, LIGHT, 13, true);
        k.setMinWidth(135);

        Label v = label(value, WHITE, 13, false);
        v.setWrapText(true);

        row.getChildren().addAll(k, v);
        HBox.setHgrow(v, Priority.ALWAYS);

        return row;
    }

    /**
     * Performs row.
     * @param key the key.
     * @param checkBox the checkBox.
     * @return the HBox.
     */
    private HBox pmrRow(String key, CheckBox checkBox) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.045);" +
                "-fx-background-radius: 12;"
        );
    
        Label k = label(key, LIGHT, 13, true);
        k.setMinWidth(135);
    
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
    
        row.getChildren().addAll(k, spacer, checkBox);
        row.setOnMouseClicked(e -> checkBox.setSelected(!checkBox.isSelected()));
    
        return row;
    }

    /**
     * Performs advice.
     * @param iconText the iconText.
     * @param title the title.
     * @param text the text.
     * @return the VBox.
     */
    private VBox smallAdvice(String iconText, String title, String text) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(18));
        box.setMinHeight(145);
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.045);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-radius: 18;"
        );

        StackPane icon = lineIcon(iconText);

        Label t = label(title, WHITE, 14, true);
        Label d = label(text, LIGHT, 12, false);
        d.setWrapText(true);

        box.getChildren().addAll(icon, t, d);
        return box;
    }

    /**
     * Performs link.
     * @param number the number.
     * @param description the description.
     * @param uri the uri.
     * @return the Hyperlink.
     */
    private Hyperlink emergencyLink(String number, String description, String uri) {
        Hyperlink link = new Hyperlink(number + " · " + description);
        link.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        link.setTextFill(Color.web(WHITE));
        link.setPadding(new Insets(8, 12, 8, 12));
        link.setStyle(
                "-fx-background-color: linear-gradient(to right, " + BLUE + ", " + BLUE_DARK + ");" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 999;" +
                        "-fx-text-fill: white;" +
                        "-fx-underline: false;"
        );

        link.setOnAction(event -> openLink(uri));
        return link;
    }

    /**
     * Performs link.
     * @param uri the uri.
     */
    private void openLink(String uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(uri));
            }
        } catch (Exception e) {
            System.out.println("Impossible d’ouvrir le lien : " + uri);
        }
    }

    /**
     * Performs icon.
     * @param text the text.
     * @return the StackPane.
     */
    private StackPane lineIcon(String text) {
        StackPane icon = new StackPane();
        icon.setPrefSize(34, 34);
        icon.setMinSize(34, 34);
        icon.setMaxSize(34, 34);
        icon.setStyle(
                "-fx-background-color: rgba(255,255,255,0.035);" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: rgba(255,255,255,0.75);" +
                        "-fx-border-radius: 999;" +
                        "-fx-border-width: 1.2;"
        );

        Label symbol = label(text, WHITE, 17, true);
        icon.getChildren().add(symbol);

        return icon;
    }

    /**
     * Performs stat.
     * @param key the key.
     * @param value the value.
     * @return the VBox.
     */
    private VBox miniStat(String key, String value) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(12));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.055);" +
                        "-fx-background-radius: 14;"
        );

        Label v = label(value, WHITE, 18, true);
        Label k = label(key, MUTED, 12, true);

        box.getChildren().addAll(v, k);
        return box;
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
                        "-fx-background-radius: 999;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 3);"
        );
        return badge;
    }

    /**
     * Performs card.
     * @param padding the padding.
     * @return the VBox.
     */
    private VBox glassCard(double padding) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(padding));
        card.setStyle(
                "-fx-background-color: rgba(8, 22, 42, 0.78);" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: rgba(255,255,255,0.14);" +
                        "-fx-border-radius: 22;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 22, 0, 0, 8);"
        );
        return card;
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
