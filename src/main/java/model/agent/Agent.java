
package model.agent;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import model.graph.Node;
import model.strategy.Strategy;
import model.zone.Zone;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Citizen.class, name = "citizen"),
    @JsonSubTypes.Type(value = RescueAgent.class, name = "rescueAgent"),
    @JsonSubTypes.Type(value = AdminAgent.class, name = "admin")
})

public abstract class Agent {
    private int id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String passwordHash;
    private String address;
    private String city;
    private String country;

    private Node position;
    private Node destination;
    private double maxSpeed;
    private double congestionTolerance;
    private Strategy strategy;



    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Agent() {
        this.maxSpeed = 3.0;
        this.congestionTolerance = 1.0;
    }

    public Agent(int id, String firstName, String lastName, Node position) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.maxSpeed = 3.0;
        this.congestionTolerance = 1.0;
    }

    // Getters et Setters basiques pour que le code compile
    public int getId() {
         return id;
         }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Node getPosition() { return position; }
    public void setPosition(Node position) { this.position = position; }
    public Node getDestination() { return destination; }
    public void setDestination(Node destination) { this.destination = destination; }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public void decideDestination(List<Zone> zones) {
        if (strategy != null) {
            strategy.apply(this, zones);
        }
    }

    public boolean hasStrategy() {
        return strategy != null;
    }
    
    

    public double getMaxSpeed() { 
        return maxSpeed; 
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getCongestionTolerance() {
        return congestionTolerance;
    }

    public void setCongestionTolerance(double congestionTolerance) {
        this.congestionTolerance = congestionTolerance;
    }

}
