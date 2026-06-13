
package model.agent;

import java.time.LocalDate;
import java.util.List;
import java.time.Period;

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
    private Zone currentZone;


    private Node destination;
    private double maxSpeed;
    private double congestionTolerance;
    private Strategy strategy;



    /**
     * Sets the first name.
     * @param firstName the firstName.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Set the agent's first name.
     *
     * @param firstName the first name to set
     */

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Set the agent's last name.
     *
     * @param lastName the last name to set
     */

    public LocalDate getBirthDate() {
        return birthDate;
    }

    /**
     * Get the agent's birth date.
     *
     * @return the birth date or null if not set
     */

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * Set the agent's birth date.
     *
     * @param birthDate the birth date to set
     */

    public String getEmail() {
        return email;
    }

    /**
     * Get the agent's email address.
     *
     * @return the email address or null
     */

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Set the agent's email address.
     *
     * @param email the email to set
     */

    public String getPhone() {
        return phone;
    }

    /**
     * Get the agent's phone number.
     *
     * @return the phone number or null
     */

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Set the agent's phone number.
     *
     * @param phone the phone number to set
     */

    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Get the stored password hash for the agent.
     *
     * @return the password hash or null
     */

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Set the stored password hash for the agent.
     *
     * @param passwordHash the password hash to set
     */

    public String getAddress() {
        return address;
    }

    /**
     * Get the agent's address.
     *
     * @return the address or null
     */

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Set the agent's address.
     *
     * @param address the address to set
     */

    public String getCity() {
        return city;
    }

    /**
     * Get the agent's city.
     *
     * @return the city or null
     */

    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Set the agent's city.
     *
     * @param city the city to set
     */

    public String getCountry() {
        return country;
    }

    /**
     * Get the agent's country.
     *
     * @return the country or null
     */

    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Set the agent's country.
     *
     * @param country the country to set
     */

    public Zone getCurrentZone() {
        return currentZone;
    }

    /**
     * Get the current zone the agent is in.
     *
     * @return the current Zone or null
     */

    public void setCurrentZone(Zone currentZone) {
        this.currentZone = currentZone;
    }

    /**
     * Set the current zone for the agent.
     *
     * @param currentZone the zone to set
     */


    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Compute the agent's age in years based on the birth date.
     *
     * @return the age in years, or 0 if birth date is not set
     */

    public void setAge(int age) {
        this.birthDate = LocalDate.now().minusYears(age);
    }

    /**
     * Set the agent's age by computing and setting the birth date.
     *
     * @param age the age in years
     */


    public Agent() {
        this.maxSpeed = 3.0;
        this.congestionTolerance = 1.0;
    }

    /**
     * Constructs a new Agent.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     */
    public Agent(int id, String firstName, String lastName, Node position) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.maxSpeed = 3.0;
        this.congestionTolerance = 1.0;
    }

    /**
     * Constructs a new Agent.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     * @param currentZone the currentZone.
     */
    public Agent(int id,String firstName,String lastName, Node position,Zone currentZone) {
        this(id, firstName, lastName, position);
        setCurrentZone(currentZone);
    }
    

    // Getters et Setters basiques pour que le code compile
    /**
     * Returns the id.
     * @return the int result.
     */
    public int getId() {
        return id;
    }

    /**
     * Get the unique identifier of the agent.
     *
     * @return the agent id
     */
    
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Set the unique identifier of the agent.
     *
     * @param id the id to set
     */
    public String getFirstName() { return firstName; }
    /**
     * Get the agent's first name.
     *
     * @return the first name or null
     */
    public String getLastName() { return lastName; }
    /**
     * Get the agent's last name.
     *
     * @return the last name or null
     */
    public Node getPosition() { return position; }
    /**
     * Get the current position node of the agent.
     *
     * @return the position Node or null
     */
    public void setPosition(Node position) { this.position = position; }
    /**
     * Set the agent's position node.
     *
     * @param position the Node position to set
     */
    public Node getDestination() { return destination; }
    /**
     * Get the agent's destination node.
     *
     * @return the destination Node or null
     */
    public void setDestination(Node destination) { this.destination = destination; }
    /**
     * Set the agent's destination node.
     *
     * @param destination the Node to set as destination
     */

    public Strategy getStrategy() {
        return strategy;
    }

    /**
     * Get the strategy associated with this agent.
     *
     * @return the Strategy or null
     */

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Set the strategy for this agent.
     *
     * @param strategy the Strategy to assign
     */

    public void decideDestination(List<Zone> zones) {
        if (strategy != null) {
            strategy.apply(this, zones);
        }
    }

    /**
     * Ask the agent's strategy to decide a destination given available zones.
     *
     * @param zones the list of available zones
     */

    public boolean hasStrategy() {
        return strategy != null;
    }

    /**
     * Check whether the agent has an assigned strategy.
     *
     * @return true if a strategy is set, false otherwise
     */
    
    
    public boolean isSaved() {
        // Un agent est sauvé s'il est arrivé sur un nœud de type REFUGE (on complétera après)
        return false; 
    }

    /**
     * Determine whether the agent is considered saved (has reached a refuge).
     *
     * @return true if the agent is saved, false otherwise
     */


    public double getMaxSpeed() { 
        return maxSpeed; 
    }

    /**
     * Get the agent's maximum speed value.
     *
     * @return the maximum speed
     */

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    /**
     * Set the agent's maximum speed.
     *
     * @param maxSpeed the speed value to set
     */

    public double getCongestionTolerance() {
        return congestionTolerance;
    }

    /**
     * Get the agent's congestion tolerance factor.
     *
     * @return the congestion tolerance
     */

    public void setCongestionTolerance(double congestionTolerance) {
        this.congestionTolerance = congestionTolerance;
    }

    /**
     * Set the agent's congestion tolerance factor.
     *
     * @param congestionTolerance the tolerance to set
     */
}
