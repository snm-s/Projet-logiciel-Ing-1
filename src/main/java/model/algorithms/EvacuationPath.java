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

    public EvacuationPath(List<Zone> zones, List<Edge> edges, double totalCost) {
        this.zones    = Collections.unmodifiableList(new ArrayList<>(zones));
        this.edges    = Collections.unmodifiableList(new ArrayList<>(edges));
        this.totalCost = totalCost;
        this.gpsWaypoints = buildGpsWaypoints(zones, edges);
    }

    /** Chemin trivial (départ == arrivée). */
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
     * Retourne la position GPS interpolée d'un agent en fonction de sa progression
     * sur le chemin (0.0 = départ, 1.0 = arrivée).
     *
     * @param progress  avancement de l'agent, entre 0.0 et 1.0
     * @return position GPS interpolée
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

    public List<Zone>         getZones()        { return zones; }
    public List<Edge>         getEdges()        { return edges; }
    public double             getTotalCost()    { return totalCost; }
    public List<GeoPosition>  getGpsWaypoints() { return Collections.unmodifiableList(gpsWaypoints); }
    public Zone               getDestination()  { return zones.isEmpty() ? null : zones.get(zones.size() - 1); }
    public Zone               getOrigin()       { return zones.isEmpty() ? null : zones.get(0); }
    public boolean            isEmpty()         { return edges.isEmpty(); }

    /** Distance totale approximative en km. */
    public double getTotalDistanceKm() {
        return edges.stream().mapToDouble(Edge::geoDistance).sum();
    }

    /** Nombre d'arêtes à risque sur le chemin. */
    public long countAtRiskEdges() {
        return edges.stream().filter(e -> e.getState() == EdgeState.AT_RISK).count();
    }

    @Override
    public String toString() {
        if (zones.isEmpty()) return "EvacuationPath[empty]";
        return String.format("EvacuationPath[%s → %s, %d étapes, coût=%.1f]",
            getOrigin().getName(), getDestination().getName(),
            edges.size(), totalCost);
    }
}