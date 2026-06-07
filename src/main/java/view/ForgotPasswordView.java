package view;

import controller.AuthPage.ForgotPasswordController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ForgotPasswordView extends StackPane {

    private final Button backButton = new Button("←");
    private final TextField codeField = new TextField();
    
    // Champs de mot de passe
    private final PasswordField passwordField = new PasswordField();
    private final TextField visiblePasswordField = new TextField();
    
    private final Button confirmButton = new Button("Réinitialiser");
    private final Label errorLabel = new Label();
    private ForgotPasswordController controller;

    public ForgotPasswordView() {
        this.setPrefSize(1100, 650);

        // 1. Fond et Overlay (Style cohérent avec LoginView)
        Region backgroundFiller = new Region();
        backgroundFiller.prefWidthProperty().bind(this.widthProperty());
        backgroundFiller.prefHeightProperty().bind(this.heightProperty());
        backgroundFiller.setStyle("-fx-background-color: #0d1e3d;");

        Region gradientOverlay = new Region();
        gradientOverlay.prefWidthProperty().bind(this.widthProperty());
        gradientOverlay.prefHeightProperty().bind(this.heightProperty());
        gradientOverlay.setStyle("-fx-background-color: linear-gradient(to right, #0b1a30 0%, #0b1a30 35%, rgba(11, 26, 48, 0.8) 55%, rgba(11, 26, 48, 0.3) 75%, transparent 100%);");

        // 2. Colonne de gauche
        VBox leftColumn = new VBox(25);
        leftColumn.setAlignment(Pos.CENTER_LEFT);
        leftColumn.setPadding(new Insets(40, 80, 40, 80));
        leftColumn.setMaxWidth(520);

        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-font-size: 28px; -fx-cursor: hand;");

        Text formTitle = new Text("Réinitialisation");
        formTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 22));
        formTitle.setFill(Color.WHITE);

        // Bind bidirectionnel pour le mot de passe
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        HBox codeBox = createStyledInputField("🔢", codeField, "Code reçu par email");
        StackPane passStack = createPasswordFieldWithEye(passwordField, visiblePasswordField, "Nouveau mot de passe");
        
        // Wrapper pour le champ de mot de passe dans un HBox pour le style
        HBox passBox = createStyledInputField("🔑", passStack, null);

        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setPrefHeight(45);
        confirmButton.setStyle("-fx-background-color: #0b5cbf; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        errorLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
        errorLabel.setWrapText(true);

        leftColumn.getChildren().addAll(backButton, formTitle, codeBox, passBox, confirmButton, errorLabel);
        
        this.getChildren().addAll(backgroundFiller, gradientOverlay, leftColumn);
    }

    private HBox createStyledInputField(String icon, javafx.scene.Node input, String prompt) {
        HBox container = new HBox(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(0, 15, 0, 15));
        container.setPrefHeight(45);
        container.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-width: 1; -fx-border-radius: 6;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: #a0b2ce;");
        
        if (input instanceof TextField && !(input instanceof PasswordField)) {
            ((TextField) input).setPromptText(prompt);
            ((TextField) input).setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;");
        }
        HBox.setHgrow(input, Priority.ALWAYS);
        container.getChildren().addAll(iconLabel, input);
        return container;
    }

    private StackPane createPasswordFieldWithEye(PasswordField pf, TextField tf, String prompt) {
        pf.setPromptText(prompt); tf.setPromptText(prompt);
        pf.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;");
        tf.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;");
        
        tf.setVisible(false); tf.setManaged(false);
        pf.setVisible(true); pf.setManaged(true);
        
        Button btn = new Button("👁");
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            boolean v = tf.isVisible();
            tf.setVisible(!v); tf.setManaged(!v);
            pf.setVisible(v); pf.setManaged(v);
            btn.setText(v ? "👁" : "🔒");
        });
        
        StackPane s = new StackPane(pf, tf, btn);
        StackPane.setAlignment(btn, Pos.CENTER_RIGHT);
        return s;
    }

    public void displayErrorMessage(String message) {
        errorLabel.setText(message);
        errorLabel.setTextFill(Color.web("#e74c3c"));
    }

    public void displaySuccessMessage(String message) {
        errorLabel.setText(message);
        errorLabel.setTextFill(Color.web("#2ecc71"));
    }

    public Button getBackButton() { return backButton; }
    public Button getConfirmButton() { return confirmButton; }
    public String getCodeInput() { return codeField.getText(); }
    public String getNewPasswordInput() { return passwordField.getText(); }
    public void setController(ForgotPasswordController controller) { this.controller = controller; }
}