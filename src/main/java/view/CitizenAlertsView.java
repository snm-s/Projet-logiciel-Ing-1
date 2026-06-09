package view;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.alert.Alert;
import model.enums.AlertType;

public class CitizenAlertsView extends BorderPane {

    private static final String GLASS = "rgba(8, 22, 42, 0.72)";
    private static final String CARD = "rgba(255,255,255,0.055)";
    private static final String BORDER = "rgba(255,255,255,0.16)";
    private static final String BLUE = "#1683ff";
    private static final String GREEN = "#22c55e";
    private static final String RED = "#ef4444";
    private static final String ORANGE = "#f59e0b";
    private static final String LIGHT = "#b8c7dd";
    private static final String MUTED = "#7f91aa";
    private static final String WHITE = "#ffffff";

    private final ObservableList<Alert> allAlerts = FXCollections.observableArrayList();

    private VBox alertsContainer;
    private Label tabToutes;
    private Label tabActives;
    private Label tabResolues;

    private Label totalCount;
    private Label activeCount;
    private Label resolvedCount;
    private Label pendingCount;

    private String currentTab = "Toutes";
    private controller.CitizenPage.CitizenAlertsController controller;

    public CitizenAlertsView() {
        setStyle("-fx-background-color:linear-gradient(to bottom right, #06172b, #0b1a30, #08162a);");
        setCenter(buildContent());
    }

    private VBox buildContent() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color:transparent;");

        HBox header = buildHeader();

        HBox stats = new HBox(16);

        totalCount = statValue("0");
        activeCount = statValue("0");
        resolvedCount = statValue("0");
        pendingCount = statValue("0");

        stats.getChildren().addAll(
                statCard("Total", totalCount, BLUE, "Alertes publiées"),
                statCard("Actives", activeCount, RED, "Situations en cours"),
                statCard("Résolues", resolvedCount, GREEN, "Situations terminées"),
                statCard("Suggestions", pendingCount, ORANGE, "En attente admin")
        );

        for (Node node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        VBox mainCard = glassCard(22);

        HBox topRow = new HBox(18);
        topRow.setAlignment(Pos.CENTER_LEFT);

        HBox tabs = new HBox(22);

        tabToutes = makeTab("Toutes", true);
        tabActives = makeTab("Actives", false);
        tabResolues = makeTab("Résolues", false);

        tabToutes.setOnMouseClicked(e -> switchTab("Toutes"));
        tabActives.setOnMouseClicked(e -> switchTab("Actives"));
        tabResolues.setOnMouseClicked(e -> switchTab("Résolues"));

        tabs.getChildren().addAll(tabToutes, tabActives, tabResolues);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button suggest = blueButton("+ Suggérer une alerte");
        suggest.setOnAction(e -> openSuggestDialog());

        topRow.getChildren().addAll(tabs, spacer, suggest);

        alertsContainer = new VBox(14);

        ScrollPane scroll = new ScrollPane(alertsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        mainCard.getChildren().addAll(topRow, separator(), scroll);
        VBox.setVgrow(mainCard, Priority.ALWAYS);

        root.getChildren().addAll(header, stats, buildTopAlertCard(), mainCard);
        VBox.setVgrow(mainCard, Priority.ALWAYS);

        refreshView();

        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = roundIcon("!", RED, 64);

        VBox texts = new VBox(5);
        Label title = label("Alertes en cours", WHITE, 30, true);
        Label subtitle = label("Flux des alertes publiées par l’administration et mises à jour en temps réel.", LIGHT, 14, false);
        subtitle.setWrapText(true);

        texts.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, texts);

        return header;
    }

