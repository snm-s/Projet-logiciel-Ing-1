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
import app.Main;
import controller.RegisterController;
import java.time.LocalDate;

public class RegisterView extends StackPane {

    // ==========================================
    // CHAMPS
    // ==========================================
    private ScrollPane scrollPane = new ScrollPane();
    private VBox root = new VBox(20);
    private RegisterController controller;
    private Button backButton;

    private TextField firstName = new TextField(), lastName = new TextField();
    private DatePicker birthDate = new DatePicker();
    private TextField email = new TextField(), phone = new TextField();
    private PasswordField password = new PasswordField(), confirmPassword = new PasswordField();
    private TextField visiblePassword = new TextField(), visibleConfirmPassword = new TextField();
    private Label ruleLength = new Label("• At least 8 characters"),
                  ruleUpper  = new Label("• At least 1 uppercase letter"),
                  ruleDigit  = new Label("• At least 1 number");
    private TextField address = new TextField(), city = new TextField(), country = new TextField();
    private ComboBox<String> houseType = new ComboBox<>();
    private TextField floor = new TextField();
    private Label gpsLabel = new Label("GPS not set");
    private Button gpsButton = new Button("Detect GPS");
    private ComboBox<String> role = new ComboBox<>();
    private VBox citizenBox = new VBox(15);
    private TextField householdSize = new TextField();
    private CheckBox pets = new CheckBox("Has pets");
    private CheckBox pmrCheckBox = new CheckBox("PMR (Person with reduced mobility)");
    private TextArea medicalNeeds = new TextArea();
    private TextField emergencyContact = new TextField();
    private Button registerBtn = new Button("Register");

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
        Text brandSubtitle = new Text("Simulation & emergency management");
        brandSubtitle.setFont(Font.font("System", FontWeight.LIGHT, 12)); brandSubtitle.setFill(Color.web("#a0b2ce"));

