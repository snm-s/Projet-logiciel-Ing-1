package app;

import controller.ForgotPasswordController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import view.WelcomeView;
import view.ForgotPasswordView;
import view.LoginView;
import view.RegisterView;

public class Main extends Application {

    private static Stage mainStage;

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

        // CRITIQUE : On instancie le contrôleur pour lier le bouton retour !
        new controller.LoginController(loginView, mainStage);

        Scene scene = new Scene(loginView, 1000, 650);
        mainStage.setScene(scene);
    }

    public static void showRegisterView() {
        RegisterView registerView = new RegisterView();

        // CRITIQUE : On instancie le contrôleur d'inscription en lui passant la vue et le stage
        new controller.RegisterController(registerView, mainStage);

        Scene scene = new Scene(registerView, 1000, 650);
        mainStage.setScene(scene);
    }


    public static void showForgotPasswordView(String email) {
            // 1. Instanciation de votre vue
            ForgotPasswordView view = new ForgotPasswordView();
            
            // 2. Instanciation du contrôleur (en utilisant mainStage comme déclaré dans votre classe)
            new ForgotPasswordController(view, mainStage, email);
            
            // 3. Création de la scène et affichage
            Scene scene = new Scene(view.getRoot(), 1000, 650);
            mainStage.setScene(scene);
    }


    public static void main(String[] args) {
        launch(args);
    }
}