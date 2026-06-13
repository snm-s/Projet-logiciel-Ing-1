package model.agent;

import model.graph.Node;

public class RescueTeam extends Agent {
    private String teamType;

    /**
     * Constructs a new RescueTeam.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     */
    public RescueTeam(int id, String firstName, String lastName, Node position) {
        // Appelle le constructeur à 4 arguments d'Agent
        super(id, firstName, lastName, position);
    }

    /**
     * Returns whether idle.
     * @return the boolean result.
     */
    public boolean isIdle() {
        return getDestination() == null;
    }
}
