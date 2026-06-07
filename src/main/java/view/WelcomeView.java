package view;

import app.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class WelcomeView extends StackPane {

    private static final String BLUE = "#0e73eb";
    private static final String LIGHT = "#b8c7dd";
    private static final String WHITE = "#ffffff";

    public WelcomeView() {
        buildUI();
    }

    private void buildUI() {
        setPrefSize(1100, 650);

        Region background = new Region();

var imageUrl = getClass().getResource("/images/imagefond.png");

if (imageUrl != null) {
    background.setStyle(
        "-fx-background-image: url('" + imageUrl.toExternalForm() + "');" +
        "-fx-background-size: cover;" +
        "-fx-background-position: center;" +
        "-fx-background-repeat: no-repeat;"
    );
} else {
    background.setStyle("-fx-background-color: #06172b;");
    System.out.println("Image introuvable : /images/imagefond.png");
}

Region overlay = new Region();
overlay.setStyle(
    "-fx-background-color: linear-gradient(to bottom," +
    "rgba(2,8,18,0.08)," +
    "rgba(3,12,25,0.18)," +
    "rgba(3,12,25,0.35));"
);

        VBox card = new VBox(22);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(35, 45, 35, 45));
        card.setMaxWidth(620);
        card.setStyle(
            "-fx-background-color: rgba(8, 22, 42, 0.58);" +
            "-fx-background-radius: 28;" +
            "-fx-border-radius: 28;" +
            "-fx-border-color: rgba(255,255,255,0.28);" +
            "-fx-border-width: 1.2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 32, 0, 0, 10);"
        );

        StackPane logo = createLogo();

        Text title = new Text("SIMULATION ");
        title.setFont(Font.font("System", FontWeight.BOLD, 34));
        title.setFill(Color.WHITE);

        Text titleBlue = new Text("D'INONDATION");
        titleBlue.setFont(Font.font("System", FontWeight.BOLD, 34));
        titleBlue.setFill(Color.web(BLUE));

        HBox titleBox = new HBox(title, titleBlue);
        titleBox.setAlignment(Pos.CENTER);

        Text subtitle = new Text("Système de gestion des urgences");
        subtitle.setFont(Font.font("System", FontWeight.LIGHT, 18));
        subtitle.setFill(Color.web(LIGHT));

        Region line = new Region();
        line.setPrefSize(45, 3);
        line.setStyle("-fx-background-color: " + BLUE + "; -fx-background-radius: 20;");

        HBox features = new HBox(44);
        features.setAlignment(Pos.CENTER);
        features.getChildren().addAll(
            createFeature(createShieldIcon(), "Anticiper", "les risques"),
            createFeature(createPinIcon(), "Coordonner", "les secours"),
            createFeature(createUsersIcon(), "Protéger", "les populations"),
            createFeature(createChartIcon(), "Simuler", "les scénarios")
        );

        Button loginButton = createMainButton("Se connecter", createLoginIcon());
        Button registerButton = createSecondaryButton("S'inscrire", createUserPlusIcon());

        loginButton.setOnAction(e -> Main.showLoginView());
        registerButton.setOnAction(e -> Main.showRegisterView());

        VBox buttons = new VBox(14, loginButton, registerButton);
        buttons.setAlignment(Pos.CENTER);

        card.getChildren().addAll(logo, titleBox, subtitle, line, features, buttons);

        VBox mission = createMissionBlock();
        HBox footer = createFooter();

        VBox content = new VBox(18, card, mission, footer);
        content.setAlignment(Pos.CENTER);

        getChildren().addAll(background, overlay, content);
    }

    private VBox createFeature(Node icon, String title, String subtitle) {
        StackPane circle = new StackPane(icon);
        circle.setPrefSize(58, 58);
        circle.setStyle(
            "-fx-background-color: rgba(14,115,235,0.18);" +
            "-fx-background-radius: 50;" +
            "-fx-effect: dropshadow(gaussian, rgba(14,115,235,0.25), 18, 0, 0, 0);"
        );

        Text titleText = new Text(title);
        titleText.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleText.setFill(Color.WHITE);

        Text subText = new Text(subtitle);
        subText.setFont(Font.font("System", FontWeight.NORMAL, 13));
        subText.setFill(Color.web(LIGHT));

        VBox box = new VBox(7, circle, titleText, subText);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox createMissionBlock() {
        StackPane drop = new StackPane(createDropIcon());
        drop.setPrefSize(45, 32);

        Text title = new Text("Notre mission");
        title.setFont(Font.font("System", FontWeight.BOLD, 17));
        title.setFill(Color.web(BLUE));

        Text desc = new Text("Fournir des outils avancés pour anticiper, gérer et réduire\nl'impact des inondations et sauver des vies.");
        desc.setFont(Font.font("System", FontWeight.NORMAL, 13));
        desc.setFill(Color.web(LIGHT));
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox box = new VBox(6, drop, title, desc);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private HBox createFooter() {
        HBox footer = new HBox(70);
        footer.setAlignment(Pos.CENTER);
        footer.getChildren().addAll(
            createFooterItem(createLockIcon(), "Sécurisé et confidentiel"),
            createFooterItem(createMapIcon(), "Données fiables et à jour"),
            createFooterItem(createSmallUsersIcon(), "Conçu pour les professionnels\net les citoyens")
        );
        return footer;
    }

    private HBox createFooterItem(Node icon, String text) {
        StackPane circle = new StackPane(icon);
        circle.setPrefSize(32, 32);
        circle.setStyle("-fx-background-color: rgba(14,115,235,0.18); -fx-background-radius: 50;");

        Text label = new Text(text);
        label.setFont(Font.font("System", FontWeight.NORMAL, 12));
        label.setFill(Color.web(LIGHT));

        HBox box = new HBox(10, circle, label);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Button createMainButton(String text, Node icon) {
        Button button = new Button(text);
        button.setGraphic(icon);
        button.setGraphicTextGap(12);
        button.setPrefWidth(380);
        button.setPrefHeight(48);
        button.setFont(Font.font("System", FontWeight.BOLD, 16));
        button.setTextFill(Color.WHITE);
        button.setStyle(
            "-fx-background-color: linear-gradient(to right, #0b5cbf, #1683ff);" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: #1683ff;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: linear-gradient(to right, #0b5cbf, #1683ff);" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        ));

        return button;
    }

    private Button createSecondaryButton(String text, Node icon) {
        Button button = new Button(text);
        button.setGraphic(icon);
        button.setGraphicTextGap(12);
        button.setPrefWidth(380);
        button.setPrefHeight(48);
        button.setFont(Font.font("System", FontWeight.BOLD, 15));
        button.setTextFill(Color.web("#c4d2e6"));
        button.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: rgba(255,255,255,0.30);" +
            "-fx-border-width: 1.5;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;" +
            "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-border-color: white;" +
            "-fx-border-width: 1.5;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;" +
            "-fx-text-fill: white;" +
            "-fx-cursor: hand;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: rgba(255,255,255,0.30);" +
            "-fx-border-width: 1.5;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;" +
            "-fx-text-fill: #c4d2e6;"
        ));

        return button;
    }

    private StackPane createLogo() {
        StackPane logoPane = new StackPane();
        logoPane.setPrefSize(90, 80);
    
        SVGPath house = new SVGPath();
        house.setContent(
            "M20 42 L45 20 L70 42 " +
            "M28 40 V65 H62 V40 " +
            "M38 65 V50 H52 V65"
        );
        house.setStroke(Color.WHITE);
        house.setStrokeWidth(4);
        house.setFill(Color.TRANSPARENT);
    
        Path waves = new Path(
            new MoveTo(25, 70),
            new CubicCurveTo(35, 66, 45, 74, 55, 70),
            new CubicCurveTo(65, 66, 75, 74, 85, 70),
    
            new MoveTo(25, 77),
            new CubicCurveTo(35, 73, 45, 81, 55, 77),
            new CubicCurveTo(65, 73, 75, 81, 85, 77)
        );
        waves.setStroke(Color.WHITE);
        waves.setStrokeWidth(3);
        waves.setFill(Color.TRANSPARENT);
    
        logoPane.getChildren().addAll(house, waves);
        return logoPane;
    }

    
    private SVGPath svg(String content, String color) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setStroke(Color.web(color));
        path.setStrokeWidth(2.2);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private Node createShieldIcon() {
        return svg("M10 2 L20 6 V13 C20 19 15 23 10 25 C5 23 0 19 0 13 V6 Z M10 8 V18 M6 13 H14", BLUE);
    }

    private Node createPinIcon() {
        return svg("M10 2 C6 2 3 5 3 9 C3 15 10 23 10 23 C10 23 17 15 17 9 C17 5 14 2 10 2 Z M10 7 A2.5 2.5 0 1 1 10 12 A2.5 2.5 0 1 1 10 7", BLUE);
    }

    private Node createUsersIcon() {
        return svg("M8 11 A4 4 0 1 1 8 3 A4 4 0 1 1 8 11 M1 22 C1 16 15 16 15 22 M18 10 A3 3 0 1 1 18 4 A3 3 0 1 1 18 10 M16 15 C21 15 24 17 24 22", BLUE);
    }

    private Node createChartIcon() {
        return svg("M2 22 H23 M5 18 L10 13 L14 16 L21 7 M21 7 V13 M21 7 H15", BLUE);
    }

    private Node createLoginIcon() {
        return svg("M3 12 H17 M12 7 L17 12 L12 17 M20 4 H24 V20 H20", WHITE);
    }

    private Node createUserPlusIcon() {
        return svg("M8 10 A4 4 0 1 1 8 2 A4 4 0 1 1 8 10 M1 22 C1 16 15 16 15 22 M20 8 V18 M15 13 H25", LIGHT);
    }

    private Node createDropIcon() {
        return svg("M10 2 C10 2 4 10 4 15 A6 6 0 1 0 16 15 C16 10 10 2 10 2 Z M1 23 C5 20 15 20 19 23", BLUE);
    }

    private Node createLockIcon() {
        return svg("M5 11 H19 V23 H5 Z M8 11 V7 A4 4 0 0 1 16 7 V11", BLUE);
    }

    private Node createMapIcon() {
        return svg("M3 5 L9 2 L15 5 L21 2 V20 L15 23 L9 20 L3 23 Z M9 2 V20 M15 5 V23", BLUE);
    }

    private Node createSmallUsersIcon() {
        return svg("M8 10 A4 4 0 1 1 8 2 A4 4 0 1 1 8 10 M1 22 C1 16 15 16 15 22 M18 9 A3 3 0 1 1 18 3 A3 3 0 1 1 18 9 M16 15 C21 15 24 17 24 22", BLUE);
    }
}
