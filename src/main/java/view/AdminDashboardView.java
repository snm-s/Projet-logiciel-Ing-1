package view;

import app.Main;
import controller.AdminController;
import controller.SimulationController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;

public class AdminDashboardView extends BorderPane {

    private final StackPane centralViewContainer;

    private VBox supervisionPage;
    private VBox registeredAgentsPage;
    private VBox graphPage;
    private VBox simulationPage;
    private WebView mapWebView;

    private final AdminController adminController;
    private final SimulationController simulationController;

    public AdminDashboardView(AdminController adminController) {
        this.adminController      = adminController;
        this.simulationController = new SimulationController();

        this.setPrefSize(1280, 750);
        this.setStyle("-fx-background-color: #f4f7fc;");

        // ==========================================
        // SIDEBAR
        // ==========================================
        VBox sidebar = new VBox(5);
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: #0b1a30;");
        sidebar.setPadding(new Insets(20, 15, 20, 15));

        Label lblLogo = new Label("🛡️ PC DES SECOURS");
        lblLogo.setTextFill(Color.WHITE);
        lblLogo.setFont(Font.font("System", FontWeight.BOLD, 16));
        Label lblSub = new Label("Système de Crise Cartographique");
        lblSub.setTextFill(Color.web("#a0b2ce"));
        lblSub.setFont(Font.font("System", 11));
        VBox headerBox = new VBox(3, lblLogo, lblSub);
        headerBox.setPadding(new Insets(10, 5, 30, 5));
        sidebar.getChildren().add(headerBox);

        Button btnHome = createMenuButton("🏠  Tableau de bord", false);
        Button btnSupervision = createMenuButton("🗺️   Carte de Supervision Live", true);
        Button btnGraph       = createMenuButton("🛠️   Modifications Graphe",       false);
        Button btnAgents      = createMenuButton("👥   Liste des Inscrits",         false);
        Button btnAlerts      = createMenuButton("⚠️   Liste des Alertes",         false);
        Button btnSimulation  = createMenuButton("⏱️   Moteur & Simulation",         false);
        Button btnStatistics  = createMenuButton("📊  Statistiques",         false);
        Button btnSettings    = createMenuButton("⚙️  Paramètres",         false);

        sidebar.getChildren().addAll(btnHome, btnSupervision, btnAgents, btnGraph,btnAlerts, btnSimulation, btnStatistics, btnSettings);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Button btnLogout = createMenuButton("🚪   Déconnexion", false);
        btnLogout.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-alignment: center-left; -fx-cursor: hand;");
        sidebar.getChildren().addAll(new Separator(), btnLogout);

        this.setLeft(sidebar);

        // ==========================================
        // CENTRAL AREA
        // ==========================================
        centralViewContainer = new StackPane();
        this.setCenter(centralViewContainer);

        buildSupervisionPage();
        buildRegisteredAgentsPage();
        buildGraphPage();
        buildSimulationPage();

        centralViewContainer.getChildren().add(supervisionPage);

        btnSupervision.setOnAction(e -> {
            showPage(supervisionPage);
            updateActiveTabs(btnSupervision, btnAgents, btnGraph, btnSimulation);
            refreshMap();
        });
        btnAgents.setOnAction(e -> {
            showPage(registeredAgentsPage);
            updateActiveTabs(btnAgents, btnSupervision, btnGraph, btnSimulation);
        });
        btnGraph.setOnAction(e -> {
            showPage(graphPage);
            updateActiveTabs(btnGraph, btnSupervision, btnAgents, btnSimulation);
        });
        btnSimulation.setOnAction(e -> {
            showPage(simulationPage);
            updateActiveTabs(btnSimulation, btnSupervision, btnAgents, btnGraph);
        });
        btnLogout.setOnAction(e -> {
            Main.currentUser = null;
            Main.showWelcomeView();
        });
    }

    // ── Page builders ───────────────────────────────────────────────────────────

