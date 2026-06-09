package view;

import app.Main;
import controller.CitizenPage.CitizenAlertsController;
import controller.CitizenPage.CitizenController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.agent.Agent;
import model.alert.Alert;
import model.zone.Zone;

public class CitizenDashboardView extends BorderPane {

    private static final String BG_DARK = "#06172b";
    private static final String BG_SIDEBAR = "#0b1a30";
    private static final String GLASS = "rgba(8, 22, 42, 0.72)";
    private static final String BLUE = "#0e73eb";
    private static final String BLUE_2 = "#1683ff";
    private static final String LIGHT = "#b8c7dd";
    private static final String WHITE = "#ffffff";
    private static final String BORDER_GLASS = "rgba(255,255,255,0.18)";

    private final CitizenController controller;
    private final Agent user;

    private VBox sidebar;
    private StackPane contentRoot;
    private VBox dashboardContent;
    private MapView mapComponent;
    private Button activeButton;

    public CitizenDashboardView(CitizenController controller) {
        this.controller = controller;
        this.user = Main.currentUser;
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
        brand.getChildren().addAll(createLogoIcon(), new VBox(title("Inondation", 21), muted("Espace citoyen", 12)));

        HBox profile = new HBox(10);
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.setPadding(new Insets(12));
        profile.setStyle(glassStyle(14));

        Circle avatar = new Circle(20, controller.isCitizenInPanic(user) ? Color.web("#ef4444") : Color.web(BLUE));
        VBox names = new VBox(2);
        Label full = new Label(controller.getFullName(user));
        full.setTextFill(Color.WHITE);
        full.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label role = new Label("Citoyen • " + controller.getCitizenState(user).toLowerCase());
        role.setTextFill(Color.web(LIGHT));
        role.setFont(Font.font("Segoe UI", 11));
        names.getChildren().addAll(full, role);
        profile.getChildren().addAll(avatar, names);

        Button btnDashboard = sidebarButton("🏠  Tableau de bord");
        btnDashboard.setOnAction(e -> { setActive(btnDashboard); showPage(dashboardContent); });

        Button btnMap = sidebarButton("🗺️  Carte");
        btnMap.setOnAction(e -> { setActive(btnMap); showPage(buildMapPage(null)); });

        Button btnRoutes = sidebarButton("🔀  Mes trajets");
        btnRoutes.setOnAction(e -> { setActive(btnRoutes); showPage(new CitizenRoutesView(controller, user, mapComponent)); });

        Button btnAlerts = sidebarButton("⚠️  Alertes");
        btnAlerts.setOnAction(e -> {
            setActive(btnAlerts);
            CitizenAlertsView alertsView = new CitizenAlertsView();
            new CitizenAlertsController(alertsView, controller.getAlertSystem());
            showPage(alertsView);
        });

        Button btnHistory = sidebarButton("🕘  Historique");
        btnHistory.setOnAction(e -> { setActive(btnHistory); showPage(new CitizenHistoryView(controller)); });

        Button btnRefuges = sidebarButton("🏫  Refuges");
        btnRefuges.setOnAction(e -> { setActive(btnRefuges); showPage(new CitizenRefugesView(controller, user, this::openRouteToRefuge)); });

        Button btnProfile = sidebarButton("👤  Profil");
        btnProfile.setOnAction(e -> { setActive(btnProfile); showPage(new CitizenProfileView(controller, user)); });

        Button btnSettings = sidebarButton("⚙️  Paramètres");
        btnSettings.setOnAction(e -> { setActive(btnSettings); showPage(new CitizenSettingsView()); });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logout = sidebarButton("🚪  Déconnexion");
        logout.setOnAction(e -> { Main.currentUser = null; Main.showWelcomeView(); });

        sidebar.getChildren().addAll(brand, profile, btnDashboard, btnMap, btnRoutes, btnAlerts, btnHistory, btnRefuges, btnProfile, btnSettings, spacer, logout);
        setActive(btnDashboard);
        return sidebar;
    }

