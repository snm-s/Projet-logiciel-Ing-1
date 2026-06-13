package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import model.algorithms.EvacuationPath;
import model.graph.Edge;
import model.graph.EdgeState;
import model.graph.RouteGraph;

/**
 * Dessine le vrai graphe RouteGraph sur la carte.
 *
 * Version lisible :
 * - les arêtes du réseau sont fines et transparentes ;
 * - les arêtes dangereuses restent visibles, mais ne masquent plus la carte ;
 * - le chemin Dijkstra sélectionné est dessiné en bleu épais ;
 * - la congestion est indiquée par un petit point au milieu de l'arête, pas par 50 gros labels.
 */
public class RoutePainter implements Painter<JXMapViewer> {

    private RouteGraph routeGraph;
    private EvacuationPath highlightedPath;
    private JXMapViewer mapViewer;
    private Color overridePathColor = null;

    private static final Color SAFE       = new Color(34, 197, 94, 140);
    private static final Color RISK       = new Color(245, 158, 11, 150);
    private static final Color FLOODED    = new Color(239, 68, 68, 170);
    private static final Color CONGESTED  = new Color(234, 179, 8, 170);
    private static final Color OVERLOADED = new Color(185, 28, 28, 185);
    private static final Color PATH_BLUE  = new Color(14, 115, 235, 235);

    /**
     * Constructs a new RoutePainter.
     */
    public RoutePainter() {}

    /**
     * Attach the RouteGraph used to draw edges and compute styles.
     *
     * @param routeGraph the RouteGraph to visualize
     */
    public void setRouteGraph(RouteGraph routeGraph) {
        this.routeGraph = routeGraph;
    }

    /**
     * Set the map viewer instance used to convert geo positions to screen points.
     *
     * @param mapViewer JXMapViewer instance
     */
    public void setMapViewer(JXMapViewer mapViewer) {
        this.mapViewer = mapViewer;
    }

    /**
     * Highlight a computed evacuation path on top of the route graph.
     *
     * @param path the EvacuationPath to highlight, or null to clear
     */
    public void setHighlightedPath(EvacuationPath path) {
        this.highlightedPath = path;
    }

    /**
     * Clears highlighted path.
     */
    public void clearHighlightedPath() {
        this.highlightedPath = null;
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

        if (routeGraph != null) {
            for (Edge edge : routeGraph.getEdges()) {
                drawEdge(g2, map, edge, false);
            }
        }

        if (highlightedPath != null && !highlightedPath.isEmpty()) {
            for (Edge edge : highlightedPath.getEdges()) {
                drawEdge(g2, map, edge, true);
            }
        }

        g2.dispose();
    }

    /**
     * Performs edge.
     * @param g2 the g2.
     * @param map the map.
     * @param edge the edge.
     * @param highlighted the highlighted.
     */
    private void drawEdge(Graphics2D g2, JXMapViewer map, Edge edge, boolean highlighted) {
        try {
            List<GeoPosition> points = cleanWaypoints(edge);
            if (points.size() < 2) return;

            GeneralPath path = buildPath(map, points);

            if (highlighted) {
                Color base = overridePathColor != null ? overridePathColor : PATH_BLUE;
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 80));
                g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);

