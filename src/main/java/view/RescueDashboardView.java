package view;

import app.Main;
import controller.MapController;
import controller.RescuePage.RescueController;
import javafx.application.Platform;
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
import model.simulation.SimulationDataService;
import model.zone.Zone;

public class RescueDashboardView extends BorderPane {

    private static final String BG_DARK     = "#06172b";
    private static final String BG_SIDEBAR  = "#0b1a30";
    private static final String GLASS       = "rgba(8, 22, 42, 0.72)";
    private static final String BLUE        = "#0e73eb";
    private static final String BLUE_2      = "#1683ff";
    private static final String LIGHT       = "#b8c7dd";
    private static final String WHITE       = "#ffffff";
    private static final String RED         = "#ef4444";
    private static final String GREEN       = "#22c55e";
    private static final String ORANGE      = "#f59e0b";
    private static final String BORDER_GLASS = "rgba(255,255,255,0.18)";

    private final RescueController controller;
    /** MapView partagée avec le reste de l'application (même instance que CitizenDashboardView). */
    private final MapView mapComponent;

    private VBox       dashboardContent;
    private StackPane  contentRoot;
    private VBox       sidebar;
    private Button     activeButton;
    private final SimulationDataService dataService;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    public RescueDashboardView() {
        this(new RescueController(Main.getSharedSimulation()));
    }

    public RescueDashboardView(RescueController controller) {
        this.controller = controller;
        this.dataService = new SimulationDataService();

        // ── Carte partagée ─────────────────────────────────────────────
        // On réutilise la MapView du MapController partagé (même graphe,
        // mêmes agents) exactement comme CitizenDashboardView.
        MapController sharedMC = Main.getSharedMapController();
        if (sharedMC != null) {
            this.mapComponent = sharedMC.getMapView();
        } else {
            this.mapComponent = Main.getSharedMapView();
        }
        if (Main.currentUser != null) {
        mapComponent.setConnectedUser(Main.currentUser);
    }

        // Le secouriste voit TOUS les agents (citoyens + secouristes),
        // contrairement au citoyen qui ne se voit que lui-même.
        mapComponent.setAgents(controller.getAgents());

        // Mise à jour live si la liste d'agents change
        Main.getSharedSimulation().addAgentObserver(updatedAgents ->
            Platform.runLater(() -> mapComponent.setAgents(controller.getAgents()))
        );

        setPrefSize(1100, 650);
        setStyle("-fx-background-color:" + BG_DARK + ";");

        setLeft(buildSidebar());

        contentRoot = new StackPane();
        contentRoot.setStyle("-fx-background-color:linear-gradient(to bottom right, #06172b, #0b1a30, #08162a);");
        setCenter(contentRoot);

        dashboardContent = buildDashboardContent();
        showPage(dashboardContent);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SIDEBAR
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        sidebar = new VBox(9);
        sidebar.setPrefWidth(252);
        sidebar.setPadding(new Insets(22, 14, 18, 14));
        sidebar.setStyle("-fx-background-color:" + BG_SIDEBAR
            + "; -fx-border-color:rgba(255,255,255,0.10); -fx-border-width:0 1 0 0;");

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 8, 20, 8));
        brand.getChildren().addAll(createLogoIcon(),
            new VBox(title("Inondation", 21), muted("Poste secours", 12)));

        HBox profile = new HBox(10);
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.setPadding(new Insets(12));
        profile.setStyle(glassStyle(14));

        Circle avatar = new Circle(20, Color.web(RED));
        VBox names = new VBox(2);
        String name = Main.currentUser != null && Main.currentUser.getFirstName() != null
            ? Main.currentUser.getFirstName() : "Coordinateur";
        Label full = new Label(name);
        full.setTextFill(Color.WHITE);
        full.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label role = new Label("Secours • " + controller.getOperationalStatus());
        role.setTextFill(Color.web(LIGHT));
        role.setFont(Font.font("Segoe UI", 11));
        names.getChildren().addAll(full, role);
        profile.getChildren().addAll(avatar, names);

        Button btnDashboard = sidebarButton("Tableau de bord", "dashboard");
        btnDashboard.setOnAction(e -> { setActive(btnDashboard); showPage(dashboardContent); });

        Button btnMap = sidebarButton("Carte opérationnelle", "map");
        btnMap.setOnAction(e -> { setActive(btnMap); showPage(buildMapPage()); });

        Button btnAlerts = sidebarButton("Alertes", "alert");
        btnAlerts.setOnAction(e -> { setActive(btnAlerts); showPage(new RescueAlertsView(controller)); });

        Button btnMissions = sidebarButton("Missions", "missions");
        btnMissions.setOnAction(e -> { setActive(btnMissions); showPage(new RescueMissionsView(mapComponent, controller)); });

        Button btnProfile = sidebarButton("Profil", "user");
        btnProfile.setOnAction(e -> { setActive(btnProfile); showPage(new RescueProfileView()); });

        Button btnSettings = sidebarButton("Paramètres", "settings");
        btnSettings.setOnAction(e -> { setActive(btnSettings); showPage(new RescueSettingsView()); });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logout = sidebarButton("Déconnexion", "logout");
        logout.setOnAction(e -> { Main.currentUser = null; Main.showWelcomeView(); });

        sidebar.getChildren().addAll(
            brand, profile,
            btnDashboard, btnMap, btnAlerts,
            btnMissions, btnProfile, btnSettings,
            spacer, logout
        );

        setActive(btnDashboard);
        return sidebar;
    }

    // ─────────────────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildDashboardContent() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(30));

        long refugesOuverts = controller.getZones().stream()
        .filter(z -> z instanceof model.zone.Shelter)
        .count();

        long zonesPrioritaires = controller.getZones().stream()
        .filter(Zone::isFlooded)
        .count();

