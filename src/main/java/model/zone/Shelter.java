package model.zone;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Shelter extends Zone {
    private int capacity;
    private int currentOccupancy;

    public Shelter(int id, String name, double latitude, double longitude, double altitude, int population, String description, int capacity) {
        super(id, name, latitude, longitude, altitude, population, description);
        this.capacity = capacity;
        this.currentOccupancy = 0;
    }
    
    public Shelter() {super();}

    public void setPopulation(int pop) {this.population=pop;}
    public void setDescription(String des) {this.description=des;}
    public void setCapacity(int ca) {capacity=ca;}

    public int getCapacity() { return capacity; }
    @JsonIgnore public int getCurrentOccupancy() { return currentOccupancy; }
    public boolean canAccommodate(int numberOfPeople) { return (currentOccupancy + numberOfPeople) <= capacity; }
    public void accommodate(int numberOfPeople) {
        if (canAccommodate(numberOfPeople)) {
            currentOccupancy += numberOfPeople;
        } else {
            throw new IllegalArgumentException("Not enough capacity to accommodate " + numberOfPeople + " people.");
        }
    }
}