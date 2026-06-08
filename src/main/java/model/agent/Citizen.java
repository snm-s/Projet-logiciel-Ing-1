package model.agent;

import java.time.LocalDate;
import java.time.Period;

import model.enums.CitizenState;
import model.graph.Node;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Citizen extends Agent {
    private CitizenState state;


    private String mobilityStatus;
    private HouseType houseType;
    private int floor;
    private int householdSize;
    private boolean hasPets;
    private String medicalNeeds;
    private String emergencyContact;

    
    public CitizenState getState() {
        return state;
    }


    public String getMobilityStatus() {
        return mobilityStatus;
    }

    public HouseType getHouseType() {
        return houseType;
    }

    public int getFloor() {
        return floor;
    }

    public int getHouseholdSize() {
        return householdSize;
    }

    public boolean isHasPets() {
        return hasPets;
    }

    public String getMedicalNeeds() {
        return medicalNeeds;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }


    // Constructeur vide nécessaire pour Jackson
    public Citizen() {
        super(); // Appelle le constructeur par défaut de Agent
        this.state = CitizenState.CALM; // État par défaut
    }
    
    public Citizen(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
        this.state = CitizenState.CALM; // État par défaut
    }

    // --- setters

    public void setHasPets(boolean hasPets) {
        this.hasPets = hasPets;
    }

    public void setMedicalNeeds(String medicalNeeds) {
        this.medicalNeeds = medicalNeeds;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public void setHouseholdSize(int householdSize) {
        this.householdSize = householdSize;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public void setHouseType(HouseType houseType) {
        this.houseType = houseType;
    }

    public void setState(CitizenState state) {
        this.state = state;
    }

    // --- Méthodes existantes ---

    public void calculateMobilityStatus(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 18) this.mobilityStatus = "child";
        else if (age >= 65) this.mobilityStatus = "elderly";
        else this.mobilityStatus = "normal";
    }

    public void setMobilityStatus(String mobilityStatus) {
        this.mobilityStatus = mobilityStatus;
    }
}