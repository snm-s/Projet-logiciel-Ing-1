package view;

import java.util.List;

import app.Main;
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
import model.agent.Agent;
import model.simulation.FloodSimulation;
import model.zone.Zone;

/**
 * Vue simulation : carte OSM + graphe interactif + agents Mii sur arêtes.
 * Couvre les consignes : nœuds/arêtes ajout/modif/déplacement/suppression,
 * ajout masse, agents ajoutés/supprimés, sélection/statistiques, vitesse, pause, pas.
 */
public class SimulationView extends BorderPane {

    private static final String BG_DARK = "#0d1117";
    private static final String BG_CARD = "#131b2e";
    private static final String ACCENT_BLUE = "#3b82f6";
    private static final String ACCENT_TEAL = "#06b6d4";
    private static final String ACCENT_GREEN = "#22c55e";
    private static final String ACCENT_ORANGE = "#f59e0b";
    private static final String ACCENT_RED = "#ef4444";
    private static final String TEXT_PRIMARY = "#f1f5f9";
    private static final String TEXT_MUTED = "#94a3b8";
    private static final String BORDER_COLOR = "#1e293b";

    private final SimulationController controller;
    private final FloodSimulation modele;
    private MapController mapController;
    private MapView mapView;

    private Label lblSelectedZone, lblNiveauEauZone, lblStatutZone;
    private Label lblPopRisque, lblPersonnesSec, lblAgentsActifs;
    private Label lblAretesSures, lblAretesRisque, lblAretesInond, lblAretesCong, lblAretesOver;
    private Label lblRefugesTotal, lblRefugesAccess, lblRefugesInacc;
    private Label lblNiveauActuel, lblNiveauMax, lblTempsRestant, lblZoneNiveaux;
    private Label lblTimer, lblSimStatus, lblVitesseVal, lblGraphInfo;
    private Button btnPause, btnPlay, btnStop, btnAleatoire, btnManuelle;
    private Slider sliderVitesse;
    private ToggleButton[] zoneButtons;
    private int selectedZoneIndex = 0;
    private Timeline refreshTimeline;
    private Timeline simTimeline;

    public SimulationView(SimulationController controller) {
        this.controller = controller;
        this.modele = controller != null ? controller.getModele() : null;

        this.mapView = Main.getSharedMapView();
        this.mapController = Main.getSharedMapController();

        setStyle("-fx-background-color:" + BG_DARK + ";");
        buildContent();
        startRefreshLoop();
    }

