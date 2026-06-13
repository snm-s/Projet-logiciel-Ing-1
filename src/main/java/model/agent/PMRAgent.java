package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.graph.Node;
import model.zone.Zone;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PMRAgent extends Citizen{
    
    
    // Constructeur vide nécessaire pour Jackson
    public PMRAgent() {
        super();
    }
    
    public PMRAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.setMobilityStatus("pmr");
    }

    public PMRAgent(int id,String firstName,String lastName, Node position,Zone currentZone) {
        super(id, firstName, lastName, position, currentZone);
        this.setMobilityStatus("pmr");
    }


}
