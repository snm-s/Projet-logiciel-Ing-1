package model.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueTeam;
import model.alert.Alert;
import model.alert.AlertSystem;
import model.enums.AlertType;
import model.enums.CitizenState;
import model.graph.Graph;
import model.observer.Observer;
import model.observer.Subject;
import model.strategy.Strategy;
import model.zone.Shelter;
import model.zone.Zone;
import model.zone.ZoneUpdateListener;

public class FloodSimulation {
    private Graph graph;
    private List<Agent> agents;
    private AlertSystem alertSystem;

    private static final double DEFAULT_NIVEAU_EAU = 0.0;
    private static final double DEFAULT_GRAVITE = 1.0;
    private static final double DEFAULT_TEMPS_ECOULE = 0.0;
    private static final int DEFAULT_AGENTS_ACTIFS = 5;

    private static final double MIN_NIVEAU_EAU = 0.0;
    private static final double MAX_NIVEAU_EAU = 12.0;
    private static final double MIN_GRAVITE = 0.1;
    private static final double BASE_WATER_RISE_PER_SECOND = 0.08;
    private static final double SIMULATION_STEP_SECONDS = 1.0;

    private double niveauEau;
    private double gravite;
    private double tempsEcoule;
    private boolean enPause;
    private int agentsActifs;
    private int agentsEvacues;

    private final List<Zone> zones;
    private final List<ZoneUpdateListener> listeners;
    private final Subject<Zone> zoneObservers;
    private final SimulationDataService dataService;

    private final Subject<List<Agent>> agentSubject = new Subject<>();
    private final Subject<List<Zone>> zoneSubject = new Subject<>();

    private final Map<Integer, Zone[]> citizenPaths = new HashMap<>();
    private final List<EvacuationEvent> evacuationHistory = new ArrayList<>();
    private boolean simulationStartAlertPublished = false;

    public FloodSimulation(SimulationDataService dataService) {
        this.dataService = dataService;
        this.graph = new Graph();
        this.agents = dataService.loadAgents();
        this.zones = dataService.loadZones();

        if (this.agents == null) this.agents = new ArrayList<>();

        this.alertSystem = new AlertSystem();
        this.niveauEau = DEFAULT_NIVEAU_EAU;
        this.gravite = DEFAULT_GRAVITE;
        this.tempsEcoule = DEFAULT_TEMPS_ECOULE;
        this.enPause = true;
        this.agentsActifs = DEFAULT_AGENTS_ACTIFS;
        this.agentsEvacues = 0;

        this.listeners = new ArrayList<>();
        this.zoneObservers = new Subject<>();
    }

    public FloodSimulation() {
        this(app.Main.getSharedDataService());
    }

    public boolean hasSimulationStartAlertBeenPublished() {
        return simulationStartAlertPublished;
    }

    public void registerCitizenPath(Agent agent, Zone from, Zone to) {
        if (agent != null && from != null && to != null) {
            citizenPaths.put(agent.getId(), new Zone[]{from, to});
        }
    }

    public Map<Integer, Zone[]> getCitizenPaths() {
        return Collections.unmodifiableMap(citizenPaths);
    }

    public void publishSimulationStartAlert() {
        if (simulationStartAlertPublished) return;
        simulationStartAlertPublished = true;

        String time = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        alertSystem.addAlert(new Alert(
                AlertType.EVACUATION,
                "Alerte évacuation : la simulation d'inondation est lancée. Suivez l'itinéraire vers le refuge indiqué.",
                "Toutes les zones",
                "Élevée",
                time,
                "Active",
                "simulation"
        ));
    }

    public void recordEvacuationDeparture(Agent agent, Zone from, Zone to) {
        if (agent == null || from == null || to == null) return;

        registerCitizenPath(agent, from, to);

        if (agent instanceof Citizen c) {
            c.setState(CitizenState.ESCAPING);
        }

        String agentName = agentName(agent);
        evacuationHistory.add(new EvacuationEvent(
                agent.getId(),
                agentName,
                "DEPART",
                agentName + " a reçu son itinéraire et part vers le refuge " + to.getName() + ".",
                from.getName(),
                to.getName()
        ));
    }

