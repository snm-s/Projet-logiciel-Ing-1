package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.enums.RescueState;
import model.graph.Node;
import model.zone.Zone;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RescueAgent extends Agent {
    private String teamType;
    private RescueState state;


    /**
     * Returns the team type.
     * @return the String.
     */
    public String getTeamType() {
        return teamType;
    }

    /**
     * Sets the team type.
     * @param teamType the teamType.
     */
    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

  
    /**
     * Returns the state.
     * @return the RescueState.
     */
    public RescueState getState() {
        return state;
    }

    /**
     * Sets the state.
     * @param state the state.
     */
    public void setState(RescueState state) {
        this.state = state;
    }






    // Constructeur vide nécessaire pour Jackson
    /**
     * Constructs a new RescueAgent.
     */
    public RescueAgent() {
        super();
        this.state = RescueState.DISPONIBLE; // État par défaut
    }
    
    /**
     * Constructs a new RescueAgent.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     */
    public RescueAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.state = RescueState.DISPONIBLE; // État par défaut
    }

        /**
         * Constructs a new RescueAgent.
         * @param id the id.
         * @param firstName the firstName.
         * @param lastName the lastName.
         * @param position the position.
         * @param currentZone the currentZone.
         */
        public RescueAgent(int id,String firstName,String lastName, Node position,Zone currentZone) {
        super(id, firstName, lastName, position, currentZone);
        this.state = RescueState.INDISPONIBLE;
    }


    /**
     * Returns whether idle.
     * @return the boolean result.
     */
    public boolean isIdle() {
        // Renvoie true si l'équipe n'a pas de mission en cours
        return getDestination() == null;
    }
}
