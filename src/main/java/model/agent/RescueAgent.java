package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.enums.RescueState;
import model.graph.Node;
import model.zone.Zone;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RescueAgent extends Agent {
    private String teamType;
    private RescueState state;


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






    // Constructeur vide nécessaire pour Jackson
    public RescueAgent() {
        super();
        this.state = RescueState.DISPONIBLE; // État par défaut
    }
    
    public RescueAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.state = RescueState.DISPONIBLE; // État par défaut
    }

        public RescueAgent(int id,String firstName,String lastName, Node position,Zone currentZone) {
        super(id, firstName, lastName, position, currentZone);
        this.state = RescueState.INDISPONIBLE;
    }


    public boolean isIdle() {
        // Renvoie true si l'équipe n'a pas de mission en cours
        return getDestination() == null;
    }
}
