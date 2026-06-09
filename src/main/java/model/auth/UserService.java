package model.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import model.agent.AdminAgent;
import model.agent.Agent;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class UserService {

    private static final String ADMIN_EMAIL = "admin@exemple.com";
    private static final String ADMIN_PASSWORD_HASH = "8c6976e5b5410415bde908bd4dee15dfb16f1c1c2c3c4c5c6c7c8c9d0e1f234";

    private static final Path FILE_PATH = Paths.get("dataUser", "users.json");

    private static Map<String, String> resetTokens = new HashMap<>();

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static List<Agent> loadAgents() {
        File file = FILE_PATH.toFile();

        if (!file.exists()) {
            System.out.println("Fichier inexistant, création d'une nouvelle liste.");
            return new ArrayList<>();
        }

        try {
            List<Agent> agents = mapper.readValue(file, new TypeReference<List<Agent>>() {});
            System.out.println("Chargement réussi : " + agents.size() + " agents trouvés.");
            return agents;
        } catch (Exception e) {
            System.err.println("ERREUR LORS DU CHARGEMENT DU JSON :");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveAgents(List<Agent> agents) {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            mapper.writerFor(new TypeReference<List<Agent>>() {})
                    .writeValue(FILE_PATH.toFile(), agents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addAgent(Agent agent) {
        List<Agent> agents = loadAgents();
        agents.add(agent);
        saveAgents(agents);
    }

    public static Agent authenticate(String email, String passwordHash) {
        if (ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD_HASH.equals(passwordHash)) {
            return new AdminAgent(999, "Admin", "System", null);
        }

        return loadAgents().stream()
                .filter(a -> a.getEmail() != null
                        && a.getPasswordHash() != null
                        && a.getEmail().equalsIgnoreCase(email)
                        && a.getPasswordHash().equals(passwordHash))
                .findFirst()
                .orElse(null);
    }

    public static void setResetToken(String email, String token) {
        resetTokens.put(email, token);
    }

    public static boolean verifyToken(String email, String token) {
        return token != null && token.equals(resetTokens.get(email));
    }

    public static boolean resetPassword(String email, String newPasswordHash) {
        List<Agent> agents = loadAgents();
        boolean found = false;

        if (ADMIN_EMAIL.equalsIgnoreCase(email)) {
            return false;
        }

        for (Agent agent : agents) {
            if (agent.getEmail() != null && agent.getEmail().equalsIgnoreCase(email)) {
                agent.setPasswordHash(newPasswordHash);
                found = true;
                break;
            }
        }

        if (found) {
            saveAgents(agents);
        }

        return found;
    }

    public static Agent findByEmail(String email) {
        return loadAgents()
                .stream()
                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    public static boolean updateAgent(Agent updatedAgent) {
        if (updatedAgent == null) {
            return false;
        }

        List<Agent> agents = loadAgents();
        boolean found = false;

        for (int i = 0; i < agents.size(); i++) {
            Agent current = agents.get(i);

            boolean sameId = current.getId() == updatedAgent.getId();

            boolean sameEmail = current.getEmail() != null
                    && updatedAgent.getEmail() != null
                    && current.getEmail().equalsIgnoreCase(updatedAgent.getEmail());

            if (sameId || sameEmail) {
                agents.set(i, updatedAgent);
                found = true;
                break;
            }
        }

        if (found) {
            saveAgents(agents);
        }

        return found;
    }
}
