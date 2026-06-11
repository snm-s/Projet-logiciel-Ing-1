package model.manager;

import model.zone.Zone;
import model.graph.Edge;
import model.zone.Shelter;
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
 * Responsable du chargement des routes depuis JSON et du tracé GPS via OSRM.
 *
 * <p>Ne connaît ni les agents ni les mouvements. Son seul rôle est de
 * produire la liste d'arêtes ({@link Edge}) qui constitue la topologie.</p>
 */
public class RouteManager {

    private static final Logger LOG       = Logger.getLogger(RouteManager.class.getName());
    private static final String JSON_PATH = "/lyon_routes.json";
    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/driving/";

    // ─────────────────────────────────────────────────────────────────────
    // CHARGEMENT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Charge les arêtes depuis le fichier JSON embarqué.
     * Retourne des routes par défaut si le fichier est absent ou invalide.
     *
     * @param zoneMap zones indexées par id
     * @return liste mutable des arêtes construites
     */
    public List<Edge> loadEdges(Map<Integer, Zone> zoneMap) {
        try (InputStream is = getClass().getResourceAsStream(JSON_PATH)) {
            if (is != null) {
                List<Edge> edges = parseJson(
                    new String(is.readAllBytes(), StandardCharsets.UTF_8), zoneMap);
                if (!edges.isEmpty()) {
                    LOG.info(edges.size() + " routes chargées depuis " + JSON_PATH);
                    return edges;
                }
            }
        } catch (Exception e) {
            LOG.warning("Erreur chargement routes : " + e.getMessage());
        }
        LOG.info("Génération des routes par défaut");
        return generateDefaultEdges(zoneMap);
    }

    // ─────────────────────────────────────────────────────────────────────
    // ROUTES PAR DÉFAUT
    // ─────────────────────────────────────────────────────────────────────

    private List<Edge> generateDefaultEdges(Map<Integer, Zone> zoneMap) {
        // Réseau dense adapté aux zones réellement chargées depuis data/zones.json.
        // Les IDs 8 et 9 sont les refuges dans le jeu de données actuel.
        int[][] connections = {
            {1,2,6},{1,3,6},{1,5,5},{1,6,4},{1,7,4},{1,8,5},{1,9,5},
            {2,3,6},{2,4,5},{2,6,5},{2,8,5},{2,9,5},
            {3,4,5},{3,5,5},{3,7,5},{3,8,4},{3,9,4},
            {4,5,5},{4,7,4},{4,8,4},{4,9,4},
            {5,6,4},{5,7,4},{5,8,5},{5,9,5},
            {6,7,5},{6,8,7},{6,9,5},
            {7,8,5},{7,9,7},
            {8,9,6}
        };
        List<Edge> edges = new ArrayList<>();
        int id = 1;
        for (int[] c : connections) {
            Zone from = zoneMap.get(c[0]);
            Zone to   = zoneMap.get(c[1]);
            if (from == null || to == null) continue;
            edges.add(buildEdge(id++, from.getName() + " → " + to.getName(), from, to, c[2]));
        }
        return edges;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARSING JSON
    // ─────────────────────────────────────────────────────────────────────

    private List<Edge> parseJson(String json, Map<Integer, Zone> zoneMap) {
        List<Edge> edges = new ArrayList<>();
        int rs = json.indexOf("\"routes\"");
        if (rs < 0) return edges;
        int as = json.indexOf('[', rs), ae = json.lastIndexOf(']');
        if (as < 0 || ae < 0) return edges;
        for (String block : splitObjects(json.substring(as + 1, ae))) {
            try {
                int id      = parseInt(block, "\"id\"");
                int fromId  = parseInt(block, "\"fromZoneId\"");
                int toId    = parseInt(block, "\"toZoneId\"");
                String name = parseStr(block, "\"name\"");
                int cap     = parseIntOr(block, "\"capacityMax\"", 1000);
                Zone from   = zoneMap.get(fromId);
                Zone to     = zoneMap.get(toId);
                if (from == null || to == null) continue;
                edges.add(buildEdge(id, name, from, to, cap));
            } catch (Exception e) {
                LOG.warning("Parse route: " + e.getMessage());
            }
        }
        return edges;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION D'UNE ARÊTE
    // ─────────────────────────────────────────────────────────────────────

    /** Crée un Edge en récupérant le tracé OSRM (ou fallback ligne droite). */
    public Edge buildEdge(int id, String name, Zone from, Zone to, int capacityMax) {
        return new Edge(id, name, from, to, fetchOsrmRoute(from, to), capacityMax, 0, 0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // OSRM
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Interroge OSRM pour obtenir le tracé réel entre deux zones.
     * Retourne une ligne droite en cas d'indisponibilité.
     */
    public List<GeoPosition> fetchOsrmRoute(Zone from, Zone to) {
        String url = String.format(Locale.US,
            "%s%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
            OSRM_BASE, from.getLongitude(), from.getLatitude(),
            to.getLongitude(), to.getLatitude());
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4)).build();
            HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder().uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "LyonFloodSim/1.0").GET().build(),
                HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return parseOsrmGeoJson(resp.body());
        } catch (Exception e) {
            LOG.fine("OSRM unavailable: " + e.getMessage());
        }
        return fallbackLine(from, to);
    }

