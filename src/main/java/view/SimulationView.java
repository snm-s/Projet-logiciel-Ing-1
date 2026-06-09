package view;

import java.util.List;

import controller.AdminPage.SimulationController;
import controller.MapController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import model.simulation.FloodSimulation;
import model.zone.Zone;

/**
 * Vue principale de simulation d'inondation — dark dashboard.
 *
 * <h3>Corrections par rapport à la version précédente :</h3>
 * <ul>
 *   <li>MapView + MapController construits <em>avant</em> l'ajout au graphe de scène,
 *       puis {@code swingNode.setContent()} appelé depuis {@link javax.swing.SwingUtilities#invokeLater}
 *       au bon moment.</li>
 *   <li>MapController créé avec le constructeur complet (zones + agents).</li>
 *   <li>SimulationController reçoit le MapController via {@code setMapController()}.</li>
 *   <li>Le tick SimulationController est le seul point d'avancement
 *       (il appelle MapController.tick() en interne).</li>
 *   <li>Compteur d'agents en déplacement dans la barre du bas.</li>
 * </ul>
 */
public class SimulationView extends BorderPane {

    // ── Couleurs ──────────────────────────────────────────────────────────
    private static final String BG_DARK    = "#0d1117";
    private static final String BG_CARD    = "#131b2e";
    private static final String ACCENT_BLUE   = "#3b82f6";
    private static final String ACCENT_TEAL   = "#06b6d4";
    private static final String ACCENT_GREEN  = "#22c55e";
    private static final String ACCENT_ORANGE = "#f59e0b";
    private static final String ACCENT_RED    = "#ef4444";
    private static final String TEXT_PRIMARY  = "#f1f5f9";
    private static final String TEXT_MUTED    = "#94a3b8";
    private static final String BORDER_COLOR  = "#1e293b";

    // ── Modèle / Controllers ──────────────────────────────────────────────
    private final SimulationController controller;
    private final FloodSimulation      modele;
    private MapController              mapController;
    private MapView                    mapView;

    // ── Composants UI ────────────────────────────────────────────────────
    private Label lblSelectedZone, lblNiveauEauZone, lblStatutZone;
    private Label lblPopRisque, lblPersonnesSec, lblAgentsActifs;
    private Label lblAretesSures, lblAretesRisque, lblAretesInond, lblAretesCong, lblAretesOver;
    private Label lblRefugesTotal, lblRefugesAccess, lblRefugesInacc;
    private Label lblNiveauActuel, lblNiveauMax;
    private Label lblTempsRestant, lblZoneNiveaux;
    private Label lblTimer, lblSimStatus, lblVitesseVal;
    private Button btnPause, btnPlay, btnStop, btnAleatoire, btnManuelle;
    private Slider sliderVitesse;
    private ToggleButton[] zoneButtons;
    private int selectedZoneIndex = 0;

    // ── Timelines ──────────────────────────────────────────────────────────
    private Timeline refreshTimeline;
    private Timeline simTimeline;

    // ─────────────────────────────────────────────────────────────────────

    public SimulationView(SimulationController controller) {
        this.controller = controller;
        this.modele     = (controller != null) ? controller.getModele() : null;
        this.setStyle("-fx-background-color: " + BG_DARK + ";");
        buildContent();
        startRefreshLoop();
    }

    // ═════════════════════════════════════════════════════════════════════
    // CONSTRUCTION
    // ═════════════════════════════════════════════════════════════════════

    private void buildContent() {
        this.setTop(buildTopBar());
        this.setCenter(buildMapContainer());
        this.setBottom(buildBottomBar());
    }

