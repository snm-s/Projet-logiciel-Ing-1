package controller.AdminPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.agent.Agent;
import model.agent.AdminAgent;
import model.agent.Citizen;
import model.agent.RescueAgent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminController
 * Charge les agents depuis users.json (hiérarchie Agent) et expose
 * les données agrégées pour AdminDashboardView.
 */
public class AdminController {

    private static final String USERS_FILE = "dataUser/users.json";

    private final ObjectMapper mapper;
    private ObservableList<Agent> allAgents = FXCollections.observableArrayList();

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static AdminController instance;

    public static AdminController getInstance() {
        if (instance == null) instance = new AdminController();
        return instance;
    }

    public AdminController() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadAgents();
    }

    // ── Chargement ─────────────────────────────────────────────────────────────

    public void loadAgents() {
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                System.err.println("[AdminController] users.json introuvable : " + file.getAbsolutePath());
                return;
            }
            List<Agent> loaded = mapper.readValue(file, new TypeReference<List<Agent>>() {});
            allAgents.setAll(loaded);
            System.out.println("[AdminController] " + loaded.size() + " agents chargés.");
        } catch (IOException e) {
            System.err.println("[AdminController] Erreur lecture users.json : " + e.getMessage());
        }
    }

    // ── Listes observables ─────────────────────────────────────────────────────

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

    private ObservableList<Agent> filter(String type) {
        return allAgents.stream()
                .filter(a -> a.getClass().getSimpleName().toLowerCase().contains(
                        type.equals("admin") ? "admin" :
                        type.equals("rescueAgent") ? "rescue" : "citizen"))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    // ── KPIs ───────────────────────────────────────────────────────────────────

    public int getTotalAgents()        { return allAgents.size(); }
    public int getTotalCitizens()      { return getCitizens().size(); }
    public int getTotalRescueAgents()  { return getRescueAgents().size(); }

    public int getAtRiskCount() {
        return (int) getCitizens().stream()
                .filter(a -> a instanceof Citizen)
                .map(a -> (Citizen) a)
                .filter(c -> c.getState() != null && !"CALME".equalsIgnoreCase(c.getState().name()))
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

    /** Map<état, nb> pour les citoyens */
    public Map<String, Long> getCitizenStateBreakdown() {
        return getCitizens().stream()
                .filter(a -> a instanceof Citizen)
                .map(a -> (Citizen) a)
                .collect(Collectors.groupingBy(
                        c -> c.getState() != null ? c.getState().name() : "INCONNU",
                        Collectors.counting()));
    }

    /** Map<état, nb> pour les agents secours */
    public Map<String, Long> getRescueStateBreakdown() {
        return getRescueAgents().stream()
                .filter(a -> a instanceof RescueAgent)
                .map(a -> (RescueAgent) a)
                .collect(Collectors.groupingBy(
                        r -> r.getState() != null ? r.getState().name() : "INCONNU",
                        Collectors.counting()));
    }

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public void addAgent(Agent a) {
        allAgents.add(a);
        saveAgents();
    }

    public void deleteAgent(Agent a) {
        allAgents.remove(a);
        saveAgents();
    }

    public void saveAgents() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(USERS_FILE), allAgents);
        } catch (IOException e) {
            System.err.println("[AdminController] Erreur sauvegarde : " + e.getMessage());
        }
    }

    // ── Recherche ──────────────────────────────────────────────────────────────

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

    // ── Helpers type string ────────────────────────────────────────────────────

    /** Renvoie "admin" / "rescueAgent" / "citizen" / "pmr" depuis la classe */
    public static String typeOf(Agent a) {
        String name = a.getClass().getSimpleName().toLowerCase();
        if (name.contains("admin"))   return "admin";
        if (name.contains("rescue"))  return "rescueAgent";
        if (name.contains("pmr"))     return "pmr";
        return "citizen";
    }

    /** Renvoie l'état textuel d'un agent (Citizen ou RescueAgent), "" sinon */
    public static String stateOf(Agent a) {
        if (a instanceof Citizen  c && c.getState()  != null) return c.getState().name();
        if (a instanceof RescueAgent r && r.getState() != null) return r.getState().name();
        return "";
    }
}
