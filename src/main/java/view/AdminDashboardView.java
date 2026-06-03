package view;

import app.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
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

public class AdminDashboardView extends BorderPane {

    public AdminDashboardView() {
        // Fond principal sombre tactique / cockpit
        this.setStyle("-fx-background-color: #081225;");

        // Récupération de l'admin en session
        Agent user = Main.currentUser;
        String adminName = (user != null) ? user.getFirstName() + " " + user.getLastName() : "Chef Admin";

        // ==========================================
        // 1. SIDEBAR (GAUCHE) - Design Pro & Déconnexion
        // ==========================================
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setStyle("-fx-background-color: #030a16; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 1 0 0;");
        sidebar.setPadding(new Insets(25, 15, 25, 15));
        sidebar.setSpacing(10);

        // Profil Admin
        HBox profileBox = new HBox(12);
        profileBox.setAlignment(Pos.CENTER_LEFT);
        profileBox.setPadding(new Insets(0, 0, 25, 5));
        
        Circle avatar = new Circle(18, Color.web("#e74c3c")); // Rouge Alerte pour l'admin
        VBox profileTexts = new VBox(2);
        Label lblName = new Label(adminName);
        lblName.setTextFill(Color.WHITE);
        lblName.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label lblRole = new Label("Super Administrateur");
        lblRole.setTextFill(Color.web("#718096"));
        lblRole.setFont(Font.font("System", 11));
        profileTexts.getChildren().addAll(lblName, lblRole);
        profileBox.getChildren().addAll(avatar, profileTexts);
        sidebar.getChildren().add(profileBox);

        // Liens de Navigation Admin
        sidebar.getChildren().addAll(
            createSidebarButton("🎛️  Console Générale", true),
            createSidebarButton("🗺️  Éditeur de Graphe", false),
            createSidebarButton("👥  Gestion des Agents", false),
            createSidebarButton("📊  Statistiques", false),
            createSidebarButton("⚙️  Configuration", false)
        );

        // Bouton Déconnexion en bas (enfin là !)
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button btnLogout = createSidebarButton("🚪  Déconnexion", false);
        
        // Action de déconnexion : vide la session et renvoie à l'accueil
        btnLogout.setOnAction(e -> {
            Main.currentUser = null;
            Main.showWelcomeView();
        });
        sidebar.getChildren().addAll(spacer, btnLogout);

        this.setLeft(sidebar);

        // ==========================================
        // 2. CONTENU PRINCIPAL (CENTRE) - Mode Cockpit Pro
        // ==========================================
        VBox contentArea = new VBox(25);
        contentArea.setPadding(new Insets(30));
        
        // Titre de la page
        Label titleLabel = new Label("Simulation Control Panel");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        contentArea.getChildren().add(titleLabel);

        // Rangée des KPIs / Statistiques globales de crise
        HBox kpiRow = new HBox(15);
        kpiRow.getChildren().addAll(
            createKpiCard("Agents Actifs", "⚡ 142", "#3498db"),
            createKpiCard("Citoyens Secourus", "💚 89%", "#2ecc71"),
            createKpiCard("Niveau des Eaux", "🌊 +1.45m", "#e74c3c"),
            createKpiCard("Réseau Graphe", "📐 42 Nœuds", "#f1c40f")
        );
        contentArea.getChildren().add(kpiRow);

        // Bloc du milieu : Moteur de simulation + Générateur de masse
        HBox panelsRow = new HBox(20);
        
        // Panneau de Contrôle Temporel
        VBox enginePanel = new VBox(15);
        enginePanel.setPrefWidth(450);
        enginePanel.setPadding(new Insets(20));
        enginePanel.setStyle("-fx-background-color: #0f1c30; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.08);");
        
        Label engineTitle = new Label("⏱️ Moteur de Contrôle Temporel");
        engineTitle.setTextFill(Color.WHITE);
        engineTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox controlButtons = new HBox(10);
        Button btnStart = createControlButton("▶ START", "#2ecc71");
        Button btnPause = createControlButton("⏸ PAUSE", "#f39c12");
        Button btnStep = createControlButton("⏭ STEP", "#3498db");
        Button btnStop = createControlButton("⏹ RESET", "#e74c3c");
        controlButtons.getChildren().addAll(btnStart, btnPause, btnStep, btnStop);

        VBox slidersBox = new VBox(12);
        slidersBox.setPadding(new Insets(10, 0, 0, 0));
        
        Label lblWater = new Label("Vitesse de montée des eaux (m/h) :");
        lblWater.setTextFill(Color.web("#a0b2ce"));
        Slider waterSlider = new Slider(0, 5, 0.5);
        waterSlider.setShowTickLabels(true);
        waterSlider.setStyle("-fx-text-fill: white;");

        Label lblInterval = new Label("Pas temporel du moteur (ms) :");
        lblInterval.setTextFill(Color.web("#a0b2ce"));
        Slider intervalSlider = new Slider(100, 2000, 500);
        intervalSlider.setShowTickLabels(true);

        slidersBox.getChildren().addAll(lblWater, waterSlider, lblInterval, intervalSlider);
        enginePanel.getChildren().addAll(engineTitle, controlButtons, slidersBox);

        // Panneau d'injection de masse (Graphe et Population)
        VBox injectionPanel = new VBox(15);
        injectionPanel.setPrefWidth(450);
        injectionPanel.setPadding(new Insets(20));
        injectionPanel.setStyle("-fx-background-color: #0f1c30; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.08);");

        Label injectTitle = new Label("📦 Injection Réseau & Population");
        injectTitle.setTextFill(Color.WHITE);
        injectTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        VBox injectForm = new VBox(12);
        
        HBox rowNodes = new HBox(10);
        TextField tfNodes = createStyledTextField("Nombre de nœuds");
        Button btnNodes = new Button("Générer Graphe");
        btnNodes.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        HBox.setHgrow(tfNodes, Priority.ALWAYS);
        rowNodes.getChildren().addAll(tfNodes, btnNodes);

        HBox rowAgents = new HBox(10);
        TextField tfAgents = createStyledTextField("Nombre d'agents");
        Button btnAgents = new Button("Injecter Pop.");
        btnAgents.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        HBox.setHgrow(tfAgents, Priority.ALWAYS);
        rowAgents.getChildren().addAll(tfAgents, btnAgents);

        injectForm.getChildren().addAll(rowNodes, rowAgents);
        injectionPanel.getChildren().addAll(injectTitle, injectForm);

        panelsRow.getChildren().addAll(enginePanel, injectionPanel);
        contentArea.getChildren().add(panelsRow);

        this.setCenter(contentArea);
    }

    // Helper Boutons Sidebar
    private Button createSidebarButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 15, 12, 15));
        if (active) {
            btn.setStyle("-fx-background-color: #0b5cbf; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-background-radius: 6; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: white; -fx-background-radius: 6;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-background-radius: 6;"));
        }
        return btn;
    }

    // Helper KPI Cards
    private VBox createKpiCard(String title, String value, String colorHex) {
        VBox card = new VBox(5);
        card.setPrefWidth(215);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #0f1c30; -fx-background-radius: 6; -fx-border-color: rgba(255,255,255,0.05);");
        
        Label lblTitle = new Label(title);
        lblTitle.setTextFill(Color.web("#718096"));
        lblTitle.setFont(Font.font("System", 12));
        
        Label lblValue = new Label(value);
        lblValue.setTextFill(Color.web(colorHex));
        lblValue.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }

    // Helper Boutons d'Action Moteur
    private Button createControlButton(String text, String colorHex) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 4; -fx-cursor: hand;");
        return btn;
    }

    // Helper Input Text stylisé
    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #14233c; -fx-text-fill: white; -fx-prompt-text-fill: #4a5568; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        return tf;
    }
}