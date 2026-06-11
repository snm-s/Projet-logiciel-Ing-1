package model.graph;

import model.graph.EdgeState;
import model.zone.Zone;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Arête du graphe routier reliant deux zones.
 *
 * <p>Chaque arête possède :
 * <ul>
 *   <li>Un tracé GPS (waypoints OSRM ou ligne droite en fallback)</li>
 *   <li>Une capacité maximale (véhicules/heure)</li>
 *   <li>Un flux courant (mis à jour par la simulation)</li>
 *   <li>Un état dérivé (SAFE / AT_RISK / FLOODED) calculé automatiquement</li>
 * </ul>
 */
public class Edge {


    // ─── Champs ───────────────────────────────────────────────────────────
    private final int    id;
    private final String name;
    private final Zone   fromZone;
    private final Zone   toZone;
    private final List<GeoPosition> waypoints;
    private final int    capacityMax;   // véhicules/heure
    private int          currentFlow;   // flux courant (agents en transit)
    private int          floodedCount;  // nombre de fois inondée (statistiques)
    private EdgeState        state;

    // Observateurs (notifiés quand l'état change)
    private final List<EdgeObserver> observers = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────

    public Edge(int id, String name, Zone fromZone, Zone toZone,
                List<GeoPosition> waypoints, int capacityMax,
                int currentFlow, int floodedCount) {
        this.id           = id;
        this.name         = name;
        this.fromZone     = fromZone;
        this.toZone       = toZone;
        this.waypoints    = new ArrayList<>(waypoints);
        this.capacityMax  = capacityMax;
        this.currentFlow  = currentFlow;
        this.floodedCount = floodedCount;
        this.state        = computeState();
    }

    // ─────────────────────────────────────────────────────────────────────
    // ÉTAT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Recalcule l'état de l'arête selon :
     *  - Les zones aux extrémités (inondées ou non)
     *  - L'altitude moyenne (zone basse = risque)
     *  - Le flux courant vs capacité
     */
    public EdgeState computeState() {
        // 1. Gestion des inondations (Priorité maximale)
        boolean fromFlooded = fromZone.isFlooded();
        boolean toFlooded   = toZone.isFlooded();

        if (fromFlooded && toFlooded) return EdgeState.FLOODED;
        if (fromFlooded || toFlooded) return EdgeState.AT_RISK;

        // 2. Gestion de l'altitude (Risque géographique)
        double avgAlt = (fromZone.getAltitude() + toZone.getAltitude()) / 2.0;
        if (avgAlt < 1.0) return EdgeState.AT_RISK;

        // 3. Gestion du flux (Congestion)
        // On calcule le ratio une seule fois pour tout le reste
        double flowRatio = (capacityMax > 0) ? (double) currentFlow / capacityMax : 0.0;

        if (flowRatio > 1.0) return EdgeState.OVERLOADED; // Flux dépasse la capacité
        if (flowRatio > 0.8) return EdgeState.CONGESTED;  // Congestion critique
        if (flowRatio > 0.5) return EdgeState.AT_RISK;    // Risque modéré

        // 4. État sain (SAFE et NORMAL fusionnés)
        return EdgeState.SAFE;
    }

    /** Met à jour l'état et notifie les observateurs si changement. */
    public void refreshState() {
        EdgeState newState = computeState();
        if (newState != this.state) {
            this.state = newState;
            notifyObservers();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // FLUX
    // ─────────────────────────────────────────────────────────────────────

    public void setCurrentFlow(int flow) {
        this.currentFlow = Math.max(0, flow);
        refreshState();
    }

    public void addFlow(int delta) {
        setCurrentFlow(this.currentFlow + delta);
    }

    public void removeFlow(int delta) {
        setCurrentFlow(Math.max(0, this.currentFlow - delta));
    }

    /** Vérifie si l'arête peut accepter des agents supplémentaires. */
    public boolean hasCapacity(int agentsToAdd) {
        return (currentFlow + agentsToAdd) <= capacityMax;
    }

    /** Capacité disponible restante. */
    public int availableCapacity() {
        return Math.max(0, capacityMax - currentFlow);
    }

    /**
     * Coût de traversée pour l'algorithme de routage.
     * Combine : distance géographique + pénalités inondation + pénalité congestion.
     *
     * @param floodWeight  pondération risque inondation (ex : 100.0)
     * @return coût ≥ 0, Double.MAX_VALUE si infranchissable
     */
    public double routingCost(double floodWeight) {
        if (state == EdgeState.FLOODED) return Double.MAX_VALUE; // route coupée

        double dist = geoDistance();
        double floodPenalty = (state == EdgeState.AT_RISK) ? floodWeight * dist : 0.0;

        // Pénalité congestion proportionnelle au taux de remplissage
        double congestionRatio = (capacityMax > 0) ? (double) currentFlow / capacityMax : 0.0;
        double congestionPenalty = dist * congestionRatio * 2.0;

        return dist + floodPenalty + congestionPenalty;
    }

    /** Distance géographique entre les deux extrémités (en km approx). */
    public double geoDistance() {
        double dLat = Math.toRadians(toZone.getLatitude()  - fromZone.getLatitude());
        double dLng = Math.toRadians(toZone.getLongitude() - fromZone.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(fromZone.getLatitude()))
                 * Math.cos(Math.toRadians(toZone.getLatitude()))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─────────────────────────────────────────────────────────────────────
    // OBSERVATEURS
    // ─────────────────────────────────────────────────────────────────────

    public interface EdgeObserver {
        void onEdgeStateChanged(Edge edge, EdgeState newState);
    }

    public void addObserver(EdgeObserver obs)    { observers.add(obs); }
    public void removeObserver(EdgeObserver obs) { observers.remove(obs); }

    private void notifyObservers() {
        for (EdgeObserver obs : observers) obs.onEdgeStateChanged(this, state);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────────

    public int              getId()            { return id; }
    public String           getName()          { return name; }
    public Zone             getFromZone()      { return fromZone; }
    public Zone             getToZone()        { return toZone; }
    public List<GeoPosition>getWaypoints()     { return Collections.unmodifiableList(waypoints); }
    public int              getCapacityMax()   { return capacityMax; }
    public int              getCurrentFlow()   { return currentFlow; }
    public int              getFloodedCount()  { return floodedCount; }
    public EdgeState            getState()         { return state; }

    /** L'arête est-elle franchissable (pas inondée) ? */
    public boolean isCrossable()               { return state != EdgeState.FLOODED; }

    @Override
    public String toString() {
        return String.format("Edge[%d: %s → %s, state=%s, flow=%d/%d]",
            id, fromZone.getName(), toZone.getName(), state, currentFlow, capacityMax);
    }
}