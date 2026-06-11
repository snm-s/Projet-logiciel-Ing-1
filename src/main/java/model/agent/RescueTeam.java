package model.agent;

import model.graph.Node;

public class RescueTeam extends Agent {
    private String teamType;

    public RescueTeam(int id, String firstName, String lastName, Node position) {
        // Appelle le constructeur à 4 arguments d'Agent
        super(id, firstName, lastName, position);
    }

    public boolean isIdle() {
        return getDestination() == null;
    }
}