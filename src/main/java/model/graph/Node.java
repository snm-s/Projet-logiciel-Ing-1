package model.graph;

public class Node {
    private int id;
    private String name;
    private double x;
    private double y;
    private int maxCapacity;
    private boolean closed;

    public Node(int id, String name, double x, double y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.closed = false;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
