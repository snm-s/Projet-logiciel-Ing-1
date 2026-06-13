package view;

import java.time.LocalDate;

import app.Main;
import controller.AuthPage.RegisterController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
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
import javafx.scene.text.Text;
import javafx.util.Duration;
import service.AddressService;

public class RegisterView extends StackPane {

    private static final String PANEL = "#0d111b";
    private static final String TEXT = "#f4f7fb";
    private static final String MUTED = "#8493ad";
    private static final String BLUE = "#3b6cff";
    private static final String BLUE_HOVER = "#4d7dff";
    private static final String BLUE_DARK = "#244fd6";
    private static final String BORDER = "rgba(255,255,255,0.10)";
    private static final String FIELD = "rgba(255,255,255,0.045)";
    private static final String FIELD_BORDER = "rgba(255,255,255,0.13)";
    private static final String ERROR = "#ff6b6b";
    private static final String SUCCESS = "#42d782";

    private ScrollPane scrollPane = new ScrollPane();
    private VBox root = new VBox(17);
    private RegisterController controller;
    private Button backButton;

    private TextField firstName = new TextField(), lastName = new TextField();
    private ComboBox<Integer> birthDay = new ComboBox<>();
    private ComboBox<Integer> birthMonth = new ComboBox<>();
    private ComboBox<Integer> birthYear = new ComboBox<>();
    private TextField email = new TextField(), phone = new TextField();
    private PasswordField password = new PasswordField(), confirmPassword = new PasswordField();
    private TextField visiblePassword = new TextField(), visibleConfirmPassword = new TextField();
    private ComboBox<String> address = new ComboBox<>();
    private boolean addressSelectionInProgress = false;
    private TextField country = new TextField();
    private TextField city = new TextField();
    private ComboBox<String> houseType = new ComboBox<>();
    private TextField floor = new TextField();
    private ComboBox<String> role = new ComboBox<>();
    private VBox citizenBox = new VBox(15);
    private TextField householdSize = new TextField();

    private double detectedLat = 48.8566;
    private double detectedLng = 2.3522;

    private CheckBox pets = new CheckBox("Je possède des animaux de compagnie");
    private CheckBox pmrCheckBox = new CheckBox("Je suis une personne à mobilité réduite (PMR)");
    private TextArea medicalNeeds = new TextArea();
    private TextField emergencyContact = new TextField();
    private Button registerBtn = new Button("Créer mon compte");

    private Label errFirstName = errLabel(), errLastName = errLabel(), errBirthDate = errLabel(),
            errEmail = errLabel(), errPhone = errLabel(),
            errPassword = errLabel(),
            errAddress = errLabel(), errCity = errLabel(), errCountry = errLabel(),
            errHouseType = errLabel(), errFloor = errLabel(),
            errHouseholdSize = errLabel(), errEmergencyContact = errLabel(),
            errRole = errLabel();

    private Circle glow1;
    private Circle glow2;
    private Circle glow3;
    private Circle glow4;
    private Circle glow5;
    private Circle glow6;

    private static final String TF_NORMAL =
            "-fx-background-color:" + FIELD + ";" +
                    "-fx-border-color:" + FIELD_BORDER + ";" +
                    "-fx-border-width:1.2;" +
                    "-fx-border-radius:14;" +
                    "-fx-background-radius:14;" +
                    "-fx-text-fill:" + TEXT + ";" +
                    "-fx-prompt-text-fill:#607292;" +
                    "-fx-padding:0 14 0 14;";

    private static final String TF_FOCUS =
            "-fx-background-color:rgba(255,255,255,0.065);" +
                    "-fx-border-color:rgba(59,108,255,0.55);" +
                    "-fx-border-width:1.2;" +
                    "-fx-border-radius:14;" +
                    "-fx-background-radius:14;" +
                    "-fx-text-fill:" + TEXT + ";" +
                    "-fx-prompt-text-fill:#607292;" +
                    "-fx-padding:0 14 0 14;";

    private static final String TF_ERROR =
            "-fx-background-color:rgba(255,107,107,0.10);" +
                    "-fx-border-color:" + ERROR + ";" +
                    "-fx-border-width:1.4;" +
                    "-fx-border-radius:14;" +
                    "-fx-background-radius:14;" +
                    "-fx-text-fill:" + TEXT + ";" +
                    "-fx-prompt-text-fill:#607292;" +
                    "-fx-padding:0 14 0 14;";