                g2.setColor(overridePathColor != null ? overridePathColor : PATH_BLUE);
                g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{14, 8}, 0));
                g2.draw(path);

                drawArrow(g2, map, points, overridePathColor != null ? overridePathColor : PATH_BLUE);
                return;
            }

            RouteStyle style = styleFor(edge.getState());
            g2.setColor(style.color);
            g2.setStroke(style.stroke);
            g2.draw(path);

            drawSmallCapacityDot(g2, map, edge, points);
        } catch (Exception ignored) {
        }
    }
    /**
     * Highlight a given evacuation path using a specific hex color string.
     *
     * @param path     the EvacuationPath to highlight (may be null)
     * @param hexColor CSS hex color string (e.g. "#ff0000"); invalid values are ignored
     */
    public void setHighlightedPath(model.algorithms.EvacuationPath path, String hexColor) {
        this.highlightedPath = path;
        try {
            java.awt.Color parsed = java.awt.Color.decode(hexColor);
            this.overridePathColor = new Color(
                parsed.getRed(), parsed.getGreen(), parsed.getBlue(), 235);
        } catch (Exception e) {
            this.overridePathColor = null;
        }
    }

    /**
     * Performs waypoints.
     * @param edge the edge.
     * @return the List<GeoPosition>.
     */
    private List<GeoPosition> cleanWaypoints(Edge edge) {
        List<GeoPosition> points = edge.getWaypoints();
        if (points != null && points.size() >= 2) return points;

        List<GeoPosition> fallback = new ArrayList<>();
        fallback.add(new GeoPosition(edge.getFromZone().getLatitude(), edge.getFromZone().getLongitude()));
        fallback.add(new GeoPosition(edge.getToZone().getLatitude(), edge.getToZone().getLongitude()));
        return fallback;
    }

    /**
     * Builds path.
     * @param map the map.
     * @param points the points.
     * @return the GeneralPath.
     */
    private GeneralPath buildPath(JXMapViewer map, List<GeoPosition> points) {
        GeneralPath path = new GeneralPath();
        boolean first = true;

        for (GeoPosition gp : points) {
            Point2D pt = map.convertGeoPositionToPoint(gp);
            if (first) {
                path.moveTo(pt.getX(), pt.getY());
                first = false;
            } else {
                path.lineTo(pt.getX(), pt.getY());
            }
        }

        return path;
    }

    /**
     * Performs small capacity dot.
     * @param g2 the g2.
     * @param map the map.
     * @param edge the edge.
     * @param points the points.
     */
    private void drawSmallCapacityDot(Graphics2D g2, JXMapViewer map, Edge edge, List<GeoPosition> points) {
        if (edge.getCapacityMax() <= 0 || edge.getCurrentFlow() <= 0) return;

        double ratio = (double) edge.getCurrentFlow() / edge.getCapacityMax();
        if (ratio < 0.25) return;

        GeoPosition mid = points.get(points.size() / 2);
        Point2D p = map.convertGeoPositionToPoint(mid);

        Color c = ratio > 1.0 ? OVERLOADED : ratio > 0.75 ? CONGESTED : RISK;
        int r = ratio > 1.0 ? 7 : 5;

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillOval((int) p.getX() - r - 2, (int) p.getY() - r - 2, (r + 2) * 2, (r + 2) * 2);
        g2.setColor(c);
        g2.fillOval((int) p.getX() - r, (int) p.getY() - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval((int) p.getX() - r, (int) p.getY() - r, r * 2, r * 2);
    }

    /**
     * Performs arrow.
     * @param g2 the g2.
     * @param map the map.
     * @param points the points.
     * @param color the color.
     */
    private void drawArrow(Graphics2D g2, JXMapViewer map, List<GeoPosition> points, Color color) {
        if (points.size() < 2) return;

        GeoPosition a = points.get(Math.max(0, points.size() / 2 - 1));
        GeoPosition b = points.get(Math.min(points.size() - 1, points.size() / 2));

        Point2D p1 = map.convertGeoPositionToPoint(a);
        Point2D p2 = map.convertGeoPositionToPoint(b);

        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;

        double ux = dx / len;
        double uy = dy / len;
        double mx = (p1.getX() + p2.getX()) / 2.0;
        double my = (p1.getY() + p2.getY()) / 2.0;

        int size = 9;
        int[] xs = {
                (int) (mx + ux * size),
                (int) (mx - ux * size - uy * size * 0.6),
                (int) (mx - ux * size + uy * size * 0.6)
        };
        int[] ys = {
                (int) (my + uy * size),
                (int) (my - uy * size + ux * size * 0.6),
                (int) (my - uy * size - ux * size * 0.6)
        };

        g2.setColor(color);
        g2.fillPolygon(xs, ys, 3);
    }

    /**
     * Performs for.
     * @param state the state.
     * @return the RouteStyle.
     */
    private RouteStyle styleFor(EdgeState state) {
        if (state == null) {
            return new RouteStyle(SAFE, new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }

        return switch (state) {
            case FLOODING -> new RouteStyle(RISK,
        new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{9, 6}, 0));
            case FLOODED -> new RouteStyle(FLOODED,
                    new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8, 6}, 0));
            case OVERLOADED -> new RouteStyle(OVERLOADED,
                    new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            case CONGESTED -> new RouteStyle(CONGESTED,
                    new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            case AT_RISK -> new RouteStyle(RISK,
                    new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{10, 6}, 0));
            case SAFE -> new RouteStyle(SAFE,
                    new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        };
    }

    /**
     * Performs style.
     * @param color the color.
     * @param stroke the stroke.
     * @return the record.
     */
    private record RouteStyle(Color color, Stroke stroke) {}
}
