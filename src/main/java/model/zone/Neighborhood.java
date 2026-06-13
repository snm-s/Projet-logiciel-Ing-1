package model.zone;

public class Neighborhood extends Zone {
    public Neighborhood(int id, String name, double latitude, double longitude, double altitude, int population, String description) {
        super(id, name, latitude, longitude, altitude, population, description);
    }
    public Neighborhood() {
        super();
    }
    public void setPopulation(int pop) {this.population=pop;}
    public void setDescription(String des) {this.description=des;}

}