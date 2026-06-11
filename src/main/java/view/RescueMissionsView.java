package view;

import controller.RescuePage.RescueController;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.zone.Zone;

public class RescueMissionsView extends BorderPane {
    public RescueMissionsView(MapView mapView, RescueController controller) {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));

        VBox root = new VBox(18);
        root.getChildren().addAll(label("Missions", "#ffffff", 24, true), label("Gérez les interventions en cours.", "#b8c7dd", 13, false));

        FlowPane list = new FlowPane();
        list.setHgap(14);
        list.setVgap(14);

        int i = 1;
        for (Zone z : controller.getZones()) {
            if (i > 6) break;
            list.getChildren().add(missionCard("Mission #" + i, z, mapView));
            i++;
        }

        root.getChildren().add(list);
        setCenter(root);
    }

    private VBox missionCard(String mission, Zone zone, MapView mapView) {
        VBox card = glass();
        card.setPrefWidth(275);
        Label title = label("📋 " + mission, "#ffffff", 16, true);
        Label location = label("Zone : " + zone.getName(), "#b8c7dd", 12, false);
        Label priority = label(zone.isFlooded() ? "Priorité haute" : "Priorité moyenne", zone.isFlooded() ? "#ef4444" : "#f59e0b", 13, true);
        Button focus = button("Voir sur la carte");
        focus.setOnAction(e -> mapView.focusZone(zone));
        card.getChildren().addAll(title, location, priority, focus);
        return card;
    }

    private VBox glass() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18; -fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;");
        return box;
    }

    private Button button(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff); -fx-text-fill:white; -fx-background-radius:8; -fx-font-weight:bold; -fx-cursor:hand;");
        return b;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
