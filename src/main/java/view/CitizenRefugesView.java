package view;

import java.util.List;
import java.util.function.Consumer;

import controller.CitizenPage.CitizenController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.agent.Agent;
import model.zone.Shelter;
import model.zone.Zone;

public class CitizenRefugesView extends BorderPane {

    private static final String WHITE = "#ffffff";
    private static final String LIGHT = "#b8c7dd";
    private static final String MUTED = "#7f91aa";
    private static final String BLUE = "#1683ff";
    private static final String BLUE_DARK = "#0e73eb";
    private static final String GREEN = "#22c55e";
    private static final String RED = "#ef4444";
    private static final String ORANGE = "#f59e0b";
    private static final String CARD_BG = "rgba(8,22,42,0.72)";

    public CitizenRefugesView(CitizenController controller, Agent user, Consumer<Zone> routeAction) {
        setStyle("-fx-background-color: transparent;");
        setPadding(new Insets(0));

        VBox page = new VBox(22);
        page.setPadding(new Insets(30));

        VBox header = new VBox(6);
        Label title = label("Refuges", WHITE, 28, true);
        Label subtitle = label(
                "Refuges officiels extraits de zones.json, classés selon votre position actuelle.",
                LIGHT,
                14,
                false
        );
        header.getChildren().addAll(title, subtitle);

        List<Shelter> shelters = controller.getSheltersSortedByDistance(user);
        Zone nearestShelter = controller.getNearestSafeRefuge(user);

        if (shelters.isEmpty()) {
            page.getChildren().addAll(header, emptyCard());
        } else {
            page.getChildren().addAll(
                    header,
                    nearestRefugeCard(controller, user, nearestShelter, routeAction),
                    buildRefugeList(controller, user, shelters, nearestShelter, routeAction)
            );
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(scroll);
    }

    private VBox nearestRefugeCard(CitizenController controller, Agent user, Zone nearestShelter, Consumer<Zone> routeAction) {
        VBox card = glassCard(22);

        HBox top = new HBox(18);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = bigIcon("⌂", GREEN);

        VBox textBox = new VBox(6);
        Label label = label("Refuge le plus proche", MUTED, 12, true);
        Label name = label(nearestShelter != null ? nearestShelter.getName() : "Aucun refuge disponible", WHITE, 24, true);

        Label desc = label(
                nearestShelter != null ? controller.shortDescription(nearestShelter) : "Aucun refuge accessible n’a été trouvé.",
                LIGHT,
                14,
                false
        );
        desc.setWrapText(true);

        textBox.getChildren().addAll(label, name, desc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button routeButton = blueButton("Voir l’itinéraire");
        routeButton.setDisable(nearestShelter == null);
        routeButton.setOnAction(e -> {
            if (nearestShelter != null) {
                routeAction.accept(nearestShelter);
            }
        });

        top.getChildren().addAll(icon, textBox, spacer, routeButton);

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
                statBox("Distance", nearestShelter != null ? controller.getDistanceFromUserLabel(user, nearestShelter) : "--"),
                statBox("Temps estimé", nearestShelter != null ? controller.getEtaToZoneLabel(user, nearestShelter) : "--"),
                statBox("Altitude", nearestShelter != null ? String.format("%.0f m", nearestShelter.getAltitude()) : "--"),
                statBox("Capacité", nearestShelter instanceof Shelter s ? s.getCapacity() + " places" : "--")
        );

        for (javafx.scene.Node n : stats.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        card.getChildren().addAll(top, stats);
        return card;
    }

    private VBox buildRefugeList(
            CitizenController controller,
            Agent user,
            List<Shelter> shelters,
            Zone nearestShelter,
            Consumer<Zone> routeAction
    ) {
        VBox section = new VBox(12);

        Label title = label("Tous les refuges disponibles", WHITE, 18, true);
        Label subtitle = label("Les refuges sont triés du plus proche au plus éloigné.", LIGHT, 13, false);

        FlowPane list = new FlowPane();
        list.setHgap(16);
        list.setVgap(16);

        for (Shelter shelter : shelters) {
            boolean nearest = nearestShelter != null && shelter.getId() == nearestShelter.getId();
            list.getChildren().add(refugeCard(controller, user, shelter, nearest, routeAction));
        }

        section.getChildren().addAll(title, subtitle, list);
        return section;
    }

    private VBox refugeCard(
            CitizenController controller,
            Agent user,
            Shelter shelter,
            boolean nearest,
            Consumer<Zone> routeAction
    ) {
        boolean danger = shelter.isFlooded() || shelter.isEvacuated();

        VBox card = new VBox(14);
        card.setPrefWidth(320);
        card.setMinHeight(250);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + (nearest ? "rgba(34,197,94,0.70)" : "rgba(255,255,255,0.16)") + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: " + (nearest ? "1.6" : "1") + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.24), 20, 0, 0, 7);"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallIcon(nearest ? "✓" : "⌂", danger ? RED : nearest ? GREEN : BLUE);

        VBox titles = new VBox(4);
        Label name = label(shelter.getName(), WHITE, 16, true);
        name.setWrapText(true);

        Label status = label(
                danger ? "Inaccessible / à éviter" : nearest ? "Refuge recommandé" : "Accessible",
                danger ? RED : nearest ? GREEN : BLUE,
                12,
                true
        );

        titles.getChildren().addAll(name, status);
        header.getChildren().addAll(icon, titles);

        Label desc = label(controller.shortDescription(shelter), LIGHT, 12, false);
        desc.setWrapText(true);

        VBox infos = new VBox(8);
        infos.getChildren().addAll(
                infoLine("Distance", controller.getDistanceFromUserLabel(user, shelter)),
                infoLine("Temps estimé", controller.getEtaToZoneLabel(user, shelter)),
                infoLine("Altitude", String.format("%.0f m", shelter.getAltitude())),
                infoLine("Capacité", shelter.getCapacity() + " places"),
                infoLine("Occupation", shelter.getCurrentOccupancy() + " personne")
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btn = danger ? disabledButton("Indisponible") : blueButton("Voir l’itinéraire");
        btn.setDisable(danger);
        btn.setOnAction(e -> routeAction.accept(shelter));

        card.getChildren().addAll(header, desc, infos, spacer, btn);
        return card;
    }

    private HBox infoLine(String key, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label k = label(key + " :", MUTED, 12, true);
        k.setMinWidth(88);

        Label v = label(value, WHITE, 12, false);
        v.setWrapText(true);

        row.getChildren().addAll(k, v);
        return row;
    }

    private VBox emptyCard() {
        VBox card = glassCard(20);
        card.getChildren().addAll(
                label("Aucun refuge trouvé", WHITE, 18, true),
                label("Aucune zone de type shelter n’a été trouvée dans zones.json.", LIGHT, 13, false)
        );
        return card;
    }

    private VBox statBox(String title, String value) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(13));
        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.045);" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-radius: 14;"
        );

