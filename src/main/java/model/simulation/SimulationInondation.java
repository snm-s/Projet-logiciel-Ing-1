package model.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import model.alert.Alert;
import model.alert.AlertSystem;

public class SimulationInondation {
    private double niveauEau;
    private double gravite;
    private double tempsEcoule;
    private boolean enPause;
    private int agentsActifs;
    private int agentsEvacues;
    private final List<FloodZone> zones;
    private final AlertSystem alertSystem;

    public SimulationInondation() {
        this.niveauEau = 0.0;
        this.gravite = 1.0;
        this.tempsEcoule = 0.0;
        this.enPause = true;
        this.agentsActifs = 5;
        this.agentsEvacues = 0;
        this.zones = new ArrayList<>();
        this.alertSystem = new AlertSystem();
        initializeZones();
    }

    public boolean isEnPause() {
        return enPause;
    }

    public double getTempsEcoule() {
        return tempsEcoule;
    }

    public int getNombreAgents() {
        return agentsActifs;
    }

    public int getNombreAgentsEvacues() {
        return agentsEvacues;
    }

    public int getNombreZonesInondees() {
        return (int) zones.stream().filter(FloodZone::isFlooded).count();
    }

    public double getNiveauEau() {
        return niveauEau;
    }

    public AlertSystem getAlertSystem() {
        return alertSystem;
    }

    public List<FloodZone> getZones() {
        return new ArrayList<>(zones);
    }

    public void setEnPause(boolean enPause) {
        this.enPause = enPause;
    }

    public void setNiveauEau(double niveauEau) {
        this.niveauEau = clamp(niveauEau, 0.0, 12.0);
        updateFloodState();
    }

    public void setGravite(double gravite) {
        this.gravite = Math.max(0.1, gravite);
    }

    public void resetSimulation() {
        this.niveauEau = 0.0;
        this.gravite = 1.0;
        this.tempsEcoule = 0.0;
        this.enPause = true;
        this.agentsEvacues = 0;
        this.alertSystem.getActiveAlerts().clear();
        this.zones.forEach(FloodZone::reset);
    }

    public void demarrer() {
        this.enPause = false;
    }

    public void executerPas() {
        advanceSimulation(1.0);
    }

    public void avancerSimulation(double secondes) {
        if (enPause || secondes <= 0) {
            return;
        }
        advanceSimulation(secondes);
    }

    private void advanceSimulation(double secondes) {
        this.tempsEcoule += secondes;
        double hausse = secondes * 0.08 * gravite;
        this.niveauEau = clamp(this.niveauEau + hausse, 0.0, 12.0);
        propagateFlood();
        evacuateVictims();
    }

    private void initializeZones() {
        zones.clear();
        String[] noms = { "Centre-ville", "Quartier sud", "Bergerie", "Parc urbain", "Espace industriel",
                "Hameau du lac", "Avenue des Platanes", "Côte de la Vallée", "Zone commerciale", "Port",
                "Campagne nord", "Stade" };
        for (int i = 0; i < noms.length; i++) {
            double altitude = 1.0 + i * 0.9 + ThreadLocalRandom.current().nextDouble(-0.2, 0.8);
            int population = 8 + ThreadLocalRandom.current().nextInt(0, 12);
            zones.add(new FloodZone(noms[i], altitude, population));
        }
    }

    private void propagateFlood() {
        for (FloodZone zone : zones) {
            if (!zone.isFlooded() && niveauEau >= zone.getAltitude()) {
                zone.setFlooded(true);
                alertSystem.addAlert(new Alert(
                        alertSystem.getActiveAlerts().size() + 1,
                        "Zone inondée : " + zone.getName() + " (altitude " + String.format("%.1f", zone.getAltitude())
                                + " m)",
                        4));
            }
        }
    }

    private void evacuateVictims() {
        for (FloodZone zone : zones) {
            if (zone.isFlooded() && !zone.isEvacuated()) {
                int rescued = zone.evacuate();
                if (rescued > 0) {
                    agentsEvacues += rescued;
                    alertSystem.addAlert(new Alert(
                            alertSystem.getActiveAlerts().size() + 1,
                            "Évacuation réussie dans " + zone.getName() + " : " + rescued + " personnes",
                            3));
                }
            }
        }
    }

    private void updateFloodState() {
        for (FloodZone zone : zones) {
            if (!zone.isFlooded() && niveauEau >= zone.getAltitude()) {
                zone.setFlooded(true);
            }
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
