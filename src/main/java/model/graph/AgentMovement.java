package model.graph;

import java.util.ArrayList;
import java.util.List;

import org.jxmapviewer.viewer.GeoPosition;

import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.algorithms.EvacuationPath;
import model.enums.CitizenState;
import model.zone.Zone;

/**
 * Déplacement temporel d'un agent sur un chemin Dijkstra.
 *
 * Règles :
 * - Citoyen : arête FLOODED interdite → blocage puis replanification.
 * - Citoyen : arête FLOODING/AT_RISK  → état STRESSED et vitesse accélérée.
 * - Secouriste : peut traverser rouge/orange, mais FLOODED est ralenti.
 *
 * Invariants :
 * - currentEdgeIndex  : index dans path.getEdges()  (-1 = hors arête)
 * - currentZoneIndex  : index dans path.getZones()  (dernier nœud atteint)
 * - occupiedEdge      : arête courante (flow comptabilisé), null si hors arête
 *   → occupiedEdge == path.getEdges().get(currentEdgeIndex) quand currentEdgeIndex >= 0
 *   → getOccupiedEdge() supprimé : utiliser getCurrentEdge() à la place
 */
public class AgentMovement {

    public enum Status { PENDING, WAITING, MOVING, ARRIVED, BLOCKED }

    private final Agent        agent;
    private final EvacuationPath path;

    private double       progress         = 0.0;
    private Status       status           = Status.PENDING;
    private GeoPosition  currentPosition;

    // Index dans path.getZones() — dernier nœud franchi
    private int          currentZoneIndex = 0;

    // Index dans path.getEdges() et arête occupée (flow +1)
    private int          currentEdgeIndex = -1;
    private Edge         occupiedEdge     = null;

