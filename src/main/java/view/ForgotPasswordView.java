package view;

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
    private final PasswordField newPasswordField = new PasswordField();
    private final Button confirmButton = new Button("Réinitialiser");

    public ForgotPasswordView() {
        this.setPrefSize(1100, 650);

        // 1. Fond identique à LoginView
        Region backgroundFiller = new Region();
        backgroundFiller.prefWidthProperty().bind(this.widthProperty());
        backgroundFiller.prefHeightProperty().bind(this.heightProperty());
        backgroundFiller.setStyle("-fx-background-color: #0d1e3d;"); // Fallback couleur

        Region gradientOverlay = new Region();
        gradientOverlay.prefWidthProperty().bind(this.widthProperty());
        gradientOverlay.prefHeightProperty().bind(this.heightProperty());
        gradientOverlay.setStyle("-fx-background-color: linear-gradient(to right, #0b1a30 0%, #0b1a30 35%, rgba(11, 26, 48, 0.8) 55%, rgba(11, 26, 48, 0.3) 75%, transparent 100%);");

        // 2. Colonne de gauche (le formulaire)
        VBox leftColumn = new VBox(25);
        leftColumn.setAlignment(Pos.CENTER_LEFT);
        leftColumn.setPadding(new Insets(40, 80, 40, 80));
        leftColumn.setMaxWidth(520);

        // Bouton retour
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-font-size: 28px; -fx-cursor: hand;");

        // Titre
        Text formTitle = new Text("Réinitialisation");
        formTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 22));
        formTitle.setFill(Color.WHITE);

        // Champs de saisie réutilisant la logique de style
        codeField.setPromptText("Code reçu par email");
        newPasswordField.setPromptText("Nouveau mot de passe");
        
        HBox codeBox = createStyledInputField("🔢", codeField);
        HBox passBox = createStyledInputField("🔑", newPasswordField);

        // Bouton confirmer
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setPrefHeight(45);
        confirmButton.setStyle("-fx-background-color: #0b5cbf; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        leftColumn.getChildren().addAll(backButton, formTitle, codeBox, passBox, confirmButton);
        
        this.getChildren().addAll(backgroundFiller, gradientOverlay, leftColumn);
    }

    private HBox createStyledInputField(String icon, Control input) {
        HBox container = new HBox(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(0, 15, 0, 15));
        container.setPrefHeight(45);
        container.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-width: 1; -fx-border-radius: 6;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: #a0b2ce;");
        
        input.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;");
        HBox.setHgrow(input, Priority.ALWAYS);
        
        container.getChildren().addAll(iconLabel, input);
        return container;
    }

    public Button getBackButton() { return backButton; }
    public Button getConfirmButton() { return confirmButton; }
    public String getCodeInput() { return codeField.getText(); }
    public String getNewPasswordInput() { return newPasswordField.getText(); }
}