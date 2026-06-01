package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import view.WelcomeView;
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

        Scene scene = new Scene(loginView, 1000, 650);

        mainStage.setScene(scene);
    }

    public static void showRegisterView() {
        RegisterView registerView = new RegisterView();
        Scene scene = new Scene(registerView, 1000, 650);
        mainStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}