package view;

import controller.CitizenPage.CitizenController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.agent.Agent;
import model.zone.Zone;

public class CitizenRoutesView extends BorderPane {

    public CitizenRoutesView(CitizenController controller, Agent user, MapView mapView) {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));

        VBox root = new VBox(18);

        Label title = label("Mes trajets", "#ffffff", 24, true);
        Label subtitle = label("Votre itinéraire d'évacuation calculé depuis votre zone vers le refuge conseillé.", "#b8c7dd", 13, false);

        Zone from = controller.getNearestZone(user);
        Zone to = controller.getNearestSafeRefuge(user);

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                card("Départ", from != null ? from.getName() : "Inconnu", "#1683ff"),
                card("Refuge", to != null ? to.getName() : "Aucun", "#22c55e"),
                card("Temps estimé", controller.getEtaLabel(user), "#f59e0b"),
                card("Distance", controller.getDistanceLabel(user), "#ffffff")
        );

        VBox route = glassCard();
        Label routeTitle = label("Étapes recommandées", "#ffffff", 16, true);
        route.getChildren().addAll(
                routeTitle,
                step("1", "Rejoignez l'axe sécurisé le plus proche."),
                step("2", "Évitez les routes orange ou rouges visibles sur la carte."),
                step("3", "Suivez l'itinéraire bleu jusqu'au refuge conseillé."),
                step("4", "Attendez les consignes des secours une fois arrivé.")
        );

        Button show = new Button("Afficher l'itinéraire sur la carte");
        show.setStyle("-fx-background-color:linear-gradient(to right, #0b5cbf, #1683ff); -fx-text-fill:white; -fx-background-radius:8; -fx-font-weight:bold; -fx-cursor:hand;");
        show.setOnAction(e -> mapView.showRoute(from, to));

        root.getChildren().addAll(title, subtitle, cards, route, show);
        setCenter(root);
    }

    private VBox card(String title, String value, String color) {
        VBox c = glassCard();
        c.setPrefWidth(210);
        Label t = label(title, "#b8c7dd", 12, false);
        Label v = label(value, color, 15, true);
        v.setWrapText(true);
        c.getChildren().addAll(t, v);
        return c;
    }

    private VBox glassCard() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-background-radius:18;"
                + "-fx-border-color:rgba(255,255,255,0.18); -fx-border-radius:18;"
                + "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.30), 24, 0, 0, 8);");
        return box;
    }

    private HBox step(String num, String text) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label n = label(num, "#ffffff", 12, true);
        n.setAlignment(Pos.CENTER);
        n.setMinSize(26, 26);
        n.setStyle("-fx-background-color:#0e73eb; -fx-background-radius:13;");
        Label label = label(text, "#b8c7dd", 13, false);
        label.setWrapText(true);
        row.getChildren().addAll(n, label);
        return row;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
