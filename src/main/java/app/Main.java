package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.agent.Agent;
import view.AdminDashboardView;
import view.CitizenDashboardView;
import view.ForgotPasswordView;
import view.LoginView;
import view.RegisterView;
import view.RescueDashboardView;
import view.WelcomeView;
import view.SimulationView;
import controller.AdminPage.AdminController;
import controller.AdminPage.SimulationController;
import controller.AuthPage.ForgotPasswordController;
import controller.CitizenPage.CitizenController;
import controller.RescuePage.RescueController;

public class Main extends Application {

    private static Stage mainStage;
    // 🔥 Variable globale pour stocker l'agent connecté (Session)
    public static Agent currentUser;
    public static ListView<String> alertesListView;
    public static TextArea logArea;

    @Override
    public void start(Stage stage) {
        mainStage = stage;
        showWelcomeView();

        stage.setTitle("Flood Simulation - Agents & Graphs");
        stage.setMinWidth(850);
        stage.setMinHeight(550);
        stage.show();
    }

    public static void showWelcomeView() {
        WelcomeView welcomeView = new WelcomeView();
        Scene scene = new Scene(welcomeView, 1000, 650);
        mainStage.setScene(scene);
    }

    public static void showLoginView() {
        LoginView loginView = new LoginView();
        new controller.AuthPage.LoginController(loginView, mainStage);

        Scene scene = new Scene(loginView, 1000, 650);
        mainStage.setScene(scene);
    }

    public static void showRegisterView() {
        RegisterView registerView = new RegisterView();
        new controller.AuthPage.RegisterController(registerView, mainStage);

        Scene scene = new Scene(registerView, 1000, 650);
        mainStage.setScene(scene);
    }

    public static void showForgotPasswordView(String email) {
        ForgotPasswordView view = new ForgotPasswordView();
        new ForgotPasswordController(view, mainStage, email);

        Scene scene = new Scene(view, 1000, 650);
        mainStage.setScene(scene);
    }

    public static void showSimulationView() {
        // Initialisez le contrôleur et la vue
        SimulationController simulationController = new SimulationController();
        SimulationView simulationView = new SimulationView(simulationController);

        Scene scene = new Scene(simulationView, 1100, 700);
        mainStage.setTitle("Flood Simulation - Administration");
        mainStage.setScene(scene);
    }

    // 🔥 Redirection vers les vues en utilisant le constructeur vide d'origine
    public static void showDashboardView(String role) {
        if (role == null) {
            role = "citizen";
        }

        String cleanRole = role.trim().toLowerCase();
        Scene scene;

        switch (cleanRole) {
            case "admin":
                scene = new Scene(new AdminDashboardView(new AdminController()), 1000, 650);
                mainStage.setTitle("Flood Simulation - Admin Panel");
                break;

            case "rescue":
            case "rescueagent":
                
                 scene = new Scene(new RescueDashboardView(), 1000, 650);
                 mainStage.setTitle("Flood Simulation - Rescue Command");
                 break;
                 

            case "citizen":
            default:
                scene = new Scene(new CitizenDashboardView(new CitizenController(null)), 1000, 650);
                mainStage.setTitle("Flood Simulation - Citizen Portal");
                break;
        }

        mainStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}