    private VBox buildTopAlertCard() {
        VBox card = glassCard(22);

        Alert latest = getLatestActiveAlert();

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = roundIcon(latest != null ? "!" : "i", latest != null ? RED : BLUE, 58);

        VBox texts = new VBox(6);

        Label small = label("Alerte prioritaire", MUTED, 12, true);
        Label title = label(
                latest != null ? latest.getDescription() : "Aucune alerte active",
                WHITE,
                22,
                true
        );
        title.setWrapText(true);

        Label desc = label(
                latest != null
                        ? latest.getLocalisation() + " • " + latest.getSeverity() + " • " + latest.getTime()
                        : "Aucune situation urgente n’est publiée actuellement.",
                LIGHT,
                13,
                false
        );
        desc.setWrapText(true);

        texts.getChildren().addAll(small, title, desc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = badge(latest != null ? latest.getStatus() : "Stable", latest != null ? RED : GREEN);

        row.getChildren().addAll(icon, texts, spacer, status);
        card.getChildren().add(row);

        return card;
    }

    private VBox statCard(String title, Label value, String color, String subtitle) {
        VBox card = glassCard(18);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane dot = new StackPane();
        dot.setPrefSize(12, 12);
        dot.setMinSize(12, 12);
        dot.setMaxSize(12, 12);
        dot.setStyle("-fx-background-color:" + color + "; -fx-background-radius:999;");

        Label titleLabel = label(title, MUTED, 12, true);
        row.getChildren().addAll(dot, titleLabel);

        Label sub = label(subtitle, LIGHT, 12, false);
        sub.setWrapText(true);

        card.getChildren().addAll(row, value, sub);
        return card;
    }

    private Label statValue(String text) {
        return label(text, WHITE, 28, true);
    }

    public void setAlerts(List<Alert> alerts) {
        allAlerts.setAll(alerts);
        refreshView();
    }

    public void refreshView() {
        updateCounts();
        refreshCards();
    }

    public void refreshTable() {
        refreshView();
    }

    private void updateCounts() {
        if (totalCount != null) {
            totalCount.setText(String.valueOf(allAlerts.size()));
        }

        if (activeCount != null) {
            activeCount.setText(String.valueOf(countByStatus("Active")));
        }

        if (resolvedCount != null) {
            resolvedCount.setText(String.valueOf(countByStatus("Résolue")));
        }

        if (pendingCount != null && controller != null) {
            pendingCount.setText(String.valueOf(controller.getPendingSuggestionsCount()));
        }
    }

    private long countByStatus(String status) {
        return allAlerts.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .count();
    }

    private void refreshCards() {
        if (alertsContainer == null) return;

        alertsContainer.getChildren().clear();

        ObservableList<Alert> filtered = FXCollections.observableArrayList();

        switch (currentTab) {
            case "Actives" -> allAlerts.stream()
                    .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                    .forEach(filtered::add);

            case "Résolues" -> allAlerts.stream()
                    .filter(a -> "Résolue".equalsIgnoreCase(a.getStatus()))
                    .forEach(filtered::add);

            default -> filtered.addAll(allAlerts);
        }

        if (filtered.isEmpty()) {
            alertsContainer.getChildren().add(emptyState());
            return;
        }

        for (Alert alert : filtered) {
            alertsContainer.getChildren().add(alertCard(alert));
        }
    }

    private VBox alertCard(Alert alert) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color:" + CARD + ";" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:16;"
        );

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallIcon(getTypeCode(alert.getType()), getSeverityColor(alert.getSeverity()));

        VBox titleBox = new VBox(4);
        Label type = label(formatType(alert.getType()), MUTED, 12, true);

        Label description = label(alert.getDescription(), WHITE, 18, true);
        description.setWrapText(true);

        titleBox.getChildren().addAll(type, description);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = badge(alert.getStatus(), getStatusColor(alert.getStatus()));

        top.getChildren().addAll(icon, titleBox, spacer, status);

        HBox details = new HBox(12);
        details.getChildren().addAll(
                detailBox("Localisation", alert.getLocalisation()),
                detailBox("Sévérité", alert.getSeverity()),
                detailBox("Heure", alert.getTime())
        );

        for (Node node : details.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        card.getChildren().addAll(top, details);
        return card;
    }

