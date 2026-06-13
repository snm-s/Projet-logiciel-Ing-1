package view;

import java.io.File;
import java.util.List;

import app.Main;
import controller.AdminPage.SimulationController;
import controller.AdminPage.SimulationController.AgentRole;
import controller.AdminPage.SimulationController.NodeEdgeStats;
import controller.MapController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.graph.Edge;
import model.simulation.FloodSimulation;
import model.zone.Zone;

/**
 * Vue principale de simulation — refonte complète.
 *
 * Structure :
 *   TOP    → barre fixe 52px (retour + modes + chrono + vitesse + save/export)
 *   LEFT   → panneau outils 220px (édition graphe, agents, sélection, trajet)
 *   CENTER → carte OSM interactive
 *   RIGHT  → panneau contextuel 240px (formulaires add/edit zone/edge/agent + légende)
 *   BOTTOM → barre stats 80px
 */
public class SimulationView extends BorderPane {

    // ─── Palette ──────────────────────────────────────────────────────────
    private static final String BG     = "#0d1117";
    private static final String CARD   = "#131b2e";
    private static final String BLUE   = "#3b82f6";
    private static final String TEAL   = "#06b6d4";
    private static final String GREEN  = "#22c55e";
    private static final String ORANGE = "#f59e0b";
    private static final String RED    = "#ef4444";
    private static final String PURPLE = "#7c3aed";
    private static final String YELLOW = "#eab308";
    private static final String TEXT   = "#f1f5f9";
    private static final String MUTED  = "#94a3b8";
    private static final String BORDER = "#1e293b";

    // ─── Contrôleurs ─────────────────────────────────────────────────────
    private final SimulationController ctrl;
    private final FloodSimulation      modele;
    private MapController              mapCtrl;
    private MapView                    mapView;

    // ─── Labels stats globaux ─────────────────────────────────────────────
    private Label lblStatus, lblTimer;
    private Label lblNiveauActuel, lblNiveauMax;
    private Label lblPopRisque, lblPersonnesSec, lblAgentsActifs;
    private Label lblSures, lblRisque, lblCong, lblOver, lblInond;
    private Label lblRefTotal, lblRefAccess, lblRefInacc;
    private Label lblActifs, lblBloques, lblCongNds;

    // ─── Panneau sélection (gauche) ───────────────────────────────────────
    private VBox  panelSel;
    private Label lblSelNom, lblSelType, lblSelStatus;
    private Label lblSelAgents, lblSelPassed, lblSelSpeed, lblSelWait, lblSelCap;

    // ─── Panneau trajet (gauche) ──────────────────────────────────────────
    private VBox  panelPath;
    private Label lblPathInfo;

    // ─── Info graphe ──────────────────────────────────────────────────────
    private Label lblGraphInfo;

    // ─── Panneau droit contextuel ─────────────────────────────────────────
    private VBox   rightContent;
    private VBox   panelAddNeighborhood;
    private VBox   panelAddShelter;
    private VBox   panelAddAgent;
    private VBox   panelEditZone;
    private VBox   panelEditEdge;
    private VBox   panelEditAgent;
    private VBox   panelLegend;

    // ─── Champs Add Neighborhood ──────────────────────────────────────────
    private TextField tfNhName, tfNhAlt, tfNhPop, tfNhDesc;
    private Label     lblNhCoords;
    private double    pendingNhLat = Double.NaN, pendingNhLng = Double.NaN;
    private boolean   waitingClickNh = false;

    // ─── Champs Add Shelter ───────────────────────────────────────────────
    private TextField tfShName, tfShAlt, tfShCap, tfShDesc;
    private Label     lblShCoords;
    private double    pendingShLat = Double.NaN, pendingShLng = Double.NaN;
    private boolean   waitingClickSh = false;

    // ─── Champs Add Agent ─────────────────────────────────────────────────
    private ComboBox<String> cbAgentRole;
    private TextField        tfAgFn, tfAgLn, tfAgAge;
    private Slider           slAgSpeed;
    private Label            lblAgSpeed;
    private ToggleGroup      agStressGroup;
    private RadioButton      rbAgCalme, rbAgStresse;
    private Label            lblAgZone;
    private Zone             pendingAgZone   = null;
    private boolean          waitingAgZone   = false;

    // ─── Champs Edit Zone ─────────────────────────────────────────────────
    private TextField tfEzName, tfEzAlt, tfEzPop, tfEzDesc;
    private Zone      editingZone = null;

    // ─── Champs Edit Edge ─────────────────────────────────────────────────
    private TextField tfEeCap;
    private Edge      editingEdge = null;

    // ─── Champs Edit Agent ────────────────────────────────────────────────
    private TextField   tfEaFn, tfEaLn, tfEaAge;
    private Slider      slEaSpeed;
    private Label       lblEaSpeed;
    private ToggleGroup eaStressGroup;
    private RadioButton rbEaCalme, rbEaStresse;
    private Agent       editingAgent = null;

    // ─── Timelines ────────────────────────────────────────────────────────
    private Timeline refreshLoop;
    private Timeline simLoop;
    private boolean  simRunning = false;

    // ─── Mode d'édition courant ───────────────────────────────────────────
    private enum ToolMode { NONE, ADD_NEIGHBORHOOD, ADD_SHELTER, ADD_AGENT, ADD_EDGE,
                            EDIT_ZONE, EDIT_EDGE, EDIT_AGENT, MOVE_ZONE, DELETE }
    private ToolMode currentTool = ToolMode.NONE;

    // ─── Sélection en cours pour liaison d'arête ─────────────────────────
    private Zone edgeZoneA = null;

    // ─────────────────────────────────────────────────────────────────────

