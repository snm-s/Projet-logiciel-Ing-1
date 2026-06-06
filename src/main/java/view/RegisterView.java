package view;

import java.time.LocalDate;

import app.Main;
import controller.RegisterController;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import service.CityService;

public class RegisterView extends StackPane {

    // ==========================================
    // CHAMPS
    // ==========================================
    private ScrollPane scrollPane = new ScrollPane();
    private VBox root = new VBox(20);
    private RegisterController controller;
    private Button backButton;

    private TextField firstName = new TextField(), lastName = new TextField();
    private ComboBox<Integer> birthDay = new ComboBox<>();
    private ComboBox<Integer> birthMonth = new ComboBox<>();
    private ComboBox<Integer> birthYear = new ComboBox<>();
    private TextField email = new TextField(), phone = new TextField();
    private PasswordField password = new PasswordField(), confirmPassword = new PasswordField();
    private TextField visiblePassword = new TextField(), visibleConfirmPassword = new TextField();
    private Label ruleLength = new Label("• Au moins 8 charactères"),
                  ruleUpper  = new Label("• Au moins 1 lettre majuscule"),
                  ruleDigit  = new Label("• Au moins 1 chiffre");
    private TextField address = new TextField(), country = new TextField();
    private ComboBox<String> city = new ComboBox<>();
    private boolean citySelectionInProgress = false;
    private ComboBox<String> houseType = new ComboBox<>();
    private TextField floor = new TextField();
    private Label gpsLabel = new Label("GPS non configuré");
    private Button gpsButton = new Button("Détecter ma localisation GPS");
    private ComboBox<String> role = new ComboBox<>();
    private VBox citizenBox = new VBox(15);
    private TextField householdSize = new TextField();
    private double detectedLat = 48.8566; // Valeur par défaut (Paris)
private double detectedLng = 2.3522;
    private CheckBox pets = new CheckBox("Je possède des animaux de compagnie");
    private CheckBox pmrCheckBox = new CheckBox("Je suis une personne à mobilité réduite (PMR)");
    private TextArea medicalNeeds = new TextArea();
    private TextField emergencyContact = new TextField();
    private Button registerBtn = new Button("Créer mon compte");

    // Validation labels
    private Label errFirstName = errLabel(), errLastName = errLabel(), errBirthDate = errLabel(),
                  errEmail = errLabel(), errPhone = errLabel(),
                  errAddress = errLabel(), errCity = errLabel(), errCountry = errLabel(),
                  errHouseType = errLabel(), errFloor = errLabel(),
                  errHouseholdSize = errLabel(), errEmergencyContact = errLabel(),
                  errRole = errLabel();

    // ==========================================
    // STYLES CONSTANTS
    // ==========================================
    private static final String TF_NORMAL =
        "-fx-background-color: rgba(255,255,255,0.05);" +
        "-fx-border-color: rgba(255,255,255,0.25);" +
        "-fx-border-radius: 6; -fx-background-radius: 6;" +
        "-fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;";
    private static final String TF_FOCUS =
        "-fx-background-color: rgba(255,255,255,0.08);" +
        "-fx-border-color: #0b5cbf;" +
        "-fx-border-radius: 6; -fx-background-radius: 6;" +
        "-fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;";
    private static final String TF_ERROR =
        "-fx-background-color: rgba(231,76,60,0.1);" +
        "-fx-border-color: #e74c3c; -fx-border-width: 1.5;" +
        "-fx-border-radius: 6; -fx-background-radius: 6;" +
        "-fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;";
    private static final String BACK_NORMAL =
        "-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-font-size: 28px; -fx-padding: 0 0 10 0; -fx-cursor: hand;";
    private static final String BACK_HOVER =
        "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 28px; -fx-padding: 0 0 10 0; -fx-cursor: hand;";
    private static final String GPS_NORMAL =
        "-fx-background-color: transparent; -fx-border-color: #0b5cbf; -fx-border-radius: 6; -fx-text-fill: #0b5cbf; -fx-font-weight: bold; -fx-cursor: hand;";
    private static final String GPS_HOVER =
        "-fx-background-color: rgba(11,92,191,0.1); -fx-border-color: #0b5cbf; -fx-border-radius: 6; -fx-text-fill: #0b5cbf; -fx-cursor: hand;";

