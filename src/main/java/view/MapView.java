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

    private static final GeoPosition LYON_CENTER = new GeoPosition(45.7640, 4.8357);
    private static final int DEFAULT_ZOOM = 6;

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

    private void configureInteraction() {
        PanMouseInputListener panListener = new PanMouseInputListener(mapViewer);
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
                if (editMode == EditMode.ADD_EDGE && hit instanceof GraphNode node) {
                    if (pendingEdgeStart == null) {
                        pendingEdgeStart = node;
                        selectedGraphElement = node;
                        setInfo("Départ d'arête choisi : " + node.name + ". Clique un deuxième nœud.");
                    } else if (pendingEdgeStart != node) {
                        addVisualEdge(pendingEdgeStart, node);
                        pendingEdgeStart = null;
                        setInfo("Arête ajoutée en direct.");
                    }
                    mapViewer.repaint();
                    return;
                }

                if (hit != null) {
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
            
                Object hit = graphOverlayPainter.hitTest(mapViewer, e.getPoint());
                if (editMode == EditMode.MOVE_NODE && hit instanceof GraphNode node) {
                    draggedNode = node;
                    selectedGraphElement = node;
                    graphOverlayPainter.setSelected(node);
                    mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    e.consume();
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
                    mapViewer.setCursor(Cursor.getDefaultCursor());
                    mapViewer.repaint();
                    e.consume();
                }
                if (draggedNode != null) {
                    draggedNode = null;
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

    private void loadZones() {
        zonePainter.setZones(zones);
        agentPainter.updateZones(zones);
        graphOverlayPainter.rebuildBaseNodes(zones);
        mapViewer.repaint();
    }

    /**
     * Retourne l'agent actuellement sélectionné, ou null.
     * Utilisé par SimulationView pour déléguer la suppression à SimulationController.
     */
    public Agent getSelectedAgent() {
        if (selectedGraphElement instanceof Agent a) return a;
        return null;
    }

    public JXMapViewer getMapViewer() { return mapViewer; }
    public SwingNode getSwingNode() { return swingNode; }
    public Object getWebView() { return null; }
    public Object getWebEngine() { return null; }

    public void setMapController(MapController controller) { this.mapController = controller; }

    public void setRouteGraph(RouteGraph routeGraph) {
        this.routeGraph = routeGraph;
        routePainter.setRouteGraph(routeGraph);
        graphOverlayPainter.setRouteGraph(routeGraph);
        agentPainter.setRouteGraph(routeGraph);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }
    
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

    public void setOnAgentSelected(Consumer<Agent> callback) { this.onAgentSelected = callback; }
    public void setOnZoneSelected(Consumer<Zone> callback) { this.onZoneSelected = callback; }
    public void setOnGraphInfoChanged(Consumer<String> callback) { this.onGraphInfoChanged = callback; }

    public void setEditMode(EditMode mode) {
        this.editMode = mode == null ? EditMode.SELECT : mode;
        this.pendingEdgeStart = null;
        setInfo("Mode : " + this.editMode.name());
    }

    public void setManualFloodMode(boolean enabled) {
        this.manualFloodMode = enabled;
        setInfo(enabled ? "Mode inondation manuelle : clique sur la carte." : "Mode inondation désactivé.");
    }

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

    public EditMode getEditMode() { return editMode; }

    public void setFloodSpeedFromSlider(double sliderValue) {
        floodTimerDelayMs = (int) sliderValue;
    
        floodRadiusStep = Math.max(0.5, 1000.0 / sliderValue);
    
        if (floodTimer != null && floodTimer.isRunning()) {
            startFloodPropagation();
        }
    }

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

    public void addAgent(Agent agent) {
        if (agent == null) return;
        if (!agents.contains(agent)) agents.add(agent);
        agentPainter.setAgents(agents, zones);
        setInfo("Agent ajouté : " + nameOf(agent));
        mapViewer.repaint();
    }

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

    public void deleteSelectedGraphElement() { deleteGraphElement(selectedGraphElement); }

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

    public void updateZoneColor(Zone zone) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZone(zone);
            agentPainter.updateZones(zones);
            mapViewer.repaint();
        });
    }

    public void updateZoneWithWaterLevel(Zone zone, double niveauEau) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZoneWaterLevel(zone, niveauEau);
            agentPainter.updateZones(zones);
            mapViewer.repaint();
        });
    }

    public void refreshRouteColors() { SwingUtilities.invokeLater(mapViewer::repaint); }

    public void updateAllZones(List<Zone> updatedZones) {
        this.zones = updatedZones == null ? new ArrayList<>() : new ArrayList<>(updatedZones);
        SwingUtilities.invokeLater(() -> {
            zonePainter.setZones(this.zones);
            agentPainter.updateZones(this.zones);
            graphOverlayPainter.rebuildBaseNodes(this.zones);
            mapViewer.repaint();
        });
    }

    public void zoomIn() { SwingUtilities.invokeLater(() -> { int z = mapViewer.getZoom(); if (z > 1) mapViewer.setZoom(z - 1); }); }
    public void zoomOut() { SwingUtilities.invokeLater(() -> { int z = mapViewer.getZoom(); if (z < 17) mapViewer.setZoom(z + 1); }); }
    public void setZoom(int level) { SwingUtilities.invokeLater(() -> mapViewer.setZoom(level)); }
    public void resetView() { SwingUtilities.invokeLater(() -> { mapViewer.setAddressLocation(LYON_CENTER); mapViewer.setZoom(DEFAULT_ZOOM); clearRoute(); }); }
    public void flyTo(double lat, double lng) { SwingUtilities.invokeLater(() -> { mapViewer.setAddressLocation(new GeoPosition(lat, lng)); mapViewer.setZoom(2); }); }
    public void panTo(double lat, double lng) { SwingUtilities.invokeLater(() -> mapViewer.setAddressLocation(new GeoPosition(lat, lng))); }

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
    


    public void focusZone(Zone zone) {
        if (zone == null) return;
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(new GeoPosition(zone.getLatitude(), zone.getLongitude()));
            mapViewer.setZoom(3);
            selectZone(zone);
        });
    }

    public void selectZone(Zone zone) {
        if (zone == null) return;
        zonePainter.setSelectedZone(zone);
        selectedGraphElement = graphOverlayPainter.nodeFor(zone);
        graphOverlayPainter.setSelected(selectedGraphElement);
        mapViewer.repaint();
        if (onZoneSelected != null) onZoneSelected.accept(zone);
        setInfo("Nœud sélectionné : " + zone.getName());
    }

    public void highlightZone(int id) { zones.stream().filter(z -> z.getId() == id).findFirst().ifPresent(this::selectZone); }

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

    
    public void clearRoute() { SwingUtilities.invokeLater(() -> { routeHighlightPainter.clear(); mapViewer.repaint(); }); }

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
                edge.setFloodLevel(edge.getFloodLevel() + 0.12);
            }
        }
    
        mapViewer.repaint();
    }

    private void handleMapClick(Point screenPoint) {
        GeoPosition clickPos = mapViewer.convertPointToGeoPosition(screenPoint);

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

    private GraphNode addVisualNode(double lat, double lng) {
        GraphNode n = new GraphNode("N" + (graphOverlayPainter.visualNodes.size() + 1), lat, lng, false);
        graphOverlayPainter.visualNodes.add(n);
        return n;
    }

    private void addVisualEdge(GraphNode a, GraphNode b) {
        graphOverlayPainter.visualEdges.add(new GraphEdge(a, b, 20 + graphOverlayPainter.visualEdges.size() * 3));
    }

    private void autoConnectVisualNodes() {
        List<GraphNode> nodes = graphOverlayPainter.visualNodes;
        if (nodes.size() < 2) return;
        for (int i = Math.max(1, nodes.size() - 5); i < nodes.size(); i++) {
            GraphNode a = nodes.get(i);
            GraphNode b = nodes.get(Math.max(0, i - 1));
            addVisualEdge(a, b);
        }
    }

    public void setGraphInfo(String text) {
        setInfo(text);
    }

    private void setInfo(String text) {
        if (onGraphInfoChanged != null) Platform.runLater(() -> onGraphInfoChanged.accept(text));
    }

    private String nameOf(Agent a) {
        if (a == null) return "Agent";
        String n = ((a.getFirstName() == null ? "" : a.getFirstName()) + " " + (a.getLastName() == null ? "" : a.getLastName())).trim();
        return n.isBlank() ? "Agent #" + a.getId() : n;
    }

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

        private void drawVisualEdge(Graphics2D g2, JXMapViewer map, GraphEdge e) {
            Point2D a = map.convertGeoPositionToPoint(e.from.geo());
            Point2D b = map.convertGeoPositionToPoint(e.to.geo());
            g2.setColor(selected == e ? new Color(250, 204, 21, 230) : new Color(14, 165, 233, 180));
            g2.setStroke(new BasicStroke(selected == e ? 5f : 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(a, b));
        }

    
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
            for (GraphNode n : visualNodes) if (map.convertGeoPositionToPoint(n.geo()).distance(p) < 15) return n;
            for (GraphNode n : baseNodes) if (map.convertGeoPositionToPoint(n.geo()).distance(p) < 15) return n;
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
}