    private List<GeoPosition> parseOsrmGeoJson(String json) {
        List<GeoPosition> result = new ArrayList<>();
        try {
            int ci = json.indexOf("\"coordinates\"");
            if (ci < 0) return fallbackEmpty();
            int ao = json.indexOf('[', ci);
            int depth = 0, start = -1;
            StringBuilder sb = new StringBuilder();
            for (int i = ao; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '[') { if (depth == 0) start = i; depth++; }
                else if (c == ']') { depth--; if (depth == 0) { sb.append(json, start, i + 1); break; } }
            }
            for (String pair : splitArrays(sb.toString().substring(1, sb.length() - 1))) {
                String inner = pair.trim();
                if (!inner.startsWith("[")) continue;
                inner = inner.substring(1, inner.length() - 1);
                String[] p = inner.split(",");
                if (p.length >= 2)
                    result.add(new GeoPosition(
                        Double.parseDouble(p[1].trim()),
                        Double.parseDouble(p[0].trim())));
            }
        } catch (Exception ignored) {}
        return result.isEmpty() ? fallbackEmpty() : result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS JSON
    // ─────────────────────────────────────────────────────────────────────

    private List<String> splitObjects(String json) {
        List<String> r = new ArrayList<>();
        int d = 0, s = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (d == 0) s = i; d++; }
            else if (c == '}') { d--; if (d == 0 && s >= 0) { r.add(json.substring(s, i + 1)); s = -1; } }
        }
        return r;
    }

    private List<String> splitArrays(String json) {
        List<String> r = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') { if (depth == 0) start = i; depth++; }
            else if (c == ']') { depth--; if (depth == 0 && start >= 0) { r.add(json.substring(start, i + 1)); start = -1; } }
        }
        return r;
    }

    private int parseInt(String b, String k) {
        int i = b.indexOf(k), c = b.indexOf(':', i), e = b.indexOf(',', c);
        if (e < 0) e = b.indexOf('}', c);
        return Integer.parseInt(b.substring(c + 1, e).trim());
    }

    private int parseIntOr(String b, String k, int def) {
        try { return parseInt(b, k); } catch (Exception e) { return def; }
    }

    private String parseStr(String b, String k) {
        int i = b.indexOf(k), c = b.indexOf(':', i), q1 = b.indexOf('"', c + 1), q2 = b.indexOf('"', q1 + 1);
        return b.substring(q1 + 1, q2);
    }

    // ─────────────────────────────────────────────────────────────────────
    // FALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public List<GeoPosition> fallbackLine(Zone f, Zone t) {
        return Arrays.asList(
            new GeoPosition(f.getLatitude(), f.getLongitude()),
            new GeoPosition(t.getLatitude(), t.getLongitude()));
    }

    private List<GeoPosition> fallbackEmpty() {
        return new ArrayList<>();
    }
}