    // ==========================================
    // CONSTRUCTEUR
    // ==========================================
    public RegisterView() {
        this.setPrefSize(1100, 700);

        Region backgroundFiller = new Region();
        try {
            var resource = getClass().getResource("/images/Gemini_Generated_Image_qyih9tqyih9tqyih.png");
            backgroundFiller.setStyle(resource != null
                ? "-fx-background-image: url('" + resource.toExternalForm() + "');" +
                  "-fx-background-repeat: no-repeat; -fx-background-size: cover; -fx-background-position: center right;"
                : "-fx-background-color: #0b1a30;");
        } catch (Exception e) {
            backgroundFiller.setStyle("-fx-background-color: #0b1a30;");
        }

        Region gradientOverlay = new Region();
        gradientOverlay.setStyle(
            "-fx-background-color: linear-gradient(to right," +
            "#0b1a30 0%, #0b1a30 40%, rgba(11,26,48,0.9) 60%," +
            "rgba(11,26,48,0.3) 85%, rgba(11,26,48,0.1) 100%);");

        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-viewport-transparent: true;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(30, 80, 50, 80));
        root.setMaxWidth(550);
        StackPane.setAlignment(scrollPane, Pos.CENTER_LEFT);

        build();
        setupRoleVisibility();
        setupOnlyLettersFields();
        setupEnterNavigation();
        this.getChildren().addAll(backgroundFiller, gradientOverlay, scrollPane);
    }

    // ==========================================
    // BUILD
    // ==========================================
    private void build() {

        // --- HEADER ---
        backButton = new Button("←");
        backButton.setStyle(BACK_NORMAL);
        backButton.setOnMouseEntered(e -> backButton.setStyle(BACK_HOVER));
        backButton.setOnMouseExited(e -> backButton.setStyle(BACK_NORMAL));

        SVGPath logoSvg = new SVGPath();
        logoSvg.setContent("M15 2 L28 12 H23 V22 H7 V12 H2 Z M2 25 Q8 23 15 25 T28 25 M2 28 Q8 26 15 28 T28 28");
        logoSvg.setStroke(Color.WHITE); logoSvg.setStrokeWidth(2.2); logoSvg.setFill(Color.TRANSPARENT);
        StackPane logoContainer = new StackPane(logoSvg); logoContainer.setPrefSize(35, 35);

        Text brandTitle = new Text("Inondation");
        brandTitle.setFont(Font.font("System", FontWeight.BOLD, 30)); brandTitle.setFill(Color.WHITE);
        Text brandSubtitle = new Text("Simulation & Gestion des urgences");
        brandSubtitle.setFont(Font.font("System", FontWeight.LIGHT, 12)); brandSubtitle.setFill(Color.web("#a0b2ce"));

        HBox brandHeader = new HBox(15, backButton, logoContainer, new VBox(2, brandTitle, brandSubtitle));
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        Text pageTitle = new Text("Créer un compte");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28)); pageTitle.setFill(Color.WHITE);
        Text pageSubtitle = new Text("Rejoignez le système de simulation et de gestion des urgences");
        pageSubtitle.setFont(Font.font("System", FontWeight.LIGHT, 12)); pageSubtitle.setFill(Color.web("#a0b2ce"));
        VBox headerBox = new VBox(5, pageTitle, pageSubtitle);
        headerBox.setPadding(new Insets(20, 0, 10, 0));
        root.getChildren().addAll(brandHeader, headerBox);

        // --- IDENTITY ---
        firstName.setPromptText("Prénom"); lastName.setPromptText("Nom");
        applyTextFieldStyle(firstName); applyTextFieldStyle(lastName);
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

        // --- CONTACT ---
        email.setPromptText("Email"); applyTextFieldStyle(email);
        phone.setPromptText("Numéro de téléphone"); applyTextFieldStyle(phone);
        setupLiveValidation(email, errEmail, f -> f.getText().contains("@") && !f.getText().trim().isEmpty());
        setupLiveValidation(phone, errPhone, f -> f.getText().matches("\\d{10}"));
        root.getChildren().addAll(createSectionLabel("Contact"),
            fieldRow(email, errEmail), fieldRow(phone, errPhone));

        // --- PASSWORD ---
        password.textProperty().bindBidirectional(visiblePassword.textProperty());
        confirmPassword.textProperty().bindBidirectional(visibleConfirmPassword.textProperty());
        applyRuleLabelErrorStyle(ruleLength); applyRuleLabelErrorStyle(ruleUpper); applyRuleLabelErrorStyle(ruleDigit);
        VBox rulesBox = new VBox(4, ruleLength, ruleUpper, ruleDigit);
        rulesBox.setPadding(new Insets(0, 0, 10, 5));
        root.getChildren().addAll(createSectionLabel("Mot de passe"),
            createPasswordFieldWithEye(password, visiblePassword, "Mot de passe"),
            createPasswordFieldWithEye(confirmPassword, visibleConfirmPassword, "Confirmer le mot de passe"),
            rulesBox);

        // --- ADDRESS ---
        address.setPromptText("Addresse"); applyTextFieldStyle(address);
        city.setPromptText("Ville");
        city.setEditable(true);
        city.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {

    if (citySelectionInProgress) {
        return;
    }

    if (newValue == null || newValue.trim().length() < 2) {
        city.getItems().clear();
        city.hide();
        return;
    }

    new Thread(() -> {
        var results = CityService.searchCities(newValue);

        Platform.runLater(() -> {
            if (!city.getEditor().getText().equals(newValue)) {
                return;
            }

            city.getItems().setAll(results);

            if (!results.isEmpty() && city.isFocused()) {
                city.show();
            } else {
                city.hide();
            }
        });
    }).start();
});

