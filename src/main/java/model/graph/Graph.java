package model.graph;

import model.zone.Zone;
import model.manager.RouteManager;
import model.zone.Shelter;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Graphe topologique : associe des {@link Zone}s à des {@link Edge}s.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Indexer les zones par id</li>
 *   <li>Stocker et exposer les arêtes (chargées via {@link RouteManager})</li>
 *   <li>Garantir la connectivité vers les refuges ({@code ensureShelterConnectivity})</li>
 *   <li>Fournir des requêtes structurelles (voisins, arêtes d'une zone, …)</li>
 * </ul>
 *
 * <p>Ne connaît ni les agents, ni les mouvements, ni le routage.</p>
 */
public class Graph {

    private static final Logger LOG = Logger.getLogger(Graph.class.getName());

    // ─── Données ──────────────────────────────────────────────────────────
    private final Map<Integer, Zone> zoneMap;
    /** Liste mutable : Graph peut l'enrichir (ensureShelterConnectivity). */
    private final List<Edge>         edges;
    private final RouteManager       routeManager;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Construit le graphe à partir d'une liste de zones et du gestionnaire
     * de routes qui se charge de lire/créer les arêtes.
     *
     * @param zones        liste de toutes les zones
     * @param routeManager gestionnaire responsable du chargement des routes
     */
    public Graph(List<Zone> zones, RouteManager routeManager) {
        this.routeManager = routeManager;
        this.zoneMap = new LinkedHashMap<>();
        for (Zone z : zones) zoneMap.put(z.getId(), z);
        this.edges = routeManager.loadEdges(zoneMap);
        ensureShelterConnectivity();
        LOG.info(edges.size() + " routes après vérification connectivité refuges");
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONNECTIVITÉ REFUGES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Garantit que chaque quartier a au moins un chemin vers chaque refuge
     * et au moins 3 connexions locales pour que Dijkstra puisse contourner
     * une route coupée.
     */
    private void ensureShelterConnectivity() {
        List<Zone> allZones = new ArrayList<>(zoneMap.values());
        List<Zone> shelters = allZones.stream()
            .filter(z -> z instanceof Shelter)
            .collect(Collectors.toList());

        int nextId = edges.stream().mapToInt(Edge::getId).max().orElse(0) + 1;

        for (Zone from : allZones) {
            if (from instanceof Shelter) continue;

            // Connexion directe vers chaque refuge manquant
            for (Zone shelter : shelters) {
                if (!hasEdgeBetween(from, shelter)) {
                    edges.add(routeManager.buildEdge(nextId++,
                        from.getName() + " → " + shelter.getName(), from, shelter, 5));
                }
            }

            // 3 connexions locales vers les zones non-refuge les plus proches
            allZones.stream()
                .filter(z -> z.getId() != from.getId())
                .filter(z -> !(z instanceof Shelter))
                .sorted(Comparator.comparingDouble(z -> geoDistanceSquared(from, z)))
                .limit(3)
                .forEach(to -> {
                    if (!hasEdgeBetween(from, to)) {
                        int id = edges.stream().mapToInt(Edge::getId).max().orElse(0) + 1;
                        edges.add(routeManager.buildEdge(id,
                            from.getName() + " → " + to.getName(), from, to, 5));
                    }
                });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MISES À JOUR D'ÉTAT
    // ─────────────────────────────────────────────────────────────────────

    /** Recalcule l'état de toutes les arêtes. */
    public void refreshAllEdges() {
        edges.forEach(Edge::refreshState);
    }

    // ─────────────────────────────────────────────────────────────────────
    // REQUÊTES STRUCTURELLES
    // ─────────────────────────────────────────────────────────────────────

    /** Toutes les arêtes du graphe (lecture seule). */
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /** Toutes les zones indexées par id (lecture seule). */
    public Map<Integer, Zone> getZoneMap() {
        return Collections.unmodifiableMap(zoneMap);
    }

    /** Toutes les zones (lecture seule). */
    public Collection<Zone> getZones() {
        return Collections.unmodifiableCollection(zoneMap.values());
    }

    /** Zone par id, ou {@code null} si inconnue. */
    public Zone getZone(int id) {
        return zoneMap.get(id);
    }

    /** Arêtes adjacentes à une zone (entrantes ou sortantes). */
    public List<Edge> getEdgesForZone(Zone zone) {
        return edges.stream()
            .filter(e -> e.getFromZone().getId() == zone.getId()
                      || e.getToZone().getId()   == zone.getId())
            .collect(Collectors.toList());
    }

    /** Arêtes partant d'une zone (sortantes uniquement). */
    public List<Edge> getOutEdges(Zone zone) {
        return edges.stream()
            .filter(e -> e.getFromZone().getId() == zone.getId())
            .collect(Collectors.toList());
    }

    /**
     * Zones voisines directement accessibles depuis une zone donnée
     * (via une arête non inondée).
     */
    public List<Zone> getAccessibleNeighbors(Zone zone) {
        return getOutEdges(zone).stream()
            .filter(Edge::isCrossable)
            .map(Edge::getToZone)
            .collect(Collectors.toList());
    }

    /** Zone la plus proche géographiquement d'une position (lat, lng). */
    public Zone findClosestZone(double lat, double lng) {
        Zone closest = null;
        double minDist = Double.MAX_VALUE;
        for (Zone z : zoneMap.values()) {
            double d = Math.abs(z.getLatitude() - lat) + Math.abs(z.getLongitude() - lng);
            if (d < minDist) { minDist = d; closest = z; }
        }
        return closest;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────

    /** Vérifie l'existence d'une arête entre deux zones (dans un sens ou l'autre). */
    public boolean hasEdgeBetween(Zone a, Zone b) {
        if (a == null || b == null) return true;
        return edges.stream().anyMatch(e ->
            (e.getFromZone().getId() == a.getId() && e.getToZone().getId() == b.getId()) ||
            (e.getFromZone().getId() == b.getId() && e.getToZone().getId() == a.getId())
        );
    }

    private double geoDistanceSquared(Zone a, Zone b) {
        double dLat = a.getLatitude() - b.getLatitude();
        double dLng = a.getLongitude() - b.getLongitude();
        return dLat * dLat + dLng * dLng;
    }
}