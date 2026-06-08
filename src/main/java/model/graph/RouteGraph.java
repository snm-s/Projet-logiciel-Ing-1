package model.graph;

import model.zone.Zone;
import org.jxmapviewer.viewer.GeoPosition;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

/**
 * Graphe de routes pour la simulation d'inondation de Lyon.
 *
 * <h3>Pipeline de construction :</h3>
 * <ol>
 *   <li>Charge la <em>topologie</em> (zones connectées, capacités) depuis
 *       {@code /resources/lyon_routes.json}.</li>
 *   <li>Pour chaque route, interroge l'API publique OSRM
 *       ({@code router.project-osrm.org}) afin d'obtenir le tracé réel
 *       (polyline encodée en GeoJSON).</li>
 *   <li>En cas d'échec OSRM (hors-ligne, quota…), replie sur la ligne droite
 *       entre les coordonnées des zones.</li>
 * </ol>
 *
 * <h3>Format lyon_routes.json :</h3>
 * <pre>{@code
 * { "routes": [
 *     { "id":1, "fromZoneId":1, "toZoneId":2,
 *       "name":"...", "capacityMax":1200 }
 * ] }
 * }</pre>
 *
 * Cette classe est purement <em>Model</em> : aucune dépendance View/Controller.
 */
public class RouteGraph {

    private static final Logger LOG = Logger.getLogger(RouteGraph.class.getName());
    private static final String JSON_PATH    = "/lyon_routes.json";

    /**
     * URL de base OSRM (routing car, sans géométrie compressée).
     * On utilise le profil "driving" public d'OpenStreetMap.
     */
    private static final String OSRM_BASE =
        "https://router.project-osrm.org/route/v1/driving/";

    private final List<Edge>         edges   = new ArrayList<>();
    private final Map<Integer, Zone> zoneMap = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    public RouteGraph(List<Zone> zones) {
        LOG.info("Construction de RouteGraph pour " + zones.size() + " zones");
        for (Zone z : zones) zoneMap.put(z.getId(), z);
        load();
    }

    // ─────────────────────────────────────────────────────────────────────
    // CHARGEMENT
    // ─────────────────────────────────────────────────────────────────────

