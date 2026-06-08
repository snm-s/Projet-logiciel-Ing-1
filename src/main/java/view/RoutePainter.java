package view;

import model.graph.Edge;
import model.graph.EdgeState;
import model.graph.RouteGraph;
import model.observer.Observer;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * Painter JXMapViewer pour les routes inter-zones.
 *
 * <p>Rendu de type Google Maps :
 * <ul>
 *   <li>Ombre portée sous chaque route (couleur sombre décalée).</li>
 *   <li>Épaisseur variable selon le zoom de la carte.</li>
 *   <li>Couleur codée selon {@link EdgeState} (vert → orange → rouge → bleu).</li>
 *   <li>Indicateur de capacité (barre proportionnelle au centre de chaque route).</li>
 *   <li>Flèche directionnelle au milieu du tracé.</li>
 * </ul>
 *
 * <p>Observe chaque {@link Edge} : dès qu'un edge change d'état (inondation ou
 * variation de flux), il notifie ce painter qui demande un repaint de la carte.
 */
public class RoutePainter implements Painter<JXMapViewer>, Observer<Edge> {

    // ─── Carte ────────────────────────────────────────────────────────────
    private JXMapViewer mapViewer;

    // ─── Snapshot thread-safe ─────────────────────────────────────────────
    private volatile List<Edge> edges = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────

    public RoutePainter() {}

    public void setMapViewer(JXMapViewer mapViewer) {
        this.mapViewer = mapViewer;
    }

