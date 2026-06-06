package model.simulation;

import java.util.ArrayList;
import java.util.List;

import model.alert.Alert;
import model.alert.AlertSystem;
import model.enums.AlertType;
import model.agent.Agent;
import model.observer.Observer;
import model.observer.Subject;
import model.strategy.Strategy;
import model.zone.Zone;
import model.zone.ZoneManager;
import model.zone.ZoneUpdateListener;

/**
 * Moteur de simulation d'inondation : g?re la propagation des eaux,
 * l'inondation des zones et l'?vacuation
 */
public class SimulationInondation {
    // Valeurs par d?faut
    private static final double DEFAULT_NIVEAU_EAU = 0.0;
    private static final double DEFAULT_GRAVITE = 1.0;
    private static final double DEFAULT_TEMPS_ECOULE = 0.0;
    private static final int DEFAULT_AGENTS_ACTIFS = 5;

    // Param?tres de simulation
    private static final double MIN_NIVEAU_EAU = 0.0;
    private static final double MAX_NIVEAU_EAU = 12.0;
    private static final double MIN_GRAVITE = 0.1;
    private static final double BASE_WATER_RISE_PER_SECOND = 0.08;
    private static final double SIMULATION_STEP_SECONDS = 1.0;

    // Niveaux d'urgence des alertes
    private static final int ALERT_URGENCY_FLOOD = 4;
    private static final int ALERT_URGENCY_EVACUATION = 3;

    private double niveauEau;
    private double gravite;
    private double tempsEcoule;
    private boolean enPause;
    private int agentsActifs;
    private int agentsEvacues;
    private final List<Zone> zones;
    private final AlertSystem alertSystem;
    private final List<ZoneUpdateListener> listeners;
    private final Subject<Zone> zoneObservers;
    private final ZoneManager zoneManager;

    public SimulationInondation() {
        this.niveauEau = DEFAULT_NIVEAU_EAU;
        this.gravite = DEFAULT_GRAVITE;
        this.tempsEcoule = DEFAULT_TEMPS_ECOULE;
        this.enPause = true;
        this.agentsActifs = DEFAULT_AGENTS_ACTIFS;
        this.agentsEvacues = 0;
        this.listeners = new ArrayList<>();
        this.zoneObservers = new Subject<>();
        this.zoneManager = new ZoneManager();
        this.zones = zoneManager.getZones();
        this.alertSystem = new AlertSystem();
    }

    public void addZoneUpdateListener(ZoneUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeZoneUpdateListener(ZoneUpdateListener listener) {
        listeners.remove(listener);
    }

    public void addZoneObserver(Observer<Zone> observer) {
        zoneObservers.addObserver(observer);
    }

    public void removeZoneObserver(Observer<Zone> observer) {
        zoneObservers.removeObserver(observer);
    }

    public void assignStrategyToAgent(Agent agent, Strategy strategy) {
        if (agent != null) {
            agent.setStrategy(strategy);
        }
    }

    public void planAgentRoutes(List<Agent> agents) {
        if (agents == null || agents.isEmpty()) {
            return;
        }
        for (Agent agent : agents) {
            agent.decideDestination(zones);
        }
    }

    private void notifyZoneFlooded(Zone zone) {
        for (ZoneUpdateListener listener : listeners) {
            listener.onZoneFlooded(zone);
        }
        zoneObservers.notifyObservers(zone);
    }

    private void notifyZoneEvacuated(Zone zone) {
        for (ZoneUpdateListener listener : listeners) {
            listener.onZoneEvacuated(zone);
        }
        zoneObservers.notifyObservers(zone);
    }

    private void notifySimulationUpdated() {
        for (ZoneUpdateListener listener : listeners) {
            listener.onSimulationUpdated();
        }
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
        return (int) zones.stream().filter(Zone::isFlooded).count();
    }

    public double getNiveauEau() {
        return niveauEau;
    }

    public AlertSystem getAlertSystem() {
        return alertSystem;
    }

    public List<Zone> getZones() {
        return new ArrayList<>(zones);
    }

    public void setEnPause(boolean enPause) {
        this.enPause = enPause;
    }

    public void setNiveauEau(double niveauEau) {
        this.niveauEau = clamp(niveauEau, MIN_NIVEAU_EAU, MAX_NIVEAU_EAU);
        updateFloodState();
    }

    public void setGravite(double gravite) {
        this.gravite = Math.max(MIN_GRAVITE, gravite);
    }

    public void resetSimulation() {
        this.niveauEau = DEFAULT_NIVEAU_EAU;
        this.gravite = DEFAULT_GRAVITE;
        this.tempsEcoule = DEFAULT_TEMPS_ECOULE;
        this.enPause = true;
        this.agentsEvacues = 0;
        this.alertSystem.getActiveAlerts().clear();
        this.zones.forEach(Zone::reset);
        notifySimulationUpdated();
    }

    public void demarrer() {
        this.enPause = false;
    }

    public void executerPas() {
        advanceSimulation(SIMULATION_STEP_SECONDS);
    }

    public void avancerSimulation(double secondes) {
        if (enPause || secondes <= 0)
            return;
        advanceSimulation(secondes);
    }

    private void advanceSimulation(double secondes) {
        this.tempsEcoule += secondes;
        double hausse = secondes * BASE_WATER_RISE_PER_SECOND * gravite;
        this.niveauEau = clamp(this.niveauEau + hausse, MIN_NIVEAU_EAU, MAX_NIVEAU_EAU);
        propagateFlood();
        evacuateVictims();
        notifySimulationUpdated();
    }

    private void propagateFlood() {
        // Format pour l'heure actuelle
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        for (Zone zone : zones) {
            if (!zone.isFlooded() && niveauEau >= zone.getAltitude()) {
                zone.setFlooded(true);
                notifyZoneFlooded(zone);
                
                // Création de l'alerte avec le nouveau constructeur
                alertSystem.addAlert(new Alert(
                    AlertType.INONDATION,                                     // Type
                    "Zone inondée : " + zone.getName(),                // Description
                    zone.getName(),                                   // Localisation
                    "Élevée",                                         // Sévérité (remplace ALERT_URGENCY_FLOOD)
                    time,                                             // Heure
                    "Active"                                          // Statut
                ));
            }
        }
    }

    private void evacuateVictims() {
        for (Zone zone : zones) {
            if (zone.isFlooded() && !zone.isEvacuated()) {
                int rescued = Math.max(1, zone.getPopulation() / 2);
                zone.setEvacuated(true);
                agentsEvacues += rescued;
                notifyZoneEvacuated(zone);
                String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                alertSystem.addAlert(new Alert(
                    AlertType.EVACUATION,                                        // Type
                    "Évacuation réussie dans " + zone.getName(),          // Description
                    zone.getName(),                                       // Localisation
                    "Faible",                                             // Sévérité (remplace l'ancien niveau numérique)
                    time,                                                 // Heure actuelle
                    "Résolue"                                             // Statut
                ));
            }
        }
    }

    private void updateFloodState() {
        for (Zone zone : zones) {
            if (!zone.isFlooded() && niveauEau >= zone.getAltitude()) {
                zone.setFlooded(true);
                notifyZoneFlooded(zone);
            }
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}