package model.graph;

public class Node {
    private int id;
    private double lat;
    private double lng;

    public Node() {
    }

    public Node(double lat, double lng) {
        this.id = 0;
        this.lat = lat;
        this.lng = lng;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}