package view;

import app.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
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
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;

public class CitizenDashboardView extends BorderPane {

    public CitizenDashboardView() {
        this.setStyle("-fx-background-color: #f4f6f9;");

        // 1. Récupération de l'agent connecté en session
        Agent user = Main.currentUser;
        
        String firstName = (user != null) ? user.getFirstName() : "Citoyen";
        String lastName = (user != null && user.getLastName() != null) ? user.getLastName() : "";
        String fullName = (lastName.isEmpty()) ? firstName : firstName + " " + lastName;
        
        String positionName = "Inconnue";
        String targetRefuge = "Aucun refuge assigné";
        String distanceStr = "Calcul...";
        String etaStr = "-- min";

        // 2. Traitement des données à partir des liaisons Node réelles de l'agent
        if (user != null && user.getPosition() != null) {
            positionName = "Nœud #" + user.getId(); 
            
            if (user.getDestination() != null) {
                targetRefuge = "Nœud #" + user.getId();
                
                // Simulation d'une distance d'évacuation relative propre à l'utilisateur
                double userDistance = 1.2; 
                distanceStr = userDistance + " km";
                
                // Calcul de l'ETA basé sur sa vitesse maximale réelle
                if (user.getMaxSpeed() > 0) {
                    etaStr = (int) ((userDistance / user.getMaxSpeed()) * 60) + " min";
                } else {
                    etaStr = "18 min"; // Valeur par défaut standard
                }
            }
        }

        String stateStr;

        if (user instanceof Citizen c) {
            stateStr = (c.getState() != null) ? c.getState().name() : "CALME";
        } else {
            throw new IllegalStateException("L'utilisateur connecté n'est pas un citoyen !");
        }
        
        // ==========================================
        // SIDEBAR (GAUCHE) - Adaptée à l'état de l'User
        // ==========================================
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: #0b1a30;");
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.setSpacing(8);

        HBox profileBox = new HBox(10);
        profileBox.setAlignment(Pos.CENTER_LEFT);
        profileBox.setPadding(new Insets(0, 0, 20, 10));
        
        // Couleur de l'avatar change si l'utilisateur passe en mode PANIQUE ou FOLIE
        Color avatarColor = "PANIQUE".equalsIgnoreCase(stateStr) || "FOLIE".equalsIgnoreCase(stateStr) 
                            ? Color.web("#e74c3c") 
                            : Color.web("#a0b2ce");
        Circle avatar = new Circle(18, avatarColor);
        
        VBox profileTexts = new VBox(2);
        Label userNameLabel = new Label(fullName);
        userNameLabel.setTextFill(Color.WHITE);
        userNameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label userRole = new Label("Citoyen (" + stateStr.toLowerCase() + ")");
        userRole.setTextFill(Color.web("#a0b2ce"));
        userRole.setFont(Font.font("System", 11));
        profileTexts.getChildren().addAll(userNameLabel, userRole);
        profileBox.getChildren().addAll(avatar, profileTexts);
        sidebar.getChildren().add(profileBox);


        
        
        
        Button btnAlerts = createSidebarButton("⚠️  Alertes", false);
        btnAlerts.setOnAction(e -> {
            // On met à jour le centre du BorderPane principal avec votre vue
            this.setCenter(new CitizenAlertsView());
        });
        
        
        
        sidebar.getChildren().addAll(
            createSidebarButton("🏠  Tableau de bord", true),
            createSidebarButton("🗺️  Carte", false),
            createSidebarButton("🔀  Mes trajets", false),
            btnAlerts,
            createSidebarButton("🏫  Refuges", false)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button btnLogout = createSidebarButton("🚪  Déconnexion", false);
        btnLogout.setOnAction(e -> Main.showWelcomeView());
        sidebar.getChildren().addAll(spacer, btnLogout);

        this.setLeft(sidebar);

        // ==========================================
        // CONTENU PRINCIPAL (CENTRE)
        // ==========================================
        VBox contentArea = new VBox(20);
        contentArea.setPadding(new Insets(25));
        
        Label welcomeLabel = new Label("Bonjour, " + firstName + " !");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        welcomeLabel.setTextFill(Color.web("#333333"));
        contentArea.getChildren().add(welcomeLabel);

        // Les mini-cartes exploitent uniquement les variables textuelles et numériques corrigées
        HBox miniCardsRow = new HBox(15);
        miniCardsRow.getChildren().addAll(
            createMiniCard("Ma Position", positionName, "#3498db"),
            createMiniCard("Itinéraire actuel", (user != null && user.isSaved()) ? "Arrivé" : "En cours", "#2ecc71"),
            createMiniCard("Temps estimé", "🕒 " + etaStr, "#333333"),
            createMiniCard("Distance au refuge", "🏠 " + distanceStr, "#2ecc71")
        );
        contentArea.getChildren().add(miniCardsRow);

        HBox middleRow = new HBox(20);
        
        // Panneau Alerte
        VBox alertPanel = new VBox(12);
        alertPanel.setPrefWidth(420);
        alertPanel.setPadding(new Insets(20));
        alertPanel.setStyle("-fx-background-color: #fff5f5; -fx-background-radius: 8; -fx-border-color: #ffe3e3; -fx-border-radius: 8;");
        Label alertTitle = new Label("⚠ Consignes d'Évacuation");
        alertTitle.setTextFill(Color.web("#c0392b"));
        alertTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label alertText = new Label("Une crise est en cours. Les administrateurs gèrent la charge des axes de circulation en direct. Restez sur les itinéraires officiels.");
        alertText.setWrapText(true);
        Hyperlink alertLink = new Hyperlink("Voir les détails de la zone");
        alertLink.setStyle("-fx-underline: false;");
        alertPanel.getChildren().addAll(alertTitle, alertText, alertLink);

        // Panneau Itinéraire recommandé
        VBox routePanel = new VBox(12);
        routePanel.setPrefWidth(420);
        routePanel.setPadding(new Insets(20));
        routePanel.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        Label routeTitle = new Label("Plan de Route Assigné");
        routeTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        VBox routeDetails = new VBox(8);
        routeDetails.getChildren().addAll(
            new Label("🟢 Position actuelle : " + positionName),
            new Label("🔴 Cible d'évacuation : " + targetRefuge)
        );
        
        HBox routeFooter = new HBox(20);
        routeFooter.setAlignment(Pos.CENTER_LEFT);
        Label routeTime = new Label(etaStr + " (" + distanceStr + ")");
        Button btnMap = new Button("Suivre sur la carte");
        btnMap.setStyle("-fx-background-color: #0b1a30; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
        Region rSpacer = new Region();
        HBox.setHgrow(rSpacer, Priority.ALWAYS);
        routeFooter.getChildren().addAll(routeTime, rSpacer, btnMap);
        
        routePanel.getChildren().addAll(routeTitle, routeDetails, routeFooter);
        middleRow.getChildren().addAll(alertPanel, routePanel);
        contentArea.getChildren().add(middleRow);

        this.setCenter(contentArea);
    }

    private Button createSidebarButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 15, 10, 15));
        if (active) {
            btn.setStyle("-fx-background-color: #0b5cbf; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-background-radius: 6; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 6;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-background-radius: 6;"));
        }
        return btn;
    }

    private VBox createMiniCard(String title, String value, String colorHex) {
        VBox card = new VBox(8);
        card.setPrefWidth(200);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        Label lblTitle = new Label(title);
        lblTitle.setTextFill(Color.web("#718096"));
        lblTitle.setFont(Font.font("System", 12));
        Label lblValue = new Label(value);
        lblValue.setTextFill(Color.web(colorHex));
        lblValue.setFont(Font.font("System", FontWeight.BOLD, 18));
        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }
}