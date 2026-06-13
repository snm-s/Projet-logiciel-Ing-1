package view;

import app.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.agent.Agent;
import model.agent.RescueAgent;
import model.auth.UserService;

public class RescueProfileView extends BorderPane {

    private static final String GLASS = "rgba(8,22,42,0.72)";
    private static final String BORDER = "rgba(255,255,255,0.18)";
    private static final String WHITE = "#ffffff";
    private static final String MUTED = "#b8c7dd";
    private static final String RED = "#ef4444";
    private static final String GREEN = "#22c55e";
    private static final String BLUE = "#1683ff";
    private static final String CARD_ROW = "rgba(255,255,255,0.055)";

    public RescueProfileView() {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(30));

        Agent user = Main.currentUser;

        String firstName = user != null && user.getFirstName() != null ? user.getFirstName() : "Secours";
        String lastName = user != null && user.getLastName() != null ? user.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();

        String email = user != null && user.getEmail() != null ? user.getEmail() : "Non renseigné";
        String phone = user != null && user.getPhone() != null ? user.getPhone() : "Non renseigné";
        String city = user != null && user.getCity() != null ? user.getCity() : "Lyon";

        String state = "OPÉRATIONNEL";
        String teamType = "Équipe de secours";

        if (user instanceof RescueAgent rescue) {
            if (rescue.getState() != null) {
                state = rescue.getState().name();
            }
            if (rescue.getTeamType() != null && !rescue.getTeamType().isBlank()) {
                teamType = rescue.getTeamType();
            }
        }

        VBox root = new VBox(24);

        Label title = label("Profil secouriste", WHITE, 30, true);
        Label subtitle = label("Informations du compte secours et état opérationnel.", MUTED, 14, false);

        VBox header = new VBox(4, title, subtitle);

        HBox topCards = new HBox(22);
        topCards.getChildren().addAll(
                identityCard(fullName, teamType, state),
                statusCard(state)
        );

        VBox infoCard = glassCard();
        Label infoTitle = label("Informations personnelles", WHITE, 20, true);
        Label infoSub = label("Données utiles pour identifier le secouriste dans l’application.", MUTED, 13, false);

        infoCard.getChildren().addAll(
            infoTitle,
            infoSub,
            editableRow("Nom complet", fullName, user, "name"),
editableRow("Email", email, user, "email"),
editableRow("Téléphone", phone, user, "phone"),
            row("Ville", city),
            row("Rôle", "Secours"),
            row("Type d’équipe", teamType)
    );

        VBox missionCard = glassCard();
        Label missionTitle = label("État d’intervention", WHITE, 20, true);
        Label missionSub = label("Résumé de la disponibilité opérationnelle du secouriste.", MUTED, 13, false);

        missionCard.getChildren().addAll(
                missionTitle,
                missionSub,
                row("Statut", state),
                row("Disponibilité", "Prêt à intervenir"),
                row("Zone principale", city),
                row("Priorité", "Alertes critiques")
        );

        HBox bottom = new HBox(22, infoCard, missionCard);
        HBox.setHgrow(infoCard, Priority.ALWAYS);
        HBox.setHgrow(missionCard, Priority.ALWAYS);

        root.getChildren().addAll(header, topCards, bottom);
        setCenter(root);
    }

    private VBox identityCard(String name, String teamType, String state) {
        VBox card = glassCard();
        card.setPrefWidth(560);

        HBox content = new HBox(20);
        content.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(45, Color.web(RED));

        VBox texts = new VBox(8);
        Label nameLbl = label(name, WHITE, 24, true);
        Label roleLbl = label(teamType, MUTED, 14, false);

        HBox badges = new HBox(8);
        badges.getChildren().addAll(
                badge("Secours", BLUE),
                badge(state, GREEN)
        );

        texts.getChildren().addAll(nameLbl, roleLbl, badges);
        content.getChildren().addAll(avatar, texts);

        card.getChildren().add(content);
        return card;
    }

    private VBox statusCard(String state) {
        VBox card = glassCard();
        card.setPrefWidth(430);

        Label title = label("État opérationnel", WHITE, 20, true);
        Label status = label("Statut : " + state, GREEN, 20, true);
        Label desc = label("Le secouriste est disponible pour les alertes et interventions.", MUTED, 13, false);

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
                miniStat("Alertes", "Prioritaires"),
                miniStat("Missions", "Actives"),
                miniStat("Réseau", "Connecté")
        );

        card.getChildren().addAll(title, status, desc, stats);
        return card;
    }

    private VBox glassCard() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.setStyle(
                "-fx-background-color:" + GLASS + ";" +
                "-fx-background-radius:24;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-radius:24;" +
                "-fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0, 0, 8);"
        );
        return box;
    }

    private HBox row(String left, String right) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color:" + CARD_ROW + "; -fx-background-radius:12;");

        Label l = label(left, WHITE, 13, true);
        l.setPrefWidth(150);

        Label r = label(right, WHITE, 13, false);

        row.getChildren().addAll(l, r);
        return row;
    }

    private HBox editableRow(String left, String value, Agent user, String fieldName) {
    HBox row = new HBox(10);
    row.setAlignment(Pos.CENTER_LEFT);
    row.setPadding(new Insets(10, 14, 10, 14));
    row.setStyle("-fx-background-color:" + CARD_ROW + "; -fx-background-radius:12;");

    Label l = label(left, WHITE, 13, true);
    l.setPrefWidth(150);

    TextField field = new TextField(value);
    field.setEditable(false);
    field.setStyle(
        "-fx-background-color:transparent;" +
        "-fx-text-fill:white;" +
        "-fx-border-color:transparent;" +
        "-fx-font-size:13px;"
    );

    HBox.setHgrow(field, Priority.ALWAYS);

    Button edit = new Button("✎");
    edit.setStyle(
        "-fx-background-color:rgba(22,131,255,0.18);" +
        "-fx-text-fill:#1683ff;" +
        "-fx-font-weight:bold;" +
        "-fx-background-radius:10;" +
        "-fx-cursor:hand;"
    );

    edit.setOnAction(e -> {
        if (!field.isEditable()) {
            field.setEditable(true);
            field.requestFocus();
            edit.setText("✓");
        } else {
            field.setEditable(false);
            edit.setText("✎");

            if (user != null) {
                String newValue = field.getText().trim();

                switch (fieldName) {
                    case "name" -> {
                        String[] parts = newValue.split(" ", 2);
                        user.setFirstName(parts.length > 0 ? parts[0] : "");
                        user.setLastName(parts.length > 1 ? parts[1] : "");
                    }
                    case "email" -> user.setEmail(newValue);
                    case "phone" -> user.setPhone(newValue);
                }

                UserService.updateAgent(user);
            }
        }
    });

    row.getChildren().addAll(l, field, edit);
    return row;
}

    private Label badge(String text, String color) {
        Label b = label(text, WHITE, 12, true);
        b.setPadding(new Insets(5, 12, 5, 12));
        b.setStyle("-fx-background-color:" + color + "; -fx-background-radius:99;");
        return b;
    }

    private VBox miniStat(String title, String value) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(12));
        box.setPrefWidth(120);
        box.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:14;");

        box.getChildren().addAll(
                label(title, MUTED, 12, false),
                label(value, WHITE, 14, true)
        );

        return box;
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
