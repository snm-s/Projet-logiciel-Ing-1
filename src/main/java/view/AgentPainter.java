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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import model.graph.Edge;
import model.graph.Node;
import model.graph.RouteGraph;
import model.zone.Zone;

/**
 * Dessine les agents en petits personnages type Mii directement SUR le graphe routier.
 * Les agents statiques sont automatiquement aimantés aux arêtes du RouteGraph.
 * Drag & drop : quand on attrape un citoyen, il devient STRESSED, puis redevient CALM
 * au relâchement si la zone n'est pas inondée.
 */
public class AgentPainter implements Painter<JXMapViewer> {

    private RouteGraph routeGraph;
    private List<Agent> agents = new ArrayList<>();
    private List<Zone> zones = new ArrayList<>();

    private final Map<Integer, Edge> edgeByAgent = new HashMap<>();
    private final Map<Integer, Double> progressByAgent = new HashMap<>();
    private final Map<Integer, Integer> waitCyclesByAgent = new HashMap<>();

    private Agent draggedAgent;
    private Agent hoveredAgent;
    private boolean animateStaticAgents = false;
    private Integer currentUserId;

    public void setRouteGraph(RouteGraph routeGraph) {
        this.routeGraph = routeGraph;
    }

    public void setAgents(List<Agent> agents, List<Zone> zones) {
        this.agents = agents == null ? new ArrayList<>() : new ArrayList<>(agents);
        this.zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
    
        Set<Integer> ids = new HashSet<>();
        for (Agent a : this.agents) ids.add(a.getId());
    
        edgeByAgent.keySet().removeIf(id -> !ids.contains(id));
        progressByAgent.keySet().removeIf(id -> !ids.contains(id));
        waitCyclesByAgent.keySet().removeIf(id -> !ids.contains(id));
    
        ensurePositions();
    }

    public List<Agent> getAgents() {
        return agents;
    }

    private boolean isDisplayedAgent(Agent agent) {

        if (agent == null) return false;
    
        return agents.stream()
                .anyMatch(a -> a.getId() == agent.getId());
    }

    public void updateZones(List<Zone> zones) {
        this.zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
    }

    public void setAnimateStaticAgents(boolean animateStaticAgents) {
        this.animateStaticAgents = animateStaticAgents;
    }

    public void setCurrentUserId(Integer id) {
            this.currentUserId = id;
     }

    public Agent getDraggedAgent() { return draggedAgent; }
    public Agent getHoveredAgent() { return hoveredAgent; }
    public void setHoveredAgent(Agent hoveredAgent) { this.hoveredAgent = hoveredAgent; }

    public void startDrag(Agent agent) {
        this.draggedAgent = agent;
        setPanic(agent, true);
    }

    public void dragTo(Agent agent, GeoPosition position) {
        if (agent == null || position == null) return;
        edgeByAgent.remove(agent.getId());
        progressByAgent.remove(agent.getId());
        agent.setPosition(new Node(position.getLatitude(), position.getLongitude()));
        setPanic(agent, true);
    }

    public void endDrag() {
        if (draggedAgent != null) {
            boolean danger = isDangerAt(positionOf(draggedAgent).getLatitude(), positionOf(draggedAgent).getLongitude());
            if (!danger) setPanic(draggedAgent, false);
        }
        draggedAgent = null;
    }

    public void snapAgentsToGraph() {
        if (routeGraph == null || agents == null) return;
        for (Agent a : agents) snapAgentToNearestGraphElement(a);
    }

