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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
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
import model.graph.RouteGraph;
import model.observer.Observer;
import model.zone.Zone;
import model.zone.ZoneUpdateListener;

/**
 * Carte JXMapViewer.
 * Elle garde ta vraie carte OSM en arrière-plan, dessine zones + routes + Mii agents.
 */
public class MapView implements ZoneUpdateListener, Observer<Zone> {

    private final JXMapViewer mapViewer;
    private final SwingNode swingNode;
    private List<Zone> zones;

    private ZonePainter zonePainter;
    private RoutePainter routePainter;
    private RouteHighlightPainter routeHighlightPainter;
    private AgentPainter agentPainter;
    private CompoundPainter<JXMapViewer> compound;

    private MapController mapController;
    private Consumer<Zone> onZoneSelected;
    private Consumer<Agent> onAgentSelected;

    private Agent draggedAgent;

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

        routeHighlightPainter = new RouteHighlightPainter();

        agentPainter = new AgentPainter();
        agentPainter.setAgents(List.of(), zones);

        compound = new CompoundPainter<>();
        compound.addPainter(routePainter);
        compound.addPainter(routeHighlightPainter);
        compound.addPainter(zonePainter);
        compound.addPainter(agentPainter);

        mapViewer.setOverlayPainter(compound);
    }

    private void configureInteraction() {
        PanMouseInputListener panListener = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panListener);
        mapViewer.addMouseMotionListener(panListener);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Agent clickedAgent = agentPainter.findAgentAt(mapViewer, e.getPoint());
                if (clickedAgent != null) {
                    if (onAgentSelected != null) onAgentSelected.accept(clickedAgent);
                    mapViewer.repaint();
                    return;
                }

                handleMapClick(e.getPoint());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Agent agent = agentPainter.findAgentAt(mapViewer, e.getPoint());
                if (agent != null) {
                    draggedAgent = agent;
                    agentPainter.startDrag(agent);
                    if (onAgentSelected != null) onAgentSelected.accept(agent);
                    mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    mapViewer.repaint();
                    e.consume();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedAgent != null) {
                    GeoPosition pos = mapViewer.convertPointToGeoPosition(e.getPoint());
                    agentPainter.dragTo(draggedAgent, pos);
                    mapViewer.repaint();
                    e.consume();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedAgent != null) {
                    agentPainter.endDrag();
                    draggedAgent = null;
                    mapViewer.setCursor(Cursor.getDefaultCursor());
                    mapViewer.repaint();
                    e.consume();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Agent over = agentPainter.findAgentAt(mapViewer, e.getPoint());
                agentPainter.setHoveredAgent(over);
                mapViewer.setCursor(over != null
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
        mapViewer.repaint();
    }

    public JXMapViewer getMapViewer() {
        return mapViewer;
    }

    public SwingNode getSwingNode() {
        return swingNode;
    }

    public Object getWebView() {
        return null;
    }

    public Object getWebEngine() {
        return null;
    }

    public void setMapController(MapController controller) {
        this.mapController = controller;
    }

    public void setRouteGraph(RouteGraph routeGraph) {
        routePainter.setRouteGraph(routeGraph);
        agentPainter.setRouteGraph(routeGraph);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    public void setAgents(List<Agent> agents) {
        agentPainter.setAgents(agents, zones);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    public void setOnAgentSelected(Consumer<Agent> callback) {
        this.onAgentSelected = callback;
    }

    public void setOnZoneSelected(Consumer<Zone> callback) {
        this.onZoneSelected = callback;
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

    public void refreshRouteColors() {
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    public void updateAllZones(List<Zone> updatedZones) {
        this.zones = updatedZones == null ? new ArrayList<>() : new ArrayList<>(updatedZones);

        SwingUtilities.invokeLater(() -> {
            zonePainter.setZones(this.zones);
            agentPainter.updateZones(this.zones);
            mapViewer.repaint();
        });
    }

    public void zoomIn() {
        SwingUtilities.invokeLater(() -> {
            int z = mapViewer.getZoom();
            if (z > 1) mapViewer.setZoom(z - 1);
        });
    }

    public void zoomOut() {
        SwingUtilities.invokeLater(() -> {
            int z = mapViewer.getZoom();
            if (z < 17) mapViewer.setZoom(z + 1);
        });
    }

    public void setZoom(int level) {
        SwingUtilities.invokeLater(() -> mapViewer.setZoom(level));
    }

    public void resetView() {
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(LYON_CENTER);
            mapViewer.setZoom(DEFAULT_ZOOM);
            clearRoute();
        });
    }

    public void flyTo(double lat, double lng) {
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(new GeoPosition(lat, lng));
            mapViewer.setZoom(2);
        });
    }

    public void panTo(double lat, double lng) {
        SwingUtilities.invokeLater(() ->
                mapViewer.setAddressLocation(new GeoPosition(lat, lng))
        );
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
        mapViewer.repaint();

        if (onZoneSelected != null) {
            onZoneSelected.accept(zone);
        }
    }

    public void highlightZone(int id) {
        zones.stream()
                .filter(z -> z.getId() == id)
                .findFirst()
                .ifPresent(this::selectZone);
    }

    public void showRoute(Zone from, Zone to) {
        if (from == null || to == null) return;

        SwingUtilities.invokeLater(() -> {
            routeHighlightPainter.setRoute(from, to);

            double lat = (from.getLatitude() + to.getLatitude()) / 2.0;
            double lng = (from.getLongitude() + to.getLongitude()) / 2.0;

            mapViewer.setAddressLocation(new GeoPosition(lat, lng));
            mapViewer.setZoom(4);

            zonePainter.setSelectedZone(to);
            mapViewer.repaint();
        });
    }

    public void clearRoute() {
        SwingUtilities.invokeLater(() -> {
            routeHighlightPainter.clear();
            mapViewer.repaint();
        });
    }

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

            if (dist < 0.007 && dist < minDist) {
                minDist = dist;
                closest = z;
            }
        }

        if (closest != null) {
            selectZone(closest);
            zonePainter.showPopup(closest, mapViewer, screenPoint);
        }
    }

    @Override
    public void update(Zone zone) {
        if (zone != null) updateZoneColor(zone);
    }

    @Override
    public void onZoneFlooded(Zone zone) {
        updateZoneColor(zone);
    }

    @Override
    public void onZoneEvacuated(Zone zone) {
        updateZoneColor(zone);
    }

    @Override
    public void onZoneReset(Zone zone) {
        updateZoneColor(zone);
    }

    @Override
    public void onSimulationUpdated() {
        for (Zone zone : zones) {
            updateZoneColor(zone);
        }
        refreshRouteColors();
    }

    private static class RouteHighlightPainter implements Painter<JXMapViewer> {
        private Zone from;
        private Zone to;

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
            if (from == null || to == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            try {
                Point2D p1 = map.convertGeoPositionToPoint(
                        new GeoPosition(from.getLatitude(), from.getLongitude())
                );
                Point2D p2 = map.convertGeoPositionToPoint(
                        new GeoPosition(to.getLatitude(), to.getLongitude())
                );

                g2.setColor(new Color(14, 115, 235, 70));
                g2.setStroke(new BasicStroke(
                        10f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                ));
                g2.drawLine(
                        (int) p1.getX(),
                        (int) p1.getY(),
                        (int) p2.getX(),
                        (int) p2.getY()
                );

                g2.setColor(new Color(14, 115, 235, 245));
                g2.setStroke(new BasicStroke(
                        4f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND,
                        1,
                        new float[]{12, 8},
                        0
                ));
                g2.drawLine(
                        (int) p1.getX(),
                        (int) p1.getY(),
                        (int) p2.getX(),
                        (int) p2.getY()
                );

                drawPin(g2, p1, new Color(34, 197, 94));
                drawPin(g2, p2, new Color(239, 68, 68));

            } catch (Exception ignored) {
            }

            g2.dispose();
        }

        private void drawPin(Graphics2D g2, Point2D p, Color color) {
            int x = (int) p.getX();
            int y = (int) p.getY();

            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillOval(x - 10, y - 10, 20, 20);

            g2.setColor(color);
            g2.fillOval(x - 8, y - 8, 16, 16);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x - 8, y - 8, 16, 16);
        }
    }
}
