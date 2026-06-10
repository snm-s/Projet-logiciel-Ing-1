package model.simulation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.auth.JsonMapper;
import model.enums.CitizenState;
import model.graph.Node;
import model.zone.Shelter;
import model.zone.Zone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service de persistance JSON pour zones et agents.
 *
 * Responsabilités :
 * <ul>
 *   <li>Charger zones et agents depuis les fichiers JSON au démarrage.</li>
 *   <li>Sauvegarder immédiatement tout ajout / suppression d'agent ou de zone.</li>
 *   <li>Conserver un snapshot immuable de l'état initial pour le {@link #reset()}.</li>
 * </ul>
 *
 * Les chemins par défaut sont relatifs au répertoire de travail :
 * {@code data/zones.json} et {@code data/agents.json}.
 * Ils peuvent être surchargés via le constructeur.
 */
public class SimulationDataService {

    // ─── Chemins JSON ──────────────────────────────────────────────────────
    private final Path zonesPath;
    private final Path agentsPath;
        private final List<Runnable> listeners = new ArrayList<>();

    // ─── Mapper Jackson ────────────────────────────────────────────────────
    private static final ObjectMapper mapper = JsonMapper.INSTANCE;

    // ─── Snapshots initiaux (sérialisés une fois au démarrage) ────────────
    private byte[] zonesSnapshot;
    private byte[] agentsSnapshot;

    // ─── Nœuds Lyon pour placement aléatoire ─────────────────────────────
    private static final double[][] LYON_COORDS = {
        {45.7640, 4.8357}, {45.7580, 4.8320}, {45.7700, 4.8400},
        {45.7490, 4.8250}, {45.7750, 4.8450}, {45.7610, 4.8500},
        {45.7530, 4.8180}, {45.7680, 4.8270}, {45.7440, 4.8350},
        {45.7810, 4.8390}
    };

    private static final Random RNG = new Random();

    // ─────────────────────────────────────────────────────────────────────
    public SimulationDataService() {
        this(
            Paths.get("data", "zones.json"),
            Paths.get("data", "users.json")
        );
    }

    public SimulationDataService(Path zonesPath, Path agentsPath) {
        this.zonesPath  = zonesPath;
        this.agentsPath = agentsPath;
        ensureDataDir();
    }

