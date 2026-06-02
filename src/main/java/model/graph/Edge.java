package model.graph;

import model.enums.EdgeState;

public class Edge {
    private int id;
    private Node source;
    private Node destination;
    private double distance;
    private EdgeState state;

    public Edge(int id, Node source, Node destination) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.state = EdgeState.ACCESSIBLE;
    }
}
