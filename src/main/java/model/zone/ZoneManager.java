package model.zone;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Gestionnaire de zones : charge les zones depuis un fichier JSON et les expose
 * en liste
 */
public class ZoneManager {
    private List<Zone> zones;
    private static final String DEFAULT_ZONES_FILE = "data/zones.json";

    public ZoneManager() {
        this.zones = new ArrayList<>();
        loadZonesFromFile(DEFAULT_ZONES_FILE);
    }

    private void loadZonesFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("⚠️ Fichier zones non trouvé : " + filePath);
                createDefaultZones();
                return;
            }

            Gson gson = new Gson();
            FileReader reader = new FileReader(file);
            List<ZoneData> zoneDataList = gson.fromJson(reader, new TypeToken<List<ZoneData>>() {
            }.getType());
            reader.close();

            if (zoneDataList != null) {
                for (ZoneData data : zoneDataList) {
                    Zone zone;
                    
                    // Détection du type
                    if ("shelter".equalsIgnoreCase(data.type)) {
                        zone = new Shelter(data.id, data.name, data.latitude, data.longitude, 
                                        data.altitude, data.population, data.description, data.capacity);
                    } else {
                        // Par défaut, on crée un Neighborhood
                        zone = new Neighborhood(data.id, data.name, data.latitude, data.longitude, 
                                                data.altitude, data.population, data.description);
                    }
                    
                    zones.add(zone);
                }
            System.out.println("✅ " + zones.size() + " zones chargees depuis zones.json");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la lecture des zones : " + e.getMessage());
            createDefaultZones();
        }
    }

    private void createDefaultZones() {
        zones.clear();
        zones.add(new Neighborhood(1, "Centre-ville", 45.7649, 4.8357, 1.2, 850, "Zone urbaine centrale"));
        zones.add(new Neighborhood(2, "Confluence", 45.7324, 4.8210, 0.8, 320, "Zone musées et publics"));
        zones.add(new Neighborhood(3, "Parc Tête d'Or", 45.7698, 4.8537, 1.5, 150, "Parc urbain"));
    }

    public List<Zone> getZones() {
        return new ArrayList<>(zones);
    }

    public Zone getZoneById(int id) {
        return zones.stream().filter(z -> z.getId() == id).findFirst().orElse(null);
    }

    public void resetAllZones() {
        zones.forEach(Zone::reset);
    }

    // Classe interne pour deserialize le JSON
    public static class ZoneData {
        public int id;
        public String name;
        public String type;
        public double latitude;
        public double longitude;
        public double altitude;
        public int population;
        public String description;
        public int capacity;
    }
}
