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

public class RegisterView extends StackPane {

    private ScrollPane scrollPane = new ScrollPane();
    private VBox root = new VBox(20);
    private RegisterController controller; 
    private Button backButton; 

    // Identity
    private TextField firstName = new TextField();
    private TextField lastName = new TextField();
    private DatePicker birthDate = new DatePicker();

    // Contact-
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
        
        // 4. HEADER (Logo + Titre)
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
        Text brandTitle = new Text("Inondation"); // Renommé
        brandTitle.setFont(Font.font("System", FontWeight.BOLD, 30));
        brandTitle.setFill(Color.WHITE);
        Text brandSubtitle = new Text("Simulation & emergency management"); // Renommé
        brandSubtitle.setFont(Font.font("System", FontWeight.LIGHT, 12));
        brandSubtitle.setFill(Color.web("#a0b2ce"));
        brandTitles.getChildren().addAll(brandTitle, brandSubtitle);
        brandHeader.getChildren().addAll(logoContainer, brandTitles);
        root.getChildren().add(brandHeader);

        // 5. TITRE DE LA PAGE (Sign up)
        Text pageTitle = new Text("Sign up"); // Renommé
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
        pageTitle.setFill(Color.WHITE);

        Text pageSubtitle = new Text("Join the flood emergency and simulation system"); // Renommé
        pageSubtitle.setFont(Font.font("System", FontWeight.LIGHT, 12));
        pageSubtitle.setFill(Color.web("#a0b2ce"));
        
        VBox headerBox = new VBox(5, pageTitle, pageSubtitle);
        headerBox.setPadding(new Insets(20, 0, 10, 0));
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


        // --- PASSWORD (Version unique et synchronisée) ---
        root.getChildren().add(createSectionLabel("Password"));

        // Liaison bidirectionnelle
        password.textProperty().bindBidirectional(visiblePassword.textProperty());
        confirmPassword.textProperty().bindBidirectional(visibleConfirmPassword.textProperty());

        // 1. Ajout des champs avec l'œil (ces deux-là suffisent)
        root.getChildren().addAll(
            createPasswordFieldWithEye(password, visiblePassword, "Password"),
            createPasswordFieldWithEye(confirmPassword, visibleConfirmPassword, "Confirm password")
        );

        // 2. Ajout des règles de validation
        VBox rulesBox = new VBox(4, ruleLength, ruleUpper, ruleDigit);
        rulesBox.setPadding(new Insets(0, 0, 10, 5));
        root.getChildren().add(rulesBox);

        // 3. Initialisation des styles des règles (en rouge par défaut)
        applyRuleLabelErrorStyle(ruleLength);
        applyRuleLabelErrorStyle(ruleUpper);
        applyRuleLabelErrorStyle(ruleDigit);

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


        houseType.getItems().addAll("APARTMENT", "HOUSE");
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
                houseType,
                floor,
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
        pf.setPromptText(prompt); tf.setPromptText(prompt);
        pf.setPrefHeight(42); tf.setPrefHeight(42);
        applyTextFieldStyle(pf); applyTextFieldStyle(tf);
        
        // État initial : le TextField est caché, le PasswordField est visible
        tf.setVisible(false); tf.setManaged(false);
        pf.setVisible(true); pf.setManaged(true);
        
        Button btn = new Button("👁");
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-cursor: hand;");
        
        btn.setOnAction(e -> {
            boolean v = tf.isVisible();
            // Inversion de visibilité
            tf.setVisible(!v); tf.setManaged(!v);
            pf.setVisible(v); pf.setManaged(v);
            btn.setText(v ? "👁" : "🔒");
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
        
        // Style de la boîte (fermé)
        combo.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.05);" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-text-fill: white;"
        );

        // Style de l'affichage du choix actuel (le bouton)
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(combo.getPromptText());
                } else {
                    setText(item);
                }
                setTextFill(Color.WHITE);
            }
        });

        // Style de la liste déroulante (le menu qui s'ouvre)
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0b1a30;");
                } else {
                    setText(item);
                    // Fond sombre et texte blanc pour chaque ligne
                    setStyle("-fx-background-color: #0b1a30; -fx-text-fill: white;");
                }
            }
        });
        
        // Pour forcer le fond du menu déroulant (Popup)
        combo.setOnShowing(e -> {
            combo.getScene().getRoot().lookupAll(".combo-box-popup .list-view .list-cell").forEach(n -> {
                n.setStyle("-fx-background-color: #0b1a30; -fx-text-fill: white;");
            });
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