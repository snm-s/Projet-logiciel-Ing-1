package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import app.Main;
import controller.RegisterController;

public class RegisterView extends StackPane {

    private ScrollPane scrollPane = new ScrollPane();
    private VBox root = new VBox(20);
    private RegisterController controller; 
    private Button backButton; 

    // Identity
    private TextField firstName = new TextField();
    private TextField lastName = new TextField();
    private DatePicker birthDate = new DatePicker();

    // Contact
    private TextField email = new TextField();
    private TextField phone = new TextField();

    // Password
    private PasswordField password = new PasswordField();
    private PasswordField confirmPassword = new PasswordField();

    private TextField visiblePassword = new TextField();
    private TextField visibleConfirmPassword = new TextField();

    private Label ruleLength = new Label("• At least 8 characters");
    private Label ruleUpper = new Label("• At least 1 uppercase letter");
    private Label ruleDigit = new Label("• At least 1 number");

    // Address
    private TextField address = new TextField();
    private TextField city = new TextField();
    private TextField country = new TextField();

    // House
    private ComboBox<String> houseType = new ComboBox<>();
    private TextField floor = new TextField();

    // GPS
    private Label gpsLabel = new Label("GPS not set");
    private Button gpsButton = new Button("Detect GPS");

    // Role
    private ComboBox<String> role = new ComboBox<>();

    // Citizen section
    private VBox citizenBox = new VBox(15);
    private TextField householdSize = new TextField();
    private CheckBox pets = new CheckBox("Has pets");
    private CheckBox pmrCheckBox = new CheckBox("PMR (Person with reduced mobility)");
    private TextArea medicalNeeds = new TextArea();
    private TextField emergencyContact = new TextField();

    // Register
    private Button registerBtn = new Button("Register");

