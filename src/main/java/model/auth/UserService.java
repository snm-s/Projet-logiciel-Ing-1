package model.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import model.agent.AdminAgent;
import model.agent.Agent; // Import de votre classe abstraite

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class UserService {


    private static final String ADMIN_EMAIL = "admin@exemple.com";
    // Le hash SHA-256 (ou autre) du mot de passe admin
    private static final String ADMIN_PASSWORD_HASH = "8c6976e5b5410415bde908bd4dee15dfb16f1c1c2c3c4c5c6c7c8c9d0e1f234";

    private static final Path FILE_PATH = Paths.get("dataUser", "users.json");

    
    // Pour sécuriser la réinitialisation
    private static Map<String, String> resetTokens = new HashMap<>();
    
    // Configuration de l'ObjectMapper pour gérer les dates (Java 8) et le polymorphisme
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT); 
            

    public static List<Agent> loadAgents() {
        try {
            File file = FILE_PATH.toFile();
            if (!file.exists()) {
                return new ArrayList<>();
            }
            // Lecture du fichier vers une liste d'Agent (gère automatiquement les sous-types)
            return mapper.readValue(file, new TypeReference<List<Agent>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveAgents(List<Agent> agents) {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            mapper.writeValue(FILE_PATH.toFile(), agents);
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
        // 1. Vérification Admin "en dur" (Hardcoded)
        if (ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD_HASH.equals(passwordHash)) {
                return new AdminAgent(999, "Admin", "System", null);
            }

        // 2. Sinon, recherche dans le fichier JSON
        return loadAgents().stream()
                .filter(a -> a.getEmail().equalsIgnoreCase(email) && a.getPasswordHash().equals(passwordHash))
                .findFirst()
                .orElse(null);
    }


    // Ajoutez ces deux méthodes pour manipuler la Map de tokens
    public static void setResetToken(String email, String token) {
        resetTokens.put(email, token);
    }

    public static boolean verifyToken(String email, String token) {
        // Retourne true si le token existe et correspond à l'email
        return token.equals(resetTokens.get(email));
    }

    public static boolean resetPassword(String email, String newPasswordHash) {
        List<Agent> agents = loadAgents();
        boolean found = false;
        if (ADMIN_EMAIL.equalsIgnoreCase(email)) return false;
        for (Agent agent : agents) {
            if (agent.getEmail().equalsIgnoreCase(email)) {
                agent.setPasswordHash(newPasswordHash);
                found = true;
                break;
            }
        }

        if (found) {
            saveAgents(agents); // On réécrit la liste complète avec le nouveau hash
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
}