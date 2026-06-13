package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import controller.MapController;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import model.agent.Agent;
import model.agent.Citizen;
import model.enums.CitizenState;
import model.graph.Edge;
import model.graph.Node;
import model.graph.RouteGraph;
import model.observer.Observer;
import model.zone.Shelter;
import model.zone.Zone;
import model.zone.ZoneUpdateListener;


/**
 * Carte OSM + graphe interactif + personnages Mii.
 * Les zones sont les nœuds principaux du graphe, les arêtes viennent de RouteGraph.
 * Cette vue ajoute aussi des nœuds/arêtes visuels pour répondre aux actions demandées
 * en direct pendant la simulation, sans casser le RouteGraph existant du projet.
 */
public class MapView implements ZoneUpdateListener, Observer<Zone> {

    public enum EditMode { SELECT, ADD_NODE, ADD_EDGE, MOVE_NODE, DELETE }

    private final JXMapViewer mapViewer;
    private final SwingNode swingNode;
    private List<Zone> zones;
    private List<Agent> agents = new ArrayList<>();

    private ZonePainter zonePainter;
    private RoutePainter routePainter;
    private GraphOverlayPainter graphOverlayPainter;
    private RouteHighlightPainter routeHighlightPainter;
    private AgentPainter agentPainter;
    private CompoundPainter<JXMapViewer> compound;

    private MapController mapController;
    private RouteGraph routeGraph;
    private Consumer<Zone> onZoneSelected;
    private Consumer<Agent> onAgentSelected;
    private Consumer<String> onGraphInfoChanged;

    private Agent draggedAgent;
    private EditMode editMode = EditMode.SELECT;
    private GraphNode draggedNode;
    private GraphNode pendingEdgeStart;
    private Object selectedGraphElement;
    private boolean manualFloodMode = false;
    private GeoPosition floodCenter = null;
    private double floodRadius = 0;
    private javax.swing.Timer floodTimer;
    private int floodTimerDelayMs = 300;
    private double floodRadiusStep = 2.0;

    private java.util.function.Consumer<model.graph.Edge> onEdgeSelected;
    private java.util.function.BiConsumer<Double, Double> onMapClicked;
    private boolean lastClickConsumedByEdge = false;
    private model.graph.Edge selectedEdge;
    private boolean densityOverlayEnabled = false;
    private Runnable onNodeMoveFinished;

    private static final GeoPosition LYON_CENTER = new GeoPosition(45.7640, 4.8357);
    private static final int DEFAULT_ZOOM = 6;

    /**
     * Constructs a new MapView.
     * @param zones the zones.
     */
    public MapView(List<Zone> zones) {
        this.zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
        this.mapViewer = new JXMapViewer();
        this.swingNode = new SwingNode();

        configureTiles();
        configurePainters();
        configureInteraction();
        loadZones();

        swingNode.setContent(mapViewer);
        Platform.runLater(() -> {
            mapViewer.repaint();
            mapViewer.revalidate();
        });
    }

    /**
     * Performs tiles.
     */
    private void configureTiles() {
        System.setProperty("http.agent", "Mozilla/5.0");
        TileFactoryInfo info = new OSMTileFactoryInfo("OSM", "https://tile.openstreetmap.org");
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(8);
        mapViewer.setTileFactory(tileFactory);
        mapViewer.setPreferredSize(new Dimension(800, 600));
        mapViewer.setMinimumSize(new Dimension(400, 300));
        mapViewer.setZoom(DEFAULT_ZOOM);
        mapViewer.setAddressLocation(LYON_CENTER);
    }

    /**
     * Performs painters.
     */
    private void configurePainters() {
        zonePainter = new ZonePainter(zones);
        routePainter = new RoutePainter();
        routePainter.setMapViewer(mapViewer);
        graphOverlayPainter = new GraphOverlayPainter();
        routeHighlightPainter = new RouteHighlightPainter();
        agentPainter = new AgentPainter();
        agentPainter.setAnimateStaticAgents(false); // les agents ne bougent pas au hasard : ils attendent un chemin d'évacuation
        agentPainter.setAgents(agents, zones);

        compound = new CompoundPainter<>();
        compound.addPainter(zonePainter);
        compound.addPainter(routePainter);
        compound.addPainter(graphOverlayPainter);
        compound.addPainter(routeHighlightPainter);
        compound.addPainter(agentPainter);
        mapViewer.setOverlayPainter(compound);
    }

