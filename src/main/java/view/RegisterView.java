package view;

import app.Main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class RegisterView extends StackPane {

    public RegisterView() {
        buildUI();
    }

    private void buildUI() {

        setStyle("-fx-background-color: linear-gradient(to bottom, #F7FBFF, #DCEEFF);");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox card = new VBox(11);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(380);

        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-border-color: #BFD4EA;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 18, 0, 0, 5);"
        );

        Button backButton = new Button("←");
        backButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-font-size: 20px;" +
                "-fx-text-fill: #1565C0;" +
                "-fx-cursor: hand;"
        );
        backButton.setOnAction(e -> Main.showWelcomeView());

        HBox topBar = new HBox(backButton);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Create an account");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#0A3D91"));

        Label subtitle = new Label("Fill in your emergency profile");
        subtitle.setFont(Font.font("Arial", 12));
        subtitle.setTextFill(Color.web("#6B7A90"));

        TextField fullNameField = createInput("• Full name");
        TextField emailField = createInput("• Email");
        TextField phoneField = createInput("• Phone number");
        limitToDigits(phoneField, 10);

        PasswordField passwordField = createPasswordInput("• Password");
        PasswordField confirmPasswordField = createPasswordInput("• Confirm password");

        TextField addressField = createInput("• Address");
        TextField cityField = createInput("• City");
        TextField countryField = createInput("• Country");

        ComboBox<String> housingTypeBox = createComboBox("• Housing type");
        housingTypeBox.getItems().addAll("House", "Apartment");

        TextField floorField = createInput("• Floor / apartment");

        TextField coordinatesField = createInput("• GPS coordinates");
        coordinatesField.setEditable(false);

        Button locateButton = new Button("• USE MY LOCATION");
        locateButton.setPrefWidth(300);
        locateButton.setPrefHeight(38);
        locateButton.setStyle(
                "-fx-background-color: #EAF4FF;" +
                "-fx-text-fill: #1565C0;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: #B8D8F5;" +
                "-fx-cursor: hand;"
        );

        locateButton.setOnAction(e -> {
            try {
                URL url = new URL("http://ip-api.com/json/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject data = new JSONObject(response.toString());

                cityField.setText(data.getString("city"));
                countryField.setText(data.getString("country"));

                double lat = data.getDouble("lat");
                double lon = data.getDouble("lon");

                coordinatesField.setText(lat + ", " + lon);

            } catch (Exception ex) {
                coordinatesField.setText("Location unavailable");
                ex.printStackTrace();
            }
        });

        TextField emergencyContactField = createInput("• Emergency contact");
        limitToDigits(emergencyContactField, 10);

        ComboBox<String> roleBox = createComboBox("• Role");
        roleBox.getItems().addAll("Citizen", "Rescue Team");

        VBox citizenBox = new VBox(10);
        citizenBox.setAlignment(Pos.CENTER);
        citizenBox.setVisible(false);
        citizenBox.setManaged(false);

        ComboBox<String> mobilityBox = createComboBox("• Mobility status");
        mobilityBox.getItems().addAll(
                "Standard",
                "Reduced mobility",
                "Elderly",
                "Child"
        );

        TextField householdField = createInput("• Number of people in household");
        limitToDigits(householdField, 2);

        ComboBox<String> petsBox = createComboBox("• Pets present?");
        petsBox.getItems().addAll("No", "Yes");

        TextField petsNumberField = createInput("• Number of pets");
        limitToDigits(petsNumberField, 2);
        petsNumberField.setVisible(false);
        petsNumberField.setManaged(false);

        ComboBox<String> medicalNeedsBox = createComboBox("• Medical needs?");
        medicalNeedsBox.getItems().addAll("No", "Yes");

        ComboBox<String> medicalTypeBox = createComboBox("• Medical need type");
        medicalTypeBox.getItems().addAll(
                "Medication",
                "Wheelchair",
                "Oxygen",
                "Other"
        );
        medicalTypeBox.setVisible(false);
        medicalTypeBox.setManaged(false);

        petsBox.setOnAction(e -> {
            boolean hasPets = "Yes".equals(petsBox.getValue());
            petsNumberField.setVisible(hasPets);
            petsNumberField.setManaged(hasPets);
            if (!hasPets) {
                petsNumberField.clear();
            }
        });

        medicalNeedsBox.setOnAction(e -> {
            boolean hasMedicalNeeds = "Yes".equals(medicalNeedsBox.getValue());
            medicalTypeBox.setVisible(hasMedicalNeeds);
            medicalTypeBox.setManaged(hasMedicalNeeds);
            if (!hasMedicalNeeds) {
                medicalTypeBox.setValue(null);
            }
        });

        citizenBox.getChildren().addAll(
                mobilityBox,
                householdField,
                petsBox,
                petsNumberField,
                medicalNeedsBox,
                medicalTypeBox
        );

        roleBox.setOnAction(e -> {
            boolean isCitizen = "Citizen".equals(roleBox.getValue());
            citizenBox.setVisible(isCitizen);
            citizenBox.setManaged(isCitizen);
        });

        Label messageLabel = new Label();
        messageLabel.setFont(Font.font("Arial", 11));

        addPasswordMatchValidation(passwordField, confirmPasswordField, messageLabel);
        addEmailValidation(emailField, messageLabel);

        Button registerButton = new Button("REGISTER");
        registerButton.setPrefWidth(300);
        registerButton.setPrefHeight(42);
        registerButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #0A4EA3, #1976D2);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        );

        setNextOnEnter(fullNameField, emailField);
        setNextOnEnter(emailField, phoneField);
        setNextOnEnter(phoneField, passwordField);
        setNextOnEnter(passwordField, confirmPasswordField);
        setNextOnEnter(confirmPasswordField, addressField);
        setNextOnEnter(addressField, cityField);
        setNextOnEnter(cityField, countryField);
        setNextOnEnter(countryField, housingTypeBox);
        setNextOnEnter(floorField, coordinatesField);
        setNextOnEnter(emergencyContactField, roleBox);
        setNextOnEnter(householdField, petsBox);
        setNextOnEnter(petsNumberField, medicalNeedsBox);

        registerButton.setOnAction(e -> {

            if (fullNameField.getText().isEmpty()
                    || emailField.getText().isEmpty()
                    || phoneField.getText().isEmpty()
                    || passwordField.getText().isEmpty()
                    || confirmPasswordField.getText().isEmpty()
                    || addressField.getText().isEmpty()
                    || cityField.getText().isEmpty()
                    || countryField.getText().isEmpty()
                    || housingTypeBox.getValue() == null
                    || emergencyContactField.getText().isEmpty()
                    || roleBox.getValue() == null) {

                showError(messageLabel, "Please fill all required fields.");
                return;
            }

            if (!isValidEmail(emailField.getText())) {
                showError(messageLabel, "Please enter a valid email address.");
                emailField.setStyle(errorStyle());
                return;
            }

            if (phoneField.getText().length() != 10) {
                showError(messageLabel, "Phone number must contain 10 digits.");
                phoneField.setStyle(errorStyle());
                return;
            }

            if (emergencyContactField.getText().length() != 10) {
                showError(messageLabel, "Emergency contact must contain 10 digits.");
                emergencyContactField.setStyle(errorStyle());
                return;
            }

            if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                showError(messageLabel, "Passwords do not match.");
                confirmPasswordField.setStyle(errorStyle());
                return;
            }

            if ("Citizen".equals(roleBox.getValue())) {
                if (mobilityBox.getValue() == null
                        || householdField.getText().isEmpty()
                        || petsBox.getValue() == null
                        || medicalNeedsBox.getValue() == null) {

                    showError(messageLabel, "Please complete the citizen profile.");
                    return;
                }

                if (Integer.parseInt(householdField.getText()) <= 0) {
                    showError(messageLabel, "Household size must be at least 1.");
                    return;
                }

                if ("Yes".equals(petsBox.getValue())) {
                    if (petsNumberField.getText().isEmpty()) {
                        showError(messageLabel, "Please enter the number of pets.");
                        return;
                    }

                    if (Integer.parseInt(petsNumberField.getText()) <= 0) {
                        showError(messageLabel, "Number of pets must be at least 1.");
                        return;
                    }
                }

                if ("Yes".equals(medicalNeedsBox.getValue()) && medicalTypeBox.getValue() == null) {
                    showError(messageLabel, "Please select the medical need type.");
                    return;
                }
            }

            messageLabel.setText("Account created successfully!");
            messageLabel.setTextFill(Color.GREEN);
        });

        Label alreadyLabel = new Label("Already have an account?");
        alreadyLabel.setTextFill(Color.web("#6B7A90"));
        alreadyLabel.setFont(Font.font("Arial", 12));

        Hyperlink loginLink = new Hyperlink("Log in");
        loginLink.setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold; -fx-font-size: 12px;");
        loginLink.setOnAction(e -> Main.showLoginView());

        HBox bottomText = new HBox(4, alreadyLabel, loginLink);
        bottomText.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                topBar,
                title,
                subtitle,
                fullNameField,
                emailField,
                phoneField,
                passwordField,
                confirmPasswordField,
                addressField,
                cityField,
                countryField,
                housingTypeBox,
                floorField,
                locateButton,
                coordinatesField,
                emergencyContactField,
                roleBox,
                citizenBox,
                messageLabel,
                registerButton,
                bottomText
        );

        StackPane wrapper = new StackPane(card);
        wrapper.setPadding(new Insets(25));

        scrollPane.setContent(wrapper);

        getChildren().add(scrollPane);
    }

    private TextField createInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(300);
        field.setPrefHeight(36);
        field.setStyle(inputStyle());
        return field;
    }

    private PasswordField createPasswordInput(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefWidth(300);
        field.setPrefHeight(36);
        field.setStyle(inputStyle());
        return field;
    }

    private ComboBox<String> createComboBox(String prompt) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPromptText(prompt);
        comboBox.setPrefWidth(300);
        comboBox.setPrefHeight(36);
        comboBox.setStyle(inputStyle());
        return comboBox;
    }

    private void limitToDigits(TextField field, int maxLength) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("[^\\d]", ""));
            }

            if (field.getText().length() > maxLength) {
                field.setText(field.getText().substring(0, maxLength));
            }

            field.setStyle(inputStyle());
        });
    }

    private void setNextOnEnter(Control current, Control next) {
        current.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    next.requestFocus();
                    event.consume();
                    break;
                default:
                    break;
            }
        });
    }

    private void addPasswordMatchValidation(
            PasswordField passwordField,
            PasswordField confirmPasswordField,
            Label messageLabel
    ) {
        confirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> {
            validatePasswordsLive(passwordField, confirmPasswordField, messageLabel);
        });

        passwordField.textProperty().addListener((obs, oldValue, newValue) -> {
            validatePasswordsLive(passwordField, confirmPasswordField, messageLabel);
        });
    }

    private void validatePasswordsLive(
            PasswordField passwordField,
            PasswordField confirmPasswordField,
            Label messageLabel
    ) {
        if (confirmPasswordField.getText().isEmpty()) {
            confirmPasswordField.setStyle(inputStyle());
            return;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            confirmPasswordField.setStyle(errorStyle());
            messageLabel.setText("Passwords do not match.");
            messageLabel.setTextFill(Color.RED);
        } else {
            confirmPasswordField.setStyle(inputStyle());
            messageLabel.setText("");
        }
    }

    private void addEmailValidation(TextField emailField, Label messageLabel) {
        emailField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !emailField.getText().isEmpty()) {
                if (!isValidEmail(emailField.getText())) {
                    emailField.setStyle(errorStyle());
                    showError(messageLabel, "Please enter a valid email address.");
                } else {
                    emailField.setStyle(inputStyle());
                    messageLabel.setText("");
                }
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showError(Label messageLabel, String message) {
        messageLabel.setText(message);
        messageLabel.setTextFill(Color.RED);
    }

    private String inputStyle() {
        return "-fx-background-color: white;" +
                "-fx-border-color: #DDE8F3;" +
                "-fx-border-width: 1.2;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 0 10 0 10;";
    }

    private String errorStyle() {
        return "-fx-background-color: white;" +
                "-fx-border-color: #E53935;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 0 10 0 10;";
    }
}