    /**
     * Create the user registration view.
     */
    public RegisterView() {
        this.setMinSize(0, 0);
        this.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setStyle("-fx-background-color:#060a12;");

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
        shell.setMaxWidth(980);
        shell.setMaxHeight(620);
        shell.setStyle(
                "-fx-background-color:" + PANEL + ";" +
                        "-fx-background-radius:28;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:28;" +
                        "-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.55), 42, 0, 0, 18);"
        );

        VBox leftPanel = buildLeftPanel();

        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-viewport-background-color:transparent;"
        );

        VBox rightPanel = new VBox(scrollPane);
        rightPanel.setPrefWidth(635);
        rightPanel.setPadding(new Insets(34, 46, 34, 46));
        rightPanel.setStyle(
                "-fx-background-color:linear-gradient(to bottom right, #101725, #0d111b);" +
                        "-fx-background-radius:0 28 28 0;"
        );

        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(0, 0, 8, 0));
        root.setMaxWidth(540);

        build();
        setupRoleVisibility();
        setupOnlyLettersFields();
        setupEnterNavigation();

        shell.getChildren().addAll(leftPanel, rightPanel);
        this.getChildren().addAll(background, grid, shell);
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
                "-fx-background-color:linear-gradient(to bottom right, #08101d, #050912);" +
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

        Label overline = label("INSCRIPTION", BLUE, 13, true);
        overline.setStyle("-fx-letter-spacing:3px;");

        Label title = label("Créer\nun compte", TEXT, 34, true);
        title.setLineSpacing(-4);

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
     * Builds.
     */
    private void build() {
        Label small = label("ACCÈS SÉCURISÉ", BLUE, 12, true);
        small.setStyle("-fx-letter-spacing:3px;");

        Text pageTitle = new Text("Créer un compte");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 34));
        pageTitle.setFill(Color.web(TEXT));

        Text pageSubtitle = new Text("Renseignez les informations demandées pour accéder à l’application.");
        pageSubtitle.setFont(Font.font("System", FontWeight.NORMAL, 13));
        pageSubtitle.setFill(Color.web(MUTED));

        VBox headerBox = new VBox(5, small, pageTitle, pageSubtitle);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        root.getChildren().add(headerBox);

        firstName.setPromptText("Prénom");
        lastName.setPromptText("Nom");
        applyTextFieldStyle(firstName);
        applyTextFieldStyle(lastName);
        setupBirthDateComboBoxes();

        setupLiveValidation(firstName, errFirstName, f -> f.getText().matches("[a-zA-ZÀ-ÿ\\s'-]+"));
        setupLiveValidation(lastName, errLastName, f -> f.getText().matches("[a-zA-ZÀ-ÿ\\s'-]+"));

        HBox birthBox = new HBox(10, birthDay, birthMonth, birthYear);
        birthBox.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(
                createSectionLabel("Identité"),
                fieldRow(firstName, errFirstName),
                fieldRow(lastName, errLastName),
                new VBox(3, birthBox, errBirthDate)
        );

        email.setPromptText("Email");
        phone.setPromptText("Numéro de téléphone");
        applyTextFieldStyle(email);
        applyTextFieldStyle(phone);

        setupLiveValidation(email, errEmail, f -> f.getText().contains("@") && !f.getText().trim().isEmpty());
        setupLiveValidation(phone, errPhone, f -> f.getText().matches("\\d{10}"));

        root.getChildren().addAll(
                createSectionLabel("Contact"),
                fieldRow(email, errEmail),
                fieldRow(phone, errPhone)
        );

        password.textProperty().bindBidirectional(visiblePassword.textProperty());
        confirmPassword.textProperty().bindBidirectional(visibleConfirmPassword.textProperty());

        root.getChildren().addAll(
                createSectionLabel("Mot de passe"),
                createPasswordFieldWithEye(password, visiblePassword, "Mot de passe"),
                createPasswordFieldWithEye(confirmPassword, visibleConfirmPassword, "Confirmer le mot de passe"),
                errPassword
        );

        address.setPromptText("Adresse à Lyon");
        address.setEditable(true);
        applyEditableComboBoxStyle(address);
        
        address.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (addressSelectionInProgress) return;
        
            if (newValue == null || newValue.trim().length() < 3) {
                address.getItems().clear();
                address.hide();
                return;
            }
        
            new Thread(() -> {
                var results = AddressService.searchLyonAddresses(newValue);
        
                Platform.runLater(() -> {
                    if (!address.getEditor().getText().equals(newValue)) return;
        
                    address.getItems().setAll(results);
        
                    if (!results.isEmpty() && address.isFocused()) {
                        address.show();
                    } else {
                        address.hide();
                    }
                });
            }).start();
        });

        address.valueProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) return;
        
            addressSelectionInProgress = true;
            address.getEditor().setText(selected);
            addressSelectionInProgress = false;
        
            address.hide();
            showError(errAddress, address, false, "");
        });

        city.setText("Lyon");
