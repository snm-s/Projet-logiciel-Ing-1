package view;

import app.Main;
import model.zone.ZoneManager;
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

public class RescueDashboardView extends BorderPane {

    private VBox dashboardContent;
    private MapView mapComponent;
    
    public RescueDashboardView() {
        this.setStyle("-fx-background-color: #f4f6f9;");
        // Construire la carte depuis zones.json
        this.mapComponent = new MapView(new ZoneManager().getZones());

        // 🔥 Lecture du nom de l'agent de secours connecté
        String name = (Main.currentUser != null && Main.currentUser.getFirstName() != null) ? Main.currentUser.getFirstName() : "Coordinateur";

        // ==========================================
        // 1. SIDEBAR (GAUCHE) - Bleu Nuit
        // ==========================================
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: #0b1a30;");
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.setSpacing(8);

        HBox profileBox = new HBox(10);
        profileBox.setAlignment(Pos.CENTER_LEFT);
        profileBox.setPadding(new Insets(0, 0, 20, 10));
        Circle avatar = new Circle(18, Color.web("#e2e8f0"));
        VBox profileTexts = new VBox(2);
        
        Label userName = new Label(name); // <-- Affiche le vrai prénom
        userName.setTextFill(Color.WHITE);
        userName.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label userRole = new Label("Secours");
        userRole.setTextFill(Color.web("#a0b2ce"));
        userRole.setFont(Font.font("System", 11));
        profileTexts.getChildren().addAll(userName, userRole);
        profileBox.getChildren().addAll(avatar, profileTexts);
        sidebar.getChildren().add(profileBox);

        
        Button btnAlerts = createSidebarButton("⚠️  Alertes", false);
        btnAlerts.setOnAction(e -> this.setCenter(new AdminAlertsView()));
        
        Button btnMapView = createSidebarButton("🗺️  Carte", false);
        btnMapView.setOnAction(e -> this.setCenter(mapComponent.getSwingNode()));
        
        
        
        sidebar.getChildren().addAll(
            createSidebarButton("🏠   Tableau de bord", true),
            btnMapView,
            createSidebarButton("👥   Agents", false),
            btnAlerts,
            createSidebarButton("📦   Ressources", false),
            createSidebarButton("📋   Missions", false),
            createSidebarButton("👤  Profil", false),
            createSidebarButton("⚙️   Paramètres", false)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button btnLogout = createSidebarButton("🚪   Déconnexion", false);
        btnLogout.setOnAction(e -> app.Main.showWelcomeView());
        sidebar.getChildren().addAll(spacer, btnLogout);

        this.setLeft(sidebar);

        // ==========================================
        // 2. CONTENU PRINCIPAL (CENTRE)
        // ==========================================
        VBox contentArea = new VBox(20);
        contentArea.setPadding(new Insets(25));

        Label overviewLabel = new Label("Vue d'ensemble");
        overviewLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        overviewLabel.setTextFill(Color.web("#333333"));
        contentArea.getChildren().add(overviewLabel);

        HBox statsRow = new HBox(15);
        statsRow.getChildren().addAll(
            createMiniCard("Agents déployés", "👥 24", "#0b5cbf"),
            createMiniCard("Victimes secourues", "👤 15", "#2ecc71"),
            createMiniCard("Missions actives", "⚙ 3", "#2ecc71"),
            createMiniCard("Alertes actives", "⚠ 4", "#e74c3c")
        );
        contentArea.getChildren().add(statsRow);

        HBox middleRow = new HBox(20);

        VBox alertsBox = new VBox(12);
        alertsBox.setPrefWidth(420);
        alertsBox.setPadding(new Insets(15));
        alertsBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");
        Label lblAlertTitle = new Label("Alertes récentes");
        lblAlertTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        VBox alertList = new VBox(10);
        alertList.getChildren().addAll(
            createLogItem("⚠ Route D12 inondée", "10:24"),
            createLogItem("⚠ Pont des Lilas fermé", "09:58"),
            createLogItem("🔸 Quartier Gare évacué", "09:12"),
            createLogItem("🟢 Hôpital Central accessible", "08:45")
        );
        Hyperlink allAlertsLink = new Hyperlink("Voir toutes les alertes");
        allAlertsLink.setStyle("-fx-underline: false;");
        alertsBox.getChildren().addAll(lblAlertTitle, alertList, allAlertsLink);

        VBox missionsBox = new VBox(12);
        missionsBox.setPrefWidth(420);
        missionsBox.setPadding(new Insets(15));
        missionsBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");
        Label lblMissionTitle = new Label("Missions en cours");
        lblMissionTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        VBox missionList = new VBox(10);
        missionList.getChildren().addAll(
            createStatusItem("Évacuation Quartier Nord", "En cours", "#2ecc71"),
            createStatusItem("Secours PMR - Rue des Écoles", "En cours", "#2ecc71"),
            createStatusItem("Transport vers Hôpital", "En attente", "#f39c12")
        );
        Hyperlink allMissionsLink = new Hyperlink("Voir toutes les missions");
        allMissionsLink.setStyle("-fx-underline: false;");
        missionsBox.getChildren().addAll(lblMissionTitle, missionList, allMissionsLink);

        middleRow.getChildren().addAll(alertsBox, missionsBox);
        contentArea.getChildren().add(middleRow);

        VBox resourcesBox = new VBox(12);
        resourcesBox.setPadding(new Insets(15));
        resourcesBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");
        Label lblResTitle = new Label("Ressources disponibles");
        lblResTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox resourcesRow = new HBox(40);
        resourcesRow.setPadding(new Insets(5, 10, 5, 10));
        resourcesRow.getChildren().addAll(
            new Label("👤 Secouristes : 12"),
            new Label("🚘 Véhicules : 5"),
            new Label("⛵ Bateaux : 2"),
            new Label("🛸 Drones : 3")
        );
        resourcesBox.getChildren().addAll(lblResTitle, resourcesRow);
        contentArea.getChildren().add(resourcesBox);

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

    private HBox createLogItem(String title, String time) {
        HBox item = new HBox();
        Label lblTitle = new Label(title);
        Label lblTime = new Label(time);
        lblTime.setTextFill(Color.GRAY);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        item.getChildren().addAll(lblTitle, sp, lblTime);
        return item;
    }

    private HBox createStatusItem(String title, String status, String statusColorHex) {
        HBox item = new HBox();
        Label lblTitle = new Label(title);
        Label lblStatus = new Label(status);
        lblStatus.setTextFill(Color.web(statusColorHex));
        lblStatus.setStyle("-fx-font-weight: bold;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        item.getChildren().addAll(lblTitle, sp, lblStatus);
        return item;
    }
}