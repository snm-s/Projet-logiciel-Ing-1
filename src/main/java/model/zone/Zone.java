package model.zone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Neighborhood.class, name = "neighborhood"),
    @JsonSubTypes.Type(value = Shelter.class,      name = "shelter")
})
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
    
    
    // Constructeur requis par Jackson pour la désérialisation
    /**
     * Constructs a new Zone.
     */
    public Zone() {}

    // Getters
    /**
     * Returns the id.
     * @return the int result.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name.
     * @return the String.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the latitude.
     * @return the double result.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the longitude.
     * @return the double result.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the altitude.
     * @return the double result.
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * Returns the population.
     * @return the int result.
     */
    public int getPopulation() {
        return population;
    }

    /**
     * Returns the description.
     * @return the String.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the max capacity.
     * @return the int result.
     */
    public int getMaxCapacity() {
        return 50; // valeur par défaut dans Zone.java
    }

    @JsonIgnore
    /**
     * Returns whether flooded.
     * @return the boolean result.
     */
    public boolean isFlooded() {
        return flooded;
    }

    @JsonIgnore
    /**
     * Returns whether evacuated.
     * @return the boolean result.
     */
    public boolean isEvacuated() {
        return evacuated;
    }

    // Setters
    /**
     * Sets the flooded.
     * @param flooded the flooded.
     */
    public void setFlooded(boolean flooded) {
        this.flooded = flooded;
    }

    /**
     * Sets the evacuated.
     * @param evacuated the evacuated.
     */
    public void setEvacuated(boolean evacuated) {
        this.evacuated = evacuated;
    }
    /**
     * Sets the longitude.
     * @param l the l.
     */
    public void setLongitude(double l) {this.longitude=l;}
    /**
     * Sets the latitude.
     * @param l the l.
     */
    public void setLatitude(double l) {this.latitude=l;}
    /**
     * Sets the altitude.
     * @param l the l.
     */
    public void setAltitude(double l) {this.altitude=l;}
    /**
     * Sets the name.
     * @param nm the nm.
     */
    public void setName(String nm) {this.name = nm;}

    /**
     * Resets.
     */
    public void reset() {
        this.flooded = false;
        this.evacuated = false;
    }
}
