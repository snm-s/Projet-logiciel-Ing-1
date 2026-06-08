package view;

import model.graph.EdgeState;
import model.graph.Edge;
//import model.graph.Edge.State;
import model.graph.RouteGraph;
import model.zone.Zone;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Painter JXMapViewer pour les routes inter-zones.
 * Lit le {@link RouteGraph} injecté et trace chaque arête selon son état.
 *
 * <ul>
 *   <li>SAFE    → vert plein 2.5px</li>
 *   <li>AT_RISK → orange tirets 10-5, 2px</li>
 *   <li>FLOODED → rouge tirets 6-4, 3px</li>
 * </ul>
 *
 * Suit le tracé réel (waypoints OSRM) si disponible,
 * sinon ligne droite entre les deux zones.
 */
public class RoutePainter implements Painter<JXMapViewer> {

    private RouteGraph   routeGraph;
    private JXMapViewer  mapViewer;

    // ─── Couleurs ──────────────────────────────────────────────────────────
    private static final Color ROUTE_SAFE    = new Color(34,  197, 94,  210);
    private static final Color ROUTE_RISK    = new Color(245, 158, 11,  210);
    private static final Color ROUTE_FLOODED = new Color(239, 68,  68,  230);

    // ─────────────────────────────────────────────────────────────────────

    public RoutePainter() {}

    public void setRouteGraph(RouteGraph rg) { 
        this.routeGraph = rg; 
        System.out.println("Graphe reçu avec " + (routeGraph != null ? routeGraph.getEdges().size() : "null") + " arêtes.");
    }
    
    public void setMapViewer(JXMapViewer mv)  { this.mapViewer  = mv; }

    // ─────────────────────────────────────────────────────────────────────
    // PAINT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void paint(Graphics2D g2, JXMapViewer map, int w, int h) {
        if (routeGraph == null) return;

        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        /*
        Rectangle rect = map.getViewportBounds();
        g2.translate(-rect.x, -rect.y);
        */

        for (Edge edge : routeGraph.getEdges()) {
            drawEdge(g2, map, edge);
        }

        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────
    // DESSIN D'UNE ARÊTE
    // ─────────────────────────────────────────────────────────────────────

    private void drawEdge(Graphics2D g2, JXMapViewer map, Edge edge) {
        try {
            List<GeoPosition> waypoints = edge.getWaypoints();
            if (waypoints.size() < 2) {
                // Fallback : ligne droite entre les deux zones
                waypoints = new ArrayList<>();
                waypoints.add(new GeoPosition(
                    edge.getFromZone().getLatitude(), edge.getFromZone().getLongitude()));
                waypoints.add(new GeoPosition(
                    edge.getToZone().getLatitude(), edge.getToZone().getLongitude()));
            }

            // Construire le chemin pixel
            GeneralPath path = buildPath(map, waypoints);

            // Style selon l'état
            RouteStyle style = resolveStyle(edge.getState());
            g2.setColor(style.color);
            g2.setStroke(style.stroke);
            g2.draw(path);

            // Indicateur de capacité (petite barre d'occupation) au milieu
            drawCapacityIndicator(g2, map, edge, waypoints);

        } catch (Exception ignored) {}
    }

    /** Construit le chemin vectoriel depuis la liste de waypoints GPS. */
    private GeneralPath buildPath(JXMapViewer map, List<GeoPosition> waypoints) {
        GeneralPath path = new GeneralPath();
        boolean first = true;
        for (GeoPosition gp : waypoints) {
            Point2D pt = map.convertGeoPositionToPoint(gp);
            if (first) { path.moveTo(pt.getX(), pt.getY()); first = false; }
            else         path.lineTo(pt.getX(), pt.getY());
        }
        return path;
    }

    /**
     * Dessine un petit segment coloré au milieu de l'arête pour indiquer
     * le taux d'occupation (blanc = vide, orange = saturé).
     */
    private void drawCapacityIndicator(Graphics2D g2, JXMapViewer map,
                                        Edge edge, List<GeoPosition> waypoints) {
        if (edge.getCapacityMax() <= 0) return;
        try {
            // Point médian
            int mid = waypoints.size() / 2;
            Point2D pt = map.convertGeoPositionToPoint(waypoints.get(mid));

            double ratio = (double) edge.getCurrentFlow() / edge.getCapacityMax();
            if (ratio <= 0.05) return; // pas d'agents = rien à afficher

            // Couleur du taux de remplissage
            Color barColor = ratio > 0.8 ? new Color(239, 68, 68, 200)
                           : ratio > 0.5 ? new Color(245, 158, 11, 200)
                           : new Color(34, 197, 94, 200);

            int barW = (int) (20 * ratio);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect((int) pt.getX() - 11, (int) pt.getY() - 5, 22, 8, 3, 3);
            g2.setColor(barColor);
            g2.fillRoundRect((int) pt.getX() - 10, (int) pt.getY() - 4, barW, 6, 2, 2);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // STYLES
    // ─────────────────────────────────────────────────────────────────────

    
    private RouteStyle resolveStyle(EdgeState state) {
        // Note : state.color utilise la couleur définie dans votre enum
        Color color = state.color; 
        
        return switch (state) {
            case FLOODED -> new RouteStyle(color, 
                new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 4}, 0));
                
            case OVERLOADED -> new RouteStyle(color, 
                new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Très épais
                
            case CONGESTED -> new RouteStyle(color, 
                new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Épais
                
            case AT_RISK -> new RouteStyle(color, 
                new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{10, 5}, 0));
                
            case SAFE -> new RouteStyle(color, 
                new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        };
    }

    private record RouteStyle(Color color, Stroke stroke) {}
}