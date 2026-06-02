package model.agent;

import model.graph.Node;

public class AdminAgent extends Agent {
    // Constructeur par défaut requis pour Jackson
    public AdminAgent() {
        super(0, "System", "Admin", null);
    }

    public AdminAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
    }
}