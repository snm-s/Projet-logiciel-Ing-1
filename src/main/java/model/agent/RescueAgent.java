package model.agent;

import model.graph.Node;

public class RescueTeam extends Agent {
    private String teamType;

    public RescueTeam(int id, String name, Node position) {
        super(id, name, position);
    }

    public boolean isIdle() {
        // Renvoie true si l'équipe n'a pas de mission en cours
        return getDestination() == null;
    }
}
