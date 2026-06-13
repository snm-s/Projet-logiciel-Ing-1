package model.zone;

public class Neighborhood extends Zone {
    /**
     * Constructs a new Neighborhood.
     * @param id the id.
     * @param name the name.
     * @param latitude the latitude.
     * @param longitude the longitude.
     * @param altitude the altitude.
     * @param population the population.
     * @param description the description.
     */
    public Neighborhood(int id, String name, double latitude, double longitude, double altitude, int population, String description) {
        super(id, name, latitude, longitude, altitude, population, description);
    }
    /**
     * Constructs a new Neighborhood.
     */
    public Neighborhood() {
        super();
    }
    /**
     * Sets the population.
     * @param pop the pop.
     */
    public void setPopulation(int pop) {this.population=pop;}
    /**
     * Sets the description.
     * @param des the des.
     */
    public void setDescription(String des) {this.description=des;}

}