    public void snapAgentToNearestGraphElement(Agent agent) {
        if (agent == null || routeGraph == null || routeGraph.getEdges().isEmpty()) return;

        GeoPosition p = positionOf(agent);
        Edge bestEdge = null;
        double bestDist = Double.MAX_VALUE;
        double bestProgress = 0.0;

        for (Edge e : routeGraph.getEdges()) {
            List<GeoPosition> pts = cleanWaypoints(e);
            for (int i = 0; i < pts.size() - 1; i++) {
                Projection pr = projectOnSegment(p, pts.get(i), pts.get(i + 1));
                if (pr.distance < bestDist) {
                    bestDist = pr.distance;
                    bestEdge = e;
                    bestProgress = segmentProgress(pts, i, pr.t);
                }
            }
        }

        if (bestEdge == null) return;
        edgeByAgent.put(agent.getId(), bestEdge);
        progressByAgent.put(agent.getId(), clamp(bestProgress, 0.02, 0.98));
        GeoPosition snapped = positionOnEdge(bestEdge, progressByAgent.get(agent.getId()));
        if (snapped != null) agent.setPosition(new Node(snapped.getLatitude(), snapped.getLongitude()));
    }

    public void moveAgentToNearestNode(Agent agent) {
        if (agent == null || routeGraph == null || routeGraph.getEdges().isEmpty()) return;
        GeoPosition p = positionOf(agent);
        Zone closest = null;
        double best = Double.MAX_VALUE;
        for (Edge e : routeGraph.getEdges()) {
            for (Zone z : List.of(e.getFromZone(), e.getToZone())) {
                double d = geoDist(p, new GeoPosition(z.getLatitude(), z.getLongitude()));
                if (d < best) { best = d; closest = z; }
            }
        }
        if (closest != null) {
            agent.setPosition(new Node(closest.getLatitude(), closest.getLongitude()));
            edgeByAgent.remove(agent.getId());
            progressByAgent.remove(agent.getId());
        }
    }