    public SimulationView(SimulationController ctrl) {
        this.ctrl    = ctrl;
        this.modele  = ctrl != null ? ctrl.getModele() : null;
        this.mapView = Main.getSharedMapView();
        this.mapCtrl = Main.getSharedMapController();

        setStyle("-fx-background-color:" + BG + ";");
        buildUI();
        wireCallbacks();
        startRefreshLoop();
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION UI
    // ─────────────────────────────────────────────────────────────────────

    private void buildUI() {
        setTop(buildTopBar());
        setLeft(buildLeftPanel());
        setCenter(buildMapContainer());
        setRight(buildRightPanel());
        setBottom(buildBottomBar());
    }

    private void wireCallbacks() {
        if (ctrl == null) return;
        ctrl.setOnStatusChanged(s -> Platform.runLater(() -> lblStatus.setText(s)));
        ctrl.setOnWaterLevelChanged(l -> Platform.runLater(() -> {
            lblNiveauActuel.setText(String.format("%.2f m", l));
            lblNiveauMax.setText(String.format("Max %.2f m", l * 1.78));
        }));
        ctrl.setOnZonesUpdated(zs -> { if (mapView != null) mapView.updateAllZones(zs); });
        ctrl.setOnSelectedAgentPathChanged(this::updatePathPanel);
        ctrl.setOnStatsUpdated(this::updateSelPanel);
    }

    // ═════════════════════════════════════════════════════════════════════
    // TOP BAR — 52px
    // ═════════════════════════════════════════════════════════════════════

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinHeight(52);
        bar.setMaxHeight(52);
        bar.setStyle("-fx-background-color:" + CARD
            + ";-fx-border-color:transparent transparent " + BORDER + " transparent;"
            + "-fx-border-width:0 0 1 0;");

        // Retour
        Button btnBack = actionBtn("← Retour", RED);
        btnBack.setOnAction(e -> { stopAll(); Main.showDashboardView("admin"); });
        HBox backBox = padBox(btnBack, 0, 12, 0, 12);
        backBox.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Logo
        VBox logoBox = new VBox(1,
            lbl("Simulation Inondation", FontWeight.BOLD, 13, TEXT),
            lbl("Graphe • Agents • Réseau", FontWeight.NORMAL, 10, MUTED));
        HBox logo = padBox(logoBox, 0, 16, 0, 12);
        logo.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Modes
        Button btnAlea   = modeBtn("⟳ Aléatoire", false);
        Button btnManual = modeBtn("↺ Manuel",    false);
        Button btnStep   = modeBtn("⏭ Pas",       false);
        Button btnReset  = modeBtn("↻ Reset",     false);

        btnAlea.setOnAction(e -> {
            activateMode(btnAlea, btnManual, btnStep, btnReset);
            if (mapView != null) { mapView.setManualFloodMode(false); mapView.startRandomFlood(); }
            if (ctrl != null)   { ctrl.setModeAleatoire(true); ctrl.demarrerSimulation(); startSim(); }
        });
        btnManual.setOnAction(e -> {
            activateMode(btnManual, btnAlea, btnStep, btnReset);
            if (mapView != null) mapView.setManualFloodMode(true);
            if (ctrl != null)   { ctrl.setModeAleatoire(false); ctrl.demarrerSimulation(); startSim(); }
        });
        btnStep.setOnAction(e -> {
            activateMode(btnStep, btnAlea, btnManual, btnReset);
            stopSim();
            if (ctrl != null) { ctrl.reprendreSimulation(); ctrl.executerPas(); ctrl.mettreEnPause(); refreshUI(); }
        });
        btnReset.setOnAction(e -> {
            activateMode(btnReset, btnAlea, btnManual, btnStep);
            if (mapView != null) mapView.resetManualFlood();
            if (ctrl != null)   { ctrl.resetSimulation(); ctrl.mettreEnPause(); }
            stopSim(); refreshUI();
        });

        HBox modes = new HBox(5, btnAlea, btnManual, btnStep, btnReset);
        modes.setAlignment(Pos.CENTER);
        modes.setPadding(new Insets(0, 14, 0, 14));
        modes.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Chrono
        lblTimer  = lbl("⏱ 00:00:00", FontWeight.BOLD, 13, TEXT);
        lblStatus = lbl("En pause",    FontWeight.NORMAL, 11, ORANGE);
        VBox chronoBox = new VBox(2, lblTimer, lblStatus);
        chronoBox.setAlignment(Pos.CENTER_LEFT);
        HBox chrono = padBox(chronoBox, 0, 14, 0, 14);
        chrono.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Vitesse + Play/Pause (même bouton)
        Slider slVitesse = new Slider(200, 5000, 1500);
        slVitesse.setPrefWidth(110);
        slVitesse.setStyle("-fx-control-inner-background:#1e293b;-fx-accent:" + BLUE + ";");
        Label lblVitesse = lbl("1500ms", FontWeight.BOLD, 10, TEXT);
        slVitesse.valueProperty().addListener((o, ov, nv) -> {
            int v = (int) Math.round(nv.doubleValue());
            lblVitesse.setText(v + "ms");
            if (ctrl != null) ctrl.setVitesseSimulation(v);
            if (simRunning) startSim();
        });

        Button btnPlayPause = iconBtn("▶");
        btnPlayPause.setOnAction(e -> {
            if (ctrl == null) return;
            simRunning = !simRunning;
            if (simRunning) {
                ctrl.reprendreSimulation();
                startSim();
                btnPlayPause.setText("⏸");
            } else {
                ctrl.mettreEnPause();
                stopSim();
                btnPlayPause.setText("▶");
            }
        });

        HBox speedBox = new HBox(6, lbl("Vitesse:", FontWeight.NORMAL, 10, MUTED),
                slVitesse, lblVitesse, btnPlayPause);
        speedBox.setAlignment(Pos.CENTER);
        speedBox.setPadding(new Insets(0, 14, 0, 14));
        speedBox.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Sauvegarder état initial + Export/Import
        Button btnSaveInit = smallBtn("💾 Sauvegarder");
        Button btnExport   = smallBtn("↑ Exporter");
        Button btnImport   = smallBtn("↓ Importer");
        btnSaveInit.setOnAction(e -> { if (ctrl != null) ctrl.saveCurrentStateAsInitial(); });
        btnExport.setOnAction(e -> handleExport());
        btnImport.setOnAction(e -> handleImport());
        HBox ioBox = new HBox(5, btnSaveInit, btnExport, btnImport);
        ioBox.setAlignment(Pos.CENTER);
        ioBox.setPadding(new Insets(0, 12, 0, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(backBox, logo, modes, chrono, speedBox, spacer, ioBox);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════
    // PANNEAU GAUCHE — 220px
    // ═════════════════════════════════════════════════════════════════════

    private VBox buildLeftPanel() {
        VBox panel = new VBox(0);
        panel.setMinWidth(220);
        panel.setMaxWidth(220);
        panel.setStyle("-fx-background-color:" + CARD
            + ";-fx-border-color:transparent " + BORDER + " transparent transparent;"
            + "-fx-border-width:0 1 0 0;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:" + CARD + ";");

        // ── Section : NŒUDS / ZONES ──────────────────────────────────────
        content.getChildren().add(sectionHeader("NŒUDS / ZONES"));
        VBox nodesSec = section();

        Button bSel     = toolBtn("⊙ Sélection",   false);
        Button bAddNh   = toolBtn("＋ Quartier",    false);
        Button bAddSh   = toolBtn("＋ Refuge",      false);
        Button bAdd5    = toolBtn("＋ 5 nœuds",     false);
        Button bMoveZ   = toolBtn("↕ Déplacer",     false);
        Button bEditZ   = toolBtn("✏ Modifier",     false);
        Button bResetZ  = toolBtn("↺ Reset zones",  false);
        Button bDelZ    = toolBtn("🗑 Supprimer",    false);

        bSel.setOnAction(e -> {
            setTool(ToolMode.NONE, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
            showRightPanel(null);
            setGraphInfo("Mode sélection.");
        });
        bAddNh.setOnAction(e -> {
            setTool(ToolMode.ADD_NEIGHBORHOOD, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
            showRightPanel(panelAddNeighborhood);
            resetNhForm();
            setGraphInfo("Remplissez le formulaire à droite, puis cliquez la carte pour la position.");
        });
        bAddSh.setOnAction(e -> {
            setTool(ToolMode.ADD_SHELTER, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
            showRightPanel(panelAddShelter);
            resetShForm();
            setGraphInfo("Remplissez le formulaire à droite, puis cliquez la carte pour la position.");
        });
        bAdd5.setOnAction(e -> {
            setTool(ToolMode.NONE, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (ctrl != null) {
                for (int i = 0; i < 3; i++) ctrl.addRandomNeighborhood();
                for (int i = 0; i < 2; i++) ctrl.addRandomShelter();
                refreshUI();
            }
            setGraphInfo("5 nœuds aléatoires ajoutés.");
        });
        bMoveZ.setOnAction(e -> {
            setTool(ToolMode.MOVE_ZONE, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.MOVE_NODE);
            showRightPanel(null);
            setGraphInfo("Glissez un nœud pour le déplacer.");
        });
        bEditZ.setOnAction(e -> {
            setTool(ToolMode.EDIT_ZONE, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
            Zone sel = mapCtrl != null ? mapCtrl.getSelectedZone() : null;
            if (sel != null) {
                editingZone = sel;
                fillEditZoneForm(sel);
                showRightPanel(panelEditZone);
            } else {
                showRightPanel(panelEditZone);
                setGraphInfo("Sélectionnez d'abord un nœud sur la carte.");
            }
        });
        bResetZ.setOnAction(e -> {
            setTool(ToolMode.NONE, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (ctrl != null) { ctrl.resetAllZones(); refreshUI(); }
        });
        bDelZ.setOnAction(e -> {
            setTool(ToolMode.DELETE, bSel, bAddNh, bAddSh, bAdd5, bMoveZ, bEditZ, bResetZ, bDelZ);
            if (mapCtrl != null && ctrl != null) {
                Zone sel = mapCtrl.getSelectedZone();
                if (sel != null) { ctrl.removeZone(sel.getId()); showRightPanel(null); refreshUI(); }
                else setGraphInfo("⚠ Sélectionnez d'abord un nœud.");
            }
        });

        nodesSec.getChildren().addAll(
            wrapFlow(5, bSel, bAddNh, bAddSh, bAdd5),
            wrapFlow(5, bMoveZ, bEditZ, bResetZ, bDelZ));
        content.getChildren().add(nodesSec);

        // ── Section : ROUTES / ARÊTES ─────────────────────────────────────
        content.getChildren().add(sectionHeader("ROUTES / ARÊTES"));
        VBox edgesSec = section();

        Button bAddEdge  = toolBtn("＋ Route",      false);
        Button bEditEdge = toolBtn("✏ Modifier",    false);
        Button bDelEdge  = toolBtn("🗑 Supprimer",   false);

        bAddEdge.setOnAction(e -> {
            setTool(ToolMode.ADD_EDGE, bAddEdge, bEditEdge, bDelEdge);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
            edgeZoneA = null;
            showRightPanel(null);
            setGraphInfo("Cliquez la 1ʳᵉ zone, puis la 2ᵉ pour créer une route.");
        });
        bEditEdge.setOnAction(e -> {
            setTool(ToolMode.EDIT_EDGE, bAddEdge, bEditEdge, bDelEdge);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
            Edge sel = mapView != null ? mapView.getSelectedEdge() : null;
            if (sel != null) {
                editingEdge = sel;
                fillEditEdgeForm(sel);
                showRightPanel(panelEditEdge);
            } else {
                showRightPanel(panelEditEdge);
                setGraphInfo("Sélectionnez d'abord une route sur la carte.");
            }
        });
        bDelEdge.setOnAction(e -> {
            setTool(ToolMode.DELETE, bAddEdge, bEditEdge, bDelEdge);
            if (mapView != null && ctrl != null) {
                Edge selEdge = mapView.getSelectedEdge();
                if (selEdge != null) { ctrl.removeEdgeWithAgentRelocation(selEdge.getId()); showRightPanel(null); refreshUI(); }
                else setGraphInfo("⚠ Sélectionnez d'abord une route.");
            }
        });

        edgesSec.getChildren().add(wrapFlow(5, bAddEdge, bEditEdge, bDelEdge));
        content.getChildren().add(edgesSec);

        // ── Section : AGENTS ──────────────────────────────────────────────
        content.getChildren().add(sectionHeader("AGENTS"));
        VBox agentsSec = section();

        Button bAddAg   = toolBtn("＋ Agent",       false);
        Button bAdd10   = toolBtn("＋ 10 agents",   false);
        Button bEv      = toolBtn("⚡ Évacuer",      false);
        Button bEditAg  = toolBtn("✏ Modifier",     false);
        Button bDelAg   = toolBtn("🗑 Supprimer",    false);

        bAddAg.setOnAction(e -> {
            setTool(ToolMode.ADD_AGENT, bAddAg, bAdd10, bEv, bEditAg, bDelAg);
            showRightPanel(panelAddAgent);
            resetAgentForm();
            setGraphInfo("Remplissez le formulaire et choisissez une zone de départ.");
        });
        bAdd10.setOnAction(e -> {
            setTool(ToolMode.NONE, bAddAg, bAdd10, bEv, bEditAg, bDelAg);
            if (ctrl != null) { ctrl.addRandomAgents(10); syncMapAgents(); }
        });
        bEv.setOnAction(e -> {
            setTool(ToolMode.NONE, bAddAg, bAdd10, bEv, bEditAg, bDelAg);
            if (mapCtrl != null) { mapCtrl.evacuateAllCitizensToShelters(); refreshUI(); }
        });
        bEditAg.setOnAction(e -> {
            setTool(ToolMode.EDIT_AGENT, bAddAg, bAdd10, bEv, bEditAg, bDelAg);
            if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
            Agent sel = mapView != null ? mapView.getSelectedAgent() : null;
            if (sel != null) {
                editingAgent = sel;
                fillEditAgentForm(sel);
                showRightPanel(panelEditAgent);
            } else {
                showRightPanel(panelEditAgent);
                setGraphInfo("Sélectionnez d'abord un agent sur la carte.");
            }
        });
        bDelAg.setOnAction(e -> {
            setTool(ToolMode.DELETE, bAddAg, bAdd10, bEv, bEditAg, bDelAg);
            if (mapView != null && ctrl != null) {
                Agent sel = mapView.getSelectedAgent();
                if (sel != null) {
                    ctrl.removeAgent(sel.getId()); syncMapAgents();
                    showRightPanel(null); panelSel.setVisible(false);
                } else setGraphInfo("⚠ Sélectionnez d'abord un agent.");
            }
        });

        agentsSec.getChildren().addAll(
            wrapFlow(5, bAddAg, bAdd10, bEv),
            wrapFlow(5, bEditAg, bDelAg));
        content.getChildren().add(agentsSec);

        // ── Section : SÉLECTION ───────────────────────────────────────────
        content.getChildren().add(sectionHeader("SÉLECTION"));
        panelSel = section();
        panelSel.setVisible(false);
        panelSel.setStyle("-fx-background-color:#0d111780;");
        lblSelNom    = lbl("—", FontWeight.BOLD,   12, TEXT);
        lblSelType   = lbl("—", FontWeight.NORMAL, 10, MUTED);
        lblSelAgents = lbl("0",   FontWeight.BOLD, 11, BLUE);
        lblSelPassed = lbl("0",   FontWeight.BOLD, 11, TEAL);
        lblSelSpeed  = lbl("0.00",FontWeight.BOLD, 11, GREEN);
        lblSelWait   = lbl("—",   FontWeight.BOLD, 11, ORANGE);
        lblSelStatus = lbl("Normal", FontWeight.BOLD, 11, GREEN);
        lblSelCap    = lbl("—",   FontWeight.BOLD, 11, MUTED);
        panelSel.getChildren().addAll(
            lblSelNom, lblSelType, new Separator(),
            hrow("Agents",    lblSelAgents),
            hrow("Passés",    lblSelPassed),
            hrow("Vit. moy.", lblSelSpeed),
            hrow("Capacité",  lblSelCap),
            hrow("Attente",   lblSelWait),
            hrow("Statut",    lblSelStatus));
        content.getChildren().add(panelSel);

        // ── Section : TRAJET ACTIF ────────────────────────────────────────
        content.getChildren().add(sectionHeader("TRAJET ACTIF"));
        panelPath = section();
        panelPath.setVisible(false);
        panelPath.setStyle("-fx-background-color:#0d111780;");
        lblPathInfo = lbl("Aucun trajet.", FontWeight.NORMAL, 9, MUTED);
        lblPathInfo.setWrapText(true);
        panelPath.getChildren().add(lblPathInfo);
        content.getChildren().add(panelPath);

        // ── Info graphe (pied) ────────────────────────────────────────────
        content.getChildren().add(sectionHeader("INFO"));
        VBox infoSec = section();
        lblGraphInfo = lbl("Prêt.", FontWeight.NORMAL, 9, MUTED);
        lblGraphInfo.setWrapText(true);
        infoSec.getChildren().add(lblGraphInfo);
        content.getChildren().add(infoSec);

        scroll.setContent(content);
        panel.getChildren().add(scroll);
        return panel;
    }

    // ═════════════════════════════════════════════════════════════════════
    // CARTE CENTRALE
    // ═════════════════════════════════════════════════════════════════════

    private StackPane buildMapContainer() {
        StackPane stack = new StackPane();
        stack.setStyle("-fx-background-color:#090e1a;");

        if (modele != null && mapView != null) {
            mapView.setAgents(modele.getAgents());
            modele.addZoneUpdateListener(mapView);
            stack.getChildren().add(mapView.getSwingNode());

            // Clic agent
            mapView.setOnAgentSelected(agent -> {
                if (ctrl != null) ctrl.selectAgent(agent);
                updateSelPanelForAgent(agent);
                // Si mode ADD_EDGE ou attente de zone, on ignore
                if (currentTool == ToolMode.ADD_AGENT && waitingAgZone) {
                    // ne pas changer de panneau
                } else if (currentTool == ToolMode.EDIT_AGENT) {
                    editingAgent = agent;
                    fillEditAgentForm(agent);
                    showRightPanel(panelEditAgent);
                }
            });

            // Clic zone
            mapCtrl.setOnZoneSelected(zone -> handleZoneClick(zone));

            // Clic arête
            mapView.setOnEdgeSelected(edge -> handleEdgeClick(edge));

            // Info graphe
            mapView.setOnGraphInfoChanged(info -> {
                if (lblGraphInfo != null) Platform.runLater(() -> lblGraphInfo.setText(info));
            });
        }

        return stack;
    }

    /**
     * Gestion centralisée du clic sur une zone selon le mode courant.
     */
    private void handleZoneClick(Zone zone) {
        switch (currentTool) {
            case ADD_NEIGHBORHOOD -> {
                // Ne rien faire : la position est choisie via le bouton "Cliquer carte"
                // ou on intercepte le clic brut
                if (waitingClickNh) {
                    waitingClickNh = false;
                    // position obtenue via mapView click direct — ici on prend les coords de la zone cliquée
                    pendingNhLat = zone.getLatitude();
                    pendingNhLng = zone.getLongitude();
                    updateNhCoordsLabel();
                }
            }
            case ADD_SHELTER -> {
                if (waitingClickSh) {
                    waitingClickSh = false;
                    pendingShLat = zone.getLatitude();
                    pendingShLng = zone.getLongitude();
                    updateShCoordsLabel();
                }
            }
            case ADD_AGENT -> {
                if (waitingAgZone) {
                    waitingAgZone = false;
                    pendingAgZone = zone;
                    if (lblAgZone != null) {
                        lblAgZone.setText(zone.getName());
                        lblAgZone.setTextFill(Color.web(GREEN));
                    }
                }
            }
            case ADD_EDGE -> {
                if (edgeZoneA == null) {
                    edgeZoneA = zone;
                    setGraphInfo("Zone A : " + zone.getName() + " — cliquez la zone B.");
                } else if (ctrl != null) {
                    ctrl.addEdgeBetween(edgeZoneA.getId(), zone.getId());
                    edgeZoneA = null;
                    refreshUI();
                    setGraphInfo("Route créée.");
                }
            }
            case EDIT_ZONE -> {
                editingZone = zone;
                fillEditZoneForm(zone);
                showRightPanel(panelEditZone);
            }
            default -> {
                if (ctrl != null) updateSelPanel(ctrl.getZoneStats(zone.getId()));
            }
        }
    }

    /**
     * Gestion centralisée du clic sur une arête.
     */
    private void handleEdgeClick(Edge edge) {
        if (edge == null) return;
        if (currentTool == ToolMode.EDIT_EDGE) {
            editingEdge = edge;
            fillEditEdgeForm(edge);
            showRightPanel(panelEditEdge);
        } else {
            if (ctrl != null) updateSelPanel(ctrl.getEdgeStats(edge.getId()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PANNEAU DROIT CONTEXTUEL — 240px
    // ═════════════════════════════════════════════════════════════════════

    private VBox buildRightPanel() {
        VBox panel = new VBox(0);
        panel.setMinWidth(240);
        panel.setMaxWidth(240);
        panel.setStyle("-fx-background-color:" + CARD
            + ";-fx-border-color:transparent transparent transparent " + BORDER + ";"
            + "-fx-border-width:0 0 0 1;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        rightContent = new VBox(0);
        rightContent.setStyle("-fx-background-color:" + CARD + ";");

        // Construire tous les sous-panneaux
        panelAddNeighborhood = buildPanelAddNeighborhood();
        panelAddShelter      = buildPanelAddShelter();
        panelAddAgent        = buildPanelAddAgent();
        panelEditZone        = buildPanelEditZone();
        panelEditEdge        = buildPanelEditEdge();
        panelEditAgent       = buildPanelEditAgent();
        panelLegend          = buildPanelLegend();

        // Par défaut : légende visible
        rightContent.getChildren().add(panelLegend);

        scroll.setContent(rightContent);
        panel.getChildren().add(scroll);
        return panel;
    }

    /** Affiche uniquement le panneau demandé dans le contenu droit (+ légende en bas). */
    private void showRightPanel(VBox target) {
        rightContent.getChildren().clear();
        if (target != null) rightContent.getChildren().add(target);
        rightContent.getChildren().add(panelLegend);
    }

    // ─── Formulaire : Ajouter un Quartier ────────────────────────────────

    private VBox buildPanelAddNeighborhood() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + CARD + ";");
        v.getChildren().add(sectionHeader("NOUVEAU QUARTIER"));
        VBox form = section();

        tfNhName = styledField("Nom (vide = auto)");
        tfNhAlt  = styledField("Altitude (m)");
        tfNhPop  = styledField("Population");
        tfNhDesc = styledField("Description");

        lblNhCoords = lbl("📍 Cliquez la carte", FontWeight.NORMAL, 9, MUTED);

        Button btnPickMap = smallBtn("🗺 Choisir position sur carte");
        btnPickMap.setMaxWidth(Double.MAX_VALUE);
        btnPickMap.setOnAction(e -> {
            waitingClickNh = true;
            setGraphInfo("Cliquez sur la carte pour placer le quartier.");
            lblNhCoords.setText("📍 En attente de clic…");
            lblNhCoords.setTextFill(Color.web(YELLOW));
            // On intercept aussi le clic brut via mapView en ajoutant un listener temporaire
            if (mapView != null) {
                mapView.setEditMode(MapView.EditMode.ADD_NODE);
            }
        });

        Button btnConfirm = actionBtn("＋ Créer le quartier", GREEN);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setOnAction(e -> confirmAddNeighborhood());

        Button btnCancel = smallBtn("Annuler");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> { showRightPanel(null); currentTool = ToolMode.NONE; });

        form.getChildren().addAll(
            lbl("Nom", FontWeight.BOLD, 9, MUTED), tfNhName,
            lbl("Altitude (m)", FontWeight.BOLD, 9, MUTED), tfNhAlt,
            lbl("Population", FontWeight.BOLD, 9, MUTED), tfNhPop,
            lbl("Description", FontWeight.BOLD, 9, MUTED), tfNhDesc,
            new Separator(),
            lbl("Position", FontWeight.BOLD, 9, MUTED), lblNhCoords, btnPickMap,
            new Separator(),
            btnConfirm, btnCancel);
        v.getChildren().add(form);
        return v;
    }

    private void resetNhForm() {
        tfNhName.clear(); tfNhAlt.clear(); tfNhPop.clear(); tfNhDesc.clear();
        pendingNhLat = Double.NaN; pendingNhLng = Double.NaN; waitingClickNh = false;
        lblNhCoords.setText("📍 Cliquez la carte");
        lblNhCoords.setTextFill(Color.web(MUTED));
    }

    private void updateNhCoordsLabel() {
        if (!Double.isNaN(pendingNhLat))
            Platform.runLater(() -> {
                lblNhCoords.setText(String.format("📍 %.4f, %.4f", pendingNhLat, pendingNhLng));
                lblNhCoords.setTextFill(Color.web(GREEN));
            });
    }

    private void confirmAddNeighborhood() {
        if (ctrl == null) return;
        String name = tfNhName.getText().trim().isEmpty() ? null : tfNhName.getText().trim();
        double lat  = !Double.isNaN(pendingNhLat) ? pendingNhLat : 45.7640 + (Math.random() - 0.5) * 0.06;
        double lng  = !Double.isNaN(pendingNhLng) ? pendingNhLng : 4.8357  + (Math.random() - 0.5) * 0.08;
        double alt  = parseDouble(tfNhAlt.getText(), 1.0);
        int    pop  = parseInt(tfNhPop.getText(), 100);
        String desc = tfNhDesc.getText().trim();
        ctrl.addNeighborhood(name, lat, lng, alt, pop, desc);
        refreshUI();
        showRightPanel(null);
        setGraphInfo("Quartier créé.");
        currentTool = ToolMode.NONE;
        if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
    }

    // ─── Formulaire : Ajouter un Refuge ──────────────────────────────────

    private VBox buildPanelAddShelter() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + CARD + ";");
        v.getChildren().add(sectionHeader("NOUVEAU REFUGE"));
        VBox form = section();

        tfShName = styledField("Nom (vide = auto)");
        tfShAlt  = styledField("Altitude (m)");
        tfShCap  = styledField("Capacité (personnes)");
        tfShDesc = styledField("Description");
        lblShCoords = lbl("📍 Cliquez la carte", FontWeight.NORMAL, 9, MUTED);

        Button btnPickMap = smallBtn("🗺 Choisir position sur carte");
        btnPickMap.setMaxWidth(Double.MAX_VALUE);
        btnPickMap.setOnAction(e -> {
            waitingClickSh = true;
            setGraphInfo("Cliquez sur la carte pour placer le refuge.");
            lblShCoords.setText("📍 En attente de clic…");
            lblShCoords.setTextFill(Color.web(YELLOW));
            if (mapView != null) mapView.setEditMode(MapView.EditMode.ADD_NODE);
        });

        Button btnConfirm = actionBtn("＋ Créer le refuge", PURPLE);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setOnAction(e -> confirmAddShelter());

        Button btnCancel = smallBtn("Annuler");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> { showRightPanel(null); currentTool = ToolMode.NONE; });

        form.getChildren().addAll(
            lbl("Nom", FontWeight.BOLD, 9, MUTED), tfShName,
            lbl("Altitude (m)", FontWeight.BOLD, 9, MUTED), tfShAlt,
            lbl("Capacité", FontWeight.BOLD, 9, MUTED), tfShCap,
            lbl("Description", FontWeight.BOLD, 9, MUTED), tfShDesc,
            new Separator(),
            lbl("Position", FontWeight.BOLD, 9, MUTED), lblShCoords, btnPickMap,
            new Separator(),
            btnConfirm, btnCancel);
        v.getChildren().add(form);
        return v;
    }

    private void resetShForm() {
        tfShName.clear(); tfShAlt.clear(); tfShCap.clear(); tfShDesc.clear();
        pendingShLat = Double.NaN; pendingShLng = Double.NaN; waitingClickSh = false;
        lblShCoords.setText("📍 Cliquez la carte");
        lblShCoords.setTextFill(Color.web(MUTED));
    }

    private void updateShCoordsLabel() {
        if (!Double.isNaN(pendingShLat))
            Platform.runLater(() -> {
                lblShCoords.setText(String.format("📍 %.4f, %.4f", pendingShLat, pendingShLng));
                lblShCoords.setTextFill(Color.web(GREEN));
            });
    }

    private void confirmAddShelter() {
        if (ctrl == null) return;
        String name = tfShName.getText().trim().isEmpty() ? null : tfShName.getText().trim();
        double lat  = !Double.isNaN(pendingShLat) ? pendingShLat : 45.7640 + (Math.random() - 0.5) * 0.06;
        double lng  = !Double.isNaN(pendingShLng) ? pendingShLng : 4.8357  + (Math.random() - 0.5) * 0.08;
        double alt  = parseDouble(tfShAlt.getText(), 3.0);
        int    cap  = parseInt(tfShCap.getText(), 200);
        String desc = tfShDesc.getText().trim();
        ctrl.addShelter(name, lat, lng, alt, cap, desc);
        refreshUI();
        showRightPanel(null);
        setGraphInfo("Refuge créé.");
        currentTool = ToolMode.NONE;
        if (mapView != null) mapView.setEditMode(MapView.EditMode.SELECT);
    }

    // ─── Formulaire : Ajouter un Agent ───────────────────────────────────

    private VBox buildPanelAddAgent() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + CARD + ";");
        v.getChildren().add(sectionHeader("NOUVEL AGENT"));
        VBox form = section();

        cbAgentRole = new ComboBox<>();
        cbAgentRole.getItems().addAll("Citoyen", "PMR", "Secours");
        cbAgentRole.setValue("Citoyen");
        cbAgentRole.setMaxWidth(Double.MAX_VALUE);
        styleCombo(cbAgentRole);
        cbAgentRole.valueProperty().addListener((o, ov, nv) -> {
            boolean showStress = "Citoyen".equals(nv);
            rbAgCalme.setVisible(showStress); rbAgCalme.setManaged(showStress);
            rbAgStresse.setVisible(showStress); rbAgStresse.setManaged(showStress);
        });

        tfAgFn  = styledField("Prénom (vide = aléatoire)");
        tfAgLn  = styledField("Nom (vide = aléatoire)");
        tfAgAge = styledField("Âge (vide = aléatoire)");

        slAgSpeed  = paramSlider(0.1, 5.0, 1.0);
        lblAgSpeed = lbl("1.0 m/s", FontWeight.BOLD, 10, TEAL);
        slAgSpeed.valueProperty().addListener((o, ov, nv) ->
            lblAgSpeed.setText(String.format("%.1f m/s", nv.doubleValue())));

        agStressGroup = new ToggleGroup();
        rbAgCalme   = styledRadio("Calme",   agStressGroup, GREEN);
        rbAgStresse = styledRadio("Stressé", agStressGroup, ORANGE);
        rbAgCalme.setSelected(true);
        HBox stressRow = new HBox(8, rbAgCalme, rbAgStresse);
        stressRow.setAlignment(Pos.CENTER_LEFT);

        lblAgZone = lbl("Aléatoire", FontWeight.BOLD, 10, MUTED);
        Button btnPickZ = smallBtn("🗺 Choisir zone");
        btnPickZ.setOnAction(e -> {
            waitingAgZone = true;
            lblAgZone.setText("Cliquez une zone…");
            lblAgZone.setTextFill(Color.web(YELLOW));
            setGraphInfo("Cliquez une zone de départ pour l'agent.");
        });
        Button btnClearZ = smallBtn("↺ Aléatoire");
        btnClearZ.setOnAction(e -> {
            waitingAgZone = false; pendingAgZone = null;
            lblAgZone.setText("Aléatoire");
            lblAgZone.setTextFill(Color.web(MUTED));
        });

        Button btnConfirm = actionBtn("＋ Ajouter l'agent", BLUE);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setOnAction(e -> confirmAddAgent());

        Button btnCancel = smallBtn("Annuler");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> { showRightPanel(null); currentTool = ToolMode.NONE; });

        form.getChildren().addAll(
            lbl("Rôle", FontWeight.BOLD, 9, MUTED), cbAgentRole,
            lbl("Prénom", FontWeight.BOLD, 9, MUTED), tfAgFn,
            lbl("Nom", FontWeight.BOLD, 9, MUTED), tfAgLn,
            lbl("Âge", FontWeight.BOLD, 9, MUTED), tfAgAge,
            lbl("Vitesse", FontWeight.BOLD, 9, MUTED), paramRow(slAgSpeed, lblAgSpeed),
            lbl("État", FontWeight.BOLD, 9, MUTED), stressRow,
            new Separator(),
            lbl("Zone de départ", FontWeight.BOLD, 9, MUTED),
            lblAgZone, new HBox(4, btnPickZ, btnClearZ),
            new Separator(),
            btnConfirm, btnCancel);
        v.getChildren().add(form);
        return v;
    }

    private void resetAgentForm() {
        cbAgentRole.setValue("Citoyen");
        tfAgFn.clear(); tfAgLn.clear(); tfAgAge.clear();
        slAgSpeed.setValue(1.0);
        rbAgCalme.setSelected(true);
        pendingAgZone = null; waitingAgZone = false;
        lblAgZone.setText("Aléatoire");
        lblAgZone.setTextFill(Color.web(MUTED));
    }

    private void confirmAddAgent() {
        if (ctrl == null) return;
        AgentRole role = switch (cbAgentRole.getValue()) {
            case "PMR"    -> AgentRole.PMR;
            case "Secours"-> AgentRole.RESCUE;
            default       -> AgentRole.CITIZEN;
        };
        String fn = tfAgFn.getText().trim().isEmpty() ? null : tfAgFn.getText().trim();
        String ln = tfAgLn.getText().trim().isEmpty()  ? null : tfAgLn.getText().trim();
        int    age   = -1;
        try { age = Integer.parseInt(tfAgAge.getText().trim()); } catch (NumberFormatException ignored) {}
        double speed  = slAgSpeed.getValue();
        int    stress = (role == AgentRole.CITIZEN)
            ? (rbAgCalme.isSelected() ? 0 : 1) : -1;

        ctrl.addAgent(role, fn, ln, age, speed, stress, pendingAgZone);
        syncMapAgents();
        showRightPanel(null);
        setGraphInfo("Agent ajouté.");
        currentTool = ToolMode.NONE;
    }

    // ─── Formulaire : Modifier une Zone ──────────────────────────────────

    private VBox buildPanelEditZone() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + CARD + ";");
        v.getChildren().add(sectionHeader("MODIFIER ZONE"));
        VBox form = section();

        tfEzName = styledField("Nom");
        tfEzAlt  = styledField("Altitude (m)");
        tfEzPop  = styledField("Population / Capacité");
        tfEzDesc = styledField("Description");

        Button btnConfirm = actionBtn("✓ Appliquer", GREEN);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setOnAction(e -> confirmEditZone());

        Button btnCancel = smallBtn("Annuler");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> { showRightPanel(null); currentTool = ToolMode.NONE; });

        form.getChildren().addAll(
            lbl("Nom", FontWeight.BOLD, 9, MUTED), tfEzName,
            lbl("Altitude (m)", FontWeight.BOLD, 9, MUTED), tfEzAlt,
            lbl("Population / Capacité", FontWeight.BOLD, 9, MUTED), tfEzPop,
            lbl("Description", FontWeight.BOLD, 9, MUTED), tfEzDesc,
            new Separator(),
            btnConfirm, btnCancel);
        v.getChildren().add(form);
        return v;
    }

    private void fillEditZoneForm(Zone z) {
        Platform.runLater(() -> {
            tfEzName.setText(z.getName() != null ? z.getName() : "");
            tfEzAlt.setText(String.format("%.1f", z.getAltitude()));
            tfEzPop.setText(String.valueOf((int) z.getPopulation()));
            tfEzDesc.setText(""); // description non exposée universellement
        });
    }

    private void confirmEditZone() {
        if (ctrl == null || editingZone == null) return;
        String name = tfEzName.getText().trim().isEmpty() ? null : tfEzName.getText().trim();
        double alt  = parseDouble(tfEzAlt.getText(), editingZone.getAltitude());
        int    pop  = parseInt(tfEzPop.getText(), (int) editingZone.getPopulation());
        String desc = tfEzDesc.getText().trim();
        ctrl.updateZoneParams(editingZone.getId(), name, alt, pop, desc);
        refreshUI();
        showRightPanel(null);
        setGraphInfo("Zone mise à jour.");
        currentTool = ToolMode.NONE;
    }

    // ─── Formulaire : Modifier une Arête ─────────────────────────────────

    private VBox buildPanelEditEdge() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + CARD + ";");
        v.getChildren().add(sectionHeader("MODIFIER ROUTE"));
        VBox form = section();

        tfEeCap = styledField("Capacité max (agents)");

        Button btnConfirm = actionBtn("✓ Appliquer", GREEN);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setOnAction(e -> confirmEditEdge());

        Button btnCancel = smallBtn("Annuler");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> { showRightPanel(null); currentTool = ToolMode.NONE; });

