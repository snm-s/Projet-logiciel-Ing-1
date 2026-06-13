package model.graph;

public class Node {
    private int id;
    private double lat;
    private double lng;

    /**
     * Constructs a new Node.
     */
    public Node() {
    }

    /**
     * Constructs a new Node.
     * @param lat the lat.
     * @param lng the lng.
     */
    public Node(double lat, double lng) {
        this.id = 0;
        this.lat = lat;
        this.lng = lng;
    }

    // Getters et Setters
    /**
     * Returns the id.
     * @return the int result.
     */
    public int getId() { return id; }
    /**
     * Sets the id.
     * @param id the id.
     */
    public void setId(int id) { this.id = id; }
    /**
     * Returns the lat.
     * @return the double result.
     */
    public double getLat() { return lat; }
    /**
     * Sets the lat.
     * @param lat the lat.
     */
    public void setLat(double lat) { this.lat = lat; }
    /**
     * Returns the lng.
     * @return the double result.
     */
    public double getLng() { return lng; }
    /**
     * Sets the lng.
     * @param lng the lng.
     */
    public void setLng(double lng) { this.lng = lng; }
}
