package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RescueSettingsView extends BorderPane {

    private static final String WHITE = "#ffffff";
    private static final String LIGHT = "#b8c7dd";
    private static final String GLASS = "rgba(8,22,42,0.72)";
    private static final String BORDER = "rgba(255,255,255,0.18)";
    private static final String BLUE = "#1683ff";
    private static final String RED = "#ef4444";

    /**
     * Constructs a new RescueSettingsView.
     */
    public RescueSettingsView() {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(30));

        VBox root = new VBox(24);

        Label title = label("Paramètres", WHITE, 28, true);
        Label subtitle = label("Gérez les notifications essentielles du poste secours.", LIGHT, 14, false);

        VBox header = new VBox(4, title, subtitle);

        VBox card = new VBox(18);
        card.setPadding(new Insets(24));
        card.setMaxWidth(760);
        card.setStyle(
                "-fx-background-color:" + GLASS + ";" +
                "-fx-background-radius:22;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-radius:22;" +
                "-fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 28, 0, 0, 10);"
        );

        Label notifTitle = label("Notifications", WHITE, 22, true);

        card.getChildren().addAll(
                notifTitle,
                notificationRow(
                        "Alertes critiques prioritaires",
                        "Afficher en priorité les alertes urgentes.",
                        "!",
                        RED,
                        true
                ),
                notificationRow(
                        "Son des nouvelles missions",
                        "Prévenir lorsqu’une nouvelle mission est assignée.",
                        "•",
                        BLUE,
                        true
                ),
                notificationRow(
                        "Mises à jour d’itinéraire",
                        "Recevoir les changements de trajet en temps réel.",
                        "↻",
                        BLUE,
                        true
                )
        );

        root.getChildren().addAll(header, card);
        setCenter(root);
    }

    /**
     * Performs row.
     * @param title the title.
     * @param desc the desc.
     * @param icon the icon.
     * @param color the color.
     * @param selected the selected.
     * @return the HBox.
     */
    private HBox notificationRow(String title, String desc, String icon, String color, boolean selected) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(
                "-fx-background-color:rgba(255,255,255,0.045);" +
                "-fx-background-radius:16;" +
                "-fx-border-color:rgba(255,255,255,0.08);" +
                "-fx-border-radius:16;"
        );

        Circle iconBg = new Circle(22);
        iconBg.setFill(Color.web(color + "33"));

        Label iconLbl = label(icon, color, 18, true);
        javafx.scene.layout.StackPane iconBox = new javafx.scene.layout.StackPane(iconBg, iconLbl);

        VBox texts = new VBox(3);
        texts.getChildren().addAll(
                label(title, WHITE, 15, true),
                label(desc, LIGHT, 12, false)
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(selected);

        row.getChildren().addAll(iconBox, texts, spacer, checkBox);
        return row;
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
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
