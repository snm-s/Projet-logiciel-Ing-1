package model.strategy;

import java.util.List;

import model.agent.Agent;
import model.agent.Citizen;
import model.enums.CitizenState;
import model.graph.Node;
import model.zone.Zone;

public class CalmStrategy implements Strategy {

    @Override
    public Node chooseDestination(Agent agent, List<Zone> zones) {

        if (agent.getDestination() != null) {
            return agent.getDestination();
        }

        return Strategy.findNearestSafeZone(agent.getPosition(), zones);
    }
}