    // ─────────────────────────────────────────────────────────────────────
    // BARRE SUPÉRIEURE
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: " + BG_CARD + ";"
            + "-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent;"
            + "-fx-border-width: 0 0 1 0;");
        bar.setPrefHeight(46);

        // Logo
        HBox logo = new HBox(10);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setPadding(new Insets(0, 20, 0, 16));
        logo.setPrefWidth(220);
        logo.setStyle("-fx-border-color: transparent " + BORDER_COLOR
            + " transparent transparent; -fx-border-width: 0 1 0 0;");
        Label ico = new Label("🏠");
        ico.setStyle("-fx-font-size:16px;");
        VBox titreBox = new VBox(1);
        titreBox.getChildren().addAll(
            styledLabel("Inondation",               FontWeight.BOLD,   13, TEXT_PRIMARY),
            styledLabel("Simulation & management",  FontWeight.NORMAL, 10, TEXT_MUTED));
        logo.getChildren().addAll(ico, titreBox);

        // Boutons mode
        HBox modeBox = new HBox(8);
        modeBox.setAlignment(Pos.CENTER);
        modeBox.setPadding(new Insets(0, 24, 0, 24));
        btnAleatoire = modeButton("⟳  Aléatoire", true);
        btnManuelle  = modeButton("↺  Manuelle",  false);
        btnAleatoire.setOnAction(e -> {
            setModeActive(btnAleatoire, btnManuelle);
            if (controller != null) { controller.setModeAleatoire(true); controller.demarrerSimulation(); startSimLoop(); }
        });
        btnManuelle.setOnAction(e -> {
            setModeActive(btnManuelle, btnAleatoire);
            if (controller != null) { controller.setModeAleatoire(false); controller.mettreEnPause(); stopSimLoop(); }
        });
        modeBox.getChildren().addAll(btnAleatoire, btnManuelle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(0, 20, 0, 0));
        lblSimStatus = styledLabel("Simulation en cours", FontWeight.NORMAL, 11, ACCENT_GREEN);
        lblTimer     = styledLabel("⏱  00:00:00",          FontWeight.BOLD,   12, TEXT_PRIMARY);
        statusBox.getChildren().addAll(lblSimStatus, lblTimer);

        bar.getChildren().addAll(logo, modeBox, spacer, statusBox);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTENEUR CARTE + OVERLAYS
    // ─────────────────────────────────────────────────────────────────────

    private StackPane buildMapContainer() {
        StackPane stack = new StackPane();
        stack.setStyle("-fx-background-color:#090e1a;");

        if (modele != null) {
            // ── Étape 1 : créer MapView (initialise JXMapViewer sur thread Swing en interne)
            mapView = new MapView(modele.getZones());
            mapView.setAgents(modele.getAgents());
            modele.addZoneUpdateListener(mapView);

            // ── Étape 2 : ajouter le SwingNode à la scène (doit être sur thread FX — on est déjà dessus)
            stack.getChildren().add(mapView.getSwingNode());

            // ── Étape 3 : créer MapController (construit le RouteGraph, injecte dans MapView)
            mapController = new MapController(mapView, modele.getZones(), modele.getAgents());

            // ── Étape 4 : injecter MapController dans SimulationController
            if (controller != null) controller.setMapController(mapController);

            mapView.setOnAgentSelected(agent -> {
                lblSelectedZone.setText(agent.getFirstName() != null ? agent.getFirstName() : "Agent #" + agent.getId());
                lblNiveauEauZone.setText("Agent sélectionné");
                lblStatutZone.setText(agent.getClass().getSimpleName());
                lblStatutZone.setTextFill(Color.web(ACCENT_TEAL));
            });

            // ── Étape 5 : callbacks
            mapController.setOnZoneSelected(zone -> {
                int idx = modele.getZones().indexOf(zone);
                if (idx >= 0) selectedZoneIndex = idx;
                lblSelectedZone.setText("Zone " + zone.getName().substring(0, Math.min(8, zone.getName().length())));
                lblNiveauEauZone.setText(String.format("%.2f m", modele.getNiveauEau()));
                boolean fl = zone.isFlooded(); double nv = modele.getNiveauEau();
                lblStatutZone.setText(fl ? "Inondée ⚠" : nv > 0.5 ? "En montée ↗" : "Stable →");
                lblStatutZone.setTextFill(Color.web(fl ? ACCENT_RED : nv > 0.5 ? ACCENT_ORANGE : ACCENT_GREEN));
            });
        }

        // ── Overlays par-dessus la carte ──────────────────────────────────
        VBox panelZone = buildZoneInfoPanel();
        StackPane.setAlignment(panelZone, Pos.TOP_LEFT);
        StackPane.setMargin(panelZone, new Insets(12, 0, 0, 12));

        VBox legendePanel = buildLegendePanel();
        StackPane.setAlignment(legendePanel, Pos.TOP_RIGHT);
        StackPane.setMargin(legendePanel, new Insets(12, 12, 0, 0));

        stack.getChildren().addAll(panelZone, legendePanel);

        // ── Callbacks SimulationController → UI ───────────────────────────
        if (controller != null) {
            controller.setOnStatusChanged(s -> lblSimStatus.setText(s));
            controller.setOnWaterLevelChanged(l -> lblNiveauActuel.setText(String.format("%.2f m", l)));
            controller.setOnZonesUpdated(zs -> { if (mapView != null) mapView.updateAllZones(zs); });
        }

        return stack;
    }

    // ─── Panel info zone ─────────────────────────────────────────────────

    private VBox buildZoneInfoPanel() {
        VBox p = cardPanel(180);
        lblSelectedZone  = styledLabel("Zone —",      FontWeight.BOLD,   15, TEXT_PRIMARY);
        lblNiveauEauZone = styledLabel("—",           FontWeight.BOLD,   11, ACCENT_BLUE);
        lblStatutZone    = styledLabel("Stable →",    FontWeight.BOLD,   11, ACCENT_GREEN);

        p.getChildren().addAll(
            styledLabel("Zone sélectionnée", FontWeight.NORMAL, 10, TEXT_MUTED),
            lblSelectedZone,
            hrow("Niveau d'eau", lblNiveauEauZone),
            hrow("Statut",       lblStatutZone));
        return p;
    }

    // ─── Légende + contrôles ─────────────────────────────────────────────

    private VBox buildLegendePanel() {
        VBox p = cardPanel(190);

        p.getChildren().add(styledLabel("LÉGENDE", FontWeight.BOLD, 10, TEXT_MUTED));
        p.getChildren().add(colorBar(ACCENT_GREEN,  "Arête sûre"));
        p.getChildren().add(colorBar(ACCENT_ORANGE, "Arête à risque"));
        p.getChildren().add(colorBar("#eab308",     "Congestionnée")); // Jaune/Orange vif
        p.getChildren().add(colorBar("#b91c1c",     "Surchargée"));    // Rouge foncé
        p.getChildren().add(colorBar(ACCENT_RED,    "Arête inondée"));

        p.getChildren().add(separator());
        p.getChildren().add(styledLabel("NIVEAU D'EAU", FontWeight.BOLD, 10, TEXT_MUTED));
        for (String[] n : new String[][]{
            {"#bfdbfe","0–0.5 m"},{"#60a5fa","0.5–1 m"},
            {"#3b82f6","1–1.5 m"},{"#1d4ed8","1.5–2 m"},{"#1e3a8a","> 2 m"}
        }) p.getChildren().add(colorBar(n[0], n[1]));

        p.getChildren().add(separator());
        p.getChildren().add(styledLabel("ZONES", FontWeight.BOLD, 10, TEXT_MUTED));
        p.getChildren().add(buildZoneGrid());

        p.getChildren().add(separator());
        p.getChildren().add(styledLabel("CONTRÔLES", FontWeight.BOLD, 10, TEXT_MUTED));
        p.getChildren().add(buildControlButtons());
        p.getChildren().add(buildSpeedRow());

        return p;
    }

    private GridPane buildZoneGrid() {
        ToggleGroup tg = new ToggleGroup();
        String[] names = {"A","B","C","D","E","F"};
        zoneButtons = new ToggleButton[6];
        GridPane g = new GridPane();
        g.setHgap(4); g.setVgap(4);
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            ToggleButton tb = new ToggleButton(names[i]);
            tb.setToggleGroup(tg);
            tb.setStyle(zoneBtnStyle(i == selectedZoneIndex));
            tb.setSelected(i == selectedZoneIndex);
            tb.setOnAction(e -> {
                selectedZoneIndex = idx;
                for (ToggleButton b : zoneButtons) b.setStyle(zoneBtnStyle(false));
                tb.setStyle(zoneBtnStyle(true));
                if (mapController != null && modele != null) {
                    List<Zone> zs = modele.getZones();
                    if (idx < zs.size()) mapController.focusZone(zs.get(idx));
                }
            });
            zoneButtons[i] = tb;
            g.add(tb, i % 3, i / 3);
        }
        return g;
    }

    private HBox buildControlButtons() {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        btnPause = iconBtn("⏸"); btnPlay = iconBtn("▶"); btnStop = iconBtn("⏹");
        btnPause.setOnAction(e -> { if (controller != null) controller.mettreEnPause(); stopSimLoop(); });
        btnPlay.setOnAction(e  -> { if (controller != null) { controller.demarrerSimulation(); startSimLoop(); } });
        btnStop.setOnAction(e  -> { if (controller != null) controller.resetSimulation(); stopSimLoop(); });
        row.getChildren().addAll(btnPause, btnPlay, btnStop);
        return row;
    }

    private HBox buildSpeedRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        sliderVitesse = new Slider(0.5, 3.0, 1.0);
        sliderVitesse.setPrefWidth(80);
        sliderVitesse.setStyle("-fx-control-inner-background:#1e293b;-fx-accent:" + ACCENT_BLUE + ";");
        lblVitesseVal = styledLabel("1.0×", FontWeight.BOLD, 10, TEXT_PRIMARY);
        sliderVitesse.valueProperty().addListener((o, ov, nv) -> {
            double v = Math.round(nv.doubleValue() * 10) / 10.0;
            lblVitesseVal.setText(v + "×");
            if (controller != null) controller.setGravite(v);
        });
        row.getChildren().addAll(styledLabel("Vitesse", FontWeight.NORMAL, 10, TEXT_MUTED), sliderVitesse, lblVitesseVal);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────
    // BARRE DU BAS
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildBottomBar() {
        HBox bar = new HBox(0);
        bar.setStyle("-fx-background-color:" + BG_CARD
            + ";-fx-border-color:" + BORDER_COLOR + " transparent transparent transparent;"
            + "-fx-border-width:1 0 0 0;");
        bar.setPrefHeight(90);

        // Bloc 1 — Population
        VBox b1 = statBloc("POPULATION");
        b1.getChildren().addAll(
            statRow("👥","Population à risque",    lblPopRisque    = val("—", ACCENT_ORANGE)),
            statRow("🚶","En sécurité",             lblPersonnesSec = val("—", ACCENT_GREEN)),
            statRow("⚙","Agents en déplacement",   lblAgentsActifs = val("0", ACCENT_BLUE)));

        // Bloc 2 — Réseau
        VBox b2 = statBloc("RÉSEAU");
        b2.getChildren().addAll(
            barRow("—", ACCENT_GREEN,  lblAretesSures  = val("—%", TEXT_PRIMARY)),
            barRow("—", ACCENT_ORANGE, lblAretesRisque = val("—%", TEXT_PRIMARY)),
            barRow("Congest.",   "#eab308",     lblAretesCong    = val("—%", TEXT_PRIMARY)), // Nouveau
            barRow("Surchargé",  "#b91c1c",     lblAretesOver    = val("—%", TEXT_PRIMARY)),
            barRow("—", ACCENT_RED,    lblAretesInond  = val("—%", TEXT_PRIMARY)));

        // Bloc 3 — Refuges
        VBox b3 = statBloc("REFUGES");
        b3.getChildren().addAll(
            statRow("⛺","Total",         lblRefugesTotal  = val("12", TEXT_PRIMARY)),
            statRow("✅","Accessibles",   lblRefugesAccess = val("—",  ACCENT_GREEN)),
            statRow("❌","Inaccessibles", lblRefugesInacc  = val("—",  ACCENT_RED)));

        // Bloc 4 — Niveau
        VBox b4 = statBloc("NIVEAU D'EAU");
        lblNiveauActuel = styledLabel("0.00 m", FontWeight.BOLD, 18, ACCENT_BLUE);
        lblNiveauMax    = styledLabel("Max prédit  0.00 m", FontWeight.NORMAL, 10, ACCENT_RED);
        b4.getChildren().addAll(styledLabel("Actuel", FontWeight.NORMAL, 10, TEXT_MUTED), lblNiveauActuel, lblNiveauMax);

        // Bloc 5 — Prochaine étape
        VBox b5 = statBloc("PROCHAINE ÉTAPE");
        lblTempsRestant = styledLabel("+ 2 min", FontWeight.BOLD, 13, ACCENT_TEAL);
        lblZoneNiveaux  = styledLabel("—", FontWeight.NORMAL, 10, TEXT_PRIMARY);
        lblZoneNiveaux.setStyle("-fx-font-family:monospace;-fx-font-size:10px;");
        b5.getChildren().addAll(lblTempsRestant,
            styledLabel("Niveaux estimés par zone", FontWeight.NORMAL, 10, TEXT_MUTED), lblZoneNiveaux);

        bar.getChildren().addAll(
            wrap(b1), div(), wrap(b2), div(), wrap(b3), div(), wrap(b4), div(), wrap(b5));
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════
    // BOUCLES
    // ═════════════════════════════════════════════════════════════════════

    private void startRefreshLoop() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> refreshUI()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void startSimLoop() {
        if (simTimeline != null) simTimeline.stop();
        double intervalMs = (controller != null) ? controller.getVitesseSimulationMs() : 1000;
        simTimeline = new Timeline(new KeyFrame(Duration.millis(intervalMs), e -> {
            if (controller != null) controller.executerPas();
        }));
        simTimeline.setCycleCount(Timeline.INDEFINITE);
        simTimeline.play();
    }

    private void stopSimLoop() { if (simTimeline != null) simTimeline.stop(); }

    // ─────────────────────────────────────────────────────────────────────
    // RAFRAÎCHISSEMENT UI
    // ─────────────────────────────────────────────────────────────────────

    private void refreshUI() {
        if (modele == null) return;
        try {
            // Timer
            int s = (int) modele.getTempsEcoule();
            lblTimer.setText(String.format("⏱  %02d:%02d:%02d", s/3600, (s%3600)/60, s%60));

            // Statut
            boolean pause = modele.isEnPause();
            lblSimStatus.setText(pause ? "En pause" : "Simulation en cours");
            lblSimStatus.setTextFill(Color.web(pause ? ACCENT_ORANGE : ACCENT_GREEN));

            // Niveau eau
            double nv = modele.getNiveauEau();
            lblNiveauActuel.setText(String.format("%.2f m", nv));
            lblNiveauMax.setText(String.format("Max prédit  %.2f m", nv * 1.78));

            if (controller != null) {
                double[] r = controller.getStatutReseau();
                if (r != null && r.length >= 5) { // On vérifie bien la taille 5
                    lblAretesSures.setText(String.format("%.0f%%", r[0]));
                    lblAretesRisque.setText(String.format("%.0f%%", r[1]));
                    lblAretesCong.setText(String.format("%.0f%%", r[2]));
                    lblAretesOver.setText(String.format("%.0f%%", r[3]));
                    lblAretesInond.setText(String.format("%.0f%%", r[4]));
                }
                lblPopRisque.setText(String.valueOf(controller.getPopulationARisque()));
                lblPersonnesSec.setText(String.valueOf(controller.getPopulationEnSecurite()));
                lblAgentsActifs.setText(String.valueOf(controller.getNombreAgentsEnDeplacement()));
            }

            // Zone sélectionnée
            List<Zone> zones = modele.getZones();
            if (!zones.isEmpty() && selectedZoneIndex < zones.size()) {
                Zone z = zones.get(selectedZoneIndex);
                lblNiveauEauZone.setText(String.format("%.2f m", nv));
                boolean fl = z.isFlooded();
                lblStatutZone.setText(fl ? "Inondée ⚠" : nv > 0.5 ? "En montée ↗" : "Stable →");
                lblStatutZone.setTextFill(Color.web(fl ? ACCENT_RED : nv > 0.5 ? ACCENT_ORANGE : ACCENT_GREEN));
            }

            // Agents
            lblAgentsActifs.setText(String.valueOf(modele.getNombreAgents()));
            int zInond = (int) controller.getNombreZonesInondees();
            lblRefugesInacc.setText(String.valueOf(zInond));
            lblRefugesAccess.setText(String.valueOf(Math.max(0, 8 - zInond)));

            // Niveaux par zone
            if (zones.size() >= 6)
                lblZoneNiveaux.setText(String.format(
                    "A %.2fm   B %.2fm%nC %.2fm   F %.2fm",
                    nv*.8, nv, nv*.9, nv*.65));

        } catch (Exception e) {
            System.err.println("refreshUI: " + e.getMessage());
        }
    }

    public void stopRefresh() {
        if (refreshTimeline != null) refreshTimeline.stop();
        if (simTimeline      != null) simTimeline.stop();
    }

    // ═════════════════════════════════════════════════════════════════════
    // UTILITAIRES UI
    // ═════════════════════════════════════════════════════════════════════

    private Label styledLabel(String t, FontWeight fw, int sz, String col) {
        Label l = new Label(t);
        l.setFont(Font.font("System", fw, sz));
        l.setTextFill(Color.web(col));
        return l;
    }
    private Label val(String t, String col) { return styledLabel(t, FontWeight.BOLD, 12, col); }

    private VBox cardPanel(int maxW) {
        VBox p = new VBox(6);
        p.setPadding(new Insets(12, 14, 12, 14));
        p.setMaxWidth(maxW);
        p.setStyle("-fx-background-color:" + BG_CARD
            + ";-fx-background-radius:8;-fx-border-color:" + BORDER_COLOR
            + ";-fx-border-radius:8;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),12,0,0,4);");
        return p;
    }

    private VBox statBloc(String title) {
        VBox b = new VBox(5);
        b.setPadding(new Insets(10, 16, 10, 16));
        b.getChildren().add(styledLabel(title, FontWeight.BOLD, 9, TEXT_MUTED));
        return b;
    }

    private HBox hrow(String label, Label value) {
        HBox r = new HBox(6); r.setAlignment(Pos.CENTER_LEFT);
        r.getChildren().addAll(styledLabel(label, FontWeight.NORMAL, 10, TEXT_MUTED), value);
        return r;
    }

    private HBox statRow(String icon, String label, Label value) {
        HBox r = new HBox(8); r.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon); ic.setFont(Font.font("System", 12));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(ic, styledLabel(label, FontWeight.NORMAL, 11, TEXT_MUTED), sp, value);
        return r;
    }

    private HBox barRow(String label, String color, Label value) {
        HBox r = new HBox(8); r.setAlignment(Pos.CENTER_LEFT);
        Rectangle dot = new Rectangle(12, 3); dot.setFill(Color.web(color));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(dot, styledLabel(label, FontWeight.NORMAL, 11, TEXT_MUTED), sp, value);
        return r;
    }

    private HBox colorBar(String color, String text) {
        HBox r = new HBox(6); r.setAlignment(Pos.CENTER_LEFT);
        Rectangle rect = new Rectangle(16, 8);
        rect.setFill(Color.web(color)); rect.setArcWidth(3); rect.setArcHeight(3);
        r.getChildren().addAll(rect, styledLabel(text, FontWeight.NORMAL, 10, TEXT_MUTED));
        return r;
    }

    private Separator separator() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:" + BORDER_COLOR + ";");
        return s;
    }

    private Button modeButton(String text, boolean active) {
        Button b = new Button(text);
        b.setStyle(active
            ? "-fx-background-color:#1e40af;-fx-text-fill:#93c5fd;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;"
            : "-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_MUTED + ";-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;");
        return b;
    }

    private void setModeActive(Button active, Button inactive) {
        active.setStyle(  "-fx-background-color:#1e40af;-fx-text-fill:#93c5fd;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;");
        inactive.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_MUTED + ";-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;");
    }

    private Button iconBtn(String icon) {
        Button b = new Button(icon);
        String base = "-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_PRIMARY + ";-fx-background-radius:6;-fx-font-size:13px;-fx-padding:5 10 5 10;-fx-cursor:hand;";
        String hov  = "-fx-background-color:" + ACCENT_BLUE + ";-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:13px;-fx-padding:5 10 5 10;-fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hov));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private String zoneBtnStyle(boolean sel) {
        return sel
            ? "-fx-background-color:" + ACCENT_BLUE + ";-fx-text-fill:white;-fx-background-radius:4;-fx-font-size:11px;-fx-font-weight:bold;-fx-min-width:36;-fx-min-height:24;-fx-cursor:hand;"
            : "-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_MUTED + ";-fx-background-radius:4;-fx-font-size:11px;-fx-min-width:36;-fx-min-height:24;-fx-cursor:hand;";
    }

    private HBox wrap(VBox b)       { HBox.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); HBox w = new HBox(b); HBox.setHgrow(w, Priority.ALWAYS); return w; }
    private Rectangle div()         { Rectangle r = new Rectangle(1, 70); r.setFill(Color.web(BORDER_COLOR)); return r; }
}