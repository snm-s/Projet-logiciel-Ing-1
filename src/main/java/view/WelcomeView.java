package view;

import app.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.io.File;

public class WelcomeView extends StackPane {

    public WelcomeView() {
        buildUI();
    }

    private void buildUI() {
        // Dimensions idéales de la fenêtre
        this.setPrefSize(1100, 650);

        // ==========================================
        // 1. IMAGE D'ARRIÈRE-PLAN SOMBRE ET IMMERSIVE
        // ==========================================
        Region backgroundFiller = new Region();
        String imageName = "WhatsApp Image 2026-05-30 at 22.24.27.jpeg";
        File imageFile = new File("src/resources/images/" + imageName);

        if (imageFile.exists()) {
            String imageUrl = imageFile.toURI().toString();
            backgroundFiller.setStyle(
                "-fx-background-image: url('" + imageUrl + "');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-size: cover;" +
                "-fx-background-position: center;"
            );
        } else {
            backgroundFiller.setStyle("-fx-background-color: #0b1a30;");
        }

        // Calque d'assombrissement global pour la lisibilité
        Region darkOverlay = new Region();
        darkOverlay.setStyle("-fx-background-color: rgba(11, 26, 48, 0.75);");

        // ==========================================
        // 2. CARTE CENTRÉE (EFFET GLASSMORPHISM)
        // ==========================================
        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER); // ÉCRITURE CENTRÉE
        card.setPadding(new Insets(45));
        card.setMaxWidth(620);
        card.setMaxHeight(520);
        
