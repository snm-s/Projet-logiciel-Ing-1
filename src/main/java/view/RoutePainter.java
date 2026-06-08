package view;

import model.zone.Zone;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Painter JXMapViewer pour les routes inter-zones.
 *
 * Couleurs :
 *   - Vert   (#22c55e) = deux zones sûres
 *   - Orange (#f59e0b) = une zone à risque (altitude basse ou une inondée)
 *   - Rouge  (#ef4444) = les deux zones inondées
 *
 * Style :
 *   - Route sûre     : ligne pleine, 2.5px
 *   - Route à risque : tirets 10-5, 2px
 *   - Route inondée  : tirets 6-4, 3px
 */
public class RoutePainter implements Painter<JXMapViewer> {

    private List<Zone>       zones = new ArrayList<>();
    private List<int[]>      customRoutes = null; // null = réseau par défaut

    // Réseau par défaut entre les zones (paires d'IDs)
    private static final int[][] DEFAULT_CONNECTIONS = {
        {1, 2}, {1, 3}, {1, 4}, {1, 5},
        {2, 6}, {2, 10},
        {3, 7}, {3, 9},
        {4, 9}, {4, 1},
        {5, 7}, {5, 10},
        {6, 10}, {6, 2},
        {7, 8}, {7, 5},
        {8, 3}, {8, 7},
        {9, 4}, {9, 3},
        {10, 5}, {10, 2}
    };

    // ─── Couleurs routes ──────────────────────────────────────────────────
    private static final Color ROUTE_SAFE    = new Color(34,  197, 94,  210);
    private static final Color ROUTE_RISK    = new Color(245, 158, 11,  210);
    private static final Color ROUTE_FLOODED = new Color(239, 68,  68,  230);

    // ─────────────────────────────────────────────────────────────────────

    public RoutePainter(List<Zone> zones) {
        setZones(zones);
    }

    public synchronized void setZones(List<Zone> zones) {
        this.zones = new ArrayList<>(zones);
    }

    /** Définit un réseau de routes custom (liste de paires d'IDs). */
    public synchronized void setCustomRoutes(List<int[]> routes) {
        this.customRoutes = routes == null ? null : new ArrayList<>(routes);
    }

    /** Revient au réseau par défaut. */
    public synchronized void useDefaultRoutes() {
        this.customRoutes = null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAINT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void paint(Graphics2D g2, JXMapViewer map, int w, int h) {
        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        List<Zone>  zSnap;
        List<int[]> connections;
        synchronized (this) {
            zSnap       = new ArrayList<>(zones);
            connections = customRoutes != null
                ? new ArrayList<>(customRoutes)
                : toList(DEFAULT_CONNECTIONS);
        }

        Map<Integer, Zone> zoneMap = buildZoneMap(zSnap);

        for (int[] edge : connections) {
            if (edge.length < 2) continue;
            Zone from = zoneMap.get(edge[0]);
            Zone to   = zoneMap.get(edge[1]);
            if (from == null || to == null) continue;
            drawRoute(g2, map, from, to);
        }

        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────
    // DESSIN D'UNE ROUTE
    // ─────────────────────────────────────────────────────────────────────

    private void drawRoute(Graphics2D g2, JXMapViewer map, Zone from, Zone to) {
        try {
            Point2D pFrom = map.convertGeoPositionToPoint(
                new GeoPosition(from.getLatitude(), from.getLongitude()));
            Point2D pTo   = map.convertGeoPositionToPoint(
                new GeoPosition(to.getLatitude(), to.getLongitude()));

            RouteStyle style = computeStyle(from, to);

            g2.setColor(style.color);
            g2.setStroke(style.stroke);

            // Ligne légèrement courbée (bezier) pour un aspect plus naturel
            GeneralPath path = buildCurvedPath(pFrom, pTo);
            g2.draw(path);

            // Flèche directionnelle au milieu
            drawArrowHead(g2, pFrom, pTo, style.color);

        } catch (Exception ignored) {}
    }

    /**
     * Construit une courbe de Bézier quadratique légèrement déviée vers la droite,
     * ce qui donne l'impression d'une route sinueuse plutôt qu'un segment rectiligne.
     */
    private GeneralPath buildCurvedPath(Point2D from, Point2D to) {
        double mx = (from.getX() + to.getX()) / 2.0;
        double my = (from.getY() + to.getY()) / 2.0;

        // Perpendiculaire au segment, décalage proportionnel à la longueur
        double dx  = to.getX() - from.getX();
        double dy  = to.getY() - from.getY();
        double len = Math.sqrt(dx * dx + dy * dy);
        double offset = Math.min(len * 0.08, 20.0);

        // Contrôle décalé perpendiculairement
        double ctrlX = mx - dy / len * offset;
        double ctrlY = my + dx / len * offset;

        GeneralPath path = new GeneralPath();
        path.moveTo(from.getX(), from.getY());
        path.quadTo(ctrlX, ctrlY, to.getX(), to.getY());
        return path;
    }

    /** Dessine une petite flèche directionnelle au milieu de la route. */
    private void drawArrowHead(Graphics2D g2, Point2D from, Point2D to, Color color) {
        double mx = (from.getX() + to.getX()) / 2.0;
        double my = (from.getY() + to.getY()) / 2.0;

        double dx  = to.getX() - from.getX();
        double dy  = to.getY() - from.getY();
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 30) return; // trop proche, pas de flèche

        double ux = dx / len;
        double uy = dy / len;
        double arrowSize = 6.0;

        int[] xPoints = {
            (int)(mx + ux * arrowSize),
            (int)(mx - ux * arrowSize - uy * arrowSize * 0.5),
            (int)(mx - ux * arrowSize + uy * arrowSize * 0.5)
        };
        int[] yPoints = {
            (int)(my + uy * arrowSize),
            (int)(my - uy * arrowSize + ux * arrowSize * 0.5),
            (int)(my - uy * arrowSize - ux * arrowSize * 0.5)
        };

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
        g2.fillPolygon(xPoints, yPoints, 3);
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGIQUE COULEUR / STYLE
    // ─────────────────────────────────────────────────────────────────────

    private RouteStyle computeStyle(Zone from, Zone to) {
        if (from.isFlooded() && to.isFlooded()) {
            return new RouteStyle(ROUTE_FLOODED,
                new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{6, 4}, 0));
        }
        if (from.isFlooded() || to.isFlooded()) {
            return new RouteStyle(ROUTE_RISK,
                new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{10, 5}, 0));
        }
        double avgAlt = (from.getAltitude() + to.getAltitude()) / 2.0;
        if (avgAlt < 1.0) {
            return new RouteStyle(ROUTE_RISK,
                new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{10, 5}, 0));
        }
        return new RouteStyle(ROUTE_SAFE,
            new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────

    private Map<Integer, Zone> buildZoneMap(List<Zone> zoneList) {
        Map<Integer, Zone> map = new HashMap<>();
        for (Zone z : zoneList) map.put(z.getId(), z);
        return map;
    }

    private List<int[]> toList(int[][] arr) {
        List<int[]> list = new ArrayList<>();
        for (int[] a : arr) list.add(a);
        return list;
    }

    // ─── Structure interne ───────────────────────────────────────────────

    private static class RouteStyle {
        final Color  color;
        final Stroke stroke;
        RouteStyle(Color c, Stroke s) { color = c; stroke = s; }
    }
}
