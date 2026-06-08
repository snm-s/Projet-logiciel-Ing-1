package view;

import model.graph.AgentMovement;
import model.graph.RouteGraph;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Painter JXMapViewer pour afficher les agents en déplacement sur la carte.
 *
 * <ul>
 *   <li>Citoyen normal   : point blanc 8px</li>
 *   <li>Citoyen elderly  : point jaune 9px</li>
 *   <li>RescueAgent      : point rouge 11px avec halo</li>
 * </ul>
 *
 * Lit directement les {@link AgentMovement} actifs depuis le {@link RouteGraph}.
 */
public class AgentPainter implements Painter<JXMapViewer> {

    private RouteGraph routeGraph;

    // ─── Couleurs ──────────────────────────────────────────────────────────
    private static final Color COLOR_CITIZEN         = new Color(241, 245, 249, 220);
    private static final Color COLOR_CITIZEN_ELDERLY = new Color(250, 204, 21,  220);
    private static final Color COLOR_RESCUE          = new Color(239, 68,  68,  240);
    private static final Color COLOR_RESCUE_HALO     = new Color(239, 68,  68,  70);
    private static final Color COLOR_OUTLINE         = new Color(0,   0,   0,   140);
    private static final Color LABEL_BG              = new Color(0,   0,   0,   160);
    private static final Color LABEL_FG              = new Color(241, 245, 249);

    // ─────────────────────────────────────────────────────────────────────

    public AgentPainter() {}

    public void setRouteGraph(RouteGraph rg) {
        this.routeGraph = rg;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAINT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void paint(Graphics2D g2, JXMapViewer map, int w, int h) {
        if (routeGraph == null) return;

        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle rect = map.getViewportBounds();
        g2.translate(-rect.x, -rect.y);

        List<AgentMovement> snapshot = new ArrayList<>(routeGraph.getActiveMovements());

        for (AgentMovement mv : snapshot) {
            if (!mv.isMoving() && !mv.isBlocked()) continue;
            drawAgent(g2, map, mv);
        }

        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────
    // DESSIN D'UN AGENT
    // ─────────────────────────────────────────────────────────────────────

    private void drawAgent(Graphics2D g2, JXMapViewer map, AgentMovement mv) {
        try {
            GeoPosition geoPos = mv.getCurrentPosition();
            Point2D pt = map.convertGeoPositionToPoint(geoPos);
            Agent agent = mv.getAgent();

            double px = pt.getX();
            double py = pt.getY();

            if (agent instanceof RescueAgent) {
                drawRescueAgent(g2, px, py, (RescueAgent) agent);
            } else if (agent instanceof Citizen) {
                drawCitizen(g2, px, py, (Citizen) agent);
            } else {
                drawGenericAgent(g2, px, py);
            }

        } catch (Exception ignored) {}
    }

    // ─── Citoyen ──────────────────────────────────────────────────────────

    private void drawCitizen(Graphics2D g2, double px, double py, Citizen c) {
        String mob  = c.getMobilityStatus();
        Color  col  = "elderly".equals(mob) ? COLOR_CITIZEN_ELDERLY : COLOR_CITIZEN;
        int    r    = "elderly".equals(mob) ? 5 : 4;

        // Contour
        g2.setColor(COLOR_OUTLINE);
        g2.fill(new Ellipse2D.Double(px - r - 1, py - r - 1, (r + 1) * 2, (r + 1) * 2));
        // Point
        g2.setColor(col);
        g2.fill(new Ellipse2D.Double(px - r, py - r, r * 2, r * 2));
    }

    // ─── Secouriste ───────────────────────────────────────────────────────

    private void drawRescueAgent(Graphics2D g2, double px, double py, RescueAgent ra) {
        // Halo animé (halo fixe pour éviter les re-renders lourds)
        g2.setColor(COLOR_RESCUE_HALO);
        g2.fill(new Ellipse2D.Double(px - 10, py - 10, 20, 20));

        // Contour
        g2.setColor(COLOR_OUTLINE);
        g2.fill(new Ellipse2D.Double(px - 7, py - 7, 14, 14));

        // Point rouge
        g2.setColor(COLOR_RESCUE);
        g2.fill(new Ellipse2D.Double(px - 6, py - 6, 12, 12));

        // Mini-label : 1ère lettre du type
        if (ra.getTeamType() != null && !ra.getTeamType().isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 8));
            g2.setColor(Color.WHITE);
            g2.drawString(ra.getTeamType().substring(0, 1).toUpperCase(),
                (int) px - 3, (int) py + 3);
        }
    }

    // ─── Agent générique ──────────────────────────────────────────────────

    private void drawGenericAgent(Graphics2D g2, double px, double py) {
        g2.setColor(COLOR_OUTLINE);
        g2.fill(new Ellipse2D.Double(px - 5, py - 5, 10, 10));
        g2.setColor(new Color(148, 163, 184, 200));
        g2.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));
    }
}