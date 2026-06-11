package model.graph;

import model.agent.Agent;
import model.agent.RescueAgent;
import model.algorithms.EvacuationPath;
import model.algorithms.EvacuationRouter;
import model.manager.RouteManager;
import model.zone.Zone;
import model.zone.Shelter;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Façade principale de la couche graphe.
 *
 * <p>Délègue :</p>
 * <ul>
 *   <li>Topologie, connectivité et requêtes structurelles → {@link Graph}</li>
 *   <li>Chargement JSON / tracé OSRM                     → {@link RouteManager}</li>
 *   <li>Calcul de chemins citoyens                       → {@link EvacuationRouter}</li>
 * </ul>
 *
 * <p>Gère uniquement :</p>
 * <ul>
 *   <li>Les {@link AgentMovement} actifs (cycle de vie, replanification)</li>
 *   <li>Le Dijkstra spécial secouristes ({@link #findPathForRescue})</li>
 *   <li>Les notifications d'arrivée / blocage</li>
 * </ul>
 */
public class RouteGraph {

    private static final Logger LOG = Logger.getLogger(RouteGraph.class.getName());

    // ─── Collaborateurs ───────────────────────────────────────────────────
    private final Graph            graph;
    private final EvacuationRouter router;

    // ─── Déplacements actifs ──────────────────────────────────────────────
    /** Liste thread-safe des mouvements en cours. */
    private final CopyOnWriteArrayList<AgentMovement> activeMovements
        = new CopyOnWriteArrayList<>();

    // ─────────────────────────────────────────────────────────────────────
    // LISTENER
    // ─────────────────────────────────────────────────────────────────────

    public interface ArrivalListener {
        void onAgentArrived(AgentMovement movement);
        void onAgentBlocked(AgentMovement movement);
    }

    private final List<ArrivalListener> arrivalListeners = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    public RouteGraph(List<Zone> zones) {
        this.graph  = new Graph(zones, new RouteManager());
        this.router = new EvacuationRouter(this);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PLANIFICATION DES DÉPLACEMENTS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Planifie l'évacuation d'un citoyen vers le refuge le plus proche.
     *
     * @param agent agent à évacuer
     * @param from  zone de départ
     * @param zones toutes les zones (pour trouver les refuges disponibles)
     * @return mouvement créé, ou {@code null} si aucun chemin possible
     */
    public AgentMovement planEvacuation(Agent agent, Zone from, List<Zone> zones) {
        List<Zone> safeZones = zones.stream()
            .filter(z -> z instanceof Shelter)
            .filter(z -> !z.isFlooded() && z.getId() != from.getId())
            .collect(Collectors.toList());

        if (safeZones.isEmpty()) {
            LOG.warning("Aucun refuge disponible : évacuation impossible depuis " + from.getName());
            return null;
        }

        EvacuationPath path = router.findNearestSafe(from, safeZones);
        if (path == null) {
            LOG.fine("Aucun chemin depuis " + from.getName()
                + " pour l'agent " + agent.getId());
            return null;
        }

        AgentMovement movement = new AgentMovement(agent, path);
        movement.start();
        activeMovements.add(movement);
        LOG.fine("Évacuation planifiée : " + path);
        return movement;
    }

    /**
     * Planifie le déplacement d'un {@link RescueAgent} vers une zone cible.
     * Utilise un Dijkstra permissif : les routes inondées restent traversables
     * mais avec un coût très élevé.
     *
     * @param agent agent de secours
     * @param from  zone de départ
     * @param to    zone cible
     * @return mouvement créé, ou {@code null} si aucun chemin possible
     */
    public AgentMovement planRescueMission(RescueAgent agent, Zone from, Zone to) {
        EvacuationPath path = findPathForRescue(from, to);
        if (path == null) return null;

        AgentMovement movement = new AgentMovement(agent, path);
        movement.start();
        activeMovements.add(movement);
        return movement;
    }

    // ─────────────────────────────────────────────────────────────────────
    // DIJKSTRA SECOURISTES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Dijkstra spécial secouristes : les arêtes inondées restent traversables
     * mais coûtent beaucoup plus cher, permettant d'atteindre des citoyens
     * bloqués quand aucun chemin libre n'existe.
     */
    public EvacuationPath findPathForRescue(Zone from, Zone to) {
        if (from == null || to == null) return null;
        if (from.getId() == to.getId()) return EvacuationPath.trivial(from);

        List<Edge> allEdges = graph.getEdges();
        Map<Integer, Zone> zoneMap = graph.getZoneMap();

        // Graphe non-orienté pour les secouristes
        Map<Integer, List<RescueEntry>> adj = new HashMap<>();
        for (Edge edge : allEdges) {
            int a = edge.getFromZone().getId();
            int b = edge.getToZone().getId();
            adj.computeIfAbsent(a, k -> new ArrayList<>()).add(new RescueEntry(edge, b));
            adj.computeIfAbsent(b, k -> new ArrayList<>()).add(new RescueEntry(edge, a));
        }

        final double INF = Double.MAX_VALUE / 4.0;
        Map<Integer, Double>  dist     = new HashMap<>();
        Map<Integer, Integer> prevZone = new HashMap<>();
        Map<Integer, Edge>    prevEdge = new HashMap<>();
        PriorityQueue<Integer> pq = new PriorityQueue<>(
            Comparator.comparingDouble(id -> dist.getOrDefault(id, INF)));

        dist.put(from.getId(), 0.0);
        pq.offer(from.getId());

        while (!pq.isEmpty()) {
            int curr = pq.poll();
            if (curr == to.getId()) break;
            double currDist = dist.getOrDefault(curr, INF);
            for (RescueEntry entry : adj.getOrDefault(curr, Collections.emptyList())) {
                double cost = entry.edge.rescueRoutingCost();
                double nd   = currDist + cost;
                if (nd < dist.getOrDefault(entry.toId, INF)) {
                    dist.put(entry.toId, nd);
                    prevZone.put(entry.toId, curr);
                    prevEdge.put(entry.toId, entry.edge);
                    pq.offer(entry.toId);
                }
            }
        }

        if (!dist.containsKey(to.getId())) return null;

        // Reconstruction du chemin
        List<Zone> zoneSeq = new ArrayList<>();
        List<Edge> edgeSeq = new ArrayList<>();
        int curr = to.getId();
        while (curr != from.getId()) {
            Zone z = zoneMap.get(curr);
            if (z == null && curr == to.getId()) z = to;
            if (z != null) zoneSeq.add(0, z);
            Edge e = prevEdge.get(curr);
            if (e != null) edgeSeq.add(0, e);
            Integer prev = prevZone.get(curr);
            if (prev == null) return null;
            curr = prev;
        }
        zoneSeq.add(0, from);

        double total = edgeSeq.stream().mapToDouble(Edge::rescueRoutingCost).sum();
        return new EvacuationPath(zoneSeq, edgeSeq, total);
    }

    /** Entrée du graphe d'adjacence pour le Dijkstra secouristes. */
    private static class RescueEntry {
        final Edge edge;
        final int  toId;
        RescueEntry(Edge edge, int toId) { this.edge = edge; this.toId = toId; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BOUCLE DE SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Avance tous les mouvements d'un pas de simulation.
     * Doit être appelé par le {@code SimulationController} à chaque tick.
     *
     * @param deltaSeconds durée du pas simulé (ex : 1.0)
     */
    public void tick(double deltaSeconds) {
        List<AgentMovement> toRemove = new ArrayList<>();

        for (AgentMovement mv : activeMovements) {
            boolean arrived = mv.step(deltaSeconds);
            if (arrived) {
                toRemove.add(mv);
                notifyArrived(mv);
            } else if (mv.isBlocked()) {
                toRemove.add(mv);
                notifyBlocked(mv);
                replanBlocked(mv);
            }
        }
        activeMovements.removeAll(toRemove);
    }

    /** Replanifie un agent bloqué depuis sa position courante. */
    private void replanBlocked(AgentMovement blocked) {
        GeoPosition pos = blocked.getCurrentPosition();
        Zone closestZone = graph.findClosestZone(pos.getLatitude(), pos.getLongitude());
        if (closestZone == null || closestZone.isFlooded()) return;

        List<Zone> allZones = new ArrayList<>(graph.getZones());
        AgentMovement newMovement = planEvacuation(blocked.getAgent(), closestZone, allZones);
        if (newMovement != null)
            LOG.fine("Agent " + blocked.getAgent().getId()
                + " replanifié depuis " + closestZone.getName());
    }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR D'ÉTAT
    // ─────────────────────────────────────────────────────────────────────

    /** Recalcule l'état de tous les edges — délégué à {@link Graph}. */
    public void refreshAllEdges() {
        graph.refreshAllEdges();
    }

    /** Met à jour les flux selon les agents actuellement en transit. */
    public void simulateFlows() {
        graph.getEdges().forEach(e -> e.setCurrentFlow(0));
        for (AgentMovement mv : activeMovements) {
            if (mv == null || mv.getPath() == null) continue;
            mv.getPath().getEdges().forEach(e -> e.addFlow(1));
        }
    }

    /**
     * Supprime tous les mouvements actifs d'un agent donné et
     * recalcule immédiatement les flux et états des arêtes.
     *
     * @param agentId identifiant de l'agent
     */
    public void removeMovementsOfAgent(int agentId) {
        activeMovements.removeIf(mv ->
            mv != null &&
            mv.getAgent() != null &&
            mv.getAgent().getId() == agentId);
        simulateFlows();
        refreshAllEdges();
    }

    // ─────────────────────────────────────────────────────────────────────
    // LISTENERS
    // ─────────────────────────────────────────────────────────────────────

    public void addArrivalListener(ArrivalListener l)    { arrivalListeners.add(l); }
    public void removeArrivalListener(ArrivalListener l) { arrivalListeners.remove(l); }

    private void notifyArrived(AgentMovement mv) {
        arrivalListeners.forEach(l -> l.onAgentArrived(mv));
    }

    private void notifyBlocked(AgentMovement mv) {
        arrivalListeners.forEach(l -> l.onAgentBlocked(mv));
    }

    // ─────────────────────────────────────────────────────────────────────
    // API PUBLIQUE — délégations vers Graph
    // ─────────────────────────────────────────────────────────────────────

    /** Toutes les arêtes du graphe (lecture seule). */
    public List<Edge> getEdges()                        { return graph.getEdges(); }

    /** Arêtes adjacentes à une zone. */
    public List<Edge> getEdgesForZone(Zone zone)        { return graph.getEdgesForZone(zone); }

    /** Zone la plus proche géographiquement. */
    public Zone findClosestZone(double lat, double lng) { return graph.findClosestZone(lat, lng); }

    /** Routeur d'évacuation (exposé pour {@link EvacuationRouter}). */
    public EvacuationRouter getRouter()                 { return router; }

    /** Mouvements actifs en cours (lecture seule). */
    public List<AgentMovement> getActiveMovements()     { return Collections.unmodifiableList(activeMovements); }

    /** Graphe topologique sous-jacent. */
    public Graph getGraph()                             { return graph; }
}