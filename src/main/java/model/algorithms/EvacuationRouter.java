package model.algorithms;

import model.graph.Edge;
import model.graph.RouteGraph;
import model.zone.Zone;

import java.util.*;

/**
 * Routeur d'évacuation basé sur Dijkstra étendu.
 *
 * <h3>Critères de routage (multi-objectif) :</h3>
 * <ol>
 *   <li><b>Sécurité</b> : pénalité massive sur les arêtes inondées ou à risque</li>
 *   <li><b>Capacité</b> : pénalité de congestion quand le flux dépasse 80 % de la capacité</li>
 *   <li><b>Distance</b> : distance géographique minimale</li>
 * </ol>
 *
 * <p>Le poids de chaque critère est configurable via {@link #setFloodWeight(double)}.
 *
 * <h3>Utilisation :</h3>
 * <pre>{@code
 * EvacuationRouter router = new EvacuationRouter(graph);
 * EvacuationPath path = router.findPath(fromZone, toZone);
 * if (path != null) {
 *     List<Zone> zones = path.getZones();
 *     List<Edge> edges = path.getEdges();
 * }
 * }</pre>
 */
public class EvacuationRouter {

    private final RouteGraph graph;

    /** Pénalité appliquée aux arêtes AT_RISK (multiplicateur de distance). */
    private double floodWeight = 50.0;

    /** Pénalité infinie pour les arêtes FLOODED. */
    private static final double INF = Double.MAX_VALUE / 2.0;

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Constructs a new EvacuationRouter.
     * @param graph the graph.
     */
    public EvacuationRouter(RouteGraph graph) {
        this.graph = graph;
    }

    /**
     * Create a router for the provided RouteGraph instance.
     *
     * @param graph the RouteGraph to use for routing
     */

    /**
     * Set the flood weight multiplier used when computing routing costs.
     *
     * @param w flood weight multiplier (larger values penalize risky edges more)
     */
    public void setFloodWeight(double w) { this.floodWeight = w; }

    // ─────────────────────────────────────────────────────────────────────
    // DIJKSTRA PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Compute the optimal evacuation path between two zones using a weighted Dijkstra.
     *
     * @param from origin zone
     * @param to destination zone
     * @return EvacuationPath or null if no path is available
     */
    public EvacuationPath findPath(Zone from, Zone to) {
        if (from == null || to == null) return null;
        if (from.getId() == to.getId()) return EvacuationPath.trivial(from);

        // Graphe d'adjacence depuis les edges
        Map<Integer, List<EdgeEntry>> adjacency = buildAdjacency();

        // Dijkstra
        Map<Integer, Double>  dist    = new HashMap<>();
        Map<Integer, Integer> prevZone = new HashMap<>();  // zoneId → zoneId précédente
        Map<Integer, Edge>    prevEdge = new HashMap<>();  // zoneId → edge utilisée
        PriorityQueue<int[]>  pq      = new PriorityQueue<>(Comparator.comparingDouble(a -> dist.getOrDefault(a[0], INF)));

        dist.put(from.getId(), 0.0);
        pq.offer(new int[]{from.getId()});

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int   currId = curr[0];
            double currDist = dist.getOrDefault(currId, INF);

            if (currId == to.getId()) break; // arrivé

            List<EdgeEntry> neighbors = adjacency.getOrDefault(currId, Collections.emptyList());
            for (EdgeEntry entry : neighbors) {
                Edge   edge     = entry.edge;
                int    nextId   = entry.toId;
                double edgeCost = edge.routingCost(floodWeight);

                if (edgeCost >= INF) continue; // arête infranchissable

                double newDist = currDist + edgeCost;
                if (newDist < dist.getOrDefault(nextId, INF)) {
                    dist.put(nextId, newDist);
                    prevZone.put(nextId, currId);
                    prevEdge.put(nextId, edge);
                    pq.offer(new int[]{nextId});
                }
            }
        }

