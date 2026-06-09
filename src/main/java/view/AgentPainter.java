package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import model.agent.AdminAgent;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.enums.CitizenState;
import model.graph.AgentMovement;
import model.graph.Node;
import model.graph.RouteGraph;
import model.zone.Zone;

/**
 * Dessine les agents sous forme de petits personnages type Mii sur la carte JXMapViewer.
 *
 * Ce painter affiche :
 * - les agents statiques chargés depuis users.json ;
 * - les agents en déplacement depuis RouteGraph ;
 * - le drag & drop ;
 * - l'état paniqué/stressé quand on prend un agent ou quand il est proche d'une zone inondée.
 */
public class AgentPainter implements Painter<JXMapViewer> {

    private RouteGraph routeGraph;
    private List<Agent> agents = new ArrayList<>();
    private List<Zone> zones = new ArrayList<>();

    private Agent draggedAgent;
    private Agent hoveredAgent;

    public void setRouteGraph(RouteGraph routeGraph) {
        this.routeGraph = routeGraph;
    }

    public void setAgents(List<Agent> agents, List<Zone> zones) {
        this.agents = agents == null ? new ArrayList<>() : new ArrayList<>(agents);
        this.zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
        ensurePositions();
    }

    public void updateZones(List<Zone> zones) {
        this.zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
    }

    public Agent getDraggedAgent() {
        return draggedAgent;
    }

    public Agent getHoveredAgent() {
        return hoveredAgent;
    }

    public void setHoveredAgent(Agent hoveredAgent) {
        this.hoveredAgent = hoveredAgent;
    }

    public void startDrag(Agent agent) {
        this.draggedAgent = agent;
        setPanic(agent, true);
    }

    public void dragTo(Agent agent, GeoPosition position) {
        if (agent == null || position == null) return;
        agent.setPosition(new Node(position.getLatitude(), position.getLongitude()));
        setPanic(agent, true);
    }

    public void endDrag() {
        if (draggedAgent != null) {
            boolean danger = isDangerAt(draggedAgent.getPosition() != null
                    ? draggedAgent.getPosition().getLat()
                    : 0.0,
                    draggedAgent.getPosition() != null
                    ? draggedAgent.getPosition().getLng()
                    : 0.0);

            if (!danger) {
                setPanic(draggedAgent, false);
            }
        }
        draggedAgent = null;
    }

