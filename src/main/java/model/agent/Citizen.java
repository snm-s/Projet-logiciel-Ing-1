package model.agent;

import java.time.LocalDate;
import java.time.Period;

import model.graph.Node;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Citizen extends Agent {
    private boolean hasPhone;
    private String mobilityStatus;
    private HouseType houseType;
    private int floor;
    private int householdSize;
    private boolean hasPets;
    private String medicalNeeds;
    private String emergencyContact;

    
    public Citizen() {
        super(); // Appelle le constructeur par défaut de Agent
    }
    
    public Citizen(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
    }

    // --- setters corrigés (suppression du TODO et du throw) ---

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