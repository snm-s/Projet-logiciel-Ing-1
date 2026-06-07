package view;

import controller.MapController;
import controller.AdminPage.SimulationController;
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
import model.simulation.FloodSimulation;
import model.zone.Zone;

import java.util.List;

/**
 * Vue simulation d'inondation — dark dashboard fidèle à la maquette.
 * Layout : carte centrale plein écran + overlays (panel zone, légende, contrôles, barre stats bas).
 */
public class SimulationView extends BorderPane {

    // ── Couleurs du thème sombre ──────────────────────────────────────────
    private static final String BG_DARK       = "#0d1117";
    private static final String BG_PANEL      = "rgba(13,17,27,0.88)";
    private static final String BG_CARD       = "#131b2e";
    private static final String BG_CARD2      = "#0f1923";
    private static final String ACCENT_BLUE   = "#3b82f6";
    private static final String ACCENT_TEAL   = "#06b6d4";
    private static final String ACCENT_GREEN  = "#22c55e";
    private static final String ACCENT_ORANGE = "#f59e0b";
    private static final String ACCENT_RED    = "#ef4444";
    private static final String TEXT_PRIMARY  = "#f1f5f9";
    private static final String TEXT_MUTED    = "#94a3b8";
    private static final String BORDER_COLOR  = "#1e293b";

    // ── Contrôleur & modèle ───────────────────────────────────────────────
    private final SimulationController controller;
    private final FloodSimulation  modele;
    private MapController mapController;

    // ── Composants carte ──────────────────────────────────────────────────
    private MapView  mapView;
    private StackPane mapContainer;

    // ── Panel info zone sélectionnée ──────────────────────────────────────
    private Label lblSelectedZone;
    private Label lblNiveauEauZone;
    private Label lblStatutZone;

    // ── Stats barre du bas ────────────────────────────────────────────────
    private Label lblPopRisque;
    private Label lblPersonnesSec;
    private Label lblAgentsActifs;
    private Label lblAretesSures;
    private Label lblAretesRisque;
    private Label lblAretesInond;
    private Label lblRefugesTotal;
    private Label lblRefugesAccess;
    private Label lblRefugesInacc;
    private Label lblNiveauActuel;
    private Label lblNiveauMax;
    private Label lblTempsRestant;
    private Label lblZoneNiveaux;

    // ── Contrôles simulation ──────────────────────────────────────────────
    private Button  btnPause;
    private Button  btnPlay;
    private Button  btnStop;
    private Button  btnAleatoire;
    private Button  btnManuelle;
    private Label   lblTimer;
    private Label   lblVitesseVal;
    private Slider  sliderVitesse;
    private Label   lblSimStatus;

    // ── Boutons zones (légende) ────────────────────────────────────────────
    private ToggleButton[] zoneButtons;
    private int selectedZoneIndex = 1; // Zone B par défaut

    // ── Rafraîchissement ──────────────────────────────────────────────────
    private Timeline refreshTimeline;
    private Timeline simTimeline;

    // ──────────────────────────────────────────────────────────────────────
    public SimulationView(SimulationController controller) {
        this.controller = controller;
        this.modele = (controller != null) ? controller.getModele() : null;
        this.setStyle("-fx-background-color: " + BG_DARK + ";");
        buildContent();
        startRefreshLoop();
    }

    // ═════════════════════════════════════════════════════════════════════
    // CONSTRUCTION PRINCIPALE
    // ═════════════════════════════════════════════════════════════════════
    private void buildContent() {
        // Carte plein écran comme fond
        mapContainer = buildMapContainer();
        this.setCenter(mapContainer);

        // Barre supérieure (header)
        this.setTop(buildTopBar());

        // Barre inférieure (stats)
        this.setBottom(buildBottomBar());
    }

