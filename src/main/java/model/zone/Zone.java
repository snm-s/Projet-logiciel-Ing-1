package model.zone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import model.observer.Observer;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Neighborhood.class, name = "neighborhood"),
    @JsonSubTypes.Type(value = Shelter.class,      name = "shelter")
})
public abstract class Zone {
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private double altitude;
    private int population;
    private String description;
    private boolean flooded;
    private boolean evacuated;

    @JsonIgnore
    private final List<Observer> observers = new ArrayList<>();


    
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
    public Zone() {}

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

    @JsonIgnore
    public boolean isFlooded() {
        return flooded;
    }

    @JsonIgnore
    public boolean isEvacuated() {
        return evacuated;
    }

    // Setters
    public void setFlooded(boolean flooded) {
        if (this.flooded != flooded) {
            this.flooded = flooded;
            notifyObservers();
        }
    }

    public void setEvacuated(boolean evacuated) {
        if (this.evacuated != evacuated) {
            this.evacuated = evacuated;
            notifyObservers();
        }
    }

    public void reset() {
        this.flooded = false;
        this.evacuated = false;
        notifyObservers();
    }


    public void addObserver(Observer o) {
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    private void notifyObservers() {
        for (Observer o : observers) {
            o.update(this);
        }
    }


}