    public Agent findAgentAt(JXMapViewer map, Point screenPoint) {
        if (map == null || screenPoint == null) return null;
        Agent best = null;
        double bestDist = 99999;

        for (Agent agent : agents) {
            GeoPosition gp = renderedPositionOf(agent);
            if (gp == null) continue;
            Point2D p = map.convertGeoPositionToPoint(gp);
            double dist = p.distance(screenPoint);
            if (dist < 24 && dist < bestDist) { bestDist = dist; best = agent; }
        }

        if (routeGraph != null) {
            for (AgentMovement mv : routeGraph.getActiveMovements()) {
                if (mv == null || mv.getAgent() == null || mv.getCurrentPosition() == null) continue;
        
                Agent movingAgent = mv.getAgent();
                if (!isDisplayedAgent(movingAgent)) continue;
        
                Point2D p = map.convertGeoPositionToPoint(mv.getCurrentPosition());
                double dist = p.distance(screenPoint);
        
                if (dist < 24 && dist < bestDist) {
                    bestDist = dist;
                    best = movingAgent;
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
            if (!isDisplayedAgent(agent)) continue;

            movingAgentIds.add(agent.getId());

            Edge currentEdge = mv.getCurrentEdge();
            boolean edgeRisk = isEdgeRiskForPanic(currentEdge);

            boolean isSafe = agent instanceof Citizen c && c.getState() == CitizenState.SAFE;

            if (!isSafe) {
                setPanic(agent, edgeRisk || mv.isBlocked());
            }

            boolean panicking = !isSafe && (isPanicking(agent) || mv.isBlocked());

            Point2D p = map.convertGeoPositionToPoint(mv.getCurrentPosition());
            drawMiiAgent(g2, agent, p.getX(), p.getY(), colorFor(agent, panicking), panicking);
            drawTooltipIfNeeded(g2, agent, p.getX(), p.getY());
        }
    }

    for (Agent agent : agents) {
        if (movingAgentIds.contains(agent.getId())) continue;

        if (agent == draggedAgent) {
            // position mise à jour directement par le drag
        } else if (animateStaticAgents) {
            advanceAgentOnEdge(agent);
        }

        GeoPosition gp = renderedPositionOf(agent);
        if (gp == null) continue;

        boolean isSafe = agent instanceof Citizen c && c.getState() == CitizenState.SAFE;
        boolean panicking = !isSafe && (isPanicking(agent) || agent == draggedAgent);

        if (agent instanceof Citizen) {
            Edge snappedEdge = edgeByAgent.get(agent.getId());
            boolean edgeRisk = isEdgeRiskForPanic(snappedEdge);

            if (agent != draggedAgent && !isSafe) {
                setPanic(agent, edgeRisk);
            }

            panicking = !isSafe && (panicking || edgeRisk);
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
                double lat = z.getLatitude() + (rng.nextDouble() - 0.5) * 0.004;
                double lng = z.getLongitude() + (rng.nextDouble() - 0.5) * 0.004;
                agent.setPosition(new Node(lat, lng));
            }
        }
    }

    private void advanceAgentOnEdge(Agent agent) {
        if (agent == null || routeGraph == null || routeGraph.getEdges().isEmpty()) return;
        if (!edgeByAgent.containsKey(agent.getId())) snapAgentToNearestGraphElement(agent);
        Edge e = edgeByAgent.get(agent.getId());
        if (e == null) return;

        if (e.getCurrentFlow() > e.getCapacityMax()) {
            int left = waitCyclesByAgent.getOrDefault(agent.getId(), 2);
            if (left > 0) { waitCyclesByAgent.put(agent.getId(), left - 1); return; }
        } else {
            waitCyclesByAgent.remove(agent.getId());
        }

        double p = progressByAgent.getOrDefault(agent.getId(), 0.05);
        double speed = 0.0009 * Math.max(0.6, agent.getMaxSpeed());
        if (isPanicking(agent)) speed *= 1.35;
        p += speed;
        if (p >= 1.0) {
            Zone next = e.getToZone();
            Edge nextEdge = chooseNextEdge(next, e);
            if (nextEdge == null) {
                p = 0.02;
            } else {
                e = nextEdge;
                edgeByAgent.put(agent.getId(), e);
                p = 0.02;
            }
        }
        progressByAgent.put(agent.getId(), p);
        GeoPosition gp = positionOnEdge(e, p);
        if (gp != null) agent.setPosition(new Node(gp.getLatitude(), gp.getLongitude()));
    }

    private Edge chooseNextEdge(Zone from, Edge previous) {
        if (routeGraph == null || from == null) return null;
        List<Edge> candidates = new ArrayList<>();
        for (Edge e : routeGraph.getEdges()) {
            if (e == previous) continue;
            if (e.getFromZone().getId() == from.getId() && e.isCrossable()) candidates.add(e);
            else if (e.getToZone().getId() == from.getId() && e.isCrossable()) candidates.add(e);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort((a, b) -> Integer.compare(a.getCurrentFlow(), b.getCurrentFlow()));
        return candidates.get(Math.floorMod((int) System.nanoTime(), candidates.size()));
    }

    private GeoPosition renderedPositionOf(Agent agent) {
        return positionOf(agent);
    }

    private GeoPosition positionOf(Agent agent) {
        if (agent == null || agent.getPosition() == null) {
            if (!zones.isEmpty()) {
                Zone z = zones.get(Math.floorMod(agent == null ? 0 : agent.getId(), zones.size()));
                return new GeoPosition(z.getLatitude(), z.getLongitude());
            }
            return null;
        }
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
            if (dist < bestDist) { bestDist = dist; best = z; }
        }
        return best;
    }

    private void setPanic(Agent agent, boolean panic) {
        if (!(agent instanceof Citizen c)) return;
        if (panic) {
            if (c.getState() != CitizenState.INJURED && c.getState() != CitizenState.PMR && c.getState() != CitizenState.SAFE) {
                c.setState(CitizenState.STRESSED);
            }
        } else if (c.getState() == CitizenState.STRESSED) {
            c.setState(CitizenState.CALM);
        }
    }

    private boolean isEdgeRiskForPanic(Edge edge) {
        if (edge == null) return false;
        return switch (edge.getState()) {
            case AT_RISK, FLOODING, OVERLOADED, FLOODED -> true;
            default -> false;
        };
    }

private boolean isPanicking(Agent agent) {
    if (!(agent instanceof Citizen c) || c.getState() == null) {
        return false;
    }

    if (c.getState() == CitizenState.SAFE) {
        return false;
    }

    return c.getState() == CitizenState.STRESSED;
}

    private Color colorFor(Agent agent, boolean panicking) {
        if (currentUserId != null && agent.getId() == currentUserId && agent instanceof RescueAgent) {
            return new Color(255, 215, 0); // Jaune Or pour se repérer
        }

        if (agent instanceof RescueAgent) return new Color(67, 160, 71);
        if (agent instanceof AdminAgent) return new Color(38, 198, 218);
        if (panicking) return new Color(229, 57, 53);
        if (agent instanceof Citizen c && c.getState() != null) {
            return switch (c.getState()) {
                case INJURED -> new Color(233, 30, 99);
                case PMR -> new Color(142, 36, 170);
                case STRESSED -> new Color(229, 57, 53);
                case ESCAPING -> new Color(30, 136, 229);
                case SAFE -> new Color(34, 197, 94);
                case CALM -> new Color(30, 136, 229);
            };
        }
        return new Color(30, 136, 229);
    }

    private Color skinFor(Agent agent) {
        Color[] skins = { new Color(0xF2C6A0), new Color(0xE0AC69), new Color(0xC68642), new Color(0x8D5524), new Color(0xFFDBAC) };
        return skins[Math.floorMod(agent.getId(), skins.length)];
    }

    private Color hairFor(Agent agent) {
        Color[] hairs = { new Color(0x2B1D16), new Color(0x111827), new Color(0x5C4033), new Color(0x8B5A2B), new Color(0x3B2F2F) };
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

        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval((int) x - 11, (int) y + 14, 22, 6);
        g2.setColor(new Color(17, 24, 39));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) x - 4, (int) y + 9, (int) x - 7, (int) (y + 16 + walk));
        g2.drawLine((int) x + 4, (int) y + 9, (int) x + 7, (int) (y + 16 - walk));
        g2.setColor(mainColor);
        g2.fillRoundRect((int) x - 8, (int) y - 2, 16, 15, 7, 7);
        g2.setColor(mainColor.brighter());
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) x - 8, (int) y + 1, (int) x - 13, (int) (y + 7 - walk));
        g2.drawLine((int) x + 8, (int) y + 1, (int) x + 13, (int) (y + 7 + walk));
        g2.setColor(skin);
        g2.fillOval((int) (x - 8.5), (int) (y - 19), 17, 17);
        g2.setColor(hair);
        g2.fillArc((int) (x - 8.5), (int) (y - 19), 17, 10, 0, 180);
        g2.fillOval((int) x - 8, (int) y - 15, 4, 6);
        g2.fillOval((int) x + 4, (int) y - 15, 4, 6);
        g2.setColor(new Color(17, 24, 39));
        g2.fillOval((int) (x - 4.8), (int) (y - 12), 3, 3);
        g2.fillOval((int) (x + 2.7), (int) (y - 12), 3, 3);
        g2.setColor(new Color(127, 29, 29));
        g2.setStroke(new BasicStroke(1.2f));
        if (panicking) g2.drawOval((int) x - 2, (int) y - 9, 5, 5);
        else g2.draw(new Arc2D.Double(x - 3.3, y - 9, 6.6, 4.5, 200, 140, Arc2D.OPEN));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawOval((int) (x - 8.5), (int) (y - 19), 17, 17);

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
        if (agent instanceof Citizen c && c.getState() == CitizenState.PMR) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine((int) x + 13, (int) y - 2, (int) x + 13, (int) y + 17);
            g2.drawLine((int) x + 13, (int) y + 17, (int) x + 17, (int) y + 17);
        }
        if (agent instanceof RescueAgent) {
            g2.setColor(Color.WHITE);
            g2.fillRoundRect((int) x - 7, (int) y - 23, 14, 5, 2, 2);
            g2.setColor(new Color(229, 57, 53));
            g2.fillRect((int) x - 1, (int) y - 22, 2, 4);
            g2.fillRect((int) x - 3, (int) y - 21, 6, 2);
        }
        if (agent instanceof AdminAgent) {
            g2.setColor(new Color(38, 198, 218));
            int[] xs = {(int) x, (int) x + 7, (int) x + 5, (int) x, (int) x - 5, (int) x - 7};
            int[] ys = {(int) y - 24, (int) y - 20, (int) y - 12, (int) y - 9, (int) y - 12, (int) y - 20};
            g2.fillPolygon(xs, ys, 6);
        }
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
        String text = safeName(agent) + " • " + stateLabel(agent);
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
        if (agent instanceof Citizen c && c.getState() != null) return c.getState().name();
        if (agent instanceof RescueAgent r && r.getState() != null) return r.getState().name();
        if (agent instanceof AdminAgent) return "ADMIN";
        return "NORMAL";
    }

    private List<GeoPosition> cleanWaypoints(Edge e) {
        List<GeoPosition> pts = e == null ? List.of() : e.getWaypoints();
        if (pts == null || pts.size() < 2) {
            return List.of(new GeoPosition(e.getFromZone().getLatitude(), e.getFromZone().getLongitude()),
                    new GeoPosition(e.getToZone().getLatitude(), e.getToZone().getLongitude()));
        }
        return pts;
    }

    private GeoPosition positionOnEdge(Edge edge, double progress) {
        if (edge == null) return null;
        List<GeoPosition> pts = cleanWaypoints(edge);
        if (pts.size() < 2) return null;
        double total = totalLength(pts);
        if (total <= 0) return pts.get(0);
        double target = clamp(progress, 0, 1) * total;
        double acc = 0;
        for (int i = 0; i < pts.size() - 1; i++) {
            double len = geoDist(pts.get(i), pts.get(i + 1));
            if (acc + len >= target) {
                double t = len <= 0 ? 0 : (target - acc) / len;
                return lerp(pts.get(i), pts.get(i + 1), t);
            }
            acc += len;
        }
        return pts.get(pts.size() - 1);
    }

    private double segmentProgress(List<GeoPosition> pts, int index, double t) {
        double total = totalLength(pts);
        if (total <= 0) return 0;
        double acc = 0;
        for (int i = 0; i < index && i < pts.size() - 1; i++) acc += geoDist(pts.get(i), pts.get(i + 1));
        acc += geoDist(pts.get(index), pts.get(index + 1)) * t;
        return acc / total;
    }

    private double totalLength(List<GeoPosition> pts) {
        double total = 0;
        for (int i = 0; i < pts.size() - 1; i++) total += geoDist(pts.get(i), pts.get(i + 1));
        return total;
    }

    private Projection projectOnSegment(GeoPosition p, GeoPosition a, GeoPosition b) {
        double ax = a.getLongitude(), ay = a.getLatitude();
        double bx = b.getLongitude(), by = b.getLatitude();
        double px = p.getLongitude(), py = p.getLatitude();
        double dx = bx - ax, dy = by - ay;
        double den = dx * dx + dy * dy;
        double t = den <= 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / den;
        t = clamp(t, 0, 1);
        GeoPosition projected = new GeoPosition(ay + dy * t, ax + dx * t);
        return new Projection(t, geoDist(p, projected));
    }

    private GeoPosition lerp(GeoPosition a, GeoPosition b, double t) {
        return new GeoPosition(a.getLatitude() + (b.getLatitude() - a.getLatitude()) * t,
                a.getLongitude() + (b.getLongitude() - a.getLongitude()) * t);
    }

    private double geoDist(GeoPosition a, GeoPosition b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        double dLat = a.getLatitude() - b.getLatitude();
        double dLng = a.getLongitude() - b.getLongitude();
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    private record Projection(double t, double distance) {}
}