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
 * - Citoyen : arête rouge/FLOODED interdite → blocage puis replanification.
 * - Citoyen : arête orange/FLOODING/AT_RISK → état STRESSED et vitesse accélérée.
 * - Secouriste : peut traverser rouge/orange, mais rouge est ralenti.
 */
public class AgentMovement {

    public enum Status { PENDING, WAITING, MOVING, ARRIVED, BLOCKED }

    private final Agent agent;
    private final EvacuationPath path;

    private double progress;
    private Status status;
    private GeoPosition currentPosition;
    private int currentZoneIndex = 0;
    private double speed;


    private int currentEdgeIndex = -1;
    private Edge occupiedEdge = null;

    public AgentMovement(Agent agent, EvacuationPath path) {
        this.agent = agent;
        this.path = path;
        this.progress = 0.0;
        this.status = Status.PENDING;
        this.speed = computeSpeed(agent);
        this.currentPosition = path != null ? path.interpolatePosition(0.0) : null;
    }

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

    public boolean step(double deltaSeconds) {
        if (status == Status.ARRIVED || status == Status.BLOCKED) return false;
        if (path == null) {
            status = Status.BLOCKED;
            return false;
        }

        if (!isPathStillCrossable()) {
            releaseOccupiedEdge();
            status = Status.BLOCKED;
            return false;
        }

        if (!tryEnterCurrentEdge()) {
            status = Status.WAITING;
            return false;
        }

        status = Status.MOVING;

        double factor = occupiedEdge != null ? occupiedEdge.getSpeedFactor() : 1.0;
        if (occupiedEdge != null) {
            EdgeState st = occupiedEdge.getState();

            if (agent instanceof Citizen c) {
                if (st == EdgeState.FLOODING || st == EdgeState.AT_RISK) {
                    c.setState(CitizenState.STRESSED);
                    factor *= 1.35; // stress = fuite plus rapide
                } else if (c.getState() != CitizenState.SAFE) {
                    c.setState(CitizenState.ESCAPING);
                }
                if (st == EdgeState.CONGESTED) factor *= 0.55;
                if (st == EdgeState.OVERLOADED) factor *= 0.35;
            }

            if (agent instanceof RescueAgent) {
                if (st == EdgeState.FLOODED) factor *= 0.45;
                if (st == EdgeState.FLOODING || st == EdgeState.AT_RISK) factor *= 0.80;
            }
        }

        progress = Math.min(1.0, progress + speed * factor * deltaSeconds);
        currentPosition = path.interpolatePosition(progress);
        updateAgentPosition(currentPosition);
        updateEdgeOccupation();

        if (progress >= 1.0) {
            status = Status.ARRIVED;
            Zone destination = path.getDestination();
            if (destination != null) {
                updateAgentPosition(new GeoPosition(destination.getLatitude(), destination.getLongitude()));
            }
            releaseOccupiedEdge();
            return true;
        }

        return false;
    }

    private boolean tryEnterCurrentEdge() {
        if (path == null || path.getEdges().isEmpty()) return true;

        int wantedIndex = edgeIndexForProgress();
        Edge wanted = path.getEdges().get(wantedIndex);

        if (occupiedEdge == wanted) return true;

        boolean canEnter = agent instanceof RescueAgent
                ? wanted.canEnterAsRescue()
                : wanted.canEnter();
        if (!canEnter) return false;

        releaseOccupiedEdge();
        occupiedEdge = wanted;
        currentEdgeIndex = wantedIndex;
        occupiedEdge.addFlow(1);
        return true;
    }

    private void updateEdgeOccupation() {
        if (path == null || path.getEdges().isEmpty()) return;

        int wantedIndex = edgeIndexForProgress();
        if (wantedIndex != currentEdgeIndex) {
            Edge old = occupiedEdge;
            currentZoneIndex = wantedIndex;
            if (tryEnterCurrentEdge() && old != null) {
                old.recordPassage(agent != null ? agent.getMaxSpeed() : 1.0);
            }
        }
    }


