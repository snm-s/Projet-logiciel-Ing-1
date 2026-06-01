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

public class WelcomeView extends StackPane {

    public WelcomeView() {
        buildUI();
    }

    private void buildUI() {

        this.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #f7fbff, #dbeeff);"
        );

        VBox card = new VBox(25);

        card.setAlignment(Pos.CENTER);

        card.setPadding(new Insets(45));

        card.setMaxWidth(620);
        card.setMaxHeight(520);

        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 30;" +
                "-fx-border-radius: 30;" +
                "-fx-border-color: #A8D2FF;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 30, 0, 0, 10);"
        );

        StackPane logo = createLogo();

        Text title = new Text("FLOOD SIMULATION");

        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));

        title.setFill(Color.web("#083B88"));

        Text subtitle = new Text("Agents & Graphs");

        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 24));

        subtitle.setFill(Color.web("#124A8A"));

        Button loginButton = createMainButton("LOG IN");

        Button signUpButton = createSecondaryButton("SIGN UP");

        loginButton.setOnAction(e -> {
            Main.showLoginView();
        });

        signUpButton.setOnAction(e -> {
                Main.showRegisterView();
            });

        VBox texts = new VBox(8, title, subtitle);

        texts.setAlignment(Pos.CENTER);

        VBox buttons = new VBox(18, loginButton, signUpButton);

        buttons.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                logo,
                texts,
                buttons
        );

        Pane waves = createWaves();

        this.getChildren().addAll(waves, card);

        StackPane.setAlignment(card, Pos.CENTER);
    }

    private StackPane createLogo() {

        StackPane logoPane = new StackPane();

        logoPane.setPrefSize(140, 120);

        Rectangle houseBody = new Rectangle(58, 45);

        houseBody.setArcWidth(8);
        houseBody.setArcHeight(8);

        houseBody.setFill(Color.TRANSPARENT);

        houseBody.setStroke(Color.web("#0A4EA3"));

        houseBody.setStrokeWidth(4);

        houseBody.setTranslateY(10);

        Polygon roof = new Polygon();

        roof.getPoints().addAll(
                20.0, 45.0,
                70.0, 5.0,
                120.0, 45.0
        );

        roof.setFill(Color.TRANSPARENT);

        roof.setStroke(Color.web("#0A4EA3"));

        roof.setStrokeWidth(4);

        Rectangle door = new Rectangle(16, 24);

        door.setArcWidth(4);
        door.setArcHeight(4);

        door.setFill(Color.TRANSPARENT);

        door.setStroke(Color.web("#0A4EA3"));

        door.setStrokeWidth(3);

        door.setTranslateY(20);

        Path wave1 = new Path();

        wave1.getElements().add(new MoveTo(10, 70));

        wave1.getElements().add(
                new CubicCurveTo(
                        35, 55,
                        55, 85,
                        80, 70
                )
        );

        wave1.getElements().add(
                new CubicCurveTo(
                        100, 58,
                        120, 80,
                        135, 68
                )
        );

        wave1.setStroke(Color.web("#1E88E5"));

        wave1.setStrokeWidth(5);

        wave1.setFill(Color.TRANSPARENT);

        logoPane.getChildren().addAll(
                houseBody,
                roof,
                door,
                wave1
        );

        return logoPane;
    }

    private Button createMainButton(String text) {

        Button button = new Button(text);

        button.setPrefWidth(320);
        button.setPrefHeight(58);

        button.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        button.setTextFill(Color.WHITE);

        button.setStyle(
                "-fx-background-color: linear-gradient(to right, #0A4EA3, #1976D2);" +
                "-fx-background-radius: 15;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    private Button createSecondaryButton(String text) {

        Button button = new Button(text);

        button.setPrefWidth(320);
        button.setPrefHeight(58);

        button.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        button.setTextFill(Color.web("#0A4EA3"));

        button.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #0A4EA3;" +
                "-fx-border-width: 3;" +
                "-fx-background-radius: 15;" +
                "-fx-border-radius: 15;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    private Pane createWaves() {

        Pane pane = new Pane();

        pane.setMouseTransparent(true);

        Path waveBack = new Path();

        waveBack.getElements().add(new MoveTo(0, 520));

        waveBack.getElements().add(
                new CubicCurveTo(
                        250, 430,
                        450, 620,
                        700, 530
                )
        );

        waveBack.getElements().add(
                new CubicCurveTo(
                        950, 450,
                        1150, 620,
                        1400, 540
                )
        );

        waveBack.getElements().add(new LineTo(1400, 900));

        waveBack.getElements().add(new LineTo(0, 900));

        waveBack.getElements().add(new ClosePath());

        waveBack.setFill(Color.web("#B9DBF8"));

        Path waveMiddle = new Path();

        waveMiddle.getElements().add(new MoveTo(0, 590));

        waveMiddle.getElements().add(
                new CubicCurveTo(
                        220, 500,
                        500, 690,
                        760, 590
                )
        );

        waveMiddle.getElements().add(
                new CubicCurveTo(
                        980, 510,
                        1200, 700,
                        1400, 600
                )
        );

        waveMiddle.getElements().add(new LineTo(1400, 900));

        waveMiddle.getElements().add(new LineTo(0, 900));

        waveMiddle.getElements().add(new ClosePath());

        waveMiddle.setFill(Color.web("#67B3EA"));

        Path waveFront = new Path();

        waveFront.getElements().add(new MoveTo(0, 650));

        waveFront.getElements().add(
                new CubicCurveTo(
                        250, 560,
                        500, 760,
                        800, 650
                )
        );

        waveFront.getElements().add(
                new CubicCurveTo(
                        1050, 560,
                        1250, 760,
                        1450, 640
                )
        );

        waveFront.getElements().add(new LineTo(1450, 900));

        waveFront.getElements().add(new LineTo(0, 900));

        waveFront.getElements().add(new ClosePath());

        waveFront.setFill(Color.web("#118FD6"));

        pane.getChildren().addAll(
                waveBack,
                waveMiddle,
                waveFront
        );

        return pane;
    }
}