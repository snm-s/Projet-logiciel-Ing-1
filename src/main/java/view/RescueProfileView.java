package view;

import app.Main;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RescueProfileView extends BorderPane {
    public RescueProfileView() {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));
        VBox root = new VBox(16);
        root.getChildren().add(label("Profil secouriste", "#ffffff", 24, true));
        VBox card = new VBox(10); card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18; -fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;");
        String name = Main.currentUser != null ? Main.currentUser.getFirstName() + " " + Main.currentUser.getLastName() : "Coordinateur";
        card.getChildren().addAll(label("Nom : " + name, "#ffffff", 14, false), label("Rôle : Secours", "#b8c7dd", 14, false), label("Statut : opérationnel", "#22c55e", 14, true));
        root.getChildren().add(card);
        setCenter(root);
    }
    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text); l.setTextFill(Color.web(color)); l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size)); return l;
    }
}