    private void buildSupervisionPage() {
        supervisionPage = new VBox(15);
        supervisionPage.setPadding(new Insets(25));

        Label title = new Label("Supervision Globale - Carte Réelle (user.json)");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#0b1a30"));

        mapWebView = new WebView();
        VBox.setVgrow(mapWebView, Priority.ALWAYS);

        refreshMap();

        supervisionPage.getChildren().addAll(title, mapWebView);
    }

    private void refreshMap() {
        mapWebView.getEngine().loadContent(adminController.generateMapHtml());
    }

    private void buildRegisteredAgentsPage() {
        registeredAgentsPage = new VBox(15);
        registeredAgentsPage.setPadding(new Insets(25));

        Label title = new Label("Base de Données — Registre Matricule (user.json)");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        TableView<AgentInscrit> table = new TableView<>();
        table.setItems(adminController.getRegisteredAgents());

        TableColumn<AgentInscrit, Integer> colId        = new TableColumn<>("ID");
        TableColumn<AgentInscrit, String>  colType      = new TableColumn<>("Type");
        TableColumn<AgentInscrit, String>  colFirstName = new TableColumn<>("Prénom");
        TableColumn<AgentInscrit, String>  colLastName  = new TableColumn<>("Nom");
        TableColumn<AgentInscrit, String>  colEmail     = new TableColumn<>("Email");
        TableColumn<AgentInscrit, String>  colState     = new TableColumn<>("État");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));

        table.getColumns().addAll(colId, colType, colFirstName, colLastName, colEmail, colState);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        Button btnDelete = new Button("🗑️ Supprimer de la Base de Données");
        btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDelete.setOnAction(e -> {
            AgentInscrit selected = table.getSelectionModel().getSelectedItem();
            adminController.deleteAgent(selected);
        });

        registeredAgentsPage.getChildren().addAll(title, table, btnDelete);
    }

    private void buildGraphPage() {
        graphPage = new VBox(20);
        graphPage.setPadding(new Insets(25));
        graphPage.getChildren()
                .add(new Label("🛠️ Configuration du Graphe — Gestion des goulots d'accès (Pénalité de 2 cycles)"));
    }

    private void buildSimulationPage() {
        simulationPage = new VBox(0);
        simulationPage.setPadding(new Insets(0));
        simulationPage.setStyle("-fx-background-color: #f4f7fc;");

        SimulationView simulationView = new SimulationView(simulationController);
        VBox.setVgrow(simulationView, Priority.ALWAYS);
        simulationPage.getChildren().add(simulationView);
    }

    // ── UI helpers ──────────────────────────────────────────────────────────────

    private Button createMenuButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(44);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(0, 15, 0, 15));
        setButtonStyle(btn, isActive);
        return btn;
    }

    private void setButtonStyle(Button btn, boolean isActive) {
        if (isActive) {
            btn.setStyle(
                    "-fx-background-color: #0b5cbf; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold;");
        } else {
            btn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-background-radius: 6; -fx-cursor: hand;");
        }
    }

    private void showPage(VBox page) {
        centralViewContainer.getChildren().clear();
        centralViewContainer.getChildren().add(page);
    }

    private void updateActiveTabs(Button active, Button... others) {
        setButtonStyle(active, true);
        for (Button b : others)
            setButtonStyle(b, false);
    }

    // ========================================================
    // INNER MODEL CLASSES (JSON mapping for admin user list)
    // ========================================================
    public static class AgentInscrit {
        private String type;
        private int id;
        private String firstName;
        private String lastName;
        private String email;
        private String state;
        private String destination;
        private Position position;

        public AgentInscrit(String type, int id, String firstName, String lastName, String email, String state,
                String destination, Position position) {
            this.type        = type;
            this.id          = id;
            this.firstName   = firstName;
            this.lastName    = lastName;
            this.email       = email;
            this.state       = state;
            this.destination = destination;
            this.position    = position;
        }

        public String   getType()        { return type; }
        public int      getId()          { return id; }
        public String   getFirstName()   { return firstName; }
        public String   getLastName()    { return lastName; }
        public String   getEmail()       { return email; }
        public String   getState()       { return state; }
        public String   getDestination() { return destination; }
        public Position getPosition()    { return position; }
    }

    public static class Position {
        private double lat;
        private double lng;

        public Position(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }
}
