package view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RescueAgentsView extends BorderPane {
    public RescueAgentsView() {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));
        VBox root = new VBox(18);
        root.getChildren().addAll(label("Agents", "#ffffff", 24, true), label("État des équipes de secours.", "#b8c7dd", 13, false));

        FlowPane list = new FlowPane();
        list.setHgap(14); list.setVgap(14);
        list.getChildren().addAll(
                card("Équipe Alpha", "Disponible", "#22c55e"),
                card("Équipe Bravo", "En intervention", "#ef4444"),
                card("Équipe Charlie", "En route", "#f59e0b"),
                card("Unité médicale", "Disponible", "#22c55e")
        );
        root.getChildren().add(list);
        setCenter(root);
    }
    private VBox card(String name, String state, String color) {
        VBox c = new VBox(8);
        c.setPrefWidth(230);
        c.setPadding(new Insets(18));
        c.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18; -fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;");
        c.getChildren().addAll(label("👥 " + name, "#ffffff", 16, true), label(state, color, 13, true));
        return c;
    }
    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text); l.setTextFill(Color.web(color)); l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size)); return l;
    }
}