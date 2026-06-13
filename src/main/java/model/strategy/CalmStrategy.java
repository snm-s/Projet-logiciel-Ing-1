package model.strategy;

import java.util.List;

import model.agent.Agent;
import model.agent.Citizen;
import model.enums.CitizenState;
import model.graph.Node;
import model.zone.Zone;

public class CalmStrategy implements Strategy {

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

        if (agent instanceof Citizen) {
            ((Citizen) agent).setState(CitizenState.CALM);
        }

        return Strategy.findHighestSafeZone(zones);
    }
}