city.valueProperty().addListener((obs, oldValue, selected) -> {
    if (selected != null) {
        citySelectionInProgress = true;
        city.getEditor().setText(selected);
        citySelectionInProgress = false;
        city.hide();
        country.requestFocus();
    }
});

applyEditableComboBoxStyle(city);
        country.setPromptText("Pays"); applyTextFieldStyle(country);
        setupLiveValidation(address, errAddress, f ->
            f.getText().matches(".*\\d+.*") &&
            f.getText().matches(".*[a-zA-ZÀ-ÿ]+.*")
        );
        
        
        setupLiveValidation(country, errCountry, f ->
            f.getText().equalsIgnoreCase("France")
        );
        root.getChildren().addAll(createSectionLabel("Addresse"),
            fieldRow(address, errAddress), fieldRow(city, errCity), fieldRow(country, errCountry));

        // --- LOCATION ---
        gpsButton.setMaxWidth(Double.MAX_VALUE); gpsButton.setPrefHeight(40);
        gpsButton.setStyle(GPS_NORMAL);
        gpsButton.setOnMouseEntered(e -> gpsButton.setStyle(GPS_HOVER));
        gpsButton.setOnMouseExited(e -> gpsButton.setStyle(GPS_NORMAL));
        gpsLabel.setFont(Font.font("System", 13)); gpsLabel.setTextFill(Color.web("#a0b2ce"));
  
        gpsButton.setOnAction(e -> {
        gpsLabel.setText("⏳ Détection...");
        controller.handleDetectGps();
        });
        root.getChildren().addAll(createSectionLabel("Localisation"), gpsButton, gpsLabel);

        // --- ROLE ---
        role.getItems().addAll("Citoyen", "Sauveteur");
        role.setPromptText("Selectionnez votre rôle"); applyComboBoxStyle(role);
        role.valueProperty().addListener((obs, o, n) -> showError(errRole, role, n == null || n.trim().isEmpty(), "Veuillez sélectionner un rôle"));
        root.getChildren().addAll(createSectionLabel("Rôle"), fieldRow(role, errRole));

        // --- CITIZEN BOX ---
        houseType.getItems().addAll("Appartement", "Maison");
        houseType.setPromptText("Selectionnez votre type de logement"); applyComboBoxStyle(houseType);
        floor.setPromptText("Etage"); applyTextFieldStyle(floor);
        householdSize.setPromptText("Nombre de membres du foyer"); applyTextFieldStyle(householdSize);
        emergencyContact.setPromptText("Contact d'urgence"); applyTextFieldStyle(emergencyContact);
        medicalNeeds.setPromptText("Besoins médicaux"); medicalNeeds.setPrefHeight(80);
        medicalNeeds.setStyle(
            "-fx-control-inner-background: rgba(255,255,255,0.05); -fx-text-fill: white;" +
            "-fx-prompt-text-fill: #a0b2ce; -fx-background-color: transparent;" +
            "-fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 6;");
        pets.setFont(Font.font("System", 13)); pets.setTextFill(Color.WHITE);
        setupLiveValidation(floor, errFloor, f -> !f.getText().trim().isEmpty());
        setupLiveValidation(householdSize, errHouseholdSize, f -> !f.getText().trim().isEmpty());
        setupLiveValidation(emergencyContact, errEmergencyContact, f -> !f.getText().trim().isEmpty());
        houseType.valueProperty().addListener((obs, o, n) -> showError(errHouseType, houseType, n == null || n.trim().isEmpty(), "House type is required"));
        citizenBox.getChildren().addAll(
            createSectionLabel("Informations sur le citoyen"),
            fieldRow(houseType, errHouseType), fieldRow(floor, errFloor),
            fieldRow(householdSize, errHouseholdSize), pets, pmrCheckBox, medicalNeeds,
            createSectionLabel("Contact d'urgence"), fieldRow(emergencyContact, errEmergencyContact));
        citizenBox.setVisible(false); citizenBox.setManaged(false);
        root.getChildren().add(citizenBox);

        // --- REGISTER BUTTON ---
        registerBtn.setMaxWidth(Double.MAX_VALUE); registerBtn.setPrefHeight(45);
        registerBtn.setFont(Font.font("System", FontWeight.BOLD, 15));
        registerBtn.setTextFill(Color.WHITE);
        registerBtn.setStyle("-fx-background-color: #0b5cbf; -fx-background-radius: 6; -fx-cursor: hand;");
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle("-fx-background-color: #0e73eb; -fx-background-radius: 6; -fx-cursor: hand;"));
        registerBtn.setOnMouseExited(e -> registerBtn.setStyle("-fx-background-color: #0b5cbf; -fx-background-radius: 6;"));
        registerBtn.setOnAction(e -> handleRegister());
        VBox.setMargin(registerBtn, new Insets(15, 0, 0, 0));
        root.getChildren().add(registerBtn);
    }

    // ==========================================
    // UTILITAIRES UI
    // ==========================================
    private Label createSectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.SEMI_BOLD, 15));
        l.setTextFill(Color.WHITE); l.setPadding(new Insets(10, 0, 2, 0));
        return l;
    }

    private Label errLabel() {
        Label l = new Label();
        l.setFont(Font.font("System", 11));
        l.setTextFill(Color.web("#e74c3c"));
        l.setVisible(false); l.setManaged(false);
        return l;
    }

    private VBox fieldRow(Control field, Label err) {
        VBox box = new VBox(3, field, err);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void applyTextFieldStyle(TextField tf) {
        tf.setPrefHeight(42); tf.setFont(Font.font("System", 13));
        tf.setStyle(TF_NORMAL);
        tf.focusedProperty().addListener((obs, o, n) -> {
            if (!tf.getStyle().contains("#e74c3c"))
                tf.setStyle(n ? TF_FOCUS : TF_NORMAL);
        });
    }

    private void applyTextFieldStyle(PasswordField pf) {
        pf.setPrefHeight(42); pf.setFont(Font.font("System", 13));
        pf.setStyle(TF_NORMAL);
        pf.focusedProperty().addListener((obs, o, n) -> {
            if (!pf.getStyle().contains("#e74c3c"))
                pf.setStyle(n ? TF_FOCUS : TF_NORMAL);
        });
    }

    private void applyComboBoxStyle(ComboBox<String> combo) {
        combo.setMaxWidth(Double.MAX_VALUE); combo.setPrefHeight(42);
        combo.setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-border-color: rgba(255,255,255,0.25);" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-text-fill: white;"
        );
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? combo.getPromptText() : item);
                setTextFill(Color.WHITE);
            }
        });
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color: #0b1a30;"); }
                else { setText(item); setStyle("-fx-background-color: #0b1a30; -fx-text-fill: white;"); }
            }
        });
        combo.setOnShowing(e -> combo.getScene().getRoot()
            .lookupAll(".combo-box-popup .list-view .list-cell")
            .forEach(n -> n.setStyle("-fx-background-color: #0b1a30; -fx-text-fill: white;")));
    }

    private void applyEditableComboBoxStyle(ComboBox<String> combo) {
        applyComboBoxStyle(combo);
    
        combo.getEditor().setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #a0b2ce;" +
            "-fx-border-color: transparent;"
        );
    }

    private void applyPasswordColorStyle(PasswordField field, String text, boolean isValid) {
        if (text == null || text.isEmpty()) { field.setStyle(TF_NORMAL); return; }
        field.setStyle(isValid
            ? "-fx-background-color: rgba(46,204,113,0.1); -fx-border-color: #2ecc71; -fx-text-box-border: transparent; -fx-focus-color: transparent; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;"
            : "-fx-background-color: rgba(231,76,60,0.1); -fx-border-color: #e74c3c; -fx-text-box-border: transparent; -fx-focus-color: transparent; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;");
    }

    private void applyRuleLabelSuccessStyle(Label l) { l.setFont(Font.font("System", FontWeight.BOLD, 12)); l.setTextFill(Color.web("#2ecc71")); }
    private void applyRuleLabelErrorStyle(Label l)   { l.setFont(Font.font("System", FontWeight.NORMAL, 12)); l.setTextFill(Color.web("#e74c3c")); }

    private StackPane createPasswordFieldWithEye(PasswordField pf, TextField tf, String prompt) {
        pf.setPromptText(prompt);
        tf.setPromptText(prompt);
    
        pf.setPrefHeight(42);
        tf.setPrefHeight(42);
    
        applyTextFieldStyle(pf);
        applyTextFieldStyle(tf);
    
        tf.setVisible(false);
        tf.setManaged(false);
    
        Button eyeButton = new Button();
        eyeButton.setPrefSize(38, 38);
        eyeButton.setMinSize(38, 38);
        eyeButton.setMaxSize(38, 38);
        eyeButton.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-cursor: hand;"
        );
    
        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent(
            "M2 10 Q10 2 18 10 Q10 18 2 10 " +
            "M10 6 A4 4 0 1 1 10 14 A4 4 0 1 1 10 6"
        );
        eyeIcon.setStroke(Color.WHITE);
        eyeIcon.setStrokeWidth(1.8);
        eyeIcon.setFill(Color.TRANSPARENT);
    
        SVGPath slashIcon = new SVGPath();
        slashIcon.setContent("M3 17 L17 3");
        slashIcon.setStroke(Color.WHITE);
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

    // ==========================================
    // VALIDATION EN TEMPS RÉEL
    // ==========================================
    @FunctionalInterface interface FieldValidator { boolean isValid(TextField f); }

    private void setupLiveValidation(TextField tf, Label err, FieldValidator validator) {
        tf.textProperty().addListener((obs, o, n) -> {
            if (tf.getText().trim().isEmpty()) { showError(err, tf, true, getFieldError(tf)); }
            else { showError(err, tf, !validator.isValid(tf), getFieldError(tf)); }
        });
    }

    private void showError(Label err, Control field, boolean hasError, String message) {
        err.setText(message); err.setVisible(hasError); err.setManaged(hasError);
        if (field instanceof TextField tf) tf.setStyle(hasError ? TF_ERROR : TF_NORMAL);
        else if (field instanceof ComboBox<?> cb)
            cb.setStyle(hasError
                ? "-fx-background-color: rgba(231,76,60,0.1); -fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white;"
                : "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white;");
    }

    private String getFieldError(TextField tf) {
        if (tf == email)            return "E-mail valide requis (doit contenir @)";
        if (tf == phone)            return "Le numéro de téléphone doit comporter exactement 10 chiffres";
        if (tf == firstName)        return "Le prénom doit contenir uniquement des lettres";
        if (tf == lastName)         return "Le nom doit contenir uniquement des lettres";
        if (tf == address)          return "L'adresse doit contenir un numéro de rue et un nom de rue";
        if (tf == country)          return "Le pays doit être la France";
        if (tf == floor)            return "L'étage est requis";
        if (tf == householdSize)    return "La taille du foyer est requise";
        if (tf == emergencyContact) return "Le contact d'urgence est requis";
        return "Ce champ est requis";
    }
    
    private void setupBirthDateComboBoxes() {
        birthDay.setPromptText("Jour");
        birthMonth.setPromptText("Mois");
        birthYear.setPromptText("Année");
    
        for (int i = 1; i <= 31; i++) {
            birthDay.getItems().add(i);
        }
    
        for (int i = 1; i <= 12; i++) {
            birthMonth.getItems().add(i);
        }
    
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear; y >= 1900; y--) {
            birthYear.getItems().add(y);
        }
    
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
    
    private void applyIntegerComboBoxStyle(ComboBox<Integer> combo) {
        combo.setPrefHeight(42);
        combo.setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-border-color: rgba(255,255,255,0.25);" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-text-fill: white;"
        );
    
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? combo.getPromptText() : String.valueOf(item));
                setTextFill(Color.WHITE);
            }
        });
    
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0b1a30;");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-background-color: #0b1a30; -fx-text-fill: white;");
                }
            }
        });
    }
    
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

    private void validateBirthDate() {
    LocalDate date = getBirthDateValue();

    boolean hasError = date == null || date.isAfter(LocalDate.now());

    errBirthDate.setText(date == null ? "La date de naissance est requise" : "La date de naissance ne peut pas être dans le futur");
    errBirthDate.setVisible(hasError);
    errBirthDate.setManaged(hasError);

    String style = hasError
        ? "-fx-background-color: rgba(231,76,60,0.1); -fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;"
        : "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 6; -fx-background-radius: 6;";

    birthDay.setStyle(style);
    birthMonth.setStyle(style);
    birthYear.setStyle(style);
}

    private boolean validateAll() {
        boolean valid = true;
        // Identity
        if (firstName.getText().trim().isEmpty())  { showError(errFirstName, firstName, true, getFieldError(firstName)); valid = false; }
        if (lastName.getText().trim().isEmpty())   { showError(errLastName, lastName, true, getFieldError(lastName)); valid = false; }
        if (getBirthDateValue() == null || getBirthDateValue().isAfter(LocalDate.now())) {
            validateBirthDate();
            valid = false;
        }
        // Contact
        if (!email.getText().contains("@") || email.getText().trim().isEmpty()) { showError(errEmail, email, true, getFieldError(email)); valid = false; }
        if (!phone.getText().matches("\\d{10}")) { showError(errPhone, phone, true, getFieldError(phone)); valid = false; }
        // Address
        if (!address.getText().matches(".*\\d+.*") || !address.getText().matches(".*[a-zA-ZÀ-ÿ]+.*")) {
            showError(errAddress, address, true, getFieldError(address));
            valid = false;
        }
        
        String cityValue = city.getEditor().getText().trim();

if (cityValue.isEmpty() || !city.getItems().contains(cityValue)) {
    showError(errCity, city, true, "Sélectionnez une ville de la liste");
    valid = false;
}
        
        if (!country.getText().equalsIgnoreCase("France")) {
            showError(errCountry, country, true, getFieldError(country));
            valid = false;
        }
        // Role
        if (role.getValue() == null) { showError(errRole, role, true, "Le rôle est requis"); valid = false; }
        // Citizen fields
        if (citizenBox.isVisible()) {
            if (houseType.getValue() == null) { showError(errHouseType, houseType, true, "Le type de logement est requis"); valid = false; }
            if (floor.getText().trim().isEmpty())           { showError(errFloor, floor, true, getFieldError(floor)); valid = false; }
            if (householdSize.getText().trim().isEmpty())   { showError(errHouseholdSize, householdSize, true, getFieldError(householdSize)); valid = false; }
            if (emergencyContact.getText().trim().isEmpty()){ showError(errEmergencyContact, emergencyContact, true, getFieldError(emergencyContact)); valid = false; }
        }
        return valid;
    }

    // ==========================================
    // COMPORTEMENT DYNAMIQUE
    // ==========================================
    public void setController(RegisterController controller) {
        this.controller = controller;
        setupPasswordLiveCheck();
    }

    private void setupPasswordLiveCheck() {
        if (controller == null) return;
        Runnable validate = () -> {
            String pwd     = password.isVisible() ? password.getText() : visiblePassword.getText();
            String confirm = confirmPassword.isVisible() ? confirmPassword.getText() : visibleConfirmPassword.getText();
            boolean length = controller.validatePasswordLength(pwd);
            boolean upper  = controller.validatePasswordUpper(pwd);
            boolean digit  = controller.validatePasswordDigit(pwd);
            ruleLength.setTextFill(length ? Color.web("#2ecc71") : Color.web("#e74c3c"));
            ruleUpper.setTextFill(upper   ? Color.web("#2ecc71") : Color.web("#e74c3c"));
            ruleDigit.setTextFill(digit   ? Color.web("#2ecc71") : Color.web("#e74c3c"));
            boolean passwordValid = length && upper && digit;
            applyPasswordColorStyle(password, pwd, passwordValid);
            applyPasswordColorStyle(confirmPassword, confirm, controller.passwordsMatch(pwd, confirm) && passwordValid);
        };
        password.textProperty().addListener((o, ov, n) -> validate.run());
        visiblePassword.textProperty().addListener((o, ov, n) -> validate.run());
        confirmPassword.textProperty().addListener((o, ov, n) -> validate.run());
        visibleConfirmPassword.textProperty().addListener((o, ov, n) -> validate.run());
    }

    private void setupRoleVisibility() {
        role.valueProperty().addListener((obs, o, n) -> {
            String technicalValue = (n != null && n.equals("Citoyen")) ? "citizen" : "rescue";
            boolean isCitizen = "citizen".equalsIgnoreCase(technicalValue);
            citizenBox.setVisible(isCitizen); citizenBox.setManaged(isCitizen);
        });
        houseType.valueProperty().addListener((obs, o, n) -> {
            String technicalValue = (n != null && n.equals("Appartement")) ? "Apartment" : "House";
            if ("Apartment".equalsIgnoreCase(technicalValue))  floor.setPromptText("Etage");
            else if ("House".equalsIgnoreCase(technicalValue)) floor.setPromptText("Nombre d'étages dans la maison");
        });
    }

    private void allowOnlyLetters(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZÀ-ÿ\\s'-]*")) {
                field.setText(oldValue);
            }
        });
    }

    private void setupOnlyLettersFields() {
        allowOnlyLetters(firstName);
        allowOnlyLetters(lastName);
    }

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
        city.getEditor().setOnAction(e -> country.requestFocus());
    
        country.setOnAction(e -> gpsButton.requestFocus());
    
        floor.setOnAction(e -> householdSize.requestFocus());
        householdSize.setOnAction(e -> emergencyContact.requestFocus());
        emergencyContact.setOnAction(e -> registerBtn.fire());
    }

    // ==========================================
    // SOUMISSION DU FORMULAIRE
    // ==========================================
    private void handleRegister() {
        if (controller == null) return;
        if (!validateAll()) { System.out.println("[Créer un compte] FORMULAIRE INVALIDE"); return; }

        String pwd     = password.isVisible() ? password.getText() : visiblePassword.getText();
        String confirm = confirmPassword.isVisible() ? confirmPassword.getText() : visibleConfirmPassword.getText();
        if (!controller.canRegister(pwd, confirm)) {
            System.out.println("[Créer FORMULAIRE INVALIDE (Mots de passe incorrects)");
            applyPasswordColorStyle(confirmPassword, confirm, false);
            return;
        }

        int size = 0, floorNum = 0;
        try { size     = householdSize.getText() != null && !householdSize.getText().isEmpty() ? Integer.parseInt(householdSize.getText()) : 0; }
        catch (NumberFormatException e) { System.out.println("[Créer un compte] Erreur : Taille du foyer invalide"); }
        try { floorNum = floor.getText() != null && !floor.getText().isEmpty() ? Integer.parseInt(floor.getText()) : 0; }
        catch (NumberFormatException e) { System.out.println("[Créer un compte] Erreur : Étage invalide"); }

        boolean writeSuccess = controller.handleUserRegistration(

        firstName.getText(), lastName.getText(), getBirthDateValue(),
        email.getText(), phone.getText(), pwd,
        address.getText(), city.getEditor().getText(), country.getText(),
        houseType.getValue(), floorNum, this.detectedLat, this.detectedLng, role.getValue(), // <-- CORRIGÉ ICI
        size, pets.isSelected(), medicalNeeds.getText(),
        emergencyContact.getText(), pmrCheckBox.isSelected());

        if (writeSuccess) { System.out.println("[Créer un compte] INSCRIPTION RÉUSSIE ET ENREGISTRÉE !"); Main.showWelcomeView(); }
        else              { System.out.println("[Créer un compte] L'inscription a échoué (Email déjà utilisé ou erreur JSON)"); }


    }


    // ==========================================
    // GETTER
    // ==========================================
    public Button getBackButton() { return backButton; }

   public void fillLocationFields(double lat, double lng, String cityValue, String countryValue) {
    this.detectedLat = lat;
    this.detectedLng = lng;
    city.getEditor().setText(cityValue);
    country.setText(countryValue);
    gpsLabel.setText(cityValue.isEmpty()
    ? "❌ Détection échouée — veuillez entrer votre adresse manuellement"
    : "✅ Localisation approximative détectée — veuillez entrer votre rue");
}
}
