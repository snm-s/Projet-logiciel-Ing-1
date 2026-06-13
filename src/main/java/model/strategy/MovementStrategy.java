package model.strategy;

import java.util.List;

import model.agent.Agent;
import model.graph.Node;
import model.zone.Zone;

public class MovementStrategy implements Strategy {

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

        Node currentDestination = agent.getDestination();
        if (currentDestination != null) {
            boolean stillSafe = zones.stream()
                    .filter(zone -> !zone.isFlooded())
                    .map(zone -> new Node(zone.getLatitude(), zone.getLongitude()))
                    .anyMatch(node -> node.getLat() == currentDestination.getLat()
                            && node.getLng() == currentDestination.getLng());
            if (stillSafe) {
                return currentDestination;
            }
        }

        return Strategy.findNearestSafeZone(agent.getPosition(), zones);
    }
}
