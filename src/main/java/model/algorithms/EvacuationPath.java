package model.algorithms;

import model.graph.EdgeState;
import model.graph.Edge;
import model.zone.Zone;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Résultat d'un calcul de chemin d'évacuation.
 * Contient la séquence de zones, les arêtes à emprunter, le coût total
 * et les waypoints GPS pour l'affichage sur la carte.
 */
public class EvacuationPath {

    private final List<Zone>        zones;
    private final List<Edge>        edges;
    private final double            totalCost;
    private final List<GeoPosition> gpsWaypoints; // tous les points GPS du tracé

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Constructs a new EvacuationPath.
     * @param zones the zones.
     * @param edges the edges.
     * @param totalCost the totalCost.
     */
    public EvacuationPath(List<Zone> zones, List<Edge> edges, double totalCost) {
        this.zones    = Collections.unmodifiableList(new ArrayList<>(zones));
        this.edges    = Collections.unmodifiableList(new ArrayList<>(edges));
        this.totalCost = totalCost;
        this.gpsWaypoints = buildGpsWaypoints(zones, edges);
    }

    /**
     * Create a trivial path where origin and destination are the same zone.
     *
     * @param zone the zone used as both origin and destination
     * @return an EvacuationPath representing a trivial path
     */
    public static EvacuationPath trivial(Zone zone) {
        return new EvacuationPath(
            Collections.singletonList(zone),
            Collections.emptyList(),
            0.0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // INTERPOLATION DE POSITION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Return an interpolated GPS position for an agent given a progress value.
     *
     * @param progress progress along the path from 0.0 (origin) to 1.0 (destination)
     * @return interpolated GeoPosition for the given progress
     */
    public GeoPosition interpolatePosition(double progress) {
        if (gpsWaypoints.isEmpty()) {
            if (!zones.isEmpty()) {
                Zone z = zones.get(0);
                return new GeoPosition(z.getLatitude(), z.getLongitude());
            }
            return new GeoPosition(45.7640, 4.8357);
        }

        progress = Math.max(0.0, Math.min(1.0, progress));
        if (progress >= 1.0) {
            GeoPosition last = gpsWaypoints.get(gpsWaypoints.size() - 1);
            return last;
        }

        int totalPts = gpsWaypoints.size();
        double floatIdx = progress * (totalPts - 1);
        int    idx0     = (int) floatIdx;
        int    idx1     = Math.min(idx0 + 1, totalPts - 1);
        double frac     = floatIdx - idx0;

        GeoPosition p0 = gpsWaypoints.get(idx0);
        GeoPosition p1 = gpsWaypoints.get(idx1);

        double lat = p0.getLatitude()  + frac * (p1.getLatitude()  - p0.getLatitude());
        double lng = p0.getLongitude() + frac * (p1.getLongitude() - p0.getLongitude());
        return new GeoPosition(lat, lng);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION DES GPS WAYPOINTS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds gps waypoints.
     * @param zoneSeq the zoneSeq.
     * @param edgeSeq the edgeSeq.
     * @return the List<GeoPosition>.
     */
    private List<GeoPosition> buildGpsWaypoints(List<Zone> zoneSeq, List<Edge> edgeSeq) {
        List<GeoPosition> pts = new ArrayList<>();
        if (zoneSeq.isEmpty()) return pts;

        // Si pas d'edges, juste les centres des zones
        if (edgeSeq.isEmpty()) {
            for (Zone z : zoneSeq)
                pts.add(new GeoPosition(z.getLatitude(), z.getLongitude()));
            return pts;
        }

        // Sinon, on suit les waypoints réels des arêtes
        pts.add(new GeoPosition(zoneSeq.get(0).getLatitude(), zoneSeq.get(0).getLongitude()));
        for (Edge edge : edgeSeq) {
            List<GeoPosition> wp = edge.getWaypoints();
            if (wp.size() > 2) {
                // Ajouter les waypoints intermédiaires (pas les extrémités déjà ajoutées)
                for (int i = 1; i < wp.size() - 1; i++)
                    pts.add(wp.get(i));
            }
            // Ajouter la zone de destination de cet edge
            pts.add(wp.isEmpty()
                ? new GeoPosition(edge.getToZone().getLatitude(), edge.getToZone().getLongitude())
                : wp.get(wp.size() - 1));
        }
        return pts;
    }

    // ─────────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────────

    /** @return an unmodifiable list of zones composing the path */
    public List<Zone> getZones() { return zones; }

    /** @return an unmodifiable list of edges composing the path */
    public List<Edge> getEdges() { return edges; }

    /** @return the computed total cost for this path */
    public double getTotalCost() { return totalCost; }

    /** @return GPS waypoints representing the full path for rendering */
    public List<GeoPosition> getGpsWaypoints() { return Collections.unmodifiableList(gpsWaypoints); }

    /** @return the destination zone of this path, or null if empty */
    public Zone getDestination() { return zones.isEmpty() ? null : zones.get(zones.size() - 1); }

    /** @return the origin zone of this path, or null if empty */
    public Zone getOrigin() { return zones.isEmpty() ? null : zones.get(0); }

    /** @return true if the path contains no edges */
    public boolean isEmpty() { return edges.isEmpty(); }

    /**
     * Get approximate total distance of the path in kilometers.
     *
     * @return total distance in kilometers
     */
    public double getTotalDistanceKm() {
        return edges.stream().mapToDouble(Edge::geoDistance).sum();
    }

    /**
     * Count how many edges on the path are marked as AT_RISK.
     *
     * @return number of at-risk edges
     */
    public long countAtRiskEdges() {
        return edges.stream().filter(e -> e.getState() == EdgeState.AT_RISK).count();
    }


    /**
     * Retourne les zones restantes à parcourir à partir de la position courante
     * de l'AgentMovement. Appelé depuis SimulationController.getSelectedAgentRemainingPath().
     *
     * NOTE : comme EvacuationPath est immutable, c'est AgentMovement qui doit
     * exposer cette méthode en filtrant selon son état courant.
     * Si vous préférez la garder ici, passez l'index courant en paramètre.
     */
    public List<Zone> getRemainingZones() {
        // Retourne toutes les zones (le filtrage selon la progression
        // se fait dans AgentMovement.getRemainingZones() ci-dessous).
        return new ArrayList<>(zones);
    }
    


    @Override
    /**
     * Performs string.
     * @return the String.
     */
    public String toString() {
        if (zones.isEmpty()) return "EvacuationPath[empty]";
        return String.format("EvacuationPath[%s → %s, %d étapes, coût=%.1f]",
            getOrigin().getName(), getDestination().getName(),
            edges.size(), totalCost);
    }
}
