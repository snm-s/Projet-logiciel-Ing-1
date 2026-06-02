package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.graph.Node;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RescueAgent extends Agent {
    private String teamType;

    // Constructeur vide nécessaire pour Jackson
    public RescueAgent() {
        super();
    }
    
    public RescueAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
    }

    public boolean isIdle() {
        // Renvoie true si l'équipe n'a pas de mission en cours
        return getDestination() == null;
    }
}
