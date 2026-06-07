package controller.AdminPage;

import model.simulation.SimulationInondation;

public class SimulationController {
    private final SimulationInondation modele;
    private double vitesseSimulationMs;

    public SimulationController() {
        this.modele = new SimulationInondation();
        this.vitesseSimulationMs = 500.0;
    }

    public SimulationInondation getModele() {
        return modele;
    }

    public void demarrerSimulation() {
        modele.demarrer();
    }

    public void mettreEnPause() {
        modele.setEnPause(true);
    }

    public void reprendreSimulation() {
        modele.setEnPause(false);
    }

    public void executerPas() {
        modele.executerPas();
    }

    public void resetSimulation() {
        modele.resetSimulation();
    }

    public void setVitesseSimulation(double vitesseSimulationMs) {
        this.vitesseSimulationMs = Math.max(100.0, vitesseSimulationMs);
    }

    public double getVitesseSimulationMs() {
        return vitesseSimulationMs;
    }

    public void setGravite(double gravite) {
        modele.setGravite(gravite);
    }

    public void setNiveauEau(double niveauEau) {
        modele.setNiveauEau(niveauEau);
    }
}