    public Agent findAgentAt(JXMapViewer map, Point screenPoint) {
        if (map == null || screenPoint == null) return null;

        Agent best = null;
        double bestDist = 99999;

        for (Agent agent : agents) {
            GeoPosition gp = positionOf(agent);
            if (gp == null) continue;

            Point2D p = map.convertGeoPositionToPoint(gp);
            double dist = p.distance(screenPoint);

            if (dist < 24 && dist < bestDist) {
                bestDist = dist;
                best = agent;
            }
        }

        if (routeGraph != null) {
            for (AgentMovement mv : routeGraph.getActiveMovements()) {
                if (mv == null || mv.getAgent() == null || mv.getCurrentPosition() == null) continue;

                Point2D p = map.convertGeoPositionToPoint(mv.getCurrentPosition());
                double dist = p.distance(screenPoint);

                if (dist < 24 && dist < bestDist) {
                    bestDist = dist;
                    best = mv.getAgent();
                }
            }
        }

        return best;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        if (map == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Set<Integer> movingAgentIds = new HashSet<>();

        if (routeGraph != null) {
            for (AgentMovement mv : new ArrayList<>(routeGraph.getActiveMovements())) {
                if (mv == null || mv.getAgent() == null || mv.getCurrentPosition() == null) continue;

                Agent agent = mv.getAgent();
                movingAgentIds.add(agent.getId());

                boolean panicking = isPanicking(agent) || mv.isBlocked();
                if (isDangerAt(mv.getCurrentPosition().getLatitude(), mv.getCurrentPosition().getLongitude())) {
                    setPanic(agent, true);
                    panicking = true;
                }

                Point2D p = map.convertGeoPositionToPoint(mv.getCurrentPosition());
                drawMiiAgent(g2, agent, p.getX(), p.getY(), colorFor(agent, panicking), panicking);
                drawTooltipIfNeeded(g2, agent, p.getX(), p.getY());
            }
        }

        for (Agent agent : agents) {
            if (movingAgentIds.contains(agent.getId())) continue;

            GeoPosition gp = positionOf(agent);
            if (gp == null) continue;

            boolean panicking = isPanicking(agent) || agent == draggedAgent;
            if (isDangerAt(gp.getLatitude(), gp.getLongitude()) && agent instanceof Citizen) {
                setPanic(agent, true);
                panicking = true;
            }

            Point2D p = map.convertGeoPositionToPoint(gp);
            drawMiiAgent(g2, agent, p.getX(), p.getY(), colorFor(agent, panicking), panicking);
            drawTooltipIfNeeded(g2, agent, p.getX(), p.getY());
        }

        g2.dispose();
    }

    private void ensurePositions() {
        if (agents == null || agents.isEmpty()) return;

        Random rng = new Random(123);
        for (Agent agent : agents) {
            if (agent.getPosition() != null) continue;

            if (!zones.isEmpty()) {
                Zone z = zones.get(Math.floorMod(agent.getId(), zones.size()));
                double lat = z.getLatitude() + (rng.nextDouble() - 0.5) * 0.006;
                double lng = z.getLongitude() + (rng.nextDouble() - 0.5) * 0.006;
                agent.setPosition(new Node(lat, lng));
            }
        }
    }

    private GeoPosition positionOf(Agent agent) {
        if (agent == null || agent.getPosition() == null) return null;
        return new GeoPosition(agent.getPosition().getLat(), agent.getPosition().getLng());
    }

    private boolean isDangerAt(double lat, double lng) {
        Zone nearest = nearestZone(lat, lng);
        return nearest != null && nearest.isFlooded();
    }

    private Zone nearestZone(double lat, double lng) {
        Zone best = null;
        double bestDist = Double.MAX_VALUE;

        for (Zone z : zones) {
            double dLat = z.getLatitude() - lat;
            double dLng = z.getLongitude() - lng;
            double dist = dLat * dLat + dLng * dLng;

            if (dist < bestDist) {
                bestDist = dist;
                best = z;
            }
        }

        return best;
    }

    private void setPanic(Agent agent, boolean panic) {
        if (!(agent instanceof Citizen c)) return;

        if (panic) {
            if (c.getState() != CitizenState.INJURED
                    && c.getState() != CitizenState.PMR
                    && c.getState() != CitizenState.SAFE
                    && c.getState() != CitizenState.ESCAPING) {
                c.setState(CitizenState.STRESSED);
            }
        } else {
            if (c.getState() == CitizenState.STRESSED) {
                c.setState(CitizenState.CALM);
            }
        }
    }

    private boolean isPanicking(Agent agent) {
        if (!(agent instanceof Citizen c) || c.getState() == null) return false;
        return c.getState() == CitizenState.STRESSED || c.getState() == CitizenState.ESCAPING;
    }

    private Color colorFor(Agent agent, boolean panicking) {
        if (agent instanceof RescueAgent) return new Color(67, 160, 71);
        if (agent instanceof AdminAgent) return new Color(38, 198, 218);
        if (panicking) return new Color(229, 57, 53);

        if (agent instanceof Citizen c && c.getState() != null) {
            return switch (c.getState()) {
                case INJURED -> new Color(233, 30, 99);
                case PMR -> new Color(142, 36, 170);
                case STRESSED, ESCAPING -> new Color(229, 57, 53);
                case SAFE -> new Color(34, 197, 94);
                case CALM -> new Color(30, 136, 229);
            };
        }

        return new Color(30, 136, 229);
    }

    private Color skinFor(Agent agent) {
        Color[] skins = {
                new Color(0xF2C6A0),
                new Color(0xE0AC69),
                new Color(0xC68642),
                new Color(0x8D5524),
                new Color(0xFFDBAC)
        };
        return skins[Math.floorMod(agent.getId(), skins.length)];
    }

    private Color hairFor(Agent agent) {
        Color[] hairs = {
                new Color(0x2B1D16),
                new Color(0x111827),
                new Color(0x5C4033),
                new Color(0x8B5A2B),
                new Color(0x3B2F2F)
        };
        return hairs[Math.floorMod(agent.getId() * 7, hairs.length)];
    }

    private void drawMiiAgent(Graphics2D g2, Agent agent, double x, double y, Color mainColor, boolean panicking) {
        long now = System.currentTimeMillis();
        double seed = agent.getId() * 37.0;
        double walk = Math.sin(now * 0.010 + seed) * 2.2;
        double panicShake = panicking ? Math.sin(now * 0.080 + seed) * 3.0 : 0.0;

        x += panicShake;

        Color skin = skinFor(agent);
        Color hair = hairFor(agent);

        if (panicking) {
            double pulse = 0.35 + 0.25 * Math.sin(now * 0.015 + seed);
            g2.setColor(new Color(255, 40, 20, Math.max(35, Math.min(150, (int) (pulse * 255)))));
            g2.fillOval((int) x - 19, (int) y - 27, 38, 47);
        }

        // Ombre
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval((int) x - 11, (int) y + 14, 22, 6);

        // Jambes
        g2.setColor(new Color(17, 24, 39));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) x - 4, (int) y + 9, (int) x - 7, (int) (y + 16 + walk));
        g2.drawLine((int) x + 4, (int) y + 9, (int) x + 7, (int) (y + 16 - walk));

