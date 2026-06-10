package view;

import java.util.Map;

import app.Main;
import controller.AdminPage.AdminAlertsController;
import controller.AdminPage.AdminController;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import model.agent.Agent;
import model.simulation.FloodSimulation;
import model.simulation.SimulationDataService;
import model.zone.ZoneManager;

public class AdminDashboardView extends BorderPane {

    private static final String BG_DARK = "#0d1b2a";
    private static final String BG_PANEL = "#132337";
    private static final String BG_CARD = "#1a2f45";
    private static final String BG_SIDEBAR = "#0f1e30";
    private static final String ACCENT_BLUE = "#1e88e5";
    private static final String ACCENT_CYAN = "#26c6da";
    private static final String ACCENT_GREEN = "#43a047";
    private static final String ACCENT_ORANGE = "#fb8c00";
    private static final String ACCENT_RED = "#e53935";
    private static final String TEXT_PRIMARY = "#e8f0fe";
    private static final String TEXT_MUTED = "#8eaabf";
    private static final String BORDER = "#1e3a52";

    // ── État ──────────────────────────────────────────────────────────────────
    private final AdminController ctrl;
    private String currentSection = "dashboard";
    private VBox sidebarBox;
    private StackPane contentArea;
    private final SimulationDataService dataService;

    // ── Constructeur (appelé par Main : new AdminDashboardView()) ─────────────
    public AdminDashboardView(AdminController controller, SimulationDataService dataService) {
        this.ctrl = controller;
        this.dataService = dataService;
        this.setStyle("-fx-background-color: " + BG_DARK + ";");
        this.setTop(buildTopBar());
        this.setLeft(buildSidebar());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: " + BG_DARK + ";");
        this.setCenter(contentArea);

        showDashboard();
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));
        bar.setStyle("-fx-background-color: " + BG_SIDEBAR
                + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Rectangle logo = new Rectangle(32, 32);
        logo.setArcWidth(8);
        logo.setArcHeight(8);
        logo.setFill(Color.web(ACCENT_BLUE));

        Label title = new Label("Inondation");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(TEXT_PRIMARY));

        Label sub = new Label("Administration & Gestion d'urgence");
        sub.setFont(Font.font("Segoe UI", 12));
        sub.setTextFill(Color.web(TEXT_MUTED));

        VBox titles = new VBox(2, title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timerLbl = new Label("Simulation en cours");
        timerLbl.setFont(Font.font("Segoe UI", 13));
        timerLbl.setTextFill(Color.web(ACCENT_CYAN));

        Button simBtn = btn("Simulation", ACCENT_CYAN);
        simBtn.setOnAction(e -> Main.showSimulationView());

        Button refreshBtn = btn("Actualiser", ACCENT_BLUE);
        refreshBtn.setOnAction(e -> {
            ctrl.loadAgents();
            refreshAll();
        });

        Button logoutBtn = btn("Déconnexion", BG_CARD);
        logoutBtn.setStyle("-fx-background-color: " + BG_CARD
                + "; -fx-text-fill: " + TEXT_MUTED
                + "; -fx-border-color: " + BORDER
                + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            Main.currentUser = null;
            Main.showLoginView();
        });

        bar.getChildren().addAll(logo, titles, spacer, timerLbl, simBtn, refreshBtn, logoutBtn);
        return bar;
    }

    private VBox buildSidebar() {
        sidebarBox = new VBox(4);
        sidebarBox.setPrefWidth(210);
        sidebarBox.setMinWidth(210);
        sidebarBox.setPadding(new Insets(20, 12, 20, 12));
        sidebarBox.setStyle("-fx-background-color: " + BG_SIDEBAR
                + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 1 0 0;");

        sidebarBox.getChildren().addAll(
                navBtn("▦", "Tableau de bord", "dashboard"),
                navBtn("⌂", "Citoyens", "citizens"),
                navBtn("✚", "Agents secours", "rescue"),
                navBtn("◆", "Admins", "admins"),
                navBtn("!", "Alertes", "alerts"),
                navBtn("▥", "Statistiques", "stats")
        );

        return sidebarBox;
    }

    private Button navBtn(String icon, String label, String section) {
        Button b = new Button(icon + "  " + label);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font("Segoe UI", 13));
        b.setPadding(new Insets(10, 14, 10, 14));
        b.setStyle(navStyle(section.equals(currentSection)));

        b.setOnAction(e -> {
            currentSection = section;
            refreshNavStyles();

            switch (section) {
                case "dashboard" -> showDashboard();
                case "citizens" -> showAgentList("citizen", "Citoyens");
                case "rescue" -> showAgentList("rescueAgent", "Agents de Secours");
                case "admins" -> showAgentList("admin", "Administrateurs");
                case "alerts" -> showAlerts();
                case "stats" -> showStats();
            }
        });

        return b;
    }

    private String navStyle(boolean active) {
        return active
                ? "-fx-background-color: " + ACCENT_BLUE + "22; -fx-text-fill: " + ACCENT_CYAN
                + "; -fx-background-radius: 6; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED
                + "; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private void refreshNavStyles() {
        for (Node n : sidebarBox.getChildren()) {
            if (n instanceof Button b) {
                String txt = b.getText();

                boolean active = (currentSection.equals("dashboard") && txt.contains("Tableau"))
                        || (currentSection.equals("citizens") && txt.contains("Citoyens"))
                        || (currentSection.equals("rescue") && txt.contains("secours"))
                        || (currentSection.equals("admins") && txt.contains("Admins"))
                        || (currentSection.equals("alerts") && txt.contains("Alertes"))
                        || (currentSection.equals("stats") && txt.contains("Statistiques"));

                b.setStyle(navStyle(active));
            }
        }
    }

    private void showDashboard() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label("Tableau de bord");
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLbl.setTextFill(Color.web(TEXT_PRIMARY));

        String nom = Main.currentUser != null
                ? Main.currentUser.getFirstName() + " " + Main.currentUser.getLastName()
                : "Administrateur";

        Label welcomeLbl = new Label("Connecté en tant que : " + nom);
        welcomeLbl.setFont(Font.font("Segoe UI", 13));
        welcomeLbl.setTextFill(Color.web(TEXT_MUTED));

        titleRow.getChildren().addAll(titleLbl, welcomeLbl);

        FlowPane kpis = new FlowPane();
        kpis.setHgap(16);
        kpis.setVgap(16);
        kpis.setPrefWrapLength(1100);

        kpis.getChildren().addAll(
                kpiCard("Total agents", String.valueOf(ctrl.getTotalAgents()), ACCENT_CYAN, "AG"),
                kpiCard("Citoyens", String.valueOf(ctrl.getTotalCitizens()), ACCENT_BLUE, "CI"),
                kpiCard("Agents secours", String.valueOf(ctrl.getTotalRescueAgents()), ACCENT_GREEN, "SE"),
                kpiCard("Citoyens à risque", String.valueOf(ctrl.getAtRiskCount()), ACCENT_ORANGE, "!"),
                kpiCard("Citoyens sauvés", String.valueOf(ctrl.getSavedCount()), ACCENT_GREEN, "OK"),
                kpiCard("En intervention", String.valueOf(ctrl.getActiveRescueCount()), ACCENT_RED, "IN")
        );

        HBox bottomRow = new HBox(16);
        VBox chart = buildStateChart();
        HBox.setHgrow(chart, Priority.ALWAYS);

        bottomRow.getChildren().addAll(
                chart,
                statusPanel("Statut Agents Secours", ctrl.getRescueStateBreakdown(), ACCENT_GREEN),
                statusPanel("Statut Citoyens", ctrl.getCitizenStateBreakdown(), ACCENT_BLUE)
        );

        MapView mapView = new MapView(new ZoneManager().getZones());
        SwingNode sn = mapView.getSwingNode();

        sn.minWidth(1000);
        sn.maxHeight(380);
        sn.maxWidth(1000);
        sn.maxHeight(380);

        

        javafx.application.Platform.runLater(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                mapView.getMapViewer().setZoom(6);
                mapView.getMapViewer().setAddressLocation(new org.jxmapviewer.viewer.GeoPosition(45.7640, 4.8357));
                mapView.getMapViewer().repaint();
            });
        });

    
        // Lier un détecteur de niveau d'eau qui alimente le système d'alertes
        model.simulation.FloodSimulation localSim = app.Main.getSharedSimulation();
        model.sensor.WaterLevelDetector detector = new model.sensor.WaterLevelDetector(localSim.getAlertSystem());
        // Polling initial pour démonstration : met à jour la carte et crée des alertes
        // si nécessaire
        detector.pollRandom((zone, lvl) -> {
            try {
                mapView.updateZoneWithWaterLevel(zone, lvl);
            } catch (Exception ignored) {
            }
        });

        VBox tableBox = new VBox(10);
        tableBox.setPadding(new Insets(16));
        tableBox.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-background-radius: 10;");

        Label recentLbl = new Label("Derniers agents enregistrés");
        recentLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        recentLbl.setTextFill(Color.web(TEXT_PRIMARY));

        tableBox.getChildren().addAll(recentLbl, buildTable(ctrl.getAllAgents(), true));

        root.getChildren().addAll(titleRow, kpis, bottomRow, mapView.getSwingNode(), tableBox);

        fadeIn(root);
        contentArea.getChildren().setAll(scrollWrap(root));
    }

    private void showAlerts() {
        AdminAlertsView alertsView = new AdminAlertsView();
        new AdminAlertsController(alertsView, app.Main.getSharedSimulation().getAlertSystem());

        fadeIn(alertsView);
        contentArea.getChildren().setAll(alertsView);
    }

    private VBox kpiCard(String label, String value, String accent, String icon) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setPrefWidth(180);
        card.setMinWidth(180);
        card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 10;"
                + "-fx-border-color: " + accent + "44; -fx-border-radius: 10; -fx-border-width: 1;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        iconLbl.setTextFill(Color.web(accent));

        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 12));
        lbl.setTextFill(Color.web(TEXT_MUTED));
        lbl.setWrapText(true);
        lbl.setMaxWidth(130);

        header.getChildren().addAll(iconLbl, lbl);

        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 34));
        val.setTextFill(Color.web(TEXT_PRIMARY));

        Rectangle bar = new Rectangle(46, 3);
        bar.setArcWidth(3);
        bar.setArcHeight(3);
        bar.setFill(Color.web(accent));

        card.getChildren().addAll(header, val, bar);
        return card;
    }

    private VBox buildStateChart() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-background-radius: 10;");

        Label title = new Label("Répartition états citoyens");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setTextFill(Color.web(TEXT_PRIMARY));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickLabelFill(Color.web(TEXT_MUTED));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFill(Color.web(TEXT_MUTED));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setHorizontalGridLinesVisible(false);
        chart.setPrefHeight(210);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        ctrl.getCitizenStateBreakdown()
                .forEach((state, count) -> series.getData().add(new XYChart.Data<>(state, count)));

        chart.getData().add(series);

        box.getChildren().addAll(title, chart);
        return box;
    }

    private VBox statusPanel(String title, Map<String, Long> data, String accent) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        box.setPrefWidth(260);
        box.setMinWidth(260);
        box.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-background-radius: 10;");

        Label lbl = new Label(title);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lbl.setTextFill(Color.web(TEXT_PRIMARY));
        lbl.setWrapText(true);
        box.getChildren().add(lbl);

        long total = data.values().stream().mapToLong(Long::longValue).sum();

        data.forEach((state, count) -> {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Circle dot = new Circle(5);
            dot.setFill(Color.web(stateColor(state)));

            Label stateLbl = new Label(state);
            stateLbl.setFont(Font.font("Segoe UI", 12));
            stateLbl.setTextFill(Color.web(TEXT_MUTED));
            stateLbl.setPrefWidth(120);
            stateLbl.setWrapText(true);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label countLbl = new Label(count + (total > 0 ? " (" + (count * 100 / total) + "%)" : ""));
            countLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            countLbl.setTextFill(Color.web(accent));

            row.getChildren().addAll(dot, stateLbl, spacer, countLbl);
            box.getChildren().add(row);

            StackPane track = new StackPane();
            track.setStyle("-fx-background-color: " + BORDER + "; -fx-background-radius: 3;");
            track.setPrefHeight(4);
            track.setMaxWidth(Double.MAX_VALUE);

            Region fill = new Region();
            fill.setPrefHeight(4);
            fill.setPrefWidth(total > 0 ? ((double) count / total) * 200 : 0);
            fill.setStyle("-fx-background-color: " + stateColor(state) + "; -fx-background-radius: 3;");

            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().add(fill);
            box.getChildren().add(track);
        });

        return box;
    }

    private void showAgentList(String type, String sectionTitle) {
        ObservableList<Agent> source = switch (type) {
            case "citizen" -> ctrl.getCitizens();
            case "rescueAgent" -> ctrl.getRescueAgents();
            case "admin" -> ctrl.getAdmins();
            default -> ctrl.getAllAgents();
        };

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(sectionTitle);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLbl.setTextFill(Color.web(TEXT_PRIMARY));

        Label badge = new Label(source.size() + " entrées");
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setFont(Font.font("Segoe UI", 12));
        badge.setTextFill(Color.web(ACCENT_CYAN));
        badge.setStyle("-fx-background-color: " + ACCENT_CYAN + "22; -fx-background-radius: 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField search = new TextField();
        search.setPromptText("Rechercher…");
        search.setPrefWidth(260);
        search.setStyle("-fx-background-color: " + BG_CARD + "; -fx-text-fill: " + TEXT_PRIMARY
                + "; -fx-prompt-text-fill: " + TEXT_MUTED
                + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6; -fx-background-radius: 6;");

        header.getChildren().addAll(titleLbl, badge, spacer, search);

        TableView<Agent> table = buildTable(source, false);

        FilteredList<Agent> filtered = new FilteredList<>(source, a -> true);
        table.setItems(filtered);

        search.textProperty().addListener((obs, o, q) -> {
            String lq = q == null ? "" : q.toLowerCase();

            filtered.setPredicate(a -> lq.isBlank()
                    || String.valueOf(a.getId()).contains(lq)
                    || (a.getFirstName() != null && a.getFirstName().toLowerCase().contains(lq))
                    || (a.getLastName() != null && a.getLastName().toLowerCase().contains(lq))
                    || (a.getEmail() != null && a.getEmail().toLowerCase().contains(lq))
                    || AdminController.stateOf(a).toLowerCase().contains(lq));
        });

        HBox toolbar = new HBox(10);

        Button delBtn = btn("Supprimer la sélection", ACCENT_RED);
        delBtn.setOnAction(e -> {
            Agent sel = table.getSelectionModel().getSelectedItem();

            if (sel == null) return;

            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Supprimer " + sel.getFirstName() + " " + sel.getLastName() + " ?",
                    ButtonType.YES,
                    ButtonType.NO
            );

            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    ctrl.deleteAgent(sel);
                }
            });
        });

        toolbar.getChildren().add(delBtn);

        VBox tableBox = new VBox(10);
        tableBox.setPadding(new Insets(16));
        tableBox.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-background-radius: 10;");
        tableBox.getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(header, tableBox);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        fadeIn(root);
        contentArea.getChildren().setAll(scrollWrap(root));
    }

    private TableView<Agent> buildTable(ObservableList<Agent> items, boolean limitRows) {
        TableView<Agent> table = new TableView<>();
        table.setStyle("-fx-background-color: " + BG_CARD + "; -fx-text-fill: " + TEXT_PRIMARY
                + "; -fx-border-color: " + BORDER + ";");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(limitRows ? 260 : 520);

        TableColumn<Agent, String> colId = strCol("ID", a -> String.valueOf(a.getId()), 60);
        TableColumn<Agent, String> colType = strCol("Type", a -> AdminController.typeOf(a), 130);
        TableColumn<Agent, String> colFirst = strCol("Prénom", Agent::getFirstName, 130);
        TableColumn<Agent, String> colLast = strCol("Nom", Agent::getLastName, 130);
        TableColumn<Agent, String> colEmail = strCol("Email", Agent::getEmail, 220);
        TableColumn<Agent, String> colPhone = strCol("Tél.", Agent::getPhone, 120);
        TableColumn<Agent, String> colCity = strCol("Ville", Agent::getCity, 120);

        TableColumn<Agent, String> colState = new TableColumn<>("État");
        colState.setPrefWidth(150);
        colState.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                AdminController.stateOf(c.getValue())));

        colState.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String state, boolean empty) {
                super.updateItem(state, empty);

                if (empty || state == null || state.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(state);
                badge.setPadding(new Insets(2, 8, 2, 8));
                badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                badge.setTextFill(Color.WHITE);
                badge.setStyle("-fx-background-color: " + stateColor(state)
                        + "; -fx-background-radius: 10;");

                setGraphic(badge);
                setText(null);
            }
        });

        TableColumn<Agent, String> colPos = strCol("Position",
                a -> a.getPosition() != null ? a.getPosition().toString() : "—", 220);

        TableColumn<Agent, String> colSaved = strCol("Sauvé",
                a -> a.isSaved() ? "Oui" : "—", 80);

        table.getColumns().addAll(
                colId,
                colType,
                colFirst,
                colLast,
                colEmail,
                colPhone,
                colCity,
                colState,
                colPos,
                colSaved
        );

        ObservableList<Agent> data = (limitRows && items.size() > 8)
                ? javafx.collections.FXCollections.observableArrayList(items.subList(0, 8))
                : items;

        table.setItems(data);

        table.setRowFactory(tv -> {
            TableRow<Agent> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");

            row.hoverProperty().addListener((obs, old, hover) -> row.setStyle(hover
                    ? "-fx-background-color: " + ACCENT_BLUE + "22;"
                    : "-fx-background-color: transparent;"));

            return row;
        });

        return table;
    }

    private TableColumn<Agent, String> strCol(String title,
                                              java.util.function.Function<Agent, String> fn,
                                              double width) {
        TableColumn<Agent, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(fn.apply(c.getValue())));

        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label lbl = new Label(item);
                lbl.setTextFill(Color.web(TEXT_PRIMARY));
                lbl.setWrapText(false);
                lbl.setMaxWidth(tc.getWidth() - 12);

                setGraphic(lbl);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });

        return col;
    }

    private void showStats() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label titleLbl = new Label("Statistiques");
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLbl.setTextFill(Color.web(TEXT_PRIMARY));

        HBox panels = new HBox(16,
                statusPanel("États Citoyens", ctrl.getCitizenStateBreakdown(), ACCENT_BLUE),
                statusPanel("États Agents Secours", ctrl.getRescueStateBreakdown(), ACCENT_GREEN)
        );

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-background-radius: 10;");

        addStatRow(grid, 0, "Total agents", String.valueOf(ctrl.getTotalAgents()), TEXT_PRIMARY);
        addStatRow(grid, 1, "Citoyens", String.valueOf(ctrl.getTotalCitizens()), ACCENT_BLUE);
        addStatRow(grid, 2, "Agents de secours", String.valueOf(ctrl.getTotalRescueAgents()), ACCENT_GREEN);
        addStatRow(grid, 3, "Citoyens à risque", String.valueOf(ctrl.getAtRiskCount()), ACCENT_ORANGE);
        addStatRow(grid, 4, "Citoyens sauvés", String.valueOf(ctrl.getSavedCount()), ACCENT_GREEN);
        addStatRow(grid, 5, "Agents en intervention", String.valueOf(ctrl.getActiveRescueCount()), ACCENT_RED);
        addStatRow(grid, 6, "Agents disponibles", String.valueOf(ctrl.getAvailableRescueCount()), ACCENT_CYAN);

        root.getChildren().addAll(titleLbl, panels, grid);

        fadeIn(root);
        contentArea.getChildren().setAll(scrollWrap(root));
    }

    private void addStatRow(GridPane grid, int row, String label, String value, String color) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 13));
        lbl.setTextFill(Color.web(TEXT_MUTED));

        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        val.setTextFill(Color.web(color));

        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private void refreshAll() {
        switch (currentSection) {
            case "dashboard" -> showDashboard();
            case "citizens" -> showAgentList("citizen", "Citoyens");
            case "rescue" -> showAgentList("rescueAgent", "Agents de Secours");
            case "admins" -> showAgentList("admin", "Administrateurs");
            case "alerts" -> showAlerts();
            case "stats" -> showStats();
        }
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        b.setTextFill(Color.WHITE);
        b.setPadding(new Insets(8, 16, 8, 16));
        b.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6; -fx-cursor: hand;");

        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: derive(" + color + ", 20%);"
                        + " -fx-background-radius: 6; -fx-cursor: hand;"));

        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: " + color + "; -fx-background-radius: 6; -fx-cursor: hand;"));

        return b;
    }

    private ScrollPane scrollWrap(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: " + BG_DARK + "; -fx-background-color: " + BG_DARK + ";");
        return sp;
    }

    private void fadeIn(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private String stateColor(String state) {
        if (state == null) {
            return TEXT_MUTED;
        }

        return switch (state.toUpperCase()) {
            case "CALME" -> ACCENT_GREEN;
            case "CALM" -> ACCENT_GREEN;
            case "PANIQUE" -> ACCENT_ORANGE;
            case "STRESSED" -> ACCENT_ORANGE;
            case "BLESSE" -> ACCENT_RED;
            case "INJURED" -> ACCENT_RED;
            case "DISPONIBLE" -> ACCENT_CYAN;
            case "EN_INTERVENTION" -> ACCENT_RED;
            case "EN_ROUTE" -> ACCENT_BLUE;
            default -> TEXT_MUTED;
        };
    }
}