        form.getChildren().addAll(
            lbl("Capacité maximale", FontWeight.BOLD, 9, MUTED), tfEeCap,
            new Separator(),
            btnConfirm, btnCancel);
        v.getChildren().add(form);
        return v;
    }

    private void fillEditEdgeForm(Edge e) {
        Platform.runLater(() -> tfEeCap.setText(String.valueOf(e.getCapacityMax())));
    }

    private void confirmEditEdge() {
        if (ctrl == null || editingEdge == null) return;
        int cap = parseInt(tfEeCap.getText(), editingEdge.getCapacityMax());
        ctrl.updateEdgeParams(editingEdge.getId(), cap);
        refreshUI();
        showRightPanel(null);
        setGraphInfo("Route mise à jour.");
        currentTool = ToolMode.NONE;
    }

    // ─── Formulaire : Modifier un Agent ──────────────────────────────────

    private VBox buildPanelEditAgent() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + CARD + ";");
        v.getChildren().add(sectionHeader("MODIFIER AGENT"));
        VBox form = section();

        tfEaFn  = styledField("Prénom");
        tfEaLn  = styledField("Nom");
        tfEaAge = styledField("Âge");

        slEaSpeed  = paramSlider(0.1, 5.0, 1.0);
        lblEaSpeed = lbl("1.0 m/s", FontWeight.BOLD, 10, TEAL);
        slEaSpeed.valueProperty().addListener((o, ov, nv) ->
            lblEaSpeed.setText(String.format("%.1f m/s", nv.doubleValue())));

        eaStressGroup = new ToggleGroup();
        rbEaCalme   = styledRadio("Calme",   eaStressGroup, GREEN);
        rbEaStresse = styledRadio("Stressé", eaStressGroup, ORANGE);
        rbEaCalme.setSelected(true);
        HBox stressRow = new HBox(8, rbEaCalme, rbEaStresse);
        stressRow.setAlignment(Pos.CENTER_LEFT);

        Button btnConfirm = actionBtn("✓ Appliquer", GREEN);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setOnAction(e -> confirmEditAgent());

        Button btnCancel = smallBtn("Annuler");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> { showRightPanel(null); currentTool = ToolMode.NONE; });

        form.getChildren().addAll(
            lbl("Prénom", FontWeight.BOLD, 9, MUTED), tfEaFn,
            lbl("Nom", FontWeight.BOLD, 9, MUTED), tfEaLn,
            lbl("Âge", FontWeight.BOLD, 9, MUTED), tfEaAge,
            lbl("Vitesse", FontWeight.BOLD, 9, MUTED), paramRow(slEaSpeed, lblEaSpeed),
            lbl("État", FontWeight.BOLD, 9, MUTED), stressRow,
            new Separator(),
            btnConfirm, btnCancel);
        v.getChildren().add(form);
        return v;
    }

    private void fillEditAgentForm(Agent agent) {
        Platform.runLater(() -> {
            tfEaFn.setText(agent.getFirstName() != null ? agent.getFirstName() : "");
            tfEaLn.setText(agent.getLastName()  != null ? agent.getLastName()  : "");
            tfEaAge.setText(String.valueOf(agent.getAge()));
            slEaSpeed.setValue(agent.getMaxSpeed());
            if (agent instanceof Citizen c) {
                boolean stressed = c.getState() == model.enums.CitizenState.STRESSED;
                if (stressed) rbEaStresse.setSelected(true);
                else rbEaCalme.setSelected(true);
            } else {
                rbEaCalme.setVisible(false); rbEaCalme.setManaged(false);
                rbEaStresse.setVisible(false); rbEaStresse.setManaged(false);
            }
        });
    }

    private void confirmEditAgent() {
        if (ctrl == null || editingAgent == null) return;
        String fn  = tfEaFn.getText().trim().isEmpty() ? null : tfEaFn.getText().trim();
        String ln  = tfEaLn.getText().trim().isEmpty()  ? null : tfEaLn.getText().trim();
        int    age = parseInt(tfEaAge.getText(), -1);
        double spd = slEaSpeed.getValue();
        int stress = (editingAgent instanceof Citizen)
            ? (rbEaCalme.isSelected() ? 0 : 1) : -1;
        ctrl.updateAgentParams(editingAgent.getId(), fn, ln, age, spd, stress);
        syncMapAgents();
        showRightPanel(null);
        setGraphInfo("Agent mis à jour.");
        currentTool = ToolMode.NONE;
    }

    // ─── Légende permanente ───────────────────────────────────────────────

    private VBox buildPanelLegend() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + CARD + ";");
        v.getChildren().add(sectionHeader("LÉGENDE"));
        VBox leg = section();
        leg.getChildren().addAll(
            lbl("Nœuds", FontWeight.BOLD, 9, MUTED),
            colorRow(TEAL,   "Quartier"),
            colorRow(PURPLE, "Refuge"),
            colorRow(RED,    "Inondé"),
            colorRow(YELLOW, "Sélectionné"),
            lbl("Routes", FontWeight.BOLD, 9, MUTED),
            colorRow(GREEN,  "Sûre"),
            colorRow(ORANGE, "À risque"),
            colorRow(YELLOW, "Congestionnée"),
            colorRow(RED,    "Surchargée / Inondée"),
            lbl("Agents", FontWeight.BOLD, 9, MUTED),
            colorRow(BLUE,   "Citoyen / PMR"),
            colorRow(GREEN,  "En sécurité"),
            colorRow(RED,    "Bloqué"),
            colorRow(TEAL,   "Secouriste"));
        v.getChildren().add(leg);
        return v;
    }

    // ═════════════════════════════════════════════════════════════════════
    // BARRE BASSE — 80px
    // ═════════════════════════════════════════════════════════════════════

    private HBox buildBottomBar() {
        HBox bar = new HBox(0);
        bar.setMinHeight(80);
        bar.setMaxHeight(80);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:" + CARD
            + ";-fx-border-color:" + BORDER + " transparent transparent transparent;"
            + "-fx-border-width:1 0 0 0;");

        VBox b1 = bottomBloc("GÉNÉRAL");
        lblPopRisque    = val("0", ORANGE);
        lblPersonnesSec = val("0", GREEN);
        lblAgentsActifs = val("0", BLUE);
        b1.getChildren().addAll(
            statRow("○", "À évacuer",   lblPopRisque),
            statRow("□", "En sécurité", lblPersonnesSec),
            statRow("▣", "Agents",      lblAgentsActifs));

        VBox b2 = bottomBloc("RÉSEAU");
        lblSures = val("0%", TEXT); lblRisque = val("0%", TEXT);
        lblCong  = val("0%", TEXT); lblOver   = val("0%", TEXT); lblInond = val("0%", TEXT);
        b2.getChildren().addAll(
            barRow("Sûres",      GREEN,  lblSures),
            barRow("Risque",     ORANGE, lblRisque),
            barRow("Congestion", YELLOW, lblCong),
            barRow("Surcharge",  RED,    lblOver),
            barRow("Inondées",   PURPLE, lblInond));

        VBox b3 = bottomBloc("REFUGES");
        lblRefTotal  = val("0", TEXT);
        lblRefAccess = val("0", GREEN);
        lblRefInacc  = val("0", RED);
        b3.getChildren().addAll(
            statRow("⌂", "Total",         lblRefTotal),
            statRow("✓", "Accessibles",   lblRefAccess),
            statRow("✗", "Inaccessibles", lblRefInacc));

        VBox b4 = bottomBloc("NIVEAU EAU");
        lblNiveauActuel = lbl("0.00 m",     FontWeight.BOLD, 18, BLUE);
        lblNiveauMax    = lbl("Max 0.00 m", FontWeight.NORMAL, 10, RED);
        b4.getChildren().addAll(lblNiveauActuel, lblNiveauMax);

        VBox b5 = bottomBloc("DÉPLACEMENTS");
        lblActifs  = val("0", TEAL);
        lblBloques = val("0", RED);
        lblCongNds = val("0", ORANGE);
        b5.getChildren().addAll(
            statRow("→", "En transit",   lblActifs),
            statRow("✗", "Bloqués",      lblBloques),
            statRow("⚠", "Nœuds cong.", lblCongNds));

        refreshLoop = new Timeline(new KeyFrame(Duration.millis(400), e -> {
            refreshUI();
            if (ctrl != null && modele != null) {
                lblActifs.setText(String.valueOf(ctrl.getNombreAgentsEnDeplacement()));
                long nc = modele.getZones().stream()
                    .filter(z -> ctrl.isZoneOvercrowded(z.getId())).count();
                lblCongNds.setText(String.valueOf(nc));
            }
        }));
        refreshLoop.setCycleCount(Timeline.INDEFINITE);
        refreshLoop.play();

        for (VBox b : new VBox[]{b1, b2, b3, b4, b5}) {
            HBox.setHgrow(b, Priority.ALWAYS);
            b.setMaxWidth(Double.MAX_VALUE);
        }
        bar.getChildren().addAll(b1, vDiv(), b2, vDiv(), b3, vDiv(), b4, vDiv(), b5);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════
    // UPDATE PANELS SÉLECTION
    // ═════════════════════════════════════════════════════════════════════

    private void updateSelPanel(NodeEdgeStats s) {
        if (s == null || panelSel == null) return;
        Platform.runLater(() -> {
            panelSel.setVisible(true);
            lblSelNom.setText(s.name);
            lblSelType.setText(s.type);
            lblSelAgents.setText(String.valueOf(s.currentAgents));
            lblSelPassed.setText(String.valueOf(s.totalPassed));
            lblSelSpeed.setText(String.format("%.2f ag/cyc", s.avgSpeed));
            lblSelCap.setText(s.capacity > 0 ? String.valueOf(s.capacity) : "—");
            lblSelWait.setText(s.congested ? s.waitCycles + " cyc" : "—");
            lblSelStatus.setText(s.status);
            String col = switch (s.status) {
                case "Inondé", "OVERLOADED" -> RED;
                case "Forte congestion", "AT_RISK" -> ORANGE;
                case "CONGESTED" -> YELLOW;
                default -> GREEN;
            };
            lblSelStatus.setTextFill(Color.web(col));
        });
    }

    private void updateSelPanelForAgent(Agent agent) {
        if (panelSel == null) return;
        Platform.runLater(() -> {
            panelSel.setVisible(true);
            String name = ((agent.getFirstName() != null ? agent.getFirstName() + " " : "")
                + (agent.getLastName() != null ? agent.getLastName() : "")).trim();
            lblSelNom.setText(name.isBlank() ? "Agent #" + agent.getId() : name);
            lblSelType.setText(agent.getClass().getSimpleName()
                + " | vit. " + String.format("%.1f", agent.getMaxSpeed()) + " m/s");
            lblSelStatus.setText("En déplacement");
            lblSelStatus.setTextFill(Color.web(BLUE));
        });
    }

    private void updatePathPanel(List<Zone> path) {
        Platform.runLater(() -> {
            if (panelPath == null) return;
            panelPath.setVisible(!path.isEmpty());
            if (path.isEmpty()) { lblPathInfo.setText("Aucun trajet actif."); return; }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.size(); i++) {
                sb.append(path.get(i).getName());
                if (i < path.size() - 1) sb.append(" →\n");
            }
            lblPathInfo.setText(sb.toString());
        });
    }

    // ═════════════════════════════════════════════════════════════════════
    // IMPORT / EXPORT
    // ═════════════════════════════════════════════════════════════════════

    private void handleExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'état");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Flood Simulation", "*.flood"));
        fc.setInitialFileName("simulation.flood");
        File f = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (f != null && ctrl != null) ctrl.exportState(f);
    }

    private void handleImport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importer un état");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Flood Simulation", "*.flood"));
        File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (f != null && ctrl != null) { stopSim(); ctrl.importState(f); refreshUI(); }
    }

    // ═════════════════════════════════════════════════════════════════════
    // BOUCLES SIMULATION
    // ═════════════════════════════════════════════════════════════════════

    private void startSim() {
        if (simLoop != null) simLoop.stop();
        double ms = ctrl != null ? ctrl.getVitesseSimulationMs() : 1500;
        simLoop = new Timeline(new KeyFrame(Duration.millis(ms),
            e -> { if (ctrl != null) ctrl.executerPas(); }));
        simLoop.setCycleCount(Timeline.INDEFINITE);
        simLoop.play();
        simRunning = true;
    }

    private void stopSim() {
        if (simLoop != null) simLoop.stop();
        simRunning = false;
    }

    public void stopAll() {
        if (refreshLoop != null) refreshLoop.stop();
        stopSim();
    }

    public void stopRefresh() { stopAll(); }

    private void startRefreshLoop() { /* lancé dans buildBottomBar */ }

    // ═════════════════════════════════════════════════════════════════════
    // REFRESH UI
    // ═════════════════════════════════════════════════════════════════════

    private void refreshUI() {
        if (modele == null) return;
        try {
            int s = (int) modele.getTempsEcoule();
            lblTimer.setText(String.format("⏱ %02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60));
            boolean pause = modele.isEnPause();
            lblStatus.setText(pause ? "En pause" : "En cours");
            lblStatus.setTextFill(Color.web(pause ? ORANGE : GREEN));

            double nv = modele.getNiveauEau() / SimulationController.FLOOD_SPEED_FACTOR;
            lblNiveauActuel.setText(String.format("%.2f m", nv));
            lblNiveauMax.setText(String.format("Max %.2f m", nv * 1.78));

            if (ctrl != null) {
                double[] r = ctrl.getStatutReseau();
                if (r != null && r.length >= 5) {
                    lblSures.setText(String.format("%.0f%%", r[0]));
                    lblRisque.setText(String.format("%.0f%%", r[1]));
                    lblCong.setText(String.format("%.0f%%", r[2]));
                    lblOver.setText(String.format("%.0f%%", r[3]));
                    lblInond.setText(String.format("%.0f%%", r[4]));
                }
                lblPopRisque.setText(String.valueOf(ctrl.getPopulationARisque()));
                lblPersonnesSec.setText(String.valueOf(ctrl.getPopulationEnSecurite()));
            }
            lblAgentsActifs.setText(String.valueOf(modele.getNombreAgents()));

            if (mapCtrl != null) {
                lblRefTotal.setText(String.valueOf(mapCtrl.countSheltersTotal()));
                lblRefAccess.setText(String.valueOf(mapCtrl.countSheltersAccessible()));
                lblRefInacc.setText(String.valueOf(Math.max(0,
                    mapCtrl.countSheltersTotal() - mapCtrl.countSheltersAccessible())));
            }
        } catch (Exception ex) {
            System.err.println("refreshUI: " + ex.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS AGENTS
    // ═════════════════════════════════════════════════════════════════════

    private void syncMapAgents() {
        if (mapCtrl != null) mapCtrl.syncAgents(modele.getAgents());
        if (mapView != null) mapView.setAgents(modele.getAgents());
        try { Main.getSharedAdminCtrl().loadAgents(); } catch (Exception ignored) {}
        refreshUI();
    }

    private void setGraphInfo(String msg) {
        if (lblGraphInfo != null) Platform.runLater(() -> lblGraphInfo.setText(msg));
    }

    // ═════════════════════════════════════════════════════════════════════
    // GESTION DES MODES OUTILS
    // ═════════════════════════════════════════════════════════════════════

    private void setTool(ToolMode mode, Button... buttons) {
        currentTool = mode;
        // tous inactifs, puis le premier actif
        for (Button b : buttons) applyToolStyle(b, false);
        if (buttons.length > 0) applyToolStyle(buttons[0], true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS VISUELS
    // ═════════════════════════════════════════════════════════════════════

    private HBox sectionHeader(String title) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(5, 12, 5, 12));
        h.setStyle("-fx-background-color:" + BORDER + ";");
        h.getChildren().add(lbl(title, FontWeight.BOLD, 9, MUTED));
        return h;
    }

    private VBox section() {
        VBox v = new VBox(5);
        v.setPadding(new Insets(7, 10, 7, 10));
        return v;
    }

    private VBox bottomBloc(String title) {
        VBox b = new VBox(3);
        b.setPadding(new Insets(6, 12, 6, 12));
        b.setAlignment(Pos.CENTER_LEFT);
        b.getChildren().add(lbl(title, FontWeight.BOLD, 9, MUTED));
        return b;
    }

    private Label lbl(String t, FontWeight fw, int sz, String col) {
        Label l = new Label(t);
        l.setFont(Font.font("System", fw, sz));
        l.setTextFill(Color.web(col));
        return l;
    }

    private Label val(String t, String col) { return lbl(t, FontWeight.BOLD, 11, col); }

    private HBox hrow(String label, Label val) {
        HBox r = new HBox(6);
        r.setAlignment(Pos.CENTER_LEFT);
        Label l = lbl(label + ":", FontWeight.NORMAL, 10, MUTED);
        l.setMinWidth(58);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(l, sp, val);
        return r;
    }

    private HBox statRow(String icon, String label, Label val) {
        HBox r = new HBox(5);
        r.setAlignment(Pos.CENTER_LEFT);
        Label ic = lbl(icon, FontWeight.BOLD, 11, MUTED);
        ic.setMinWidth(14);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(ic, lbl(label, FontWeight.NORMAL, 10, MUTED), sp, val);
        return r;
    }

    private HBox barRow(String label, String color, Label val) {
        HBox r = new HBox(5);
        r.setAlignment(Pos.CENTER_LEFT);
        Rectangle dot = new Rectangle(10, 3, Color.web(color));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(dot, lbl(label, FontWeight.NORMAL, 10, MUTED), sp, val);
        return r;
    }

    private HBox colorRow(String color, String text) {
        HBox r = new HBox(6);
        r.setAlignment(Pos.CENTER_LEFT);
        r.setPadding(new Insets(1, 0, 1, 0));
        Rectangle rect = new Rectangle(12, 6, Color.web(color));
        rect.setArcWidth(2); rect.setArcHeight(2);
        r.getChildren().addAll(rect, lbl(text, FontWeight.NORMAL, 10, MUTED));
        return r;
    }

    private HBox paramRow(Slider sl, Label val) {
        HBox r = new HBox(5);
        r.setAlignment(Pos.CENTER_LEFT);
        sl.setPrefWidth(120);
        r.getChildren().addAll(sl, val);
        return r;
    }

    private HBox padBox(javafx.scene.Node node, double top, double right, double bottom, double left) {
        HBox b = new HBox(node);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(top, right, bottom, left));
        return b;
    }

    private Slider paramSlider(double min, double max, double val) {
        Slider s = new Slider(min, max, val);
        s.setStyle("-fx-control-inner-background:#1e293b;-fx-accent:" + BLUE + ";");
        return s;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT
            + ";-fx-prompt-text-fill:" + MUTED + ";-fx-background-radius:5;"
            + "-fx-font-size:11px;-fx-padding:5 8 5 8;");
        return tf;
    }

    private void styleCombo(ComboBox<?> cb) {
        cb.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT
            + ";-fx-font-size:10px;-fx-background-radius:5;");
    }

    private RadioButton styledRadio(String text, ToggleGroup group, String activeColor) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setFont(Font.font("System", FontWeight.NORMAL, 10));
        rb.setTextFill(Color.web(MUTED));
        rb.setStyle("-fx-cursor:hand;");
        rb.selectedProperty().addListener((o, ov, nv) ->
            rb.setTextFill(Color.web(nv ? activeColor : MUTED)));
        return rb;
    }

    private Button smallBtn(String t) {
        Button b = new Button(t);
        String base  = "-fx-background-color:#1e293b;-fx-text-fill:" + TEXT
            + ";-fx-background-radius:5;-fx-font-size:10px;-fx-padding:4 7 4 7;-fx-cursor:hand;";
        String hover = "-fx-background-color:" + BLUE + ";-fx-text-fill:white;"
            + "-fx-background-radius:5;-fx-font-size:10px;-fx-padding:4 7 4 7;-fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Button actionBtn(String t, String col) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + col + "22;-fx-text-fill:" + col
            + ";-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;"
            + "-fx-border-color:" + col + "55;-fx-border-radius:6;-fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:" + col + ";-fx-text-fill:white;"
            + "-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;"));
        b.setOnMouseExited(e  -> b.setStyle(
            "-fx-background-color:" + col + "22;-fx-text-fill:" + col
            + ";-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;"
            + "-fx-border-color:" + col + "55;-fx-border-radius:6;-fx-cursor:hand;"));
        return b;
    }

    /** Bouton outil : inactif par défaut, met en surbrillance quand actif. */
    private Button toolBtn(String t, boolean active) {
        Button b = new Button(t);
        applyToolStyle(b, active);
        return b;
    }

    private void applyToolStyle(Button b, boolean active) {
        b.setStyle(active
            ? "-fx-background-color:#1e40af;-fx-text-fill:#93c5fd;"
              + "-fx-background-radius:5;-fx-font-size:10px;-fx-padding:4 7 4 7;-fx-cursor:hand;"
            : "-fx-background-color:#1e293b;-fx-text-fill:" + TEXT + ";"
              + "-fx-background-radius:5;-fx-font-size:10px;-fx-padding:4 7 4 7;-fx-cursor:hand;");
    }

    private Button modeBtn(String t, boolean active) {
        Button b = new Button(t);
        applyModeStyle(b, active);
        return b;
    }

    private void applyModeStyle(Button b, boolean active) {
        b.setStyle(active
            ? "-fx-background-color:#1e40af;-fx-text-fill:#93c5fd;"
              + "-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 11 5 11;-fx-cursor:hand;"
            : "-fx-background-color:#1e293b;-fx-text-fill:" + MUTED + ";"
              + "-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 11 5 11;-fx-cursor:hand;");
    }

    private void activateMode(Button active, Button... others) {
        applyModeStyle(active, true);
        for (Button b : others) applyModeStyle(b, false);
    }

    private Button iconBtn(String icon) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT
            + ";-fx-background-radius:6;-fx-font-size:14px;-fx-padding:4 10 4 10;-fx-cursor:hand;");
        return b;
    }

    private Rectangle vDiv() {
        return new Rectangle(1, 60, Color.web(BORDER));
    }

    private FlowPane wrapFlow(double gap, Button... buttons) {
        FlowPane fp = new FlowPane(gap, gap);
        fp.getChildren().addAll(buttons);
        return fp;
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS PARSING
    // ═════════════════════════════════════════════════════════════════════

    private double parseDouble(String s, double defaultVal) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return defaultVal; }
    }

    private int parseInt(String s, int defaultVal) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return defaultVal; }
    }

    // ─── Overlay ajout agent (modal, conservé pour compatibilité externe) ─

    public void showAddAgentOverlay() {
        // Redirige vers le panneau droit intégré
        showRightPanel(panelAddAgent);
        resetAgentForm();
    }
}