        // Corps
        g2.setColor(mainColor);
        g2.fillRoundRect((int) x - 8, (int) y - 2, 16, 15, 7, 7);

        // Bras
        g2.setColor(mainColor.brighter());
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) x - 8, (int) y + 1, (int) x - 13, (int) (y + 7 - walk));
        g2.drawLine((int) x + 8, (int) y + 1, (int) x + 13, (int) (y + 7 + walk));

        // Tête
        g2.setColor(skin);
        g2.fillOval((int) (x - 8.5), (int) (y - 19), 17, 17);

        // Cheveux
        g2.setColor(hair);
        g2.fillArc((int) (x - 8.5), (int) (y - 19), 17, 10, 0, 180);
        g2.fillOval((int) x - 8, (int) y - 15, 4, 6);
        g2.fillOval((int) x + 4, (int) y - 15, 4, 6);

        // Yeux
        g2.setColor(new Color(17, 24, 39));
        g2.fillOval((int) (x - 4.8), (int) (y - 12), 3, 3);
        g2.fillOval((int) (x + 2.7), (int) (y - 12), 3, 3);

        // Bouche
        g2.setColor(new Color(127, 29, 29));
        g2.setStroke(new BasicStroke(1.2f));
        if (panicking) {
            g2.drawOval((int) x - 2, (int) y - 9, 5, 5);
        } else {
            g2.draw(new Arc2D.Double(x - 3.3, y - 9, 6.6, 4.5, 200, 140, Arc2D.OPEN));
        }

        // Contour tête
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawOval((int) (x - 8.5), (int) (y - 19), 17, 17);

        // Blessé
        if (agent instanceof Citizen c && c.getState() == CitizenState.INJURED) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine((int) x + 13, (int) y - 1, (int) x + 18, (int) y + 17);
            g2.drawLine((int) x + 15, (int) y + 5, (int) x + 21, (int) y + 5);

            g2.setColor(new Color(229, 57, 53));
            g2.fillOval((int) x - 17, (int) y - 20, 11, 11);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
            g2.drawString("+", (int) x - 14, (int) y - 11);
        }

        // PMR
        if (agent instanceof Citizen c && c.getState() == CitizenState.PMR) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine((int) x + 13, (int) y - 2, (int) x + 13, (int) y + 17);
            g2.drawLine((int) x + 13, (int) y + 17, (int) x + 17, (int) y + 17);
        }

        // Secours : casque
        if (agent instanceof RescueAgent) {
            g2.setColor(Color.WHITE);
            g2.fillRoundRect((int) x - 7, (int) y - 23, 14, 5, 2, 2);
            g2.setColor(new Color(229, 57, 53));
            g2.fillRect((int) x - 1, (int) y - 22, 2, 4);
            g2.fillRect((int) x - 3, (int) y - 21, 6, 2);
        }

        // Admin : couronne
        if (agent instanceof AdminAgent) {
            g2.setColor(new Color(38, 198, 218));
            int[] xs = {(int) x, (int) x + 7, (int) x + 5, (int) x, (int) x - 5, (int) x - 7};
            int[] ys = {(int) y - 24, (int) y - 20, (int) y - 12, (int) y - 9, (int) y - 12, (int) y - 20};
            g2.fillPolygon(xs, ys, 6);
        }

        // Pictogramme panique
        if (panicking) {
            g2.setColor(new Color(229, 57, 53));
            g2.fillOval((int) x + 7, (int) y - 25, 13, 13);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString("!", (int) x + 11, (int) y - 15);
        }
    }

    private void drawTooltipIfNeeded(Graphics2D g2, Agent agent, double x, double y) {
        if (agent != hoveredAgent && agent != draggedAgent) return;

        String state = stateLabel(agent);
        String text = safeName(agent) + " • " + state;
        int width = Math.max(70, text.length() * 6 + 12);

        g2.setColor(new Color(19, 35, 55, 245));
        g2.fillRoundRect((int) x - width / 2, (int) y - 50, width, 23, 8, 8);
        g2.setColor(new Color(30, 58, 82));
        g2.drawRoundRect((int) x - width / 2, (int) y - 50, width, 23, 8, 8);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString(text, (int) x - width / 2 + 6, (int) y - 34);
    }

    private String safeName(Agent agent) {
        String first = agent.getFirstName() == null ? "" : agent.getFirstName();
        String last = agent.getLastName() == null ? "" : agent.getLastName();
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Agent #" + agent.getId() : full;
    }

    private String stateLabel(Agent agent) {
        if (agent instanceof Citizen c && c.getState() != null) {
            return c.getState().name();
        }
        if (agent instanceof RescueAgent r && r.getState() != null) {
            return r.getState().name();
        }
        if (agent instanceof AdminAgent) return "ADMIN";
        return "NORMAL";
    }
}
