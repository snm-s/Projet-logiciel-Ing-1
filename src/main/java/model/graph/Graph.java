package model.graph;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();

    public void addNode(Node n) { nodes.add(n); }
    public void addEdge(Edge e) { edges.add(e); }
    public List<Node> getNodes() { return nodes; }
    public List<Edge> getEdges() { return edges; }
}
