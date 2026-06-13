package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.graph.Node;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminAgent extends Agent {
    // Constructeur par défaut requis pour Jackson
    /**
     * Constructs a new AdminAgent.
     */
    public AdminAgent() {
        super(0, "System", "Admin", null);
    }

    /**
     * Constructs a new AdminAgent.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     */
    public AdminAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
    }

}
