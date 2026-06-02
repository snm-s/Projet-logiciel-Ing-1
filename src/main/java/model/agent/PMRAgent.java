package model.agent;

import model.graph.Node;

public class PMRAgent extends Citizen{
    public PMRAgent(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.setMobilityStatus("pmr");
    }


}
