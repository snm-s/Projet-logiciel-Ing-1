package model.zone;

public abstract class Zone {
    protected int id;
    protected String name;
    protected double latitude;
    protected double longitude;
    protected double altitude;
    protected int population;
    protected String description;
    protected boolean flooded;
    protected boolean evacuated;

    public Zone(int id, String name, double latitude, double longitude, double altitude, int population,
            String description) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.population = population;
        this.description = description;
        this.flooded = false;
        this.evacuated = false;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public int getPopulation() {
        return population;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFlooded() {
        return flooded;
    }

    public boolean isEvacuated() {
        return evacuated;
    }

    // Setters
    public void setFlooded(boolean flooded) {
        this.flooded = flooded;
    }

    public void setEvacuated(boolean evacuated) {
        this.evacuated = evacuated;
    }

    public void reset() {
        this.flooded = false;
        this.evacuated = false;
    }
}