        Label v = label(value, WHITE, 17, true);
        Label t = label(title, MUTED, 12, true);

        box.getChildren().addAll(v, t);
        return box;
    }

    private StackPane bigIcon(String text, String color) {
        StackPane icon = new StackPane();
        icon.setPrefSize(72, 72);
        icon.setMinSize(72, 72);
        icon.setMaxSize(72, 72);
        icon.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0, 0, 6);"
        );

        Label symbol = label(text, WHITE, 30, true);
        icon.getChildren().add(symbol);

        return icon;
    }

    private StackPane smallIcon(String text, String color) {
        StackPane icon = new StackPane();
        icon.setPrefSize(42, 42);
        icon.setMinSize(42, 42);
        icon.setMaxSize(42, 42);
        icon.setStyle(
                "-fx-background-color: rgba(22,131,255,0.12);" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-radius: 14;"
        );

        Label symbol = label(text, WHITE, 18, true);
        icon.getChildren().add(symbol);

        return icon;
    }

    private VBox glassCard(double padding) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(padding));
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: rgba(255,255,255,0.16);" +
                        "-fx-border-radius: 22;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 22, 0, 0, 8);"
        );
        return card;
    }

    private Button blueButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
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

    private Button disabledButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-text-fill: #64748b;" +
                        "-fx-background-radius: 10;"
        );
        return button;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return label;
    }
}