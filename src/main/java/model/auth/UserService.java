package model.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.agent.AdminAgent;
import model.agent.Agent;
import model.simulation.SimulationDataService;

public class UserService {

    private static final String ADMIN_EMAIL = "admin@exemple.com";
    private static final String ADMIN_PASSWORD_HASH = "8c6976e5b5410415bde908bd4dee15dfb16f1c1c2c3c4c5c6c7c8c9d0e1f234";

    private static Map<String, String> resetTokens = new HashMap<>();

    // ── Délégation totale à SimulationDataService ──────────────────────

    private static SimulationDataService ds() {
        return app.Main.getSharedDataService();
    }

    public static List<Agent> loadAgents() {
        return ds().loadAgents();
    }

    public static void saveAgents(List<Agent> agents) {
        ds().saveAgents(agents);
    }

    public static void addAgent(Agent agent) {
        List<Agent> agents = loadAgents();
    
        int nextId = agents.stream()
                .mapToInt(Agent::getId)
                .max()
                .orElse(0) + 1;
    
        agent.setId(nextId);
    
        agents.add(agent);
        saveAgents(agents);
    
        app.Main.getSharedSimulation().addAgent(agent);
    }

    public static boolean updateAgent(Agent updatedAgent) {
        if (updatedAgent == null) return false;
        List<Agent> agents = loadAgents();
        boolean found = false;
        for (int i = 0; i < agents.size(); i++) {
            Agent current = agents.get(i);
            if (current.getId() == updatedAgent.getId() ||
               (current.getEmail() != null && current.getEmail().equalsIgnoreCase(updatedAgent.getEmail()))) {
                agents.set(i, updatedAgent);
                found = true;
                break;
            }
        }
        if (found) saveAgents(agents);
        return found;
    }

    // ── Authentification — inchangée ───────────────────────────────────

    public static Agent authenticate(String email, String password) {
        String hashedPassword = PasswordHasher.hash(password);
    
        if (ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD_HASH.equals(hashedPassword)) {
            return new AdminAgent(999, "Admin", "System", null);
        }
    
        return loadAgents().stream()
            .filter(a -> a.getEmail() != null
                      && a.getPasswordHash() != null
                      && a.getEmail().equalsIgnoreCase(email)
                      && a.getPasswordHash().equals(hashedPassword))
            .findFirst()
            .orElse(null);
    }

    public static Agent findByEmail(String email) {
        return loadAgents().stream()
            .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(email))
            .findFirst()
            .orElse(null);
    }

    // ── Reset mot de passe — inchangé ──────────────────────────────────

    public static void setResetToken(String email, String token) {
        resetTokens.put(email, token);
    }

    public static boolean verifyToken(String email, String token) {
        return token != null && token.equals(resetTokens.get(email));
    }

    public static boolean resetPassword(String email, String newPasswordHash) {
        if (ADMIN_EMAIL.equalsIgnoreCase(email)) return false;
        List<Agent> agents = loadAgents();
        boolean found = false;
        for (Agent agent : agents) {
            if (agent.getEmail() != null && agent.getEmail().equalsIgnoreCase(email)) {
                agent.setPasswordHash(newPasswordHash);
                found = true;
                break;
            }
        }
        if (found) saveAgents(agents);
        return found;
    }
}
