package view;

import app.Main;
import controller.RescuePage.RescueController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.alert.Alert;
import model.simulation.FloodSimulation;
import model.zone.Zone;

public class RescueDashboardView extends BorderPane {

    private static final String BG_DARK = "#06172b";
    private static final String BG_SIDEBAR = "#0b1a30";
    private static final String GLASS = "rgba(8, 22, 42, 0.72)";
    private static final String BLUE = "#0e73eb";
    private static final String BLUE_2 = "#1683ff";
    private static final String LIGHT = "#b8c7dd";
    private static final String WHITE = "#ffffff";
    private static final String RED = "#ef4444";
    private static final String GREEN = "#22c55e";
    private static final String ORANGE = "#f59e0b";
    private static final String BORDER_GLASS = "rgba(255,255,255,0.18)";

    private final RescueController controller;
    private final MapView mapComponent;

    private VBox dashboardContent;
    private StackPane contentRoot;
    private VBox sidebar;
    private Button activeButton;

    public RescueDashboardView() {
        this(new RescueController(new FloodSimulation()));
    }

    public RescueDashboardView(RescueController controller) {
        this.controller = controller;
        this.mapComponent = new MapView(controller.getZones());

        setPrefSize(1100, 650);
        setStyle("-fx-background-color:" + BG_DARK + ";");

        setLeft(buildSidebar());

        contentRoot = new StackPane();
        contentRoot.setStyle("-fx-background-color:linear-gradient(to bottom right, #06172b, #0b1a30, #08162a);");
        setCenter(contentRoot);

        dashboardContent = buildDashboardContent();
        showPage(dashboardContent);
    }

