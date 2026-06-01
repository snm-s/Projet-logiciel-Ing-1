package model.agent;

import model.graph.Node;

public class Citizen extends Agent {
    private int age;
    private boolean hasPhone;

    public Citizen(int id, String name, Node position) {
        super(id, name, position);
    }
}
