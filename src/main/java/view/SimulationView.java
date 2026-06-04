package view;

import controller.SimulationController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import model.simulation.SimulationInondation;

/**
 * Vue principale de la simulation d'inondation.
 * S'insère dans la zone center du BorderPane parent (la sidebar est gérée
 * ailleurs).
 * Fond blanc, layout autonome : stats + contrôles + carte + alertes/log.
 *
 * Méthodes à ajouter dans SimulationInondation :
 * + isEnPause() : boolean
 * + getTempsEcoule() : double (secondes simulées)
 * + getNombreAgents() : int
 * + getNombreAgentsEvacues() : int
 * + getNombreZonesInondees() : int
 * + setNiveauEau(double v)
 * + setGravite(double g)
 * + resetSimulation()
 *
 * Champs statiques à ajouter dans app.Main :
 * public static SimulationController simulationController;
 * public static ListView<String> alertesListView;
 * public static TextArea logArea;
 */
public class SimulationView extends BorderPane {

    // ── Contrôleur & modèle ────────────────────────────────────────────────
    private SimulationController controller;
    private SimulationInondation modele;

    public SimulationView(SimulationController controller) {
        this.setStyle("-fx-background-color: white;");
        this.controller = controller;
        this.modele = (controller != null) ? controller.getModele() : null;
        buildContent();
        startRefreshLoop();
    }

    // ── Boutons de contrôle ────────────────────────────────────────────────
    private Button btnDemarrer;
    private Button btnPause;
    private Button btnReprendre;
    private Button btnPas;
    private Button btnReset;

    // ── Sliders ────────────────────────────────────────────────────────────
    private Slider sliderVitesse;
    private Slider sliderGravite;
    private Slider sliderNiveauEau;

    // ── Labels valeurs sliders ──────────────────────────────────────────────
    private Label lblVitesseVal;
    private Label lblGraviteVal;
    private Label lblNiveauEauVal;

    // ── Labels stats temps réel ─────────────────────────────────────────────
    private Label lblStatEtat;
    private Label lblStatTemps;
    private Label lblStatAgents;
    private Label lblStatEvacues;
    private Label lblStatZones;
    private Label lblStatNiveauEau;

    // ── Barre progression eau ────────────────────────────────────────────────
    private ProgressBar barreEau;

    // ── Conteneur carte ──────────────────────────────────────────────────────
    private StackPane carteContainer;
    private MapView mapView;

    // ── Timeline rafraîchissement ────────────────────────────────────────────
    private Timeline refreshTimeline;

    // ════════════════════════════════════════════════════════════════════════
    // CONSTRUCTION DU CONTENU
    // ════════════════════════════════════════════════════════════════════════
    private void buildContent() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white;");

        // ── 1. Barre de titre + boutons de contrôle ──
        root.getChildren().add(buildTopBar());

        // ── 2. Ligne de statistiques ──
        root.getChildren().add(buildStatsRow());

        // ── 3. Zone principale : carte | panneau droite ──
        HBox mainZone = new HBox(0);
        mainZone.setStyle("-fx-background-color: white;");
        VBox.setVgrow(mainZone, Priority.ALWAYS);

        carteContainer = buildCarteContainer();
        HBox.setHgrow(carteContainer, Priority.ALWAYS);

        VBox panneauDroit = buildPanneauDroit();
        panneauDroit.setPrefWidth(310);
        panneauDroit.setMinWidth(290);
        panneauDroit.setMaxWidth(310);

        mainZone.getChildren().addAll(carteContainer, panneauDroit);
        VBox.setVgrow(mainZone, Priority.ALWAYS);
        root.getChildren().add(mainZone);

