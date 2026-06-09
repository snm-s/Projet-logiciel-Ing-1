package view;

import controller.CitizenPage.CitizenController;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.agent.Agent;

public class CitizenProfileView extends BorderPane {
    public CitizenProfileView(CitizenController controller, Agent user) {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));
        VBox root = new VBox(16);
        root.getChildren().add(label("Mon profil", "#ffffff", 24, true));
        GridPane grid = new GridPane();
        grid.setHgap(18); grid.setVgap(14); grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18; -fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;");
        addRow(grid, 0, "Nom complet", controller.getFullName(user));
        addRow(grid, 1, "État", controller.getCitizenState(user));
        addRow(grid, 2, "Email", user != null && user.getEmail() != null ? user.getEmail() : "—");
        addRow(grid, 3, "Téléphone", user != null && user.getPhone() != null ? user.getPhone() : "—");
        addRow(grid, 4, "Ville", user != null && user.getCity() != null ? user.getCity() : "—");
        addRow(grid, 5, "Zone actuelle", controller.getDetailedPositionLabel(user));
        root.getChildren().add(grid);
        setCenter(root);
    }
    private void addRow(GridPane grid, int row, String key, String value) {
        Label k = label(key + " :", "#b8c7dd", 13, true);
        Label v = label(value, "#ffffff", 13, false);
        grid.add(k, 0, row); grid.add(v, 1, row);
    }
    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
