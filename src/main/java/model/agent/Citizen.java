package model.agent;

import java.time.LocalDate;
import java.time.Period;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import model.enums.CitizenState;
import model.graph.Node;


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

    public Citizen() {
        super();
        this.state = CitizenState.CALM;
        this.mobilityStatus = "normal";
    }

    public Citizen(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.state = CitizenState.CALM;
        this.mobilityStatus = "normal";
    }

    @JsonIgnore
    public CitizenState getState() {
        return state;
    }

    public void setState(CitizenState state) {
        this.state = state;
    }

    public String getMobilityStatus() {
        return mobilityStatus;
    }

    public void setMobilityStatus(String mobilityStatus) {
        this.mobilityStatus = mobilityStatus;
    }

    public HouseType getHouseType() {
        return houseType;
    }

    public void setHouseType(HouseType houseType) {
        this.houseType = houseType;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getHouseholdSize() {
        return householdSize;
    }

    public void setHouseholdSize(int householdSize) {
        this.householdSize = householdSize;
    }

    public boolean isHasPets() {
        return hasPets;
    }

    public void setHasPets(boolean hasPets) {
        this.hasPets = hasPets;
    }

    public String getMedicalNeeds() {
        return medicalNeeds;
    }

    public void setMedicalNeeds(String medicalNeeds) {
        this.medicalNeeds = medicalNeeds;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public boolean isEmergencyAlertsEnabled() {
        return emergencyAlertsEnabled;
    }

    public void setEmergencyAlertsEnabled(boolean emergencyAlertsEnabled) {
        this.emergencyAlertsEnabled = emergencyAlertsEnabled;
    }

    public boolean isSoundNotificationsEnabled() {
        return soundNotificationsEnabled;
    }

    public void setSoundNotificationsEnabled(boolean soundNotificationsEnabled) {
        this.soundNotificationsEnabled = soundNotificationsEnabled;
    }

    public boolean isRouteUpdatesEnabled() {
        return routeUpdatesEnabled;
    }

    public void setRouteUpdatesEnabled(boolean routeUpdatesEnabled) {
        this.routeUpdatesEnabled = routeUpdatesEnabled;
    }

    public boolean isBackgroundLocationEnabled() {
        return backgroundLocationEnabled;
    }

    public void setBackgroundLocationEnabled(boolean backgroundLocationEnabled) {
        this.backgroundLocationEnabled = backgroundLocationEnabled;
    }

    public double getDisplayScale() {
        return displayScale;
    }

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
    public boolean isMobilityReduced() {
        return "pmr".equalsIgnoreCase(mobilityStatus);
    }
    
    public void setMobilityReduced(boolean mobilityReduced) {
        this.mobilityStatus = mobilityReduced ? "pmr" : "normal";
    }

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