int citoyensAEvacuer = 0;
int citoyensEnAttente = 0;
int personnesEvacuees = controller.getRescuedVictims();

int alertesActives = controller.getActiveAlertsCount();
int missionsActives = controller.getActiveMissions();
    
        VBox header = new VBox(5,
            title("Vue d'ensemble", 28),
            muted("Priorités opérationnelles en temps réel.", 14)
        );
    
        HBox stats = new HBox(16);
        stats.getChildren().addAll(
            miniCard("Alertes actives", String.valueOf(alertesActives), RED),
            miniCard("Missions en cours", String.valueOf(missionsActives), ORANGE),
            miniCard("Citoyens à évacuer", String.valueOf(citoyensAEvacuer), BLUE_2),
            miniCard("Zones prioritaires", String.valueOf(zonesPrioritaires), GREEN)
        );
        for (Node n : stats.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
    
        HBox middle = new HBox(18);
    
        VBox alertsBox = glassCard(20);
        alertsBox.setPrefWidth(520);
        VBox alertList = new VBox(12);

if (controller.getRecentAlerts().isEmpty()) {
    alertList.getChildren().add(muted("Aucune alerte active pour le moment.", 13));
} else {
    for (Alert a : controller.getRecentAlerts()) {
        alertList.getChildren().add(logItem(a.getDescription(), a.getTime()));
    }
}
        Hyperlink allAlerts = link("Voir toutes les alertes");
        allAlerts.setOnAction(e -> showPage(new RescueAlertsView(controller)));
        alertsBox.getChildren().addAll(title("Alertes prioritaires", 18), alertList, allAlerts);
    
        VBox missionsBox = glassCard(20);
        missionsBox.setPrefWidth(520);
        VBox missionList = new VBox(12);

if (missionsActives == 0) {
    missionList.getChildren().add(muted("Aucune mission active pour le moment.", 13));
} else {
    missionList.getChildren().add(
        statusItem("Mission d’évacuation en cours", "En cours", GREEN)
    );
}
        Hyperlink allMissions = link("Voir toutes les missions");
        allMissions.setOnAction(e -> showPage(new RescueMissionsView(mapComponent, controller)));
        missionsBox.getChildren().addAll(title("Missions prioritaires", 18), missionList, allMissions);
    
        middle.getChildren().addAll(alertsBox, missionsBox);
    
        VBox situation = glassCard(20);
        HBox situationRow = new HBox(18);
        situationRow.getChildren().addAll(
            resourceBadge("Refuges ouverts", String.valueOf(refugesOuverts)),
            resourceBadge("Citoyens en attente", String.valueOf(citoyensEnAttente)),
            resourceBadge("Zones critiques", String.valueOf(zonesPrioritaires)),
            resourceBadge("Personnes évacuées", String.valueOf(personnesEvacuees))
        );
        situation.getChildren().addAll(title("Situation générale", 18), situationRow);
    
        root.getChildren().addAll(header, stats, middle, situation);
    
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent; -fx-viewport-background-color:transparent;");
    
        VBox wrapper = new VBox(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAGE CARTE
    // ─────────────────────────────────────────────────────────────────────

    private BorderPane buildMapPage() {
        BorderPane page = new BorderPane();
        page.setPadding(new Insets(26));
        page.setStyle("-fx-background-color:transparent;");

        VBox top = new VBox(12);
        Label titleLbl = title("Carte opérationnelle", 24);
        Label sub = muted("Visualisez les zones inondées, les routes à risque et les secteurs prioritaires.", 13);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button zoomIn  = blueButton("+ Zoom");
        Button zoomOut = darkButton("- Zoom");
        Button reset   = darkButton("Recentrer");
        zoomIn.setOnAction(e  -> mapComponent.zoomIn());
        zoomOut.setOnAction(e -> mapComponent.zoomOut());
        reset.setOnAction(e   -> mapComponent.resetView());
        toolbar.getChildren().addAll(zoomIn, zoomOut, reset);

        top.getChildren().addAll(titleLbl, sub, toolbar);
        top.setPadding(new Insets(0, 0, 14, 0));

        // Tous les agents visibles pour le secouriste
        mapComponent.setAgents(controller.getAgents());

        StackPane mapBox = new StackPane(mapComponent.getSwingNode());
        mapBox.setPrefHeight(480);
        mapBox.setStyle(glassStyle(18));

        VBox bottom = glassCard(16);
        Label bottomTitle = title("Zones prioritaires", 16);
        HBox chips = new HBox(12);
        for (Zone z : controller.getZones()) {
            if (z.isFlooded()) chips.getChildren().add(zoneChip(z));
        }
        if (chips.getChildren().isEmpty())
            chips.getChildren().add(muted("Aucune zone critique détectée pour le moment.", 13));

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

    // ─────────────────────────────────────────────────────────────────────
    // COMPOSANTS UI
    // ─────────────────────────────────────────────────────────────────────

    private VBox zoneChip(Zone z) {
        VBox chip = new VBox(5);
        chip.setPrefWidth(190);
        chip.setPadding(new Insets(12));
        chip.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:12;"
            + " -fx-border-color:rgba(255,255,255,0.12); -fx-border-radius:12;");
            Label name = label(z.getName(), WHITE, 13, true);
        Label info = muted("Alt. " + String.format("%.1f m", z.getAltitude()) + " • Pop. " + z.getPopulation(), 11);
        Button btn = blueButton("Centrer");
        btn.setOnAction(e -> mapComponent.focusZone(z));
        chip.getChildren().addAll(name, info, btn);
        return chip;
    }

    private void showPage(Node page) { contentRoot.getChildren().setAll(page); }

    private void setActive(Button selected) {
        if (activeButton != null) activeButton.setStyle(sidebarStyle(false));
        activeButton = selected;
        if (activeButton != null) activeButton.setStyle(sidebarStyle(true));
    }

    private Button sidebarButton(String text, String iconType) {
        Button btn = new Button(text);
        btn.setGraphic(createSidebarIcon(iconType));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphicTextGap(14);
        btn.setPadding(new Insets(13, 16, 13, 16));
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btn.setStyle(sidebarStyle(false));
        btn.setOnMouseEntered(e -> { if (btn != activeButton)
            btn.setStyle("-fx-background-color:rgba(255,255,255,0.07); -fx-text-fill:white;"
                + " -fx-background-radius:12; -fx-border-color:rgba(255,255,255,0.08);"
                + " -fx-border-radius:12; -fx-cursor:hand;"); });
        btn.setOnMouseExited(e -> { if (btn != activeButton) btn.setStyle(sidebarStyle(false)); });
        return btn;
    }

    private Node createSidebarIcon(String type) {
        SVGPath icon = new SVGPath();
        switch (type) {
            case "dashboard" -> icon.setContent("M3 3 H10 V10 H3 Z M14 3 H21 V10 H14 Z M3 14 H10 V21 H3 Z M14 14 H21 V21 H14 Z");
            case "map"       -> icon.setContent("M12 21 C12 21 5 14 5 8 A7 7 0 0 1 19 8 C19 14 12 21 12 21 Z M12 10 A2 2 0 1 0 12 6 A2 2 0 0 0 12 10");
            case "agents"    -> icon.setContent("M8 11 A3 3 0 1 0 8 5 A3 3 0 0 0 8 11 M16 11 A3 3 0 1 0 16 5 A3 3 0 0 0 16 11 M3 21 Q8 15 13 21 M11 21 Q16 15 21 21");
            case "alert"     -> icon.setContent("M12 3 L22 20 H2 Z M12 9 V14 M12 17 V18");
            case "resources" -> icon.setContent("M4 7 L12 3 L20 7 V17 L12 21 L4 17 Z M4 7 L12 11 L20 7 M12 11 V21");
            case "missions"  -> icon.setContent("M6 3 H18 V21 H6 Z M9 7 H15 M9 11 H15 M9 15 H13");
            case "user"      -> icon.setContent("M12 12 A4 4 0 1 0 12 4 A4 4 0 0 0 12 12 M4 21 Q12 15 20 21");
            case "settings"  -> icon.setContent("M12 8 A4 4 0 1 0 12 16 A4 4 0 0 0 12 8 M12 2 V5 M12 19 V22 M4.9 4.9 L7 7 M17 17 L19.1 19.1 M2 12 H5 M19 12 H22 M4.9 19.1 L7 17 M17 7 L19.1 4.9");
            case "logout"    -> icon.setContent("M10 4 H5 V20 H10 M14 8 L18 12 L14 16 M18 12 H8");
        }
        icon.setStroke(Color.web(LIGHT));
        icon.setStrokeWidth(1.8);
        icon.setFill(Color.TRANSPARENT);
        StackPane box = new StackPane(icon);
        box.setPrefSize(22, 22);
        return box;
    }

    private String sidebarStyle(boolean active) {
        return active
            ? "-fx-background-color:linear-gradient(to right, #b91c1c, #ef4444);"
              + "-fx-text-fill:white; -fx-background-radius:12; -fx-font-weight:bold;"
              + "-fx-cursor:hand; -fx-effect:dropshadow(gaussian, rgba(239,68,68,0.35), 18, 0, 0, 4);"
            : "-fx-background-color:rgba(255,255,255,0.025); -fx-text-fill:#b8c7dd;"
              + "-fx-background-radius:12; -fx-border-color:rgba(255,255,255,0.07);"
              + "-fx-border-radius:12; -fx-cursor:hand;";
    }

    private VBox miniCard(String titleStr, String value, String color) {
        VBox card = glassCard(16);
        card.getChildren().addAll(muted(titleStr, 12), label(value, color, 18, true));
        return card;
    }

    private HBox logItem(String text, String time) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        item.getChildren().addAll(muted(text, 13), sp, muted(time == null ? "--:--" : time, 12));
        return item;
    }

    private HBox statusItem(String text, String status, String color) {
        HBox item = new HBox(10);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        item.getChildren().addAll(muted(text, 13), sp, label(status, color, 13, true));
        return item;
    }

    private VBox resourceBadge(String labelStr, String value) {
        VBox b = new VBox(4);
        b.setPadding(new Insets(12));
        b.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:12;");
        b.getChildren().addAll(muted(labelStr, 12), title(value, 20));
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
    private Label muted(String text, int size)  { return label(text, LIGHT, size, false); }

    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }

    private Button blueButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff);"
            + "-fx-text-fill:white; -fx-background-radius:8; -fx-font-weight:bold; -fx-cursor:hand;");
        return b;
    }

    private Button darkButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:rgba(255,255,255,0.07); -fx-text-fill:#b8c7dd;"
            + "-fx-border-color:rgba(255,255,255,0.16); -fx-border-radius:8;"
            + "-fx-background-radius:8; -fx-cursor:hand;");
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
