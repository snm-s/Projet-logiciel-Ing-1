package view;

import controller.AuthPage.LoginController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class LoginView extends StackPane {

    private static final String PANEL = "#0d111b";
    private static final String TEXT = "#f4f7fb";
    private static final String MUTED = "#8493ad";
    private static final String BLUE = "#3b6cff";
    private static final String BLUE_HOVER = "#4d7dff";
    private static final String BLUE_DARK = "#244fd6";
    private static final String BORDER = "rgba(255,255,255,0.10)";
    private static final String FIELD = "rgba(255,255,255,0.045)";
    private static final String FIELD_BORDER = "rgba(255,255,255,0.13)";

    private Button backButton;
    private Hyperlink registerLink;
    private Hyperlink forgotPasswordLink;

    private TextField emailField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private Button loginButton;
    private Label errorLabel;

    private Circle glow1;
    private Circle glow2;
    private Circle glow3;
    private Circle glow4;
    private Circle glow5;
    private Circle glow6;

    private LoginController controller;

    /**
     * Create the login view for user authentication.
     */
    public LoginView() {
        this.setMinSize(0, 0);
        this.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setStyle("-fx-background-color:#060a12;");

        buildUI();

        loginButton.setDefaultButton(true);
        emailField.setOnAction(e -> loginButton.fire());
        passwordField.setOnAction(e -> loginButton.fire());
        visiblePasswordField.setOnAction(e -> loginButton.fire());
    }

    /**
     * Builds ui.
     */
    private void buildUI() {
        Region background = new Region();
        background.prefWidthProperty().bind(this.widthProperty());
        background.prefHeightProperty().bind(this.heightProperty());
        background.setMinSize(0, 0);
        background.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        background.setStyle(
                "-fx-background-color:" +
                        "radial-gradient(center 18% 20%, radius 60%, rgba(59,108,255,0.22), transparent 58%)," +
                        "radial-gradient(center 85% 78%, radius 70%, rgba(59,108,255,0.14), transparent 60%)," +
                        "linear-gradient(to bottom right, #060a12, #0a101b, #05070d);"
        );

        Pane grid = buildGrid();
        grid.prefWidthProperty().bind(this.widthProperty());
        grid.prefHeightProperty().bind(this.heightProperty());
        grid.setMinSize(0, 0);
        grid.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        startBackgroundAnimation();

        HBox shell = new HBox();
        shell.setMaxWidth(900);
        shell.setMaxHeight(520);
        shell.setStyle(
                "-fx-background-color:" + PANEL + ";" +
                        "-fx-background-radius:28;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:28;" +
                        "-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.55), 42, 0, 0, 18);"
        );

        VBox left = buildLeftPanel();
        VBox right = buildFormPanel();

        shell.getChildren().addAll(left, right);
        getChildren().addAll(background, grid, shell);
    }

    /**
     * Builds grid.
     * @return the Pane.
     */
    private Pane buildGrid() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);

        for (int x = 0; x < 2400; x += 54) {
            Line line = new Line(x, 0, x, 1800);
            line.setStroke(Color.web("rgba(255,255,255,0.045)"));
            line.setStrokeWidth(1);
            pane.getChildren().add(line);
        }

        for (int y = 0; y < 1800; y += 54) {
            Line line = new Line(0, y, 2400, y);
            line.setStroke(Color.web("rgba(255,255,255,0.040)"));
            line.setStrokeWidth(1);
            pane.getChildren().add(line);
        }

        glow1 = new Circle(230, 155, 185);
        glow1.setFill(Color.web("rgba(59,108,255,0.110)"));

        glow2 = new Circle(700, 120, 125);
        glow2.setFill(Color.web("rgba(59,108,255,0.055)"));

        glow3 = new Circle(1030, 610, 215);
        glow3.setFill(Color.web("rgba(59,108,255,0.075)"));

        glow4 = new Circle(170, 700, 105);
        glow4.setFill(Color.web("rgba(59,108,255,0.045)"));

        glow5 = new Circle(1210, 250, 90);
        glow5.setFill(Color.web("rgba(59,108,255,0.040)"));

        glow6 = new Circle(1260, 860, 135);
        glow6.setFill(Color.web("rgba(59,108,255,0.050)"));

        pane.getChildren().addAll(glow1, glow2, glow3, glow4, glow5, glow6);
        return pane;
    }

    /**
     * Starts background animation.
     */
    private void startBackgroundAnimation() {
        if (glow1 == null || glow2 == null || glow3 == null || glow4 == null || glow5 == null || glow6 == null) {
            return;
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow1.translateXProperty(), 0),
                        new KeyValue(glow1.translateYProperty(), 0),
                        new KeyValue(glow2.translateXProperty(), 0),
                        new KeyValue(glow2.translateYProperty(), 0),
                        new KeyValue(glow3.translateXProperty(), 0),
                        new KeyValue(glow3.translateYProperty(), 0),
                        new KeyValue(glow4.translateXProperty(), 0),
                        new KeyValue(glow4.translateYProperty(), 0),
                        new KeyValue(glow5.translateXProperty(), 0),
                        new KeyValue(glow5.translateYProperty(), 0),
                        new KeyValue(glow6.translateXProperty(), 0),
                        new KeyValue(glow6.translateYProperty(), 0),

                        new KeyValue(glow1.opacityProperty(), 0.75),
                        new KeyValue(glow2.opacityProperty(), 0.55),
                        new KeyValue(glow3.opacityProperty(), 0.68),
                        new KeyValue(glow4.opacityProperty(), 0.42),
                        new KeyValue(glow5.opacityProperty(), 0.38),
                        new KeyValue(glow6.opacityProperty(), 0.45)
                ),
                new KeyFrame(Duration.seconds(6),
                        new KeyValue(glow1.translateXProperty(), 55),
                        new KeyValue(glow1.translateYProperty(), 28),
                        new KeyValue(glow2.translateXProperty(), -40),
                        new KeyValue(glow2.translateYProperty(), 18),
                        new KeyValue(glow3.translateXProperty(), -62),
                        new KeyValue(glow3.translateYProperty(), -36),
                        new KeyValue(glow4.translateXProperty(), 22),
                        new KeyValue(glow4.translateYProperty(), -30),
                        new KeyValue(glow5.translateXProperty(), -25),
                        new KeyValue(glow5.translateYProperty(), 35),
                        new KeyValue(glow6.translateXProperty(), 30),
                        new KeyValue(glow6.translateYProperty(), -20),

                        new KeyValue(glow1.opacityProperty(), 0.90),
                        new KeyValue(glow2.opacityProperty(), 0.72),
                        new KeyValue(glow3.opacityProperty(), 0.83),
                        new KeyValue(glow4.opacityProperty(), 0.58),
                        new KeyValue(glow5.opacityProperty(), 0.50),
                        new KeyValue(glow6.opacityProperty(), 0.62)
                ),
                new KeyFrame(Duration.seconds(12),
                        new KeyValue(glow1.translateXProperty(), -20),
                        new KeyValue(glow1.translateYProperty(), 15),
                        new KeyValue(glow2.translateXProperty(), 35),
                        new KeyValue(glow2.translateYProperty(), -15),
                        new KeyValue(glow3.translateXProperty(), 18),
                        new KeyValue(glow3.translateYProperty(), 20),
                        new KeyValue(glow4.translateXProperty(), -18),
                        new KeyValue(glow4.translateYProperty(), 24),
                        new KeyValue(glow5.translateXProperty(), 20),
                        new KeyValue(glow5.translateYProperty(), -28),
                        new KeyValue(glow6.translateXProperty(), -22),
                        new KeyValue(glow6.translateYProperty(), 26),

                        new KeyValue(glow1.opacityProperty(), 0.78),
                        new KeyValue(glow2.opacityProperty(), 0.56),
                        new KeyValue(glow3.opacityProperty(), 0.70),
                        new KeyValue(glow4.opacityProperty(), 0.47),
                        new KeyValue(glow5.opacityProperty(), 0.40),
                        new KeyValue(glow6.opacityProperty(), 0.48)
                ),
                new KeyFrame(Duration.seconds(18),
                        new KeyValue(glow1.translateXProperty(), 0),
                        new KeyValue(glow1.translateYProperty(), 0),
                        new KeyValue(glow2.translateXProperty(), 0),
                        new KeyValue(glow2.translateYProperty(), 0),
                        new KeyValue(glow3.translateXProperty(), 0),
                        new KeyValue(glow3.translateYProperty(), 0),
                        new KeyValue(glow4.translateXProperty(), 0),
                        new KeyValue(glow4.translateYProperty(), 0),
                        new KeyValue(glow5.translateXProperty(), 0),
                        new KeyValue(glow5.translateYProperty(), 0),
                        new KeyValue(glow6.translateXProperty(), 0),
                        new KeyValue(glow6.translateYProperty(), 0),

                        new KeyValue(glow1.opacityProperty(), 0.75),
                        new KeyValue(glow2.opacityProperty(), 0.55),
                        new KeyValue(glow3.opacityProperty(), 0.68),
                        new KeyValue(glow4.opacityProperty(), 0.42),
                        new KeyValue(glow5.opacityProperty(), 0.38),
                        new KeyValue(glow6.opacityProperty(), 0.45)
                )
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /**
     * Builds left panel.
     * @return the VBox.
     */
    private VBox buildLeftPanel() {
        VBox left = new VBox();
        left.setPrefWidth(345);
        left.setPadding(new Insets(38, 42, 38, 42));
        left.setAlignment(Pos.TOP_LEFT);
        left.setStyle(
                "-fx-background-color:" +
                        "linear-gradient(to bottom right, #08101d, #050912);" +
                        "-fx-background-radius:28 0 0 28;" +
                        "-fx-border-color:transparent rgba(255,255,255,0.07) transparent transparent;" +
                        "-fx-border-width:0 1 0 0;"
        );

        backButton = new Button("← Retour");
        backButton.setStyle(backStyle(false));
        backButton.setOnMouseEntered(e -> backButton.setStyle(backStyle(true)));
        backButton.setOnMouseExited(e -> backButton.setStyle(backStyle(false)));

        Region topSpace = new Region();
        topSpace.setPrefHeight(70);

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);

        StackPane logo = createLogoBox(42);

        VBox brandText = new VBox(1);
        Label brandTitle = label("Inondation", TEXT, 22, true);
        Label brandSub = label("Simulation & Gestion", "#4f6384", 12, false);
        brandText.getChildren().addAll(brandTitle, brandSub);

        brand.getChildren().addAll(logo, brandText);

        Region middle = new Region();
        middle.setPrefHeight(54);

        Label overline = label("CONNEXION", BLUE, 13, true);
        overline.setStyle("-fx-letter-spacing:3px;");

        Label title = label("Se connecter", TEXT, 34, true);

        Region accent = new Region();
        accent.setPrefWidth(190);
        accent.setMaxWidth(190);
        accent.setPrefHeight(3);
        accent.setStyle(
                "-fx-background-color:linear-gradient(to right, " + BLUE + ", rgba(59,108,255,0.15));" +
                        "-fx-background-radius:99;"
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label bottom = label("Projet ING1", "#2f3b50", 12, false);

        left.getChildren().addAll(backButton, topSpace, brand, middle, overline, title, accent, spacer, bottom);
        return left;
    }

    /**
     * Builds form panel.
     * @return the VBox.
     */
    private VBox buildFormPanel() {
        VBox form = new VBox(18);
        form.setPrefWidth(555);
        form.setPadding(new Insets(62, 70, 58, 70));
        form.setAlignment(Pos.CENTER_LEFT);
        form.setStyle(
                "-fx-background-color:" +
                        "linear-gradient(to bottom right, #101725, #0d111b);" +
                        "-fx-background-radius:0 28 28 0;"
        );

        Label small = label("ACCÈS SÉCURISÉ", BLUE, 12, true);
        small.setStyle("-fx-letter-spacing:3px;");

        Label title = label("Se connecter", TEXT, 38, true);

        Label subtitle = label("Entrez vos identifiants pour accéder à votre espace.", MUTED, 14, false);
        subtitle.setWrapText(true);

        Region gap = new Region();
        gap.setPrefHeight(8);

        emailField = new TextField();
        emailField.setPromptText("Email");

        passwordField = new PasswordField();
        visiblePasswordField = new TextField();

        passwordField.setPromptText("Mot de passe");
        visiblePasswordField.setPromptText("Mot de passe");
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        Button eyeButton = createEyeButton();

        StackPane passwordStack = new StackPane(passwordField, visiblePasswordField, eyeButton);
        StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);
        StackPane.setMargin(eyeButton, new Insets(0, 10, 0, 0));

        eyeButton.setOnAction(e -> {
            boolean isVisible = visiblePasswordField.isVisible();

            visiblePasswordField.setVisible(!isVisible);
            visiblePasswordField.setManaged(!isVisible);

            passwordField.setVisible(isVisible);
            passwordField.setManaged(isVisible);

            updateEyeIcon(eyeButton, !isVisible);

            if (!isVisible) {
                visiblePasswordField.requestFocus();
                visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
            } else {
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });

        HBox emailBox = createStyledInputField("mail", emailField);
        HBox passwordBox = createStyledInputField("key", passwordStack);

        forgotPasswordLink = new Hyperlink("Mot de passe oublié ?");
        forgotPasswordLink.setTextFill(Color.web(BLUE_HOVER));
        forgotPasswordLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        forgotPasswordLink.setStyle("-fx-underline:false; -fx-padding:0;");

        HBox forgotAligner = new HBox(forgotPasswordLink);
        forgotAligner.setAlignment(Pos.CENTER_RIGHT);

        loginButton = mainButton("Se connecter");

        errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#ff6b6b"));
        errorLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        errorLabel.setWrapText(true);

        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label noAccount = label("Pas encore de compte ?", MUTED, 13, false);

        registerLink = new Hyperlink("Créer un compte");
        registerLink.setTextFill(Color.web(BLUE_HOVER));
        registerLink.setFont(Font.font("System", FontWeight.BOLD, 13));
        registerLink.setStyle("-fx-underline:false; -fx-padding:0;");

        footer.getChildren().addAll(noAccount, registerLink);

        form.getChildren().addAll(
                small,
                title,
                subtitle,
                gap,
                emailBox,
                passwordBox,
                forgotAligner,
                loginButton,
                errorLabel,
                footer
        );

        return form;
    }

    /**
     * Creates styled input field.
     * @param iconType the iconType.
     * @param inputField the inputField.
     * @return the HBox.
     */
    private HBox createStyledInputField(String iconType, Node inputField) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 15, 0, 15));
        box.setPrefHeight(50);
        box.setStyle(fieldStyle(false));

        box.setOnMouseEntered(e -> box.setStyle(fieldStyle(true)));
        box.setOnMouseExited(e -> box.setStyle(fieldStyle(false)));

        Node icon = createInputIcon(iconType);

        if (inputField instanceof TextField textField) {
            styleInput(textField);
        }

        if (inputField instanceof StackPane stackPane) {
            for (Node node : stackPane.getChildren()) {
                if (node instanceof TextField textField) {
                    styleInput(textField);
                }
            }
        }

        HBox.setHgrow(inputField, Priority.ALWAYS);
        box.getChildren().addAll(icon, inputField);
        return box;
    }

    /**
     * Performs style.
     * @param hover the hover.
     * @return the String.
     */
    private String fieldStyle(boolean hover) {
        return "-fx-background-color:" + (hover ? "rgba(255,255,255,0.065)" : FIELD) + ";" +
                "-fx-border-color:" + (hover ? "rgba(59,108,255,0.55)" : FIELD_BORDER) + ";" +
                "-fx-border-width:1.2;" +
                "-fx-border-radius:14;" +
                "-fx-background-radius:14;";
    }

    /**
     * Performs input.
     * @param field the field.
     */
    private void styleInput(TextField field) {
        field.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:" + TEXT + ";" +
                        "-fx-prompt-text-fill:#607292;" +
                        "-fx-padding:0;"
        );
        field.setFont(Font.font("System", FontWeight.NORMAL, 14));
    }

    /**
     * Creates input icon.
     * @param type the type.
     * @return the Node.
     */
    private Node createInputIcon(String type) {
        SVGPath icon = new SVGPath();

        if ("mail".equals(type)) {
            icon.setContent("M2 5 H22 V17 H2 Z M2 5 L12 12 L22 5");
        } else {
            icon.setContent("M8 14 A4 4 0 1 1 8 6 A4 4 0 1 1 8 14 M12 10 H22 M18 10 V13 M15 10 V12");
        }

        icon.setStroke(Color.web("#335fae"));
        icon.setStrokeWidth(1.8);
        icon.setFill(Color.TRANSPARENT);

        StackPane iconBox = new StackPane(icon);
        iconBox.setPrefSize(22, 22);
        return iconBox;
    }

    /**
     * Creates eye button.
     * @return the Button.
     */
    private Button createEyeButton() {
        Button eyeButton = new Button();
        eyeButton.setPrefSize(34, 34);
        eyeButton.setMinSize(34, 34);
        eyeButton.setMaxSize(34, 34);
        eyeButton.setStyle("-fx-background-color:transparent; -fx-cursor:hand;");
        updateEyeIcon(eyeButton, false);
        return eyeButton;
    }

    /**
     * Updates eye icon.
     * @param eyeButton the eyeButton.
     * @param crossed the crossed.
     */
    private void updateEyeIcon(Button eyeButton, boolean crossed) {
        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent(
                "M2 10 Q10 2 18 10 Q10 18 2 10 " +
                        "M10 6 A4 4 0 1 1 10 14 A4 4 0 1 1 10 6"
        );
        eyeIcon.setStroke(Color.web("#456db8"));
        eyeIcon.setStrokeWidth(1.7);
        eyeIcon.setFill(Color.TRANSPARENT);

        SVGPath slashIcon = new SVGPath();
        slashIcon.setContent("M3 17 L17 3");
        slashIcon.setStroke(Color.web("#456db8"));
        slashIcon.setStrokeWidth(2.1);
        slashIcon.setFill(Color.TRANSPARENT);
        slashIcon.setVisible(crossed);

        StackPane iconPane = new StackPane(eyeIcon, slashIcon);
        iconPane.setPrefSize(22, 22);

        eyeButton.setGraphic(iconPane);
    }

    /**
     * Performs button.
     * @param text the text.
     * @return the Button.
     */
    private Button mainButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(52);
        button.setTextFill(Color.WHITE);
        button.setFont(Font.font("System", FontWeight.BOLD, 15));
        button.setStyle(mainButtonStyle(false));

        button.setOnMouseEntered(e -> button.setStyle(mainButtonStyle(true)));
        button.setOnMouseExited(e -> button.setStyle(mainButtonStyle(false)));

        return button;
    }

    /**
     * Performs button style.
     * @param hover the hover.
     * @return the String.
     */
    private String mainButtonStyle(boolean hover) {
        return "-fx-background-color:" +
                (hover
                        ? "linear-gradient(to right, #4778ff, #3157d7)"
                        : "linear-gradient(to right, " + BLUE_DARK + ", " + BLUE + ")") + ";" +
                "-fx-background-radius:14;" +
                "-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian, rgba(59,108,255,0.30), 18, 0, 0, 7);";
    }

    /**
     * Performs style.
     * @param hover the hover.
     * @return the String.
     */
    private String backStyle(boolean hover) {
        return "-fx-background-color:transparent;" +
                "-fx-text-fill:" + (hover ? TEXT : "#50617d") + ";" +
                "-fx-font-size:14;" +
                "-fx-cursor:hand;" +
                "-fx-padding:0 0 8 0;";
    }

    /**
     * Creates logo box.
     * @param size the size.
     * @return the StackPane.
     */
    private StackPane createLogoBox(int size) {
        StackPane logo = new StackPane();
        logo.setPrefSize(size, size);
        logo.setMinSize(size, size);
        logo.setMaxSize(size, size);
        logo.setStyle(
                "-fx-background-color:linear-gradient(to bottom right, rgba(59,108,255,0.95), rgba(35,79,214,0.90));" +
                        "-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian, rgba(59,108,255,0.35), 18, 0, 0, 5);"
        );

        SVGPath icon = new SVGPath();
        icon.setContent(
                "M10 21 L21 11 L32 21 " +
                        "M14 20 V32 H28 V20 " +
                        "M18 32 V25 H24 V32"
        );
        icon.setStroke(Color.WHITE);
        icon.setStrokeWidth(2.2);
        icon.setFill(Color.TRANSPARENT);

        logo.getChildren().add(icon);
        return logo;
    }

    /**
     * Performs label.
     * @param text the text.
     * @param color the color.
     * @param size the size.
     * @param bold the bold.
     * @return the Label.
     */
    private Label label(String text, String color, int size, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return label;
    }

    /**
     * Returns the back button.
     * @return the Button.
     */
    public Button getBackButton() {
        return backButton;
    }

    /**
     * Returns the register link.
     * @return the Hyperlink.
     */
    public Hyperlink getRegisterLink() {
        return registerLink;
    }

    /**
     * Returns the email input.
     * @return the String.
     */
    public String getEmailInput() {
        return emailField.getText();
    }

    /**
     * Returns the password input.
     * @return the String.
     */
    public String getPasswordInput() {
        return passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();
    }

    /**
     * Returns the login button.
     * @return the Button.
     */
    public Button getLoginButton() {
        return loginButton;
    }

    /**
     * Performs error message.
     * @param message the message.
     */
    public void displayErrorMessage(String message) {
        errorLabel.setText(message);
    }

    /**
     * Returns the forgot password link.
     * @return the Hyperlink.
     */
    public Hyperlink getForgotPasswordLink() {
        return forgotPasswordLink;
    }

    /**
     * Sets the controller.
     * @param controller the controller.
     */
    public void setController(LoginController controller) {
        this.controller = controller;
    }
}
