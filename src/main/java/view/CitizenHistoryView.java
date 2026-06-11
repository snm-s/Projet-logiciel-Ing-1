package view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.Main;
import controller.CitizenPage.CitizenController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
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
import model.simulation.EvacuationEvent;

public class CitizenHistoryView extends BorderPane {

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

    private final CitizenController controller;

    public CitizenHistoryView(CitizenController controller) {
        this.controller = controller;

        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));

        VBox root = new VBox(22);
        root.setStyle("-fx-background-color:transparent;");

        root.getChildren().addAll(
                buildHeader(),
                buildStats(),
                buildHistoryList()
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        setCenter(scroll);
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);

        Label title = label("Historique", WHITE, 28, true);
        Label sub = label(
                "Retrouvez vos alertes générales et uniquement votre historique personnel d'évacuation.",
                LIGHT,
                14,
                false
        );
        sub.setWrapText(true);

        header.getChildren().addAll(title, sub);
        return header;
    }

    private HBox buildStats() {
        List<Alert> alerts = getRealAlerts();
        List<EvacuationEvent> events = getMyEvacuationEvents();

        long active = alerts.stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .count();

        long departures = events.stream()
                .filter(e -> "DEPART".equalsIgnoreCase(e.getType()))
                .count();

        long arrivals = events.stream()
                .filter(e -> "ARRIVEE".equalsIgnoreCase(e.getType()))
                .count();

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                statCard("Alertes reçues", String.valueOf(alerts.size()), BLUE, "Messages généraux"),
                statCard("Actives", String.valueOf(active), RED, "Situations en cours"),
                statCard("Départs", String.valueOf(departures), ORANGE, "Votre trajet lancé"),
                statCard("Arrivées", String.valueOf(arrivals), GREEN, "Votre arrivée refuge")
        );

        for (Node node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        return stats;
    }

    private VBox buildHistoryList() {
        VBox section = glassCard(22);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(4);
        Label title = label("Journal réel", WHITE, 20, true);
        Label subtitle = label(
                "Les événements affichés ici sont filtrés sur votre compte citoyen.",
                LIGHT,
                13,
                false
        );
        subtitle.setWrapText(true);

        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label count = badge((getRealAlerts().size() + getMyEvacuationEvents().size()) + " événement(s)", BLUE);

        header.getChildren().addAll(texts, spacer, count);

        VBox list = new VBox(12);

        List<Alert> alerts = getRealAlerts();
        List<EvacuationEvent> events = getMyEvacuationEvents();

        if (alerts.isEmpty() && events.isEmpty()) {
            list.getChildren().add(emptyBox());
        } else {
            Collections.reverse(alerts);
            Collections.reverse(events);

            for (EvacuationEvent event : events) {
                list.getChildren().add(evacuationHistoryCard(event));
            }

            for (Alert alert : alerts) {
                list.getChildren().add(alertHistoryCard(alert));
            }
        }

        section.getChildren().addAll(header, list);
        return section;
    }

    private VBox evacuationHistoryCard(EvacuationEvent event) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color:" + CARD + ";" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:16;"
        );

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);

        boolean arrived = "ARRIVEE".equalsIgnoreCase(event.getType());
        StackPane icon = smallIcon(arrived ? "OK" : "EV", arrived ? GREEN : ORANGE);

        VBox main = new VBox(4);
        Label type = label(arrived ? "Arrivée au refuge" : "Départ vers refuge", MUTED, 12, true);
        Label desc = label(clean(event.getMessage(), "Événement d'évacuation"), WHITE, 18, true);
        desc.setWrapText(true);
        main.getChildren().addAll(type, desc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = badge(arrived ? "Sécurisé" : "En route", arrived ? GREEN : ORANGE);

        top.getChildren().addAll(icon, main, spacer, status);

        HBox details = new HBox(12);
        details.getChildren().addAll(
                detailBox("Heure", clean(event.getTime(), "--:--")),
                detailBox("Départ", clean(event.getFromZone(), "--")),
                detailBox("Refuge", clean(event.getToZone(), "--")),
                detailBox("Confidentialité", "Visible uniquement par vous")
        );

        for (Node node : details.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        card.getChildren().addAll(top, details);
        return card;
    }

    private VBox alertHistoryCard(Alert alert) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color:" + CARD + ";" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:16;"
        );

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallIcon(typeCode(alert.getType()), severityColor(alert.getSeverity()));

        VBox main = new VBox(4);

        Label type = label(formatType(alert.getType()), MUTED, 12, true);

        Label desc = label(clean(alert.getDescription(), "Alerte sans description"), WHITE, 18, true);
        desc.setWrapText(true);

        main.getChildren().addAll(type, desc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = badge(clean(alert.getStatus(), "--"), statusColor(alert.getStatus()));

        top.getChildren().addAll(icon, main, spacer, status);

        HBox details = new HBox(12);
        details.getChildren().addAll(
                detailBox("Heure", clean(alert.getTime(), "--:--")),
                detailBox("Localisation", clean(alert.getLocalisation(), "Non précisée")),
                detailBox("Sévérité", clean(alert.getSeverity(), "--")),
                detailBox("Origine", clean(alert.getOrigin(), "Système"))
        );

        for (Node node : details.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        card.getChildren().addAll(top, details);
        return card;
    }

    private VBox emptyBox() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(42));
        box.setStyle(
                "-fx-background-color:rgba(255,255,255,0.04);" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:rgba(255,255,255,0.10);" +
                        "-fx-border-radius:16;"
        );

        Label title = label("Aucun événement réel enregistré", WHITE, 18, true);
        Label sub = label(
                "Les alertes et votre historique personnel apparaîtront ici dès que la simulation sera lancée.",
                LIGHT,
                13,
                false
        );
        sub.setWrapText(true);

        box.getChildren().addAll(title, sub);
        return box;
    }

    private List<Alert> getRealAlerts() {
        if (controller == null || controller.getAlertSystem() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(controller.getAlertSystem().getAlerts());
    }

    private List<EvacuationEvent> getMyEvacuationEvents() {
        if (controller == null || Main.currentUser == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(controller.getEvacuationHistoryFor(Main.currentUser));
    }


    private VBox statCard(String title, String value, String color, String subtitle) {
        VBox card = glassCard(18);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane dot = new StackPane();
        dot.setPrefSize(12, 12);
        dot.setMinSize(12, 12);
        dot.setMaxSize(12, 12);
        dot.setStyle("-fx-background-color:" + color + "; -fx-background-radius:999;");

        Label titleLabel = label(title, MUTED, 12, true);
        top.getChildren().addAll(dot, titleLabel);

        Label valueLabel = label(value, WHITE, 28, true);

        Label sub = label(subtitle, LIGHT, 12, false);
        sub.setWrapText(true);

        card.getChildren().addAll(top, valueLabel, sub);
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
        Label v = label(value, WHITE, 13, false);
        v.setWrapText(true);

        box.getChildren().addAll(t, v);
        return box;
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

        Label label = label(text, WHITE, text.length() > 1 ? 13 : 17, true);
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

    private String typeCode(AlertType type) {
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

    private String severityColor(String severity) {
        if (severity == null) return BLUE;

        return switch (severity.toLowerCase()) {
            case "élevée" -> RED;
            case "moyenne" -> ORANGE;
            case "faible" -> BLUE;
            default -> BLUE;
        };
    }

    private String statusColor(String status) {
        if (status == null) return BLUE;

        return switch (status.toLowerCase()) {
            case "active" -> RED;
            case "résolue" -> GREEN;
            case "en attente" -> ORANGE;
            default -> BLUE;
        };
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label label = new Label(text == null ? "" : text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return label;
    }
}