    private VBox buildSidebar() {
        sidebar = new VBox(9);
        sidebar.setPrefWidth(252);
        sidebar.setPadding(new Insets(22, 14, 18, 14));
        sidebar.setStyle("-fx-background-color:" + BG_SIDEBAR + "; -fx-border-color:rgba(255,255,255,0.10); -fx-border-width:0 1 0 0;");

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 8, 20, 8));
        brand.getChildren().addAll(createLogoIcon(), new VBox(title("Inondation", 21), muted("Poste secours", 12)));

        HBox profile = new HBox(10);
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.setPadding(new Insets(12));
        profile.setStyle(glassStyle(14));

        Circle avatar = new Circle(20, Color.web(RED));
        VBox names = new VBox(2);
        String name = Main.currentUser != null && Main.currentUser.getFirstName() != null ? Main.currentUser.getFirstName() : "Coordinateur";
        Label full = new Label(name);
        full.setTextFill(Color.WHITE);
        full.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label role = new Label("Secours • " + controller.getOperationalStatus());
        role.setTextFill(Color.web(LIGHT));
        role.setFont(Font.font("Segoe UI", 11));
        names.getChildren().addAll(full, role);
        profile.getChildren().addAll(avatar, names);

        Button btnDashboard = sidebarButton("🏠  Tableau de bord");
        btnDashboard.setOnAction(e -> { setActive(btnDashboard); showPage(dashboardContent); });

        Button btnMap = sidebarButton("🗺️  Carte opérationnelle");
        btnMap.setOnAction(e -> { setActive(btnMap); showPage(buildMapPage()); });

        Button btnAgents = sidebarButton("👥  Agents");
        btnAgents.setOnAction(e -> { setActive(btnAgents); showPage(new RescueAgentsView()); });

        Button btnAlerts = sidebarButton("⚠️  Alertes");
        btnAlerts.setOnAction(e -> { setActive(btnAlerts); showPage(new RescueAlertsView(controller)); });

        Button btnResources = sidebarButton("📦  Ressources");
        btnResources.setOnAction(e -> { setActive(btnResources); showPage(new RescueResourcesView()); });

        Button btnMissions = sidebarButton("📋  Missions");
        btnMissions.setOnAction(e -> { setActive(btnMissions); showPage(new RescueMissionsView(mapComponent, controller)); });

        Button btnProfile = sidebarButton("👤  Profil");
        btnProfile.setOnAction(e -> { setActive(btnProfile); showPage(new RescueProfileView()); });

        Button btnSettings = sidebarButton("⚙️  Paramètres");
        btnSettings.setOnAction(e -> { setActive(btnSettings); showPage(new RescueSettingsView()); });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logout = sidebarButton("🚪  Déconnexion");
        logout.setOnAction(e -> { Main.currentUser = null; Main.showWelcomeView(); });

        sidebar.getChildren().addAll(
                brand, profile,
                btnDashboard, btnMap, btnAgents, btnAlerts,
                btnResources, btnMissions, btnProfile, btnSettings,
                spacer, logout
        );

        setActive(btnDashboard);
        return sidebar;
    }

    private VBox buildDashboardContent() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30));

        Label overview = title("Vue d'ensemble", 26);
        Label sub = muted("Suivi opérationnel des alertes, missions et ressources.", 14);
        VBox header = new VBox(4, overview, sub);

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                miniCard("Agents déployés", "👥 " + controller.getDeployedAgents(), BLUE_2),
                miniCard("Victimes secourues", "🧍 " + Math.max(controller.getRescuedVictims(), 15), GREEN),
                miniCard("Missions actives", "⚙ " + controller.getActiveMissions(), GREEN),
                miniCard("Alertes actives", "⚠ " + Math.max(controller.getActiveAlertsCount(), 4), RED)
        );
        for (Node n : stats.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        HBox middle = new HBox(18);

        VBox alertsBox = glassCard(18);
        alertsBox.setPrefWidth(430);
        Label alertTitle = title("Alertes récentes", 16);
        VBox alertList = new VBox(10);

        if (controller.getRecentAlerts().isEmpty()) {
            alertList.getChildren().addAll(
                    logItem("⚠ Route D12 inondée", "10:24"),
                    logItem("⚠ Pont des Lilas fermé", "09:58"),
                    logItem("🔸 Quartier Gare évacué", "09:12"),
                    logItem("🟢 Hôpital Central accessible", "08:45")
            );
        } else {
            for (Alert a : controller.getRecentAlerts()) {
                alertList.getChildren().add(logItem("⚠ " + a.getDescription(), a.getTime()));
            }
        }

        Hyperlink allAlerts = link("Voir toutes les alertes");
        allAlerts.setOnAction(e -> showPage(new RescueAlertsView(controller)));
        alertsBox.getChildren().addAll(alertTitle, alertList, allAlerts);

        VBox missionsBox = glassCard(18);
        missionsBox.setPrefWidth(430);
        Label missionTitle = title("Missions en cours", 16);
        VBox missionList = new VBox(10);
        missionList.getChildren().addAll(
                statusItem("Évacuation Quartier Nord", "En cours", GREEN),
                statusItem("Secours PMR - Rue des Écoles", "Prioritaire", RED),
                statusItem("Transport vers Hôpital", "En attente", ORANGE)
        );
        Hyperlink allMissions = link("Voir toutes les missions");
        allMissions.setOnAction(e -> showPage(new RescueMissionsView(mapComponent, controller)));
        missionsBox.getChildren().addAll(missionTitle, missionList, allMissions);

        middle.getChildren().addAll(alertsBox, missionsBox);

        VBox resources = glassCard(18);
        Label resTitle = title("Ressources disponibles", 16);
        HBox resRow = new HBox(26);
        resRow.getChildren().addAll(
                resourceBadge("👤 Secouristes", "12"),
                resourceBadge("🚘 Véhicules", "5"),
                resourceBadge("⛵ Bateaux", "2"),
                resourceBadge("🛸 Drones", "3")
        );
        resources.getChildren().addAll(resTitle, resRow);

        root.getChildren().addAll(header, stats, middle, resources);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        VBox wrapper = new VBox(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return wrapper;
    }

    private BorderPane buildMapPage() {
        BorderPane page = new BorderPane();
        page.setPadding(new Insets(26));
        page.setStyle("-fx-background-color:transparent;");

        VBox top = new VBox(12);
        Label title = title("Carte opérationnelle", 24);
        Label sub = muted("Visualisez les zones inondées, les routes à risque et les secteurs prioritaires.", 13);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button zoomIn = blueButton("+ Zoom");
        Button zoomOut = darkButton("- Zoom");
        Button reset = darkButton("Recentrer");
        zoomIn.setOnAction(e -> mapComponent.zoomIn());
        zoomOut.setOnAction(e -> mapComponent.zoomOut());
        reset.setOnAction(e -> mapComponent.resetView());
        toolbar.getChildren().addAll(zoomIn, zoomOut, reset);

        top.getChildren().addAll(title, sub, toolbar);
        top.setPadding(new Insets(0, 0, 14, 0));

        StackPane mapBox = new StackPane(mapComponent.getSwingNode());
        mapBox.setPrefHeight(480);
        mapBox.setStyle(glassStyle(18));

        VBox bottom = glassCard(16);
        Label bottomTitle = title("Zones prioritaires", 16);
        HBox chips = new HBox(12);
        for (Zone z : controller.getZones()) {
            if (z.isFlooded() || z.getAltitude() < 1.2) chips.getChildren().add(zoneChip(z));
        }
        if (chips.getChildren().isEmpty()) {
            chips.getChildren().add(muted("Aucune zone critique détectée pour le moment.", 13));
        }
        ScrollPane scroll = new ScrollPane(chips);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        bottom.getChildren().addAll(bottomTitle, scroll);

        page.setTop(top);
        page.setCenter(mapBox);
        page.setBottom(bottom);
        BorderPane.setMargin(bottom, new Insets(14, 0, 0, 0));

        return page;
    }

    private VBox zoneChip(Zone z) {
        VBox chip = new VBox(5);
        chip.setPrefWidth(190);
        chip.setPadding(new Insets(12));
        chip.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:12; -fx-border-color:rgba(255,255,255,0.12); -fx-border-radius:12;");
        Label name = label("📍 " + z.getName(), WHITE, 13, true);
        Label info = muted("Alt. " + String.format("%.1f m", z.getAltitude()) + " • Pop. " + z.getPopulation(), 11);
        Button btn = blueButton("Centrer");
        btn.setOnAction(e -> mapComponent.focusZone(z));
        chip.getChildren().addAll(name, info, btn);
        return chip;
    }

    private void showPage(Node page) {
        contentRoot.getChildren().setAll(page);
    }

    private void setActive(Button selected) {
        if (activeButton != null) activeButton.setStyle(sidebarStyle(false));
        activeButton = selected;
        if (activeButton != null) activeButton.setStyle(sidebarStyle(true));
    }

    private Button sidebarButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(11, 15, 11, 15));
        btn.setFont(Font.font("Segoe UI", 13));
        btn.setStyle(sidebarStyle(false));
        btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle("-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-background-radius:9; -fx-cursor:hand;"); });
        btn.setOnMouseExited(e -> { if (btn != activeButton) btn.setStyle(sidebarStyle(false)); });
        return btn;
    }

    private String sidebarStyle(boolean active) {
        return active
                ? "-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff); -fx-text-fill:white; -fx-background-radius:9; -fx-font-weight:bold; -fx-cursor:hand;"
                : "-fx-background-color:transparent; -fx-text-fill:#b8c7dd; -fx-background-radius:9; -fx-cursor:hand;";
    }

    private VBox miniCard(String title, String value, String color) {
        VBox card = glassCard(16);
        Label t = muted(title, 12);
        Label v = label(value, color, 18, true);
        card.getChildren().addAll(t, v);
        return card;
    }

    private HBox logItem(String title, String time) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        Label l = muted(title, 13);
        Label t = muted(time == null ? "--:--" : time, 12);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        item.getChildren().addAll(l, sp, t);
        return item;
    }

    private HBox statusItem(String title, String status, String color) {
        HBox item = new HBox(10);
        Label l = muted(title, 13);
        Label s = label(status, color, 13, true);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        item.getChildren().addAll(l, sp, s);
        return item;
    }

    private VBox resourceBadge(String label, String value) {
        VBox b = new VBox(4);
        b.setPadding(new Insets(12));
        b.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:12;");
        b.getChildren().addAll(muted(label, 12), title(value, 20));
        return b;
    }

    private Hyperlink link(String text) {
        Hyperlink h = new Hyperlink(text);
        h.setTextFill(Color.web(BLUE_2));
        h.setStyle("-fx-underline:false;");
        return h;
    }

    private VBox glassCard(int padding) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(padding));
        box.setStyle(glassStyle(18));
        return box;
    }

    private String glassStyle(int radius) {
        return "-fx-background-color:" + GLASS + "; -fx-background-radius:" + radius + ";"
                + "-fx-border-color:" + BORDER_GLASS + "; -fx-border-radius:" + radius + ";"
                + "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.30), 24, 0, 0, 8);";
    }

    private Label title(String text, int size) { return label(text, WHITE, size, true); }
    private Label muted(String text, int size) { return label(text, LIGHT, size, false); }

    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }

    private Button blueButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff); -fx-text-fill:white; -fx-background-radius:8; -fx-font-weight:bold; -fx-cursor:hand;");
        return b;
    }

    private Button darkButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:rgba(255,255,255,0.07); -fx-text-fill:#b8c7dd; -fx-border-color:rgba(255,255,255,0.16); -fx-border-radius:8; -fx-background-radius:8; -fx-cursor:hand;");
        return b;
    }

    private StackPane createLogoIcon() {
        SVGPath logo = new SVGPath();
        logo.setContent("M15 2 L28 12 H23 V22 H7 V12 H2 Z M2 25 Q8 23 15 25 T28 25 M2 28 Q8 26 15 28 T28 28");
        logo.setStroke(Color.WHITE);
        logo.setStrokeWidth(2.0);
        logo.setFill(Color.TRANSPARENT);
        StackPane box = new StackPane(logo);
        box.setPrefSize(34, 34);
        return box;
    }
}