    private VBox buildDashboardContent() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30));

        Label welcome = title("Bonjour, " + controller.getFirstName(user) + " !", 26);
        Label sub = muted("Votre position, vos alertes et votre refuge recommandé.", 14);
        VBox header = new VBox(4, welcome, sub);

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                miniCard("Ma position", controller.getDetailedPositionLabel(user), "⌖"),
                miniCard("Refuge conseillé", controller.getTargetRefugeDetails(user), "⌂"),
                miniCard("Temps estimé", controller.getEtaLabel(user), "◷"),
                miniCard("Distance", controller.getDistanceLabel(user), "⇢")
        );
        for (Node n : cards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        HBox middle = new HBox(18);
        VBox alertPanel = glassCard(20);
        alertPanel.setPrefWidth(430);
        Label alertTitle = label("⚠ Consignes d'évacuation", "#ffb4b4", 15, true);
        Alert latest = controller.getLatestCriticalAlert();
        Label alertTxt = muted(latest != null ? latest.getDescription() + " — " + latest.getLocalisation() : "Aucune alerte critique personnalisée. Restez attentif aux consignes officielles.", 13);
        alertTxt.setWrapText(true);
        Hyperlink link = new Hyperlink("Voir toutes les alertes");
        link.setTextFill(Color.web(BLUE_2));
        link.setOnAction(e -> {
            CitizenAlertsView alertsView = new CitizenAlertsView();
            new CitizenAlertsController(alertsView, controller.getAlertSystem());
            showPage(alertsView);
        });
        alertPanel.getChildren().addAll(alertTitle, alertTxt, link);

        VBox routePanel = glassCard(20);
        routePanel.setPrefWidth(430);
        Label routeTitle = title("Plan de route assigné", 16);
        routePanel.getChildren().addAll(
                routeTitle,
                muted("🟢 Départ : " + controller.getPositionLabel(user), 13),
                muted("🔴 Refuge : " + controller.getTargetRefugeLabel(user), 13),
                muted("📍 Statut : " + controller.getRouteStatusLabel(user), 13)
        );

        Button btnMap = blueButton("Suivre sur la carte");
        btnMap.setOnAction(e -> openRouteToRefuge(controller.getNearestSafeRefuge(user)));
        routePanel.getChildren().add(btnMap);

        middle.getChildren().addAll(alertPanel, routePanel);

        VBox bottom = glassCard(18);
        Label bottomTitle = title("Carte rapide", 16);
        Label bottomText = muted("La carte permet de sélectionner des zones, de visualiser les routes et d'afficher votre itinéraire vers un refuge.", 13);
        bottomText.setWrapText(true);
        bottom.getChildren().addAll(bottomTitle, bottomText);

        root.getChildren().addAll(header, cards, middle, bottom);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        VBox wrapper = new VBox(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return wrapper;
    }

    private BorderPane buildMapPage(Zone selectedRefuge) {
        BorderPane page = new BorderPane();
        page.setPadding(new Insets(26));
        page.setStyle("-fx-background-color:transparent;");

        VBox top = new VBox(12);
        Label title = title("Carte & itinéraire", 24);
        Label sub = muted("Déplacez la carte, zoomez, cliquez sur une zone ou affichez un trajet vers un refuge.", 13);

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
        mapBox.setPrefHeight(430);
        mapBox.setStyle(glassStyle(18));

        VBox refugesBottom = buildRefugeStrip();

        page.setTop(top);
        page.setCenter(mapBox);
        page.setBottom(refugesBottom);
        BorderPane.setMargin(refugesBottom, new Insets(14, 0, 0, 0));

        if (selectedRefuge != null) {
            Zone from = controller.getNearestZone(user);
            mapComponent.showRoute(from, selectedRefuge);
        }

        return page;
    }

    private VBox buildRefugeStrip() {
        VBox box = glassCard(16);
        Label title = title("Refuges disponibles", 16);
        HBox list = new HBox(12);
        for (Zone z : controller.getZones()) {
            if (!z.isFlooded()) {
                list.getChildren().add(refugeChip(z));
            }
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(muted("Aucun refuge accessible pour le moment.", 13));
        }
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        box.getChildren().addAll(title, scroll);
        return box;
    }

    private VBox refugeChip(Zone z) {
        VBox chip = new VBox(5);
        chip.setPrefWidth(180);
        chip.setPadding(new Insets(12));
        chip.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:12; -fx-border-color:rgba(255,255,255,0.12); -fx-border-radius:12;");
        Label name = label("🏫 " + z.getName(), WHITE, 13, true);
        Label pop = muted("Population : " + z.getPopulation(), 11);
        Button btn = blueButton("Itinéraire");
        btn.setOnAction(e -> openRouteToRefuge(z));
        chip.getChildren().addAll(name, pop, btn);
        return chip;
    }

    public void openRouteToRefuge(Zone refuge) {
        setActive(null);
        showPage(buildMapPage(refuge));
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

    private VBox miniCard(String title, String value, String icon) {
        VBox c = glassCard(16);
        c.setMinWidth(170);
        Label i = label(icon, BLUE_2, 22, true);
        Label t = muted(title, 12);
        Label v = label(value, WHITE, 14, true);
        v.setWrapText(true);
        c.getChildren().addAll(i, t, v);
        return c;
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

    private Label title(String text, int size) {
        return label(text, WHITE, size, true);
    }

    private Label muted(String text, int size) {
        return label(text, LIGHT, size, false);
    }

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
