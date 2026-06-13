package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import model.zone.Shelter;
import model.zone.Zone;

/**
 * Painter JXMapViewer pour afficher les zones comme des polygones colorés
 * selon leur état (normale, inondée, évacuée) et leur niveau d'eau.
 *
 * Chaque zone est dessinée comme un hexagone irrégulier centré sur ses coordonnées,
 * avec un rayon simulant sa surface réelle (~500 m).
 */
public class ZonePainter implements Painter<JXMapViewer> {

    private List<Zone>             zones      = new ArrayList<>();
    private Zone                   selected   = null;
    private final Map<Integer, Double> waterLevels = new ConcurrentHashMap<>();

    // Rayon visuel en degrés (~500 m à la latitude de Lyon)
    private static final double RADIUS_LAT = 0.0045;
    private static final double RADIUS_LNG = 0.0065;

    // Nombre de sommets du polygone
    private static final int POLYGON_SIDES = 8;

    // ─────────────────────────────────────────────────────────────────────
    // Palette de couleurs (thème dark)
    // ─────────────────────────────────────────────────────────────────────
    private static final Color COLOR_SAFE       = new Color(34,  197, 94,  90);   // vert translucide
    private static final Color COLOR_SAFE_BORDER= new Color(34,  197, 94,  200);
    private static final Color COLOR_FLOODED_L1 = new Color(191, 219, 254, 110);  // bleu clair (0–0.5m)
    private static final Color COLOR_FLOODED_L2 = new Color(96,  165, 250, 130);  // bleu (0.5–1m)
    private static final Color COLOR_FLOODED_L3 = new Color(59,  130, 246, 150);  // bleu moyen (1–1.5m)
    private static final Color COLOR_FLOODED_L4 = new Color(29,  78,  216, 170);  // bleu foncé (1.5–2m)
    private static final Color COLOR_FLOODED_L5 = new Color(30,  58,  138, 190);  // bleu très foncé (>2m)
    private static final Color COLOR_EVACUATED  = new Color(239, 68,  68,  140);  // rouge
    private static final Color COLOR_EVA_BORDER = new Color(239, 68,  68,  230);
    private static final Color COLOR_SELECTED   = new Color(250, 204, 21,  60);   // jaune sélection
    private static final Color COLOR_SEL_BORDER = new Color(250, 204, 21,  255);
    private static final Color LABEL_BG         = new Color(0,   0,   0,   160);
    private static final Color LABEL_FG         = new Color(241, 245, 249);

    // ─────────────────────────────────────────────────────────────────────

    public ZonePainter(List<Zone> zones) {
        setZones(zones);
    }

    public synchronized void setZones(List<Zone> zones) {
        this.zones = new ArrayList<>(zones);
    }

    public synchronized void setSelectedZone(Zone zone) {
        this.selected = zone;
    }

    public synchronized void updateZone(Zone zone) {
        // L'état est lu directement depuis zone.isFlooded() etc.
        // On remet le niveau à 0 si pas d'info précise
        if (!zone.isFlooded()) waterLevels.remove(zone.getId());
    }

    public synchronized void updateZoneWaterLevel(Zone zone, double niveau) {
        waterLevels.put(zone.getId(), niveau);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAINT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        // 1. Création d'une copie propre
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);



