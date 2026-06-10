package view;

import java.util.List;

import app.Main;
import controller.MapController;
import controller.CitizenPage.CitizenAlertsController;
import controller.CitizenPage.CitizenController;
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
import model.agent.Agent;
import model.alert.Alert;
import model.zone.Shelter;
import model.zone.Zone;

public class CitizenDashboardView extends BorderPane {

    private static final String BG_DARK = "#06172b";
    private static final String BG_SIDEBAR = "#0b1a30";
    private static final String GLASS = "rgba(8, 22, 42, 0.72)";
    private static final String BLUE = "#0e73eb";
    private static final String BLUE_2 = "#1683ff";
    private static final String GREEN = "#22c55e";
    private static final String RED = "#ef4444";
    private static final String ORANGE = "#f59e0b";
    private static final String LIGHT = "#b8c7dd";
    private static final String MUTED = "#7f91aa";
    private static final String WHITE = "#ffffff";
    private static final String BORDER_GLASS = "rgba(255,255,255,0.18)";

    private final CitizenController controller;
    private controller.MapController mapControllerRef;
    private final Agent user;

    private VBox sidebar;
    private StackPane contentRoot;
    private VBox dashboardContent;
    private MapView mapComponent;
    private Button activeButton;

    public CitizenDashboardView(CitizenController controller) {
        this.controller = controller;
        this.user = Main.currentUser;
        
        MapController sharedMC = Main.getSharedMapController();
        if (sharedMC != null) {
            this.mapComponent = sharedMC.getMapView();
        } else {
            // Fallback si SimulationView n'a pas encore été ouverte :
            // Créer une MapView autonome ET la brancher sur les données partagées
            this.mapComponent = Main.getSharedMapView();
            // Synchroniser agents et graphe depuis la simulation partagée
            List<Agent> agents = Main.getSharedSimulation().getAgents();
            this.mapComponent.setAgents(agents);
        }

        // Abonnement temps réel aux changements de zones
        Main.getSharedSimulation().addZoneObserver(zone ->
            Platform.runLater(() ->
                mapComponent.updateAllZones(Main.getSharedSimulation().getZones())
            )
        );

        // Abonnement temps réel aux changements d'agents
        Main.getSharedSimulation().addAgentObserver(updatedAgents ->
            Platform.runLater(() ->
                mapComponent.setAgents(updatedAgents)
            )
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

        Circle avatar = new Circle(20, controller.isCitizenInPanic(user) ? Color.web(RED) : Color.web(BLUE));

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
        btnDashboard.setOnAction(e -> {
            setActive(btnDashboard);
            showPage(dashboardContent);
        });

        Button btnMap = sidebarButton("Carte", "map");
        btnMap.setOnAction(e -> {
            setActive(btnMap);
            showPage(buildMapPage(null));
        });

        Button btnRoutes = sidebarButton("Mes trajets", "route");
        btnRoutes.setOnAction(e -> {
            setActive(btnRoutes);
            showPage(new CitizenRoutesView(controller, user, mapComponent));
        });

        Button btnAlerts = sidebarButton("Alertes", "alert");
        btnAlerts.setOnAction(e -> {
            setActive(btnAlerts);
            CitizenAlertsView alertsView = new CitizenAlertsView();
            new CitizenAlertsController(alertsView, controller.getAlertSystem());
            showPage(alertsView);
        });

        Button btnHistory = sidebarButton("Historique", "history");
        btnHistory.setOnAction(e -> {
            setActive(btnHistory);
            showPage(new CitizenHistoryView(controller));
        });

        Button btnRefuges = sidebarButton("Refuges", "home");
        btnRefuges.setOnAction(e -> {
            setActive(btnRefuges);
            showPage(new CitizenRefugesView(controller, user, this::openRouteToRefuge));
        });

        Button btnProfile = sidebarButton("Profil", "user");
        btnProfile.setOnAction(e -> {
            setActive(btnProfile);
            showPage(new CitizenProfileView(controller, user));
        });

        Button btnSettings = sidebarButton("Paramètres", "settings");
        btnSettings.setOnAction(e -> {
            setActive(btnSettings);
            showPage(new CitizenSettingsView());
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logout = sidebarButton("Déconnexion", "logout");
        logout.setOnAction(e -> {
            Main.currentUser = null;
            Main.showWelcomeView();
        });

        sidebar.getChildren().addAll(
                brand,
                profile,
                btnDashboard,
                btnMap,
                btnRoutes,
                btnAlerts,
                btnHistory,
                btnRefuges,
                btnProfile,
                btnSettings,
                spacer,
                logout
        );

        setActive(btnDashboard);
        return sidebar;
    }

    private VBox buildDashboardContent() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30));

        VBox header = new VBox(6);
        Label welcome = title("Bonjour, " + controller.getFirstName(user) + " !", 30);
        Label sub = muted("Vue d’ensemble de votre sécurité, de votre position et de votre itinéraire d’évacuation.", 14);
        header.getChildren().addAll(welcome, sub);

        HBox hero = new HBox(18);
        hero.getChildren().addAll(buildSafetyOverview(), buildRecommendedShelterPanel());

        HBox.setHgrow(hero.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(hero.getChildren().get(1), Priority.ALWAYS);

        Zone nearestRefuge = controller.getNearestSafeRefuge(user);

        String refugeAltitude = nearestRefuge != null
                ? String.format("%.0f m", nearestRefuge.getAltitude())
                : "--";

        String refugeCapacity = nearestRefuge instanceof Shelter shelter
                ? shelter.getCapacity() + " places"
                : "--";

        HBox cardsLine1 = new HBox(16);
        cardsLine1.getChildren().addAll(
                miniCard("Position actuelle", controller.getPositionLabel(user), "⌖", BLUE_2),
                miniCard("Refuge recommandé", controller.getTargetRefugeLabel(user), "⌂", GREEN),
                miniCard("Distance refuge", controller.getDistanceLabel(user), "⇢", GREEN),
                miniCard("Temps estimé", controller.getEtaLabel(user), "◷", ORANGE)
        );

        HBox cardsLine2 = new HBox(16);
        cardsLine2.getChildren().addAll(
                miniCard("Alertes actives", String.valueOf(controller.getAlertCount()), "!", RED),
                miniCard("État citoyen", controller.getCitizenState(user), "✓", BLUE_2),
                miniCard("Altitude refuge", refugeAltitude, "△", ORANGE),
                miniCard("Capacité refuge", refugeCapacity, "▣", BLUE_2)
        );

        for (Node n : cardsLine1.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        for (Node n : cardsLine2.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        HBox middle = new HBox(18);
        middle.getChildren().addAll(buildAlertPanel(), buildRoutePanel());
        HBox.setHgrow(middle.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(middle.getChildren().get(1), Priority.ALWAYS);

        VBox quickActions = buildQuickActions();

        root.getChildren().addAll(header, hero, cardsLine1, cardsLine2, middle, quickActions);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        VBox wrapper = new VBox(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildSafetyOverview() {
        VBox card = glassCard(22);

        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = roundIcon(controller.isCitizenInPanic(user) ? "!" : "✓", controller.isCitizenInPanic(user) ? RED : GREEN, 66);

        VBox texts = new VBox(5);
        Label small = label("État de sécurité", MUTED, 12, true);
        Label title = label(controller.isCitizenInPanic(user) ? "Situation à surveiller" : "Situation stable", WHITE, 24, true);

        Label desc = label(
                controller.isCitizenInPanic(user)
                        ? "Votre état indique une situation de stress. Suivez les consignes et rejoignez un refuge si nécessaire."
                        : "Aucune situation critique détectée pour votre profil actuellement.",
                LIGHT,
                13,
                false
        );
        desc.setWrapText(true);

        texts.getChildren().addAll(small, title, desc);
        top.getChildren().addAll(icon, texts);

        HBox statusLine = new HBox(10);
        statusLine.getChildren().addAll(
                badge("Profil : " + controller.getCitizenState(user), BLUE),
                badge(controller.getRouteStatusLabel(user), GREEN)
        );

        card.getChildren().addAll(top, statusLine);
        return card;
    }

    private VBox buildRecommendedShelterPanel() {
        VBox card = glassCard(22);

        Zone refuge = controller.getNearestSafeRefuge(user);

        Label small = label("Refuge recommandé", MUTED, 12, true);
        Label name = label(refuge != null ? refuge.getName() : "Aucun refuge disponible", WHITE, 22, true);
        name.setWrapText(true);

        Label desc = label(
                refuge != null ? controller.shortDescription(refuge) : "Aucun refuge accessible n’a été trouvé.",
                LIGHT,
                13,
                false
        );
        desc.setWrapText(true);

        HBox infos = new HBox(10);
        infos.getChildren().addAll(
                compactInfo("Distance", controller.getDistanceLabel(user)),
                compactInfo("Temps", controller.getEtaLabel(user)),
                compactInfo("Altitude", refuge != null ? String.format("%.0f m", refuge.getAltitude()) : "--")
        );

        for (Node n : infos.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        Button btn = blueButton("Voir l’itinéraire");
        btn.setDisable(refuge == null);
        btn.setOnAction(e -> {
            if (refuge != null) {
                openRouteToRefuge(refuge);
            }
        });

        card.getChildren().addAll(small, name, desc, infos, btn);
        return card;
    }

    private VBox buildAlertPanel() {
        VBox alertPanel = glassCard(20);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        StackPane icon = smallLineIcon("!", RED);

        VBox titleTexts = new VBox(3);
        Label alertTitle = label("Dernière alerte", WHITE, 18, true);
        Label alertSub = label("Consigne prioritaire à consulter", MUTED, 12, false);
        titleTexts.getChildren().addAll(alertTitle, alertSub);

        titleRow.getChildren().addAll(icon, titleTexts);

        Alert latest = controller.getLatestCriticalAlert();

        Label alertTxt = label(
                latest != null
                        ? latest.getDescription() + " — " + latest.getLocalisation()
                        : "Aucune alerte critique personnalisée pour le moment. Restez attentif aux consignes officielles.",
                LIGHT,
                13,
                false
        );
        alertTxt.setWrapText(true);

        Hyperlink link = new Hyperlink("Voir toutes les alertes");
        link.setTextFill(Color.web(BLUE_2));
        link.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        link.setOnAction(e -> {
            CitizenAlertsView alertsView = new CitizenAlertsView();
            new CitizenAlertsController(alertsView, controller.getAlertSystem());
            showPage(alertsView);
        });

        alertPanel.getChildren().addAll(titleRow, alertTxt, link);
        return alertPanel;
    }

    private VBox buildRoutePanel() {
        VBox routePanel = glassCard(20);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallLineIcon("↗", BLUE_2);

        VBox titleTexts = new VBox(3);
        Label routeTitle = label("Plan d’évacuation", WHITE, 18, true);
        Label routeSub = label("Trajet conseillé vers la zone sécurisée", MUTED, 12, false);
        titleTexts.getChildren().addAll(routeTitle, routeSub);

        titleRow.getChildren().addAll(icon, titleTexts);

        VBox steps = new VBox(10);
        steps.getChildren().addAll(
                routeStep("Départ", controller.getPositionLabel(user), BLUE_2),
                routeStep("Arrivée", controller.getTargetRefugeLabel(user), GREEN),
                routeStep("Statut", controller.getRouteStatusLabel(user), ORANGE)
        );

        Button btnMap = blueButton("Suivre sur la carte");
        btnMap.setOnAction(e -> openRouteToRefuge(controller.getNearestSafeRefuge(user)));

        routePanel.getChildren().addAll(titleRow, steps, btnMap);
        return routePanel;
    }

    private VBox buildQuickActions() {
        VBox card = glassCard(20);

        Label title = label("Actions rapides", WHITE, 18, true);
        Label subtitle = label("Accédez rapidement aux pages utiles de votre espace citoyen.", LIGHT, 13, false);

        HBox actions = new HBox(12);
        actions.getChildren().addAll(
                actionButton("Voir la carte", "⌖", () -> showPage(buildMapPage(null))),
                actionButton("Mes refuges", "⌂", () -> showPage(new CitizenRefugesView(controller, user, this::openRouteToRefuge))),
                actionButton("Mes trajets", "⇢", () -> showPage(new CitizenRoutesView(controller, user, mapComponent))),
                actionButton("Mon profil", "◎", () -> showPage(new CitizenProfileView(controller, user)))
        );

        for (Node n : actions.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        card.getChildren().addAll(title, subtitle, actions);
        return card;
    }

    private Button actionButton(String text, String icon, Runnable action) {
        Button button = new Button(icon + "  " + text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPadding(new Insets(13, 16, 13, 16));
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.055);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        button.setOnAction(e -> action.run());
        return button;
    }

    private HBox routeStep(String label, String value, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane dot = new StackPane();
        dot.setPrefSize(12, 12);
        dot.setMinSize(12, 12);
        dot.setMaxSize(12, 12);
        dot.setStyle("-fx-background-color:" + color + "; -fx-background-radius:999;");

        VBox text = new VBox(2);
        text.getChildren().addAll(
                label(label, MUTED, 11, true),
                label(value, WHITE, 13, false)
        );

        row.getChildren().addAll(dot, text);
        return row;
    }

    private VBox compactInfo(String title, String value) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(10));
        box.setStyle(
                "-fx-background-color:rgba(255,255,255,0.045);" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:rgba(255,255,255,0.06);" +
                        "-fx-border-radius:12;"
        );

        box.getChildren().addAll(
                label(value, WHITE, 15, true),
                label(title, MUTED, 11, true)
        );
        return box;
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
            if (z instanceof Shelter && !z.isFlooded() && !z.isEvacuated()) {
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
        VBox chip = new VBox(6);
        chip.setPrefWidth(190);
        chip.setPadding(new Insets(12));
        chip.setStyle(
                "-fx-background-color:rgba(255,255,255,0.06);" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:rgba(255,255,255,0.12);" +
                        "-fx-border-radius:12;"
        );

        Label name = label("⌂ " + z.getName(), WHITE, 13, true);
        name.setWrapText(true);

        Label altitude = muted("Altitude : " + String.format("%.0f m", z.getAltitude()), 11);

        Button btn = blueButton("Itinéraire");
        btn.setOnAction(e -> openRouteToRefuge(z));

        chip.getChildren().addAll(name, altitude, btn);
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
        if (activeButton != null) {
            activeButton.setStyle(sidebarStyle(false));
        }

        activeButton = selected;

        if (activeButton != null) {
            activeButton.setStyle(sidebarStyle(true));
        }
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
            if (btn != activeButton) {
                btn.setStyle(sidebarStyle(false));
            }
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
            default:
                icon.setContent("M4 4 H20 V20 H4 Z");
                break;
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

    private VBox miniCard(String title, String value, String icon, String color) {
        VBox c = glassCard(16);
        c.setMinWidth(170);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(smallLineIcon(icon, color), label(title, MUTED, 12, true));

        Label v = label(value, WHITE, 14, true);
        v.setWrapText(true);

        c.getChildren().addAll(top, v);
        return c;
    }

    private StackPane roundIcon(String text, String color, int size) {
        StackPane icon = new StackPane();
        icon.setPrefSize(size, size);
        icon.setMinSize(size, size);
        icon.setMaxSize(size, size);
        icon.setStyle(
                "-fx-background-color:" + color + ";" +
                        "-fx-background-radius:999;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0, 0, 6);"
        );

        Label symbol = label(text, WHITE, size >= 60 ? 28 : 18, true);
        icon.getChildren().add(symbol);
        return icon;
    }

    private StackPane smallLineIcon(String text, String color) {
        StackPane icon = new StackPane();
        icon.setPrefSize(34, 34);
        icon.setMinSize(34, 34);
        icon.setMaxSize(34, 34);
        icon.setStyle(
                "-fx-background-color:rgba(22,131,255,0.12);" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:" + color + ";" +
                        "-fx-border-radius:12;"
        );

        Label symbol = label(text, WHITE, text.length() > 1 ? 13 : 17, true);
        icon.getChildren().add(symbol);
        return icon;
    }

    private Label badge(String text, String color) {
        Label badge = label(text, WHITE, 11, true);
        badge.setPadding(new Insets(6, 10, 6, 10));
        badge.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 999;"
        );
        return badge;
    }

    private VBox glassCard(int padding) {
        VBox box = new VBox(12);
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
        b.setStyle(
                "-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff);" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:8;" +
                        "-fx-font-weight:bold;" +
                        "-fx-cursor:hand;"
        );
        return b;
    }

    private Button darkButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color:rgba(255,255,255,0.07);" +
                        "-fx-text-fill:#b8c7dd;" +
                        "-fx-border-color:rgba(255,255,255,0.16);" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;" +
                        "-fx-cursor:hand;"
        );
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
