package model.graph;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jxmapviewer.viewer.GeoPosition;

import model.agent.Agent;
import model.agent.RescueAgent;
import model.algorithms.EvacuationPath;
import model.algorithms.EvacuationRouter;
import model.zone.Shelter;
import model.zone.Zone;

/**
 * Graphe de routes pour la simulation d'inondation.
 *
 * <p>Rôles :
 * <ul>
 *   <li>Maintient la topologie du réseau routier (arêtes)</li>
 *   <li>Expose l'{@link EvacuationRouter} pour le calcul de chemins</li>
 *   <li>Gère les {@link AgentMovement} actifs (déplacements en cours)</li>
 *   <li>Fournit {@link #tick(double)} appelé à chaque pas de simulation</li>
 * </ul>
 */
public class RouteGraph {

    private static final Logger LOG       = Logger.getLogger(RouteGraph.class.getName());
    private static final String JSON_PATH = "/lyon_routes.json";
    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/driving/";

    // ─── Topologie ────────────────────────────────────────────────────────
    private final List<Edge>         edges   = new ArrayList<>();
    private final Map<Integer, Zone> zoneMap = new HashMap<>();

    // ─── Routeur ──────────────────────────────────────────────────────────
    private final EvacuationRouter router;

    // ─── Déplacements actifs ──────────────────────────────────────────────
    /** Liste thread-safe des mouvements en cours. */
    private final CopyOnWriteArrayList<AgentMovement> activeMovements
        = new CopyOnWriteArrayList<>();

    /** Listeners notifiés quand un agent arrive à destination. */
    public interface ArrivalListener {
        void onAgentArrived(AgentMovement movement);
        void onAgentBlocked(AgentMovement movement);
    }
    private final List<ArrivalListener> arrivalListeners = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    public RouteGraph(List<Zone> zones) {
        for (Zone z : zones) zoneMap.put(z.getId(), z);
        load();
        this.router = new EvacuationRouter(this);
    }

