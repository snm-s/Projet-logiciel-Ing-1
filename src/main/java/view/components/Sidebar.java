package view.components;

import app.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class Sidebar extends VBox {
    
    public Sidebar(String role) {
        // Fond Slate 900 (Ultra moderne)
        this.setStyle("-fx-background-color: #0F172A; -fx-spacing: 8;");
        this.setPrefWidth(240);
        this.setPadding(new Insets(30, 15, 30, 15));
        this.setAlignment(Pos.TOP_CENTER);

        // Logo / Titre Application
        Label logoLabel = new Label("🌊 FLOOD APP");
        logoLabel.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 18px; -fx-font-weight: 900; -fx-letter-spacing: 1px;");
        
        // Séparateur subtil
        VBox line = new VBox();
        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: #1E293B; -fx-margin-top: 10; -fx-margin-bottom: 20;");
        
        this.getChildren().addAll(logoLabel, line);

        // --- MENUS CITOYEN ---
        if (role.equals("Citizen")) {
            addModernButton("🏠  Tableau de bord", true, () -> Main.showCitizenView());
            addModernButton("🗺️  Carte de la ville", false, () -> {});
            addModernButton("⎵  Mes trajets", false, () -> {});
            addModernButton("🔔  Alertes", false, () -> {});
            addModernButton("🏥  Refuges", false, () -> {});
            addModernButton("👤  Profil", false, () -> {});
            addModernButton("⚙️  Paramètres", false, () -> {});
        } 
        // --- MENUS SECOURS ---
        else if (role.equals("Rescue")) {
            addModernButton("📊  Tableau de bord", true, () -> Main.showRescueView());
            addModernButton("🗺️  Carte globale", false, () -> {});
            addModernButton("👥  Agents déployés", false, () -> {});
            addModernButton("🚨  Gestion Alertes", false, () -> {});
            addModernButton("🚒  Missions", false, () -> {});
            addModernButton("📈  Statistiques", false, () -> {});
        }

        // Pousse la déconnexion vers le bas
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        this.getChildren().add(spacer);

        // Bouton Déconnexion Premium
        Button btnLogout = new Button("🚪  Déconnexion");
        btnLogout.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-background-radius: 8; -fx-pref-width: 210; -fx-pref-height: 40; -fx-cursor: hand;");
        btnLogout.setOnAction(e -> Main.showWelcomeView());
        this.getChildren().add(btnLogout);
    }

    private void addModernButton(String text, boolean isActive, Runnable action) {
        Button btn = new Button(text);
        
        // Style de base (Inactif)
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-alignment: CENTER_LEFT; " +
                            "-fx-pref-width: 210; -fx-pref-height: 42; -fx-font-size: 13px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 8; -fx-padding: 0 0 0 15; -fx-cursor: hand;";
        
        // Style si c'est l'onglet actif (Bleu surbrillance comme sur ta maquette)
        String activeStyle = "-fx-background-color: #38BDF8; -fx-text-fill: #0F172A; -fx-alignment: CENTER_LEFT; " +
                             "-fx-pref-width: 210; -fx-pref-height: 42; -fx-font-size: 13px; -fx-font-weight: 900; " +
                             "-fx-background-radius: 8; -fx-padding: 0 0 0 15; -fx-cursor: hand;";

        btn.setStyle(isActive ? activeStyle : baseStyle);

        if (!isActive) {
            // Effets de survol (Hover) fluides
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #F1F5F9; -fx-alignment: CENTER_LEFT; -fx-pref-width: 210; -fx-pref-height: 42; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 0 0 0 15;"));
            btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        }

        btn.setOnAction(e -> action.run());
        this.getChildren().add(btn);
    }
}