        // Reconstituer le chemin
        if (!dist.containsKey(to.getId())) return null; // inaccessible

        return reconstructPath(from, to, prevZone, prevEdge);
    }

    /**
     * Find the nearest safe (non-flooded) candidate zone reachable from the given origin.
     *
     * @param from origin zone
     * @param candidates candidate destination zones to consider
     * @return best EvacuationPath to the nearest safe candidate, or null
     */
    public EvacuationPath findNearestSafe(Zone from, List<Zone> candidates) {
        EvacuationPath best = null;
        double bestCost = INF;

        for (Zone candidate : candidates) {
            if (candidate.getId() == from.getId()) continue;
            if (candidate.isFlooded()) continue;

            EvacuationPath path = findPath(from, candidate);
            if (path != null && path.getTotalCost() < bestCost) {
                best     = path;
                bestCost = path.getTotalCost();
            }
        }
        return best;
    }

    /**
     * Compute evacuation paths for all flooded zones to their nearest safe zones.
     *
     * @param zones list of zones to analyze
     * @return a map from zone id to the best EvacuationPath
     */
    public Map<Integer, EvacuationPath> computeAllEvacuationPaths(List<Zone> zones) {
        Map<Integer, EvacuationPath> result = new HashMap<>();
        List<Zone> safeZones = zones.stream()
            .filter(z -> !z.isFlooded())
            .collect(java.util.stream.Collectors.toList());

        for (Zone zone : zones) {
            if (zone.isFlooded()) {
                EvacuationPath path = findNearestSafe(zone, safeZones);
                if (path != null) result.put(zone.getId(), path);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILITAIRES PRIVÉS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds adjacency.
     * @return the Map<Integer, List<EdgeEntry>>.
     */
    private Map<Integer, List<EdgeEntry>> buildAdjacency() {
        Map<Integer, List<EdgeEntry>> adj = new HashMap<>();
        for (Edge edge : graph.getEdges()) {
            int fromId = edge.getFromZone().getId();
            int toId   = edge.getToZone().getId();
            adj.computeIfAbsent(fromId, k -> new ArrayList<>())
               .add(new EdgeEntry(edge, toId));
            // Graphe non-dirigé : on peut traverser dans les deux sens
            adj.computeIfAbsent(toId, k -> new ArrayList<>())
               .add(new EdgeEntry(edge, fromId));
        }
        return adj;
    }

    private EvacuationPath reconstructPath(Zone from, Zone to,
                                           Map<Integer, Integer> prevZone,
                                           Map<Integer, Edge>    prevEdge) {
        List<Zone> zoneSeq = new ArrayList<>();
        List<Edge> edgeSeq = new ArrayList<>();

        // Remonter depuis la destination
        Map<Integer, Zone> zoneById = new HashMap<>();
        for (Edge e : graph.getEdges()) {
            zoneById.put(e.getFromZone().getId(), e.getFromZone());
            zoneById.put(e.getToZone().getId(),   e.getToZone());
        }
        zoneById.put(from.getId(), from);
        zoneById.put(to.getId(),   to);

        int currId = to.getId();
        while (currId != from.getId()) {
            zoneSeq.add(0, zoneById.get(currId));
            Edge edge = prevEdge.get(currId);
            if (edge != null) edgeSeq.add(0, edge);
            Integer prev = prevZone.get(currId);
            if (prev == null) return null;
            currId = prev;
        }
        zoneSeq.add(0, from);

        double totalCost = 0.0;
        for (Edge e : edgeSeq) totalCost += e.routingCost(floodWeight);

        return new EvacuationPath(zoneSeq, edgeSeq, totalCost);
    }

    // ─── Structure interne ───────────────────────────────────────────────

    private static class EdgeEntry {
        final Edge edge;
        final int  toId;
        EdgeEntry(Edge e, int toId) { this.edge = e; this.toId = toId; }
    }
}