    private VBox detailBox(String title, String value) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(12));
        box.setStyle(
                "-fx-background-color:rgba(255,255,255,0.045);" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:rgba(255,255,255,0.08);" +
                        "-fx-border-radius:12;"
        );

        Label t = label(title, MUTED, 11, true);
        Label v = label(value == null || value.isBlank() ? "--" : value, WHITE, 13, false);
        v.setWrapText(true);

        box.getChildren().addAll(t, v);
        return box;
    }

    private VBox emptyState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(44));
        box.setStyle(
                "-fx-background-color:rgba(255,255,255,0.04);" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:rgba(255,255,255,0.10);" +
                        "-fx-border-radius:16;"
        );

        Label title = label("Aucune alerte à afficher", WHITE, 18, true);
        Label sub = label("Les alertes publiées apparaîtront ici automatiquement.", LIGHT, 13, false);

        box.getChildren().addAll(title, sub);
        return box;
    }

    private Label makeTab(String text, boolean active) {
        Label tab = label(text, active ? WHITE : MUTED, 13, true);
        tab.setCursor(Cursor.HAND);
        styleTab(tab, active);
        return tab;
    }

    private void switchTab(String tab) {
        currentTab = tab;

        styleTab(tabToutes, "Toutes".equals(tab));
        styleTab(tabActives, "Actives".equals(tab));
        styleTab(tabResolues, "Résolues".equals(tab));

        refreshView();
    }

    private void styleTab(Label tab, boolean active) {
        tab.setTextFill(Color.web(active ? WHITE : MUTED));
        tab.setPadding(new Insets(10, 14, 10, 14));
        tab.setStyle(
                active
                        ? "-fx-background-color:rgba(22,131,255,0.18);" +
                        "-fx-background-radius:999;" +
                        "-fx-border-color:" + BLUE + ";" +
                        "-fx-border-radius:999;" +
                        "-fx-font-weight:bold;"
                        : "-fx-background-color:transparent;" +
                        "-fx-background-radius:999;" +
                        "-fx-font-weight:bold;"
        );
    }

    private Alert getLatestActiveAlert() {
        for (int i = allAlerts.size() - 1; i >= 0; i--) {
            Alert alert = allAlerts.get(i);

            if ("Active".equalsIgnoreCase(alert.getStatus())) {
                return alert;
            }
        }

        return null;
    }

    private void openSuggestDialog() {
        Dialog<Alert> dialog = new Dialog<>();
        dialog.setTitle("Suggérer une alerte");
        dialog.setHeaderText("Votre signalement sera envoyé à l’administrateur pour validation.");

        ComboBox<AlertType> typeField = new ComboBox<>(FXCollections.observableArrayList(AlertType.values()));
        typeField.setPrefWidth(260);

        TextField descriptionField = field("Description de la situation");
        TextField localisationField = field("Localisation");

        ComboBox<String> severityField = new ComboBox<>(FXCollections.observableArrayList("Élevée", "Moyenne", "Faible"));
        severityField.setPrefWidth(260);

        TextField timeField = field("Heure constatée, exemple : 14:30");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.addRow(0, formLabel("Type"), typeField);
        grid.addRow(1, formLabel("Description"), descriptionField);
        grid.addRow(2, formLabel("Localisation"), localisationField);
        grid.addRow(3, formLabel("Sévérité"), severityField);
        grid.addRow(4, formLabel("Heure"), timeField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Envoyer");
        ok.setStyle("-fx-background-color:" + BLUE + "; -fx-text-fill:white; -fx-font-weight:bold;");

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;

            return new Alert(
                    typeField.getValue() == null ? AlertType.INFO : typeField.getValue(),
                    descriptionField.getText().isBlank() ? "Situation signalée" : descriptionField.getText().trim(),
                    localisationField.getText().isBlank() ? "Localisation inconnue" : localisationField.getText().trim(),
                    severityField.getValue() == null ? "Faible" : severityField.getValue(),
                    timeField.getText().isBlank() ? "--:--" : timeField.getText().trim(),
                    "En attente",
                    "suggestion"
            );
        });

        dialog.showAndWait().ifPresent(alert -> {
            if (controller != null) {
                controller.submitSuggestion(alert);
                showConfirmation();
            }
        });
    }

    private void showConfirmation() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Suggestion envoyée");
        dialog.setHeaderText(null);

        Label message = new Label("Votre suggestion a bien été transmise à l’administrateur. Elle sera visible après validation.");
        message.setWrapText(true);
        message.setPadding(new Insets(16));
        message.setStyle("-fx-font-size:13px; -fx-text-fill:#1a2744;");

        dialog.getDialogPane().setContent(message);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setStyle("-fx-background-color:#1683ff; -fx-text-fill:white; -fx-font-weight:bold;");

        dialog.showAndWait();
    }

    private TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(260);
        return field;
    }

    private Label formLabel(String text) {
        Label label = new Label(text + " :");
        label.setMinWidth(100);
        label.setStyle("-fx-font-size:13px; -fx-text-fill:#1a2744; -fx-font-weight:bold;");
        return label;
    }

    private String getTypeCode(AlertType type) {
        if (type == null) return "IF";

        return switch (type.toString().toUpperCase()) {
            case "INONDATION" -> "IN";
            case "ROUTE" -> "RT";
            case "EVACUATION" -> "EV";
            case "AUTRE" -> "AU";
            default -> "IF";
        };
    }

    private String formatType(AlertType type) {
        if (type == null) return "Information";

        return switch (type.toString().toUpperCase()) {
            case "INONDATION" -> "Inondation";
            case "ROUTE" -> "Route";
            case "EVACUATION" -> "Évacuation";
            case "AUTRE" -> "Autre";
            default -> "Information";
        };
    }

    private String getSeverityColor(String severity) {
        if (severity == null) return BLUE;

        return switch (severity.toLowerCase()) {
            case "élevée" -> RED;
            case "moyenne" -> ORANGE;
            default -> BLUE;
        };
    }

    private String getStatusColor(String status) {
        if (status == null) return MUTED;

        return switch (status.toLowerCase()) {
            case "active" -> GREEN;
            case "résolue" -> MUTED;
            case "en attente" -> ORANGE;
            default -> BLUE;
        };
    }

    private StackPane roundIcon(String text, String color, int size) {
        StackPane icon = new StackPane();
        icon.setPrefSize(size, size);
        icon.setMinSize(size, size);
        icon.setMaxSize(size, size);
        icon.setStyle(
                "-fx-background-color:" + color + ";" +
                        "-fx-background-radius:999;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.25), 18, 0, 0, 6);"
        );

        Label label = label(text, WHITE, size >= 60 ? 28 : 18, true);
        icon.getChildren().add(label);

        return icon;
    }

    private StackPane smallIcon(String text, String color) {
        StackPane icon = new StackPane();
        icon.setPrefSize(42, 42);
        icon.setMinSize(42, 42);
        icon.setMaxSize(42, 42);
        icon.setStyle(
                "-fx-background-color:rgba(255,255,255,0.06);" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + color + ";" +
                        "-fx-border-radius:14;"
        );

        Label label = label(text, WHITE, 14, true);
        icon.getChildren().add(label);

        return icon;
    }

    private Label badge(String text, String color) {
        Label badge = label(text == null ? "--" : text, WHITE, 11, true);
        badge.setPadding(new Insets(6, 11, 6, 11));
        badge.setStyle(
                "-fx-background-color:" + color + ";" +
                        "-fx-background-radius:999;"
        );
        return badge;
    }

    private Separator separator() {
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color:rgba(255,255,255,0.12);");
        return separator;
    }

    private VBox glassCard(int padding) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(padding));
        box.setStyle(
                "-fx-background-color:" + GLASS + ";" +
                        "-fx-background-radius:18;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:18;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.30), 24, 0, 0, 8);"
        );
        return box;
    }

    private Button blueButton(String text) {
        Button button = new Button(text);
        button.setPadding(new Insets(11, 18, 11, 18));
        button.setStyle(
                "-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff);" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-font-weight:bold;" +
                        "-fx-cursor:hand;"
        );
        return button;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return label;
    }

    public ObservableList<Alert> getAllAlerts() {
        return allAlerts;
    }

    public TableView<Alert> getTable() {
        return new TableView<>();
    }

    public void setController(controller.CitizenPage.CitizenAlertsController controller) {
        this.controller = controller;
        updateCounts();
    }
}
