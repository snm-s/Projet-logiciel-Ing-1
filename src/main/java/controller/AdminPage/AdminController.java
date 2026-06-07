package controller.AdminPage;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import view.AdminDashboardView.AgentInscrit;
import view.AdminDashboardView.Position;

public class AdminController {

    private static final String JSON_FILE_PATH = "user.json";

    private final ObservableList<AgentInscrit> registeredAgents;

    public AdminController() {
        registeredAgents = FXCollections.observableArrayList();
        loadDataFromJSON();
    }

    // ── Data loading ────────────────────────────────────────────────────────────

    private void loadDataFromJSON() {
        File file = new File(JSON_FILE_PATH);

        if (!file.exists()) {
            System.out.println("⚠️ Fichier user.json introuvable. Création de données par défaut.");
            registeredAgents.add(new AgentInscrit(
                    "admin", 1, "Chef", "Admin", "admin@test.com",
                    "CALME", null, new Position(48.85, 2.35)));
            return;
        }

        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(file);
            List<AgentInscrit> jsonList = gson.fromJson(
                    reader, new TypeToken<List<AgentInscrit>>() {}.getType());
            reader.close();

            if (jsonList != null) {
                registeredAgents.addAll(jsonList);
                System.out.println("✅ " + registeredAgents.size() + " utilisateurs chargés avec succès depuis le JSON !");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur critique lors de la lecture du JSON via GSON : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Data access ─────────────────────────────────────────────────────────────

    public ObservableList<AgentInscrit> getRegisteredAgents() {
        return registeredAgents;
    }

    // ── Actions ─────────────────────────────────────────────────────────────────

    public void deleteAgent(AgentInscrit agent) {
        if (agent != null) {
            registeredAgents.remove(agent);
        }
    }

    // ── Map HTML generation ─────────────────────────────────────────────────────

    public String generateMapHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<html><head>")
                .append("<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>")
                .append("<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>")
                .append("<style>html, body, #map { height: 100%; margin: 0; padding: 0; }</style>")
                .append("</head><body><div id='map'></div><script>")
                .append("var map = L.map('map').setView([48.85, 2.35], 12);")
                .append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);");

        html.append("L.marker([48.8584, 2.3499]).addTo(map).bindPopup('<b>🏢 CASERNE DE POMPIERS CENTRALE</b>');");
        html.append("L.marker([48.8462, 2.3427]).addTo(map).bindPopup('<b>🏠 REFUGE DE CRISE PRINCIPAL</b>');");

        for (AgentInscrit agent : registeredAgents) {
            if (agent.getPosition() == null) continue;

            String color = "blue";
            if ("admin".equals(agent.getType()))       color = "purple";
            if ("rescueAgent".equals(agent.getType())) color = "red";

            String destination = agent.getDestination() != null ? agent.getDestination() : "Aucune";
            String firstName   = agent.getFirstName().replace("'", "\\'");
            String lastName    = agent.getLastName().replace("'", "\\'");

            html.append("L.circle([").append(agent.getPosition().getLat()).append(", ")
                    .append(agent.getPosition().getLng()).append("], {")
                    .append("color: '").append(color).append("',")
                    .append("fillColor: '").append(color).append("',")
                    .append("fillOpacity: 0.7, radius: 250 })")
                    .append(".addTo(map).bindPopup('")
                    .append("<b>Rôle:</b> ").append(agent.getType())
                    .append("<br><b>Nom:</b> ").append(firstName).append(" ").append(lastName)
                    .append("<br><b>État:</b> ").append(agent.getState())
                    .append("<br><b>Destination:</b> ").append(destination)
                    .append("');");
        }

        html.append("</script></body></html>");
        return html.toString();
    }
}
