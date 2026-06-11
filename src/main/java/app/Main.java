package app;

import controller.AdminPage.AdminController;
import controller.AdminPage.SimulationController;
import controller.AuthPage.ForgotPasswordController;
import controller.CitizenPage.CitizenController;
import controller.MapController;
import controller.RescuePage.RescueController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
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

    // ── Instance unique partagée ──────────────────────────────────────────
    private static SimulationDataService sharedDataService;
    private static FloodSimulation       sharedSimulation;
    private static SimulationController  sharedSimCtrl;
    private static AdminController       sharedAdminCtrl;
    private static MapController sharedMapController;

    @Override
    public void start(Stage stage) {
        mainStage = stage;

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
    public static FloodSimulation      getSharedSimulation()  { return sharedSimulation; }
    public static SimulationDataService getSharedDataService() { return sharedDataService; }
    public static SimulationController  getSharedSimCtrl()     { return sharedSimCtrl; }
    public static AdminController       getSharedAdminCtrl()   { return sharedAdminCtrl; }

    public static void setSharedMapController(MapController mc) {
        sharedMapController = mc;
    }
    public static MapController getSharedMapController() { return sharedMapController; }
    public static MapView       getSharedMapView()       { return sharedMapController.getMapView(); }


    // ── Navigation ────────────────────────────────────────────────────────
    public static void showWelcomeView() {
        WelcomeView welcomeView = new WelcomeView();

        Scene scene = new Scene(welcomeView, 1000, 650);
        scene.setFill(Color.web("#060a12"));

        mainStage.setScene(scene);
    }

    public static void showLoginView() {
        LoginView loginView = new LoginView();
        new controller.AuthPage.LoginController(loginView, mainStage);

        Scene scene = new Scene(loginView, 1000, 650);
        scene.setFill(Color.web("#060a12"));

        mainStage.setScene(scene);
    }

    public static void showRegisterView() {
        RegisterView registerView = new RegisterView();
        new controller.AuthPage.RegisterController(registerView, mainStage);

        Scene scene = new Scene(registerView, 1000, 650);
        scene.setFill(Color.web("#060a12"));

        mainStage.setScene(scene);
    }

    public static void showForgotPasswordView(String email) {
        ForgotPasswordView view = new ForgotPasswordView();
        new ForgotPasswordController(view, mainStage, email);

        Scene scene = new Scene(view, 1000, 650);
        scene.setFill(Color.web("#060a12"));

        mainStage.setScene(scene);
    }

    public static void showSimulationView() {
        // IMPORTANT : on réutilise le contrôleur partagé.
        // Sinon la vue simulation a un nouveau SimulationController sans MapController,
        // donc les alertes partent mais aucun AgentMovement n'est créé/avancé.
        SimulationController simulationController = sharedSimCtrl;
        simulationController.setMapController(sharedMapController);
        SimulationView simulationView = new SimulationView(simulationController);

        Scene scene = new Scene(simulationView, 1100, 700);
        scene.setFill(Color.web("#060a12"));

        mainStage.setScene(scene);
        mainStage.setTitle("Flood Simulation - Administration");
    }

    public static void showDashboardView(String role) {
        if (role == null) role = "citizen";
        String cleanRole = role.trim().toLowerCase();
        Scene scene;
        switch (cleanRole) {
            case "admin":
                // AdminController lit depuis sharedSimulation
                scene = new Scene(
                    new AdminDashboardView(sharedAdminCtrl, sharedDataService),
                    1000, 650);
                scene.setFill(Color.web("#060a12"));
                mainStage.setTitle("Flood Simulation - Admin Panel");
                break;

            case "rescue":
            case "rescueagent":
                scene = new Scene(
                    new RescueDashboardView(new RescueController(sharedSimulation)),
                    1000, 650
                );
                scene.setFill(Color.web("#060a12"));
                mainStage.setTitle("Flood Simulation - Rescue Command");
                break;

            case "citizen":
            default:
                scene = new Scene(
                    new CitizenDashboardView(new CitizenController(sharedSimulation)),
                    1000, 650
                );
                scene.setFill(Color.web("#060a12"));
                mainStage.setTitle("Flood Simulation - Citizen Portal");
                break;
        }
        mainStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
