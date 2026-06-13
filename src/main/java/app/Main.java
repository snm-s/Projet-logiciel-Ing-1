package app;

import controller.AdminPage.AdminController;
import controller.AdminPage.SimulationController;
import controller.AuthPage.ForgotPasswordController;
import controller.CitizenPage.CitizenController;
import controller.MapController;
import controller.RescuePage.RescueController;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.agent.Agent;
import model.simulation.FloodSimulation;
import model.simulation.SimulationDataService;
import view.AdminDashboardView;
import view.CitizenDashboardView;
import view.ForgotPasswordView;
import view.LoginView;
import view.MapView;
import view.RegisterView;
import view.RescueDashboardView;
import view.SimulationView;
import view.WelcomeView;

public class Main extends Application {

    private static Stage mainStage;
    public static Agent currentUser;
    public static ListView<String> alertesListView;
    public static TextArea logArea;
    private static StackPane rootContainer = new StackPane();

    // ── Instance unique partagée ──────────────────────────────────────────
    private static SimulationDataService sharedDataService;
    private static FloodSimulation       sharedSimulation;
    private static SimulationController  sharedSimCtrl;
    private static AdminController       sharedAdminCtrl;
    private static MapController sharedMapController;

    @Override
    /**
     * Starts.
     * @param stage the stage.
     */
    public void start(Stage stage) {
        mainStage = stage;
        Scene scene = new Scene(rootContainer, 1000, 650);
        mainStage.setScene(scene);

        // Créer une seule fois les objets partagés
        sharedDataService = new SimulationDataService();
        sharedSimulation  = new FloodSimulation(sharedDataService);
        sharedSimCtrl     = new SimulationController(sharedSimulation, sharedDataService);
        sharedAdminCtrl   = new AdminController(sharedDataService, sharedSimulation);

        MapView sharedMapView = new MapView(sharedSimulation.getZones());
        sharedMapController  = new MapController(sharedMapView, 
                                                sharedSimulation.getZones(),
                                                sharedSimulation.getAgents());
        sharedSimCtrl.setMapController(sharedMapController);


        // ── Observer agents : AdminController se resynchronise ───────────
        sharedSimulation.addAgentObserver(updatedAgents ->
            javafx.application.Platform.runLater(() ->
                sharedAdminCtrl.loadAgents()
            )
        );

        // ── Observer zones : AdminController se resynchronise ────────────
        sharedSimulation.addZoneObserver(updatedZones ->
            javafx.application.Platform.runLater(() ->
                sharedAdminCtrl.loadZones()
            )
        );



        showWelcomeView();
        stage.setTitle("Flood Simulation - Agents & Graphs");
        stage.setMinWidth(850);
        stage.setMinHeight(550);
        stage.show();
    }

    // ── Accesseurs statiques ──────────────────────────────────────────────
    /**
     * Returns the shared simulation.
     * @return the FloodSimulation.
     */
    public static FloodSimulation      getSharedSimulation()  { return sharedSimulation; }
    /**
     * Returns the shared data service.
     * @return the SimulationDataService.
     */
    public static SimulationDataService getSharedDataService() { return sharedDataService; }
    /**
     * Returns the shared sim ctrl.
     * @return the SimulationController.
     */
    public static SimulationController  getSharedSimCtrl()     { return sharedSimCtrl; }
    /**
     * Returns the shared admin ctrl.
     * @return the AdminController.
     */
    public static AdminController       getSharedAdminCtrl()   { return sharedAdminCtrl; }

    /**
     * Sets the shared map controller.
     * @param mc the mc.
     */
    public static void setSharedMapController(MapController mc) {
        sharedMapController = mc;
    }
    /**
     * Returns the shared map controller.
     * @return the MapController.
     */
    public static MapController getSharedMapController() { return sharedMapController; }
    /**
     * Returns the shared map view.
     * @return the MapView.
     */
    public static MapView       getSharedMapView()       { return sharedMapController.getMapView(); }


    // ── Navigation ────────────────────────────────────────────────────────
    /**
     * Displays welcome view.
     */
    public static void showWelcomeView() {
        WelcomeView welcomeView = new WelcomeView();
        rootContainer.getChildren().setAll(welcomeView);

    }

    /**
     * Displays login view.
     */
    public static void showLoginView() {
        LoginView loginView = new LoginView();
        new controller.AuthPage.LoginController(loginView, mainStage);
        rootContainer.getChildren().setAll(loginView);

    }

    /**
     * Displays register view.
     */
    public static void showRegisterView() {
        RegisterView registerView = new RegisterView();
        new controller.AuthPage.RegisterController(registerView, mainStage);
        rootContainer.getChildren().setAll(registerView);
   
    }

    /**
     * Displays forgot password view.
     * @param email the email.
     */
    public static void showForgotPasswordView(String email) {
        ForgotPasswordView view = new ForgotPasswordView();
        new ForgotPasswordController(view, mainStage, email);
        rootContainer.getChildren().setAll(view);

    }

    /**
     * Displays simulation view.
     */
    public static void showSimulationView() {
        // IMPORTANT : on réutilise le contrôleur partagé.
        // Sinon la vue simulation a un nouveau SimulationController sans MapController,
        // donc les alertes partent mais aucun AgentMovement n'est créé/avancé.
        SimulationController simulationController = sharedSimCtrl;
        simulationController.setMapController(sharedMapController);
        SimulationView simulationView = new SimulationView(simulationController);
        rootContainer.getChildren().setAll(simulationView);

  
        mainStage.setTitle("Flood Simulation - Administration");
    }

    /**
     * Displays dashboard view.
     * @param role the role.
     */
    public static void showDashboardView(String role) {
        if (role == null) role = "citizen";
        String cleanRole = role.trim().toLowerCase();
        
        // 1. On définit la vue à afficher
        Parent dashboardView;
        String title;

        switch (cleanRole) {
            case "admin":
                dashboardView = new AdminDashboardView(sharedAdminCtrl, sharedDataService);
                title = "Flood Simulation - Admin Panel";
                break;

            case "rescue":
            case "rescueagent":
                dashboardView = new RescueDashboardView(new RescueController(sharedSimulation));
                title = "Flood Simulation - Rescue Command";
                break;

            case "citizen":
            default:
                dashboardView = new CitizenDashboardView(new CitizenController(sharedSimulation));
                title = "Flood Simulation - Citizen Portal";
                break;
        }

        
        rootContainer.getChildren().setAll(dashboardView);
        mainStage.setTitle(title);
    }

    /**
     * Performs main.
     * @param args the args.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
