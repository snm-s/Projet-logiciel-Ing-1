package view;

import app.Main;
import controller.SimulationController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * SimulationView — Vue de simulation d'inondation pour l'administrateur.
 *
 * Permet de :
 *  - Lancer / Mettre en pause / Reprendre / Réinitialiser la simulation
 *  - Configurer la vitesse de montée des eaux et la hauteur maximale
 *  - Observer en temps réel : carte, capteurs, population, secouristes
 *
 * Thème : #0b1a30 (fond sombre), #0b5cbf (accent bleu), cohérent avec CitizenDashboardView.
 */
public class SimulationView extends BorderPane {

    // ── Référence contrôleur ────────────────────────────────────────────────
    private final SimulationController controller;

    // ── Composants UI état simulation ───────────────────────────────────────
    private Label lblStatut;
    private Label lblTempsSimulation;
    private Label lblNiveauEau;
    private Label lblPopulationEvacuee;
    private Label lblSecouristesActifs;
    private Label lblCapteursMoyenne;

    // ── Contrôles paramètres ────────────────────────────────────────────────
    private Slider sliderVitesseMontee;
    private Slider sliderHauteurMax;
    private Label lblVitesseMonteeVal;
    private Label lblHauteurMaxVal;
    private ComboBox<String> comboScenario;

    // ── Boutons ─────────────────────────────────────────────────────────────
    private Button btnLancer;
    private Button btnPause;
    private Button btnReprendre;
    private Button btnReset;

    // ── Carte canvas ────────────────────────────────────────────────────────
    private Canvas canvasCarte;
    private GraphicsContext gc;

    // ── Log console ─────────────────────────────────────────────────────────
    private TextArea logConsole;

    // ── Timeline affichage ──────────────────────────────────────────────────
    private Timeline uiRefreshTimeline;

    // ── Simulation locale (données visuelles mock) ──────────────────────────
    private double niveauEauActuel   = 0.0;
    private int    secondesEcoulees  = 0;
    private int    populationEvacuee = 0;
    private int    secouristesActifs = 4;
    private final Random rng = new Random();

    // ── Capteurs (nœuds graphiques simulés) ─────────────────────────────────
    private final double[] capteursX   = {80, 160, 240, 310, 390, 470};
    private final double[] capteursY   = {90, 60,  130, 70,  110, 80};
    private final double[] capteursNiv = new double[6];
    private final String[] capteursId  = {"C-01","C-02","C-03","C-04","C-05","C-06"};

    // ── Nœuds (sommets graphe) ───────────────────────────────────────────────
    private final double[] nodesX = {80, 160, 240, 310, 390, 470, 130, 290, 420};
    private final double[] nodesY = {90, 60,  130, 70,  110, 80,  200, 190, 200};

    // ── Arêtes (liaisons entre nœuds) ────────────────────────────────────────
    private final int[][] aretes = {{0,1},{1,2},{2,3},{3,4},{4,5},{0,6},{2,7},{4,8},{6,7},{7,8}};

    // ── Agents (positions visuelles) ─────────────────────────────────────────
    private final double[] agentsX = {95,  175, 260, 135, 300, 430, 285, 410};
    private final double[] agentsY = {100, 70,  80,  210, 200, 115, 75,  210};
    private final boolean[] agentsSauves = new boolean[8];