    private final double baseSpeed;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Constructs a new AgentMovement.
     * @param agent the agent.
     * @param path the path.
     */
    public AgentMovement(Agent agent, EvacuationPath path) {
        this.agent           = agent;
        this.path            = path;
        this.baseSpeed       = computeBaseSpeed(agent);
        this.currentPosition = path != null ? path.interpolatePosition(0.0) : null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CYCLE DE VIE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Starts.
     */
    public void start() {
        if (status != Status.PENDING) return;

        if (path == null || path.isEmpty()) {
            status = Status.ARRIVED;
            return;
        }
        if (!tryEnterCurrentEdge()) {
            status = Status.WAITING;
            return;
        }
        status = Status.MOVING;
        updateAgentPosition(currentPosition);
    }
    /**
     * Advance the agent movement by one simulation step.
     *
     * @param deltaSeconds simulation time step duration (e.g. 1.0)
     * @return true if the agent arrived at destination during this step
     */
    public boolean step(double deltaSeconds) {
        if (status == Status.ARRIVED || status == Status.BLOCKED) return false;
        if (path == null) {
            status = Status.BLOCKED;
            return false;
        }

        // Vérifier que le chemin restant est encore praticable
        if (!isRemainingPathCrossable()) {
            releaseOccupiedEdge();
            status = Status.BLOCKED;
            return false;
        }

        // Tenter d'entrer dans l'arête courante (peut être WAITING si pleine)
        if (!tryEnterCurrentEdge()) {
            status = Status.WAITING;
            return false;
        }

        status = Status.MOVING;

        // Calcul du facteur de vitesse selon l'état de l'arête
        double factor = effectiveSpeedFactor();

        progress = Math.min(1.0, progress + baseSpeed * factor * deltaSeconds);
        currentPosition = path.interpolatePosition(progress);
        updateAgentPosition(currentPosition);
        updateEdgeOccupation();

        if (progress >= 1.0) {
            status = Status.ARRIVED;
            Zone destination = path.getDestination();
            if (destination != null) {
                currentZoneIndex = path.getZones().size() - 1;
                updateAgentPosition(
                    new GeoPosition(destination.getLatitude(), destination.getLongitude()));
            }
            releaseOccupiedEdge();
            return true;
        }

        return false;
    }

    // ─────────────────────────────────────────────────────────────────────
    // ARÊTE COURANTE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Tente d'entrer dans l'arête correspondant à la progression actuelle.
     * Met à jour occupiedEdge et currentEdgeIndex si réussi.
     *
     * @return true si l'arête est maintenant occupée (ou chemin sans arêtes)
     */
    private boolean tryEnterCurrentEdge() {
        if (path == null || path.getEdges().isEmpty()) return true;

        int   wantedIndex = edgeIndexForProgress();
        Edge  wanted      = path.getEdges().get(wantedIndex);

        // Déjà sur la bonne arête
        if (occupiedEdge == wanted) return true;

        // Vérifier l'autorisation d'entrée
        boolean canEnter = (agent instanceof RescueAgent)
            ? wanted.canEnterAsRescue()
            : wanted.canEnter();
        if (!canEnter) return false;

        // Libérer l'ancienne arête et occuper la nouvelle
        releaseOccupiedEdge();
        occupiedEdge     = wanted;
        currentEdgeIndex = wantedIndex;
        occupiedEdge.addFlow(1);
        return true;
    }

    /**
     * Appelé après chaque avancée de progress pour mettre à jour
     * currentZoneIndex lorsqu'on passe d'une arête à la suivante.
     */
    private void updateEdgeOccupation() {
        if (path == null || path.getEdges().isEmpty()) return;

        int wantedEdgeIndex = edgeIndexForProgress();
        if (wantedEdgeIndex == currentEdgeIndex) return;

        // On franchit un nœud intermédiaire : avancer currentZoneIndex
        // currentZoneIndex = index source de la nouvelle arête = wantedEdgeIndex
        // (arête i relie zones[i] → zones[i+1])
        int newZoneIndex = wantedEdgeIndex; // source de la prochaine arête

        Edge previousEdge = occupiedEdge;

        if (tryEnterCurrentEdge()) {
            // Enregistrer le passage sur l'arête qu'on vient de quitter
            if (previousEdge != null) {
                previousEdge.recordPassage(agent != null ? agent.getMaxSpeed() : 1.0);
            }
            currentZoneIndex = newZoneIndex;
        }
    }

    /**
     * Performs occupied edge.
     */
    private void releaseOccupiedEdge() {
        if (occupiedEdge != null) {
            occupiedEdge.removeFlow(1);
            occupiedEdge.recordPassage(agent != null ? agent.getMaxSpeed() : 1.0);
            occupiedEdge     = null;
        }
        currentEdgeIndex = -1;
    }

    // ─────────────────────────────────────────────────────────────────────
    // VITESSE ET ÉTAT AGENT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Facteur de vitesse combiné : état de l'arête + réaction comportementale.
     * Met aussi à jour l'état du Citizen si nécessaire.
     */
    private double effectiveSpeedFactor() {
        if (occupiedEdge == null) return 1.0;

        EdgeState st     = occupiedEdge.getState();
        double    factor = occupiedEdge.getSpeedFactor();

        if (agent instanceof Citizen c) {
            switch (st) {
                case FLOODING, AT_RISK -> {
                    c.setState(CitizenState.STRESSED);
                    factor *= 1.35;      // stress → fuite accélérée
                }
                case CONGESTED  -> factor *= 0.55;
                case OVERLOADED -> factor *= 0.35;
                default -> {
                    if (c.getState() != CitizenState.SAFE)
                        c.setState(CitizenState.ESCAPING);
                }
            }
        } else if (agent instanceof RescueAgent) {
            switch (st) {
                case FLOODED             -> factor *= 0.45;
                case FLOODING, AT_RISK   -> factor *= 0.80;
                default -> { /* vitesse nominale */ }
            }
        }

        return factor;
    }

    /**
     * Calcule la vitesse de base (progress/seconde) à partir de la vitesse
     * métier de l'agent (m/s ou km/h selon votre modèle).
     * La division par 70 est un facteur de normalisation sur la longueur
     * moyenne d'une arête ; à ajuster selon l'échelle de votre simulation.
     */
    private static double computeBaseSpeed(Agent a) {
        if (a == null) return 0.03;

        double base = a.getMaxSpeed() > 0 ? a.getMaxSpeed() / 70.0 : 0.045;

        if (a instanceof Citizen c) {
            String mob = String.valueOf(c.getMobilityStatus());
            if ("elderly".equalsIgnoreCase(mob)) return base * 0.6;
            if ("child".equalsIgnoreCase(mob))   return base * 0.7;
            if ("pmr".equalsIgnoreCase(mob))      return base * 0.5;
            if (a.getCongestionTolerance() < 0.6) return base * 0.85;
        }

        if (a instanceof RescueAgent) return base * 1.8;
        return base;
    }

    // ─────────────────────────────────────────────────────────────────────
    // VÉRIFICATION DU CHEMIN RESTANT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Vérifie uniquement les arêtes RESTANTES (depuis currentEdgeIndex).
     * FIX : l'ancienne version testait toutes les arêtes du chemin, y compris
     * celles déjà franchies — un retour en arrière impossible.
     */
    private boolean isRemainingPathCrossable() {
        if (agent instanceof RescueAgent) return true;
        if (path == null || path.getEdges().isEmpty()) return true;

        int startIdx = Math.max(0, currentEdgeIndex);
        return path.getEdges()
            .subList(startIdx, path.getEdges().size())
            .stream()
            .allMatch(Edge::isCrossable);
    }

    // ─────────────────────────────────────────────────────────────────────
    // POSITION AGENT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Met à jour la position géographique de l'agent.
     * Crée un Node minimal (sans id/name) pour les coordonnées de déplacement.
     * Les propriétés sémantiques (id, zone) restent inchangées sur l'agent.
     */
    private void updateAgentPosition(GeoPosition pos) {
        if (pos == null || agent == null) return;
        agent.setPosition(new Node(pos.getLatitude(), pos.getLongitude()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS INTERNES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Convertit la progression [0,1] en index d'arête.
     * progress=0 → arête 0, progress→1 → dernière arête.
     */
    private int edgeIndexForProgress() {
        int n   = Math.max(1, path.getEdges().size());
        int idx = (int) Math.floor(Math.min(0.999999, Math.max(0.0, progress)) * n);
        return Math.max(0, Math.min(n - 1, idx));
    }

    // ─────────────────────────────────────────────────────────────────────
    // API PUBLIQUE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Zones restantes à parcourir depuis la position courante.
     * Utilisé par le controller pour afficher le trajet de l'agent sélectionné.
     */
    public List<Zone> getRemainingZones() {
        if (path == null) return new ArrayList<>();
        List<Zone> all  = path.getZones();
        int        from = Math.max(0, currentZoneIndex);
        if (from >= all.size()) return new ArrayList<>();
        return new ArrayList<>(all.subList(from, all.size()));
    }

    /**
     * Get the remaining zones to traverse from the current position.
     *
     * @return list of remaining zones (may be empty)
     */

    /**
     * Arête dans laquelle l'agent progresse actuellement.
     * Remplace getOccupiedEdge() (supprimé) — même sémantique, nom cohérent
     * avec l'utilisation dans SimulationController.
     */
    public Edge getCurrentEdge() {
        if (path == null || currentEdgeIndex < 0
                || currentEdgeIndex >= path.getEdges().size()) return null;
        return path.getEdges().get(currentEdgeIndex);
    }

    /**
     * Get the edge the agent is currently progressing on, or null if none.
     *
     * @return the current Edge or null
     */

    /**
     * Zone source de l'arête courante, ou dernier nœud atteint.
     * Utilisé par SimulationController pour localiser l'agent sur la carte.
     */
    public Zone getCurrentZone() {
        if (path == null || path.getZones().isEmpty()) return null;
        // Si dans une arête : zone source = zones[currentEdgeIndex]
        if (currentEdgeIndex >= 0 && currentEdgeIndex < path.getEdges().size())
            return path.getEdges().get(currentEdgeIndex).getFromZone();
        // Sinon : dernier nœud franchi
        int zi = Math.min(currentZoneIndex, path.getZones().size() - 1);
        return path.getZones().get(zi);
    }

    /**
     * Get the source zone of the current edge or the last reached zone.
     *
     * @return the current Zone or null
     */

    /** Destination finale (dernier nœud du chemin). */
    public Zone getDestination() {
        if (path == null || path.getZones().isEmpty()) return null;
        return path.getZones().get(path.getZones().size() - 1);
    }

    /**
     * Get the final destination zone of this movement.
     *
     * @return the destination Zone or null
     */

    // Accesseurs
    /**
     * Returns the agent.
     * @return the Agent.
     */
    public Agent          getAgent()           { return agent; }
    /**
     * Returns the path.
     * @return the EvacuationPath.
     */
    public EvacuationPath getPath()            { return path; }
    /**
     * Returns the progress.
     * @return the double result.
     */
    public double         getProgress()        { return progress; }
    /**
     * Returns the status.
     * @return the Status.
     */
    public Status         getStatus()          { return status; }
    /**
     * Returns the current position.
     * @return the GeoPosition.
     */
    public GeoPosition    getCurrentPosition() { return currentPosition; }
    /**
     * Returns whether arrived.
     * @return the boolean result.
     */
    public boolean        isArrived()          { return status == Status.ARRIVED; }
    /**
     * Returns whether blocked.
     * @return the boolean result.
     */
    public boolean        isBlocked()          { return status == Status.BLOCKED; }
    /**
     * Returns whether moving.
     * @return the boolean result.
     */
    public boolean        isMoving()           { return status == Status.MOVING; }
    /**
     * Returns whether waiting.
     * @return the boolean result.
     */
    public boolean        isWaiting()          { return status == Status.WAITING; }
    /**
     * Returns the destination zone.
     * @return the Zone.
     */
    public Zone           getDestinationZone() { return path == null ? null : path.getDestination(); }
    /**
     * Returns the origin zone.
     * @return the Zone.
     */
    public Zone           getOriginZone()      { return path == null ? null : path.getOrigin(); }
}
