package model.strategy;

import java.util.List;

import model.agent.Agent;
import model.graph.Node;
import model.zone.Zone;

public class PMRStrategy implements Strategy {

    @Override
    /**
     * Performs destination.
     * @param agent the agent.
     * @param zones the zones.
     * @return the Node.
     */
    public Node chooseDestination(Agent agent, List<Zone> zones) {
        if (agent == null || zones == null || zones.isEmpty()) {
            return null;
        }

        if (agent.getDestination() != null) {
            return agent.getDestination();
        }

        // Les PMR utilisent temporairement le même comportement
// d'évacuation que les autres citoyens.
        Node target = Strategy.findNearestSafeZone(agent.getPosition(), zones);
        return target != null ? target : Strategy.findHighestSafeZone(zones);
    }
}
