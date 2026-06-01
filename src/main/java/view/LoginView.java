package view;

import app.Main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.*;

import javafx.scene.layout.*;

import javafx.scene.paint.Color;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView extends StackPane {

    public LoginView() {
        buildUI();
    }

    private void buildUI() {

        setStyle(
                "-fx-background-color: linear-gradient(to bottom, #F7FBFF, #DCEEFF);"
        );

        VBox card = new VBox(14);

        card.setAlignment(Pos.TOP_CENTER);

        card.setPadding(new Insets(22));

        card.setMaxWidth(350);

        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 20;" +
                "-fx-border-radius: 20;" +
                "-fx-border-color: #D7E6F5;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 20, 0, 0, 5);"
        );

        //-----------------------------------
        // BACK BUTTON
        //-----------------------------------

        Button backButton = new Button("←");

        backButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-font-size: 18px;" +
                "-fx-text-fill: #1565C0;" +
                "-fx-cursor: hand;"
        );

        backButton.setOnAction(e -> {
            Main.showWelcomeView();
        });

        HBox topBar = new HBox(backButton);

        topBar.setAlignment(Pos.CENTER_LEFT);

        topBar.setMaxWidth(Double.MAX_VALUE);

        //-----------------------------------
        // TITLE
        //-----------------------------------

        Label title = new Label("Welcome back !");

        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        title.setTextFill(Color.web("#0A3D91"));

        Label subtitle = new Label("Please enter your credentials");

        subtitle.setFont(Font.font("Arial", 12));

        subtitle.setTextFill(Color.web("#6B7A90"));

        //-----------------------------------
        // EMAIL
        //-----------------------------------

        TextField emailField = new TextField();

        emailField.setPromptText("👤  Email");

        emailField.setPrefWidth(260);

        emailField.setPrefHeight(40);

        emailField.setStyle(inputStyle());

        //-----------------------------------
        // PASSWORD
        //-----------------------------------

        PasswordField passwordField = new PasswordField();

        passwordField.setPromptText("🔒  Password");

        passwordField.setPrefWidth(260);

        passwordField.setPrefHeight(40);

        passwordField.setStyle(inputStyle());

        //-----------------------------------
        // FORGOT PASSWORD
        //-----------------------------------

        Hyperlink forgotPassword = new Hyperlink("Forgot password?");

        forgotPassword.setStyle(
                "-fx-text-fill: #1976D2;" +
                "-fx-font-size: 11px;"
        );

        HBox forgotBox = new HBox(forgotPassword);

        forgotBox.setAlignment(Pos.CENTER_RIGHT);

        forgotBox.setMaxWidth(260);

        //-----------------------------------
        // MESSAGE LABEL
        //-----------------------------------

        Label messageLabel = new Label();

        messageLabel.setFont(Font.font("Arial", 11));

        //-----------------------------------
        // LOGIN BUTTON
        //-----------------------------------

        Button loginButton = new Button("LOGIN");

        loginButton.setPrefWidth(260);

        loginButton.setPrefHeight(42);

        loginButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #1565C0, #1E88E5);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;"
        );

        loginButton.setOnAction(e -> {

            if (emailField.getText().isEmpty()
                    || passwordField.getText().isEmpty()) {

                messageLabel.setText("Please fill all fields.");

                messageLabel.setTextFill(Color.RED);

            } else {

                messageLabel.setText("Login successful!");

                messageLabel.setTextFill(Color.GREEN);
            }
        });

        //-----------------------------------
        // SIGN UP
        //-----------------------------------

        Label noAccount = new Label("Don't have an account?");

        noAccount.setTextFill(Color.web("#6B7A90"));

        noAccount.setFont(Font.font("Arial", 12));

        Hyperlink signUp = new Hyperlink("Sign up");

        signUp.setStyle(
                "-fx-text-fill: #1976D2;" +
                "-fx-font-weight: bold;"
        );

        HBox signUpBox = new HBox(4, noAccount, signUp);

        signUpBox.setAlignment(Pos.CENTER);

        //-----------------------------------
        // ADD ALL
        //-----------------------------------

        card.getChildren().addAll(
                topBar,
                title,
                subtitle,
                emailField,
                passwordField,
                forgotBox,
                messageLabel,
                loginButton,
                signUpBox
        );

        getChildren().add(card);
    }

    //-----------------------------------
    // INPUT STYLE
    //-----------------------------------

    private String inputStyle() {

        return
                "-fx-background-color: white;" +
                "-fx-border-color: #D6E4F0;" +
                "-fx-border-width: 1.3;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 0 12 0 12;";
    }
}