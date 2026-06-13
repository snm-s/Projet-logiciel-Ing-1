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
    /**
     * Recompute the edge state and notify observers if the state changed.
     */
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

    /**
     * Sets the current flow.
     * @param flow the flow.
     */
    public void setCurrentFlow(int flow) {
        this.currentFlow = Math.max(0, flow);
        refreshState();
    }

    /**
     * Set the current flow (number of agents) on this edge.
     *
     * @param flow the current flow value (will be clamped to >= 0)
     */

    public void setCapacityMax(int ca) {capacityMax=ca;}

    /**
     * Set the maximum capacity for this edge.
     *
     * @param ca maximum capacity (agents/vehicles)
     */

    public void addFlow(int delta) {
        setCurrentFlow(this.currentFlow + delta);
    }

    /**
     * Increase the current flow by the given delta.
     *
     * @param delta amount to add to the current flow
     */

    public void removeFlow(int delta) {
        setCurrentFlow(Math.max(0, this.currentFlow - delta));
    }

    /**
     * Decrease the current flow by the given delta.
     *
     * @param delta amount to remove from the current flow
     */

    /** Vérifie si l'arête peut accepter des agents supplémentaires. */
    public boolean hasCapacity(int agentsToAdd) {
        return (currentFlow + agentsToAdd) <= capacityMax;
    }

    /**
     * Check whether the edge can accept the given number of additional agents.
     *
     * @param agentsToAdd number of additional agents
     * @return true if there is capacity, false otherwise
     */

    /** Capacité disponible restante. */
    public int availableCapacity() {
        return Math.max(0, capacityMax - currentFlow);
    }

    /**
     * Get the remaining available capacity on this edge.
     *
     * @return available capacity (>= 0)
     */

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

    /**
     * Refreshes waypoints.
     * @param changedZone the changedZone.
     * @param provider the provider.
     */
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
     * Refresh the cached waypoints for this edge when a related zone changed.
     *
     * @param changedZone the zone that changed
     * @param provider provider used to compute new waypoints
     */

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

    /**
     * Compute a routing cost optimized for rescue agents.
     *
     * @return the rescue routing cost (higher when flooded or congested)
     */

    /** Peut-on entrer dans l'arête à ce cycle ? */
    public boolean canEnter() {
        return isCrossable() && hasCapacity(1);
    }

    /**
     * Determine whether an ordinary agent can enter this edge now.
     *
     * @return true if the edge is crossable and has at least one free capacity
     */

    /** Peut-on entrer dans l'arête comme secouriste ? */
    public boolean canEnterAsRescue() {
        return hasCapacity(1);
    }

    /**
     * Determine whether a rescue agent can enter this edge now.
     * Rescue agents may enter flooded edges depending on policy.
     *
     * @return true if there is at least one capacity available
     */

    /** Statistiques : un agent a traversé cette arête. */
    public void recordPassage(double speed) {
        agentsPassed++;
        totalObservedSpeed += Math.max(0.0, speed);
    }

    /**
     * Record that an agent traversed this edge and update speed statistics.
     *
     * @param speed observed speed of the agent (km/h or relative unit)
     */

    public int getAgentsPassed() { return agentsPassed; }

    /**
     * Returns the average speed.
     * @return the double result.
     */
    public double getAverageSpeed() {
        return agentsPassed == 0 ? 0.0 : totalObservedSpeed / agentsPassed;
    }

    /**
     * Get the average observed speed of agents passing this edge.
     *
     * @return average speed, or 0.0 if no observations
     */

    public boolean isBidirectional() { return bidirectional; }
    /**
     * Sets the bidirectional.
     * @param bidirectional the bidirectional.
     */
    public void setBidirectional(boolean bidirectional) { this.bidirectional = bidirectional; }

    /**
     * Returns the speed factor.
     * @return the double result.
     */
    public double getSpeedFactor() { return speedFactor; }
    /**
     * Sets the speed factor.
     * @param speedFactor the speedFactor.
     */
    public void setSpeedFactor(double speedFactor) { this.speedFactor = Math.max(0.1, speedFactor); }

    /**
     * Returns the lanes.
     * @return the int result.
     */
    public int getLanes() { return lanes; }
    /**
     * Sets the lanes.
     * @param lanes the lanes.
     */
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

    /**
     * Compute an approximate geographical distance between the two zone centers.
     *
     * @return distance in kilometers
     */

    // ─────────────────────────────────────────────────────────────────────
    // OBSERVATEURS
    // ─────────────────────────────────────────────────────────────────────

    public interface EdgeObserver {
        void onEdgeStateChanged(Edge edge, EdgeState newState);
    }

    /**
     * Adds observer.
     * @param obs the obs.
     */
    public void addObserver(EdgeObserver obs)    { observers.add(obs); }
    /**
     * Removes observer.
     * @param obs the obs.
     */
    public void removeObserver(EdgeObserver obs) { observers.remove(obs); }

    /**
     * Add an observer to be notified when edge state changes.
     *
     * @param obs the observer to add
     */

    /**
     * Remove a previously added observer.
     *
     * @param obs the observer to remove
     */

    private void notifyObservers() {
        for (EdgeObserver obs : observers) obs.onEdgeStateChanged(this, state);
    }

    /**
     * Sets the flood level.
     * @param level the level.
     */
    public void setFloodLevel(double level) {
        double oldLevel = this.floodLevel;
    
        this.floodLevel = Math.max(0.0, Math.min(1.0, level));
    
        if (oldLevel < 1.0 && this.floodLevel >= 1.0) {
            this.floodedCount++;
        }
    
        refreshState();
    }

    /**
     * Set the flood level for this edge (0.0 to 1.0) and update state.
     *
     * @param level flood level between 0.0 and 1.0
     */
    
    public double getFloodLevel() {
        return floodLevel;
    }

    /**
     * Get the current flood level of this edge.
     *
     * @return flood level between 0.0 and 1.0
     */
    
    public void setFlooded(boolean flooded) {
        setFloodLevel(flooded ? 1.0 : 0.0);
    }

    /**
     * Mark the edge as fully flooded or not.
     *
     * @param flooded true to mark as flooded, false otherwise
     */
    
    public boolean isManuallyFlooded() {
        return floodLevel >= 1.0;
    }

    /**
     * Check whether the edge is marked as manually flooded.
     *
     * @return true if flood level >= 1.0
     */

    // ─────────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the id.
     * @return the int result.
     */
    public int              getId()            { return id; }
    /**
     * Returns the name.
     * @return the String.
     */
    public String           getName()          { return name; }
    /**
     * Returns the from zone.
     * @return the Zone.
     */
    public Zone             getFromZone()      { return fromZone; }
    /**
     * Returns the to zone.
     * @return the Zone.
     */
    public Zone             getToZone()        { return toZone; }
    public List<GeoPosition>getWaypoints()     { return Collections.unmodifiableList(waypoints); }
    /**
     * Returns the capacity max.
     * @return the int result.
     */
    public int              getCapacityMax()   { return capacityMax; }
    /**
     * Returns the current flow.
     * @return the int result.
     */
    public int              getCurrentFlow()   { return currentFlow; }
    /**
     * Returns the flooded count.
     * @return the int result.
     */
    public int              getFloodedCount()  { return floodedCount; }
    /**
     * Returns the state.
     * @return the EdgeState.
     */
    public EdgeState            getState()         { return state; }

    /** L'arête est-elle franchissable (pas inondée) ? */
    public boolean isCrossable()               { return state != EdgeState.FLOODED; }

    @Override
    /**
     * Performs string.
     * @return the String.
     */
    public String toString() {
        return String.format("Edge[%d: %s → %s, state=%s, flow=%d/%d]",
            id, fromZone.getName(), toZone.getName(), state, currentFlow, capacityMax);
    }

    @FunctionalInterface
    public interface WaypointProvider {
        List<GeoPosition> compute(Zone from, Zone to);
    }
}