    // ─────────────────────────────────────────────────────────────────────
    // CHARGEMENT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Charge les zones depuis le JSON.
     * Si le fichier n'existe pas, retourne une liste vide.
     */
    public List<Zone> loadZones() {
        if (!Files.exists(zonesPath)) return new ArrayList<>();
        try {
            List<Zone> zones = mapper.readValue(zonesPath.toFile(),
                new TypeReference<List<Zone>>() {});
            zonesSnapshot = Files.readAllBytes(zonesPath);
            return zones != null ? zones : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("[DataService] Erreur lecture zones.json : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Charge les agents depuis le JSON.
     * Si le fichier n'existe pas, retourne une liste vide.
     */
    public List<Agent> loadAgents() {
        if (!Files.exists(agentsPath)) return new ArrayList<>();
        try {
            List<Agent> agents = mapper.readValue(agentsPath.toFile(),
                new TypeReference<List<Agent>>() {});
            agentsSnapshot = Files.readAllBytes(agentsPath);
            return agents != null ? agents : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("[DataService] Erreur lecture agents.json : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Prend un snapshot de l'état courant des listes (pour reset ultérieur).
     * À appeler une fois que la simulation est initialisée avec ses données de départ.
     */
    public void takeSnapshot(List<Zone> zones, List<Agent> agents) {
        try {
            zonesSnapshot  = mapper.writerFor(new TypeReference<List<Zone>>() {})
                                .writeValueAsBytes(zones);
            agentsSnapshot = mapper.writerFor(new TypeReference<List<Agent>>() {})
                                .writeValueAsBytes(agents);
        } catch (IOException e) {
            System.err.println("[DataService] Erreur snapshot : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SAUVEGARDE
    // ─────────────────────────────────────────────────────────────────────

    public void saveZones(List<Zone> zones) {
        writeSafely(zonesPath, zones);
    }

    public void saveAgents(List<Agent> agents) {
        writeSafely(agentsPath, agents);
    }

    /** Sauvegarde les deux fichiers d'un coup. */
    public void saveAll(List<Zone> zones, List<Agent> agents) {
        saveZones(zones);
        saveAgents(agents);
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESET
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Restaure l'état initial depuis les snapshots.
     *
     * @return tableau de deux listes : [0] zones, [1] agents.
     *         Retourne des listes vides si aucun snapshot n'est disponible.
     */
    @SuppressWarnings("unchecked")
    public Object[] reset() {
        List<Zone>  zones  = new ArrayList<>();
        List<Agent> agents = new ArrayList<>();
        try {
            if (zonesSnapshot != null)
                zones  = mapper.readValue(zonesSnapshot,  new TypeReference<List<Zone>>()  {});
            if (agentsSnapshot != null)
                agents = mapper.readValue(agentsSnapshot, new TypeReference<List<Agent>>() {});
        } catch (IOException e) {
            System.err.println("[DataService] Erreur reset : " + e.getMessage());
        }

        return new Object[]{ zones, agents };
    }

    // ─────────────────────────────────────────────────────────────────────
    // CRÉATION D'AGENTS ALÉATOIRES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Crée un {@link Citizen} avec attributs aléatoires positionné sur une
     * vraie coordonnée de Lyon.
     *
     * @param id identifiant unique à attribuer
     * @param zones liste des zones existantes pour ancrer l'agent
     */
    public Citizen createRandomCitizen(int id, List<Zone> zones) {
        double[] coord = pickCoord(zones);
        Citizen c = new Citizen(
            id,
            randomFirstName(),
            "Auto" + id,
            new Node(coord[0], coord[1])
        );

        CitizenState[] states = {CitizenState.CALM, CitizenState.CALM, CitizenState.CALM,
                                 CitizenState.INJURED, CitizenState.PMR};
        c.setState(states[RNG.nextInt(states.length)]);
        c.setMaxSpeed(1.2 + RNG.nextDouble() * 3.8);           // 1.2 → 5.0 m/s
        c.setCongestionTolerance(0.3 + RNG.nextDouble() * 0.7); // 0.3 → 1.0
        return c;
    }

    /**
     * Crée un {@link RescueAgent} avec attributs aléatoires.
     *
     * @param id identifiant unique
     * @param zones liste des zones pour le placement
     */
    public RescueAgent createRandomRescueAgent(int id, List<Zone> zones) {
        double[] coord = pickCoord(zones);
        RescueAgent ra = new RescueAgent(
            id,
            randomRescueName(),
            "Secours" + id,
            new Node(coord[0], coord[1])
        );
        ra.setMaxSpeed(3.0 + RNG.nextDouble() * 2.0);
        ra.setCongestionTolerance(0.8 + RNG.nextDouble() * 0.2);
        return ra;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────

    /** Choisit des coordonnées : d'abord depuis une zone réelle, sinon depuis LYON_COORDS. */
    private double[] pickCoord(List<Zone> zones) {
        if (zones != null && !zones.isEmpty()) {
            Zone z = zones.get(RNG.nextInt(zones.size()));
            // Légère dispersion autour du centre de la zone (±0.002°)
            double jitterLat = (RNG.nextDouble() - 0.5) * 0.004;
            double jitterLng = (RNG.nextDouble() - 0.5) * 0.004;
            return new double[]{ z.getLatitude() + jitterLat, z.getLongitude() + jitterLng };
        }
        return LYON_COORDS[RNG.nextInt(LYON_COORDS.length)];
    }

    private void ensureDataDir() {
        try {
            if (zonesPath.getParent() != null)
                Files.createDirectories(zonesPath.getParent());
        } catch (IOException ignored) {}
    }

    // APRÈS — TypeReference selon le type de données :
    private void writeSafely(Path path, Object data) {
        try {
            if (data instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Agent) {
                    mapper.writerWithDefaultPrettyPrinter()
                        .forType(new TypeReference<List<Agent>>() {})
                        .writeValue(path.toFile(), data);
                } else if (first instanceof Zone) {
                    mapper.writerWithDefaultPrettyPrinter()
                        .forType(new TypeReference<List<Zone>>() {})
                        .writeValue(path.toFile(), data);
                } else {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
                }
            } else {
                mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
            }
        } catch (IOException e) {
            System.err.println("[DataService] Erreur écriture " + path + " : " + e.getMessage());
        }
    }


    public void addListener(Runnable r) {
        listeners.add(r);
    }

    private void notifyChange() {
        listeners.forEach(Runnable::run);
    }

    private static final String[] FIRST_NAMES = {
        "Alice","Bob","Clara","David","Emma","François","Gabi","Hugo",
        "Inès","Jules","Karim","Léa","Marc","Nina","Omar","Paul"
    };
    private static final String[] RESCUE_NAMES = {
        "Pompier","Secouriste","Infirmier","SAMU","Police","Militaire"
    };

    private String randomFirstName()  { return FIRST_NAMES [RNG.nextInt(FIRST_NAMES.length)];  }
    private String randomRescueName() { return RESCUE_NAMES[RNG.nextInt(RESCUE_NAMES.length)]; }
}