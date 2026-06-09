package view;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CitizenSettingsView extends BorderPane {
    public CitizenSettingsView() {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));
        VBox root = new VBox(18);
        root.getChildren().add(label("Paramètres", "#ffffff", 24, true));
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18; -fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;");
        CheckBox notif = new CheckBox("Recevoir les alertes importantes");
        notif.setTextFill(Color.web("#ffffff")); notif.setSelected(true);
        CheckBox sound = new CheckBox("Activer le son des notifications");
        sound.setTextFill(Color.web("#ffffff"));
        Label zoomLabel = label("Taille d'affichage", "#b8c7dd", 13, false);
        Slider zoom = new Slider(80, 130, 100);
        zoom.setShowTickLabels(true); zoom.setShowTickMarks(true);
        card.getChildren().addAll(notif, sound, zoomLabel, zoom);
        root.getChildren().add(card);
        setCenter(root);
    }
    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