    /**
     * Performs interaction.
     */
    private void configureInteraction() {
        PanMouseInputListener panListener = new PanMouseInputListener(mapViewer) {
            @Override public void mouseDragged(MouseEvent e) {
                // bloquer le pan si on est en train de dragger un nœud ou agent
                if (draggedNode != null || draggedAgent != null) return;
                super.mouseDragged(e);
            }
        };
        mapViewer.addMouseListener(panListener);
        mapViewer.addMouseMotionListener(panListener);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (manualFloodMode) {
                    floodCenter = mapViewer.convertPointToGeoPosition(e.getPoint());
                    floodRadius = 0;
                    startFloodPropagation();
                    setInfo("Inondation déclenchée manuellement.");
                    mapViewer.repaint();
                    return;
                }
                if (editMode == EditMode.ADD_NODE) {
                    GeoPosition gp = mapViewer.convertPointToGeoPosition(e.getPoint());
                    addVisualNode(gp.getLatitude(), gp.getLongitude());
                    setInfo("Nœud ajouté en direct.");
                    mapViewer.repaint();
                    return;
                }

                Agent clickedAgent = agentPainter.findAgentAt(mapViewer, e.getPoint());
                if (clickedAgent != null) {
                    selectedGraphElement = clickedAgent;
                    if (onAgentSelected != null) onAgentSelected.accept(clickedAgent);
                    setInfo(agentInfo(clickedAgent));
                    mapViewer.repaint();
                    return;
                }

                Object hit = graphOverlayPainter.hitTest(mapViewer, e.getPoint());
                if (editMode == EditMode.DELETE && hit != null) {
                    deleteGraphElement(hit);
                    mapViewer.repaint();
                    return;
                }
    

                if (hit instanceof Edge edge) {
                    selectedEdge = edge;
                    lastClickConsumedByEdge = true; 
                    if (onEdgeSelected != null) onEdgeSelected.accept(edge);
                    setInfo(graphOverlayPainter.infoFor(edge));
                    mapViewer.repaint();
                    return;
                }

                if (hit instanceof GraphNode node) {
                    lastClickConsumedByEdge = false;
                    selectedGraphElement = node;
                    graphOverlayPainter.setSelected(node);
                    setInfo(graphOverlayPainter.infoFor(node));
                
                    if (node.zone != null && onZoneSelected != null) {
                        onZoneSelected.accept(node.zone);
                    }
                
                    mapViewer.repaint();
                    return;
                }
                
                if (hit != null) {
                    lastClickConsumedByEdge = false;
                    selectedGraphElement = hit;
                    graphOverlayPainter.setSelected(hit);
                    setInfo(graphOverlayPainter.infoFor(hit));
                    mapViewer.repaint();
                    return;
                }

                handleMapClick(e.getPoint());
            }

