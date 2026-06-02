package model.agent;

import java.time.LocalDate;
import java.time.Period;

import model.graph.Node;

public class Citizen extends Agent {
    private boolean hasPhone;
    private String mobilityStatus;
    private HouseType houseType;
    private int floor;
    private int householdSize;

    public Citizen(int id, String firstName, String lastName, Node position) {
        super(id, firstName, lastName, position);
    }

    public Citizen(int id, String firstName, String lastName, Node position, boolean hasPhone, String mobilityStatus, HouseType houseType, int floor, int householdSize) {
        this(id, firstName, lastName, position);
        this.hasPhone = hasPhone;
        this.mobilityStatus = mobilityStatus;
        this.houseType = houseType;
        this.floor = floor;
        this.householdSize = householdSize;
    }

    public void setHasPets(boolean hasPets) {
        this.setHasPets(hasPets);
    }

    public void setMedicalNeeds(String medicalNeeds) {
        this.setMedicalNeeds(medicalNeeds);
    }

    public void setEmergencyContact(String emergencyContact) {
        this.setEmergencyContact(emergencyContact);
    }


    public void calculateMobilityStatus(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 18) this.mobilityStatus = "child";
        else if (age >= 65) this.mobilityStatus = "elderly";
        else this.mobilityStatus = "normal";
    }

    public void setMobilityStatus(String mobilityStatus) {
        this.mobilityStatus = mobilityStatus;
    }

    public void setHouseholdSize(int householdSize2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHouseholdSize'");
    }

    public void setFloor(int floor2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setFloor'");
    }

    public void setHouseType(HouseType houseType2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHouseType'");
    }
}
