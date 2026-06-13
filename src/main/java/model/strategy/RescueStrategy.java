package model.strategy;

import java.util.List;

import model.agent.Agent;
import model.agent.RescueAgent;
import model.enums.RescueState;
import model.graph.Node;
import model.zone.Zone;

public class RescueStrategy implements Strategy {

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

        if (agent instanceof RescueAgent) {
            ((RescueAgent) agent).setState(RescueState.EN_ROUTE);
        }

        Node flooded = Strategy.findNearestFloodedZone(agent.getPosition(), zones);
        if (flooded != null) {
            return flooded;
        }

        return Strategy.findNearestSafeZone(agent.getPosition(), zones);
    }
}
