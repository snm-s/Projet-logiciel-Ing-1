package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.graph.Node;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminAgent extends Agent {
    // Constructeur par défaut requis pour Jackson
    public AdminAgent() {
        super(0, "System", "Admin", null);
    }

    public AdminAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
    }

    @Override
    public double getSpeed() {
        throw new UnsupportedOperationException("L'admin n'as pas de vitesse");
    }

}