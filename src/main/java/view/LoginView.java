package view;

import controller.LoginController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class LoginView extends StackPane {

    private final Button backButton;
    private Hyperlink registerLink;
    private Hyperlink forgotPasswordLink;

    private TextField emailField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private Button loginButton;
    private Label errorLabel;
    
    private LoginController controller; 

    public LoginView() {
        this.setPrefSize(1100, 650);

        Region backgroundFiller = new Region();
        backgroundFiller.prefWidthProperty().bind(this.widthProperty());
        backgroundFiller.prefHeightProperty().bind(this.heightProperty());

        try {
            String imagePath = "/images/Gemini_Generated_Image_qyih9tqyih9tqyih.png";
            var resource = getClass().getResource(imagePath);
            if (resource != null) {
                String imageUrl = resource.toExternalForm();
                backgroundFiller.setStyle("-fx-background-image: url('" + imageUrl + "'); -fx-background-repeat: no-repeat; -fx-background-size: cover; -fx-background-position: center;");
            } else {
                backgroundFiller.setStyle("-fx-background-color: #0d1e3d;");
            }
        } catch (Exception e) {
            backgroundFiller.setStyle("-fx-background-color: #0d1e3d;");
        }

        Region gradientOverlay = new Region();
        gradientOverlay.prefWidthProperty().bind(this.widthProperty());
        gradientOverlay.prefHeightProperty().bind(this.heightProperty());
        gradientOverlay.setStyle("-fx-background-color: linear-gradient(to right, #0b1a30 0%, #0b1a30 35%, rgba(11, 26, 48, 0.8) 55%, rgba(11, 26, 48, 0.3) 75%, transparent 100%);");

        VBox leftColumn = new VBox(25);
        leftColumn.setAlignment(Pos.CENTER_LEFT);
        leftColumn.setPadding(new Insets(40, 80, 40, 80));
        leftColumn.setMaxWidth(520);

        backButton = new Button("←");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-font-size: 28px; -fx-padding: 0 0 10 0; -fx-cursor: hand;");
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 28px; -fx-padding: 0 0 10 0; -fx-cursor: hand;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-font-size: 28px; -fx-padding: 0 0 10 0;"));

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
        Text subtitleText = new Text("Simulation & emergency management");
        subtitleText.setFont(Font.font("System", FontWeight.LIGHT, 12));
        subtitleText.setFill(Color.web("#a0b2ce"));
        brandTitles.getChildren().addAll(titleText, subtitleText);
        brandHeader.getChildren().addAll(logoContainer, brandTitles);

        VBox formGroup = new VBox(15);
        Text formTitle = new Text("Log in");
        formTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 22));
        formTitle.setFill(Color.WHITE);
        emailField = new TextField();

        passwordField = new PasswordField();
        visiblePasswordField = new TextField();

        String fieldStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce; -fx-padding: 0;";
        passwordField.setStyle(fieldStyle);
        visiblePasswordField.setStyle(fieldStyle);
        passwordField.setPromptText("Mot de passe");
        visiblePasswordField.setPromptText("Mot de passe");

        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        Button eyeButton = new Button("👁");
        eyeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-cursor: hand; -fx-padding: 0 5 0 0;");

        StackPane passwordStack = new StackPane(passwordField, visiblePasswordField, eyeButton);
        StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);

        eyeButton.setOnAction(e -> {
            boolean isVisible = visiblePasswordField.isVisible();
            visiblePasswordField.setVisible(!isVisible);
            visiblePasswordField.setManaged(!isVisible);
            passwordField.setVisible(isVisible);
            passwordField.setManaged(isVisible);
            eyeButton.setText(isVisible ? "👁" : "🔒");
        });

        HBox emailBox = createStyledInputField("✉", "Email", emailField);
        HBox passwordBox = createStyledInputField("🔑", null, passwordStack);

        errorLabel = new Label("");
        errorLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
        errorLabel.setTextFill(Color.web("#e74c3c"));
        errorLabel.setWrapText(true);
        forgotPasswordLink = new Hyperlink("Mot de passe oublié ?");
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

        formGroup.getChildren().addAll(formTitle, errorLabel, emailBox, passwordBox, forgotAligner, loginButton);

        HBox footerGroup = new HBox(6);
        footerGroup.setAlignment(Pos.CENTER_LEFT);
        Text noAccountLabel = new Text("Pas encore de compte ?");
        noAccountLabel.setFill(Color.web("#a0b2ce"));
        registerLink = new Hyperlink("Créer un compte");
        registerLink.setStyle("-fx-underline: false; -fx-padding: 0;");
        footerGroup.getChildren().addAll(noAccountLabel, registerLink);

        leftColumn.getChildren().addAll(backButton, brandHeader, formGroup, footerGroup);
        StackPane.setAlignment(leftColumn, Pos.CENTER_LEFT);
        this.getChildren().addAll(backgroundFiller, gradientOverlay, leftColumn);
    }

    public Button getBackButton() { return backButton; }
    public Hyperlink getRegisterLink() { return registerLink; }
    public String getEmailInput() { return emailField.getText(); }
    public String getPasswordInput() { return passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText(); }
    public Button getLoginButton() { return loginButton; }
    public void displayErrorMessage(String message) { errorLabel.setText(message); }
    public Hyperlink getForgotPasswordLink() { return forgotPasswordLink; }
    
    public void setController(LoginController controller) { this.controller = controller; }

    private HBox createStyledInputField(String iconUnicode, String prompt, Node inputField) {
        HBox fieldContainer = new HBox(12);
        fieldContainer.setAlignment(Pos.CENTER_LEFT);
        fieldContainer.setPadding(new Insets(0, 15, 0, 15));
        fieldContainer.setPrefHeight(45);
        fieldContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label iconLabel = new Label(iconUnicode);
        iconLabel.setStyle("-fx-text-fill: #a0b2ce; -fx-font-size: 16px;");

        if (inputField instanceof TextField && !(inputField instanceof PasswordField)) {
            ((TextField) inputField).setPromptText(prompt);
            ((TextField) inputField).setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce; -fx-padding: 0;");
        }
        HBox.setHgrow(inputField, Priority.ALWAYS);
        fieldContainer.getChildren().addAll(iconLabel, inputField);
        return fieldContainer;
    }
}