    private void load() {
        try (InputStream is = getClass().getResourceAsStream(JSON_PATH)) {
            if (is == null) { generateDefaultRoutes(); return; }
            parseJson(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.warning("Erreur chargement routes : " + e.getMessage());
            generateDefaultRoutes();
        }
        LOG.info(edges.size() + " routes chargées");
    }

    private void generateDefaultRoutes() {
        int[][] connections = {
            {1, 2, 1200}, // Bellecour -> Part-Dieu
            {1, 3, 900},  // Bellecour -> Guillotière
            {1, 5, 900},  // Bellecour -> Confluence
            {1, 7, 700},  // Bellecour -> Fourvière
    
            {2, 3, 700},  // Part-Dieu -> Guillotière
            {2, 6, 1000}, // Part-Dieu -> Croix-Rousse
    
            {3, 4, 900},  // Guillotière -> Gerland
            {4, 5, 700},  // Gerland -> Confluence
    
            {6, 8, 600},  // Croix-Rousse -> Refuge Croix-Rousse
            {7, 9, 600},  // Fourvière -> Refuge Fourvière
    
            {6, 7, 800}   // Croix-Rousse -> Fourvière
        };
    
        int id = 1;
    
        for (int[] c : connections) {
            Zone from = zoneMap.get(c[0]);
            Zone to = zoneMap.get(c[1]);
    
            if (from == null || to == null) continue;
    
            List<GeoPosition> wp = fetchOsrmRoute(from, to);
    
            edges.add(new Edge(
                id++,
                from.getName() + " → " + to.getName(),
                from,
                to,
                wp,
                c[2],
                0,
                0
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DES DÉPLACEMENTS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Planifie l'évacuation d'un agent depuis sa zone courante vers
     * la zone sûre la plus proche.
     *
     * @param agent  agent à évacuer
     * @param from   zone de départ (position actuelle)
     * @param zones  toutes les zones (pour trouver la destination sûre)
     * @return le mouvement créé, ou null si aucun chemin possible
     */
    public AgentMovement planEvacuation(Agent agent, Zone from, List<Zone> zones) {
        // IMPORTANT : le but d'une évacuation est un vrai refuge, pas une zone aléatoire.
        // On ne garde donc que les objets Shelter chargés depuis zones.json.
        List<Zone> safeZones = zones.stream()
            .filter(z -> z instanceof Shelter)
            .filter(z -> !z.isFlooded() && z.getId() != from.getId())
            .collect(Collectors.toList());

        if (safeZones.isEmpty()) {
            LOG.warning("Aucun refuge disponible dans zones.json : évacuation impossible depuis " + from.getName());
            return null;
        }

        EvacuationPath path = router.findNearestSafe(from, safeZones);
        if (path == null) {
            LOG.fine("Aucun chemin trouvé depuis " + from.getName()
                + " pour l'agent " + agent.getId());
            return null;
        }

        AgentMovement movement = new AgentMovement(agent, path);
        movement.start();
        activeMovements.add(movement);
        LOG.fine("Évacuation planifiée : " + path);
        return movement;
    }

    /**
     * Planifie le déplacement d'un RescueAgent vers une zone cible.
     */
    public AgentMovement planRescueMission(RescueAgent agent, Zone from, Zone to) {
        EvacuationPath path = router.findPath(from, to);
        if (path == null) return null;

        AgentMovement movement = new AgentMovement(agent, path);
        movement.start();
        activeMovements.add(movement);
        return movement;
    }

    /**
     * Avance tous les déplacements d'un pas de simulation.
     * Doit être appelé par le SimulationController à chaque tick.
     *
     * @param deltaSeconds durée du pas simulé (ex : 1.0)
     */
    public void tick(double deltaSeconds) {
        List<AgentMovement> toRemove = new ArrayList<>();

        for (AgentMovement mv : activeMovements) {
            boolean arrived = mv.step(deltaSeconds);
            if (arrived) {
                toRemove.add(mv);
                notifyArrived(mv);
            } else if (mv.isBlocked()) {
                toRemove.add(mv);
                notifyBlocked(mv);
                // Replanifier depuis la position actuelle si possible
                replanBlocked(mv);
            }
        }
        activeMovements.removeAll(toRemove);
    }

    /** Replanifie un agent bloqué depuis sa position actuelle. */
    private void replanBlocked(AgentMovement blocked) {
        Agent agent = blocked.getAgent();
        // On retrouve la zone la plus proche de la position actuelle
        GeoPosition pos = blocked.getCurrentPosition();
        Zone closestZone = findClosestZone(pos);
        if (closestZone == null || closestZone.isFlooded()) return;

        List<Zone> allZones = new ArrayList<>(zoneMap.values());
        AgentMovement newMovement = planEvacuation(agent, closestZone, allZones);
        if (newMovement != null)
            LOG.fine("Agent " + agent.getId() + " replanifié depuis " + closestZone.getName());
    }

    private Zone findClosestZone(GeoPosition pos) {
        Zone closest = null;
        double minDist = Double.MAX_VALUE;
        for (Zone z : zoneMap.values()) {
            double d = Math.abs(z.getLatitude() - pos.getLatitude())
                     + Math.abs(z.getLongitude() - pos.getLongitude());
            if (d < minDist) { minDist = d; closest = z; }
        }
        return closest;
    }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR D'ÉTAT (appelée par la simulation)
    // ─────────────────────────────────────────────────────────────────────

    /** Recalcule l'état de tous les edges. */
    public void refreshAllEdges() {
        for (Edge edge : edges) edge.refreshState();
    }

    /** Met à jour les flux selon les populations inondées. */
    public void simulateFlows() {
        // Les consignes parlent de capacité d'arête en nombre d'agents.
        // On affiche donc la congestion selon les agents réellement en déplacement,
        // pas selon la population théorique des quartiers.
        for (Edge edge : edges) edge.setCurrentFlow(0);
        for (AgentMovement mv : activeMovements) {
            if (mv == null || mv.getPath() == null) continue;
            for (Edge edge : mv.getPath().getEdges()) {
                edge.addFlow(1);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // LISTENERS
    // ─────────────────────────────────────────────────────────────────────

    public void addArrivalListener(ArrivalListener l)    { arrivalListeners.add(l); }
    public void removeArrivalListener(ArrivalListener l) { arrivalListeners.remove(l); }

    private void notifyArrived(AgentMovement mv) {
        for (ArrivalListener l : arrivalListeners) l.onAgentArrived(mv);
    }
    private void notifyBlocked(AgentMovement mv) {
        for (ArrivalListener l : arrivalListeners) l.onAgentBlocked(mv);
    }

    // ─────────────────────────────────────────────────────────────────────
    // OSRM
    // ─────────────────────────────────────────────────────────────────────

    private List<GeoPosition> fetchOsrmRoute(Zone from, Zone to) {
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

    private List<GeoPosition> fallbackLine(Zone f, Zone t) {
        return Arrays.asList(
            new GeoPosition(f.getLatitude(), f.getLongitude()),
            new GeoPosition(t.getLatitude(), t.getLongitude()));
    }
    private List<GeoPosition> fallbackEmpty() { return new ArrayList<>(); }

    // ─────────────────────────────────────────────────────────────────────
    // PARSING JSON
    // ─────────────────────────────────────────────────────────────────────

    private void parseJson(String json) {
        int rs = json.indexOf("\"routes\"");
        if (rs < 0) { generateDefaultRoutes(); return; }
        int as = json.indexOf('[', rs), ae = json.lastIndexOf(']');
        if (as < 0 || ae < 0) return;
        for (String block : splitObjects(json.substring(as + 1, ae))) {
            try {
                int id     = parseInt(block, "\"id\"");
                int fromId = parseInt(block, "\"fromZoneId\"");
                int toId   = parseInt(block, "\"toZoneId\"");
                String name = parseStr(block, "\"name\"");
                int cap    = parseIntOr(block, "\"capacityMax\"", 1000);
                Zone from  = zoneMap.get(fromId);
                Zone to    = zoneMap.get(toId);
                if (from == null || to == null) continue;
                edges.add(new Edge(id, name, from, to, fetchOsrmRoute(from, to), cap, 0, 0));
            } catch (Exception e) { LOG.warning("Parse route: " + e.getMessage()); }
        }
    }

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

    private int parseInt(String b, String k) {
        int i = b.indexOf(k), c = b.indexOf(':', i), e = b.indexOf(',', c);
        if (e < 0) e = b.indexOf('}', c);
        return Integer.parseInt(b.substring(c + 1, e).trim());
    }
    private int parseIntOr(String b, String k, int def) { try { return parseInt(b, k); } catch (Exception e) { return def; } }
    private String parseStr(String b, String k) {
        int i = b.indexOf(k), c = b.indexOf(':', i), q1 = b.indexOf('"', c + 1), q2 = b.indexOf('"', q1 + 1);
        return b.substring(q1 + 1, q2);
    }

    // ─────────────────────────────────────────────────────────────────────
    // API PUBLIQUE
    // ─────────────────────────────────────────────────────────────────────

    public List<Edge>           getEdges()           { return Collections.unmodifiableList(edges); }
    public EvacuationRouter     getRouter()          { return router; }
    public List<AgentMovement>  getActiveMovements() { return Collections.unmodifiableList(activeMovements); }

    public void removeMovementsOfAgent(int agentId) {
        activeMovements.removeIf(mv ->
            mv != null &&
            mv.getAgent() != null &&
            mv.getAgent().getId() == agentId
        );
    
        simulateFlows();
        refreshAllEdges();
    }

    public List<Edge> getEdgesForZone(Zone zone) {
        return edges.stream()
            .filter(e -> e.getFromZone().getId() == zone.getId()
                      || e.getToZone().getId()   == zone.getId())
            .collect(Collectors.toList());
    }
}