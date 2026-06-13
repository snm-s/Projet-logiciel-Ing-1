package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panneau légende à afficher à côté de la carte.
 * Montre la signification des couleurs de zones et routes.
 */
public class MapLegendPanel extends JPanel {

    private static final Color BG     = new Color(19, 27, 46);
    private static final Color TEXT   = new Color(241, 245, 249);
    private static final Color MUTED  = new Color(148, 163, 184);

    /**
     * Constructs a new MapLegendPanel.
     */
    public MapLegendPanel() {
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        build();
    }

    /**
     * Builds.
     */
    private void build() {
        addTitle("Zones");
        addItem(new Color(34, 197, 94, 200),  "Zone sûre");
        addItem(new Color(191, 219, 254, 180),"Inondée < 0.5 m");
        addItem(new Color(59, 130, 246, 200), "Inondée 1–2 m");
        addItem(new Color(30, 58, 138, 220),  "Inondée > 2 m");
        addItem(new Color(239, 68, 68, 200),  "En évacuation");
        addItem(new Color(250, 204, 21, 200), "Sélectionnée");

        add(Box.createVerticalStrut(10));
        addTitle("Routes");
        addRouteItem(new Color(34, 197, 94),  false, "Sûre");
        addRouteItem(new Color(245, 158, 11), true,  "À risque");
        addRouteItem(new Color(234, 179, 8),  false, "Congestionnée"); // Jaune (#eab308)
        addRouteItem(new Color(185, 28, 28),  false, "Surchargée");
        addRouteItem(new Color(239, 68, 68),  true,  "Inondée");
    }

    /**
     * Adds title.
     * @param text the text.
     */
    private void addTitle(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setForeground(MUTED);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        add(lbl);
        add(Box.createVerticalStrut(4));
    }

    /**
     * Adds item.
     * @param color the color.
     * @param label the label.
     */
    private void addItem(Color color, String label) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBackground(BG);
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Carré couleur
        JPanel square = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 14, 14, 4, 4);
                g2.setColor(color.darker());
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, 13, 13, 4, 4);
            }
        };
        square.setPreferredSize(new Dimension(14, 14));
        square.setBackground(BG);

        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));

        row.add(square);
        row.add(lbl);
        add(row);
    }

    /**
     * Adds route item.
     * @param color the color.
     * @param dashed the dashed.
     * @param label the label.
     */
    private void addRouteItem(Color color, boolean dashed, String label) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBackground(BG);
        row.setAlignmentX(LEFT_ALIGNMENT);

        final Color c      = color;
        final boolean dash = dashed;
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                Stroke stroke = dash
                    ? new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{4, 3}, 0)
                    : new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                g2.setStroke(stroke);
                g2.drawLine(0, 7, 24, 7);
            }
        };
        line.setPreferredSize(new Dimension(24, 14));
        line.setBackground(BG);

        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));

        row.add(line);
        row.add(lbl);
        add(row);
    }
}
