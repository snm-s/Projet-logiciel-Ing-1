package model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.graph.Node;

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


}
