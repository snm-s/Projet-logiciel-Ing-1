package model.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.agent.Agent; // Import de votre classe abstraite

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class UserService {

    private static final Path FILE_PATH = Paths.get("src/main/resources/data/users.json");
    
    // Configuration de l'ObjectMapper pour gérer les dates (Java 8) et le polymorphisme
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // Support de LocalDate
            .enable(SerializationFeature.INDENT_OUTPUT); // Pour un JSON lisible

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

    public static Agent findByEmail(String email) {
        return loadAgents()
                .stream()
                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }
}