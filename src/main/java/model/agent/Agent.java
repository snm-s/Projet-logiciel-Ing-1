
package model.agent;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

import model.enums.AgentState;
import model.graph.Node;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Citizen.class, name = "citizen"),
    @JsonSubTypes.Type(value = PMRAgent.class, name = "pmr"),
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
    private AgentState state;
    private double congestionTolerance;

    

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

    public Agent() {}

    public Agent(int id, String firstName, String lastName, Node position) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.state = AgentState.CALME;
    }

    // Getters et Setters basiques pour que le code compile
    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Node getPosition() { return position; }
    public void setPosition(Node position) { this.position = position; }
    public Node getDestination() { return destination; }
    public void setDestination(Node destination) { this.destination = destination; }
    public AgentState getState() { return state; }
    public void setState(AgentState state) { this.state = state; }
    
    public boolean isSaved() {
        // Un agent est sauvé s'il est arrivé sur un nœud de type REFUGE (on complétera après)
        return false; 
    }

}
