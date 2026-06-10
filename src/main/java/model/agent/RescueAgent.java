package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.enums.RescueState;
import model.graph.Node;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RescueAgent extends Agent {
    private String teamType;
    private RescueState state;

    // Constructeur vide nécessaire pour Jackson
    public RescueAgent() {
        super();
        this.state = RescueState.UNAVAILABLE; // État par défaut
    }

    public RescueAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.state = RescueState.UNAVAILABLE; // État par défaut
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public RescueState getState() {
        return state;
    }

    public void setState(RescueState state) {
        this.state = state;
    }

    public boolean isAvailable() {
        // Renvoie true si l'équipe n'a pas de mission en cours
        return this.state == RescueState.AVAILABLE;
    }
}
