package model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jxmapviewer.viewer.GeoPosition;

import model.zone.Zone;

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
    private int    capacityMax;   // véhicules/heure
    private int          currentFlow=0;   // flux courant (agents en transit)
    private int          floodedCount;  // nombre de fois inondée (statistiques)
    private EdgeState        state;
    private double floodLevel = 0.0;

    // Propriétés de circulation demandées dans le sujet
    private boolean bidirectional = true;
    private double speedFactor = 1.0;
    private int lanes = 2;

    // Statistiques d'arête
    private int agentsPassed = 0;
    private double totalObservedSpeed = 0.0;
    

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

    // Rouge : arête totalement inondée
    if (floodLevel >= 1.0) {
        return EdgeState.FLOODED;
    }

    // Orange : l'eau commence à toucher l'arête
    if (floodLevel > 0.0) {
        return EdgeState.FLOODING;
    }

    // Orange/rouge si une zone reliée est touchée par l'eau
    boolean fromFlooded = fromZone.isFlooded();
    boolean toFlooded = toZone.isFlooded();

    if (fromFlooded && toFlooded) {
        return EdgeState.FLOODED;
    }

    if (fromFlooded || toFlooded) {
        return EdgeState.FLOODING;
    }

    // Congestion seulement si des agents sont vraiment dessus
    double flowRatio = capacityMax > 0 ? (double) currentFlow / capacityMax : 0.0;

    if (flowRatio > 1.0) return EdgeState.OVERLOADED;
    if (flowRatio > 0.8) return EdgeState.CONGESTED;
    if (flowRatio > 0.5) return EdgeState.AT_RISK;

    // Au démarrage : arête normale
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

    public void setCapacityMax(int ca) {capacityMax=ca;}

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
        if (state == EdgeState.FLOODED) return Double.MAX_VALUE; // rouge = interdit aux citoyens

        double dist = geoDistance();
        double congestionRatio = (capacityMax > 0) ? (double) currentFlow / capacityMax : 0.0;
        double statePenalty;

        switch (state) {
            case SAFE:
                statePenalty = 0.0;
                break;
            case FLOODING:
            case AT_RISK:
                statePenalty = dist * 2.5; // orange = possible, mais le Dijkstra préfère l'éviter
                break;
            case CONGESTED:
                statePenalty = dist * 4.0;
                break;
            case OVERLOADED:
                statePenalty = dist * 7.0;
                break;
            case FLOODED:
            default:
                return Double.MAX_VALUE;
        }

        return (dist / Math.max(0.1, speedFactor)) + statePenalty + dist * congestionRatio * 2.0;
    }

    public void refreshWaypoints(Zone changedZone, WaypointProvider provider) {
        if (changedZone == null || provider == null) return;

        boolean isRelated =
                fromZone.getId() == changedZone.getId()
            || toZone.getId() == changedZone.getId();

        if (!isRelated) return;

        List<GeoPosition> newWaypoints = provider.compute(fromZone, toZone);

        if (newWaypoints == null || newWaypoints.isEmpty()) return;

        this.waypoints.clear();
        this.waypoints.addAll(newWaypoints);
    }

    /**
     * Coût réservé aux secouristes : ils peuvent traverser une arête rouge,
     * mais très lentement et avec une forte pénalité. Cela évite de bloquer
     * définitivement les missions de secours.
     */
    public double rescueRoutingCost() {
        double dist = geoDistance();
        double congestionRatio = (capacityMax > 0) ? (double) currentFlow / capacityMax : 0.0;

        return switch (state) {
            case SAFE -> dist / Math.max(0.1, speedFactor);
            case FLOODING, AT_RISK -> dist * 2.0;
            case CONGESTED -> dist * 3.0;
            case OVERLOADED -> dist * 5.0;
            case FLOODED -> dist * 8.0; // autorisé pour secours, mais dangereux/lent
        } + dist * congestionRatio;
    }

    /** Peut-on entrer dans l'arête à ce cycle ? */
    public boolean canEnter() {
        return isCrossable() && hasCapacity(1);
    }

    /** Peut-on entrer dans l'arête comme secouriste ? */
    public boolean canEnterAsRescue() {
        return hasCapacity(1);
    }

    /** Statistiques : un agent a traversé cette arête. */
    public void recordPassage(double speed) {
        agentsPassed++;
        totalObservedSpeed += Math.max(0.0, speed);
    }

    public int getAgentsPassed() { return agentsPassed; }

    public double getAverageSpeed() {
        return agentsPassed == 0 ? 0.0 : totalObservedSpeed / agentsPassed;
    }

    public boolean isBidirectional() { return bidirectional; }
    public void setBidirectional(boolean bidirectional) { this.bidirectional = bidirectional; }

    public double getSpeedFactor() { return speedFactor; }
    public void setSpeedFactor(double speedFactor) { this.speedFactor = Math.max(0.1, speedFactor); }

    public int getLanes() { return lanes; }
    public void setLanes(int lanes) { this.lanes = Math.max(1, lanes); }

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

    public void setFloodLevel(double level) {
        double oldLevel = this.floodLevel;
    
        this.floodLevel = Math.max(0.0, Math.min(1.0, level));
    
        if (oldLevel < 1.0 && this.floodLevel >= 1.0) {
            this.floodedCount++;
        }
    
        refreshState();
    }
    
    public double getFloodLevel() {
        return floodLevel;
    }
    
    public void setFlooded(boolean flooded) {
        setFloodLevel(flooded ? 1.0 : 0.0);
    }
    
    public boolean isManuallyFlooded() {
        return floodLevel >= 1.0;
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

    @FunctionalInterface
    public interface WaypointProvider {
        List<GeoPosition> compute(Zone from, Zone to);
    }
}
