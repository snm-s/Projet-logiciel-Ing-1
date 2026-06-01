package model.agent;

import model.enums.AgentState;
import model.graph.Node;

public abstract class Agent {
    private int id;
    private String name;
    private Node position;
    private Node destination;
    private double maxSpeed;
    private AgentState state;
    private double congestionTolerance;

    public Agent(int id, String name, Node position) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.state = AgentState.CALME;
    }

    // Getters et Setters basiques pour que le code compile
    public int getId() { return id; }
    public String getName() { return name; }
    public Node getPosition() { return position; }
    public void setPosition(Node position) { this.position = position; }
    public Node getDestination() { return destination; }
    public void setDestination(Node destination) { this.destination = destination; }
    public AgentState getState() { return state; }
    public void setState(AgentState state) { this.state = state; }
    
    public boolean isSaved() {
        // Un agent est sauvé s'il est arrivé sur un nœud de type REFUGE (on complétera après)
        return false; 
    }
}