    private void load() {
        try (InputStream is = getClass().getResourceAsStream(JSON_PATH)) {
            if (is == null) {
                LOG.warning("lyon_routes.json introuvable — génération des routes par défaut");
                generateDefaultRoutes();
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            parseJson(json);
        } catch (Exception e) {
            LOG.severe("Erreur lecture lyon_routes.json : " + e.getMessage());
            generateDefaultRoutes();
        }
        LOG.info(edges.size() + " routes chargées");
    }

    /**
     * Génère un réseau de routes par défaut entre toutes les zones adjacentes
     * si le fichier JSON est absent.
     */
    private void generateDefaultRoutes() {
        int id = 1;
        // Connexions typiques Lyon : réseau en étoile depuis la Presqu'île (id=1)
        int[][] connections = {
            {1,2,800},{1,3,1000},{1,4,600},{1,5,900},{1,9,700},
            {2,6,500},{2,10,400},{3,7,1100},{5,7,1200},{7,8,1000},
            {6,10,300},{4,9,800},{9,1,700}
        };
        for (int[] c : connections) {
            Zone from = zoneMap.get(c[0]);
            Zone to   = zoneMap.get(c[1]);
            if (from == null || to == null) continue;
            int cap = c[2];
            String name = from.getName() + " → " + to.getName();
            List<GeoPosition> waypoints = fetchOsrmRoute(from, to);
            edges.add(new Edge(id++, name, from, to, waypoints, cap, 0, 0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARSING JSON (minimal, sans dépendance externe)
    // ─────────────────────────────────────────────────────────────────────

    private void parseJson(String json) {
        int routesStart = json.indexOf("\"routes\"");
        if (routesStart < 0) return;

        int arrayStart = json.indexOf('[', routesStart);
        int arrayEnd   = json.lastIndexOf(']');
        if (arrayStart < 0 || arrayEnd < 0) return;

        List<String> routeBlocks =
            splitTopLevelObjects(json.substring(arrayStart + 1, arrayEnd));

        for (String block : routeBlocks) {
            try {
                int    id     = parseInt(block,   "\"id\"");
                int    fromId = parseInt(block,   "\"fromZoneId\"");
                int    toId   = parseInt(block,   "\"toZoneId\"");
                String name   = parseString(block,"\"name\"");
                int    cap    = parseIntOrDefault(block, "\"capacityMax\"", 1000);

                Zone from = zoneMap.get(fromId);
                Zone to   = zoneMap.get(toId);
                if (from == null || to == null) {
                    LOG.warning("Route " + id + " : zone introuvable");
                    continue;
                }

                // Tracé réel via OSRM (avec fallback ligne droite)
                List<GeoPosition> waypoints = fetchOsrmRoute(from, to);
                edges.add(new Edge(id, name, from, to, waypoints, cap, 0, 0));

            } catch (Exception e) {
                LOG.warning("Erreur parsing route : " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // OSRM — tracé réel
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Interroge l'API OSRM publique pour obtenir le tracé de conduite entre
     * deux zones. Retourne les waypoints GPS du tracé réel.
     *
     * <p>En cas d'échec, retourne une ligne droite (2 points).
     */
    private List<GeoPosition> fetchOsrmRoute(Zone from, Zone to) {
        // Coordonnées au format OSRM : longitude,latitude
        String url = String.format(Locale.US,
            "%s%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
            OSRM_BASE,
            from.getLongitude(), from.getLatitude(),
            to.getLongitude(),   to.getLatitude());

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "LyonFloodSim/1.0")
                .GET()
                .build();

            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseOsrmGeoJson(response.body());
            }
        } catch (Exception e) {
            LOG.fine("OSRM indisponible pour " + from.getName()
                + " → " + to.getName() + " : " + e.getMessage());
        }

        // Fallback : ligne droite
        return fallbackStraightLine(from, to);
    }

    /**
     * Parse le GeoJSON OSRM et extrait les coordonnées de la géométrie.
     * Format attendu :
     * <pre>
     * { "routes": [{ "geometry": { "coordinates": [[lng,lat], ...] } }] }
     * </pre>
     */
    private List<GeoPosition> parseOsrmGeoJson(String json) {
        List<GeoPosition> result = new ArrayList<>();
        try {
            int coordStart = json.indexOf("\"coordinates\"");
            if (coordStart < 0) return fallbackEmpty();

            int arrOpen = json.indexOf('[', coordStart);
            // On cherche le tableau de tableaux [[lng,lat],...]
            int depth = 0;
            int start = -1;
            StringBuilder coordsBuilder = new StringBuilder();
            for (int i = arrOpen; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '[') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        coordsBuilder.append(json, start, i + 1);
                        break;
                    }
                }
            }
            String coordsJson = coordsBuilder.toString();

            // Chaque paire : [lng, lat]  (OSRM retourne lon,lat)
            // On cherche les sous-tableaux [num,num]
            List<String> pairs = splitTopLevelArrays(
                coordsJson.substring(1, coordsJson.length() - 1));

            for (String pair : pairs) {
                String inner = pair.trim();
                if (!inner.startsWith("[")) continue;
                inner = inner.substring(1, inner.length() - 1).trim();
                String[] parts = inner.split(",");
                if (parts.length < 2) continue;
                double lng = Double.parseDouble(parts[0].trim());
                double lat = Double.parseDouble(parts[1].trim());
                result.add(new GeoPosition(lat, lng));
            }
        } catch (Exception e) {
            LOG.fine("Erreur parse GeoJSON OSRM : " + e.getMessage());
        }
        return result.isEmpty() ? fallbackEmpty() : result;
    }

    /** Découpe un JSON array en sous-tableaux de premier niveau. */
    private List<String> splitTopLevelArrays(String json) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return result;
    }

    private List<GeoPosition> fallbackStraightLine(Zone from, Zone to) {
        List<GeoPosition> pts = new ArrayList<>();
        pts.add(new GeoPosition(from.getLatitude(), from.getLongitude()));
        pts.add(new GeoPosition(to.getLatitude(),   to.getLongitude()));
        return pts;
    }

    private List<GeoPosition> fallbackEmpty() {
        return new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS PARSING
    // ─────────────────────────────────────────────────────────────────────

    private int parseInt(String block, String key) {
        int idx = block.indexOf(key);
        if (idx < 0) throw new IllegalArgumentException("Clé manquante : " + key);
        int colon = block.indexOf(':', idx);
        int end   = block.indexOf(',', colon);
        if (end < 0) end = block.indexOf('}', colon);
        return Integer.parseInt(block.substring(colon + 1, end).trim());
    }

    private int parseIntOrDefault(String block, String key, int def) {
        try { return parseInt(block, key); }
        catch (Exception e) { return def; }
    }

    private String parseString(String block, String key) {
        int idx = block.indexOf(key);
        if (idx < 0) return "";
        int colon  = block.indexOf(':', idx);
        int qOpen  = block.indexOf('"', colon + 1);
        int qClose = block.indexOf('"', qOpen + 1);
        return block.substring(qOpen + 1, qClose);
    }

    private List<String> splitTopLevelObjects(String json) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // API PUBLIQUE
    // ─────────────────────────────────────────────────────────────────────

    /** Liste non-modifiable de tous les edges. */
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /** Recalcule l'état de tous les edges ; les changements notifient les observers. */
    public void refreshAllEdges() {
        for (Edge edge : edges) edge.refreshState();
    }

    /** Edges impliquant une zone donnée. */
    public List<Edge> getEdgesForZone(Zone zone) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.getFromZone().getId() == zone.getId()
             || e.getToZone().getId()   == zone.getId()) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Met à jour le flux de tous les edges selon les populations des zones
     * (simple heuristique : population moyenne × facteur d'urgence).
     * Appelé par le Controller après chaque étape de simulation.
     */
    public void simulateFlows() {
        for (Edge edge : edges) {
            Zone from = edge.getFromZone();
            Zone to   = edge.getToZone();
            // Heuristique : si une zone est inondée, ses habitants fuient
            int flow = 0;
            if (from.isFlooded()) {
                flow += (int)(from.getPopulation() * 0.3);
            }
            if (to.isFlooded()) {
                flow += (int)(to.getPopulation() * 0.3);
            }
            edge.setCurrentFlow(flow); // déclenche refreshState() + notify
        }
    }
}