    /** Retourne les zones restantes (depuis la position courante). */
    public List<Zone> getRemainingZones() {
        if (path == null) return new ArrayList<>();
        List<Zone> all = path.getZones();
        int from = Math.max(0, currentZoneIndex);
        if (from >= all.size()) return new ArrayList<>();
        return new ArrayList<>(all.subList(from, all.size()));
    }

    private int edgeIndexForProgress() {
        int n = Math.max(1, path.getEdges().size());
        int idx = (int) Math.floor(Math.min(0.999999, Math.max(0.0, progress)) * n);
        return Math.max(0, Math.min(n - 1, idx));
    }

    private void releaseOccupiedEdge() {
        if (occupiedEdge != null) {
            occupiedEdge.removeFlow(1);
            occupiedEdge.recordPassage(agent != null ? agent.getMaxSpeed() : 1.0);
            occupiedEdge = null;
        }
        currentEdgeIndex = -1;
    }

    private void updateAgentPosition(GeoPosition pos) {
        if (pos != null && agent != null) {
            agent.setPosition(new Node(pos.getLatitude(), pos.getLongitude()));
        }
    }

    private boolean isPathStillCrossable() {
        if (agent instanceof RescueAgent) return true;
        return path.getEdges().stream().allMatch(Edge::isCrossable);
    }

    private double computeSpeed(Agent a) {
        if (a == null) return 0.03;
        double base = a.getMaxSpeed() > 0 ? a.getMaxSpeed() / 70.0 : 0.045;

        if (a instanceof Citizen c) {
            String mob = c.getMobilityStatus();
            if ("elderly".equalsIgnoreCase(String.valueOf(mob))) return base * 0.6;
            if ("child".equalsIgnoreCase(String.valueOf(mob))) return base * 0.7;
            if (a.getCongestionTolerance() < 0.6) return base * 0.85;
        }

        if (a instanceof RescueAgent) return base * 1.8;
        return base;
    }




    /** Retourne l'arête dans laquelle l'agent progresse actuellement, ou null. */
    public Edge getCurrentEdge() {
        // À adapter selon l'implémentation interne de AgentMovement.
        // Exemple si l'état interne maintient currentEdgeIndex et path.getEdges() :
        if (path == null || currentEdgeIndex < 0 || currentEdgeIndex >= path.getEdges().size())
            return null;
        return path.getEdges().get(currentEdgeIndex);
    }
    
    /** Retourne la zone où se trouve l'agent (nœud courant ou source de l'arête). */
    public Zone getCurrentZone() {
        if (path == null || path.getZones().isEmpty()) return null;
        // Si l'agent est dans une arête, on retourne la zone source de cette arête.
        if (currentEdgeIndex >= 0 && currentEdgeIndex < path.getEdges().size()) {
            return path.getEdges().get(currentEdgeIndex).getFromZone();
        }
        // Sinon on retourne le dernier nœud atteint.
        int zi = Math.min(currentZoneIndex, path.getZones().size() - 1);
        return path.getZones().get(zi);
    }
    
    /** Retourne la destination finale du chemin. */
    public Zone getDestination() {
        if (path == null || path.getZones().isEmpty()) return null;
        return path.getZones().get(path.getZones().size() - 1);
    }




    public Agent getAgent() { return agent; }
    public EvacuationPath getPath() { return path; }
    public double getProgress() { return progress; }
    public Status getStatus() { return status; }
    public GeoPosition getCurrentPosition() { return currentPosition; }
    public boolean isArrived() { return status == Status.ARRIVED; }
    public boolean isBlocked() { return status == Status.BLOCKED; }
    public boolean isMoving() { return status == Status.MOVING; }
    public boolean isWaiting() { return status == Status.WAITING; }
    public Zone getDestinationZone() { return path == null ? null : path.getDestination(); }
    public Zone getOriginZone() { return path == null ? null : path.getOrigin(); }
    public Edge getOccupiedEdge() { return occupiedEdge; }
}
