package model.agent;

import java.time.LocalDate;
import java.time.Period;

import model.graph.Node;

public class Citizen extends Agent {
    private int age;
    private boolean hasPhone;
    private String mobilityStatus;

    public Citizen(int id, String name, Node position) {
        super(id, name, position);
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
}
