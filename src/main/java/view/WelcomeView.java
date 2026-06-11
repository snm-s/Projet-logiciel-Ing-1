package view;

import app.Main;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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

public class WelcomeView extends StackPane {

    private static final String TEXT = "#f4f7fb";
    private static final String MUTED = "#8493ad";
    private static final String BLUE = "#3b6cff";
    private static final String BLUE_HOVER = "#4d7dff";
    private static final String BLUE_DARK = "#244fd6";

    private Circle glow1;
    private Circle glow2;
    private Circle glow3;
    private Circle glow4;
    private Circle glow5;
    private Circle glow6;

    public WelcomeView() {
        this.setMinSize(0, 0);
        this.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setStyle("-fx-background-color:#060a12;");

        buildUI();
    }

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

        VBox content = buildHeroContent();
        StackPane.setAlignment(content, Pos.CENTER_LEFT);
        StackPane.setMargin(content, new Insets(0, 0, 0, 150));

        getChildren().addAll(background, grid, content);
    }

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

    private VBox buildHeroContent() {
        VBox left = new VBox(0);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMaxWidth(720);

        HBox brand = new HBox(14);
        brand.setAlignment(Pos.CENTER_LEFT);

        StackPane logo = createLogoBox(46);

        VBox brandText = new VBox(1);

        Text brandName = new Text("Inondation");
        brandName.setFont(Font.font("System", FontWeight.BOLD, 23));
        brandName.setFill(Color.web(TEXT));

        Text brandSub = new Text("Simulation & Gestion des urgences");
        brandSub.setFont(Font.font("System", FontWeight.NORMAL, 13));
        brandSub.setFill(Color.web("#536682"));

        brandText.getChildren().addAll(brandName, brandSub);
        brand.getChildren().addAll(logo, brandText);

        Region spacerTop = createSpacer(75);

        Text overline = new Text("PLATEFORME DE SIMULATION");
        overline.setFont(Font.font("System", FontWeight.BOLD, 12));
        overline.setFill(Color.web(BLUE));
        overline.setStyle("-fx-letter-spacing:3px;");

        Text title1 = new Text("Anticiper.");
        title1.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 70));
        title1.setFill(Color.web(TEXT));

        Text title2 = new Text("Alerter.");
        title2.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 70));
        title2.setFill(Color.web("#b8c8ff"));

        Text title3 = new Text("Protéger.");
        title3.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 70));
        title3.setFill(Color.web(BLUE_HOVER));

        VBox titleBox = new VBox(-9, title1, title2, title3);

        Text subtitle = new Text(
                "Une application de simulation d’inondation permettant de visualiser les zones à risque,\n" +
                        "gérer les alertes et coordonner les citoyens avec les équipes de secours."
        );
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 15));
        subtitle.setFill(Color.web(MUTED));
        subtitle.setLineSpacing(5);

        Region accent = new Region();
        accent.setPrefWidth(315);
        accent.setMaxWidth(315);
        accent.setPrefHeight(3);
        accent.setStyle(
                "-fx-background-color:linear-gradient(to right, " + BLUE + ", rgba(59,108,255,0.12));" +
                        "-fx-background-radius:99;"
        );

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button loginButton = createPrimaryButton("Se connecter");
        Button registerButton = createSecondaryButton("Créer un compte");

        loginButton.setOnAction(e -> Main.showLoginView());
        registerButton.setOnAction(e -> Main.showRegisterView());

        buttons.getChildren().addAll(loginButton, registerButton);

        HBox chips = new HBox(10);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.getChildren().addAll(
                createChip("Simulation temps réel"),
                createChip("Alertes"),
                createChip("Secours")
        );

        Region spacerBottom = createSpacer(45);

        Text footer = new Text("Projet ING1  •  Agents & Graphes  •  CY Tech");
        footer.setFont(Font.font("System", FontWeight.NORMAL, 12));
        footer.setFill(Color.web("#314058"));

        left.getChildren().addAll(
                brand,
                spacerTop,
                overline,
                createSpacer(14),
                titleBox,
                createSpacer(22),
                subtitle,
                createSpacer(24),
                accent,
                createSpacer(32),
                buttons,
                createSpacer(22),
                chips,
                spacerBottom,
                footer
        );

        return left;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text + "     →");
        button.setPrefWidth(230);
        button.setPrefHeight(58);
        button.setTextFill(Color.WHITE);
        button.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 16));
        button.setStyle(primaryStyle(false));

        button.setOnMouseEntered(e -> button.setStyle(primaryStyle(true)));
        button.setOnMouseExited(e -> button.setStyle(primaryStyle(false)));

        return button;
    }

    private Button createSecondaryButton(String text) {
        Button button = new Button(text + "     →");
        button.setPrefWidth(245);
        button.setPrefHeight(58);
        button.setTextFill(Color.web("#dce6ff"));
        button.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 16));
        button.setStyle(secondaryStyle(false));

        button.setOnMouseEntered(e -> button.setStyle(secondaryStyle(true)));
        button.setOnMouseExited(e -> button.setStyle(secondaryStyle(false)));

        return button;
    }

    private String primaryStyle(boolean hover) {
        return "-fx-background-color:" +
                (hover
                        ? "linear-gradient(to right, #4778ff, #3157d7)"
                        : "linear-gradient(to right, " + BLUE_DARK + ", " + BLUE + ")") + ";" +
                "-fx-background-radius:16;" +
                "-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian, rgba(59,108,255,0.35), 22, 0, 0, 8);";
    }

    private String secondaryStyle(boolean hover) {
        return "-fx-background-color:" + (hover ? "rgba(59,108,255,0.12)" : "rgba(255,255,255,0.035)") + ";" +
                "-fx-border-color:" + (hover ? "rgba(77,125,255,0.75)" : "rgba(255,255,255,0.12)") + ";" +
                "-fx-border-width:1.3;" +
                "-fx-background-radius:16;" +
                "-fx-border-radius:16;" +
                "-fx-cursor:hand;";
    }

    private StackPane createChip(String text) {
        StackPane chip = new StackPane();

        Text label = new Text(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 11));
        label.setFill(Color.web("#9fb2d4"));

        chip.getChildren().add(label);
        chip.setPadding(new Insets(8, 12, 8, 12));
        chip.setStyle(
                "-fx-background-color:rgba(255,255,255,0.045);" +
                        "-fx-background-radius:999;" +
                        "-fx-border-color:rgba(255,255,255,0.08);" +
                        "-fx-border-radius:999;"
        );

        return chip;
    }

    private StackPane createLogoBox(int size) {
        StackPane logo = new StackPane();
        logo.setPrefSize(size, size);
        logo.setMinSize(size, size);
        logo.setMaxSize(size, size);
        logo.setStyle(
                "-fx-background-color:linear-gradient(to bottom right, rgba(59,108,255,0.95), rgba(35,79,214,0.90));" +
                        "-fx-background-radius:13;" +
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

    private Region createSpacer(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        r.setMinHeight(height);
        r.setMaxHeight(height);
        return r;
    }
}