        HBox brandHeader = new HBox(15, backButton, logoContainer, new VBox(2, brandTitle, brandSubtitle));
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        Text pageTitle = new Text("Sign up");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28)); pageTitle.setFill(Color.WHITE);
        Text pageSubtitle = new Text("Join the flood emergency and simulation system");
        pageSubtitle.setFont(Font.font("System", FontWeight.LIGHT, 12)); pageSubtitle.setFill(Color.web("#a0b2ce"));
        VBox headerBox = new VBox(5, pageTitle, pageSubtitle);
        headerBox.setPadding(new Insets(20, 0, 10, 0));
        root.getChildren().addAll(brandHeader, headerBox);

        // --- IDENTITY ---
        firstName.setPromptText("First name"); lastName.setPromptText("Last name");
        applyTextFieldStyle(firstName); applyTextFieldStyle(lastName);
        birthDate.setPromptText("Birth date"); birthDate.setMaxWidth(Double.MAX_VALUE);
        birthDate.getEditor().setStyle("-fx-text-fill: white; -fx-background-color: transparent;");
        birthDate.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 6; -fx-background-radius: 6; -fx-height: 45;");
        setupLiveValidation(firstName, errFirstName, f -> !f.getText().trim().isEmpty());
        setupLiveValidation(lastName, errLastName, f -> !f.getText().trim().isEmpty());
        birthDate.valueProperty().addListener((obs, o, n) -> validateBirthDate());
        root.getChildren().addAll(createSectionLabel("Identity"),
            fieldRow(firstName, errFirstName), fieldRow(lastName, errLastName), fieldRow(birthDate, errBirthDate));

        // --- CONTACT ---
        email.setPromptText("Email"); applyTextFieldStyle(email);
        phone.setPromptText("Phone number"); applyTextFieldStyle(phone);
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
        root.getChildren().addAll(createSectionLabel("Password"),
            createPasswordFieldWithEye(password, visiblePassword, "Password"),
            createPasswordFieldWithEye(confirmPassword, visibleConfirmPassword, "Confirm password"),
            rulesBox);

        // --- ADDRESS ---
        address.setPromptText("Address"); applyTextFieldStyle(address);
        city.setPromptText("City"); applyTextFieldStyle(city);
        country.setPromptText("Country"); applyTextFieldStyle(country);
        setupLiveValidation(address, errAddress, f -> !f.getText().trim().isEmpty());
        setupLiveValidation(city, errCity, f -> !f.getText().trim().isEmpty());
        setupLiveValidation(country, errCountry, f -> !f.getText().trim().isEmpty());
        root.getChildren().addAll(createSectionLabel("Address"),
            fieldRow(address, errAddress), fieldRow(city, errCity), fieldRow(country, errCountry));

        // --- LOCATION ---
        gpsButton.setMaxWidth(Double.MAX_VALUE); gpsButton.setPrefHeight(40);
        gpsButton.setStyle(GPS_NORMAL);
        gpsButton.setOnMouseEntered(e -> gpsButton.setStyle(GPS_HOVER));
        gpsButton.setOnMouseExited(e -> gpsButton.setStyle(GPS_NORMAL));
        gpsLabel.setFont(Font.font("System", 13)); gpsLabel.setTextFill(Color.web("#a0b2ce"));
        gpsButton.setOnAction(e -> gpsLabel.setText("📍 Lat: 48.85 | Lng: 2.35"));
        root.getChildren().addAll(createSectionLabel("Location"), gpsButton, gpsLabel);

        // --- ROLE ---
        role.getItems().addAll("citizen", "rescue");
        role.setPromptText("Select your role"); applyComboBoxStyle(role);
        role.valueProperty().addListener((obs, o, n) -> showError(errRole, role, n == null || n.trim().isEmpty(), "Role is required"));
        root.getChildren().addAll(createSectionLabel("Role"), fieldRow(role, errRole));

        // --- CITIZEN BOX ---
        houseType.getItems().addAll("APARTMENT", "HOUSE");
        houseType.setPromptText("Select House Type"); applyComboBoxStyle(houseType);
        floor.setPromptText("Floor"); applyTextFieldStyle(floor);
        householdSize.setPromptText("Household size"); applyTextFieldStyle(householdSize);
        emergencyContact.setPromptText("Emergency contact"); applyTextFieldStyle(emergencyContact);
        medicalNeeds.setPromptText("Medical needs"); medicalNeeds.setPrefHeight(80);
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
            createSectionLabel("Citizen Information"),
            fieldRow(houseType, errHouseType), fieldRow(floor, errFloor),
            fieldRow(householdSize, errHouseholdSize), pets, pmrCheckBox, medicalNeeds,
            createSectionLabel("Emergency Contact"), fieldRow(emergencyContact, errEmergencyContact));
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
        combo.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white;");
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

    private void applyPasswordColorStyle(PasswordField field, String text, boolean isValid) {
        if (text == null || text.isEmpty()) { field.setStyle(TF_NORMAL); return; }
        field.setStyle(isValid
            ? "-fx-background-color: rgba(46,204,113,0.1); -fx-border-color: #2ecc71; -fx-text-box-border: transparent; -fx-focus-color: transparent; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;"
            : "-fx-background-color: rgba(231,76,60,0.1); -fx-border-color: #e74c3c; -fx-text-box-border: transparent; -fx-focus-color: transparent; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-prompt-text-fill: #a0b2ce;");
    }

    private void applyRuleLabelSuccessStyle(Label l) { l.setFont(Font.font("System", FontWeight.BOLD, 12)); l.setTextFill(Color.web("#2ecc71")); }
    private void applyRuleLabelErrorStyle(Label l)   { l.setFont(Font.font("System", FontWeight.NORMAL, 12)); l.setTextFill(Color.web("#e74c3c")); }

    private StackPane createPasswordFieldWithEye(PasswordField pf, TextField tf, String prompt) {
        pf.setPromptText(prompt); tf.setPromptText(prompt);
        pf.setPrefHeight(42); tf.setPrefHeight(42);
        applyTextFieldStyle(pf); applyTextFieldStyle(tf);
        tf.setVisible(false); tf.setManaged(false);
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
        StackPane.setMargin(btn, new Insets(0, 10, 0, 0));
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
        if (tf == email)            return "Valid email required (must contain @)";
        if (tf == phone)            return "Phone must be exactly 10 digits";
        if (tf == firstName)        return "First name is required";
        if (tf == lastName)         return "Last name is required";
        if (tf == address)          return "Address is required";
        if (tf == city)             return "City is required";
        if (tf == country)          return "Country is required";
        if (tf == floor)            return "Floor is required";
        if (tf == householdSize)    return "Household size is required";
        if (tf == emergencyContact) return "Emergency contact is required";
        return "This field is required";
    }

    private void validateBirthDate() {
        boolean err = birthDate.getValue() == null || birthDate.getValue().isAfter(LocalDate.now());
        errBirthDate.setText(birthDate.getValue() == null ? "Birth date is required" : "Birth date cannot be in the future");
        errBirthDate.setVisible(err); errBirthDate.setManaged(err);
        birthDate.setStyle(err
            ? "-fx-background-color: rgba(231,76,60,0.1); -fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-height: 45;"
            : "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 6; -fx-background-radius: 6; -fx-height: 45;");
    }

    private boolean validateAll() {
        boolean valid = true;
        // Identity
        if (firstName.getText().trim().isEmpty())  { showError(errFirstName, firstName, true, getFieldError(firstName)); valid = false; }
        if (lastName.getText().trim().isEmpty())   { showError(errLastName, lastName, true, getFieldError(lastName)); valid = false; }
        if (birthDate.getValue() == null || birthDate.getValue().isAfter(LocalDate.now())) { validateBirthDate(); valid = false; }
        // Contact
        if (!email.getText().contains("@") || email.getText().trim().isEmpty()) { showError(errEmail, email, true, getFieldError(email)); valid = false; }
        if (!phone.getText().matches("\\d{10}")) { showError(errPhone, phone, true, getFieldError(phone)); valid = false; }
        // Address
        if (address.getText().trim().isEmpty()) { showError(errAddress, address, true, getFieldError(address)); valid = false; }
        if (city.getText().trim().isEmpty())    { showError(errCity, city, true, getFieldError(city)); valid = false; }
        if (country.getText().trim().isEmpty()) { showError(errCountry, country, true, getFieldError(country)); valid = false; }
        // Role
        if (role.getValue() == null) { showError(errRole, role, true, "Role is required"); valid = false; }
        // Citizen fields
        if (citizenBox.isVisible()) {
            if (houseType.getValue() == null) { showError(errHouseType, houseType, true, "House type is required"); valid = false; }
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
            boolean isCitizen = "citizen".equalsIgnoreCase(n);
            citizenBox.setVisible(isCitizen); citizenBox.setManaged(isCitizen);
        });
        houseType.valueProperty().addListener((obs, o, n) -> {
            if ("Apartment".equalsIgnoreCase(n))  floor.setPromptText("Floor number");
            else if ("House".equalsIgnoreCase(n)) floor.setPromptText("Number of floors in the house");
        });
    }

    // ==========================================
    // SOUMISSION DU FORMULAIRE
    // ==========================================
    private void handleRegister() {
        if (controller == null) return;
        if (!validateAll()) { System.out.println("[Register] FORMULAIRE INVALIDE"); return; }

        String pwd     = password.isVisible() ? password.getText() : visiblePassword.getText();
        String confirm = confirmPassword.isVisible() ? confirmPassword.getText() : visibleConfirmPassword.getText();
        if (!controller.canRegister(pwd, confirm)) {
            System.out.println("[Register] FORMULAIRE INVALIDE (Mots de passe incorrects)");
            applyPasswordColorStyle(confirmPassword, confirm, false);
            return;
        }

        int size = 0, floorNum = 0;
        try { size     = householdSize.getText() != null && !householdSize.getText().isEmpty() ? Integer.parseInt(householdSize.getText()) : 0; }
        catch (NumberFormatException e) { System.out.println("[Register] Erreur : Taille du foyer invalide"); }
        try { floorNum = floor.getText() != null && !floor.getText().isEmpty() ? Integer.parseInt(floor.getText()) : 0; }
        catch (NumberFormatException e) { System.out.println("[Register] Erreur : Étage invalide"); }

        boolean writeSuccess = controller.handleUserRegistration(
            firstName.getText(), lastName.getText(), birthDate.getValue(),
            email.getText(), phone.getText(), pwd,
            address.getText(), city.getText(), country.getText(),
            houseType.getValue(), floorNum, 48.85, 2.35, role.getValue(),
            size, pets.isSelected(), medicalNeeds.getText(),
            emergencyContact.getText(), pmrCheckBox.isSelected());

        if (writeSuccess) { System.out.println("[Register] INSCRIPTION RÉUSSIE ET ENREGISTRÉE !"); Main.showWelcomeView(); }
        else              { System.out.println("[Register] L'inscription a échoué (Email déjà utilisé ou erreur JSON)"); }
    }

    // ==========================================
    // GETTER
    // ==========================================
    public Button getBackButton() { return backButton; }
}