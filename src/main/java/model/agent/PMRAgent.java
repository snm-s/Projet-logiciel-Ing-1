package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.graph.Node;
import model.zone.Zone;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PMRAgent extends Citizen{
    
    
    // Constructeur vide nécessaire pour Jackson
    /**
     * Constructs a new PMRAgent.
     */
    public PMRAgent() {
        super();
    }
    
    /**
     * Constructs a new PMRAgent.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     */
    public PMRAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.setMobilityStatus("pmr");
    }

    /**
     * Constructs a new PMRAgent.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     * @param currentZone the currentZone.
     */
    public PMRAgent(int id,String firstName,String lastName, Node position,Zone currentZone) {
        super(id, firstName, lastName, position, currentZone);
        this.setMobilityStatus("pmr");
    }


}
