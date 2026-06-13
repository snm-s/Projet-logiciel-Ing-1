package model.zone;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Shelter extends Zone {
    private int capacity;
    private int currentOccupancy;

    /**
     * Constructs a new Shelter.
     * @param id the id.
     * @param name the name.
     * @param latitude the latitude.
     * @param longitude the longitude.
     * @param altitude the altitude.
     * @param population the population.
     * @param description the description.
     * @param capacity the capacity.
     */
    public Shelter(int id, String name, double latitude, double longitude, double altitude, int population, String description, int capacity) {
        super(id, name, latitude, longitude, altitude, population, description);
        this.capacity = capacity;
        this.currentOccupancy = 0;
    }
    
    /**
     * Constructs a new Shelter.
     */
    public Shelter() {super();}

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
    /**
     * Sets the capacity.
     * @param ca the ca.
     */
    public void setCapacity(int ca) {capacity=ca;}

    /**
     * Returns the capacity.
     * @return the int result.
     */
    public int getCapacity() { return capacity; }
    @JsonIgnore public int getCurrentOccupancy() { return currentOccupancy; }
    /**
     * Performs accommodate.
     * @param numberOfPeople the numberOfPeople.
     * @return the boolean result.
     */
    public boolean canAccommodate(int numberOfPeople) { return (currentOccupancy + numberOfPeople) <= capacity; }
    /**
     * Performs accommodate.
     * @param numberOfPeople the numberOfPeople.
     */
    public void accommodate(int numberOfPeople) {
        if (canAccommodate(numberOfPeople)) {
            currentOccupancy += numberOfPeople;
        } else {
            throw new IllegalArgumentException("Not enough capacity to accommodate " + numberOfPeople + " people.");
        }
    }
}