        // 3. Capture thread-safe unique
        List<Zone> snapshot;
        Zone selectedSnapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(zones);
            selectedSnapshot = this.selected; // On capture l'état actuel de la sélection
        }

        // 4. Dessin des zones non sélectionnées
        for (Zone zone : snapshot) {
            if (selectedSnapshot == null || zone.getId() != selectedSnapshot.getId()) {
                drawZone(g2, map, zone, false);
            }
        }

        // 5. Dessin de la sélection (on utilise le snapshot pour être cohérent)
        if (selectedSnapshot != null) {
            snapshot.stream()
                    .filter(z -> z.getId() == selectedSnapshot.getId())
                    .findFirst()
                    .ifPresent(z -> drawZone(g2, map, z, true));
        }

        // 6. Nettoyage
        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────
    // DESSIN D'UNE ZONE
    // ─────────────────────────────────────────────────────────────────────

    private void drawZone(Graphics2D g2, JXMapViewer map, Zone zone, boolean isSelected) {
        try {
            Point2D center = map.convertGeoPositionToPoint(
                new GeoPosition(zone.getLatitude(), zone.getLongitude())
            );
    
            int r = zone instanceof Shelter ? 12 : 10;
    
            // Couleur du point
            if (zone instanceof Shelter) {
                g2.setColor(new Color(250, 204, 21)); // jaune = refuge
            } else {
                g2.setColor(new Color(14, 165, 233)); // bleu = quartier
            }
    
            g2.fillOval(
                (int) center.getX() - r,
                (int) center.getY() - r,
                r * 2,
                r * 2
            );
    
            // Contour noir
            g2.setColor(new Color(15, 23, 42));
            g2.setStroke(new BasicStroke(isSelected ? 4f : 3f));
            g2.drawOval(
                (int) center.getX() - r,
                (int) center.getY() - r,
                r * 2,
                r * 2
            );
    
            drawLabel(g2, map, zone, isSelected);
    
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION DU POLYGONE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Construit un polygone irrégulier centré sur (lat, lng) de la zone.
     * La forme varie selon l'ID pour éviter les polygones identiques.
     */
    private GeneralPath buildPolygon(JXMapViewer map, Zone zone) {
        try {
            Random rng = new Random(zone.getId() * 31337L); // seed fixe par zone

            GeneralPath path = new GeneralPath();
            boolean first = true;

            for (int i = 0; i < POLYGON_SIDES; i++) {
                double angle   = 2.0 * Math.PI * i / POLYGON_SIDES - Math.PI / 2;
                // Rayon légèrement variable pour donner un aspect naturel
                double rLat    = RADIUS_LAT * (0.75 + 0.45 * rng.nextDouble());
                double rLng    = RADIUS_LNG * (0.75 + 0.45 * rng.nextDouble());
                double geoLat  = zone.getLatitude()  + rLat * Math.sin(angle);
                double geoLng  = zone.getLongitude() + rLng * Math.cos(angle);

                Point2D pt = map.convertGeoPositionToPoint(new GeoPosition(geoLat, geoLng));
                if (first) { path.moveTo(pt.getX(), pt.getY()); first = false; }
                else         path.lineTo(pt.getX(), pt.getY());
            }
            path.closePath();
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // COULEURS
    // ─────────────────────────────────────────────────────────────────────

    private Color getFillColor(Zone zone, double niveau) {
        
        if (zone instanceof Shelter) {
            return new Color(168, 85, 247, 150); // Violet translucide pour les refuges
        }
        
        if (!zone.isFlooded()) return COLOR_SAFE;
        if (zone.isEvacuated()) return COLOR_EVACUATED;
        if (niveau > 2.0)   return COLOR_FLOODED_L5;
        if (niveau > 1.5)   return COLOR_FLOODED_L4;
        if (niveau > 1.0)   return COLOR_FLOODED_L3;
        if (niveau > 0.5)   return COLOR_FLOODED_L2;
        return COLOR_FLOODED_L1;
    }

    private Color getBorderColor(Zone zone, double niveau) {
        if (zone.isEvacuated()) return COLOR_EVA_BORDER;
        if (!zone.isFlooded())  return COLOR_SAFE_BORDER;
        // Bordure plus sombre que le fill
        Color fill = getFillColor(zone, niveau);
        return fill.darker();
    }

    // ─────────────────────────────────────────────────────────────────────
    // LABEL
    // ─────────────────────────────────────────────────────────────────────

    private void drawLabel(Graphics2D g2, JXMapViewer map, Zone zone, boolean isSelected) {
        try {
            Point2D center = map.convertGeoPositionToPoint(
                new GeoPosition(zone.getLatitude(), zone.getLongitude()));

            String name    = truncate(zone.getName(), 14);
            Font   font    = new Font("SansSerif", isSelected ? Font.BOLD : Font.PLAIN, isSelected ? 12 : 11);
            g2.setFont(font);

            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(name);
            int th = fm.getHeight();
            int px = (int) center.getX() - tw / 2;
            int py = (int) center.getY() + th / 4;

            // Fond semi-transparent
            g2.setColor(LABEL_BG);
            g2.fillRoundRect(px - 4, py - th + 2, tw + 8, th + 2, 4, 4);

            // Texte
            g2.setColor(isSelected ? new Color(250, 204, 21) : LABEL_FG);
            g2.drawString(name, px, py);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // POPUP D'INFOS (JToolTip natif Swing)
    // ─────────────────────────────────────────────────────────────────────

    public void showPopup(Zone zone, JXMapViewer mapViewer, Point screenPoint) {
        SwingUtilities.invokeLater(() -> {
            double niveau = waterLevels.getOrDefault(zone.getId(), zone.isFlooded() ? 1.0 : 0.0);
            String status = zone.isFlooded()
                ? (zone.isEvacuated() ? "↗ Évacuation" : String.format("⚠ Inondée (%.1f m)", niveau))
                : "✓ Sûre";

            String altInfo = String.format("%.1f m", zone.getAltitude());
            String altWarning = zone.getAltitude() < 1.0 ? " ⚠" : "";

            String text = String.format(
                "<html><div style='font-family:monospace;padding:4px 6px;min-width:180px;'>" +
                "<b style='font-size:13px;'>%s</b><hr style='margin:4px 0;border-color:#1e293b'/>" +
                "<table cellspacing='2'>" +
                "<tr><td style='color:#94a3b8;'>Statut</td><td><b>%s</b></td></tr>" +
                "<tr><td style='color:#94a3b8;'>Altitude</td><td>%s%s</td></tr>" +
                "</table>" +
                "<div style='font-size:10px;color:#94a3b8;margin-top:4px;'>%s</div>" +
                "</div></html>",
                zone.getName(), status, altInfo, altWarning,
                truncate(zone.getDescription(), 60));

            JOptionPane.showMessageDialog(
                mapViewer, text,
                zone.getName(),
                JOptionPane.PLAIN_MESSAGE);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
