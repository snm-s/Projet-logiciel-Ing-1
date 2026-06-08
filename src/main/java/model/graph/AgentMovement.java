package model.graph;

import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.algorithms.EvacuationPath;
import model.zone.Zone;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Suivi du déplacement d'un agent sur un chemin d'évacuation.
 *
 * <p>Chaque appel à {@link #step(double)} avance l'agent d'un incrément
 * proportionnel à sa vitesse. La position GPS courante est interpolée
 * sur le tracé réel (waypoints OSRM).
 *
 * <h3>États d'un déplacement :</h3>
 * <ul>
 *   <li>PENDING   : calculé, pas encore démarré</li>
 *   <li>MOVING    : en cours de déplacement</li>
 *   <li>ARRIVED   : arrivé à destination</li>
 *   <li>BLOCKED   : chemin devenu infranchissable en cours de route</li>
 * </ul>
 */
public class AgentMovement {

    public enum Status { PENDING, MOVING, ARRIVED, BLOCKED }

    // ─── Champs ───────────────────────────────────────────────────────────
    private final Agent          agent;
    private final EvacuationPath path;
    private double               progress;   // [0.0, 1.0]
    private Status               status;
    private GeoPosition          currentPosition;

    // Vitesse de déplacement en unités de "progress" par seconde simulée.
    // Valeur typique : 0.03 → traverse un chemin moyen en ~33 secondes simulées.
    private double speed;

    // ─────────────────────────────────────────────────────────────────────

    public AgentMovement(Agent agent, EvacuationPath path) {
        this.agent    = agent;
        this.path     = path;
        this.progress = 0.0;
        this.status   = Status.PENDING;
        this.speed    = computeSpeed(agent);
        this.currentPosition = path.interpolatePosition(0.0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // AVANCEMENT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Avance d'un pas de simulation.
     *
     * @param deltaSeconds durée du pas simulé en secondes (ex : 1.0)
     * @return {@code true} si l'agent vient d'arriver
     */
    public boolean step(double deltaSeconds) {
        if (status == Status.ARRIVED || status == Status.BLOCKED) return false;
        status = Status.MOVING;

        // Vérifier si le chemin est toujours praticable
        if (!isPathStillCrossable()) {
            status = Status.BLOCKED;
            return false;
        }

        progress = Math.min(1.0, progress + speed * deltaSeconds);
        currentPosition = path.interpolatePosition(progress);

        if (progress >= 1.0) {
            status = Status.ARRIVED;
            // Mettre à jour la position de l'agent sur le nœud d'arrivée
            updateAgentEdgeFlow(-1); // libérer la capacité
            return true;
        }

        return false;
    }

    /**
     * Démarre le déplacement et réserve la capacité sur les arêtes du chemin.
     */
    public void start() {
        if (status != Status.PENDING) return;
        status = Status.MOVING;
        updateAgentEdgeFlow(+1); // occuper la capacité
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────

    /** L'itinéraire est-il encore franchissable (aucune arête FLOODED) ? */
    private boolean isPathStillCrossable() {
        return path.getEdges().stream().allMatch(Edge::isCrossable);
    }

    /** Met à jour le flux courant sur toutes les arêtes du chemin. */
    private void updateAgentEdgeFlow(int delta) {
        for (Edge edge : path.getEdges()) {
            if (delta > 0) edge.addFlow(delta);
            else           edge.removeFlow(-delta);
        }
    }

    /**
     * Calcule la vitesse de déplacement selon le type d'agent et ses attributs.
     * - Citoyen normal    : vitesse = maxSpeed / 100
     * - Citoyen elderly   : × 0.6
     * - Citoyen child     : × 0.7
     * - RescueAgent       : × 1.5 (véhicule d'urgence)
     */
    private double computeSpeed(Agent a) {
        double base = (a.getMaxSpeed() > 0) ? a.getMaxSpeed() / 100.0 : 0.03;
        if (a instanceof Citizen) {
            Citizen c = (Citizen) a;
            String mob = c.getMobilityStatus();
            if ("elderly".equals(mob)) return base * 0.6;
            if ("child".equals(mob))   return base * 0.7;
        }
        if (a instanceof RescueAgent) return base * 1.5;
        return base;
    }

    // ─────────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────────

    public Agent          getAgent()           { return agent; }
    public EvacuationPath getPath()            { return path; }
    public double         getProgress()        { return progress; }
    public Status         getStatus()          { return status; }
    public GeoPosition    getCurrentPosition() { return currentPosition; }
    public boolean        isArrived()          { return status == Status.ARRIVED; }
    public boolean        isBlocked()          { return status == Status.BLOCKED; }
    public boolean        isMoving()           { return status == Status.MOVING; }

    /** Zone de destination finale. */
    public Zone getDestinationZone() { return path.getDestination(); }

    /** Zone d'origine. */
    public Zone getOriginZone() { return path.getOrigin(); }
}