    public RegisterView() {
        this.setPrefSize(1100, 700);

        // ==========================================
        // 1. FOND DE PAGE SÉCURISÉ
        // ==========================================
        Region backgroundFiller = new Region();
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
                backgroundFiller.setStyle("-fx-background-color: #0b1a30;");
            }
        } catch (Exception e) {
            backgroundFiller.setStyle("-fx-background-color: #0b1a30;");
        }

        // ==========================================
        // 2. CALQUE DE DÉGRADÉ BLEU NUIT (OVERLAY)
        // ==========================================
        Region gradientOverlay = new Region();
        gradientOverlay.setStyle(
            "-fx-background-color: linear-gradient(to right, " +
            "#0b1a30 0%, " +
            "#0b1a30 40%, " +
            "rgba(11, 26, 48, 0.9) 60%, " +
            "rgba(11, 26, 48, 0.3) 85%, " +
            "rgba(11, 26, 48, 0.1) 100%);"
        );

        // ==========================================
        // 3. INITIALISATION ET STYLE DU BOUTON RETOUR
        // ==========================================
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

        // ==========================================
        // 4. MISE EN FORME DU SCROLLPANE ET DU FORMULAIRE
        // ==========================================
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

    private void build() {
        root.getChildren().add(backButton);

        Text titleText = new Text("Create Account");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleText.setFill(Color.WHITE);

        Text subtitleText = new Text("Join the flood emergency and simulation system");
        subtitleText.setFont(Font.font("System", FontWeight.LIGHT, 12));
        subtitleText.setFill(Color.web("#a0b2ce"));
        
        VBox headerBox = new VBox(5, titleText, subtitleText);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        root.getChildren().add(headerBox);

        // --- IDENTITY ---
        root.getChildren().add(createSectionLabel("Identity"));
        firstName.setPromptText("First name"); applyTextFieldStyle(firstName);
        lastName.setPromptText("Last name"); applyTextFieldStyle(lastName);
        birthDate.setPromptText("Birth date"); 
        birthDate.setMaxWidth(Double.MAX_VALUE);
        birthDate.getEditor().setStyle("-fx-text-fill: white; -fx-background-color: transparent;");
        birthDate.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.25); -fx-border-radius: 6; -fx-background-radius: 6; -fx-height: 45;");

        root.getChildren().addAll(firstName, lastName, birthDate);

        // --- CONTACT ---
        root.getChildren().add(createSectionLabel("Contact"));
        email.setPromptText("Email"); applyTextFieldStyle(email);
        phone.setPromptText("Phone number"); applyTextFieldStyle(phone);
        root.getChildren().addAll(email, phone);

        // --- PASSWORD ---
        root.getChildren().add(createSectionLabel("Password"));
        
        password.setPrefHeight(42);
        password.setFont(Font.font("System", 13));
        password.setPromptText("Password");
        
        confirmPassword.setPrefHeight(42);
        confirmPassword.setFont(Font.font("System", 13));
        confirmPassword.setPromptText("Confirm password");
        

        // --- PASSWORD ---
        root.getChildren().add(createSectionLabel("Password"));
        visiblePassword.setVisible(false); visiblePassword.setManaged(false);
        visibleConfirmPassword.setVisible(false); visibleConfirmPassword.setManaged(false);
        
        StackPane pStack = createPasswordFieldWithEye(password, visiblePassword, "Password");
        StackPane cStack = createPasswordFieldWithEye(confirmPassword, visibleConfirmPassword, "Confirm password");
        
        VBox rulesBox = new VBox(4, ruleLength, ruleUpper, ruleDigit);
        root.getChildren().addAll(pStack, cStack, rulesBox);



        // Initialisation à l'état neutre (évite d'avoir du rouge dès le début)
        applyPasswordColorStyle(password, "", false);
        applyPasswordColorStyle(confirmPassword, "", false);
        
        // Règles indicatives en rouge par défaut
        applyRuleLabelErrorStyle(ruleLength);
        applyRuleLabelErrorStyle(ruleUpper);
        applyRuleLabelErrorStyle(ruleDigit);
        
        VBox rulesBox1 = new VBox(4, ruleLength, ruleUpper, ruleDigit);
        rulesBox1.setPadding(new Insets(0, 0, 0, 5));
        root.getChildren().addAll(password, confirmPassword, rulesBox1);

        // --- ADDRESS ---
        root.getChildren().add(createSectionLabel("Address"));
        address.setPromptText("Address"); applyTextFieldStyle(address);
        city.setPromptText("City"); applyTextFieldStyle(city);
        country.setPromptText("Country"); applyTextFieldStyle(country);
        root.getChildren().addAll(address, city, country);



        // --- LOCATION ---
        root.getChildren().add(createSectionLabel("Location"));
        gpsButton.setMaxWidth(Double.MAX_VALUE);
        gpsButton.setPrefHeight(40);
        gpsButton.setStyle("-fx-background-color: transparent; -fx-border-color: #0b5cbf; -fx-border-radius: 6; -fx-text-fill: #0b5cbf; -fx-font-weight: bold; -fx-cursor: hand;");
        gpsButton.setOnMouseEntered(e -> gpsButton.setStyle("-fx-background-color: rgba(11, 92, 191, 0.1); -fx-border-color: #0b5cbf; -fx-border-radius: 6; -fx-text-fill: #0b5cbf; -fx-cursor: hand;"));
        gpsButton.setOnMouseExited(e -> gpsButton.setStyle("-fx-background-color: transparent; -fx-border-color: #0b5cbf; -fx-border-radius: 6; -fx-text-fill: #0b5cbf;"));
        
        gpsLabel.setFont(Font.font("System", 13));
        gpsLabel.setTextFill(Color.web("#a0b2ce"));
        
        gpsButton.setOnAction(e -> gpsLabel.setText("📍 Lat: 48.85 | Lng: 2.35"));
        root.getChildren().addAll(gpsButton, gpsLabel);

        // --- ROLE ---
        root.getChildren().add(createSectionLabel("Role"));
        role.getItems().addAll("citizen", "rescue");
        role.setPromptText("Select your role");
        applyComboBoxStyle(role);
        root.getChildren().add(role);

        // --- CITIZEN BOX SECTION ---


        houseType.getItems().addAll("APPARTEMENT", "HOUSE");
        houseType.setPromptText("Select House Type");
        applyComboBoxStyle(houseType);

        floor.setPromptText("Floor"); 
        applyTextFieldStyle(floor);

        householdSize.setPromptText("Household size"); applyTextFieldStyle(householdSize);
        medicalNeeds.setPromptText("Medical needs");
        medicalNeeds.setPrefHeight(80);
        medicalNeeds.setStyle(
            "-fx-control-inner-background: rgba(255, 255, 255, 0.05);" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #a0b2ce;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 6;"
        );
        emergencyContact.setPromptText("Emergency contact"); applyTextFieldStyle(emergencyContact);
        pets.setFont(Font.font("System", 13));
        pets.setTextFill(Color.WHITE);

        citizenBox.getChildren().clear(); 
        citizenBox.getChildren().addAll(
                createSectionLabel("Citizen Information"),
                householdSize,
                pets,
                pmrCheckBox,
                medicalNeeds,
                createSectionLabel("Emergency Contact"),
                emergencyContact
        );

        citizenBox.setVisible(false);
        citizenBox.setManaged(false);
        root.getChildren().add(citizenBox);

        // --- REGISTER SUBMIT BUTTON ---
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setPrefHeight(45);
        registerBtn.setFont(Font.font("System", FontWeight.BOLD, 15));
        registerBtn.setTextFill(Color.WHITE);
        registerBtn.setStyle("-fx-background-color: #0b5cbf; -fx-background-radius: 6; -fx-cursor: hand;");
        VBox.setMargin(registerBtn, new Insets(15, 0, 0, 0));

        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle("-fx-background-color: #0e73eb; -fx-background-radius: 6; -fx-cursor: hand;"));
        registerBtn.setOnMouseExited(e -> registerBtn.setStyle("-fx-background-color: #0b5cbf; -fx-background-radius: 6;"));
        registerBtn.setOnAction(e -> handleRegister());
        
        root.getChildren().add(registerBtn);
    }

    public Button getBackButton() {
        return backButton;
    }

    
    private StackPane createPasswordFieldWithEye(PasswordField pf, TextField tf, String prompt) {
        pf.setPromptText(prompt); pf.setPrefHeight(42);
        tf.setPromptText(prompt); tf.setPrefHeight(42);
        applyTextFieldStyle(pf); applyTextFieldStyle(tf);
        Button btn = new Button("👁");
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            boolean v = tf.isVisible();
            tf.setVisible(!v); tf.setManaged(!v); pf.setVisible(v); pf.setManaged(v);
            if (!v) { tf.setText(pf.getText()); btn.setText("🔒"); } else { pf.setText(tf.getText()); btn.setText("👁"); }
        });
        StackPane s = new StackPane(pf, tf, btn);
        StackPane.setAlignment(btn, Pos.CENTER_RIGHT);
        StackPane.setMargin(btn, new Insets(0, 10, 0, 0));
        return s;
    }
    
    public void setController(RegisterController controller) {
        this.controller = controller;
        setupPasswordLiveCheck(); 
    }

    // ==========================================
    // MÉTHODES UTILITAIRES DE STYLISATION
    // ==========================================
    
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.SEMI_BOLD, 15));
        label.setTextFill(Color.WHITE);
        label.setPadding(new Insets(10, 0, 2, 0));
        return label;
    }

    private void applyTextFieldStyle(TextField textField) {
        textField.setPrefHeight(42);
        textField.setFont(Font.font("System", 13));
        textField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.05);" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #a0b2ce;"
        );

        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                textField.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.08);" +
                    "-fx-border-color: #0b5cbf;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #a0b2ce;"
                );
            } else {
                applyTextFieldStyle(textField);
            }
        });
    }

    // Gestion centralisée de la couleur : supprime l'impact du focus natif
    private void applyPasswordColorStyle(PasswordField field, String text, boolean isValid) {
        if (text == null || text.isEmpty()) {
            field.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                "-fx-border-color: rgba(255, 255, 255, 0.25);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #a0b2ce;"
            );
            return;
        }

        if (isValid) {
            field.setStyle(
                "-fx-background-color: rgba(46, 204, 113, 0.1);" +
                "-fx-border-color: #2ecc71;" + 
                "-fx-text-box-border: transparent;" + 
                "-fx-focus-color: transparent;" + 
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #a0b2ce;"
            );
        } else {
            field.setStyle(
                "-fx-background-color: rgba(231, 76, 60, 0.1);" +
                "-fx-border-color: #e74c3c;" + 
                "-fx-text-box-border: transparent;" + 
                "-fx-focus-color: transparent;" + 
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #a0b2ce;"
            );
        }
    }

    private void applyComboBoxStyle(ComboBox<String> combo) {
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setPrefHeight(42);
        combo.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.05);" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
        combo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: black;");
                }
            }
        });
    }

    private void applyRuleLabelSuccessStyle(Label label) {
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#2ecc71")); 
    }

    private void applyRuleLabelErrorStyle(Label label) {
        label.setFont(Font.font("System", FontWeight.NORMAL, 12));
        label.setTextFill(Color.web("#e74c3c")); 
    }

    // ==========================================
    // LOGIQUE DE COMPORTEMENT DYNAMIQUE (MDP)
    // ==========================================

    private void setupPasswordLiveCheck() {
        if (controller == null) return;

        // Fonction de validation commune pour éviter la duplication
        Runnable validate = () -> {
            // On récupère le texte selon le champ actif
            String pwd = password.isVisible() ? password.getText() : visiblePassword.getText();
            String confirm = confirmPassword.isVisible() ? confirmPassword.getText() : visibleConfirmPassword.getText();

            boolean length = controller.validatePasswordLength(pwd);
            boolean upper = controller.validatePasswordUpper(pwd);
            boolean digit = controller.validatePasswordDigit(pwd);

            // Mise à jour visuelle des labels
            ruleLength.setTextFill(length ? Color.web("#2ecc71") : Color.web("#e74c3c"));
            ruleUpper.setTextFill(upper ? Color.web("#2ecc71") : Color.web("#e74c3c"));
            ruleDigit.setTextFill(digit ? Color.web("#2ecc71") : Color.web("#e74c3c"));

            boolean passwordValid = (length && upper && digit);
            
            // Appliquer la couleur au champ actif
            applyPasswordColorStyle(password, pwd, passwordValid);
            
            // Validation de la confirmation
            boolean match = controller.passwordsMatch(pwd, confirm);
            applyPasswordColorStyle(confirmPassword, confirm, (match && passwordValid));
        };

        // On attache la même logique aux 4 champs possibles
        password.textProperty().addListener((o, oldV, n) -> validate.run());
        visiblePassword.textProperty().addListener((o, oldV, n) -> validate.run());
        confirmPassword.textProperty().addListener((o, oldV, n) -> validate.run());
        visibleConfirmPassword.textProperty().addListener((o, oldV, n) -> validate.run());
    }

    private void triggerConfirmPasswordCheck(String confirmValue) {
        if (controller == null) return;

        String mainPwd = password.getText();
        boolean match = controller.passwordsMatch(mainPwd, confirmValue);
        
        boolean complexOk = controller.validatePasswordLength(mainPwd) 
                            && controller.validatePasswordUpper(mainPwd) 
                            && controller.validatePasswordDigit(mainPwd);

        // Bascule de rouge à vert instantanément
        applyPasswordColorStyle(confirmPassword, confirmValue, (match && complexOk));
    }

    private void setupRoleVisibility() {
        role.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isCitizen = "citizen".equalsIgnoreCase(newV);
            
            // Afficher les détails citoyen
            citizenBox.setVisible(isCitizen);
            citizenBox.setManaged(isCitizen);
        });

        // Adapter le label du champ "floor" selon le type de logement
        houseType.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Apartment".equalsIgnoreCase(newV)) {
                floor.setPromptText("Floor number");
            } else if ("House".equalsIgnoreCase(newV)) {
                floor.setPromptText("Number of floors in the house");
            }
        });
    }

    private void handleRegister() {
        if (controller == null) return;
        
        String pwd = password.isVisible() ? password.getText() : visiblePassword.getText();
        String confirm = confirmPassword.isVisible() ? confirmPassword.getText() : visibleConfirmPassword.getText();

        // 1. On vérifie d'abord si les mots de passe respectent les règles
        boolean ok = controller.canRegister(pwd, confirm);

        if (!ok) {
            System.out.println("[Register] FORMULAIRE INVALIDE (Mots de passe incorrects)");
            applyPasswordColorStyle(confirmPassword, confirm, false);
            return;
        }

        // 2. On convertit les champs numériques de façon sécurisée (pour éviter les bugs si c'est vide)
        int size = 0;
        if (householdSize.getText() != null && !householdSize.getText().isEmpty()) {
            try {
                size = Integer.parseInt(householdSize.getText());
            } catch (NumberFormatException e) {
                System.out.println("[Register] Erreur : Taille du foyer invalide");
            }
        }

        int floorNum = 0;
        if (floor.getText() != null && !floor.getText().isEmpty()) {
            try {
                floorNum = Integer.parseInt(floor.getText());
            } catch (NumberFormatException e) {
                System.out.println("[Register] Erreur : Étage invalide");
            }
        }

        // 3. ON ENVOIE TOUT AU CONTRÔLEUR POUR ÉCRIRE DANS LE JSON
        boolean writeSuccess = controller.handleUserRegistration(
            firstName.getText(),
            lastName.getText(),
            birthDate.getValue(),
            email.getText(),
            phone.getText(),
            pwd,
            address.getText(),
            city.getText(),
            country.getText(),
            houseType.getValue(), // Récupère "Apartment" ou "House"
            floorNum,
            48.85, 2.35,          // Coordonnées GPS (à remplacer par tes vraies variables si besoin)
            role.getValue(),       // Récupère "citizen", "rescue" ou "admin"
            size,
            pets.isSelected(),     // Récupère true ou false si la case est cochée
            medicalNeeds.getText(),
            emergencyContact.getText(),
            pmrCheckBox.isSelected()
        );

        // 4. Si l'écriture a réussi, on change d'écran
        if (writeSuccess) {
            System.out.println("[Register] INSCRIPTION RÉUSSIE ET ENREGISTRÉE !");
            Main.showWelcomeView(); // Redirection automatique
        } else {
            System.out.println("[Register] L'inscription a échoué (Email déjà utilisé ou erreur JSON)");
        }
    }
}