    public void recordEvacuationArrival(Agent agent, Zone refuge) {
        if (agent == null || refuge == null) return;

        if (refuge instanceof Shelter && agent instanceof Citizen c) {
            c.setState(CitizenState.SAFE);
        }

        String agentName = agentName(agent);
        evacuationHistory.add(new EvacuationEvent(
                agent.getId(),
                agentName,
                "ARRIVEE",
                agentName + " est arrivé au refuge " + refuge.getName() + ".",
                "",
                refuge.getName()
        ));
    }

    public void recordEvacuationBlocked(Agent agent, Zone zone) {
        if (agent == null) return;

        if (agent instanceof Citizen c) {
            c.setState(CitizenState.STRESSED);
        }

        String agentName = agentName(agent);
        String zoneName = zone != null ? zone.getName() : "zone inconnue";

        evacuationHistory.add(new EvacuationEvent(
                agent.getId(),
                agentName,
                "BLOQUE",
                agentName + " est bloqué près de " + zoneName + ". Secours demandé.",
                zoneName,
                ""
        ));
    }

    public List<EvacuationEvent> getEvacuationHistory() {
        return new ArrayList<>(evacuationHistory);
    }

    public List<EvacuationEvent> getEvacuationHistoryFor(int agentId) {
        List<EvacuationEvent> result = new ArrayList<>();
        for (EvacuationEvent event : evacuationHistory) {
            if (event.getAgentId() == agentId) result.add(event);
        }
        return result;
    }

    private String agentName(Agent agent) {
        if (agent == null) return "Agent";
        String first = agent.getFirstName() == null ? "" : agent.getFirstName();
        String last = agent.getLastName() == null ? "" : agent.getLastName();
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Agent #" + agent.getId() : full;
    }

    public Graph getGraph() { return graph; }
    public List<Agent> getAgents() { return agents; }

    public int getActiveAgentsCount() { return agents.size(); }

    public int getRescuedVictimsCount() {
        return (int) agents.stream().filter(Agent::isSaved).count();
    }

    public int getActiveMissionsCount() {
        return (int) agents.stream()
                .filter(a -> a instanceof RescueTeam && !((RescueTeam) a).isIdle())
                .count();
    }

    public void setZones(List<Zone> zones) {
        this.zones.clear();
        if (zones != null) this.zones.addAll(zones);
        notifyZoneChange();
    }

    public void addZoneUpdateListener(ZoneUpdateListener listener) { listeners.add(listener); }
    public void removeZoneUpdateListener(ZoneUpdateListener listener) { listeners.remove(listener); }
    public void addZoneObserver(Observer<Zone> observer) { zoneObservers.addObserver(observer); }
    public void removeZoneObserver(Observer<Zone> observer) { zoneObservers.removeObserver(observer); }

    private void notifyZoneChange() {
        zoneSubject.notifyObservers(new ArrayList<>(zones));
    }

    public void addAgentObserver(Observer<List<Agent>> observer) {
        agentSubject.addObserver(observer);
    }

    public void removeAgentObserver(Observer<List<Agent>> observer) {
        agentSubject.removeObserver(observer);
    }

    private void notifyAgentChange() {
        agentSubject.notifyObservers(new ArrayList<>(agents));
    }

    public void assignStrategyToAgent(Agent agent, Strategy strategy) {
        if (agent != null) agent.setStrategy(strategy);
    }

    public void planAgentRoutes(List<Agent> agents) {
        if (agents == null || agents.isEmpty()) return;
        for (Agent agent : agents) agent.decideDestination(zones);
    }

    private void notifyZoneFlooded(Zone zone) {
        for (ZoneUpdateListener listener : listeners) listener.onZoneFlooded(zone);
        zoneObservers.notifyObservers(zone);
    }

    private void notifyZoneEvacuated(Zone zone) {
        for (ZoneUpdateListener listener : listeners) listener.onZoneEvacuated(zone);
        zoneObservers.notifyObservers(zone);
    }

