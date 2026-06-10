package model.agent;

import model.graph.Node;
import java.util.ArrayList;
import java.util.List;

public class RescueTeam {

    private int id;
    private String teamType;
    private Node position;

    private List<RescueAgent> members = new ArrayList<>();

    public RescueTeam(int id, String teamType, Node position) {
        this.id = id;
        this.teamType = teamType;
        this.position = position;
    }

    
    /*
    public boolean isAvailable() {
        return members.isEmpty(); // ou logique de mission
    }
    */

    public void addMember(RescueAgent r) {
        members.add(r);
    }

    public void removeMember(RescueAgent r) {
        members.remove(r);
    }

    public List<RescueAgent> getMembers() {
        return members;
    }
}