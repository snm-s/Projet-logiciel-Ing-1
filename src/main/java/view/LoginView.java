package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class LoginView extends StackPane {

    private final Button backButton; 
    private Hyperlink registerLink;
    
    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label errorLabel;

    public LoginView() {
        this.setPrefSize(1100, 650);

        // ==========================================
        // 1. IMAGE D'ARRIÈRE-PLAN SÉCURISÉE
        // ==========================================
        Region backgroundFiller = new Region();
        // FORCE LA RÉGION À SUIVRE LA TAILLE DU STACKPANE
        backgroundFiller.prefWidthProperty().bind(this.widthProperty());
        backgroundFiller.prefHeightProperty().bind(this.heightProperty());

        try {
            String imagePath = "/images/P32695412D5775606G-4208711993.jpeg";
            var resource = getClass().getResource(imagePath);
            if (resource != null) {
                String imageUrl = resource.toExternalForm();
                backgroundFiller.setStyle(
                    "-fx-background-image: url('" + imageUrl + "');" +
                    "-fx-background-repeat: no-repeat;" +
                    "-fx-background-size: cover;" +
                    "-fx-background-position: center right;"
                );
            } else {
                backgroundFiller.setStyle("-fx-background-color: #0d1e3d;");
            }
        } catch (Exception e) {
            backgroundFiller.setStyle("-fx-background-color: #0d1e3d;");
        }

        // ==========================================
        // 2. CALQUE DE DÉGRADÉ (OVERLAY CORRIGÉ)
        // ==========================================
        Region gradientOverlay = new Region();
        // FORCE LE DÉGRADÉ À SUIVRE LA TAILLE DU STACKPANE
        gradientOverlay.prefWidthProperty().bind(this.widthProperty());
        gradientOverlay.prefHeightProperty().bind(this.heightProperty());
        
        // Le dégradé part d'un bleu nuit opaque à gauche (0% à 35%) pour masquer l'image sous le formulaire
        // Puis il devient transparent vers la droite (70% à 100%) pour révéler l'image
        gradientOverlay.setStyle(
            "-fx-background-color: linear-gradient(to right, " +
            "#0b1a30 0%, " +
            "#0b1a30 35%, " +
            "rgba(11, 26, 48, 0.8) 55%, " +
            "rgba(11, 26, 48, 0.3) 75%, " +
            "transparent 100%);"
        );

        // ==========================================
        // 3. CONTENU ET FORMULAIRE (À GAUCHE)
        // ==========================================
        VBox leftColumn = new VBox(25); 
        leftColumn.setAlignment(Pos.CENTER_LEFT);
        leftColumn.setPadding(new Insets(40, 80, 40, 80));
        leftColumn.setMaxWidth(520);
        
        // --- BOUTON RETOUR ---
        backButton = new Button("←");
        backButton.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #a0b2ce;" +
            "-fx-font-size: 28px;" +
            "-fx-padding: 0 0 10 0;" +
            "-fx-cursor: hand;"
        );
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 28px; -fx-padding: 0 0 10 0; -fx-cursor: hand;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-font-size: 28px; -fx-padding: 0 0 10 0;"));

        // --- LOGO ET TITRE ---
        HBox brandHeader = new HBox(15);
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        SVGPath logoSvg = new SVGPath();
        logoSvg.setContent("M15 2 L28 12 H23 V22 H7 V12 H2 Z M2 25 Q8 23 15 25 T28 25 M2 28 Q8 26 15 28 T28 28");
        logoSvg.setStroke(Color.WHITE);
        logoSvg.setStrokeWidth(2.2);
        logoSvg.setFill(Color.TRANSPARENT);

        StackPane logoContainer = new StackPane(logoSvg);
        logoContainer.setPrefSize(35, 35);

        VBox brandTitles = new VBox(2);
        Text titleText = new Text("Inondation");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 30));
        titleText.setFill(Color.WHITE);

        Text subtitleText = new Text("Simulation & Gestion des secours");
        subtitleText.setFont(Font.font("System", FontWeight.LIGHT, 12));
        subtitleText.setFill(Color.web("#a0b2ce"));

        brandTitles.getChildren().addAll(titleText, subtitleText);
        brandHeader.getChildren().addAll(logoContainer, brandTitles);

        // --- SECTION FORMULAIRE ---
        VBox formGroup = new VBox(15);
        Text formTitle = new Text("Connexion");
        formTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 22));
        formTitle.setFill(Color.WHITE);

        emailField = new TextField();
        passwordField = new PasswordField();

        HBox emailBox = createStyledInputField("✉", "Email", emailField);
        HBox passwordBox = createStyledInputField("🔑", "Mot de passe", passwordField);

        errorLabel = new Label("");
        errorLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
        errorLabel.setTextFill(Color.web("#e74c3c"));
        errorLabel.setWrapText(true);

        Hyperlink forgotPasswordLink = new Hyperlink("Mot de passe oublié ?");
        forgotPasswordLink.setFont(Font.font("System", 12));
        forgotPasswordLink.setTextFill(Color.web("#a0b2ce"));
        forgotPasswordLink.setStyle("-fx-underline: false; -fx-padding: 0;");
        HBox forgotAligner = new HBox(forgotPasswordLink);
        forgotAligner.setAlignment(Pos.CENTER_RIGHT);

        loginButton = new Button("Se connecter");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(45);
        loginButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        loginButton.setTextFill(Color.WHITE);
        loginButton.setStyle("-fx-background-color: #0b5cbf; -fx-background-radius: 6; -fx-cursor: hand;");
        loginButton.setOnMouseEntered(e -> loginButton.setStyle("-fx-background-color: #0e73eb; -fx-background-radius: 6; -fx-cursor: hand;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle("-fx-background-color: #0b5cbf; -fx-background-radius: 6;"));

        formGroup.getChildren().addAll(formTitle, errorLabel, emailBox, passwordBox, forgotAligner, loginButton);

        // --- PIED DE PAGE ---
        HBox footerGroup = new HBox(6);
        footerGroup.setAlignment(Pos.CENTER_LEFT);
        Text noAccountLabel = new Text("Pas encore de compte ?");
        noAccountLabel.setFill(Color.web("#a0b2ce"));
        noAccountLabel.setFont(Font.font("System", 12));

        registerLink = new Hyperlink("Créer un compte");
        registerLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        registerLink.setTextFill(Color.web("#0e73eb"));
        registerLink.setStyle("-fx-underline: false; -fx-padding: 0;");
        footerGroup.getChildren().addAll(noAccountLabel, registerLink);

        // ==========================================
        // ASSEMBLAGE
        // ==========================================
        leftColumn.getChildren().addAll(backButton, brandHeader, formGroup, footerGroup);
        StackPane.setAlignment(leftColumn, Pos.CENTER_LEFT);

        this.getChildren().addAll(backgroundFiller, gradientOverlay, leftColumn);
    }

    // ==========================================
    // GETTERS POUR LE CONTROLEUR
    // ==========================================
    public Button getBackButton() { return backButton; }
    public Hyperlink getRegisterLink() { return registerLink; }
    public String getEmailInput() { return emailField.getText(); }
    public String getPasswordInput() { return passwordField.getText(); }
    public Button getLoginButton() { return loginButton; }
    public void displayErrorMessage(String message) { errorLabel.setText(message); }

    // ==========================================
    // MÉTHODE UTILITAIRE DE STYLE DE SAISIE
    // ==========================================
    private HBox createStyledInputField(String iconUnicode, String prompt, TextField inputField) {
        HBox fieldContainer = new HBox(12);
        fieldContainer.setAlignment(Pos.CENTER_LEFT);
        fieldContainer.setPadding(new Insets(0, 15, 0, 15));
        fieldContainer.setPrefHeight(45);
        fieldContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label iconLabel = new Label(iconUnicode);
        iconLabel.setStyle("-fx-text-fill: #a0b2ce; -fx-font-size: 16px;");

        inputField.setPromptText(prompt);
        inputField.setPrefHeight(40);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce; -fx-background-insets: 0; -fx-padding: 0;");

        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                fieldContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-border-color: #0b5cbf; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;");
                iconLabel.setStyle("-fx-text-fill: #0b5cbf; -fx-font-size: 16px;");
            } else {
                fieldContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
                iconLabel.setStyle("-fx-text-fill: #a0b2ce; -fx-font-size: 16px;");
            }
        });

        fieldContainer.getChildren().addAll(iconLabel, inputField);
        return fieldContainer;
    }
}