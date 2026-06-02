package model.graph;

public class Node {
    private double lat;
    private double lng;

    public Node() {
    }

    public Node(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    // Getters et Setters
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}