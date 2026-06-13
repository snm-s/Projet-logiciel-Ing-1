package controller.AdminPage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.simulation.FloodSimulation;
import model.simulation.SimulationDataService;
import model.zone.Zone;

/**
 * AdminController
 *
 * This controller is used by the administration dashboard.
 * It centralizes access to agents and zones, computes dashboard indicators,
 * and forwards creation, deletion and update actions to the live simulation.
 *
 * Important design choice:
 * FloodSimulation is the source of truth during execution.
 * The controller reloads observable lists from the simulation so that the UI
 * always displays the current in-memory state.
 */
public class AdminController {

    private final ObjectMapper mapper;
    private ObservableList<Agent> allAgents = FXCollections.observableArrayList();
    private ObservableList<Zone> allZones = FXCollections.observableArrayList();
    private final SimulationDataService dataService;
    private final FloodSimulation simulation;

    public AdminController(SimulationDataService dataService, FloodSimulation simulation) {
        this.dataService = dataService;
        this.simulation = simulation;

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        loadAgents();
        loadZones();
    }

    // ── Loading methods ────────────────────────────────────────────────────────

    /**
     * Reloads agents from the data service, then injects them into the live simulation.
     * This avoids using a separate JSON list as the main runtime state.
     */
    public void loadAgents() {
        List<Agent> live = simulation.getAgents();
        allAgents.setAll(live != null ? live : List.of());
    }

    public void refreshAgentsFromSimulation() {
        List<Agent> live = simulation.getAgents();
        allAgents.setAll(live != null ? live : List.of());
    }

    /**
     * Reloads zones from the live simulation.
     */
    public void loadZones() {
        List<Zone> live = simulation.getZones();
        allZones.setAll(live != null ? live : List.of());
    }

    // ── Observable lists exposed to the UI ─────────────────────────────────────

    public ObservableList<Agent> getAllAgents() { return allAgents; }

    public ObservableList<Agent> getAdmins() {
        return filter("admin");
    }

    public ObservableList<Agent> getCitizens() {
        return filter("citizen");
    }

    public ObservableList<Agent> getRescueAgents() {
        return filter("rescueAgent");
    }