        // Design vitré sombre qui s'intègre au thème néon
        card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.04);" +
                "-fx-background-radius: 30;" +
                "-fx-border-radius: 30;" +
                "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 30, 0, 0, 10);"
        );

        // --- LOGO ADAPTÉ AU THÈME BLANC/BLEU ÉLECTRIQUE ---
        StackPane logo = createLogo();

        // --- TEXTES ---
        Text title = new Text("FLOOD SIMULATION");
        title.setFont(Font.font("System", FontWeight.BOLD, 42));
        title.setFill(Color.WHITE);

        Text subtitle = new Text("Agents & Graphs Emergency System");
        subtitle.setFont(Font.font("System", FontWeight.LIGHT, 20));
        subtitle.setFill(Color.web("#a0b2ce"));

        VBox texts = new VBox(8, title, subtitle);
        texts.setAlignment(Pos.CENTER); // Centrage horizontal des textes

        // --- BOUTONS INTERACTIFS ---
        Button loginButton = createMainButton("LOG IN");
        Button signUpButton = createSecondaryButton("SIGN UP");

        loginButton.setOnAction(e -> Main.showLoginView());
        signUpButton.setOnAction(e -> Main.showRegisterView());

        VBox buttons = new VBox(18, loginButton, signUpButton);
        buttons.setAlignment(Pos.CENTER); // Centrage horizontal des boutons

        // Assemblage des composants de la carte
        card.getChildren().addAll(logo, texts, buttons);

        // ==========================================
        // 3. LES VAGUES GRAPHIQUES (CONSERVÉES DU COMPORTEMENT PRÉCÉDENT)
        // ==========================================
        Pane waves = createWaves();

        // Assemblage final sur le StackPane principal
        this.getChildren().addAll(backgroundFiller, darkOverlay, waves, card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    private StackPane createLogo() {
        StackPane logoPane = new StackPane();
        logoPane.setPrefSize(140, 120);

        Rectangle houseBody = new Rectangle(58, 45);
        houseBody.setArcWidth(8);
        houseBody.setArcHeight(8);
        houseBody.setFill(Color.TRANSPARENT);
        houseBody.setStroke(Color.WHITE); // Changé en blanc pour ressortir sur le fond sombre
        houseBody.setStrokeWidth(4);
        houseBody.setTranslateY(10);

        Polygon roof = new Polygon();
        roof.getPoints().addAll(
                20.0, 45.0,
                70.0, 5.0,
                120.0, 45.0
        );
        roof.setFill(Color.TRANSPARENT);
        roof.setStroke(Color.WHITE);
        roof.setStrokeWidth(4);

        Rectangle door = new Rectangle(16, 24);
        door.setArcWidth(4);
        door.setArcHeight(4);
        door.setFill(Color.TRANSPARENT);
        door.setStroke(Color.WHITE);
        door.setStrokeWidth(3);
        door.setTranslateY(20);

        Path wave1 = new Path();
        wave1.getElements().add(new MoveTo(10, 70));
        wave1.getElements().add(new CubicCurveTo(35, 55, 55, 85, 80, 70));
        wave1.getElements().add(new CubicCurveTo(100, 58, 120, 80, 135, 68));
        wave1.setStroke(Color.web("#0e73eb")); // Vague en bleu néon électrique
        wave1.setStrokeWidth(5);
        wave1.setFill(Color.TRANSPARENT);

        logoPane.getChildren().addAll(houseBody, roof, door, wave1);
        return logoPane;
    }

    private Button createMainButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(320);
        button.setPrefHeight(50);
        button.setFont(Font.font("System", FontWeight.BOLD, 16));
        button.setTextFill(Color.WHITE);
        button.setStyle(
                "-fx-background-color: #0b5cbf;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #0e73eb; -fx-background-radius: 12; -fx-cursor: hand;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #0b5cbf; -fx-background-radius: 12;"));

        return button;
    }

    private Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(320);
        button.setPrefHeight(50);
        button.setFont(Font.font("System", FontWeight.BOLD, 16));
        button.setTextFill(Color.web("#a0b2ce"));
        button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: rgba(255, 255, 255, 0.25);" +
                "-fx-border-width: 2;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                "-fx-border-color: white;" +
                "-fx-border-width: 2;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-text-fill: white;" +
                "-fx-cursor: hand;"
            );
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: rgba(255, 255, 255, 0.25);" +
                "-fx-border-width: 2;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-text-fill: #a0b2ce;"
            );
        });

        return button;
    }

    private Pane createWaves() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);

        // Vague du fond - Bleu Nuit de transition
        Path waveBack = new Path();
        waveBack.getElements().add(new MoveTo(0, 520));
        waveBack.getElements().add(new CubicCurveTo(250, 430, 450, 620, 700, 530));
        waveBack.getElements().add(new CubicCurveTo(950, 450, 1150, 620, 1400, 540));
        waveBack.getElements().add(new LineTo(1400, 900));
        waveBack.getElements().add(new LineTo(0, 900));
        waveBack.getElements().add(new ClosePath());
        waveBack.setFill(Color.web("#081326"));

        // Vague du milieu - Bleu Moyen Profond
        Path waveMiddle = new Path();
        waveMiddle.getElements().add(new MoveTo(0, 590));
        waveMiddle.getElements().add(new CubicCurveTo(220, 500, 500, 690, 760, 590));
        waveMiddle.getElements().add(new CubicCurveTo(980, 510, 1200, 700, 1400, 600));
        waveMiddle.getElements().add(new LineTo(1400, 900));
        waveMiddle.getElements().add(new LineTo(0, 900));
        waveMiddle.getElements().add(new ClosePath());
        waveMiddle.setFill(Color.web("#062347"));

        // Vague de devant - Bleu Électrique Atténué
        Path waveFront = new Path();
        waveFront.getElements().add(new MoveTo(0, 650));
        waveFront.getElements().add(new CubicCurveTo(250, 560, 500, 760, 800, 650));
        waveFront.getElements().add(new CubicCurveTo(1050, 560, 1250, 760, 1450, 640));
        waveFront.getElements().add(new LineTo(1450, 900));
        waveFront.getElements().add(new LineTo(0, 900));
        waveFront.getElements().add(new ClosePath());
        waveFront.setFill(Color.web("#0b4da0"));

        pane.getChildren().addAll(waveBack, waveMiddle, waveFront);
        return pane;
    }
}