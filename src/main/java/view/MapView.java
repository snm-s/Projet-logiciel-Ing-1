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
                    draggedAgent = agent;
                    agentPainter.startDrag(agent);
                    if (onAgentSelected != null) onAgentSelected.accept(agent);
                    setInfo("Agent attrapé : " + nameOf(agent) + " → état stressé/paniqué.");
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
        agentPainter.setAgents(this.agents, zones);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    public void setOnAgentSelected(Consumer<Agent> callback) { this.onAgentSelected = callback; }
    public void setOnZoneSelected(Consumer<Zone> callback) { this.onZoneSelected = callback; }
    public void setOnGraphInfoChanged(Consumer<String> callback) { this.onGraphInfoChanged = callback; }

    public void setEditMode(EditMode mode) {
        this.editMode = mode == null ? EditMode.SELECT : mode;
        this.pendingEdgeStart = null;
        setInfo("Mode : " + this.editMode.name());
    }

    public EditMode getEditMode() { return editMode; }

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
            agents.remove(a);
            agentPainter.setAgents(agents, zones);
            selectedGraphElement = null;
            setInfo("Agent supprimé.");
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
    
    public void generateLocalGraphForAgent(Agent agent) {
        if (agent == null || agent.getPosition() == null) return;
    
        double lat = agent.getPosition().getLat();
        double lng = agent.getPosition().getLng();
    
        SwingUtilities.invokeLater(() -> {
            graphOverlayPainter.visualNodes.clear();
            graphOverlayPainter.visualEdges.clear();
    
            GraphNode position = new GraphNode("Position actuelle", lat, lng, false);
            GraphNode nord = new GraphNode("Carrefour Nord", lat + 0.003, lng, false);
            GraphNode sud = new GraphNode("Carrefour Sud", lat - 0.003, lng, false);
            GraphNode est = new GraphNode("Carrefour Est", lat, lng + 0.003, false);
            GraphNode ouest = new GraphNode("Carrefour Ouest", lat, lng - 0.003, false);
            GraphNode refuge = new GraphNode("Refuge conseillé", lat + 0.006, lng + 0.006, false);
    
            graphOverlayPainter.visualNodes.add(position);
            graphOverlayPainter.visualNodes.add(nord);
            graphOverlayPainter.visualNodes.add(sud);
            graphOverlayPainter.visualNodes.add(est);
            graphOverlayPainter.visualNodes.add(ouest);
            graphOverlayPainter.visualNodes.add(refuge);
    
            graphOverlayPainter.visualEdges.add(new GraphEdge(position, nord, 10));
            graphOverlayPainter.visualEdges.add(new GraphEdge(position, sud, 10));
            graphOverlayPainter.visualEdges.add(new GraphEdge(position, est, 10));
            graphOverlayPainter.visualEdges.add(new GraphEdge(position, ouest, 10));
            graphOverlayPainter.visualEdges.add(new GraphEdge(est, refuge, 8));
            graphOverlayPainter.visualEdges.add(new GraphEdge(nord, refuge, 8));
            graphOverlayPainter.visualEdges.add(new GraphEdge(ouest, sud, 6));
    
            selectedGraphElement = position;
            graphOverlayPainter.setSelected(position);
    
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

    public void clearRoute() { SwingUtilities.invokeLater(() -> { routeHighlightPainter.clear(); mapViewer.repaint(); }); }

    private void handleMapClick(Point screenPoint) {
        GeoPosition clickPos = mapViewer.convertPointToGeoPosition(screenPoint);
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
        void setRoute(Zone from, Zone to) { this.from = from; this.to = to; }
        void clear() { this.from = null; this.to = null; }
        @Override public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
            if (from == null || to == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                Point2D p1 = map.convertGeoPositionToPoint(new GeoPosition(from.getLatitude(), from.getLongitude()));
                Point2D p2 = map.convertGeoPositionToPoint(new GeoPosition(to.getLatitude(), to.getLongitude()));
                g2.setColor(new Color(14, 115, 235, 70));
                g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
                g2.setColor(new Color(14, 115, 235, 245));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{12, 8}, 0));
                g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            } catch (Exception ignored) {}
            g2.dispose();
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

        @Override public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (GraphEdge e : visualEdges) drawVisualEdge(g2, map, e);
            for (GraphNode n : baseNodes) drawNode(g2, map, n);
            for (GraphNode n : visualNodes) drawNode(g2, map, n);

            if (rg != null) {
                for (Edge e : rg.getEdges()) {
                    Point2D a = map.convertGeoPositionToPoint(new GeoPosition(e.getFromZone().getLatitude(), e.getFromZone().getLongitude()));
                    Point2D b = map.convertGeoPositionToPoint(new GeoPosition(e.getToZone().getLatitude(), e.getToZone().getLongitude()));
                    int midX = (int) ((a.getX() + b.getX()) / 2);
                    int midY = (int) ((a.getY() + b.getY()) / 2);
                    if (selected == e) {
                        g2.setColor(new Color(250, 204, 21, 220));
                        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
                    }
                    // Pas de gros labels partout : la congestion est visible par la couleur/épaisseur des arêtes
                    // et par les statistiques quand l'arête est sélectionnée.
                }
            }
            g2.dispose();
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
            int r = n.base ? 9 : 8;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillOval((int) p.getX() - r - 3, (int) p.getY() - r - 3, (r + 3) * 2, (r + 3) * 2);
            g2.setColor(sel ? new Color(250, 204, 21) : n.base ? new Color(255, 255, 255) : new Color(34, 211, 238));
            g2.fillOval((int) p.getX() - r, (int) p.getY() - r, r * 2, r * 2);
            g2.setColor(n.isCongested() ? new Color(239, 68, 68) : new Color(15, 23, 42));
            g2.setStroke(new BasicStroke(2f));
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
