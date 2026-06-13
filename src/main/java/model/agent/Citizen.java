package model.agent;

import java.time.LocalDate;
import java.time.Period;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.enums.CitizenState;
import model.graph.Node;
import model.zone.Zone;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Citizen extends Agent {
    private CitizenState state;

    private String mobilityStatus;
    private HouseType houseType;
    private int floor;
    private int householdSize;
    private boolean hasPets;
    private String medicalNeeds;
    private String emergencyContact;

    private boolean emergencyAlertsEnabled = true;
    private boolean soundNotificationsEnabled = false;
    private boolean routeUpdatesEnabled = true;
    private boolean backgroundLocationEnabled = true;
    private double displayScale = 100.0;

    /**
     * Constructs a new Citizen.
     */
    public Citizen() {
        super();
        this.state = CitizenState.CALM;
        this.mobilityStatus = "normal";
    }

    /**
     * Constructs a new Citizen.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     */
    public Citizen(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.state = CitizenState.CALM;
        this.mobilityStatus = "normal";
    }

    /**
     * Constructs a new Citizen.
     * @param id the id.
     * @param firstName the firstName.
     * @param lastName the lastName.
     * @param position the position.
     * @param currentZone the currentZone.
     */
    public Citizen(int id,String firstName,String lastName, Node position,Zone currentZone) {
        super(id, firstName, lastName, position, currentZone);
        this.state = CitizenState.CALM;
        this.mobilityStatus = "normal";
    }




    @JsonIgnore
    /**
     * Returns the state.
     * @return the CitizenState.
     */
    public CitizenState getState() {
        return state;
    }

    /**
     * Sets the state.
     * @param state the state.
     */
    public void setState(CitizenState state) {
        this.state = state;
    }

    /**
     * Returns the mobility status.
     * @return the String.
     */
    public String getMobilityStatus() {
        return mobilityStatus;
    }

    /**
     * Sets the mobility status.
     * @param mobilityStatus the mobilityStatus.
     */
    public void setMobilityStatus(String mobilityStatus) {
        this.mobilityStatus = mobilityStatus;
    }

    /**
     * Returns the house type.
     * @return the HouseType.
     */
    public HouseType getHouseType() {
        return houseType;
    }

    /**
     * Sets the house type.
     * @param houseType the houseType.
     */
    public void setHouseType(HouseType houseType) {
        this.houseType = houseType;
    }

    /**
     * Returns the floor.
     * @return the int result.
     */
    public int getFloor() {
        return floor;
    }

    /**
     * Sets the floor.
     * @param floor the floor.
     */
    public void setFloor(int floor) {
        this.floor = floor;
    }

    /**
     * Returns the household size.
     * @return the int result.
     */
    public int getHouseholdSize() {
        return householdSize;
    }

    /**
     * Sets the household size.
     * @param householdSize the householdSize.
     */
    public void setHouseholdSize(int householdSize) {
        this.householdSize = householdSize;
    }

    /**
     * Returns whether has pets.
     * @return the boolean result.
     */
    public boolean isHasPets() {
        return hasPets;
    }

    /**
     * Sets the has pets.
     * @param hasPets the hasPets.
     */
    public void setHasPets(boolean hasPets) {
        this.hasPets = hasPets;
    }

    /**
     * Returns the medical needs.
     * @return the String.
     */
    public String getMedicalNeeds() {
        return medicalNeeds;
    }

    /**
     * Sets the medical needs.
     * @param medicalNeeds the medicalNeeds.
     */
    public void setMedicalNeeds(String medicalNeeds) {
        this.medicalNeeds = medicalNeeds;
    }

    /**
     * Returns the emergency contact.
     * @return the String.
     */
    public String getEmergencyContact() {
        return emergencyContact;
    }

    /**
     * Sets the emergency contact.
     * @param emergencyContact the emergencyContact.
     */
    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    /**
     * Returns whether emergency alerts enabled.
     * @return the boolean result.
     */
    public boolean isEmergencyAlertsEnabled() {
        return emergencyAlertsEnabled;
    }

    /**
     * Sets the emergency alerts enabled.
     * @param emergencyAlertsEnabled the emergencyAlertsEnabled.
     */
    public void setEmergencyAlertsEnabled(boolean emergencyAlertsEnabled) {
        this.emergencyAlertsEnabled = emergencyAlertsEnabled;
    }

    /**
     * Returns whether sound notifications enabled.
     * @return the boolean result.
     */
    public boolean isSoundNotificationsEnabled() {
        return soundNotificationsEnabled;
    }

    /**
     * Sets the sound notifications enabled.
     * @param soundNotificationsEnabled the soundNotificationsEnabled.
     */
    public void setSoundNotificationsEnabled(boolean soundNotificationsEnabled) {
        this.soundNotificationsEnabled = soundNotificationsEnabled;
    }

    /**
     * Returns whether route updates enabled.
     * @return the boolean result.
     */
    public boolean isRouteUpdatesEnabled() {
        return routeUpdatesEnabled;
    }

    /**
     * Sets the route updates enabled.
     * @param routeUpdatesEnabled the routeUpdatesEnabled.
     */
    public void setRouteUpdatesEnabled(boolean routeUpdatesEnabled) {
        this.routeUpdatesEnabled = routeUpdatesEnabled;
    }

    /**
     * Returns whether background location enabled.
     * @return the boolean result.
     */
    public boolean isBackgroundLocationEnabled() {
        return backgroundLocationEnabled;
    }

    /**
     * Sets the background location enabled.
     * @param backgroundLocationEnabled the backgroundLocationEnabled.
     */
    public void setBackgroundLocationEnabled(boolean backgroundLocationEnabled) {
        this.backgroundLocationEnabled = backgroundLocationEnabled;
    }

    /**
     * Returns the display scale.
     * @return the double result.
     */
    public double getDisplayScale() {
        return displayScale;
    }

    /**
     * Sets the display scale.
     * @param displayScale the displayScale.
     */
    public void setDisplayScale(double displayScale) {
        if (displayScale < 80.0) {
            this.displayScale = 80.0;
        } else if (displayScale > 130.0) {
            this.displayScale = 130.0;
        } else {
            this.displayScale = displayScale;
        }
    }

    @JsonIgnore
    /**
     * Returns whether mobility reduced.
     * @return the boolean result.
     */
    public boolean isMobilityReduced() {
        return "pmr".equalsIgnoreCase(mobilityStatus);
    }
    
    /**
     * Sets the mobility reduced.
     * @param mobilityReduced the mobilityReduced.
     */
    public void setMobilityReduced(boolean mobilityReduced) {
        this.mobilityStatus = mobilityReduced ? "pmr" : "normal";
    }

    /**
     * Calculates mobility status.
     * @param birthDate the birthDate.
     */
    public void calculateMobilityStatus(LocalDate birthDate) {
        if (birthDate == null) {
            this.mobilityStatus = "normal";
            return;
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();

        if (age < 18) {
            this.mobilityStatus = "child";
        } else if (age >= 65) {
            this.mobilityStatus = "elderly";
        } else {
            this.mobilityStatus = "normal";
        }
    }
}