    public synchronized void setRouteGraph(RouteGraph graph) {
        for (Edge e : edges)       e.removeObserver(this);
        List<Edge> newEdges = new ArrayList<>(graph.getEdges());
        for (Edge e : newEdges)    e.addObserver(this);
        this.edges = newEdges;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Observer<Edge>
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void update(Edge edge) {
        if (mapViewer != null)
            SwingUtilities.invokeLater(mapViewer::repaint);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAINT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void paint(Graphics2D g2, JXMapViewer map, int w, int h) {
        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);

        List<Edge> snapshot = edges;

        // Passe 1 : ombres (dessinées avant pour ne pas couvrir les routes)
        for (Edge edge : snapshot) drawShadow(g2, map, edge);

        // Passe 2 : routes colorées
        for (Edge edge : snapshot) drawRoute(g2, map, edge);

        // Passe 3 : annotations (flèches + jauge capacité)
        for (Edge edge : snapshot) drawAnnotations(g2, map, edge);

        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────
    // OMBRE
    // ─────────────────────────────────────────────────────────────────────

    private void drawShadow(Graphics2D g2, JXMapViewer map, Edge edge) {
        List<GeoPosition> wps = edge.getWaypoints();
        if (wps.size() < 2) return;
        try {
            GeneralPath path = buildPath(map, wps, 1.5f, 1.5f); // décalage pixel
            g2.setColor(new Color(0, 0, 0, 40));
            g2.setStroke(new BasicStroke(routeWidth(map) + 3f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // ROUTE
    // ─────────────────────────────────────────────────────────────────────

    private void drawRoute(Graphics2D g2, JXMapViewer map, Edge edge) {
        List<GeoPosition> wps = edge.getWaypoints();
        if (wps.size() < 2) return;
        try {
            EdgeState state = edge.getState();
            float width = routeWidth(map);

            // — Bordure blanche (casing style Google Maps) —
            g2.setColor(new Color(255, 255, 255, 160));
            g2.setStroke(new BasicStroke(width + 3f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(buildPath(map, wps, 0, 0));

            // — Route colorée —
            applyStyle(g2, state, width);
            g2.draw(buildPath(map, wps, 0, 0));

        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // ANNOTATIONS
    // ─────────────────────────────────────────────────────────────────────

    private void drawAnnotations(Graphics2D g2, JXMapViewer map, Edge edge) {
        List<GeoPosition> wps = edge.getWaypoints();
        if (wps.size() < 2) return;
        try {
            Point2D pFirst = map.convertGeoPositionToPoint(wps.get(0));
            Point2D pLast  = map.convertGeoPositionToPoint(wps.get(wps.size() - 1));

            // Flèche directionnelle
            drawArrow(g2, pFirst, pLast, edge.getState().color);

            // Jauge de capacité (visible si zoom assez proche, zoom < 5)
            if (map.getZoom() < 5) {
                drawCapacityBadge(g2, pFirst, pLast, edge);
            }
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private GeneralPath buildPath(JXMapViewer map, List<GeoPosition> wps,
                                   float dx, float dy) {
        GeneralPath path = new GeneralPath();
        boolean first = true;
        for (GeoPosition gp : wps) {
            Point2D p = map.convertGeoPositionToPoint(gp);
            if (first) { path.moveTo(p.getX() + dx, p.getY() + dy); first = false; }
            else         path.lineTo(p.getX() + dx, p.getY() + dy);
        }
        return path;
    }

    /** Épaisseur de route adaptée au niveau de zoom de JXMapViewer. */
    private float routeWidth(JXMapViewer map) {
        int zoom = map.getZoom();
        // JXMapViewer : zoom 1 = max près, zoom 17 = loin
        if (zoom <= 2)  return 7f;
        if (zoom <= 4)  return 5f;
        if (zoom <= 6)  return 3.5f;
        if (zoom <= 9)  return 2.5f;
        return 2f;
    }

    private void applyStyle(Graphics2D g2, EdgeState state, float width) {
        g2.setColor(state.color);
        if (state.dashed) {
            float[] dash = { 10f, 6f };
            g2.setStroke(new BasicStroke(width,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
        } else {
            g2.setStroke(new BasicStroke(width,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
    }

    /** Flèche directionnelle au milieu du segment. */
    private void drawArrow(Graphics2D g2, Point2D from, Point2D to, Color color) {
        double dx  = to.getX() - from.getX();
        double dy  = to.getY() - from.getY();
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 40) return;

        double mx = (from.getX() + to.getX()) / 2.0;
        double my = (from.getY() + to.getY()) / 2.0;
        double ux = dx / len, uy = dy / len;
        double as = 7.0;

        int[] xs = {
            (int)(mx + ux * as),
            (int)(mx - ux * as - uy * as * 0.55),
            (int)(mx - ux * as + uy * as * 0.55)
        };
        int[] ys = {
            (int)(my + uy * as),
            (int)(my - uy * as + ux * as * 0.55),
            (int)(my - uy * as - ux * as * 0.55)
        };

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
        g2.fillPolygon(xs, ys, 3);
    }

    /**
     * Badge de capacité : petite barre de progression colorée près du milieu.
     * Rouge si surchargé, orange si congestionné, vert sinon.
     */
    private void drawCapacityBadge(Graphics2D g2, Point2D from, Point2D to, Edge edge) {
        double mx = (from.getX() + to.getX()) / 2.0 + 10;
        double my = (from.getY() + to.getY()) / 2.0 - 14;

        int badgeW = 36, badgeH = 7;
        double ratio = Math.min(edge.getFlowRatio(), 1.0);

        // Fond
        g2.setColor(new Color(30, 30, 30, 160));
        g2.fillRoundRect((int)mx, (int)my, badgeW, badgeH, 4, 4);

        // Remplissage
        Color fill = ratio > 1.0 ? new Color(0xB71C1C)
                   : ratio > 0.8 ? new Color(0xE53935)
                   : ratio > 0.5 ? new Color(0xFB8C00)
                   :               new Color(0x2E7D32);
        g2.setColor(fill);
        g2.fillRoundRect((int)mx + 1, (int)my + 1,
                         (int)((badgeW - 2) * ratio), badgeH - 2, 3, 3);

        // Texte flux/capacité
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        String label = edge.getCurrentFlow() + "/" + edge.getCapacityMax();
        g2.drawString(label, (int)mx + badgeW + 3, (int)my + badgeH - 1);
    }
}