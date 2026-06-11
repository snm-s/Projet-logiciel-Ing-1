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

public class RescueSettingsView extends BorderPane {
    public RescueSettingsView() {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));
        VBox root = new VBox(18);
        root.getChildren().add(label("Paramètres", "#ffffff", 24, true));
        VBox card = new VBox(14); card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18; -fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;");
        CheckBox alerts = new CheckBox("Prioriser les alertes critiques"); alerts.setSelected(true); alerts.setTextFill(Color.WHITE);
        CheckBox sound = new CheckBox("Sonner les nouvelles missions"); sound.setTextFill(Color.WHITE);
        Label refresh = label("Fréquence de mise à jour", "#b8c7dd", 13, false);
        Slider slider = new Slider(1, 10, 3); slider.setShowTickLabels(true); slider.setShowTickMarks(true);
        card.getChildren().addAll(alerts, sound, refresh, slider);
        root.getChildren().add(card);
        setCenter(root);
    }
    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text); l.setTextFill(Color.web(color)); l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size)); return l;
    }
}