    // ───────────────────────────────────────────────────────────────────────
    public SimulationView(SimulationController controller) {
        this.controller = controller;
        this.setStyle("-fx-background-color: #0b1a30;");

        buildLayout();
        bindActions();
        initUiRefreshLoop();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCTION LAYOUT
    // ══════════════════════════════════════════════════════════════════════

    private void buildLayout() {
        this.setLeft(buildSidebar());
        this.setCenter(buildCenterArea());
        this.setRight(buildRightPanel());
    }

    // ── SIDEBAR GAUCHE ──────────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setStyle("-fx-background-color: #06111f;");
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setSpacing(6);

        // En-tête
        Label titleLabel = new Label("⚙  Simulation");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label subTitle = new Label("Panneau de contrôle");
        subTitle.setTextFill(Color.web("#a0b2ce"));
        subTitle.setFont(Font.font("System", 11));
        subTitle.setPadding(new Insets(0, 0, 15, 0));

        sidebar.getChildren().addAll(titleLabel, subTitle);

        // Séparateur
        sidebar.getChildren().add(buildSep());

        // ── Section Scénario ────────────────────────────────────────────
        sidebar.getChildren().add(buildSectionLabel("Scénario"));

        comboScenario = new ComboBox<>();
        comboScenario.getItems().addAll(
            "Crue rapide (urbaine)",
            "Montée lente (rural)",
            "Rupture de digue",
            "Pluies torrentielles"
        );
        comboScenario.setValue("Crue rapide (urbaine)");
        comboScenario.setMaxWidth(Double.MAX_VALUE);
        comboScenario.setStyle(
            "-fx-background-color: #0f2540; -fx-text-fill: white;" +
            "-fx-border-color: #1a3a5c; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        sidebar.getChildren().add(comboScenario);

        // ── Section Paramètres eau ───────────────────────────────────────
        sidebar.getChildren().add(buildSep());
        sidebar.getChildren().add(buildSectionLabel("💧 Montée des eaux"));

        // Vitesse montée
        sidebar.getChildren().add(buildParamLabel("Vitesse de montée (m/h)"));
        HBox rowVitesse = new HBox(8);
        rowVitesse.setAlignment(Pos.CENTER_LEFT);
        sliderVitesseMontee = buildSlider(0.1, 5.0, 1.0);
        lblVitesseMonteeVal = buildSliderValueLabel("1.0");
        sliderVitesseMontee.valueProperty().addListener((obs, o, n) -> {
            double v = Math.round(n.doubleValue() * 10.0) / 10.0;
            lblVitesseMonteeVal.setText(v + "");
            //controller.setVitesseMonteeEau(v);
        });
        HBox.setHgrow(sliderVitesseMontee, Priority.ALWAYS);
        rowVitesse.getChildren().addAll(sliderVitesseMontee, lblVitesseMonteeVal);
        sidebar.getChildren().add(rowVitesse);

        // Hauteur max
        sidebar.getChildren().add(buildParamLabel("Hauteur maximale (m)"));
        HBox rowHauteur = new HBox(8);
        rowHauteur.setAlignment(Pos.CENTER_LEFT);
        sliderHauteurMax = buildSlider(0.5, 10.0, 3.0);
        lblHauteurMaxVal = buildSliderValueLabel("3.0");
        sliderHauteurMax.valueProperty().addListener((obs, o, n) -> {
            double v = Math.round(n.doubleValue() * 10.0) / 10.0;
            lblHauteurMaxVal.setText(v + "");
            //controller.setHauteurMaxEau(v);
        });
        HBox.setHgrow(sliderHauteurMax, Priority.ALWAYS);
        rowHauteur.getChildren().addAll(sliderHauteurMax, lblHauteurMaxVal);
        sidebar.getChildren().add(rowHauteur);

        // ── Section Durée ────────────────────────────────────────────────
        sidebar.getChildren().add(buildSep());
        sidebar.getChildren().add(buildSectionLabel("⏱ Durée simulation"));

        Label lblDureeHint = new Label("La simulation tourne en continu\njusqu'à l'arrêt manuel.");
        lblDureeHint.setTextFill(Color.web("#607a9e"));
        lblDureeHint.setFont(Font.font("System", 10));
        lblDureeHint.setWrapText(true);
        sidebar.getChildren().add(lblDureeHint);

        // ── Section Statut ────────────────────────────────────────────────
        sidebar.getChildren().add(buildSep());
        sidebar.getChildren().add(buildSectionLabel("Statut"));

        lblStatut = new Label("● Arrêtée");
        lblStatut.setTextFill(Color.web("#718096"));
        lblStatut.setFont(Font.font("System", FontWeight.BOLD, 13));
        sidebar.getChildren().add(lblStatut);

        lblTempsSimulation = new Label("00:00:00");
        lblTempsSimulation.setTextFill(Color.web("#a0b2ce"));
        lblTempsSimulation.setFont(Font.font("Monospace", 20));
        sidebar.getChildren().add(lblTempsSimulation);

        // Espaceur
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // ── Boutons d'action ──────────────────────────────────────────────
        btnLancer    = buildActionButton("▶  Lancer",    "#0b5cbf", true);
        btnPause     = buildActionButton("⏸  Pause",     "#d97706", false);
        btnReprendre = buildActionButton("▶  Reprendre", "#059669", false);
        btnReset     = buildActionButton("↺  Réinitialiser", "#6b2737", false);

        btnPause.setDisable(true);
        btnReprendre.setDisable(true);

        sidebar.getChildren().addAll(btnLancer, btnPause, btnReprendre, btnReset);

        return sidebar;
    }

    // ── ZONE CENTRALE (carte + stats) ───────────────────────────────────────
    private VBox buildCenterArea() {
        VBox center = new VBox(15);
        center.setPadding(new Insets(20));

        // Titre section
        Label titleCarte = new Label("🗺  Carte en temps réel");
        titleCarte.setTextFill(Color.WHITE);
        titleCarte.setFont(Font.font("System", FontWeight.BOLD, 15));

        // Canvas carte
        canvasCarte = new Canvas(560, 290);
        gc = canvasCarte.getGraphicsContext2D();
        StackPane carteWrapper = new StackPane(canvasCarte);
        carteWrapper.setStyle(
            "-fx-background-color: #0a1929; -fx-background-radius: 10;" +
            "-fx-border-color: #1a3a5c; -fx-border-radius: 10;"
        );
        drawCarteInitiale();

        // Légende carte
        HBox legende = new HBox(20);
        legende.setAlignment(Pos.CENTER_LEFT);
        legende.getChildren().addAll(
            buildLegendItem("#4299e1", "Eau"),
            buildLegendItem("#48bb78", "Nœud sûr"),
            buildLegendItem("#fc8181", "Nœud inondé"),
            buildLegendItem("#f6e05e", "Agent évacué"),
            buildLegendItem("#fc8181", "Secouriste"),
            buildLegendItem("#a0aec0", "Capteur")
        );

        // Cartes statistiques inférieures
        HBox statsRow = buildStatsRow();

        center.getChildren().addAll(titleCarte, carteWrapper, legende, statsRow);
        return center;
    }

    private HBox buildStatsRow() {
        lblPopulationEvacuee = new Label("0");
        lblSecouristesActifs  = new Label("4");
        lblCapteursMoyenne    = new Label("0.00 m");
        lblNiveauEau          = new Label("0.00 m");

        HBox row = new HBox(12);
        row.getChildren().addAll(
            buildStatCard("👥 Population évacuée", lblPopulationEvacuee, "#3182ce"),
            buildStatCard("🚑 Secouristes actifs",  lblSecouristesActifs,  "#38a169"),
            buildStatCard("📡 Moy. capteurs",        lblCapteursMoyenne,    "#d97706"),
            buildStatCard("💧 Niveau eau actuel",    lblNiveauEau,          "#2b6cb0")
        );
        return row;
    }

    // ── PANNEAU DROIT (capteurs + log) ──────────────────────────────────────
    private VBox buildRightPanel() {
        VBox panel = new VBox(14);
        panel.setPrefWidth(260);
        panel.setPadding(new Insets(20, 15, 20, 5));

        // Capteurs
        Label titleCapteurs = new Label("📡  Capteurs d'eau");
        titleCapteurs.setTextFill(Color.WHITE);
        titleCapteurs.setFont(Font.font("System", FontWeight.BOLD, 14));

        VBox capteursPanel = buildCapteursPanel();

        // Console log
        Label titleLog = new Label("📋  Journal simulation");
        titleLog.setTextFill(Color.WHITE);
        titleLog.setFont(Font.font("System", FontWeight.BOLD, 14));

        logConsole = new TextArea();
        logConsole.setEditable(false);
        logConsole.setPrefHeight(220);
        logConsole.setStyle(
            "-fx-control-inner-background: #060f1c;" +
            "-fx-text-fill: #a0b2ce;" +
            "-fx-font-family: Monospace;" +
            "-fx-font-size: 10px;" +
            "-fx-border-color: #1a3a5c; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        logMessage("Simulation prête. Configurez les paramètres et cliquez sur Lancer.");

        panel.getChildren().addAll(titleCapteurs, capteursPanel, titleLog, logConsole);
        VBox.setVgrow(logConsole, Priority.ALWAYS);
        return panel;
    }

    private VBox buildCapteursPanel() {
        VBox box = new VBox(6);
        box.setStyle(
            "-fx-background-color: #060f1c; -fx-background-radius: 8;" +
            "-fx-border-color: #1a3a5c; -fx-border-radius: 8; -fx-padding: 10;"
        );
        for (int i = 0; i < capteursId.length; i++) {
            box.getChildren().add(buildCapteurRow(i));
        }
        return box;
    }

    // ══════════════════════════════════════════════════════════════════════
    // BINDING ACTIONS BOUTONS
    // ══════════════════════════════════════════════════════════════════════

    private void bindActions() {
        btnLancer.setOnAction(e -> lancerSimulation());
        btnPause.setOnAction(e -> pauseSimulation());
        btnReprendre.setOnAction(e -> reprendreSimulation());
        btnReset.setOnAction(e -> reinitialiserSimulation());
    }

    private void lancerSimulation() {
        ///////////controller.demarrerSimulation();
        lblStatut.setText("● En cours");
        lblStatut.setTextFill(Color.web("#48bb78"));
        btnLancer.setDisable(true);
        btnPause.setDisable(false);
        btnReprendre.setDisable(true);
        uiRefreshTimeline.play();
        logMessage("▶ Simulation lancée — Scénario : " + comboScenario.getValue());
        logMessage("  Vitesse montée : " + sliderVitesseMontee.getValue() + " m/h");
        logMessage("  Hauteur max    : " + sliderHauteurMax.getValue() + " m");
    }

    private void pauseSimulation() {
        ///////////controller.mettreEnPause();
        lblStatut.setText("⏸ En pause");
        lblStatut.setTextFill(Color.web("#d97706"));
        btnPause.setDisable(true);
        btnReprendre.setDisable(false);
        uiRefreshTimeline.pause();
        logMessage("⏸ Simulation mise en pause.");
    }

    private void reprendreSimulation() {
        ///////////controller.reprendreSimulation();
        lblStatut.setText("● En cours");
        lblStatut.setTextFill(Color.web("#48bb78"));
        btnPause.setDisable(false);
        btnReprendre.setDisable(true);
        uiRefreshTimeline.play();
        logMessage("▶ Simulation reprise.");
    }

    private void reinitialiserSimulation() {
        uiRefreshTimeline.stop();
        niveauEauActuel   = 0.0;
        secondesEcoulees  = 0;
        populationEvacuee = 0;
        secouristesActifs = 4;
        for (int i = 0; i < capteursNiv.length; i++) capteursNiv[i] = 0.0;
        for (int i = 0; i < agentsSauves.length; i++) agentsSauves[i] = false;

        lblStatut.setText("● Arrêtée");
        lblStatut.setTextFill(Color.web("#718096"));
        lblTempsSimulation.setText("00:00:00");
        lblNiveauEau.setText("0.00 m");
        lblPopulationEvacuee.setText("0");
        lblSecouristesActifs.setText("4");
        lblCapteursMoyenne.setText("0.00 m");

        btnLancer.setDisable(false);
        btnPause.setDisable(true);
        btnReprendre.setDisable(true);

        drawCarteInitiale();
        rafraichirCapteursUI();
        logMessage("↺ Simulation réinitialisée.");
    }

    // ══════════════════════════════════════════════════════════════════════
    // BOUCLE D'ACTUALISATION UI (toutes les 500 ms)
    // ══════════════════════════════════════════════════════════════════════

    private void initUiRefreshLoop() {
        uiRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> tickUI()));
        uiRefreshTimeline.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Appelé 2 fois/s pendant la simulation pour mettre à jour l'affichage.
     * Dans l'application réelle, les valeurs proviennent du modèle via controller.
     */
    private void tickUI() {
        secondesEcoulees++;
        ///////////controller.executerPas();

        // ── Mise à jour du temps ────────────────────────────────────────
        int h = secondesEcoulees / 3600;
        int m = (secondesEcoulees % 3600) / 60;
        int s = secondesEcoulees % 60;
        lblTempsSimulation.setText(String.format("%02d:%02d:%02d", h, m, s));

        // ── Montée de l'eau ─────────────────────────────────────────────
        double vitesse    = sliderVitesseMontee.getValue();
        double hauteurMax = sliderHauteurMax.getValue();
        double increment  = (vitesse / 3600.0) * 0.5; // par tick 500ms
        niveauEauActuel   = Math.min(niveauEauActuel + increment, hauteurMax);
        lblNiveauEau.setText(String.format("%.2f m", niveauEauActuel));

        // ── Capteurs (simulation locale) ────────────────────────────────
        for (int i = 0; i < capteursNiv.length; i++) {
            capteursNiv[i] = niveauEauActuel * (0.7 + rng.nextDouble() * 0.6);
            capteursNiv[i] = Math.min(capteursNiv[i], hauteurMax);
        }
        double moyenneCapteurs = 0;
        for (double v : capteursNiv) moyenneCapteurs += v;
        moyenneCapteurs /= capteursNiv.length;
        lblCapteursMoyenne.setText(String.format("%.2f m", moyenneCapteurs));

        // ── Population évacuée ──────────────────────────────────────────
        if (rng.nextInt(4) == 0 && populationEvacuee < 120) {
            int delta = rng.nextInt(3) + 1;
            populationEvacuee += delta;
            lblPopulationEvacuee.setText(String.valueOf(populationEvacuee));
            for (int i = 0; i < agentsSauves.length; i++) {
                if (!agentsSauves[i] && rng.nextBoolean()) {
                    agentsSauves[i] = true;
                    break;
                }
            }
        }

        // ── Sécouristes ─────────────────────────────────────────────────
        if (rng.nextInt(10) == 0) {
            secouristesActifs = Math.min(secouristesActifs + 1, 12);
            lblSecouristesActifs.setText(String.valueOf(secouristesActifs));
        }

        // ── Carte + Capteurs UI ─────────────────────────────────────────
        drawCarteAvecEau();
        rafraichirCapteursUI();

        // ── Logs périodiques ────────────────────────────────────────────
        if (secondesEcoulees % 6 == 0) {
            logMessage(String.format("[%02d:%02d] 💧 Niveau : %.2f m | Évacués : %d | Capteur moy : %.2f m",
                m, s, niveauEauActuel, populationEvacuee, moyenneCapteurs));
        }

        // ── Alerte seuil ────────────────────────────────────────────────
        if (niveauEauActuel >= 1.5 && (secondesEcoulees % 20 == 0)) {
            logMessage("⚠ ALERTE : Niveau critique dépassé (" + String.format("%.2f", niveauEauActuel) + " m)");
        }

        // ── Fin atteinte ────────────────────────────────────────────────
        if (niveauEauActuel >= hauteurMax) {
            pauseSimulation();
            logMessage("✅ Hauteur maximale atteinte. Simulation terminée automatiquement.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // RENDU CANVAS CARTE
    // ══════════════════════════════════════════════════════════════════════

    private void drawCarteInitiale() {
        drawCarteAvecEau();
    }

    private void drawCarteAvecEau() {
        double w = canvasCarte.getWidth();
        double h = canvasCarte.getHeight();
        double hauteurMax = sliderHauteurMax != null ? sliderHauteurMax.getValue() : 3.0;
        double ratio = hauteurMax > 0 ? niveauEauActuel / hauteurMax : 0;

        // ── Fond ────────────────────────────────────────────────────────
        gc.setFill(Color.web("#0a1929"));
        gc.fillRect(0, 0, w, h);

        // ── Eau (rectangle remontant depuis le bas) ──────────────────────
        double eauHauteur = h * ratio;
        if (eauHauteur > 0) {
            LinearGradient eauGrad = new LinearGradient(
                0, h - eauHauteur, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a4a7a", 0.7)),
                new Stop(1, Color.web("#0a2347", 0.9))
            );
            gc.setFill(eauGrad);
            gc.fillRect(0, h - eauHauteur, w, eauHauteur);

            // Ligne de surface eau
            gc.setStroke(Color.web("#4299e1", 0.8));
            gc.setLineWidth(1.5);
            gc.strokeLine(0, h - eauHauteur, w, h - eauHauteur);
        }

        // ── Grille de fond ───────────────────────────────────────────────
        gc.setStroke(Color.web("#0f2540", 0.6));
        gc.setLineWidth(0.5);
        for (int x = 0; x < w; x += 40) gc.strokeLine(x, 0, x, h);
        for (int y = 0; y < h; y += 40) gc.strokeLine(0, y, w, y);

        // ── Arêtes ───────────────────────────────────────────────────────
        for (int[] arete : aretes) {
            double x1 = nodesX[arete[0]]; double y1 = nodesY[arete[0]];
            double x2 = nodesX[arete[1]]; double y2 = nodesY[arete[1]];
            boolean inonde1 = isNodeInonde(arete[0], h, eauHauteur);
            boolean inonde2 = isNodeInonde(arete[1], h, eauHauteur);
            if (inonde1 || inonde2) {
                gc.setStroke(Color.web("#e53e3e", 0.6));
            } else {
                gc.setStroke(Color.web("#2d4a6a"));
            }
            gc.setLineWidth(1.5);
            gc.strokeLine(x1, y1, x2, y2);
        }

        // ── Nœuds ────────────────────────────────────────────────────────
        for (int i = 0; i < nodesX.length; i++) {
            boolean inonde = isNodeInonde(i, h, eauHauteur);
            gc.setFill(inonde ? Color.web("#fc8181") : Color.web("#48bb78"));
            gc.fillOval(nodesX[i] - 7, nodesY[i] - 7, 14, 14);
            gc.setStroke(Color.web("#0a1929"));
            gc.setLineWidth(1);
            gc.strokeOval(nodesX[i] - 7, nodesY[i] - 7, 14, 14);

            // Libellé nœud
            gc.setFill(Color.web("#a0b2ce"));
            gc.setFont(Font.font("Monospace", 8));
            gc.fillText("N" + i, nodesX[i] - 4, nodesY[i] + 20);
        }

        // ── Capteurs ──────────────────────────────────────────────────────
        for (int i = 0; i < capteursX.length; i++) {
            double cx = capteursX[i];
            double cy = capteursY[i];
            double niv = capteursNiv[i];
            double alerte = sliderHauteurMax != null ? sliderHauteurMax.getValue() * 0.7 : 2.1;
            Color c = niv > alerte ? Color.web("#fc8181") : Color.web("#a0aec0");
            gc.setFill(c);
            gc.fillRect(cx - 4, cy - 4, 8, 8);
            gc.setFill(Color.web("#718096"));
            gc.setFont(Font.font("Monospace", 8));
            gc.fillText(capteursId[i], cx - 8, cy + 16);
        }

        // ── Agents ────────────────────────────────────────────────────────
        for (int i = 0; i < agentsX.length; i++) {
            boolean sauve = agentsSauves[i];
            gc.setFill(sauve ? Color.web("#f6e05e") : Color.web("#90cdf4"));
            gc.fillOval(agentsX[i] - 4, agentsY[i] - 4, 9, 9);
        }

        // ── Légende eau ───────────────────────────────────────────────────
        gc.setFill(Color.web("#4299e1"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.fillText(String.format("Eau : %.2f m / %.1f m", niveauEauActuel,
            sliderHauteurMax != null ? sliderHauteurMax.getValue() : 3.0), 8, h - 8);
    }

    private boolean isNodeInonde(int idx, double canvasH, double eauHauteur) {
        double nodeY = nodesY[idx];
        return nodeY >= (canvasH - eauHauteur);
    }

    // ══════════════════════════════════════════════════════════════════════
    // RAFRAÎCHISSEMENT PANNEAU CAPTEURS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Reconstruit dynamiquement le panneau capteurs.
     * Dans une implémentation complète, on binderait directement les labels.
     */
    private void rafraichirCapteursUI() {
        // Les données capteursNiv[] sont lues directement dans buildCapteurRow
        // On met à jour via les labels stockés (ici, on rebind le VBox — simplifié).
        // Pour la vraie implémentation : stocker une List<Label> et la mettre à jour ici.
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS CONSTRUCTION UI
    // ══════════════════════════════════════════════════════════════════════

    private HBox buildCapteurRow(int index) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 5, 3, 5));

        Rectangle dot = new Rectangle(8, 8);
        dot.setFill(Color.web("#a0aec0"));
        dot.setArcWidth(2); dot.setArcHeight(2);

        Label lblId  = new Label(capteursId[index]);
        lblId.setTextFill(Color.web("#a0b2ce"));
        lblId.setFont(Font.font("Monospace", 11));
        lblId.setPrefWidth(40);

        Label lblNiv = new Label(String.format("%.2f m", capteursNiv[index]));
        lblNiv.setTextFill(Color.web("#4299e1"));
        lblNiv.setFont(Font.font("Monospace", FontWeight.BOLD, 11));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Barre de niveau
        StackPane barContainer = new StackPane();
        barContainer.setPrefSize(60, 8);
        barContainer.setStyle("-fx-background-color: #1a3a5c; -fx-background-radius: 4;");
        Rectangle barFill = new Rectangle(0, 8);
        barFill.setArcWidth(4); barFill.setArcHeight(4);
        barFill.setFill(Color.web("#4299e1"));
        barContainer.getChildren().add(barFill);
        barContainer.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(dot, lblId, lblNiv, spacer, barContainer);
        return row;
    }

    private VBox buildStatCard(String title, Label valueLabel, String colorHex) {
        VBox card = new VBox(5);
        card.setPrefWidth(130);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color: #0a1929; -fx-background-radius: 8;" +
            "-fx-border-color: #1a3a5c; -fx-border-radius: 8;"
        );
        Label lbl = new Label(title);
        lbl.setTextFill(Color.web("#718096"));
        lbl.setFont(Font.font("System", 10));
        lbl.setWrapText(true);
        valueLabel.setTextFill(Color.web(colorHex));
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        card.getChildren().addAll(lbl, valueLabel);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Button buildActionButton(String text, String bgColor, boolean primary) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(9, 12, 9, 12));
        btn.setFont(Font.font("System", FontWeight.BOLD, 12));
        btn.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white;" +
            "-fx-background-radius: 6; -fx-cursor: hand;", bgColor
        ));
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private Slider buildSlider(double min, double max, double val) {
        Slider s = new Slider(min, max, val);
        s.setStyle("-fx-control-inner-background: #1a3a5c;");
        return s;
    }

    private Label buildSliderValueLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#4299e1"));
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        l.setPrefWidth(35);
        return l;
    }