    private void notifySimulationUpdated() {
        for (ZoneUpdateListener listener : listeners) listener.onSimulationUpdated();
    }

    public boolean isEnPause() { return enPause; }
    public double getTempsEcoule() { return tempsEcoule; }

    public int getNombreAgents() {
        return agents == null ? 0 : agents.size();
    }

    public int getNombreAgentsEvacues() {
        if (agents == null) return 0;
        return (int) agents.stream()
                .filter(a -> a instanceof Citizen)
                .map(a -> (Citizen) a)
                .filter(c -> c.getState() == CitizenState.SAFE)
                .count();
    }

    public int getNombreZonesInondees() {
        return (int) zones.stream().filter(Zone::isFlooded).count();
    }

    public double getNiveauEau() { return niveauEau; }
    public AlertSystem getAlertSystem() { return alertSystem; }
    public List<Zone> getZones() { return new ArrayList<>(zones); }

    public void setEnPause(boolean enPause) { this.enPause = enPause; }

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
        this.evacuationHistory.clear();
        this.citizenPaths.clear();
        this.simulationStartAlertPublished = false;
        this.zones.forEach(Zone::reset);
        notifySimulationUpdated();
    }

    public void demarrer() {
        this.enPause = false;
        publishSimulationStartAlert();
    }

    public void executerPas() {
        advanceSimulation(SIMULATION_STEP_SECONDS);
    }

    public void avancerSimulation(double secondes) {
        if (enPause || secondes <= 0) return;
        advanceSimulation(secondes);
    }

    public void avancerTempsSansMontee(double secondes) {
        if (enPause || secondes <= 0) return;
        this.tempsEcoule += secondes;
        notifySimulationUpdated();
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
        String time = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        for (Zone zone : zones) {
            if (!zone.isFlooded() && niveauEau >= zone.getAltitude()) {
                zone.setFlooded(true);
                notifyZoneFlooded(zone);

                alertSystem.addAlert(new Alert(
                        AlertType.INONDATION,
                        "Zone inondée : " + zone.getName(),
                        zone.getName(),
                        "Élevée",
                        time,
                        "Active"
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

                String time = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                alertSystem.addAlert(new Alert(
                        AlertType.EVACUATION,
                        "Évacuation réussie dans " + zone.getName(),
                        zone.getName(),
                        "Faible",
                        time,
                        "Résolue"
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

    public void addAgent(Agent agent) {
        agents.add(agent);
        notifyAgentChange();
        notifySimulationUpdated();
    }

    public void removeAgent(Agent agent) {
        agents.remove(agent);
        notifyAgentChange();
        notifySimulationUpdated();
    }

    public void addZone(Zone zone) {
        zones.add(zone);
        notifyZoneChange();
        notifySimulationUpdated();
    }

    public void removeZone(Zone zone) {
        zones.remove(zone);
        notifyZoneChange();
        notifySimulationUpdated();
    }

    public void removeZoneById(int zoneId) {
        zones.removeIf(z -> z.getId() == zoneId);
        notifyZoneChange();
        notifySimulationUpdated();
    }

    public void updateZone(Zone updated) {
        for (int i = 0; i < zones.size(); i++) {
            if (zones.get(i).getId() == updated.getId()) {
                zones.set(i, updated);
                break;
            }
        }
        notifyZoneChange();
        notifySimulationUpdated();
    }

    public int nextZoneId() {
        return zones.stream().mapToInt(Zone::getId).max().orElse(0) + 1;
    }

    public void replaceAllAgents(List<Agent> newAgents) {
        agents.clear();
        agents.addAll(newAgents);
        notifyAgentChange();
    }

    public void setAgents(List<Agent> agents) {
        this.agents.clear();
        if (agents != null) {
            this.agents.addAll(agents);
        }
    }

    private void persist() {
        dataService.saveAll(
                new ArrayList<>(zones),
                new ArrayList<>(agents)
        );
    }
}