    // ═════════════════════════════════════════════════════════════════════
    // BARRE SUPÉRIEURE
    // ═════════════════════════════════════════════════════════════════════
    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent;" +
            "-fx-border-width: 0 0 1 0;");
        bar.setPrefHeight(46);

        // Logo / Titre
        HBox logo = new HBox(10);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setPadding(new Insets(0, 20, 0, 16));
        logo.setPrefWidth(220);
        logo.setStyle("-fx-border-color: transparent " + BORDER_COLOR + " transparent transparent; -fx-border-width: 0 1 0 0;");
        Label ico = new Label("🏠");
        ico.setStyle("-fx-font-size: 16px;");
        VBox titreBox = new VBox(1);
        Label titre = styledLabel("Inondation", FontWeight.BOLD, 13, TEXT_PRIMARY);
        Label sous   = styledLabel("Simulation & emergency management", FontWeight.NORMAL, 10, TEXT_MUTED);
        titreBox.getChildren().addAll(titre, sous);
        logo.getChildren().addAll(ico, titreBox);

        // Mode simulation
        HBox modeBox = new HBox(8);
        modeBox.setAlignment(Pos.CENTER);
        modeBox.setPadding(new Insets(0, 24, 0, 24));

        btnAleatoire = modeButton("⟳  Simulation aléatoire", true);
        btnManuelle  = modeButton("↺  Simulation manuelle", false);
        btnAleatoire.setOnAction(e -> { setModeActive(btnAleatoire, btnManuelle); });
        btnManuelle.setOnAction(e -> { setModeActive(btnManuelle, btnAleatoire); });
        modeBox.getChildren().addAll(btnAleatoire, btnManuelle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Statut + timer
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(0, 20, 0, 0));
        lblSimStatus = styledLabel("Simulation en cours", FontWeight.NORMAL, 11, ACCENT_GREEN);
        lblTimer     = styledLabel("⏱  00:00:00", FontWeight.BOLD, 12, TEXT_PRIMARY);
        statusBox.getChildren().addAll(lblSimStatus, lblTimer);

        bar.getChildren().addAll(logo, modeBox, spacer, statusBox);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════
    // CONTENEUR CARTE + OVERLAYS
    // ═════════════════════════════════════════════════════════════════════
    
    
    private StackPane buildMapContainer() {
        StackPane stack = new StackPane();
        stack.setStyle("-fx-background-color: #090e1a;");

        // 1. Initialiser le MapView d'abord
        if (modele != null) {
            mapView = new MapView(modele.getZones());
            modele.addZoneUpdateListener(mapView);
        }

        // 2. Ajouter la carte au stack
        if (mapView != null) {
            stack.getChildren().add(mapView.getWebView());
        }

        // 3. Créer et ajouter les panneaux D'UI par-dessus
        VBox panelZone = buildZoneInfoPanel();
        StackPane.setAlignment(panelZone, Pos.TOP_LEFT);
        StackPane.setMargin(panelZone, new Insets(12, 0, 0, 12));

        VBox legendePanel = buildLegendePanel();
        StackPane.setAlignment(legendePanel, Pos.TOP_RIGHT);
        StackPane.setMargin(legendePanel, new Insets(12, 12, 0, 0));

        VBox ctrlPanel = buildControlesPanel();
        StackPane.setAlignment(ctrlPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(ctrlPanel, new Insets(0, 12, 12, 0));

        stack.getChildren().addAll(panelZone, legendePanel, ctrlPanel);

        // 4. Initialiser le contrôleur
        mapController = new MapController(mapView, modele.getZones());

        // 5. Configurer le callback
        mapController.setOnZoneSelected(zone -> {
            int idx = modele.getZones().indexOf(zone);
            if (idx >= 0) selectedZoneIndex = idx;
            lblSelectedZone.setText("Zone " + zone.getName().substring(0, Math.min(8, zone.getName().length())));
            lblNiveauEauZone.setText(String.format("%.2f m", modele.getNiveauEau()));
            boolean flooded = zone.isFlooded();
            double nv = modele.getNiveauEau();
            lblStatutZone.setText(flooded ? "Inondée ⚠" : nv > 0.5 ? "En montée ↗" : "Stable →");
            lblStatutZone.setTextFill(Color.web(flooded ? ACCENT_RED : nv > 0.5 ? ACCENT_ORANGE : ACCENT_GREEN));
        });

        return stack;
    }
    




    // ─── Panel info zone sélectionnée ─────────────────────────────────────
    private VBox buildZoneInfoPanel() {
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(12, 14, 12, 14));
        panel.setMaxWidth(180);
        panel.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 4);");

        Label zoneSelectLbl = styledLabel("Zone sélectionnée", FontWeight.NORMAL, 10, TEXT_MUTED);

        lblSelectedZone = styledLabel("Zone B", FontWeight.BOLD, 15, TEXT_PRIMARY);

        HBox rowNiv = new HBox(6);
        rowNiv.setAlignment(Pos.CENTER_LEFT);
        Label nivLbl = styledLabel("Niveau d'eau", FontWeight.NORMAL, 10, TEXT_MUTED);
        lblNiveauEauZone = styledLabel("1.35 m", FontWeight.BOLD, 11, ACCENT_BLUE);
        rowNiv.getChildren().addAll(nivLbl, lblNiveauEauZone);

        HBox rowStat = new HBox(6);
        rowStat.setAlignment(Pos.CENTER_LEFT);
        Label statLbl = styledLabel("Statut", FontWeight.NORMAL, 10, TEXT_MUTED);
        lblStatutZone = styledLabel("En montée ↗", FontWeight.BOLD, 11, ACCENT_ORANGE);
        rowStat.getChildren().addAll(statLbl, lblStatutZone);

        panel.getChildren().addAll(zoneSelectLbl, lblSelectedZone, rowNiv, rowStat);
        return panel;
    }

    // ─── Légende ──────────────────────────────────────────────────────────
    private VBox buildLegendePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(12, 14, 12, 14));
        panel.setMaxWidth(180);
        panel.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 4);");

        Label titLeg = styledLabel("LÉGENDE", FontWeight.BOLD, 10, TEXT_MUTED);

        // Noeuds
        panel.getChildren().add(titLeg);
        panel.getChildren().add(legendeRow("○", TEXT_PRIMARY, "Nœud (intersection)", 10));

        // Arêtes
        panel.getChildren().add(legendeColorBar(ACCENT_GREEN, "Arête sûre"));
        panel.getChildren().add(legendeColorBar(ACCENT_ORANGE, "Arête à risque"));
        panel.getChildren().add(legendeColorBar(ACCENT_RED, "Arête inondée"));
        panel.getChildren().add(legendeRow("⛺", ACCENT_GREEN, "Point de refuge", 10));
        panel.getChildren().add(legendeDashed("Limite de zone"));

        // Niveau d'eau
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: " + BORDER_COLOR + ";");
        panel.getChildren().add(sep1);
        panel.getChildren().add(styledLabel("NIVEAU D'EAU", FontWeight.BOLD, 10, TEXT_MUTED));

        String[][] niveaux = {
            {"#bfdbfe", "0 - 0.5 m"},
            {"#60a5fa", "0.5 - 1 m"},
            {"#3b82f6", "1 - 1.5 m"},
            {"#1d4ed8", "1.5 - 2 m"},
            {"#1e3a8a", "> 2 m"}
        };
        for (String[] n : niveaux) {
            panel.getChildren().add(legendeColorBar(n[0], n[1]));
        }

        // Zones A-F
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: " + BORDER_COLOR + ";");
        panel.getChildren().add(sep2);
        panel.getChildren().add(styledLabel("ZONES", FontWeight.BOLD, 10, TEXT_MUTED));

        ToggleGroup tg = new ToggleGroup();
        String[] zoneNames = {"A", "B", "C", "D", "E", "F"};
        zoneButtons = new ToggleButton[6];

        GridPane zonesGrid = new GridPane();
        zonesGrid.setHgap(4);
        zonesGrid.setVgap(4);

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            ToggleButton tb = new ToggleButton(zoneNames[i]);
            tb.setToggleGroup(tg);
            tb.setStyle(zoneButtonStyle(false));
            tb.setSelected(i == selectedZoneIndex);
            if (i == selectedZoneIndex) tb.setStyle(zoneButtonStyle(true));
            tb.setOnAction(e -> {
                    selectedZoneIndex = idx;
                    for (ToggleButton b : zoneButtons) b.setStyle(zoneButtonStyle(false));
                    tb.setStyle(zoneButtonStyle(true));
                    updateSelectedZone(zoneNames[idx]);
                    
                    // Interaction avec la carte
                    if (mapController != null && modele != null) {
                        List<Zone> zones = modele.getZones();
                        if (idx < zones.size()) {
                            mapController.focusZone(zones.get(idx));
                        }
                    }
                });
            zoneButtons[i] = tb;
            zonesGrid.add(tb, i % 3, i / 3);
        }
        panel.getChildren().add(zonesGrid);

        // Contrôles simulation bas légende
        Separator sep3 = new Separator();
        sep3.setStyle("-fx-background-color: " + BORDER_COLOR + ";");
        panel.getChildren().add(sep3);
        panel.getChildren().add(styledLabel("CONTRÔLES DE SIMULATION", FontWeight.BOLD, 10, TEXT_MUTED));

        HBox ctrlBtns = new HBox(6);
        ctrlBtns.setAlignment(Pos.CENTER);
        btnPause   = iconButton("⏸");
        btnPlay    = iconButton("▶");
        btnStop    = iconButton("⏹");
        btnPause.setOnAction(e -> { if (controller != null) controller.mettreEnPause(); stopSimLoop(); });
        btnPlay.setOnAction(e  -> { if (controller != null) { controller.demarrerSimulation(); startSimLoop(); } });
        btnStop.setOnAction(e  -> { if (controller != null) controller.resetSimulation(); stopSimLoop(); });
        ctrlBtns.getChildren().addAll(btnPause, btnPlay, btnStop);
        panel.getChildren().add(ctrlBtns);

        // Slider vitesse
        HBox vitRow = new HBox(8);
        vitRow.setAlignment(Pos.CENTER_LEFT);
        Label vitLbl = styledLabel("Vitesse", FontWeight.NORMAL, 10, TEXT_MUTED);
        sliderVitesse = new Slider(0.5, 3.0, 1.0);
        sliderVitesse.setPrefWidth(80);
        sliderVitesse.setStyle("-fx-control-inner-background: #1e293b; -fx-accent: " + ACCENT_BLUE + ";");
        lblVitesseVal = styledLabel("1.0×", FontWeight.BOLD, 10, TEXT_PRIMARY);
        sliderVitesse.valueProperty().addListener((obs, o, n) -> {
            double v = Math.round(n.doubleValue() * 10.0) / 10.0;
            lblVitesseVal.setText(v + "×");
            if (controller != null) controller.setGravite(v);
        });
        vitRow.getChildren().addAll(vitLbl, sliderVitesse, lblVitesseVal);
        panel.getChildren().add(vitRow);

        return panel;
    }

    // ─── Contrôles (overlay bas droite de carte) ──────────────────────────
    private VBox buildControlesPanel() {
        // Les contrôles principaux sont dans la légende, ce panel est vide
        // mais préservé pour extension future
        return new VBox();
    }

    // ═════════════════════════════════════════════════════════════════════
    // BARRE DU BAS — 5 blocs de stats
    // ═════════════════════════════════════════════════════════════════════
    private HBox buildBottomBar() {
        HBox bar = new HBox(0);
        bar.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-border-color: " + BORDER_COLOR + " transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;");
        bar.setPrefHeight(90);

        // Bloc 1 — Informations générales
        VBox bloc1 = buildStatBloc("INFORMATIONS GÉNÉRALES", null);
        bloc1.getChildren().addAll(
            buildStatLine("👥", "Population à risque", lblPopRisque = styledLabel("1,248", FontWeight.BOLD, 12, ACCENT_ORANGE)),
            buildStatLine("🚶", "Personnes en sécurité", lblPersonnesSec = styledLabel("3,756", FontWeight.BOLD, 12, ACCENT_GREEN)),
            buildStatLine("⚙", "Agents actifs", lblAgentsActifs = styledLabel("24", FontWeight.BOLD, 12, ACCENT_BLUE))
        );

        // Bloc 2 — Statut du réseau
        VBox bloc2 = buildStatBloc("STATUT DU RÉSEAU", null);
        bloc2.getChildren().addAll(
            buildStatLineBar("— Arêtes sûres",   ACCENT_GREEN,  lblAretesSures  = styledLabel("58%", FontWeight.BOLD, 11, TEXT_PRIMARY)),
            buildStatLineBar("— Arêtes à risque", ACCENT_ORANGE, lblAretesRisque = styledLabel("27%", FontWeight.BOLD, 11, TEXT_PRIMARY)),
            buildStatLineBar("— Arêtes inondées", ACCENT_RED,    lblAretesInond  = styledLabel("15%", FontWeight.BOLD, 11, TEXT_PRIMARY))
        );

        // Bloc 3 — Points de refuge
        VBox bloc3 = buildStatBloc("POINTS DE REFUGE", null);
        bloc3.getChildren().addAll(
            buildStatLine("⛺", "Total",         lblRefugesTotal  = styledLabel("12", FontWeight.BOLD, 12, TEXT_PRIMARY)),
            buildStatLine("✅", "Accessibles",   lblRefugesAccess = styledLabel("8",  FontWeight.BOLD, 12, ACCENT_GREEN)),
            buildStatLine("❌", "Inaccessibles", lblRefugesInacc  = styledLabel("4",  FontWeight.BOLD, 12, ACCENT_RED))
        );

        // Bloc 4 — Niveau d'eau moyen
        VBox bloc4 = buildStatBloc("NIVEAU D'EAU MOYEN", null);
        lblNiveauActuel = styledLabel("1.35 m", FontWeight.BOLD, 18, ACCENT_BLUE);
        Label actLbl  = styledLabel("Actuel", FontWeight.NORMAL, 10, TEXT_MUTED);
        lblNiveauMax  = styledLabel("Max prédit  2.40 m", FontWeight.NORMAL, 10, ACCENT_RED);
        bloc4.getChildren().addAll(actLbl, lblNiveauActuel, lblNiveauMax);

        // Bloc 5 — Prochaine étape
        VBox bloc5 = buildStatBloc("PROCHAINE ÉTAPE", null);
        lblTempsRestant = styledLabel("+ 2 min", FontWeight.BOLD, 13, ACCENT_TEAL);
        Label subPE  = styledLabel("Niveau d'eau estimé dans les zones", FontWeight.NORMAL, 10, TEXT_MUTED);
        lblZoneNiveaux = styledLabel("A 1.10m   B 1.65m\nC 1.25m   F 0.90m", FontWeight.NORMAL, 10, TEXT_PRIMARY);
        lblZoneNiveaux.setStyle("-fx-font-family: monospace; -fx-font-size: 10px;");
        bloc5.getChildren().addAll(lblTempsRestant, subPE, lblZoneNiveaux);

        bar.getChildren().addAll(
            wrapBloc(bloc1), divider(),
            wrapBloc(bloc2), divider(),
            wrapBloc(bloc3), divider(),
            wrapBloc(bloc4), divider(),
            wrapBloc(bloc5)
        );
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════
    // BOUCLE DE RAFRAÎCHISSEMENT
    // ═════════════════════════════════════════════════════════════════════
    private void startRefreshLoop() {
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.millis(500), e -> actualiserUI()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void startSimLoop() {
        if (simTimeline != null) simTimeline.stop();
        simTimeline = new Timeline(
            new KeyFrame(Duration.millis(1000), e -> {
                if (controller != null) controller.executerPas();
            }));
        simTimeline.setCycleCount(Timeline.INDEFINITE);
        simTimeline.play();
    }

    private void stopSimLoop() {
        if (simTimeline != null) simTimeline.stop();
    }

    private void actualiserUI() {
        if (modele == null) return;

        try {

        // Timer
        int secs = (int) modele.getTempsEcoule();
        lblTimer.setText(String.format("⏱  %02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60));

        // Statut
        boolean pause = modele.isEnPause();
        lblSimStatus.setText(pause ? "En pause" : "Simulation en cours");
        lblSimStatus.setTextFill(Color.web(pause ? ACCENT_ORANGE : ACCENT_GREEN));

        // Niveau eau
        double nv = modele.getNiveauEau();
        lblNiveauActuel.setText(String.format("%.2f m", nv));
        lblNiveauMax.setText(String.format("Max prédit  %.2f m", nv * 1.78));


        if (controller != null) {
            // Stats du réseau (Arêtes)
            double[] reseau = controller.getStatutReseau();
            if (reseau != null && reseau.length >= 3) {
                lblAretesSures.setText(String.format("%.0f%%", reseau[0]));
                lblAretesRisque.setText(String.format("%.0f%%", reseau[1]));
                lblAretesInond.setText(String.format("%.0f%%", reseau[2]));
            }

            // Population dynamique
            lblPopRisque.setText(String.valueOf(controller.getPopulationARisque()));
            lblPersonnesSec.setText(String.valueOf(controller.getPopulationEnSecurite()));
        }



        // Zone sélectionnée
        List<Zone> zones = modele.getZones();
        if (selectedZoneIndex < zones.size()) {
            Zone z = zones.get(selectedZoneIndex);
            lblNiveauEauZone.setText(String.format("%.2f m", nv));
            lblStatutZone.setText(z.isFlooded() ? "Inondée ⚠" : nv > 0.5 ? "En montée ↗" : "Stable →");
            lblStatutZone.setTextFill(Color.web(z.isFlooded() ? ACCENT_RED : nv > 0.5 ? ACCENT_ORANGE : ACCENT_GREEN));
        }

        // Agents
        lblAgentsActifs.setText(String.valueOf(modele.getNombreAgents()));
        lblPersonnesSec.setText(String.valueOf(modele.getNombreAgentsEvacues()));
        int zInond = modele.getNombreZonesInondees();
        lblRefugesInacc.setText(String.valueOf(zInond));
        lblRefugesAccess.setText(String.valueOf(Math.max(0, 8 - zInond)));

        // Zones info bas
        if (zones.size() >= 6) {
            lblZoneNiveaux.setText(String.format(
                "A %.2fm   B %.2fm%nC %.2fm   F %.2fm",
                nv * 0.8, nv, nv * 0.9, nv * 0.65));
        }


        } catch (Exception e) {
        System.out.println("Erreur dans actualiserUI : " + e.getMessage());
        e.printStackTrace();
        }


    }

    private void updateSelectedZone(String zoneName) {
        lblSelectedZone.setText("Zone " + zoneName);
    }

    public void stopRefresh() {
        if (refreshTimeline != null) refreshTimeline.stop();
        if (simTimeline != null) simTimeline.stop();
    }

    // ═════════════════════════════════════════════════════════════════════
    // UTILITAIRES UI
    // ═════════════════════════════════════════════════════════════════════

    private Label styledLabel(String text, FontWeight fw, int size, String color) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", fw, size));
        lbl.setTextFill(Color.web(color));
        return lbl;
    }

    private Button modeButton(String text, boolean active) {
        Button btn = new Button(text);
        String activeStyle =
            "-fx-background-color: #1e40af;" +
            "-fx-text-fill: #93c5fd;" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 11px;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-cursor: hand;";
        String inactiveStyle =
            "-fx-background-color: #1e293b;" +
            "-fx-text-fill: " + TEXT_MUTED + ";" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 11px;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-cursor: hand;";
        btn.setStyle(active ? activeStyle : inactiveStyle);
        btn.setUserData(new String[]{activeStyle, inactiveStyle});
        return btn;
    }

    private void setModeActive(Button active, Button inactive) {
        String[] sa = (String[]) active.getUserData();
        String[] si = (String[]) inactive.getUserData();
        if (sa != null) active.setStyle(sa[0]);
        if (si != null) inactive.setStyle(si[1]);
    }

    private Button iconButton(String icon) {
        Button btn = new Button(icon);
        btn.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 5 10 5 10;" +
            "-fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: " + ACCENT_BLUE + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 5 10 5 10;" +
            "-fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 5 10 5 10;" +
            "-fx-cursor: hand;"));
        return btn;
    }

    private String zoneButtonStyle(boolean selected) {
        return selected
            ? "-fx-background-color: " + ACCENT_BLUE + ";" +
              "-fx-text-fill: white;" +
              "-fx-background-radius: 4;" +
              "-fx-font-size: 11px; -fx-font-weight: bold;" +
              "-fx-min-width: 36; -fx-min-height: 24; -fx-cursor: hand;"
            : "-fx-background-color: #1e293b;" +
              "-fx-text-fill: " + TEXT_MUTED + ";" +
              "-fx-background-radius: 4;" +
              "-fx-font-size: 11px;" +
              "-fx-min-width: 36; -fx-min-height: 24; -fx-cursor: hand;";
    }

    private HBox legendeRow(String icon, String iconColor, String text, int size) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setTextFill(Color.web(iconColor));
        ico.setFont(Font.font("System", size));
        Label lbl = styledLabel(text, FontWeight.NORMAL, size, TEXT_MUTED);
        row.getChildren().addAll(ico, lbl);
        return row;
    }

    private HBox legendeColorBar(String color, String text) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Rectangle rect = new Rectangle(16, 8);
        rect.setFill(Color.web(color));
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        Label lbl = styledLabel(text, FontWeight.NORMAL, 10, TEXT_MUTED);
        row.getChildren().addAll(rect, lbl);
        return row;
    }

    private HBox legendeDashed(String text) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label dash = styledLabel("- - -", FontWeight.NORMAL, 10, TEXT_MUTED);
        Label lbl  = styledLabel(text, FontWeight.NORMAL, 10, TEXT_MUTED);
        row.getChildren().addAll(dash, lbl);
        return row;
    }

    private VBox buildStatBloc(String title, String unused) {
        VBox bloc = new VBox(5);
        bloc.setPadding(new Insets(10, 16, 10, 16));
        Label t = styledLabel(title, FontWeight.BOLD, 9, TEXT_MUTED);
        bloc.getChildren().add(t);
        return bloc;
    }

    private HBox buildStatLine(String icon, String label, Label valueLabel) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setFont(Font.font("System", 12));
        Label lbl = styledLabel(label, FontWeight.NORMAL, 11, TEXT_MUTED);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(ico, lbl, sp, valueLabel);
        return row;
    }

    private HBox buildStatLineBar(String label, String color, Label valueLabel) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Rectangle dot = new Rectangle(12, 3);
        dot.setFill(Color.web(color));
        Label lbl = styledLabel(label, FontWeight.NORMAL, 11, TEXT_MUTED);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(dot, lbl, sp, valueLabel);
        return row;
    }

    private HBox wrapBloc(VBox bloc) {
        HBox.setHgrow(bloc, Priority.ALWAYS);
        bloc.setMaxWidth(Double.MAX_VALUE);
        HBox wrap = new HBox(bloc);
        HBox.setHgrow(wrap, Priority.ALWAYS);
        return wrap;
    }

    private Rectangle divider() {
        Rectangle r = new Rectangle(1, 70);
        r.setFill(Color.web(BORDER_COLOR));
        return r;
    }
}