    private Label buildSectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setTextFill(Color.web("#4a6a9a"));
        l.setFont(Font.font("System", FontWeight.BOLD, 10));
        l.setPadding(new Insets(8, 0, 2, 0));
        return l;
    }

    private Label buildParamLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#a0b2ce"));
        l.setFont(Font.font("System", 11));
        return l;
    }

    private Separator buildSep() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1a3a5c;");
        sep.setPadding(new Insets(6, 0, 2, 0));
        return sep;
    }

    private HBox buildLegendItem(String colorHex, String label) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5, Color.web(colorHex));
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#718096"));
        lbl.setFont(Font.font("System", 10));
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    private void logMessage(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logConsole.appendText("[" + time + "] " + message + "\n");
    }

    // ══════════════════════════════════════════════════════════════════════
    // MÉTHODES PUBLIQUES APPELÉES PAR LE CONTRÔLEUR
    // ══════════════════════════════════════════════════════════════════════

    /** Met à jour le niveau d'eau affiché (appelé par SimulationController). */
    public void mettreAJourNiveauEau(double niveau) {
        niveauEauActuel = niveau;
        lblNiveauEau.setText(String.format("%.2f m", niveau));
    }

    /** Met à jour les données capteurs (appelé par SimulationController). */
    public void mettreAJourCapteurs(double[] niveaux) {
        System.arraycopy(niveaux, 0, capteursNiv, 0, Math.min(niveaux.length, capteursNiv.length));
        double moy = 0;
        for (double v : capteursNiv) moy += v;
        moy /= capteursNiv.length;
        lblCapteursMoyenne.setText(String.format("%.2f m", moy));
    }

    /** Met à jour la population évacuée (appelé par SimulationController). */
    public void mettreAJourPopulation(int evacues) {
        populationEvacuee = evacues;
        lblPopulationEvacuee.setText(String.valueOf(evacues));
    }

    /** Affiche un message d'alerte dans le journal (appelé par SimulationController). */
    public void afficherAlerte(String message) {
        logMessage("⚠ ALERTE : " + message);
    }

    /** Actualise l'affichage complet (appelé par SimulationController via mettreAJour). */
    public void actualiserAffichage() {
        drawCarteAvecEau();
        rafraichirCapteursUI();
    }
}