    private void buildContent() {
        setTop(buildTopBar());
        setCenter(buildMapContainer());
        setBottom(buildBottomBar());
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:transparent transparent " + BORDER_COLOR + " transparent;-fx-border-width:0 0 1 0;");
        bar.setPrefHeight(48);

        HBox logo = new HBox(10);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setPadding(new Insets(0, 20, 0, 16));
        logo.setPrefWidth(230);
        logo.setStyle("-fx-border-color:transparent " + BORDER_COLOR + " transparent transparent;-fx-border-width:0 1 0 0;");
        Label ico = new Label("");
ico.setMinWidth(0);
ico.setPrefWidth(0);
        VBox titleBox = new VBox(1,
                styledLabel("Inondation", FontWeight.BOLD, 13, TEXT_PRIMARY),
                styledLabel("Graphe + agents Mii", FontWeight.NORMAL, 10, TEXT_MUTED));
                logo.getChildren().add(titleBox);

        HBox modeBox = new HBox(8);
        modeBox.setAlignment(Pos.CENTER);
        modeBox.setPadding(new Insets(0, 20, 0, 20));
        btnAleatoire = modeButton("⟳  Aléatoire", true);
btnManuelle = modeButton("↺  Manuelle", false);
Button btnRecommencer = modeButton("↻  Recommencer", false);

btnAleatoire.setOnAction(e -> {
    setModeActive(btnAleatoire, btnManuelle, btnRecommencer);

    if (mapView != null) {
        mapView.setManualFloodMode(false);
        mapView.startRandomFlood();
    }

    if (controller != null) {
        controller.setModeAleatoire(true);
        controller.demarrerSimulation();
        startSimLoop();
    }
});

btnManuelle.setOnAction(e -> {
    setModeActive(btnManuelle, btnAleatoire, btnRecommencer);

    if (mapView != null) {
        mapView.setManualFloodMode(true);
    }

    if (controller != null) {
        controller.setModeAleatoire(false);

        // Mode manuel : on lance officiellement la simulation et l'alerte,
        // mais les agents ne bougent PAS tant qu'aucune inondation manuelle
        // n'a été placée sur la carte.
        controller.demarrerSimulation();
        startSimLoop(); // boucle active, mais le controller attend le clic manuel
    }

    if (lblSimStatus != null) {
        lblSimStatus.setText("Mode manuel — cliquez sur la carte pour lancer l’inondation");
    }
});

btnRecommencer.setOnAction(e -> {
    if (mapView != null) {
        mapView.resetManualFlood();
    }

    if (controller != null) {
        controller.resetSimulation();
        controller.mettreEnPause();
    }

    stopSimLoop();
    refreshUI();

    lblSimStatus.setText("En pause");
    lblTimer.setText("⏱  00:00:00");
});

modeBox.getChildren().addAll(btnAleatoire, btnManuelle, btnRecommencer);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        lblSimStatus = styledLabel("En pause", FontWeight.NORMAL, 11, ACCENT_ORANGE);
        lblTimer = styledLabel("⏱  00:00:00", FontWeight.BOLD, 12, TEXT_PRIMARY);
        HBox status = new HBox(10, lblSimStatus, lblTimer);
        status.setAlignment(Pos.CENTER);
        status.setPadding(new Insets(0, 20, 0, 0));
        Button btnBack = new Button("← Retour");
        btnBack.setStyle(
            "-fx-background-color:#1e293b;" +
            "-fx-text-fill:#94a3b8;" +
            "-fx-background-radius:6;" +
            "-fx-font-size:11px;" +
            "-fx-padding:5 12 5 12;" +
            "-fx-cursor:hand;"
        );
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(
            "-fx-background-color:#3b82f6;" +
            "-fx-text-fill:white;" +
            "-fx-background-radius:6;" +
            "-fx-font-size:11px;" +
            "-fx-padding:5 12 5 12;" +
            "-fx-cursor:hand;"
        ));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(
            "-fx-background-color:#1e293b;" +
            "-fx-text-fill:#94a3b8;" +
            "-fx-background-radius:6;" +
            "-fx-font-size:11px;" +
            "-fx-padding:5 12 5 12;" +
            "-fx-cursor:hand;"
        ));
        btnBack.setOnAction(e -> {
            stopRefresh();
            Main.showDashboardView("admin");
        });

        bar.getChildren().addAll(btnBack, logo, modeBox, spacer, status);
        return bar;
    }

    private StackPane buildMapContainer() {
        StackPane stack = new StackPane();
        stack.setStyle("-fx-background-color:#090e1a;");

        if (modele != null) {

            this.mapView = Main.getSharedMapView();
            this.mapController = Main.getSharedMapController();
            
            mapView.setAgents(modele.getAgents());
            modele.addZoneUpdateListener(mapView);
            stack.getChildren().add(mapView.getSwingNode());

            mapView.setOnAgentSelected(agent -> {
                lblSelectedZone.setText(agent.getFirstName() != null ? agent.getFirstName() : "Agent #" + agent.getId());
                lblNiveauEauZone.setText("Agent sélectionné");
                lblStatutZone.setText(agent.getClass().getSimpleName());
                lblStatutZone.setTextFill(Color.web(ACCENT_TEAL));
            });
            mapView.setOnGraphInfoChanged(info -> lblGraphInfo.setText(info));

            mapController.setOnZoneSelected(zone -> {
                int idx = modele.getZones().indexOf(zone);
                if (idx >= 0) selectedZoneIndex = idx;
                lblSelectedZone.setText("Zone " + zone.getName());
                lblNiveauEauZone.setText(String.format("%.2f m", modele.getNiveauEau()));
                boolean flooded = zone.isFlooded();
                double nv = modele.getNiveauEau();
                lblStatutZone.setText(flooded ? "Inondée ⚠" : nv > 0.5 ? "En montée ↗" : "Stable →");
                lblStatutZone.setTextFill(Color.web(flooded ? ACCENT_RED : nv > 0.5 ? ACCENT_ORANGE : ACCENT_GREEN));
            });
        }

        VBox panelZone = buildZoneInfoPanel();
        StackPane.setAlignment(panelZone, Pos.TOP_LEFT);
        StackPane.setMargin(panelZone, new Insets(12, 0, 0, 12));

        VBox tools = buildGraphToolsPanel();
        StackPane.setAlignment(tools, Pos.TOP_CENTER);
        StackPane.setMargin(tools, new Insets(12, 0, 0, 0));

        VBox legend = buildLegendePanel();
        StackPane.setAlignment(legend, Pos.TOP_RIGHT);
        StackPane.setMargin(legend, new Insets(12, 12, 0, 0));

        stack.getChildren().addAll(panelZone, tools, legend);

        if (controller != null) {
            controller.setOnStatusChanged(s -> lblSimStatus.setText(s));
            controller.setOnWaterLevelChanged(l -> lblNiveauActuel.setText(String.format("%.2f m", l)));
            controller.setOnZonesUpdated(zs -> { if (mapView != null) mapView.updateAllZones(zs); });
        }
        return stack;
    }

    private VBox buildZoneInfoPanel() {
        VBox p = cardPanel(230);
        lblSelectedZone = styledLabel("Sélection —", FontWeight.BOLD, 15, TEXT_PRIMARY);
        lblNiveauEauZone = styledLabel("—", FontWeight.BOLD, 11, ACCENT_BLUE);
        lblStatutZone = styledLabel("Stable →", FontWeight.BOLD, 11, ACCENT_GREEN);
        lblGraphInfo = styledLabel("Clique un agent, un nœud ou une arête.", FontWeight.NORMAL, 10, TEXT_MUTED);
        lblGraphInfo.setWrapText(true);
        p.getChildren().addAll(
                styledLabel("ÉLÉMENT SÉLECTIONNÉ", FontWeight.BOLD, 10, TEXT_MUTED),
                lblSelectedZone,
                hrow("Niveau d'eau", lblNiveauEauZone),
                hrow("Statut", lblStatutZone),
                new Separator(),
                lblGraphInfo);
        return p;
    }

    private VBox buildGraphToolsPanel() {
        VBox box = cardPanel(620);
        box.getChildren().add(styledLabel("INTERACTION AVEC LE GRAPHE", FontWeight.BOLD, 10, TEXT_MUTED));

        HBox modes = new HBox(6);
        modes.setAlignment(Pos.CENTER);
        Button select = smallButton("Sélection");
        Button addNode = smallButton("＋ Nœud");
        Button addEdge = smallButton("＋ Arête");
        Button moveNode = smallButton("↕ Déplacer nœud");
        Button del = smallButton("🗑 Supprimer");

        HBox zones = new HBox(6);
        zones.setAlignment(Pos.CENTER);

        Button addNeighborhood = smallButton("＋ Quartier");
        Button addShelter = smallButton("＋ Refuges");
        Button removeZone = smallButton("🗑 Suppr. zone sélect.");
        Button resetZones = smallButton("↺ Reset zones");

        addNeighborhood.setOnAction(e -> {
            if (controller != null) { controller.addRandomNeighborhood();    refreshUI(); }
        });
        addShelter.setOnAction(e -> {
            if (controller != null) { controller.addRandomShelter(); refreshUI(); }
        });

        removeZone.setOnAction(e -> {
            // La zone sélectionnée est celle trackée par mapController
            if (mapController != null && controller != null) {
                model.zone.Zone selected = mapController.getSelectedZone();
                if (selected != null) {
                    controller.removeZone(selected.getId());
                    refreshUI();
                }
            }
        });

        resetZones.setOnAction(e -> {
            if (controller != null) {
                // Remet toutes les zones à l'état non-inondé
                modele.getZones().forEach(z -> z.reset());
                // Notifie via un updateZone sur chaque zone
                modele.getZones().forEach(z -> controller.updateZone(z));
                refreshUI();
            }
        });


    


        select.setOnAction(e -> mapView.setEditMode(MapView.EditMode.SELECT));
        addNode.setOnAction(e -> mapView.setEditMode(MapView.EditMode.ADD_NODE));
        addEdge.setOnAction(e -> mapView.setEditMode(MapView.EditMode.ADD_EDGE));
        moveNode.setOnAction(e -> mapView.setEditMode(MapView.EditMode.MOVE_NODE));
        del.setOnAction(e -> mapView.setEditMode(MapView.EditMode.DELETE));
        modes.getChildren().addAll(select, addNode, addEdge, moveNode, del);

        HBox mass = new HBox(6);
        mass.setAlignment(Pos.CENTER);
        Button add5Nodes = smallButton("Ajouter 5 nœuds");
        Button add1Agent = smallButton("+1 agent");
        Button add10Agents = smallButton("+10 agents");
        Button evacuate = smallButton("Évacuer → refuges");
        Button removeAgent = smallButton("Suppr. agent");
        add5Nodes.setOnAction(e -> { if (mapView != null) mapView.addRandomNodes(5); });
        add1Agent.setOnAction(e -> addOneAgent());
        add10Agents.setOnAction(e -> {
            if (controller != null) {
                controller.addRandomCitizens(10);
        
                if (mapView != null && modele != null) {
                    mapView.setAgents(modele.getAgents());
                }
        
                app.Main.getSharedAdminCtrl().loadAgents();
                refreshUI();
            }
        });
        evacuate.setOnAction(e -> { if (mapController != null) { mapController.evacuateAllCitizensToShelters(); refreshUI(); } });
        removeAgent.setOnAction(e -> {
            if (mapView == null || controller == null || modele == null) return;
        
            Agent selected = mapView.getSelectedAgent();
        
            if (selected == null) {
                lblGraphInfo.setText("Clique d'abord sur un agent.");
                return;
            }
        
            int id = selected.getId();
        
            controller.removeAgent(id);
        
            mapView.setAgents(modele.getAgents());
        
            lblGraphInfo.setText("Agent supprimé.");
            lblSelectedZone.setText("Sélection —");
        
            app.Main.getSharedAdminCtrl().loadAgents();
            refreshUI();
        });

        mass.getChildren().addAll(add5Nodes, add1Agent, add10Agents, evacuate, removeAgent);
        zones.getChildren().addAll(addNeighborhood, addShelter, removeZone, resetZones);
        box.getChildren().addAll(modes, mass, zones);
        return box;
    }

    private VBox buildLegendePanel() {
        VBox p = cardPanel(205);
    
        p.getChildren().add(styledLabel("ACTIONS TEMPORELLES", FontWeight.BOLD, 10, TEXT_MUTED));
        p.getChildren().add(buildControlButtons());
        p.getChildren().add(buildSpeedRow());
    
        return p;
    }

    private void addOneAgent() {
        if (controller == null || modele == null) return;
    
        controller.addRandomCitizen();
    
        if (mapController != null) {
            mapController.syncAgents(modele.getAgents());
        }
    
        if (mapView != null) {
            mapView.setAgents(modele.getAgents());
        }
    
        app.Main.getSharedAdminCtrl().loadAgents();
        refreshUI();
    }

    private GridPane buildZoneGrid() {
        ToggleGroup tg = new ToggleGroup();
        String[] names = {"A", "B", "C", "D", "E", "F"};
        zoneButtons = new ToggleButton[6];
        GridPane g = new GridPane();
        g.setHgap(4);
        g.setVgap(4);
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
        btnPause = iconBtn("⏸");
        btnPlay = iconBtn("▶");
        btnStop = iconBtn("⏹");
        Button btnStep = iconBtn("⏭");
        btnPause.setOnAction(e -> { if (controller != null) controller.mettreEnPause(); stopSimLoop(); });
        btnPlay.setOnAction(e -> { if (controller != null) { controller.demarrerSimulation(); startSimLoop(); } });
        btnStop.setOnAction(e -> { if (controller != null) controller.resetSimulation(); stopSimLoop(); });
        btnStep.setOnAction(e -> { if (controller != null) { controller.reprendreSimulation(); controller.executerPas(); controller.mettreEnPause(); refreshUI(); } });
        row.getChildren().addAll(btnPause, btnPlay, btnStep, btnStop);
        return row;
    }

    private HBox buildSpeedRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        sliderVitesse = new Slider(100, 1800, 500);
        sliderVitesse.setPrefWidth(90);
        sliderVitesse.setStyle("-fx-control-inner-background:#1e293b;-fx-accent:" + ACCENT_BLUE + ";");
        lblVitesseVal = styledLabel("500 ms", FontWeight.BOLD, 10, TEXT_PRIMARY);
        sliderVitesse.valueProperty().addListener((o, ov, nv) -> {
            int v = (int) Math.round(nv.doubleValue());
            lblVitesseVal.setText(v + " ms");
            if (controller != null) controller.setVitesseSimulation(v);
            if (simTimeline != null) startSimLoop();
        });
        row.getChildren().addAll(styledLabel("Vitesse", FontWeight.NORMAL, 10, TEXT_MUTED), sliderVitesse, lblVitesseVal);
        return row;
    }

    private HBox buildBottomBar() {
        HBox bar = new HBox(0);
        bar.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER_COLOR + " transparent transparent transparent;-fx-border-width:1 0 0 0;");
        bar.setPrefHeight(96);

        VBox b1 = statBloc("INFORMATIONS GÉNÉRALES");
        b1.getChildren().addAll(
            statRow("○", "Citoyens à évacuer", lblPopRisque = val("0", ACCENT_ORANGE)),
            statRow("□", "Citoyens au refuge", lblPersonnesSec = val("0", ACCENT_GREEN)),
            statRow("▣", "Agents affichés", lblAgentsActifs = val("0", ACCENT_BLUE)));

        VBox b2 = statBloc("STATUT DU RÉSEAU");
        b2.getChildren().addAll(
                barRow("Sûres", ACCENT_GREEN, lblAretesSures = val("0%", TEXT_PRIMARY)),
                barRow("À risque", ACCENT_ORANGE, lblAretesRisque = val("0%", TEXT_PRIMARY)),
                barRow("Congestion", "#eab308", lblAretesCong = val("0%", TEXT_PRIMARY)),
                barRow("Surcharge", "#b91c1c", lblAretesOver = val("0%", TEXT_PRIMARY)),
                barRow("Inondées", ACCENT_RED, lblAretesInond = val("0%", TEXT_PRIMARY)));

        VBox b3 = statBloc("POINTS DE REFUGE");
        b3.getChildren().addAll(
            statRow("⌂", "Total", lblRefugesTotal = val("0", TEXT_PRIMARY)),
            statRow("✓", "Accessibles", lblRefugesAccess = val("0", ACCENT_GREEN)),
            statRow("×", "Inaccessibles", lblRefugesInacc = val("0", ACCENT_RED)));

        VBox b4 = statBloc("NIVEAU D'EAU MOYEN");
        lblNiveauActuel = styledLabel("0.00 m", FontWeight.BOLD, 18, ACCENT_BLUE);
        lblNiveauMax = styledLabel("Max prédit 0.00 m", FontWeight.NORMAL, 10, ACCENT_RED);
        b4.getChildren().addAll(styledLabel("Actuel", FontWeight.NORMAL, 10, TEXT_MUTED), lblNiveauActuel, lblNiveauMax);

        VBox b5 = statBloc("PROCHAINE ÉTAPE");
        lblTempsRestant = styledLabel("+ 2 min", FontWeight.BOLD, 13, ACCENT_TEAL);
        lblZoneNiveaux = styledLabel("—", FontWeight.NORMAL, 10, TEXT_PRIMARY);
        lblZoneNiveaux.setStyle("-fx-font-family:monospace;-fx-font-size:10px;");
        b5.getChildren().addAll(lblTempsRestant, styledLabel("Niveaux estimés par zone", FontWeight.NORMAL, 10, TEXT_MUTED), lblZoneNiveaux);

        bar.getChildren().addAll(wrap(b1), div(), wrap(b2), div(), wrap(b3), div(), wrap(b4), div(), wrap(b5));
        return bar;
    }

    private void startRefreshLoop() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> refreshUI()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void startSimLoop() {
        if (simTimeline != null) simTimeline.stop();
        double intervalMs = controller != null ? controller.getVitesseSimulationMs() : 500;
        simTimeline = new Timeline(new KeyFrame(Duration.millis(intervalMs), e -> { if (controller != null) controller.executerPas(); }));
        simTimeline.setCycleCount(Timeline.INDEFINITE);
        simTimeline.play();
    }

    private void stopSimLoop() { if (simTimeline != null) simTimeline.stop(); }

    private void refreshUI() {
        if (modele == null) return;
        try {
            int s = (int) modele.getTempsEcoule();
            lblTimer.setText(String.format("⏱  %02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60));
            boolean pause = modele.isEnPause();
            lblSimStatus.setText(pause ? "En pause" : "Simulation en cours");
            lblSimStatus.setTextFill(Color.web(pause ? ACCENT_ORANGE : ACCENT_GREEN));

            double nv = modele.getNiveauEau();
            lblNiveauActuel.setText(String.format("%.2f m", nv));
            lblNiveauMax.setText(String.format("Max prédit %.2f m", nv * 1.78));

            if (controller != null) {
                double[] r = controller.getStatutReseau();
                if (r != null && r.length >= 5) {
                    lblAretesSures.setText(String.format("%.0f%%", r[0]));
                    lblAretesRisque.setText(String.format("%.0f%%", r[1]));
                    lblAretesCong.setText(String.format("%.0f%%", r[2]));
                    lblAretesOver.setText(String.format("%.0f%%", r[3]));
                    lblAretesInond.setText(String.format("%.0f%%", r[4]));
                }
                lblPopRisque.setText(String.valueOf(controller.getPopulationARisque()));
                lblPersonnesSec.setText(String.valueOf(controller.getPopulationEnSecurite()));
            }
            lblAgentsActifs.setText(String.valueOf(modele.getNombreAgents()));
            if (mapController != null) {
                lblRefugesTotal.setText(String.valueOf(mapController.countSheltersTotal()));
                lblRefugesAccess.setText(String.valueOf(mapController.countSheltersAccessible()));
                lblRefugesInacc.setText(String.valueOf(Math.max(0, mapController.countSheltersTotal() - mapController.countSheltersAccessible())));
            }

            List<Zone> zones = modele.getZones();
            if (!zones.isEmpty() && selectedZoneIndex < zones.size()) {
                Zone z = zones.get(selectedZoneIndex);
                lblNiveauEauZone.setText(String.format("%.2f m", nv));
                boolean fl = z.isFlooded();
                lblStatutZone.setText(fl ? "Inondée ⚠" : nv > 0.5 ? "En montée ↗" : "Stable →");
                lblStatutZone.setTextFill(Color.web(fl ? ACCENT_RED : nv > 0.5 ? ACCENT_ORANGE : ACCENT_GREEN));
            }
            if (zones.size() >= 6) lblZoneNiveaux.setText(String.format("A %.2fm   B %.2fm%nC %.2fm   F %.2fm", nv * .8, nv, nv * .9, nv * .65));
        } catch (Exception e) {
            System.err.println("refreshUI: " + e.getMessage());
        }
    }

    public void stopRefresh() {
        if (refreshTimeline != null) refreshTimeline.stop();
        if (simTimeline != null) simTimeline.stop();
    }

    private Label styledLabel(String text, FontWeight fw, int size, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("System", fw, size));
        l.setTextFill(Color.web(color));
        return l;
    }
    private Label val(String text, String color) { return styledLabel(text, FontWeight.BOLD, 12, color); }

    private VBox cardPanel(int maxW) {
        VBox p = new VBox(6);
        p.setPadding(new Insets(12, 14, 12, 14));
        p.setMaxWidth(maxW);
        // Très important : dans un StackPane, un enfant resizable peut prendre toute la hauteur.
        // On force donc les panneaux overlay à garder leur hauteur naturelle pour ne pas cacher la carte.
        p.setMinHeight(Region.USE_PREF_SIZE);
        p.setPrefHeight(Region.USE_COMPUTED_SIZE);
        p.setMaxHeight(Region.USE_PREF_SIZE);
        p.setPickOnBounds(false);
        p.setStyle("-fx-background-color:" + BG_CARD + ";-fx-background-radius:8;-fx-border-color:" + BORDER_COLOR + ";-fx-border-radius:8;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),12,0,0,4);");
        return p;
    }

    private Rectangle iconLine(String color) {
        Rectangle r = new Rectangle(22, 3);
        r.setArcWidth(3);
        r.setArcHeight(3);
        r.setFill(Color.web(color));
        return r;
    }

    private VBox statBloc(String title) {
        VBox b = new VBox(8);
        b.setPadding(new Insets(12, 18, 10, 18));
    
        Label titleLabel = styledLabel(title, FontWeight.BOLD, 9, TEXT_MUTED);
    
        Rectangle underline = new Rectangle(150, 2);
        underline.setFill(Color.web(ACCENT_BLUE));
        underline.setArcWidth(3);
        underline.setArcHeight(3);
    
        b.getChildren().addAll(titleLabel, underline);
        return b;
    }

    private HBox hrow(String label, Label value) {
        HBox r = new HBox(6);
        r.setAlignment(Pos.CENTER_LEFT);
        r.getChildren().addAll(styledLabel(label, FontWeight.NORMAL, 10, TEXT_MUTED), value);
        return r;
    }

    private HBox statRow(String icon, String label, Label value) {
        HBox r = new HBox(8);
        r.setAlignment(Pos.CENTER_LEFT);
    
        Label ic = new Label(icon);
        ic.setTextFill(Color.web(TEXT_MUTED));
        ic.setFont(Font.font("System", FontWeight.BOLD, 15));
        ic.setMinWidth(22);
    
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
    
        r.getChildren().addAll(ic, styledLabel(label, FontWeight.NORMAL, 11, TEXT_MUTED), sp, value);
        return r;
    }

    private HBox barRow(String label, String color, Label value) {
        HBox r = new HBox(8);
        r.setAlignment(Pos.CENTER_LEFT);
        Rectangle dot = new Rectangle(12, 3);
        dot.setFill(Color.web(color));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        r.getChildren().addAll(dot, styledLabel(label, FontWeight.NORMAL, 11, TEXT_MUTED), sp, value);
        return r;
    }

    private HBox colorBar(String color, String text) {
        HBox r = new HBox(6);
        r.setAlignment(Pos.CENTER_LEFT);
        Rectangle rect = new Rectangle(16, 8);
        rect.setFill(Color.web(color));
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        r.getChildren().addAll(rect, styledLabel(text, FontWeight.NORMAL, 10, TEXT_MUTED));
        return r;
    }

    private Separator separator() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:" + BORDER_COLOR + ";");
        return s;
    }

    private Button smallButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_PRIMARY + ";-fx-background-radius:6;-fx-font-size:10px;-fx-padding:5 8 5 8;-fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:" + ACCENT_BLUE + ";-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:10px;-fx-padding:5 8 5 8;-fx-cursor:hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_PRIMARY + ";-fx-background-radius:6;-fx-font-size:10px;-fx-padding:5 8 5 8;-fx-cursor:hand;"));
        return b;
    }

    private Button modeButton(String text, boolean active) {
        Button b = new Button(text);
        b.setStyle(active ? "-fx-background-color:#1e40af;-fx-text-fill:#93c5fd;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;"
                : "-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_MUTED + ";-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;");
        return b;
    }

    private void setModeActive(Button active, Button... inactives) {
        active.setStyle("-fx-background-color:#1e40af;-fx-text-fill:#93c5fd;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;");
    
        for (Button inactive : inactives) {
            inactive.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_MUTED + ";-fx-background-radius:6;-fx-font-size:11px;-fx-padding:5 12 5 12;-fx-cursor:hand;");
        }
    }

    private Button iconBtn(String icon) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_PRIMARY + ";-fx-background-radius:6;-fx-font-size:13px;-fx-padding:5 10 5 10;-fx-cursor:hand;");
        return b;
    }

    private String zoneBtnStyle(boolean selected) {
        return selected ? "-fx-background-color:" + ACCENT_BLUE + ";-fx-text-fill:white;-fx-background-radius:4;-fx-font-size:11px;-fx-font-weight:bold;-fx-min-width:36;-fx-min-height:24;-fx-cursor:hand;"
                : "-fx-background-color:#1e293b;-fx-text-fill:" + TEXT_MUTED + ";-fx-background-radius:4;-fx-font-size:11px;-fx-min-width:36;-fx-min-height:24;-fx-cursor:hand;";
    }

    private HBox wrap(VBox b) { HBox.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); HBox w = new HBox(b); HBox.setHgrow(w, Priority.ALWAYS); return w; }
    private Rectangle div() { Rectangle r = new Rectangle(1, 75); r.setFill(Color.web(BORDER_COLOR)); return r; }
}