package model.graph;

import model.observer.Subject;
import model.zone.Zone;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modèle d'une route entre deux zones.
 *
 * Nouveautés :
 * <ul>
 *   <li>{@code capacityMax}  — nombre de véhicules/heure max (chargé depuis JSON).</li>
 *   <li>{@code currentFlow}  — flux courant (mis à jour par la simulation).</li>
 *   <li>{@link #getFlowRatio()} — ratio 0-∞ utilisé par {@link EdgeState#compute}.</li>
 *   <li>{@code distanceMeters} / {@code durationSeconds} — fournis par OSRM.</li>
 * </ul>
 */
public class Edge extends Subject<Edge> {

    // ─── Identité ─────────────────────────────────────────────────────────
    private final int    id;
    private final String name;

    // ─── Graphe ───────────────────────────────────────────────────────────
    private final Zone fromZone;
    private final Zone toZone;

    /** Points GPS réels du tracé (depuis OSRM ou JSON). */
    private final List<GeoPosition> waypoints;

    // ─── Capacité / flux ─────────────────────────────────────────────────
    /** Capacité théorique en véhicules/heure. */
    private final int capacityMax;

    /** Flux courant en véhicules/heure (modifié par la simulation). */
    private volatile int currentFlow;

    // ─── Métriques OSRM ──────────────────────────────────────────────────
    private final double distanceMeters;
    private final double durationSeconds;

    // ─── État (observable) ────────────────────────────────────────────────
    private EdgeState state;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTEUR
    // ─────────────────────────────────────────────────────────────────────

    public Edge(int id, String name, Zone fromZone, Zone toZone,
                List<GeoPosition> waypoints,
                int capacityMax, double distanceMeters, double durationSeconds) {
        this.id              = id;
        this.name            = name;
        this.fromZone        = fromZone;
        this.toZone          = toZone;
        this.waypoints       = Collections.unmodifiableList(new ArrayList<>(waypoints));
        this.capacityMax     = Math.max(1, capacityMax);
        this.currentFlow     = 0;
        this.distanceMeters  = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.state           = computeState();
    }

    /** Constructeur de compatibilité (sans métriques OSRM). */
    public Edge(int id, String name, Zone fromZone, Zone toZone,
                List<GeoPosition> waypoints) {
        this(id, name, fromZone, toZone, waypoints, 1000, 0, 0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // FLUX / CAPACITÉ
    // ─────────────────────────────────────────────────────────────────────

    public int    getCapacityMax()   { return capacityMax; }
    public int    getCurrentFlow()   { return currentFlow; }
    public double getDistanceMeters(){ return distanceMeters; }
    public double getDurationSeconds(){ return durationSeconds; }

    /** Ratio flux/capacité (0.0 → libre, >1.0 → surchargé). */
    public double getFlowRatio() {
        return (double) currentFlow / capacityMax;
    }

    /**
     * Met à jour le flux courant et recalcule l'état.
     * Notifie les observers si l'état change.
     *
     * @param flow nouveau flux (véhicules/heure, ≥ 0)
     */
    public void setCurrentFlow(int flow) {
        this.currentFlow = Math.max(0, flow);
        refreshState();
    }

    // ─────────────────────────────────────────────────────────────────────
    // ÉTAT
    // ─────────────────────────────────────────────────────────────────────

    public EdgeState getState() { return state; }

    /**
     * Recalcule l'état et notifie les observers si changement.
     * @return true si l'état a effectivement changé.
     */
    public boolean refreshState() {
        EdgeState newState = computeState();
        if (newState != this.state) {
            this.state = newState;
            notifyObservers(this);
            return true;
        }
        return false;
    }

    private EdgeState computeState() {
        return EdgeState.compute(
            fromZone.isFlooded(), toZone.isFlooded(),
            getFlowRatio()
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCESSEURS
    // ─────────────────────────────────────────────────────────────────────

    public int               getId()        { return id; }
    public String            getName()      { return name; }
    public Zone              getFromZone()  { return fromZone; }
    public Zone              getToZone()    { return toZone; }
    public List<GeoPosition> getWaypoints() { return waypoints; }

    @Override
    public String toString() {
        return String.format("Edge{id=%d, '%s', %s→%s, cap=%d, flow=%d, state=%s}",
            id, name, fromZone.getName(), toZone.getName(),
            capacityMax, currentFlow, state);
    }
}