    /**
     * Filters agents by their runtime class name.
     * This keeps the UI independent from the exact subclasses used internally.
     */
    private ObservableList<Agent> filter(String type) {
        return allAgents.stream()
                .filter(a -> a.getClass().getSimpleName().toLowerCase().contains(
                        type.equals("admin") ? "admin" :
                        type.equals("rescueAgent") ? "rescue" : "citizen"))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    // ── Dashboard KPIs ─────────────────────────────────────────────────────────

    public int getTotalAgents()        { return allAgents.size(); }
    public int getTotalCitizens()      { return getCitizens().size(); }
    public int getTotalRescueAgents()  { return getRescueAgents().size(); }

    /**
     * Counts citizens that are not in a calm state.
     * This is used as a simple risk indicator for the dashboard.
     */
    public int getAtRiskCount() {
        return (int) getCitizens().stream()
                .filter(a -> a instanceof Citizen)
                .map(a -> (Citizen) a)
                .filter(c -> c.getState() != null)
                .filter(c -> {
                    String state = c.getState().name();
                    return !state.equalsIgnoreCase("CALM")
                            && !state.equalsIgnoreCase("CALME");
                })
                .count();
    }

    public int getSavedCount() {
        return (int) allAgents.stream().filter(Agent::isSaved).count();
    }

    public int getActiveRescueCount() {
        return (int) getRescueAgents().stream()
                .filter(a -> a instanceof RescueAgent)
                .map(a -> (RescueAgent) a)
                .filter(r -> r.getState() != null && "EN_INTERVENTION".equalsIgnoreCase(r.getState().name()))
                .count();
    }

    public int getAvailableRescueCount() {
        return (int) getRescueAgents().stream()
                .filter(a -> a instanceof RescueAgent)
                .map(a -> (RescueAgent) a)
                .filter(r -> r.getState() != null && "DISPONIBLE".equalsIgnoreCase(r.getState().name()))
                .count();
    }

    /**
     * Returns the number of citizens grouped by state.
     * Example: SAFE -> 12, STRESSED -> 4, PMR -> 2.
     */
    public Map<String, Long> getCitizenStateBreakdown() {
        return getCitizens().stream()
                .filter(a -> a instanceof Citizen)
                .map(a -> (Citizen) a)
                .collect(Collectors.groupingBy(
                        c -> c.getState() != null ? c.getState().name() : "UNKNOWN",
                        Collectors.counting()));
    }

    /**
     * Returns the number of rescue agents grouped by state.
     */
    public Map<String, Long> getRescueStateBreakdown() {
        return getRescueAgents().stream()
                .filter(a -> a instanceof RescueAgent)
                .map(a -> (RescueAgent) a)
                .collect(Collectors.groupingBy(
                        r -> r.getState() != null ? r.getState().name() : "UNKNOWN",
                        Collectors.counting()));
    }

    // ── CRUD operations ────────────────────────────────────────────────────────

    /**
     * Persists the current agent list through the data service.
     */
    public void saveAgents() {
        dataService.saveAgents(allAgents);
    }

    /**
     * Adds an agent to the live simulation, refreshes the observable list,
     * and persists the updated data.
     */
    public void addAgent(Agent a) {
        simulation.addAgent(a);
        allAgents.setAll(simulation.getAgents());
        dataService.saveAgents(allAgents);
    }

    /**
     * Deletes an agent from the live simulation, refreshes the observable list,
     * and persists the updated data.
     */
    public void deleteAgent(Agent a) {
        simulation.removeAgent(a);
        allAgents.setAll(simulation.getAgents());
        dataService.saveAgents(allAgents);
    }

    /**
     * Adds a zone to the live simulation, refreshes the observable list,
     * and persists the updated data.
     */
    public void addZone(Zone z) {
        simulation.addZone(z);
        allZones.setAll(simulation.getZones());
        dataService.saveZones(allZones);
    }

    /**
     * Deletes a zone from the live simulation, refreshes the observable list,
     * and persists the updated data.
     */
    public void deleteZone(Zone z) {
        simulation.removeZone(z);
        allZones.setAll(simulation.getZones());
        dataService.saveZones(allZones);
    }

    /**
     * Updates a zone inside the live simulation, refreshes the observable list,
     * and persists the updated data.
     */
    public void updateZone(Zone z) {
        simulation.updateZone(z);
        allZones.setAll(simulation.getZones());
        dataService.saveZones(allZones);
    }

    public int getTotalZones()   { return allZones.size(); }
    public int getFloodedZones() { return (int) allZones.stream().filter(Zone::isFlooded).count(); }
    public int getSafeZones()    { return (int) allZones.stream().filter(z -> !z.isFlooded()).count(); }

    // ── Search ─────────────────────────────────────────────────────────────────

    /**
     * Searches agents by id, first name, last name, email or class type.
     */
    public ObservableList<Agent> search(String query) {
        if (query == null || query.isBlank()) return allAgents;
        String q = query.toLowerCase();
        return allAgents.stream()
                .filter(a ->
                        String.valueOf(a.getId()).contains(q)
                        || (a.getFirstName() != null && a.getFirstName().toLowerCase().contains(q))
                        || (a.getLastName()  != null && a.getLastName() .toLowerCase().contains(q))
                        || (a.getEmail()     != null && a.getEmail()    .toLowerCase().contains(q))
                        || a.getClass().getSimpleName().toLowerCase().contains(q)
                )
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    // ── Static helper methods ──────────────────────────────────────────────────

    /**
     * Returns a simple type label used by the UI.
     */
    public static String typeOf(Agent a) {
        String name = a.getClass().getSimpleName().toLowerCase();
        if (name.contains("admin"))   return "admin";
        if (name.contains("rescue"))  return "rescueAgent";
        if (name.contains("pmr"))     return "pmr";
        return "citizen";
    }

    /**
     * Returns the textual state of an agent.
     */
    public static String stateOf(Agent a) {
        if (a instanceof Citizen  c && c.getState()  != null) return c.getState().name();
        if (a instanceof RescueAgent r && r.getState() != null) return r.getState().name();
        return "";
    }
}
