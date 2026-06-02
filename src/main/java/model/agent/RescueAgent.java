package model.agent;

import model.graph.Node;

public class RescueAgent extends Agent {
    private String teamType;

    public RescueAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
    }

    public boolean isIdle() {
        // Renvoie true si l'équipe n'a pas de mission en cours
        return getDestination() == null;
    }
}