city.setEditable(false);
city.setPromptText("Ville");
applyTextFieldStyle(city);

country.setText("France");
country.setEditable(false);
country.setPromptText("Pays");
applyTextFieldStyle(country);

        country.setPromptText("Pays");
        applyTextFieldStyle(country);

        address.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            boolean hasError = newValue == null
                    || newValue.trim().isEmpty()
                    || !newValue.matches(".*\\d+.*")
                    || !newValue.matches(".*[a-zA-ZÀ-ÿ]+.*");
        
            showError(
                    errAddress,
                    address,
                    hasError,
                    "L'adresse doit contenir un numéro de rue et un nom de rue"
            );
        });

        setupLiveValidation(country, errCountry, f -> f.getText().equalsIgnoreCase("France"));

        root.getChildren().addAll(
                createSectionLabel("Addresse"),
                fieldRow(address, errAddress),
                fieldRow(city, errCity),
                fieldRow(country, errCountry)
        );

        role.getItems().addAll("Citoyen", "Sauveteur");
        role.setPromptText("Selectionnez votre rôle");
        applyComboBoxStyle(role);
        role.valueProperty().addListener((obs, o, n) ->
                showError(errRole, role, n == null || n.trim().isEmpty(), "Veuillez sélectionner un rôle")
        );

        root.getChildren().addAll(createSectionLabel("Rôle"), fieldRow(role, errRole));

        houseType.getItems().addAll("Appartement", "Maison");
        houseType.setPromptText("Selectionnez votre type de logement");
        applyComboBoxStyle(houseType);

        floor.setPromptText("Etage");
        applyTextFieldStyle(floor);

        householdSize.setPromptText("Nombre de membres du foyer");
        applyTextFieldStyle(householdSize);

        emergencyContact.setPromptText("Contact d'urgence");
        applyTextFieldStyle(emergencyContact);

        medicalNeeds.setPromptText("Besoins médicaux");
        medicalNeeds.setPrefHeight(86);
        medicalNeeds.setStyle(
                "-fx-control-inner-background:#101725;" +
                        "-fx-text-fill:" + TEXT + ";" +
                        "-fx-prompt-text-fill:#607292;" +
                        "-fx-background-color:" + FIELD + ";" +
                        "-fx-border-color:" + FIELD_BORDER + ";" +
                        "-fx-border-width:1.2;" +
                        "-fx-border-radius:14;" +
                        "-fx-background-radius:14;" +
                        "-fx-padding:8;"
        );

        styleCheckBox(pets);
        styleCheckBox(pmrCheckBox);

        setupLiveValidation(floor, errFloor, f -> !f.getText().trim().isEmpty());
        setupLiveValidation(householdSize, errHouseholdSize, f -> !f.getText().trim().isEmpty());
        setupLiveValidation(emergencyContact, errEmergencyContact, f -> !f.getText().trim().isEmpty());

        houseType.valueProperty().addListener((obs, o, n) ->
                showError(errHouseType, houseType, n == null || n.trim().isEmpty(), "Le type de logement est requis")
        );

        citizenBox.getChildren().addAll(
                createSectionLabel("Informations sur le citoyen"),
                fieldRow(houseType, errHouseType),
                fieldRow(floor, errFloor),
                fieldRow(householdSize, errHouseholdSize),
                pets,
                pmrCheckBox,
                medicalNeeds,
                createSectionLabel("Contact d'urgence"),
                fieldRow(emergencyContact, errEmergencyContact)
        );

        citizenBox.setVisible(false);
        citizenBox.setManaged(false);
        root.getChildren().add(citizenBox);

        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setPrefHeight(52);
        registerBtn.setFont(Font.font("System", FontWeight.BOLD, 15));
        registerBtn.setTextFill(Color.WHITE);
        registerBtn.setStyle(mainButtonStyle(false));
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(mainButtonStyle(true)));
        registerBtn.setOnMouseExited(e -> registerBtn.setStyle(mainButtonStyle(false)));
        registerBtn.setOnAction(e -> handleRegister());

        VBox.setMargin(registerBtn, new Insets(16, 0, 0, 0));
        root.getChildren().add(registerBtn);
    }

    /**
     * Creates section label.
     * @param text the text.
     * @return the Label.
     */
    private Label createSectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 14));
        l.setTextFill(Color.web(TEXT));
        l.setPadding(new Insets(10, 0, 0, 0));
        return l;
    }

    /**
     * Performs label.
     * @return the Label.
     */
    private Label errLabel() {
        Label l = new Label();
        l.setFont(Font.font("System", FontWeight.NORMAL, 11));
        l.setTextFill(Color.web(ERROR));
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    /**
     * Performs row.
     * @param field the field.
     * @param err the err.
     * @return the VBox.
     */
    private VBox fieldRow(Control field, Label err) {
        VBox box = new VBox(4, field, err);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /**
     * Performs text field style.
     * @param tf the tf.
     */
    private void applyTextFieldStyle(TextField tf) {
        tf.setPrefHeight(46);
        tf.setFont(Font.font("System", 13));
        tf.setStyle(TF_NORMAL);
        tf.focusedProperty().addListener((obs, o, n) -> {
            if (!tf.getStyle().contains(ERROR)) {
                tf.setStyle(n ? TF_FOCUS : TF_NORMAL);
            }
        });
    }

    /**
     * Performs text field style.
     * @param pf the pf.
     */
    private void applyTextFieldStyle(PasswordField pf) {
        pf.setPrefHeight(46);
        pf.setFont(Font.font("System", 13));
        pf.setStyle(TF_NORMAL);
        pf.focusedProperty().addListener((obs, o, n) -> {
            if (!pf.getStyle().contains(ERROR)) {
                pf.setStyle(n ? TF_FOCUS : TF_NORMAL);
            }
        });
    }

    /**
     * Performs combo box style.
     * @param combo the combo.
     */
    private void applyComboBoxStyle(ComboBox<String> combo) {
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setPrefHeight(46);
        combo.setStyle(comboStyle(false));

        combo.setButtonCell(new ListCell<>() {
            @Override
            /**
             * Updates item.
             * @param item the item.
             * @param empty the empty.
             */
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? combo.getPromptText() : item);
                setTextFill(Color.web(TEXT));
            }
        });

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            /**
             * Updates item.
             * @param item the item.
             * @param empty the empty.
             */
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color:#0d111b;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color:#0d111b; -fx-text-fill:" + TEXT + ";");
                }
            }
        });
    }

    /**
     * Performs editable combo box style.
     * @param combo the combo.
     */
    private void applyEditableComboBoxStyle(ComboBox<String> combo) {
        applyComboBoxStyle(combo);

        combo.getEditor().setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:" + TEXT + ";" +
                        "-fx-prompt-text-fill:#607292;" +
                        "-fx-padding:0 14 0 14;"
        );
    }

    /**
     * Performs style.
     * @param error the error.
     * @return the String.
     */
    private String comboStyle(boolean error) {
        return "-fx-background-color:" + (error ? "rgba(255,107,107,0.10)" : FIELD) + ";" +
                "-fx-border-color:" + (error ? ERROR : FIELD_BORDER) + ";" +
                "-fx-border-width:1.2;" +
                "-fx-border-radius:14;" +
                "-fx-background-radius:14;" +
                "-fx-text-fill:" + TEXT + ";";
    }

    /**
     * Performs password color style.
     * @param field the field.
     * @param text the text.
     * @param isValid the isValid.
     */
    private void applyPasswordColorStyle(PasswordField field, String text, boolean isValid) {
        if (text == null || text.isEmpty()) {
            field.setStyle(TF_NORMAL);
            return;
        }

        field.setStyle(isValid
                ? "-fx-background-color:rgba(66,215,130,0.10); -fx-border-color:" + SUCCESS + "; -fx-border-width:1.4; -fx-border-radius:14; -fx-background-radius:14; -fx-text-fill:" + TEXT + "; -fx-prompt-text-fill:#607292; -fx-padding:0 14 0 14;"
                : "-fx-background-color:rgba(255,107,107,0.10); -fx-border-color:" + ERROR + "; -fx-border-width:1.4; -fx-border-radius:14; -fx-background-radius:14; -fx-text-fill:" + TEXT + "; -fx-prompt-text-fill:#607292; -fx-padding:0 14 0 14;"
        );
    }

    /**
     * Creates password field with eye.
     * @param pf the pf.
     * @param tf the tf.
     * @param prompt the prompt.
     * @return the StackPane.
     */
    private StackPane createPasswordFieldWithEye(PasswordField pf, TextField tf, String prompt) {
        pf.setPromptText(prompt);
        tf.setPromptText(prompt);

        pf.setPrefHeight(46);
        tf.setPrefHeight(46);

        applyTextFieldStyle(pf);
        applyTextFieldStyle(tf);

        tf.setVisible(false);
        tf.setManaged(false);

        Button eyeButton = new Button();
        eyeButton.setPrefSize(38, 38);
        eyeButton.setMinSize(38, 38);
        eyeButton.setMaxSize(38, 38);
        eyeButton.setStyle("-fx-background-color:transparent; -fx-cursor:hand;");

        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent(
                "M2 10 Q10 2 18 10 Q10 18 2 10 " +
                        "M10 6 A4 4 0 1 1 10 14 A4 4 0 1 1 10 6"
        );
        eyeIcon.setStroke(Color.web("#456db8"));
        eyeIcon.setStrokeWidth(1.8);
        eyeIcon.setFill(Color.TRANSPARENT);

        SVGPath slashIcon = new SVGPath();
        slashIcon.setContent("M3 17 L17 3");
        slashIcon.setStroke(Color.web("#456db8"));
        slashIcon.setStrokeWidth(2.2);
        slashIcon.setFill(Color.TRANSPARENT);
        slashIcon.setVisible(false);

        StackPane iconPane = new StackPane(eyeIcon, slashIcon);
        iconPane.setPrefSize(22, 22);

        eyeButton.setGraphic(iconPane);

        eyeButton.setOnAction(e -> {
            boolean isPasswordHidden = pf.isVisible();

            tf.setVisible(isPasswordHidden);
            tf.setManaged(isPasswordHidden);

            pf.setVisible(!isPasswordHidden);
            pf.setManaged(!isPasswordHidden);

            slashIcon.setVisible(isPasswordHidden);

            if (isPasswordHidden) {
                tf.requestFocus();
                tf.positionCaret(tf.getText().length());
            } else {
                pf.requestFocus();
                pf.positionCaret(pf.getText().length());
            }
        });

        StackPane s = new StackPane(pf, tf, eyeButton);
        StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);
        StackPane.setMargin(eyeButton, new Insets(0, 10, 0, 0));

        return s;
    }

    @FunctionalInterface
    interface FieldValidator {
        boolean isValid(TextField f);
    }

    /**
     * Performs live validation.
     * @param tf the tf.
     * @param err the err.
     * @param validator the validator.
     */
    private void setupLiveValidation(TextField tf, Label err, FieldValidator validator) {
        tf.textProperty().addListener((obs, o, n) -> {
            if (tf.getText().trim().isEmpty()) {
                showError(err, tf, true, getFieldError(tf));
            } else {
                showError(err, tf, !validator.isValid(tf), getFieldError(tf));
            }
        });
    }

    /**
     * Displays error.
     * @param err the err.
     * @param field the field.
     * @param hasError the hasError.
     * @param message the message.
     */
    private void showError(Label err, Control field, boolean hasError, String message) {
        err.setText(message);
        err.setVisible(hasError);
        err.setManaged(hasError);

        if (field instanceof TextField tf) {
            tf.setStyle(hasError ? TF_ERROR : TF_NORMAL);
        } else if (field instanceof ComboBox<?> cb) {
            cb.setStyle(hasError ? comboStyle(true) : comboStyle(false));
        }
    }

    /**
     * Returns the field error.
     * @param tf the tf.
     * @return the String.
     */
    private String getFieldError(TextField tf) {
        if (tf == email) return "E-mail valide requis (doit contenir @)";
        if (tf == phone) return "Le numéro de téléphone doit comporter exactement 10 chiffres";
        if (tf == firstName) return "Le prénom doit contenir uniquement des lettres";
        if (tf == lastName) return "Le nom doit contenir uniquement des lettres";
        if (tf == country) return "Le pays doit être la France";
        if (tf == floor) return "L'étage est requis";
        if (tf == householdSize) return "La taille du foyer est requise";
        if (tf == emergencyContact) return "Le contact d'urgence est requis";
        return "Ce champ est requis";
    }

    /**
     * Performs birth date combo boxes.
     */
    private void setupBirthDateComboBoxes() {
        birthDay.setPromptText("Jour");
        birthMonth.setPromptText("Mois");
        birthYear.setPromptText("Année");

        for (int i = 1; i <= 31; i++) birthDay.getItems().add(i);
        for (int i = 1; i <= 12; i++) birthMonth.getItems().add(i);

        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear; y >= 1900; y--) birthYear.getItems().add(y);

        birthDay.setPrefWidth(120);
        birthMonth.setPrefWidth(160);
        birthYear.setPrefWidth(160);

        applyIntegerComboBoxStyle(birthDay);
        applyIntegerComboBoxStyle(birthMonth);
        applyIntegerComboBoxStyle(birthYear);

        birthDay.valueProperty().addListener((obs, o, n) -> validateBirthDate());
        birthMonth.valueProperty().addListener((obs, o, n) -> validateBirthDate());
        birthYear.valueProperty().addListener((obs, o, n) -> validateBirthDate());
    }

    /**
     * Performs integer combo box style.
     * @param combo the combo.
     */
    private void applyIntegerComboBoxStyle(ComboBox<Integer> combo) {
        combo.setPrefHeight(46);
        combo.setStyle(comboStyle(false));

        combo.setButtonCell(new ListCell<>() {
            @Override
            /**
             * Updates item.
             * @param item the item.
             * @param empty the empty.
             */
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? combo.getPromptText() : String.valueOf(item));
                setTextFill(Color.web(TEXT));
            }
        });

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            /**
             * Updates item.
             * @param item the item.
             * @param empty the empty.
             */
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color:#0d111b;");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-background-color:#0d111b; -fx-text-fill:" + TEXT + ";");
                }
            }
        });
    }

    /**
     * Returns the birth date value.
     * @return the LocalDate.
     */
    private LocalDate getBirthDateValue() {
        if (birthDay.getValue() == null || birthMonth.getValue() == null || birthYear.getValue() == null) {
            return null;
        }

        try {
            return LocalDate.of(birthYear.getValue(), birthMonth.getValue(), birthDay.getValue());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates birth date.
     */
    private void validateBirthDate() {
        LocalDate date = getBirthDateValue();
        boolean hasError = date == null || date.isAfter(LocalDate.now());

        errBirthDate.setText(date == null ? "La date de naissance est requise" : "La date de naissance ne peut pas être dans le futur");
        errBirthDate.setVisible(hasError);
        errBirthDate.setManaged(hasError);

        String style = hasError ? comboStyle(true) : comboStyle(false);

        birthDay.setStyle(style);
        birthMonth.setStyle(style);
        birthYear.setStyle(style);
    }

    /**
     * Validates all.
     * @return the boolean result.
     */
    private boolean validateAll() {
        boolean valid = true;

        if (firstName.getText().trim().isEmpty()) {
            showError(errFirstName, firstName, true, getFieldError(firstName));
            valid = false;
        }

        if (lastName.getText().trim().isEmpty()) {
            showError(errLastName, lastName, true, getFieldError(lastName));
            valid = false;
        }

        if (getBirthDateValue() == null || getBirthDateValue().isAfter(LocalDate.now())) {
            validateBirthDate();
            valid = false;
        }

        if (!email.getText().contains("@") || email.getText().trim().isEmpty()) {
            showError(errEmail, email, true, getFieldError(email));
            valid = false;
        }

        if (!phone.getText().matches("\\d{10}")) {
            showError(errPhone, phone, true, getFieldError(phone));
            valid = false;
        }

        String addressValue = address.getEditor().getText().trim();

if (!addressValue.matches(".*\\d+.*") || !addressValue.matches(".*[a-zA-ZÀ-ÿ]+.*")) {
    showError(
            errAddress,
            address,
            true,
            "L'adresse doit contenir un numéro de rue et un nom de rue"
    );
    valid = false;
}

String cityValue = city.getText().trim();

if (!cityValue.equalsIgnoreCase("Lyon")) {
            showError(errCity, city, true, "Sélectionnez une ville de la liste");
            valid = false;
        }

        if (!country.getText().equalsIgnoreCase("France")) {
            showError(errCountry, country, true, getFieldError(country));
            valid = false;
        }

        if (role.getValue() == null) {
            showError(errRole, role, true, "Le rôle est requis");
            valid = false;
        }

        if (citizenBox.isVisible()) {
            if (houseType.getValue() == null) {
                showError(errHouseType, houseType, true, "Le type de logement est requis");
                valid = false;
            }

            if (floor.getText().trim().isEmpty()) {
                showError(errFloor, floor, true, getFieldError(floor));
                valid = false;
            }

            if (householdSize.getText().trim().isEmpty()) {
                showError(errHouseholdSize, householdSize, true, getFieldError(householdSize));
                valid = false;
            }

            if (emergencyContact.getText().trim().isEmpty()) {
                showError(errEmergencyContact, emergencyContact, true, getFieldError(emergencyContact));
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Sets the controller.
     * @param controller the controller.
     */
    public void setController(RegisterController controller) {
        this.controller = controller;
        setupPasswordLiveCheck();
    }

    /**
     * Performs password live check.
     */
    private void setupPasswordLiveCheck() {
        if (controller == null) return;

        Runnable validate = () -> {
            String pwd = password.isVisible() ? password.getText() : visiblePassword.getText();
            String confirm = confirmPassword.isVisible() ? confirmPassword.getText() : visibleConfirmPassword.getText();

            boolean length = controller.validatePasswordLength(pwd);
            boolean upper = controller.validatePasswordUpper(pwd);
            boolean digit = controller.validatePasswordDigit(pwd);
            boolean lower = controller.validatePasswordLower(pwd);
            boolean passwordValid = length && upper && lower && digit;
            boolean samePassword = controller.passwordsMatch(pwd, confirm);

            if (!pwd.isEmpty() && !passwordValid) {
                errPassword.setText("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre.");
                errPassword.setVisible(true);
                errPassword.setManaged(true);
            } else if (!confirm.isEmpty() && !samePassword) {
                errPassword.setText("Les mots de passe ne correspondent pas.");
                errPassword.setVisible(true);
                errPassword.setManaged(true);
            } else {
                errPassword.setVisible(false);
                errPassword.setManaged(false);
            }

            applyPasswordColorStyle(password, pwd, passwordValid);
            applyPasswordColorStyle(confirmPassword, confirm, confirm.isEmpty() || samePassword);
        };

        password.textProperty().addListener((o, ov, n) -> validate.run());
        visiblePassword.textProperty().addListener((o, ov, n) -> validate.run());
        confirmPassword.textProperty().addListener((o, ov, n) -> validate.run());
        visibleConfirmPassword.textProperty().addListener((o, ov, n) -> validate.run());
    }

    /**
     * Performs role visibility.
     */
    private void setupRoleVisibility() {
        role.valueProperty().addListener((obs, o, n) -> {
            String technicalValue = (n != null && n.equals("Citoyen")) ? "citizen" : "rescue";
            boolean isCitizen = "citizen".equalsIgnoreCase(technicalValue);
            citizenBox.setVisible(isCitizen);
            citizenBox.setManaged(isCitizen);
        });

        houseType.valueProperty().addListener((obs, o, n) -> {
            String technicalValue = (n != null && n.equals("Appartement")) ? "Apartment" : "House";

            if ("Apartment".equalsIgnoreCase(technicalValue)) {
                floor.setPromptText("Etage");
            } else if ("House".equalsIgnoreCase(technicalValue)) {
                floor.setPromptText("Nombre d'étages dans la maison");
            }
        });
    }

    /**
     * Performs only letters.
     * @param field the field.
     */
    private void allowOnlyLetters(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZÀ-ÿ\\s'-]*")) {
                field.setText(oldValue);
            }
        });
    }

    /**
     * Performs only letters fields.
     */
    private void setupOnlyLettersFields() {
        allowOnlyLetters(firstName);
        allowOnlyLetters(lastName);
    }

    /**
     * Performs enter navigation.
     */
    private void setupEnterNavigation() {
        firstName.setOnAction(e -> lastName.requestFocus());
        lastName.setOnAction(e -> birthDay.requestFocus());

        email.setOnAction(e -> phone.requestFocus());
        phone.setOnAction(e -> password.requestFocus());

        password.setOnAction(e -> confirmPassword.requestFocus());
        visiblePassword.setOnAction(e -> visibleConfirmPassword.requestFocus());

        confirmPassword.setOnAction(e -> address.requestFocus());
        visibleConfirmPassword.setOnAction(e -> address.requestFocus());

        address.setOnAction(e -> city.requestFocus());
        city.setOnAction(e -> country.requestFocus());

        country.setOnAction(e -> role.requestFocus());

        floor.setOnAction(e -> householdSize.requestFocus());
        householdSize.setOnAction(e -> emergencyContact.requestFocus());
        emergencyContact.setOnAction(e -> registerBtn.fire());
    }

    /**
     * Updates coordinates from city.
     */
    private void updateCoordinatesFromCity() {
        String cityText = city.getText().trim().toLowerCase();
    
        if (cityText.contains("parmain")) {
            detectedLat = 49.1120;
            detectedLng = 2.2090;
        } else if (cityText.contains("lyon")) {
            detectedLat = 45.7640;
            detectedLng = 4.8357;
        } else if (cityText.contains("cergy")) {
            detectedLat = 49.0360;
            detectedLng = 2.0760;
        } else if (cityText.contains("paris")) {
            detectedLat = 48.8566;
            detectedLng = 2.3522;
        }
    }

    /**
     * Handles register.
     */
    private void handleRegister() {
        if (controller == null) return;

        if (!validateAll()) {
            System.out.println("[Créer un compte] FORMULAIRE INVALIDE");
            return;
        }

        String pwd = password.isVisible() ? password.getText() : visiblePassword.getText();
        String confirm = confirmPassword.isVisible() ? confirmPassword.getText() : visibleConfirmPassword.getText();

        if (!controller.canRegister(pwd, confirm)) {
            errPassword.setText("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre. Les deux mots de passe doivent aussi correspondre.");
            errPassword.setVisible(true);
            errPassword.setManaged(true);

            applyPasswordColorStyle(password, pwd, false);
            applyPasswordColorStyle(confirmPassword, confirm, false);
            return;
        }

        int size = 0, floorNum = 0;

        try {
            size = householdSize.getText() != null && !householdSize.getText().isEmpty()
                    ? Integer.parseInt(householdSize.getText())
                    : 0;
        } catch (NumberFormatException e) {
            System.out.println("[Créer un compte] Erreur : Taille du foyer invalide");
        }

        try {
            floorNum = floor.getText() != null && !floor.getText().isEmpty()
                    ? Integer.parseInt(floor.getText())
                    : 0;
        } catch (NumberFormatException e) {
            System.out.println("[Créer un compte] Erreur : Étage invalide");
        }
        
        String fullAddress =
        address.getEditor().getText()
        + ", "
        + city.getText()
        + ", "
        + country.getText();

double[] coords =
        service.GeocodingService.getCoordinates(fullAddress);

if (coords != null) {
    detectedLat = coords[0];
    detectedLng = coords[1];
}
        boolean writeSuccess = controller.handleUserRegistration(
                firstName.getText(),
                lastName.getText(),
                getBirthDateValue(),
                email.getText(),
                phone.getText(),
                pwd,
                address.getEditor().getText(),
                city.getText(),
                country.getText(),
                houseType.getValue(),
                floorNum,
                this.detectedLat,
                this.detectedLng,
                role.getValue(),
                size,
                pets.isSelected(),
                medicalNeeds.getText(),
                emergencyContact.getText(),
                pmrCheckBox.isSelected()
        );

        if (writeSuccess) {
            System.out.println("[Créer un compte] INSCRIPTION RÉUSSIE ET ENREGISTRÉE !");
            Main.showWelcomeView();
        } else {
            System.out.println("[Créer un compte] L'inscription a échoué (Email déjà utilisé ou erreur JSON)");
        }
    }

    /**
     * Performs check box.
     * @param checkBox the checkBox.
     */
    private void styleCheckBox(CheckBox checkBox) {
        checkBox.setFont(Font.font("System", 13));
        checkBox.setTextFill(Color.web(MUTED));
        checkBox.setStyle("-fx-cursor:hand;");
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
     * Performs button style.
     * @param hover the hover.
     * @return the String.
     */
    private String secondaryButtonStyle(boolean hover) {
        return "-fx-background-color:" + (hover ? "rgba(59,108,255,0.12)" : "transparent") + ";" +
                "-fx-border-color:" + (hover ? BLUE_HOVER : "rgba(59,108,255,0.50)") + ";" +
                "-fx-border-width:1.2;" +
                "-fx-border-radius:14;" +
                "-fx-background-radius:14;" +
                "-fx-cursor:hand;";
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

}
