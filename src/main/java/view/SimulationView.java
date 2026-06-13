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
import model.graph.Edge;
import model.simulation.FloodSimulation;
import model.zone.Zone;

/**
 * Vue principale de simulation — layout corrigé.
 *
 * Structure :
 *   TOP    → barre fixe 52px (retour + modes + chrono + vitesse)
 *   LEFT   → panneau outils 240px (graphe, agents, sélection, trajet)
 *   CENTER → carte (prend tout l'espace restant)
 *   RIGHT  → panneau 210px (params agents + légende)
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

    // ─── Panneau sélection (gauche bas) ──────────────────────────────────
    private VBox  panelSel;
    private Label lblSelNom, lblSelType, lblSelStatus;
    private Label lblSelAgents, lblSelPassed, lblSelSpeed, lblSelWait, lblSelCap;

    // ─── Panneau trajet (gauche) ─────────────────────────────────────────
    private VBox  panelPath;
    private Label lblPathInfo;

    // ─── Info graphe ─────────────────────────────────────────────────────
    private Label lblGraphInfo;

    // ─── Overlay ajout agent ─────────────────────────────────────────────
    private StackPane overlayAddAgent;
    private TextField tfFirstName, tfLastName, tfAge;
    private ComboBox<String> cbRole;
    private Label lblAddAgentZone;
    private Zone  pendingZone = null;
    private boolean waitingForZoneClick = false;

    // ─── Params agents ───────────────────────────────────────────────────
    private Slider slAgeMin, slAgeMax, slSpeedMin, slSpeedMax;
    private Label  lblAgeMin, lblAgeMax, lblSpeedMin, lblSpeedMax;

    // ─── Timelines ───────────────────────────────────────────────────────
    private Timeline refreshLoop;
    private Timeline simLoop;

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

    // ─────────────────────────────────────────────────────────────────────
    // TOP BAR — hauteur fixe 52px
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinHeight(52);
        bar.setMaxHeight(52);
        bar.setPrefHeight(52);
        bar.setStyle("-fx-background-color:" + CARD
            + ";-fx-border-color:transparent transparent " + BORDER
            + " transparent;-fx-border-width:0 0 1 0;");

        // ← Retour
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
        Button btnAlea   = modeBtn("⟳ Aléatoire", true);
        Button btnManual = modeBtn("↺ Manuel",     false);
        Button btnStep   = modeBtn("⏭ Pas-à-pas",  false);
        Button btnReset  = modeBtn("↻ Restart",    false);

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

        HBox modes = new HBox(6, btnAlea, btnManual, btnStep, btnReset);
        modes.setAlignment(Pos.CENTER);
        modes.setPadding(new Insets(0, 16, 0, 16));
        modes.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Chrono + statut
        lblTimer  = lbl("⏱ 00:00:00", FontWeight.BOLD, 13, TEXT);
        lblStatus = lbl("En pause",    FontWeight.NORMAL, 11, ORANGE);
        VBox chronoBox = new VBox(2, lblTimer, lblStatus);
        chronoBox.setAlignment(Pos.CENTER_LEFT);
        HBox chrono = padBox(chronoBox, 0, 16, 0, 16);
        chrono.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Vitesse + play/pause
        Slider slVitesse = new Slider(200, 5000, 1500);
        slVitesse.setPrefWidth(120);
        slVitesse.setStyle("-fx-control-inner-background:#1e293b;-fx-accent:" + BLUE + ";");
        Label lblVitesse = lbl("1500ms", FontWeight.BOLD, 10, TEXT);
        slVitesse.valueProperty().addListener((o, ov, nv) -> {
            int v = (int) Math.round(nv.doubleValue());
            lblVitesse.setText(v + "ms");
            if (ctrl != null) ctrl.setVitesseSimulation(v);
            if (simLoop != null && simLoop.getStatus() == Timeline.Status.RUNNING) startSim();
        });
        Button bPlay  = iconBtn("▶");
        Button bPause = iconBtn("⏸");
        bPlay.setOnAction(e  -> { if (ctrl != null) { ctrl.reprendreSimulation(); startSim(); } });
        bPause.setOnAction(e -> { if (ctrl != null) ctrl.mettreEnPause(); stopSim(); });

        HBox speedBox = new HBox(6, lbl("Vitesse:", FontWeight.NORMAL, 10, MUTED),
                                    slVitesse, lblVitesse, bPlay, bPause);
        speedBox.setAlignment(Pos.CENTER);
        speedBox.setPadding(new Insets(0, 16, 0, 16));
        speedBox.setStyle("-fx-border-color:transparent " + BORDER + " transparent transparent;-fx-border-width:0 1 0 0;");

        // Import/Export
        Button btnExport = smallBtn("↑ Exporter");
        Button btnImport = smallBtn("↓ Importer");
        btnExport.setOnAction(e -> handleExport());
        btnImport.setOnAction(e -> handleImport());
        HBox ioBox = new HBox(6, btnExport, btnImport);
        ioBox.setAlignment(Pos.CENTER);
        ioBox.setPadding(new Insets(0, 12, 0, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(backBox, logo, modes, chrono, speedBox, spacer, ioBox);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PANNEAU GAUCHE — outils graphe + sélection + trajet (240px fixe)
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildLeftPanel() {
        VBox panel = new VBox(0);
        panel.setMinWidth(240);
        panel.setMaxWidth(240);
        panel.setPrefWidth(240);
        panel.setStyle("-fx-background-color:" + CARD
            + ";-fx-border-color:transparent " + BORDER + " transparent transparent;"
            + "-fx-border-width:0 1 0 0;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:#0d1117;-fx-background-color:#0d1117;"
            + "-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:" + CARD + ";");

        // ── Section : Édition graphe ──
        content.getChildren().add(sectionHeader("ÉDITION GRAPHE"));

        VBox editSec = section();
        lblGraphInfo = lbl("Clic sur nœud, arête ou agent.", FontWeight.NORMAL, 9, MUTED);
        lblGraphInfo.setWrapText(true);

        // Modes édition
        Button bSel  = smallBtn("⊙ Sélection");
        Button bAddN = smallBtn("＋ Nœud");
        Button bAddE = smallBtn("＋ Arête");
        Button bMove = smallBtn("↕ Déplacer");
        Button bDel  = smallBtn("🗑 Supprimer");
        bSel.setOnAction(e  -> { mapView.setEditMode(MapView.EditMode.SELECT);    lblGraphInfo.setText("Mode sélection."); });
        bAddN.setOnAction(e -> { mapView.setEditMode(MapView.EditMode.ADD_NODE);  lblGraphInfo.setText("Clic carte → ajouter un nœud."); });
        bAddE.setOnAction(e -> { mapView.setEditMode(MapView.EditMode.ADD_EDGE);  lblGraphInfo.setText("1. Nœud SOURCE → 2. Nœud DESTINATION."); });
        bMove.setOnAction(e -> { mapView.setEditMode(MapView.EditMode.MOVE_NODE); lblGraphInfo.setText("Glisse un nœud."); });
        bDel.setOnAction(e  -> { mapView.setEditMode(MapView.EditMode.DELETE);    lblGraphInfo.setText("Clic nœud ou arête → supprimer."); });

        WrapFlow editFlow = new WrapFlow(4, bSel, bAddN, bAddE, bMove, bDel);

        // Zones
        Button bNeigh  = smallBtn("＋ Quartier");
        Button bShelt  = smallBtn("＋ Refuge");
        Button b5Nodes = smallBtn("＋ 5 nœuds");
        Button bResetZ = smallBtn("↺ Reset zones");
        bNeigh.setOnAction(e  -> { if (ctrl != null) { ctrl.addRandomNeighborhood(); refreshUI(); }});
        bShelt.setOnAction(e  -> { if (ctrl != null) { ctrl.addRandomShelter();      refreshUI(); }});
        b5Nodes.setOnAction(e -> {
            if (ctrl != null) {
                for (int i = 0; i < 3; i++) ctrl.addRandomNeighborhood();
                for (int i = 0; i < 2; i++) ctrl.addRandomShelter();
                refreshUI(); lblGraphInfo.setText("5 nœuds ajoutés.");
            }
        });
        bResetZ.setOnAction(e -> { if (ctrl != null) { ctrl.resetAllZones(); refreshUI(); }});

        // Suppr avec reloc
        Button bDelZone = smallBtn("🗑 Nœud+reloc");
        Button bDelEdge = smallBtn("🗑 Arête+reloc");
        bDelZone.setOnAction(e -> {
            if (mapCtrl != null && ctrl != null) {
                Zone sel = mapCtrl.getSelectedZone();
                if (sel != null) { ctrl.removeZone(sel.getId()); refreshUI(); }
                else lblGraphInfo.setText("⚠ Sélectionne un nœud.");
            }
        });
        bDelEdge.setOnAction(e -> {
            if (mapView != null && ctrl != null) {
                Edge selEdge = mapView.getSelectedEdge();
                if (selEdge != null) { ctrl.removeEdgeWithAgentRelocation(selEdge.getId()); refreshUI(); }
                else lblGraphInfo.setText("⚠ Sélectionne une arête.");
            }
        });

        WrapFlow zoneFlow  = new WrapFlow(4, bNeigh, bShelt, b5Nodes, bResetZ, bDelZone, bDelEdge);

        editSec.getChildren().addAll(
            lbl("Modes édition", FontWeight.BOLD, 9, MUTED), editFlow,
            lbl("Nœuds / Zones", FontWeight.BOLD, 9, MUTED), zoneFlow,
            lblGraphInfo);
        content.getChildren().add(editSec);

        // ── Section : Agents ──
        content.getChildren().add(sectionHeader("AGENTS"));
        VBox agentSec = section();
        Button bAddAgent = smallBtn("＋ Agent…");
        Button b10       = smallBtn("＋ 10 agents");
        Button bEv       = smallBtn("⚡ Évacuer tous");
        Button bRmA      = smallBtn("🗑 Sél. agent");
        bAddAgent.setOnAction(e -> showAddAgentOverlay());
        b10.setOnAction(e -> { if (ctrl != null) { ctrl.addRandomAgents(10); syncMapAgents(); }});
        bEv.setOnAction(e -> { if (mapCtrl != null) { mapCtrl.evacuateAllCitizensToShelters(); refreshUI(); }});
        bRmA.setOnAction(e -> {
            if (mapView != null && ctrl != null && modele != null) {
                Agent sel = mapView.getSelectedAgent();
                if (sel != null) { ctrl.removeAgent(sel.getId()); syncMapAgents(); panelSel.setVisible(false); }
                else lblGraphInfo.setText("⚠ Sélectionne un agent.");
            }
        });

        // Densité
        Button bDensOn  = smallBtn("◉ Densité ON");
        Button bDensOff = smallBtn("○ Densité OFF");
        bDensOn.setOnAction(e  -> { if (mapView != null) mapView.setDensityOverlayEnabled(true); });
        bDensOff.setOnAction(e -> { if (mapView != null) mapView.setDensityOverlayEnabled(false); });

        // Fin de trajet
        ComboBox<String> cbEnd = new ComboBox<>();
        cbEnd.getItems().addAll("Destination aléatoire", "Supprimer l'agent");
        cbEnd.setValue("Destination aléatoire");
        cbEnd.setMaxWidth(Double.MAX_VALUE);
        styleCombo(cbEnd);
        cbEnd.valueProperty().addListener((o, ov, nv) -> {
            if (ctrl == null) return;
            ctrl.setAgentEndBehavior("Supprimer l'agent".equals(nv)
                ? SimulationController.AgentEndBehavior.REMOVE_AGENT
                : SimulationController.AgentEndBehavior.RANDOM_DESTINATION);
        });

        agentSec.getChildren().addAll(
            new WrapFlow(4, bAddAgent, b10, bEv, bRmA),
            new WrapFlow(4, bDensOn, bDensOff),
            lbl("Fin de trajet", FontWeight.BOLD, 9, MUTED), cbEnd);
        content.getChildren().add(agentSec);

        // ── Section : Élément sélectionné ──
        content.getChildren().add(sectionHeader("SÉLECTION"));
        panelSel = section();
        panelSel.setVisible(false);
        lblSelNom    = lbl("—", FontWeight.BOLD,   12, TEXT);
        lblSelType   = lbl("—", FontWeight.NORMAL, 10, MUTED);
        lblSelAgents = lbl("0", FontWeight.BOLD,   11, BLUE);
        lblSelPassed = lbl("0", FontWeight.BOLD,   11, TEAL);
        lblSelSpeed  = lbl("0.00", FontWeight.BOLD, 11, GREEN);
        lblSelWait   = lbl("—",   FontWeight.BOLD,  11, ORANGE);
        lblSelStatus = lbl("Normal", FontWeight.BOLD, 11, GREEN);
        lblSelCap    = lbl("—", FontWeight.BOLD,   11, MUTED);
        panelSel.getChildren().addAll(
            lblSelNom, lblSelType, new Separator(),
            hrow("Agents",    lblSelAgents),
            hrow("Passés",    lblSelPassed),
            hrow("Vit. moy.", lblSelSpeed),
            hrow("Capacité",  lblSelCap),
            hrow("Attente",   lblSelWait),
            hrow("Statut",    lblSelStatus));
        content.getChildren().add(panelSel);

        // ── Section : Trajet ──
        content.getChildren().add(sectionHeader("TRAJET ACTIF"));
        panelPath = section();
        panelPath.setVisible(false);
        lblPathInfo = lbl("Aucun trajet.", FontWeight.NORMAL, 9, MUTED);
        lblPathInfo.setWrapText(true);
        panelPath.getChildren().add(lblPathInfo);
        content.getChildren().add(panelPath);

        scroll.setContent(content);
        panel.getChildren().add(scroll);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CARTE CENTRALE — StackPane pour overlay ajout agent
    // ─────────────────────────────────────────────────────────────────────

    private StackPane buildMapContainer() {
        StackPane stack = new StackPane();
        stack.setStyle("-fx-background-color:#090e1a;");

        if (modele != null) {
            mapView.setAgents(modele.getAgents());
            modele.addZoneUpdateListener(mapView);
            stack.getChildren().add(mapView.getSwingNode());

            mapView.setOnAgentSelected(agent -> {
                if (ctrl != null) ctrl.selectAgent(agent);
                updateSelPanelForAgent(agent);
                if (waitingForZoneClick) cancelZoneWait();
            });

            mapCtrl.setOnZoneSelected(zone -> {
                if (waitingForZoneClick) {
                    pendingZone = zone;
                    waitingForZoneClick = false;
                    lblAddAgentZone.setText("Zone : " + zone.getName());
                    lblAddAgentZone.setTextFill(Color.web(GREEN));
                    return;
                }
                if (ctrl != null) updateSelPanel(ctrl.getZoneStats(zone.getId()));
            });

            mapView.setOnEdgeSelected(edge -> {
                if (ctrl != null && edge != null)
                    updateSelPanel(ctrl.getEdgeStats(edge.getId()));
            });

            mapView.setOnGraphInfoChanged(info -> {
                if (lblGraphInfo != null) lblGraphInfo.setText(info);
            });
        }

        overlayAddAgent = buildAddAgentOverlay();
        StackPane.setAlignment(overlayAddAgent, Pos.CENTER);
        stack.getChildren().add(overlayAddAgent);
        return stack;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PANNEAU DROIT — params agents + légende (210px)
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildRightPanel() {
        VBox panel = new VBox(0);
        panel.setMinWidth(210);
        panel.setMaxWidth(210);
        panel.setPrefWidth(210);
        panel.setStyle("-fx-background-color:" + CARD
            + ";-fx-border-color:transparent transparent transparent " + BORDER + ";"
            + "-fx-border-width:0 0 0 1;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:#0d1117;-fx-background-color:#0d1117;-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:" + CARD + ";");

        // ── Params agents ──
        content.getChildren().add(sectionHeader("PARAMÈTRES AGENTS"));
        VBox paramSec = section();

        slAgeMin  = paramSlider(1, 100, ctrl != null ? ctrl.getAgentAgeMin() : 18);
        slAgeMax  = paramSlider(1, 100, ctrl != null ? ctrl.getAgentAgeMax() : 75);
        lblAgeMin = lbl(String.valueOf((int) slAgeMin.getValue()), FontWeight.BOLD, 10, BLUE);
        lblAgeMax = lbl(String.valueOf((int) slAgeMax.getValue()), FontWeight.BOLD, 10, BLUE);
        slAgeMin.valueProperty().addListener((o, ov, nv) -> {
            lblAgeMin.setText(String.valueOf(nv.intValue()));
            if (ctrl != null) ctrl.setAgentAgeRange(nv.intValue(), (int) slAgeMax.getValue());
        });
        slAgeMax.valueProperty().addListener((o, ov, nv) -> {
            lblAgeMax.setText(String.valueOf(nv.intValue()));
            if (ctrl != null) ctrl.setAgentAgeRange((int) slAgeMin.getValue(), nv.intValue());
        });

        slSpeedMin  = paramSlider(0.1, 5.0, ctrl != null ? ctrl.getAgentSpeedMin() : 0.5);
        slSpeedMax  = paramSlider(0.1, 5.0, ctrl != null ? ctrl.getAgentSpeedMax() : 2.0);
        lblSpeedMin = lbl(String.format("%.1f", slSpeedMin.getValue()), FontWeight.BOLD, 10, TEAL);
        lblSpeedMax = lbl(String.format("%.1f", slSpeedMax.getValue()), FontWeight.BOLD, 10, TEAL);
        slSpeedMin.valueProperty().addListener((o, ov, nv) -> {
            lblSpeedMin.setText(String.format("%.1f", nv.doubleValue()));
            if (ctrl != null) ctrl.setAgentSpeedRange(nv.doubleValue(), slSpeedMax.getValue());
        });
        slSpeedMax.valueProperty().addListener((o, ov, nv) -> {
            lblSpeedMax.setText(String.format("%.1f", nv.doubleValue()));
            if (ctrl != null) ctrl.setAgentSpeedRange(slSpeedMin.getValue(), nv.doubleValue());
        });

        Button bSave = smallBtn("💾 Sauvegarder état initial");
        bSave.setMaxWidth(Double.MAX_VALUE);
        bSave.setOnAction(e -> { if (ctrl != null) ctrl.saveCurrentStateAsInitial(); });

        paramSec.getChildren().addAll(
            lbl("Âge min / max", FontWeight.BOLD, 9, MUTED),
            paramRow(slAgeMin, lblAgeMin), paramRow(slAgeMax, lblAgeMax),
            lbl("Vitesse min / max", FontWeight.BOLD, 9, MUTED),
            paramRow(slSpeedMin, lblSpeedMin), paramRow(slSpeedMax, lblSpeedMax),
            bSave);
        content.getChildren().add(paramSec);

        // ── Légende ──
        content.getChildren().add(sectionHeader("LÉGENDE"));
        VBox legSec = section();
        legSec.getChildren().addAll(
            lbl("Nœuds", FontWeight.BOLD, 9, MUTED),
            colorRow(BLUE,   "Quartier"),
            colorRow(PURPLE, "Refuge"),
            colorRow(RED,    "Inondé"),
            lbl("Arêtes", FontWeight.BOLD, 9, MUTED),
            colorRow(GREEN,  "Sûre"),
            colorRow(ORANGE, "À risque"),
            colorRow(YELLOW, "Congestionnée"),
            colorRow(RED,    "Surchargée"),
            colorRow(PURPLE, "Inondée"),
            lbl("Agents", FontWeight.BOLD, 9, MUTED),
            colorRow(BLUE,  "Citoyen"),
            colorRow(GREEN, "En sécurité"),
            colorRow(RED,   "Bloqué"),
            colorRow(TEAL,  "Secours"));
        content.getChildren().add(legSec);

        scroll.setContent(content);
        panel.getChildren().add(scroll);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────
    // BARRE BASSE — stats globales (80px fixe)
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildBottomBar() {
        HBox bar = new HBox(0);
        bar.setMinHeight(80);
        bar.setMaxHeight(80);
        bar.setPrefHeight(80);
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
            barRow("Sûres",     GREEN,  lblSures),
            barRow("Risque",    ORANGE, lblRisque),
            barRow("Congestion",YELLOW, lblCong),
            barRow("Surcharge", RED,    lblOver),
            barRow("Inondées",  PURPLE, lblInond));

        VBox b3 = bottomBloc("REFUGES");
        lblRefTotal  = val("0", TEXT);
        lblRefAccess = val("0", GREEN);
        lblRefInacc  = val("0", RED);
        b3.getChildren().addAll(
            statRow("⌂", "Total",         lblRefTotal),
            statRow("✓", "Accessibles",   lblRefAccess),
            statRow("✗", "Inaccessibles", lblRefInacc));

        VBox b4 = bottomBloc("NIVEAU EAU");
        lblNiveauActuel = lbl("0.00 m", FontWeight.BOLD, 18, BLUE);
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

        // Espaceurs égaux entre les blocs
        for (VBox b : new VBox[]{b1, b2, b3, b4, b5}) {
            HBox.setHgrow(b, Priority.ALWAYS);
            b.setMaxWidth(Double.MAX_VALUE);
        }

        bar.getChildren().addAll(b1, vDiv(), b2, vDiv(), b3, vDiv(), b4, vDiv(), b5);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────
    // OVERLAY AJOUT AGENT
    // ─────────────────────────────────────────────────────────────────────

    private StackPane buildAddAgentOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.6);");
        overlay.setVisible(false);
        overlay.setPickOnBounds(true);

        VBox form = new VBox(10);
        form.setMaxWidth(320);
        form.setPadding(new Insets(18, 20, 18, 20));
        form.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:10;"
            + "-fx-border-color:" + BORDER + ";-fx-border-radius:10;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.7),20,0,0,4);");

        cbRole = new ComboBox<>();
        cbRole.getItems().addAll("Citoyen", "Secours");
        cbRole.setValue("Citoyen");
        styleCombo(cbRole);
        tfFirstName = styledField("Prénom (vide = aléatoire)");
        tfLastName  = styledField("Nom (vide = aléatoire)");
        tfAge       = styledField("Âge (vide = aléatoire)");

        lblAddAgentZone = lbl("Zone : aléatoire", FontWeight.NORMAL, 10, MUTED);
        Button btnPickZone   = smallBtn("🗺 Choisir sur carte");
        Button btnRandomZone = smallBtn("↺ Aléatoire");
        btnPickZone.setOnAction(e -> startZoneWait(overlay));
        btnRandomZone.setOnAction(e -> cancelZoneWait());

        Button btnOk      = actionBtn("✓ Ajouter",  GREEN);
        Button btnAnnuler = actionBtn("✗ Annuler",  RED);
        btnOk.setMaxWidth(Double.MAX_VALUE);
        btnAnnuler.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnOk, Priority.ALWAYS);
        HBox.setHgrow(btnAnnuler, Priority.ALWAYS);
        btnAnnuler.setOnAction(e -> hideAddAgentOverlay());
        btnOk.setOnAction(e -> confirmAddAgent());

        form.getChildren().addAll(
            lbl("AJOUTER UN AGENT", FontWeight.BOLD, 12, TEXT),
            fieldRow("Rôle",   cbRole),
            fieldRow("Prénom", tfFirstName),
            fieldRow("Nom",    tfLastName),
            fieldRow("Âge",    tfAge),
            new Separator(),
            lbl("Localisation", FontWeight.BOLD, 9, MUTED),
            lblAddAgentZone,
            new HBox(8, btnPickZone, btnRandomZone),
            new Separator(),
            new HBox(10, btnOk, btnAnnuler));

        overlay.getChildren().add(form);
        return overlay;
    }

    private void showAddAgentOverlay() {
        tfFirstName.clear(); tfLastName.clear(); tfAge.clear();
        cbRole.setValue("Citoyen");
        pendingZone = null; waitingForZoneClick = false;
        lblAddAgentZone.setText("Zone : aléatoire");
        lblAddAgentZone.setTextFill(Color.web(MUTED));
        overlayAddAgent.setVisible(true);
    }

    private void hideAddAgentOverlay() {
        overlayAddAgent.setVisible(false);
        waitingForZoneClick = false;
    }

    private void startZoneWait(StackPane overlay) {
        waitingForZoneClick = true;
        lblAddAgentZone.setText("Cliquez une zone sur la carte…");
        lblAddAgentZone.setTextFill(Color.web(YELLOW));
        overlay.setPickOnBounds(false);
    }

    private void cancelZoneWait() {
        waitingForZoneClick = false;
        pendingZone = null;
        lblAddAgentZone.setText("Zone : aléatoire");
        lblAddAgentZone.setTextFill(Color.web(MUTED));
        overlayAddAgent.setPickOnBounds(true);
    }

    private void confirmAddAgent() {
        if (ctrl == null) { hideAddAgentOverlay(); return; }
        AgentRole role = "Secours".equals(cbRole.getValue()) ? AgentRole.RESCUE : AgentRole.CITIZEN;
        String fn = tfFirstName.getText().trim().isEmpty() ? null : tfFirstName.getText().trim();
        String ln = tfLastName.getText().trim().isEmpty()  ? null : tfLastName.getText().trim();
        int age = -1;
        try { age = Integer.parseInt(tfAge.getText().trim()); } catch (NumberFormatException ignored) {}
        ctrl.addAgent(role, fn, ln, age, -1, -1, pendingZone);
        syncMapAgents();
        hideAddAgentOverlay();
    }

    // ─────────────────────────────────────────────────────────────────────
    // UPDATE PANELS
    // ─────────────────────────────────────────────────────────────────────

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
        Platform.runLater(() -> {
            panelSel.setVisible(true);
            lblSelNom.setText((agent.getFirstName() != null
                ? agent.getFirstName() + " " : "") + "#" + agent.getId());
            lblSelType.setText(agent.getClass().getSimpleName());
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
                if (i < path.size() - 1) sb.append(" → ");
            }
            lblPathInfo.setText(sb.toString());
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // IMPORT / EXPORT
    // ─────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────
    // BOUCLES SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    private void startSim() {
        if (simLoop != null) simLoop.stop();
        double ms = ctrl != null ? ctrl.getVitesseSimulationMs() : 1500;
        simLoop = new Timeline(new KeyFrame(Duration.millis(ms),
            e -> { if (ctrl != null) ctrl.executerPas(); }));
        simLoop.setCycleCount(Timeline.INDEFINITE);
        simLoop.play();
    }

    private void stopSim()  { if (simLoop != null) simLoop.stop(); }

    public void stopAll() {
        if (refreshLoop != null) refreshLoop.stop();
        if (simLoop     != null) simLoop.stop();
    }

    public void stopRefresh() { stopAll(); }

    private void startRefreshLoop() { /* lancé dans buildBottomBar */ }

    // ─────────────────────────────────────────────────────────────────────
    // REFRESH UI
    // ─────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS AGENTS
    // ─────────────────────────────────────────────────────────────────────

    private void syncMapAgents() {
        if (mapCtrl != null) mapCtrl.syncAgents(modele.getAgents());
        if (mapView != null) mapView.setAgents(modele.getAgents());
        try { Main.getSharedAdminCtrl().loadAgents(); } catch (Exception ignored) {}
        refreshUI();
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS VISUELS
    // ─────────────────────────────────────────────────────────────────────

    /** En-tête de section pour les panneaux latéraux. */
    private HBox sectionHeader(String title) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(6, 12, 6, 12));
        h.setStyle("-fx-background-color:" + BORDER + ";");
        Label l = lbl(title, FontWeight.BOLD, 9, MUTED);
        h.getChildren().add(l);
        return h;
    }

    /** Section body avec padding standard. */
    private VBox section() {
        VBox v = new VBox(6);
        v.setPadding(new Insets(8, 10, 8, 10));
        return v;
    }

    /** Bloc pour la barre basse. */
    private VBox bottomBloc(String title) {
        VBox b = new VBox(4);
        b.setPadding(new Insets(6, 14, 6, 14));
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
        l.setMinWidth(60);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(l, sp, val);
        return r;
    }

    private HBox statRow(String icon, String label, Label val) {
        HBox r = new HBox(6);
        r.setAlignment(Pos.CENTER_LEFT);
        Label ic = lbl(icon, FontWeight.BOLD, 11, MUTED);
        ic.setMinWidth(14);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(ic, lbl(label, FontWeight.NORMAL, 10, MUTED), sp, val);
        return r;
    }

    private HBox barRow(String label, String color, Label val) {
        HBox r = new HBox(6);
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
        HBox r = new HBox(6);
        r.setAlignment(Pos.CENTER_LEFT);
        sl.setPrefWidth(110);
        r.getChildren().addAll(sl, val);
        return r;
    }

    private HBox fieldRow(String label, Control ctrl) {
        HBox r = new HBox(10);
        r.setAlignment(Pos.CENTER_LEFT);
        Label l = lbl(label, FontWeight.NORMAL, 10, MUTED);
        l.setMinWidth(55);
        ctrl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(ctrl, Priority.ALWAYS);
        r.getChildren().addAll(l, ctrl);
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
            + ";-fx-background-radius:6;-fx-font-size:14px;-fx-padding:4 9 4 9;-fx-cursor:hand;");
        return b;
    }

    private Rectangle vDiv() {
        return new Rectangle(1, 60, Color.web(BORDER));
    }

    // ─────────────────────────────────────────────────────────────────────
    // WrapFlow — HBox qui wrap les boutons sur plusieurs lignes
    // ─────────────────────────────────────────────────────────────────────

    private static class WrapFlow extends FlowPane {
        WrapFlow(double gap, Button... buttons) {
            super(gap, gap);
            getChildren().addAll(buttons);
        }
    }
}