package view;

import java.util.function.Consumer;

import controller.CitizenPage.CitizenController;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.agent.Agent;
import model.zone.Zone;

public class CitizenRefugesView extends BorderPane {

    public CitizenRefugesView(CitizenController controller, Agent user, Consumer<Zone> routeAction) {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));

        VBox root = new VBox(18);

        Label title = label("Refuges", "#ffffff", 24, true);
        Label sub = label("Choisissez un refuge accessible pour afficher un vrai itinéraire sur la carte.", "#b8c7dd", 13, false);

        FlowPane list = new FlowPane();
        list.setHgap(14);
        list.setVgap(14);

        for (Zone z : controller.getZones()) {
            list.getChildren().add(refugeCard(controller, user, z, routeAction));
        }

        root.getChildren().addAll(title, sub, list);
        setCenter(root);
    }

    private VBox refugeCard(CitizenController controller, Agent user, Zone z, Consumer<Zone> routeAction) {
        boolean danger = z.isFlooded() || z.isEvacuated();

        VBox card = new VBox(10);
        card.setPrefWidth(260);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18;"
                + "-fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;"
                + "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.30), 24, 0, 0, 8);");

        Label name = label("🏫 " + z.getName(), "#ffffff", 16, true);
        Label desc = label(controller.shortDescription(z), "#b8c7dd", 12, false);
        desc.setWrapText(true);
        Label status = label(danger ? "Inaccessible / à éviter" : "Accessible", danger ? "#ef4444" : "#22c55e", 13, true);
        Label info = label("Population : " + z.getPopulation() + " • Altitude : " + String.format("%.1f m", z.getAltitude()), "#b8c7dd", 11, false);

        Button btn = new Button("Voir l'itinéraire");
        btn.setDisable(danger);
        btn.setStyle(danger
                ? "-fx-background-color:rgba(255,255,255,0.05); -fx-text-fill:#64748b; -fx-background-radius:8;"
                : "-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff); -fx-text-fill:white; -fx-background-radius:8; -fx-font-weight:bold; -fx-cursor:hand;");
        btn.setOnAction(e -> routeAction.accept(z));

        card.getChildren().addAll(name, desc, status, info, btn);
        return card;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