        VBox.setVgrow(root, Priority.ALWAYS);
        this.setCenter(root);
    }

    // ════════════════════════════════════════════════════════════════════════
    // BARRE DE TITRE + CONTRÔLES SIMULATION
    // ════════════════════════════════════════════════════════════════════════
    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(18, 24, 14, 24));
        bar.setStyle(
                "-fx-background-color: white; -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

        // Titre
        VBox titreBox = new VBox(2);
        Label titre = new Label("Simulation d'Inondation");
        titre.setFont(Font.font("System", FontWeight.BOLD, 20));
        titre.setTextFill(Color.web("#1a202c"));
        Label sousTitre = new Label("Modélisation en temps réel");
        sousTitre.setFont(Font.font("System", 12));
        sousTitre.setTextFill(Color.web("#718096"));
        titreBox.getChildren().addAll(titre, sousTitre);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Boutons
        btnDemarrer = createCtrlButton("▶  Démarrer", "#27ae60", "#1e8449");
        btnPause = createCtrlButton("⏸  Pause", "#e67e22", "#ca6f1e");
        btnReprendre = createCtrlButton("▶  Reprendre", "#2980b9", "#1f618d");
        btnPas = createCtrlButton("⏭  Pas à pas", "#8e44ad", "#76389a");
        btnReset = createCtrlButton("↺  Reset", "#e74c3c", "#c0392b");

        btnPause.setDisable(true);
        btnReprendre.setDisable(true);

        btnDemarrer.setOnAction(e -> {
            if (controller != null)
                controller.demarrerSimulation();
            btnDemarrer.setDisable(true);
            btnPause.setDisable(false);
        });
        btnPause.setOnAction(e -> {
            if (controller != null)
                controller.mettreEnPause();
            btnPause.setDisable(true);
            btnReprendre.setDisable(false);
        });
        btnReprendre.setOnAction(e -> {
            if (controller != null)
                controller.reprendreSimulation();
            btnReprendre.setDisable(true);
            btnPause.setDisable(false);
        });
        btnPas.setOnAction(e -> {
            if (controller != null)
                controller.executerPas();
        });
        btnReset.setOnAction(e -> {
            if (controller != null)
                controller.resetSimulation();
            btnDemarrer.setDisable(false);
            btnPause.setDisable(true);
            btnReprendre.setDisable(true);
        });

        bar.getChildren().addAll(titreBox, spacer, btnDemarrer, btnPause, btnReprendre, btnPas, btnReset);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    // LIGNE DE STATISTIQUES (6 mini-cartes)
    // ════════════════════════════════════════════════════════════════════════
    private HBox buildStatsRow() {
        HBox row = new HBox(0);
        row.setStyle(
                "-fx-background-color: #f8fafc; -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");
        row.setPadding(new Insets(12, 24, 12, 24));
        row.setSpacing(12);

        lblStatEtat = new Label("ARRÊTÉE");
        lblStatTemps = new Label("00:00");
        lblStatAgents = new Label("0");
        lblStatEvacues = new Label("0");
        lblStatZones = new Label("0");
        lblStatNiveauEau = new Label("0.0 m");

        row.getChildren().addAll(
                createStatCard("État", lblStatEtat, "#95a5a6", "⚙"),
                createStatCard("Temps simulé", lblStatTemps, "#3498db", "⏱"),
                createStatCard("Agents actifs", lblStatAgents, "#2ecc71", "👥"),
                createStatCard("Évacués", lblStatEvacues, "#27ae60", "✅"),
                createStatCard("Zones inondées", lblStatZones, "#e74c3c", "🌊"),
                createStatCard("Niveau eau", lblStatNiveauEau, "#1565c0", "💧"));

        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONTENEUR CARTE
    // ════════════════════════════════════════════════════════════════════════
    private StackPane buildCarteContainer() {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: #eef2f7;");
        pane.setMinHeight(420);
        pane.setPrefHeight(520);

        // Initialiser la carte avec les zones du modèle
        if (modele != null) {
            mapView = new MapView(modele.getZones());
            modele.addZoneUpdateListener(mapView);
            pane.getChildren().add(mapView.getWebView());
        } else {
            // Fallback : placeholder
            VBox placeholder = new VBox(12);
            placeholder.setAlignment(Pos.CENTER);

            Label icone = new Label("🗺️");
            icone.setFont(Font.font("System", 48));

            Label lbl = new Label("Zone d'affichage de la carte");
            lbl.setTextFill(Color.web("#718096"));
            lbl.setFont(Font.font("System", FontWeight.BOLD, 14));

            Label sub = new Label("Modèle non initialisé");
            sub.setTextFill(Color.web("#a0aec0"));
            sub.setFont(Font.font("System", 11));

            placeholder.getChildren().addAll(icone, lbl, sub);
            pane.getChildren().add(placeholder);
        }
        return pane;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PANNEAU DROIT : PARAMÈTRES + ALERTES + LOG
    // ════════════════════════════════════════════════════════════════════════
    private VBox buildPanneauDroit() {
        VBox panel = new VBox(0);
        panel.setStyle(
                "-fx-background-color: white; -fx-border-color: transparent transparent transparent #e2e8f0; -fx-border-width: 0 0 0 1;");

        // ScrollPane pour tout le contenu du panneau droit
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox inner = new VBox(0);
        inner.setStyle("-fx-background-color: white;");

        inner.getChildren().addAll(
                buildSectionParametres(),
                buildDivider(),
                buildSectionAlertes(),
                buildDivider(),
                buildSectionLog());

        scroll.setContent(inner);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        panel.getChildren().add(scroll);
        return panel;
    }

    /** Section "Paramètres de simulation" avec 3 sliders. */
    private VBox buildSectionParametres() {
        VBox section = new VBox(14);
        section.setPadding(new Insets(20, 20, 20, 20));

        Label titre = sectionTitle("⚙ Paramètres de simulation");
        section.getChildren().add(titre);

        // Slider vitesse
        sliderVitesse = new Slider(100, 2000, 500);
        lblVitesseVal = new Label("500 ms");
        sliderVitesse.valueProperty().addListener((obs, o, n) -> {
            lblVitesseVal.setText(n.intValue() + " ms");
            if (controller != null)
                controller.setVitesseSimulation(n.doubleValue());
        });
        section.getChildren().add(buildSliderBlock("⏱ Vitesse (ms/pas)", sliderVitesse, lblVitesseVal));

        // Slider gravité
        sliderGravite = new Slider(0.1, 3.0, 1.0);
        lblGraviteVal = new Label("1.0×");
        sliderGravite.valueProperty().addListener((obs, o, n) -> {
            double v = Math.round(n.doubleValue() * 10.0) / 10.0;
            lblGraviteVal.setText(v + "×");
            if (controller != null)
                controller.setGravite(v);
        });
        section.getChildren().add(buildSliderBlock("🌊 Gravité de propagation", sliderGravite, lblGraviteVal));

        // Slider niveau eau
        sliderNiveauEau = new Slider(0, 10, 0);
        lblNiveauEauVal = new Label("0.0 m");
        sliderNiveauEau.valueProperty().addListener((obs, o, n) -> {
            double v = Math.round(n.doubleValue() * 10.0) / 10.0;
            lblNiveauEauVal.setText(v + " m");
            if (controller != null)
                controller.setNiveauEau(v);
        });
        section.getChildren().add(buildSliderBlock("💧 Niveau eau initial (m)", sliderNiveauEau, lblNiveauEauVal));

        // Barre de progression eau globale
        Label lblProg = new Label("Niveau eau global");
        lblProg.setTextFill(Color.web("#718096"));
        lblProg.setFont(Font.font("System", 11));

        barreEau = new ProgressBar(0.0);
        barreEau.setMaxWidth(Double.MAX_VALUE);
        barreEau.setPrefHeight(10);
        barreEau.setStyle("-fx-accent: #3498db; -fx-background-radius: 5; -fx-background-color: #e2e8f0;");

        section.getChildren().addAll(lblProg, barreEau);

        return section;
    }

    /** Section "Alertes en cours". */
    private VBox buildSectionAlertes() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(20, 20, 20, 20));

        Label titre = sectionTitle("⚠ Alertes en cours");

        ListView<String> listeAlertes = new ListView<>();
        listeAlertes.setPrefHeight(150);
        listeAlertes.setStyle(
                "-fx-background-color: #fff8f8;" +
                        "-fx-border-color: #ffe0e0;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 11px;");
        listeAlertes.getItems().addAll("Aucune alerte active", "En attente de démarrage...");

        section.getChildren().addAll(titre, listeAlertes);
        return section;
    }

    /** Section "Journal d'événements". */
    private VBox buildSectionLog() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(20, 20, 20, 20));

        Label titre = sectionTitle("📋 Journal d'événements");

        TextArea logArea = new TextArea("[Système] Simulateur prêt.\n[Système] Paramétrez et lancez.");
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setStyle(
                "-fx-control-inner-background: #f8fafc;" +
                        "-fx-font-family: monospace;" +
                        "-fx-font-size: 11px;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;");

        section.getChildren().addAll(titre, logArea);
        return section;
    }

    // ════════════════════════════════════════════════════════════════════════
    // BOUCLE DE RAFRAÎCHISSEMENT UI
    // ════════════════════════════════════════════════════════════════════════
    private void startRefreshLoop() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.millis(500), e -> actualiserStatistiques()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void actualiserStatistiques() {
        if (modele == null)
            return;

        boolean enPause = modele.isEnPause();
        lblStatEtat.setText(enPause ? "EN PAUSE" : "EN COURS");
        lblStatEtat.setTextFill(enPause ? Color.web("#e67e22") : Color.web("#27ae60"));

        int secs = (int) modele.getTempsEcoule();
        lblStatTemps.setText(String.format("%02d:%02d", secs / 60, secs % 60));
        lblStatAgents.setText(String.valueOf(modele.getNombreAgents()));
        lblStatEvacues.setText(String.valueOf(modele.getNombreAgentsEvacues()));
        lblStatZones.setText(String.valueOf(modele.getNombreZonesInondees()));

        double nv = modele.getNiveauEau();
        lblStatNiveauEau.setText(String.format("%.1f m", nv));

        double norm = Math.min(nv / 10.0, 1.0);
        barreEau.setProgress(norm);
        String couleur = norm < 0.33 ? "#27ae60" : norm < 0.66 ? "#e67e22" : "#e74c3c";
        barreEau.setStyle("-fx-accent: " + couleur + "; -fx-background-color: #e2e8f0; -fx-background-radius: 5;");
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACCESSEURS PUBLICS
    // ════════════════════════════════════════════════════════════════════════

    /** Injecter MapView : getCarteContainer().getChildren().add(mapView); */
    public StackPane getCarteContainer() {
        return carteContainer;
    }

    /** Stopper le rafraîchissement à la fermeture de la vue. */
    public void stopRefresh() {
        if (refreshTimeline != null)
            refreshTimeline.stop();
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILITAIRES UI
    // ════════════════════════════════════════════════════════════════════════

    private Button createCtrlButton(String text, String bg, String hover) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 14 8 14;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hover + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 14 8 14;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 14 8 14;"));
        return btn;
    }

    private VBox createStatCard(String title, Label valueLabel, String colorHex, String icon) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 8;");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setFont(Font.font("System", 13));
        Label lbl = new Label(title);
        lbl.setTextFill(Color.web("#718096"));
        lbl.setFont(Font.font("System", 11));
        header.getChildren().addAll(ico, lbl);

        valueLabel.setTextFill(Color.web(colorHex));
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        card.getChildren().addAll(header, valueLabel);
        return card;
    }

    private VBox buildSliderBlock(String labelText, Slider slider, Label valLabel) {
        VBox block = new VBox(5);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.setTextFill(Color.web("#4a5568"));
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        valLabel.setTextFill(Color.web("#0b1a30"));
        valLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        header.getChildren().addAll(lbl, sp, valLabel);

        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setBlockIncrement(0.1);
        slider.setStyle("-fx-control-inner-background: #e2e8f0; -fx-accent: #0b5cbf;");

        block.getChildren().addAll(header, slider);
        return block;
    }

    private Label sectionTitle(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#2d3748"));
        return lbl;
    }

    private Rectangle buildDivider() {
        Rectangle div = new Rectangle();
        div.setHeight(1);
        div.setFill(Color.web("#e2e8f0"));
        div.widthProperty().bind(this.widthProperty());
        return div;
    }
}