            @Override public void mousePressed(MouseEvent e) {
                Agent agent = agentPainter.findAgentAt(mapViewer, e.getPoint());
                if (agent != null) {
                    selectedGraphElement = agent;
                    draggedAgent = agent;
                    agentPainter.startDrag(agent);
                    if (onAgentSelected != null) onAgentSelected.accept(agent);
                    setInfo("Agent sélectionné : " + nameOf(agent));
                    mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    mapViewer.repaint();
                    e.consume();
                    return;
                }

                if (editMode == EditMode.MOVE_NODE) {
                    // chercher aussi dans baseNodes (zones réelles)
                    Object hit = graphOverlayPainter.hitTest(mapViewer, e.getPoint());
                    if (hit instanceof GraphNode node) {
                        draggedNode = node;
                        selectedGraphElement = node;
                        graphOverlayPainter.setSelected(node);
                        mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        e.consume();
                    }
                }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (draggedAgent != null) {
                    GeoPosition pos = mapViewer.convertPointToGeoPosition(e.getPoint());
                    agentPainter.dragTo(draggedAgent, pos);
                    mapViewer.repaint();
                    e.consume();
                    return;
                }
                if (draggedNode != null) {
                    GeoPosition gp = mapViewer.convertPointToGeoPosition(e.getPoint());
                    draggedNode.lat = gp.getLatitude();
                    draggedNode.lng = gp.getLongitude();

                    // NOUVEAU — synchroniser la Zone réelle du modèle
                    if (draggedNode.zone != null) {
                        draggedNode.zone.setLatitude(gp.getLatitude());
                        draggedNode.zone.setLongitude(gp.getLongitude());
                    }

                    setInfo("Nœud déplacé : " + draggedNode.name);
                    mapViewer.repaint();
                    e.consume();
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (draggedAgent != null) {
                    agentPainter.endDrag();
                    setInfo("Agent relâché et replacé sur le graphe : " + nameOf(draggedAgent));
                    draggedAgent = null;
                    if (onNodeMoveFinished != null) {
                        Platform.runLater(onNodeMoveFinished);
                    }
                    mapViewer.setCursor(Cursor.getDefaultCursor());
                    mapViewer.repaint();
                    e.consume();
                }
                if (draggedNode != null) {
                    draggedNode = null;
                    if (onNodeMoveFinished != null) {
                        Platform.runLater(onNodeMoveFinished);
                    }
                    mapViewer.setCursor(Cursor.getDefaultCursor());
                    mapViewer.repaint();
                    e.consume();
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                Agent over = agentPainter.findAgentAt(mapViewer, e.getPoint());
                agentPainter.setHoveredAgent(over);
                Object hit = graphOverlayPainter.hitTest(mapViewer, e.getPoint());
                mapViewer.setCursor(over != null || hit != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                mapViewer.repaint();
            }
        };
        mapViewer.addMouseListener(adapter);
        mapViewer.addMouseMotionListener(adapter);
    }

    /**
     * Loads zones.
     */
    private void loadZones() {
        zonePainter.setZones(zones);
        agentPainter.updateZones(zones);
        graphOverlayPainter.rebuildBaseNodes(zones);
        mapViewer.repaint();
    }

    /**
     * Return the currently selected agent in the map view, or null if none.
     * Used by other views/controllers to delegate actions on the selected agent.
     *
     * @return the selected Agent or null
     */
    public Agent getSelectedAgent() {
        if (selectedGraphElement instanceof Agent a) return a;
        return null;
    }

    /**
     * Get the underlying Swing `JXMapViewer` instance used to render the map.
     *
     * @return the JXMapViewer
     */
    public JXMapViewer getMapViewer() { return mapViewer; }

    /**
     * Get the JavaFX `SwingNode` that hosts the map swing component.
     *
     * @return the SwingNode hosting the map
     */
    public SwingNode getSwingNode() { return swingNode; }

    /**
     * Placeholder for web view integration; currently unused.
     *
     * @return null (no web view in this implementation)
     */
    public Object getWebView() { return null; }

    /**
     * Placeholder for web engine integration; currently unused.
     *
     * @return null (no web engine in this implementation)
     */
    public Object getWebEngine() { return null; }

    /**
     * Associate a `MapController` with this view to delegate actions and callbacks.
     *
     * @param controller the MapController to set
     */
    public void setMapController(MapController controller) { this.mapController = controller; }

    /**
     * Attach a `RouteGraph` to the map view so painters can visualize routes and edges.
     * This will also refresh route and agent painters.
     *
     * @param routeGraph the RouteGraph to visualize
     */
    public void setRouteGraph(RouteGraph routeGraph) {
        this.routeGraph = routeGraph;
        routePainter.setRouteGraph(routeGraph);
        graphOverlayPainter.setRouteGraph(routeGraph);
        agentPainter.setRouteGraph(routeGraph);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }
    
    /**
     * Trigger a UI refresh of the map from the Swing thread.
     */
    public void refreshMap() {
        SwingUtilities.invokeLater(() -> {
            mapViewer.revalidate();
            mapViewer.repaint();
        });
    }
    
    /**
     * Mark whether the last click was consumed by an edge hit-test.
     *
     * @param v true if last click targeted an edge
     */
    public void setLastClickConsumedByEdge(boolean v) { this.lastClickConsumedByEdge = v; }

    /**
     * Return true if the last click on the map was consumed by an edge.
     *
     * @return boolean indicating if last click was consumed by an edge
     */
    public boolean wasLastClickConsumedByEdge() { return lastClickConsumedByEdge; }

    /**
     * Set a callback to be executed after a node or agent drag operation finishes.
     *
     * @param callback runnable to execute on move completion
     */
    public void setOnNodeMoveFinished(Runnable callback) { this.onNodeMoveFinished = callback; }

    /**
     * Replace the current agent list displayed on the map. The view will snap agents to
     * the visible graph and repaint on the Swing thread.
     *
     * @param agents list of agents to display
     */
    public void setAgents(List<Agent> agents) {
        this.agents = agents == null ? new ArrayList<>() : new ArrayList<>(agents);
    
        if (selectedGraphElement instanceof Agent selected) {
            boolean stillExists = this.agents.stream()
                    .anyMatch(a -> a.getId() == selected.getId());
    
            if (!stillExists) {
                selectedGraphElement = null;
            }
        }
    
        agentPainter.setAgents(this.agents, zones);
        agentPainter.snapAgentsToGraph();
    
        SwingUtilities.invokeLater(() -> {
            mapViewer.revalidate();
            mapViewer.repaint();
        });
    }

    /**
     * Set a callback invoked when an agent is selected on the map.
     *
     * @param callback consumer receiving the selected Agent
     */
    public void setOnAgentSelected(Consumer<Agent> callback) { this.onAgentSelected = callback; }

    /**
     * Set a callback invoked when a zone node is selected on the map.
     *
     * @param callback consumer receiving the selected Zone
     */
    public void setOnZoneSelected(Consumer<Zone> callback) { this.onZoneSelected = callback; }

    /**
     * Set a callback to receive short info/status text from the map (used by status bars).
     *
     * @param callback consumer receiving status text
     */
    public void setOnGraphInfoChanged(Consumer<String> callback) { this.onGraphInfoChanged = callback; }

    /**
     * Change the edit interaction mode of the map (select, add node/edge, move, delete).
     *
     * @param mode the EditMode to switch to
     */
    public void setEditMode(EditMode mode) {
        this.editMode = mode == null ? EditMode.SELECT : mode;
        this.pendingEdgeStart = null;
        setInfo("Mode : " + this.editMode.name());
    }

    /**
     * Enable or disable manual flood mode. When enabled, clicks create a flood center.
     *
     * @param enabled true to enable manual flood mode
     */
    public void setManualFloodMode(boolean enabled) {
        this.manualFloodMode = enabled;
        setInfo(enabled ? "Mode inondation manuelle : clique sur la carte." : "Mode inondation désactivé.");
    }

    /**
     * Reset and stop any manual flood propagation currently active.
     */
    public void resetManualFlood() {
        if (floodTimer != null) {
            floodTimer.stop();
        }
    
        floodCenter = null;
        floodRadius = 0;
        manualFloodMode = false;
    
        if (routeGraph != null) {
            for (Edge edge : routeGraph.getEdges()) {
                edge.setFloodLevel(0.0);
            }
        }
    
        setInfo("Inondation réinitialisée.");
        mapViewer.repaint();
    }

    /**
     * Start a randomized flood propagation for demo/testing purposes.
     */
    public void startRandomFlood() {
        if (floodTimer != null) {
            floodTimer.stop();
        }
    
        Random r = new Random();
    
        double lat = LYON_CENTER.getLatitude() + (r.nextDouble() - 0.5) * 0.05;
        double lng = LYON_CENTER.getLongitude() + (r.nextDouble() - 0.5) * 0.07;
    
        floodCenter = new GeoPosition(lat, lng);
        floodRadius = 0;
        manualFloodMode = false;
    
        startFloodPropagation();
        setInfo("Inondation aléatoire déclenchée.");
        mapViewer.repaint();
    }

    /**
     * Return the current edit mode for the map view.
     *
     * @return the current EditMode
     */
    public EditMode getEditMode() { return editMode; }

    /**
     * Adjust internal flood propagation speed parameters from a UI slider value.
     *
     * @param sliderValue slider raw value (expected range ~200..5000 ms)
     */
    public void setFloodSpeedFromSlider(double sliderValue) {
        // Slider range [200..5000]ms, where low = fast and high = slow.
        // We keep the flood clearly visible but much slower by default.
        double clamped = Math.max(200.0, Math.min(5000.0, sliderValue));
        double ratioFast = (5000.0 - clamped) / 4800.0; // 0=slow, 1=fast

        // Timer cadence: slow 450ms -> fast 150ms
        floodTimerDelayMs = (int) Math.round(450.0 - (300.0 * ratioFast));

        // Radius growth per tick: slow 0.35px -> fast 1.60px
        floodRadiusStep = 0.35 + (1.25 * ratioFast);
    
        if (floodTimer != null && floodTimer.isRunning()) {
            startFloodPropagation();
        }
    }

    /**
     * Pause any ongoing flood propagation animation.
     */
    public void pauseFloodPropagation() {
        if (floodTimer != null) {
            floodTimer.stop();
        }
    }

    /**
     * Resume flood propagation if there is an active flood center and radius remaining.
     */
    public void resumeFloodPropagation() {
        if (floodCenter == null) return;
        if (floodRadius >= 260) return;
        startFloodPropagation();
    }

    /**
     * Add a number of visual nodes randomly around the map center for testing.
     *
     * @param count number of nodes to add
     */
    public void addRandomNodes(int count) {
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            double lat = LYON_CENTER.getLatitude() + (r.nextDouble() - 0.5) * 0.055;
            double lng = LYON_CENTER.getLongitude() + (r.nextDouble() - 0.5) * 0.075;
            addVisualNode(lat, lng);
        }
        autoConnectVisualNodes();
        setInfo(count + " nœuds ajoutés en masse + arêtes aléatoires.");
        mapViewer.repaint();
    }

