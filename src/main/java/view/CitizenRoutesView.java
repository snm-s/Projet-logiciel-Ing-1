package view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import controller.CitizenPage.CitizenController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import model.agent.Agent;
import model.zone.Shelter;
import model.zone.Zone;

public class CitizenRoutesView extends BorderPane {

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

    private final java.util.function.Consumer<RouteRequest> onShowOnMap;
    private static final List<RouteItem> ROUTE_HISTORY = new ArrayList<>();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final CitizenController controller;
    private final Agent user;
    private final MapView mapView;

    private final Zone from;
    private final Zone recommendedRefuge;

    private VBox recentRoutesBox;
    private Label historyCountLabel;
    private VBox root;

    public CitizenRoutesView(CitizenController controller, Agent user, MapView mapView, java.util.function.Consumer<RouteRequest> onShowOnMap) {
        this.controller = controller;
        this.user = user;
        this.mapView = mapView;
        this.onShowOnMap = onShowOnMap;

        this.from = controller.getNearestZone(user);
        this.recommendedRefuge = controller.getNearestSafeRefuge(user);

        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));

        root = new VBox(22);
        root.setStyle("-fx-background-color:transparent;");

        root.getChildren().addAll(
                buildHeader(),
                buildRecommendedRoute(),
                buildPossibleRoutes(),
                buildRecentRoutes()
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        setCenter(scroll);
    }


    public CitizenRoutesView(CitizenController controller, Agent user, MapView mapView) {
        this(controller, user, mapView, null);
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);

        Label title = label("Mes trajets", WHITE, 28, true);
        Label subtitle = label(
                "Consultez votre trajet recommandé, les autres itinéraires possibles et les trajets déjà ouverts.",
                LIGHT,
                14,
                false
        );
        subtitle.setWrapText(true);

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox buildRecommendedRoute() {
        VBox card = glassCard(24);

        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = roundIcon(recommendedRefuge != null ? "⇢" : "!", recommendedRefuge != null ? BLUE : RED, 62);

        VBox texts = new VBox(5);
        Label small = label("Trajet recommandé", MUTED, 12, true);

        Label title = label(
                recommendedRefuge != null ? fromName() + " → " + recommendedRefuge.getName() : "Aucun trajet recommandé",
                WHITE,
                24,
                true
        );
        title.setWrapText(true);

        Label desc = label(
                recommendedRefuge != null
                        ? "Ce trajet correspond au refuge conseillé pour votre position actuelle."
                        : "Aucun refuge sûr n’est disponible pour générer un trajet.",
                LIGHT,
                13,
                false
        );
        desc.setWrapText(true);

        texts.getChildren().addAll(small, title, desc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = badge(recommendedRefuge != null ? "Recommandé" : "Indisponible", recommendedRefuge != null ? GREEN : RED);

        top.getChildren().addAll(icon, texts, spacer, status);

        HBox info = new HBox(12);
        info.getChildren().addAll(
                detailBox("Départ", fromName(), BLUE),
                detailBox("Arrivée", recommendedRefuge != null ? recommendedRefuge.getName() : "Aucun refuge", GREEN),
                detailBox("Distance", controller.getDistanceLabel(user), WHITE),
                detailBox("Temps estimé", controller.getEtaLabel(user), ORANGE)
        );

        for (Node node : info.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button show = blueButton("Afficher ce trajet sur la carte");
        show.setDisable(from == null || recommendedRefuge == null);
        show.setOnAction(e -> openRoute(recommendedRefuge, true));

        actions.getChildren().add(show);

        card.getChildren().addAll(top, info, actions);
        return card;
    }

    private VBox buildPossibleRoutes() {
        VBox card = glassCard(22);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(4);
        Label title = label("Autres trajets possibles", WHITE, 20, true);
        Label subtitle = label("Autres refuges accessibles selon les zones disponibles.", LIGHT, 13, false);
        subtitle.setWrapText(true);
        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        List<Zone> alternatives = getAlternativeRefuges();
        Label count = badge(alternatives.size() + (alternatives.size() > 1 ? " options" : " option"), BLUE);

        header.getChildren().addAll(texts, spacer, count);

        VBox list = new VBox(12);

        if (alternatives.isEmpty()) {
            list.getChildren().add(emptyBox(
                    "Aucun autre trajet disponible",
                    "Le refuge recommandé est actuellement la seule option sûre trouvée."
            ));
        } else {
            for (Zone refuge : alternatives) {
                list.getChildren().add(possibleRouteCard(refuge));
            }
        }

        card.getChildren().addAll(header, list);
        return card;
    }

    private VBox possibleRouteCard(Zone refuge) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color:" + CARD + ";" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:16;"
        );

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallIcon("TR", BLUE);

        VBox main = new VBox(5);
        Label route = label(fromName() + " → " + refuge.getName(), WHITE, 18, true);
        route.setWrapText(true);

        Label info = label(
                "Altitude : " + String.format("%.0f m", refuge.getAltitude()) + getCapacityText(refuge),
                LIGHT,
                13,
                false
        );
        info.setWrapText(true);

        main.getChildren().addAll(route, info);
        HBox.setHgrow(main, Priority.ALWAYS);

        VBox actionBox = new VBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMinWidth(180);

        Label status = statusLabel(refuge.isFlooded() ? "Risque" : "Accessible", refuge.isFlooded() ? RED : GREEN);

        Button open = blueButton("Afficher le trajet");
        open.setMinWidth(160);
        open.setPrefWidth(160);
        open.setMaxWidth(160);
        open.setDisable(from == null || refuge.isFlooded() || refuge.isEvacuated());
        open.setOnAction(e -> openRoute(refuge, false));

        actionBox.getChildren().addAll(status, open);

        row.getChildren().addAll(icon, main, actionBox);
        card.getChildren().add(row);

        return card;
    }

    private VBox buildRecentRoutes() {
        VBox card = glassCard(22);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(4);
        Label title = label("Trajets récents", WHITE, 20, true);
        Label subtitle = label("Trajets réellement ouverts sur la carte pendant cette session.", LIGHT, 13, false);
        subtitle.setWrapText(true);
        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        historyCountLabel = badge("0 trajet", BLUE);

        Button clear = darkButton("Vider");
        clear.setOnAction(e -> {
            ROUTE_HISTORY.clear();
            refreshRecentRoutes();
        });

        header.getChildren().addAll(texts, spacer, historyCountLabel, clear);

        recentRoutesBox = new VBox(12);

        card.getChildren().addAll(header, recentRoutesBox);
        refreshRecentRoutes();

        return card;
    }


    



    private void openRoute(Zone refuge, boolean recommended) {
        if (from == null || refuge == null) return;

        controller.registerEvacuationPath(user, from, refuge);

        List<String> instructions = controller.getRouteInstructions(user, from, refuge);
        model.algorithms.EvacuationPath path = controller.computePath(from, refuge);

        if (onShowOnMap != null) {
            onShowOnMap.accept(new RouteRequest(from, refuge, instructions, recommended, path));
        }

        RouteItem item = new RouteItem(
            LocalDateTime.now().format(DATE_FORMAT),
            from.getName(),
            refuge.getName(),
            recommended ? controller.getDistanceLabel(user) : "Voir carte",
            recommended ? controller.getEtaLabel(user) : "Selon itinéraire",
            recommended ? "Recommandé" : "Consulté"
        );
        ROUTE_HISTORY.remove(item);
        ROUTE_HISTORY.add(0, item);
        refreshRecentRoutes();
    }

    private void refreshRecentRoutes() {
        if (recentRoutesBox == null) return;

        recentRoutesBox.getChildren().clear();

        if (historyCountLabel != null) {
            historyCountLabel.setText(ROUTE_HISTORY.size() + (ROUTE_HISTORY.size() > 1 ? " trajets" : " trajet"));
        }

        if (ROUTE_HISTORY.isEmpty()) {
            recentRoutesBox.getChildren().add(emptyBox(
                    "Aucun trajet récent",
                    "Ouvrez un trajet sur la carte pour l’ajouter ici."
            ));
            return;
        }

        for (RouteItem item : ROUTE_HISTORY) {
            recentRoutesBox.getChildren().add(routeHistoryCard(item));
        }
    }

    private VBox routeHistoryCard(RouteItem item) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color:" + CARD + ";" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:16;"
        );

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallIcon("OK", GREEN);

        VBox main = new VBox(4);
        Label route = label(item.start() + " → " + item.end(), WHITE, 17, true);
        route.setWrapText(true);

        Label date = label("Ouvert le " + item.date(), MUTED, 12, false);
        main.getChildren().addAll(route, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox distance = miniInfo("Distance", item.distance());
        VBox duration = miniInfo("Temps", item.duration());
        Label status = badge(item.status(), statusColor(item.status()));

        top.getChildren().addAll(icon, main, spacer, distance, duration, status);
        card.getChildren().add(top);

        return card;
    }

    private List<Zone> getAlternativeRefuges() {
        List<Zone> result = new ArrayList<>();

        for (Zone zone : controller.getZones()) {
            if (!(zone instanceof Shelter)) continue;
            if (zone.isFlooded() || zone.isEvacuated()) continue;

            if (recommendedRefuge != null && sameZone(zone, recommendedRefuge)) {
                continue;
            }

            result.add(zone);
        }

        return result;
    }

    private boolean sameZone(Zone a, Zone b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.getName(), b.getName());
    }

    private String fromName() {
        return from != null ? from.getName() : "Position inconnue";
    }

    private String getCapacityText(Zone refuge) {
        if (refuge instanceof Shelter shelter) {
            return " • Capacité : " + shelter.getCapacity() + " places";
        }

        return "";
    }

    private VBox detailBox(String title, String value, String color) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(14));
        box.setStyle(
                "-fx-background-color:rgba(255,255,255,0.045);" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + color + "66;" +
                        "-fx-border-radius:14;"
        );

        Label t = label(title, MUTED, 11, true);
        Label v = label(value == null || value.isBlank() ? "--" : value, WHITE, 14, true);
        v.setWrapText(true);

        box.getChildren().addAll(t, v);
        return box;
    }

    private VBox miniInfo(String title, String value) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER_LEFT);

        Label t = label(title, MUTED, 11, true);
        Label v = label(value, WHITE, 13, true);

        box.getChildren().addAll(t, v);
        return box;
    }

    private VBox emptyBox(String title, String subtitle) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(34));
        box.setStyle(
                "-fx-background-color:rgba(255,255,255,0.04);" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:rgba(255,255,255,0.10);" +
                        "-fx-border-radius:16;"
        );

        Label t = label(title, WHITE, 18, true);
        Label s = label(subtitle, LIGHT, 13, false);
        s.setWrapText(true);

        box.getChildren().addAll(t, s);
        return box;
    }

    private String statusColor(String status) {
        if (status == null) return BLUE;

        return switch (status.toLowerCase()) {
            case "recommandé" -> BLUE;
            case "consulté" -> GREEN;
            default -> ORANGE;
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

        Label label = label(text, WHITE, text.length() > 1 ? 13 : 17, true);
        icon.getChildren().add(label);

        return icon;
    }

    private Label statusLabel(String text, String color) {
        Label label = label(text == null ? "--" : text, color, 13, true);
        label.setPadding(new Insets(4, 0, 4, 0));
        label.setStyle("-fx-background-color:transparent;");
        return label;
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

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color:linear-gradient(to right, #0a4fa8, #0e73eb);" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-font-weight:bold;" +
                        "-fx-cursor:hand;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff);" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-font-weight:bold;" +
                        "-fx-cursor:hand;"
        ));

        return button;
    }

    private Button darkButton(String text) {
        Button button = new Button(text);
        button.setPadding(new Insets(9, 15, 9, 15));
        button.setStyle(
                "-fx-background-color:rgba(255,255,255,0.06);" +
                        "-fx-text-fill:" + LIGHT + ";" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:rgba(255,255,255,0.14);" +
                        "-fx-border-radius:10;" +
                        "-fx-cursor:hand;"
        );
        return button;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label label = new Label(text == null ? "" : text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return label;
    }

    private record RouteItem(
            String date,
            String start,
            String end,
            String distance,
            String duration,
            String status
    ) {
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RouteItem other)) return false;

            return Objects.equals(start, other.start)
                    && Objects.equals(end, other.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    public record RouteRequest(
        Zone from,
        Zone refuge,
        List<String> instructions,
        boolean recommended,
        model.algorithms.EvacuationPath path
    ) {}
}
