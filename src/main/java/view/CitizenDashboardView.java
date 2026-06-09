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

        Button btnDashboard = sidebarButton("Tableau de bord", "dashboard");
        btnDashboard.setOnAction(e -> { setActive(btnDashboard); showPage(dashboardContent); });

        Button btnMap = sidebarButton("Carte", "map");
        btnMap.setOnAction(e -> { setActive(btnMap); showPage(buildMapPage(null)); });

        Button btnRoutes = sidebarButton("Mes trajets", "route");
        btnRoutes.setOnAction(e -> { setActive(btnRoutes); showPage(new CitizenRoutesView(controller, user, mapComponent)); });

        Button btnAlerts = sidebarButton("Alertes", "alert");
        btnAlerts.setOnAction(e -> {
            setActive(btnAlerts);
            CitizenAlertsView alertsView = new CitizenAlertsView();
            new CitizenAlertsController(alertsView, controller.getAlertSystem());
            showPage(alertsView);
        });

        Button btnHistory = sidebarButton("Historique", "history");
        btnHistory.setOnAction(e -> { setActive(btnHistory); showPage(new CitizenHistoryView(controller)); });

        Button btnRefuges = sidebarButton("Refuges", "home");
        btnRefuges.setOnAction(e -> { setActive(btnRefuges); showPage(new CitizenRefugesView(controller, user, this::openRouteToRefuge)); });

        Button btnProfile = sidebarButton("Profil", "user");
        btnProfile.setOnAction(e -> { setActive(btnProfile); showPage(new CitizenProfileView(controller, user)); });

        Button btnSettings = sidebarButton("Paramètres", "settings");
        btnSettings.setOnAction(e -> { setActive(btnSettings); showPage(new CitizenSettingsView()); });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logout = sidebarButton("Déconnexion", "logout");
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

    private Button sidebarButton(String text, String iconType) {
        Button btn = new Button(text);
        btn.setGraphic(createSidebarIcon(iconType));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphicTextGap(14);
        btn.setPadding(new Insets(13, 16, 13, 16));
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btn.setStyle(sidebarStyle(false));
    
        btn.setOnMouseEntered(e -> {
            if (btn != activeButton) {
                btn.setStyle("-fx-background-color:rgba(255,255,255,0.07);"
                        + "-fx-text-fill:white;"
                        + "-fx-background-radius:12;"
                        + "-fx-border-color:rgba(255,255,255,0.08);"
                        + "-fx-border-radius:12;"
                        + "-fx-cursor:hand;");
            }
        });
    
        btn.setOnMouseExited(e -> {
            if (btn != activeButton) btn.setStyle(sidebarStyle(false));
        });
    
        return btn;
    }

    private Node createSidebarIcon(String type) {
        SVGPath icon = new SVGPath();
    
        switch (type) {
            case "dashboard":
                icon.setContent("M3 3 H10 V10 H3 Z M14 3 H21 V10 H14 Z M3 14 H10 V21 H3 Z M14 14 H21 V21 H14 Z");
                break;
            case "map":
                icon.setContent("M12 21 C12 21 5 14 5 8 A7 7 0 0 1 19 8 C19 14 12 21 12 21 Z M12 10 A2 2 0 1 0 12 6 A2 2 0 0 0 12 10");
                break;
            case "route":
                icon.setContent("M6 4 A2 2 0 1 0 6 8 A2 2 0 0 0 6 4 M18 16 A2 2 0 1 0 18 20 A2 2 0 0 0 18 16 M6 8 V11 Q6 14 9 14 H15 Q18 14 18 16");
                break;
            case "alert":
                icon.setContent("M12 3 L22 20 H2 Z M12 9 V14 M12 17 V18");
                break;
            case "history":
                icon.setContent("M12 5 A7 7 0 1 1 6 8 M6 8 H3 M6 8 V5 M12 8 V13 L16 15");
                break;
            case "home":
                icon.setContent("M3 11 L12 3 L21 11 V21 H15 V15 H9 V21 H3 Z");
                break;
            case "user":
                icon.setContent("M12 12 A4 4 0 1 0 12 4 A4 4 0 0 0 12 12 M4 21 Q12 15 20 21");
                break;
            case "settings":
                icon.setContent("M12 8 A4 4 0 1 0 12 16 A4 4 0 0 0 12 8 M12 2 V5 M12 19 V22 M4.9 4.9 L7 7 M17 17 L19.1 19.1 M2 12 H5 M19 12 H22 M4.9 19.1 L7 17 M17 7 L19.1 4.9");
                break;
            case "logout":
                icon.setContent("M10 4 H5 V20 H10 M14 8 L18 12 L14 16 M18 12 H8");
                break;
        }
    
        icon.setStroke(Color.web("#b8c7dd"));
        icon.setStrokeWidth(1.8);
        icon.setFill(Color.TRANSPARENT);
    
        StackPane box = new StackPane(icon);
        box.setPrefSize(22, 22);
        return box;
    }

    private String sidebarStyle(boolean active) {
        return active
                ? "-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff);"
                + "-fx-text-fill:white;"
                + "-fx-background-radius:12;"
                + "-fx-font-weight:bold;"
                + "-fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian, rgba(14,115,235,0.35), 18, 0, 0, 4);"
                : "-fx-background-color:rgba(255,255,255,0.025);"
                + "-fx-text-fill:#b8c7dd;"
                + "-fx-background-radius:12;"
                + "-fx-border-color:rgba(255,255,255,0.07);"
                + "-fx-border-radius:12;"
                + "-fx-cursor:hand;";
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