    /**
     * Add an agent to the map view and refresh painters.
     *
     * @param agent Agent to add (ignored if null)
     */
    public void addAgent(Agent agent) {
        if (agent == null) return;
        if (!agents.contains(agent)) agents.add(agent);
        agentPainter.setAgents(agents, zones);
        setInfo("Agent ajouté : " + nameOf(agent));
        mapViewer.repaint();
    }

    /**
     * Remove the currently selected agent from the map and update the view.
     */
    public void removeSelectedAgent() {
        if (selectedGraphElement instanceof Agent a) {
            agents.removeIf(agent -> agent.getId() == a.getId());
    
            selectedGraphElement = null;
    
            agentPainter.setAgents(agents, zones);
            agentPainter.snapAgentsToGraph();
    
            setInfo("Agent supprimé.");
    
            mapViewer.revalidate();
            mapViewer.repaint();
        }
    }

    /**
     * Create and add a number of synthetic agents for demo/testing purposes.
     *
     * @param count number of agents to add
     */
    public void addRandomAgents(int count) {
        int maxId = agents.stream().mapToInt(Agent::getId).max().orElse(100) + 1;
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            Citizen c = new Citizen(maxId + i, "Citoyen", "Auto" + (maxId + i), new Node(LYON_CENTER.getLatitude(), LYON_CENTER.getLongitude()));
            c.setState(i % 5 == 0 ? CitizenState.INJURED : i % 4 == 0 ? CitizenState.PMR : CitizenState.CALM);
            c.setMaxSpeed(1.5 + rng.nextDouble() * 3.5);
            agents.add(c);
        }
        agentPainter.setAgents(agents, zones);
        agentPainter.snapAgentsToGraph();
        setInfo(count + " agents ajoutés en masse avec propriétés aléatoires.");
        mapViewer.repaint();
    }

    /**
     * Register a callback invoked when the map is clicked (lat, lng).
     *
     * @param callback bi-consumer receiving latitude and longitude
     */
    public void setOnMapClicked(java.util.function.BiConsumer<Double, Double> callback) {
        this.onMapClicked = callback;
    }
    /**
     * Delete the currently selected graph element (agent, node or edge).
     */
    public void deleteSelectedGraphElement() { deleteGraphElement(selectedGraphElement); }

    /**
     * Deletes graph element.
     * @param element the element.
     */
    private void deleteGraphElement(Object element) {
        if (element == null) return;
        if (element instanceof Agent) { removeSelectedAgent(); return; }
        if (element instanceof GraphEdge edge) {
            graphOverlayPainter.visualEdges.remove(edge);
            selectedGraphElement = null;
            setInfo("Arête supprimée. Les agents dessus sont replacés au nœud d'origine.");
            for (Agent a : agents) agentPainter.snapAgentToNearestGraphElement(a);
            return;
        }
        if (element instanceof GraphNode node) {
            List<GraphNode> adjacent = graphOverlayPainter.adjacent(node);
            graphOverlayPainter.visualEdges.removeIf(e -> e.from == node || e.to == node);
            graphOverlayPainter.visualNodes.remove(node);
            for (Agent a : agents) {
                GeoPosition p = new GeoPosition(a.getPosition().getLat(), a.getPosition().getLng());
                Point2D ap = mapViewer.convertGeoPositionToPoint(p);
                Point2D np = mapViewer.convertGeoPositionToPoint(node.geo());
                if (ap.distance(np) < 40 && !adjacent.isEmpty()) {
                    GraphNode dest = adjacent.get(0);
                    a.setPosition(new Node(dest.lat, dest.lng));
                }
            }
            selectedGraphElement = null;
            setInfo("Nœud supprimé. Arêtes connectées supprimées, agents replacés sur un nœud adjacent.");
        }
    }

    /**
     * Update the visual color for a zone and refresh related painters on the UI thread.
     *
     * @param zone the Zone to update
     */
    public void updateZoneColor(Zone zone) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZone(zone);
            agentPainter.updateZones(zones);
            mapViewer.repaint();
        });
    }

    /**
     * Update a zone with a water-level indicator (flooding) and refresh the view.
     *
     * @param zone the Zone to update
     * @param niveauEau water level value
     */
    public void updateZoneWithWaterLevel(Zone zone, double niveauEau) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZoneWaterLevel(zone, niveauEau);
            agentPainter.updateZones(zones);
            mapViewer.repaint();
        });
    }

    /**
     * Request a repaint so that route colors are recalculated and redrawn.
     */
    public void refreshRouteColors() { SwingUtilities.invokeLater(mapViewer::repaint); }

    /**
     * Replace the current zone list and refresh all painters.
     *
     * @param updatedZones new list of zones
     */
    public void updateAllZones(List<Zone> updatedZones) {
        this.zones = updatedZones == null ? new ArrayList<>() : new ArrayList<>(updatedZones);
        SwingUtilities.invokeLater(() -> {
            zonePainter.setZones(this.zones);
            agentPainter.updateZones(this.zones);
            graphOverlayPainter.rebuildBaseNodes(this.zones);
            mapViewer.repaint();
        });
    }

    /** Zoom the map view one step in. */
    public void zoomIn() { SwingUtilities.invokeLater(() -> { int z = mapViewer.getZoom(); if (z > 1) mapViewer.setZoom(z - 1); }); }
    /** Zoom the map view one step out. */
    public void zoomOut() { SwingUtilities.invokeLater(() -> { int z = mapViewer.getZoom(); if (z < 17) mapViewer.setZoom(z + 1); }); }
    /** Set an explicit zoom level. @param level zoom level */
    public void setZoom(int level) { SwingUtilities.invokeLater(() -> mapViewer.setZoom(level)); }
    /** Reset the map view to the default center and zoom. */
    public void resetView() { SwingUtilities.invokeLater(() -> { mapViewer.setAddressLocation(LYON_CENTER); mapViewer.setZoom(DEFAULT_ZOOM); clearRoute(); }); }
    /** Move and zoom the map to a given coordinate. */
    public void flyTo(double lat, double lng) { SwingUtilities.invokeLater(() -> { mapViewer.setAddressLocation(new GeoPosition(lat, lng)); mapViewer.setZoom(2); }); }
    /** Pan the map to a given coordinate. */
    public void panTo(double lat, double lng) { SwingUtilities.invokeLater(() -> mapViewer.setAddressLocation(new GeoPosition(lat, lng))); }

    /**
     * Performs on agent.
     * @param agent the agent.
     */
    public void centerOnAgent(Agent agent) {
        if (agent == null || agent.getPosition() == null) return;
    
        double lat = agent.getPosition().getLat();
        double lng = agent.getPosition().getLng();
    
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(new GeoPosition(lat, lng));
            mapViewer.setZoom(3);
            mapViewer.repaint();
        });
    }
    


    /**
     * Focus the map on a given zone and select it.
     *
     * @param zone Zone to focus
     */
    public void focusZone(Zone zone) {
        if (zone == null) return;
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(new GeoPosition(zone.getLatitude(), zone.getLongitude()));
            mapViewer.setZoom(3);
            selectZone(zone);
        });
    }

    /**
     * Select a zone on the map and show its popup/info.
     *
     * @param zone Zone to select
     */
    public void selectZone(Zone zone) {
        if (zone == null) return;
        zonePainter.setSelectedZone(zone);
        selectedGraphElement = graphOverlayPainter.nodeFor(zone);
        graphOverlayPainter.setSelected(selectedGraphElement);
        mapViewer.repaint();
        if (onZoneSelected != null) onZoneSelected.accept(zone);
        setInfo("Nœud sélectionné : " + zone.getName());
    }

    /**
     * Highlight a zone by id (selects it if found).
     *
     * @param id zone id
     */
    public void highlightZone(int id) { zones.stream().filter(z -> z.getId() == id).findFirst().ifPresent(this::selectZone); }

    /**
     * Display a highlighted route between two zones on the map.
     *
     * @param from origin zone
     * @param to destination zone
     */
    public void showRoute(Zone from, Zone to) {
        if (from == null || to == null) return;
        SwingUtilities.invokeLater(() -> {
            routeHighlightPainter.setRoute(from, to);
            mapViewer.setAddressLocation(new GeoPosition((from.getLatitude() + to.getLatitude()) / 2.0, (from.getLongitude() + to.getLongitude()) / 2.0));
            mapViewer.setZoom(4);
            zonePainter.setSelectedZone(to);
            mapViewer.repaint();
        });
    }

    /**
     * Display a specific evacuation path between two zones with an optional color.
     *
     * @param from origin zone
     * @param to destination zone
     * @param path EvacuationPath to highlight (may be null)
     * @param hexColor CSS hex color string for highlighting
     */
    public void showRoute(Zone from, Zone to, model.algorithms.EvacuationPath path, String hexColor) {
        if (from == null || to == null) return;
        SwingUtilities.invokeLater(() -> {
            if (path != null && !path.isEmpty()) {
                routePainter.setHighlightedPath(path, hexColor);
            } else {
                routePainter.setHighlightedPath(null);
            }
            mapViewer.setAddressLocation(new GeoPosition(
                (from.getLatitude() + to.getLatitude()) / 2.0,
                (from.getLongitude() + to.getLongitude()) / 2.0
            ));
            mapViewer.setZoom(4);
            zonePainter.setSelectedZone(to);
            mapViewer.repaint();
        });
    }

    
    /**
     * Clear any highlighted route on the map.
     */
    public void clearRoute() { SwingUtilities.invokeLater(() -> { routeHighlightPainter.clear(); mapViewer.repaint(); }); }

    /**
     * Starts flood propagation.
     */
    private void startFloodPropagation() {
        if (floodTimer != null) {
            floodTimer.stop();
        }
    
        floodTimer = new javax.swing.Timer(floodTimerDelayMs, e -> {
            floodRadius += floodRadiusStep;
    
            updateFloodedEdges();
    
            if (floodRadius >= 260) {
                floodTimer.stop();
            }
    
            mapViewer.repaint();
        });
    
        floodTimer.start();
    }

    /**
     * Updates flooded edges.
     */
    private void updateFloodedEdges() {
        if (routeGraph == null || floodCenter == null) return;
    
        Point2D floodPoint = mapViewer.convertGeoPositionToPoint(floodCenter);
    
        for (Edge edge : routeGraph.getEdges()) {
            boolean touched = false;
    
            List<GeoPosition> pts = edge.getWaypoints();
    
            if (pts == null || pts.size() < 2) {
                pts = List.of(
                        new GeoPosition(edge.getFromZone().getLatitude(), edge.getFromZone().getLongitude()),
                        new GeoPosition(edge.getToZone().getLatitude(), edge.getToZone().getLongitude())
                );
            }
    
            for (int i = 0; i < pts.size() - 1; i++) {
                Point2D p1 = mapViewer.convertGeoPositionToPoint(pts.get(i));
                Point2D p2 = mapViewer.convertGeoPositionToPoint(pts.get(i + 1));
    
                double distance = new Line2D.Double(p1, p2).ptSegDist(floodPoint);
    
                if (distance <= floodRadius) {
                    touched = true;
                    break;
                }
            }
    
            if (touched) {
                edge.setFloodLevel(edge.getFloodLevel() + 0.05);
            }
        }
    
        mapViewer.repaint();
    }

    /**
     * Sets the on edge selected.
     * @param listener the listener.
     */
    public void setOnEdgeSelected(java.util.function.Consumer<model.graph.Edge> listener) {
    this.onEdgeSelected = listener; // stocker et appeler lors du clic sur une arête
    }
 
    /** Retourne l'arête actuellement sélectionnée dans la vue, ou null. */
    public model.graph.Edge getSelectedEdge() {
        return this.selectedEdge; // champ à maintenir lors de la sélection
    }
    
    /**
     * Active ou désactive la superposition de densité (gradient de couleurs
     * sur les arêtes et nœuds selon leur taux d'occupation).
     * Quand activé, MapView doit recalculer les couleurs à chaque repaint
     * en interrogeant SimulationController.getEdgeDensity() / getZoneDensity().
     */
    public void setDensityOverlayEnabled(boolean enabled) {
        this.densityOverlayEnabled = enabled;
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    /**
     * Handles map click.
     * @param screenPoint the screenPoint.
     */
    private void handleMapClick(Point screenPoint) {
        
        lastClickConsumedByEdge = false;
        GeoPosition clickPos = mapViewer.convertPointToGeoPosition(screenPoint);
        if (onMapClicked != null) {
            Platform.runLater(() -> onMapClicked.accept(clickPos.getLatitude(), clickPos.getLongitude()));
        }

        if (manualFloodMode) {
            floodCenter = clickPos;
            floodRadius = 0;
            startFloodPropagation();
            setInfo("Inondation manuelle déclenchée — les citoyens commencent l'évacuation.");
            mapViewer.repaint();
            return;
        }

        double lat = clickPos.getLatitude();
        double lng = clickPos.getLongitude();
        Zone closest = null;
        double minDist = Double.MAX_VALUE;
        for (Zone z : zones) {
            double dLat = z.getLatitude() - lat;
            double dLng = z.getLongitude() - lng;
            double dist = Math.sqrt(dLat * dLat + dLng * dLng);
            if (dist < 0.007 && dist < minDist) { minDist = dist; closest = z; }
        }
        if (closest != null) {
            selectZone(closest);
            zonePainter.showPopup(closest, mapViewer, screenPoint);
        }
    }

    @Override public void update(Zone zone) { if (zone != null) updateZoneColor(zone); }
    @Override public void onZoneFlooded(Zone zone) { updateZoneColor(zone); }
    @Override public void onZoneEvacuated(Zone zone) { updateZoneColor(zone); }
    @Override public void onZoneReset(Zone zone) { updateZoneColor(zone); }
    @Override public void onSimulationUpdated() { for (Zone zone : zones) updateZoneColor(zone); refreshRouteColors(); }

    /**
     * Adds visual node.
     * @param lat the lat.
     * @param lng the lng.
     * @return the GraphNode.
     */
    private GraphNode addVisualNode(double lat, double lng) {
        GraphNode n = new GraphNode("N" + (graphOverlayPainter.visualNodes.size() + 1), lat, lng, false);
        graphOverlayPainter.visualNodes.add(n);
        return n;
    }

    /**
     * Adds visual edge.
     * @param a the a.
     * @param b the b.
     */
    private void addVisualEdge(GraphNode a, GraphNode b) {
        graphOverlayPainter.visualEdges.add(new GraphEdge(a, b, 20 + graphOverlayPainter.visualEdges.size() * 3));
    }

    /**
     * Performs connect visual nodes.
     */
    private void autoConnectVisualNodes() {
        List<GraphNode> nodes = graphOverlayPainter.visualNodes;
        if (nodes.size() < 2) return;
        for (int i = Math.max(1, nodes.size() - 5); i < nodes.size(); i++) {
            GraphNode a = nodes.get(i);
            GraphNode b = nodes.get(Math.max(0, i - 1));
            addVisualEdge(a, b);
        }
    }

    /**
     * Sets the graph info.
     * @param text the text.
     */
    public void setGraphInfo(String text) {
        setInfo(text);
    }

    /**
     * Sets the info.
     * @param text the text.
     */
    private void setInfo(String text) {
        if (onGraphInfoChanged != null) Platform.runLater(() -> onGraphInfoChanged.accept(text));
    }

    /**
     * Performs of.
     * @param a the a.
     * @return the String.
     */
    private String nameOf(Agent a) {
        if (a == null) return "Agent";
        String n = ((a.getFirstName() == null ? "" : a.getFirstName()) + " " + (a.getLastName() == null ? "" : a.getLastName())).trim();
        return n.isBlank() ? "Agent #" + a.getId() : n;
    }

    /**
     * Performs info.
     * @param a the a.
     * @return the String.
     */
    private String agentInfo(Agent a) {
        return nameOf(a) + " | vitesse max=" + String.format("%.1f", a.getMaxSpeed()) + " | tolérance congestion=" + String.format("%.1f", a.getCongestionTolerance());
    }

    private static class RouteHighlightPainter implements Painter<JXMapViewer> {
        private Zone from, to;
    
        void setRoute(Zone from, Zone to) {
            this.from = from;
            this.to = to;
        }
    
        void clear() {
            this.from = null;
            this.to = null;
        }
    
        @Override
        /**
         * Performs paint.
         * @param g the g.
         * @param map the map.
         * @param w the w.
         * @param h the h.
         */
        public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
            // On ne dessine rien ici.
            // Les vraies arêtes sont dessinées dans GraphOverlayPainter.
        }
    }

    private class GraphOverlayPainter implements Painter<JXMapViewer> {
        private final List<GraphNode> baseNodes = new ArrayList<>();
        private final List<GraphNode> visualNodes = new ArrayList<>();
        private final List<GraphEdge> visualEdges = new ArrayList<>();
        private RouteGraph rg;
        private Object selected;

        void setRouteGraph(RouteGraph rg) { this.rg = rg; rebuildBaseNodes(zones); }
        void setSelected(Object selected) { this.selected = selected; }

        void rebuildBaseNodes(List<Zone> zones) {
            baseNodes.clear();
            if (zones == null) return;
            for (Zone z : zones) baseNodes.add(new GraphNode(z.getName(), z.getLatitude(), z.getLongitude(), true, z));
        }

        GraphNode nodeFor(Zone zone) {
            if (zone == null) return null;
            for (GraphNode n : baseNodes) if (n.zone != null && n.zone.getId() == zone.getId()) return n;
            return null;
        }

        @Override

    /**
     * Performs paint.
     * @param g the g.
     * @param map the map.
     * @param w the w.
     * @param h the h.
     */
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawFlood(g2, map);

        if (rg != null) {

            for (Edge e : rg.getEdges()) {

                drawRealEdge(g2, map, e);

            }

        }

        for (GraphNode n : baseNodes) {
            drawNode(g2, map, n);
        }
        
        for (GraphNode n : visualNodes) {
            drawNode(g2, map, n);
        }

        g2.dispose();

    }

    /**
     * Performs flood.
     * @param g2 the g2.
     * @param map the map.
     */
    private void drawFlood(Graphics2D g2, JXMapViewer map) {
        if (floodCenter == null) return;
    
        Point2D p = map.convertGeoPositionToPoint(floodCenter);
    
        int cx = (int) p.getX();
        int cy = (int) p.getY();
        int r = (int) floodRadius;
    
        if (r <= 0) return;
    
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
        // Eau principale transparente
        g2.setColor(new Color(0, 145, 255, 55));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
    
        // Plusieurs vagues irrégulières
        for (int wave = 0; wave < 4; wave++) {
            int waveRadius = r - wave * 35;
            if (waveRadius <= 10) continue;
    
            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
    
            for (int angle = 0; angle <= 360; angle += 8) {
                double rad = Math.toRadians(angle);
    
                double deformation =
                        Math.sin(rad * 5 + floodRadius * 0.08 + wave) * 8
                        + Math.cos(rad * 3 + floodRadius * 0.05) * 5;
    
                double rr = waveRadius + deformation;
    
                double x = cx + Math.cos(rad) * rr;
                double y = cy + Math.sin(rad) * rr;
    
                if (angle == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
    
            path.closePath();
    
            g2.setColor(new Color(0, 190, 255, 35));
            g2.fill(path);
    
            g2.setColor(new Color(0, 210, 255, 120));
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(path);
        }
    
        // Centre un peu plus foncé
        int inner = Math.max(15, r / 3);
        g2.setColor(new Color(0, 90, 210, 60));
        g2.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
    }

        /**
         * Performs real edge.
         * @param g2 the g2.
         * @param map the map.
         * @param e the e.
         */
        private void drawRealEdge(Graphics2D g2, JXMapViewer map, Edge e) {
            List<GeoPosition> pts = e.getWaypoints();
        
            if (pts == null || pts.size() < 2) {
                pts = List.of(
                    new GeoPosition(e.getFromZone().getLatitude(), e.getFromZone().getLongitude()),
                    new GeoPosition(e.getToZone().getLatitude(), e.getToZone().getLongitude())
                );
            }
        
            Color edgeColor = new Color(14, 165, 233, 220); // bleu
            float width = 4f;
        
            switch (e.getState()) {
                case SAFE -> edgeColor = new Color(14, 165, 233, 220);
                case AT_RISK -> edgeColor = new Color(245, 158, 11, 230);
                case FLOODING -> edgeColor = new Color(249, 115, 22, 230);
                case CONGESTED -> edgeColor = new Color(234, 179, 8, 230);
                case OVERLOADED -> edgeColor = new Color(185, 28, 28, 230);
                case FLOODED -> edgeColor = new Color(239, 68, 68, 230);
            }
        
            g2.setColor(edgeColor);
            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
            for (int i = 0; i < pts.size() - 1; i++) {
                Point2D p1 = map.convertGeoPositionToPoint(pts.get(i));
                Point2D p2 = map.convertGeoPositionToPoint(pts.get(i + 1));
        
                g2.drawLine(
                    (int) p1.getX(),
                    (int) p1.getY(),
                    (int) p2.getX(),
                    (int) p2.getY()
                );
            }
        }

        /**
         * Performs visual edge.
         * @param g2 the g2.
         * @param map the map.
         * @param e the e.
         */
        private void drawVisualEdge(Graphics2D g2, JXMapViewer map, GraphEdge e) {
            Point2D a = map.convertGeoPositionToPoint(e.from.geo());
            Point2D b = map.convertGeoPositionToPoint(e.to.geo());
            g2.setColor(selected == e ? new Color(250, 204, 21, 230) : new Color(14, 165, 233, 180));
            g2.setStroke(new BasicStroke(selected == e ? 5f : 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(a, b));
        }

    
        /**
         * Performs node.
         * @param g2 the g2.
         * @param map the map.
         * @param n the n.
         */
        private void drawNode(Graphics2D g2, JXMapViewer map, GraphNode n) {
            Point2D p = map.convertGeoPositionToPoint(n.geo());
            boolean sel = selected == n || pendingEdgeStart == n;
        
            int r = n.base ? 10 : 8;
        
            Color nodeColor;
        
            if (n.zone instanceof Shelter) {
                nodeColor = new Color(147, 51, 234); // violet = refuge
            } else if (n.base) {
                nodeColor = new Color(34, 211, 238); // bleu = quartier / zone normale
            } else {
                nodeColor = new Color(14, 165, 233); // bleu clair = nœud ajouté manuellement
            }
        
            if (sel) {
                nodeColor = new Color(250, 204, 21); // sélection en jaune
            }
        
            // Ombre noire autour
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillOval((int) p.getX() - r - 4, (int) p.getY() - r - 4, (r + 4) * 2, (r + 4) * 2);
        
            // Remplissage du nœud
            g2.setColor(nodeColor);
            g2.fillOval((int) p.getX() - r, (int) p.getY() - r, r * 2, r * 2);
        
            // Contour noir
            g2.setColor(new Color(15, 23, 42));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval((int) p.getX() - r, (int) p.getY() - r, r * 2, r * 2);
        }
        Object hitTest(JXMapViewer map, Point p) {
            // MODIFIÉ — baseNodes en priorité, rayon 18px au lieu de 15
            for (GraphNode n : baseNodes)  if (map.convertGeoPositionToPoint(n.geo()).distance(p) < 18) return n;
            for (GraphNode n : visualNodes) if (map.convertGeoPositionToPoint(n.geo()).distance(p) < 18) return n;
            for (GraphEdge e : visualEdges) {
                Point2D a = map.convertGeoPositionToPoint(e.from.geo());
                Point2D b = map.convertGeoPositionToPoint(e.to.geo());
                if (new Line2D.Double(a, b).ptSegDist(p) < 8) return e;
            }
            if (rg != null) {
                for (Edge e : rg.getEdges()) {
                    Point2D a = map.convertGeoPositionToPoint(new GeoPosition(e.getFromZone().getLatitude(), e.getFromZone().getLongitude()));
                    Point2D b = map.convertGeoPositionToPoint(new GeoPosition(e.getToZone().getLatitude(), e.getToZone().getLongitude()));
                    if (new Line2D.Double(a, b).ptSegDist(p) < 8) return e;
                }
            }
            return null;
        }

        String infoFor(Object hit) {
            if (hit instanceof GraphNode n) {
                return "Nœud " + n.name + " | capacité=" + n.capacity + " | agents≈" + countAgentsNear(n) + (n.isCongested() ? " | FORTE CONGESTION" : "");
            }
            if (hit instanceof GraphEdge e) return "Arête visuelle " + e.from.name + " → " + e.to.name + " | capacité=" + e.capacity + " | vitesse moy. simulée=3.2";
            if (hit instanceof Edge e) return "Arête " + e.getName() + " | flux=" + e.getCurrentFlow() + "/" + e.getCapacityMax() + " | état=" + e.getState();
            return "Sélection inconnue";
        }

        List<GraphNode> adjacent(GraphNode node) {
            List<GraphNode> result = new ArrayList<>();
            for (GraphEdge e : visualEdges) {
                if (e.from == node) result.add(e.to);
                if (e.to == node) result.add(e.from);
            }
            return result;
        }

        int countAgentsNear(GraphNode n) {
            int c = 0;
            for (Agent a : agents) {
                if (a.getPosition() == null) continue;
                double dLat = a.getPosition().getLat() - n.lat;
                double dLng = a.getPosition().getLng() - n.lng;
                if (dLat * dLat + dLng * dLng < 0.00002) c++;
            }
            return c;
        }
    }

    private class GraphNode {
        String name;
        double lat, lng;
        int capacity = 4;
        boolean base;
        Zone zone;
        GraphNode(String name, double lat, double lng, boolean base) { this(name, lat, lng, base, null); }
        GraphNode(String name, double lat, double lng, boolean base, Zone zone) { this.name = name; this.lat = lat; this.lng = lng; this.base = base; this.zone = zone; }
        GeoPosition geo() { return new GeoPosition(lat, lng); }
        boolean isCongested() { return graphOverlayPainter.countAgentsNear(this) > capacity; }
    }

    private class GraphEdge {
        GraphNode from, to;
        int capacity;
        GraphEdge(GraphNode from, GraphNode to, int capacity) { this.from = from; this.to = to; this.capacity = capacity; }
    }

    /**
     * Sets the connected user.
     * @param user the user.
     */
    public void setConnectedUser(Agent user) {
        if (user != null) {
            // Supposons que vous ayez ajouté cette méthode dans AgentPainter
            this.agentPainter.setCurrentUserId(user.getId());
            this.mapViewer.repaint();
        }
    }
}
