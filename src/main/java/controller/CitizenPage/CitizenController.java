package controller.CitizenPage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import model.agent.Agent;
import model.agent.Citizen;
import model.alert.Alert;
import model.alert.AlertSystem;
import model.algorithms.EvacuationPath;
import model.graph.Node;
import model.graph.Route;
import model.simulation.FloodSimulation;
import model.simulation.EvacuationEvent;
import model.zone.Shelter;
import model.zone.Zone;
import model.zone.ZoneManager;

public class CitizenController {

    private final FloodSimulation simulation;

    /**
     * Constructs a new CitizenController.
     * @param simulation the simulation.
     */
    public CitizenController(FloodSimulation simulation) {
        this.simulation = simulation;
    }

    /**
     * Create a CitizenController bound to a FloodSimulation instance.
     *
     * @param simulation the FloodSimulation to read data from
     */

    public FloodSimulation getSimulation() {
        return simulation;
    }

    /**
     * Get the underlying FloodSimulation instance.
     *
     * @return the FloodSimulation
     */

    public AlertSystem getAlertSystem() {
        return simulation.getAlertSystem();
    }

    /**
     * Get the alert system used by the simulation.
     *
     * @return the AlertSystem
     */

    public List<EvacuationEvent> getEvacuationHistoryFor(Agent user) {
        if (user == null) return new ArrayList<>();
        return simulation.getEvacuationHistoryFor(user.getId());
    }

    /**
     * Get the evacuation history for a specific agent.
     *
     * @param user the agent to query
     * @return list of EvacuationEvent records
     */

    public List<EvacuationEvent> getAllEvacuationHistory() {
        return simulation.getEvacuationHistory();
    }


    /**
     * Returns the zones.
     * @return the List<Zone>.
     */
    public List<Zone> getZones() {
        return new ZoneManager().getZones();
    }

    /**
     * Return the list of available zones.
     *
     * @return list of Zone
     */

    public List<Shelter> getShelters() {
        return getZones().stream()
                .filter(z -> z instanceof Shelter)
                .map(z -> (Shelter) z)
                .collect(Collectors.toList());
    }

    /**
     * Return the list of shelters extracted from zones.
     *
     * @return list of Shelter
     */

    public List<Shelter> getSheltersSortedByDistance(Agent user) {
        return getShelters().stream()
                .sorted(Comparator.comparingDouble(shelter -> getDistanceFromUserKm(user, shelter)))
                .collect(Collectors.toList());
    }

    /**
     * Return shelters sorted by distance from the given agent.
     *
     * @param user agent used as reference point
     * @return sorted list of Shelter
     */

    public String getFirstName(Agent user) {
        return user != null && user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName()
                : "Citoyen";
    }

    /**
     * Get a safe first name for display (falls back to "Citoyen").
     *
     * @param user agent
     * @return display first name
     */

    public String getFullName(Agent user) {
        String firstName = getFirstName(user);
        String lastName = user != null && user.getLastName() != null ? user.getLastName() : "";
        return lastName.isBlank() ? firstName : firstName + " " + lastName;
    }

    /**
     * Get the full name of an agent for display.
     *
     * @param user agent
     * @return full name or fallback
     */

    public String getCitizenState(Agent user) {
        if (user instanceof Citizen c) {
            return c.getState() != null ? c.getState().name() : "CALME";
        }
        return "CALME";
    }

    /**
     * Returns whether citizen in panic.
     * @param user the user.
     * @return the boolean result.
     */
    public boolean isCitizenInPanic(Agent user) {
        String state = getCitizenState(user);
        return "PANIQUE".equalsIgnoreCase(state) || "FOLIE".equalsIgnoreCase(state);
    }

    /**
     * Returns the nearest zone.
     * @param user the user.
     * @return the Zone.
     */
    public Zone getNearestZone(Agent user) {
        if (user == null || user.getPosition() == null) {
            return getZones().isEmpty() ? null : getZones().get(0);
        }

        double lat = user.getPosition().getLat();
        double lng = user.getPosition().getLng();

        return getZones().stream()
                .min(Comparator.comparingDouble(z -> distance(lat, lng, z.getLatitude(), z.getLongitude())))
                .orElse(null);
    }

    /**
     * Find the nearest zone to the agent's current position.
     *
     * @param user agent to locate
     * @return nearest Zone or null
     */

    public Zone getNearestSafeRefuge(Agent user) {
        return getShelters().stream()
                .filter(shelter -> !shelter.isFlooded() && !shelter.isEvacuated())
                .min(Comparator.comparingDouble(shelter -> getDistanceFromUserKm(user, shelter)))
                .orElse(null);
    }

    /**
     * Returns the position label.
     * @param user the user.
     * @return the String.
     */
    public String getPositionLabel(Agent user) {
        Zone z = getNearestZone(user);
        if (z != null) {
            return z.getName();
        }
        return "Zone inconnue";
    }

    /**
     * Returns the detailed position label.
     * @param user the user.
     * @return the String.
     */
    public String getDetailedPositionLabel(Agent user) {
        Zone z = getNearestZone(user);
        if (z != null) {
            return z.getName() + " — " + shortDescription(z);
        }
        return "Position inconnue";
    }

    /**
     * Returns the target refuge label.
     * @param user the user.
     * @return the String.
     */
    public String getTargetRefugeLabel(Agent user) {
        Zone refuge = getNearestSafeRefuge(user);
        if (refuge != null) {
            return refuge.getName();
        }
        return "Aucun refuge assigné";
    }

    /**
     * Returns the target refuge details.
     * @param user the user.
     * @return the String.
     */
    public String getTargetRefugeDetails(Agent user) {
        Zone refuge = getNearestSafeRefuge(user);
        if (refuge != null) {
            return refuge.getName() + " — " + shortDescription(refuge);
        }
        return "Aucun refuge assigné";
    }

    /**
     * Returns the evacuation distance km.
     * @param user the user.
     * @return the double result.
     */
    public double getEvacuationDistanceKm(Agent user) {
        Zone to = getNearestSafeRefuge(user);
        if (to == null) {
            return -1;
        }
        return getDistanceFromUserKm(user, to);
    }

    /**
     * Returns the eta minutes.
     * @param user the user.
     * @return the int result.
     */
    public int getEtaMinutes(Agent user) {
        double d = getEvacuationDistanceKm(user);
        if (d < 0) return -1;

        double speed = user != null && user.getMaxSpeed() > 0 ? user.getMaxSpeed() : 4.0;
        return Math.max(3, (int) Math.round((d / speed) * 60));
    }

    /**
     * Returns the distance label.
     * @param user the user.
     * @return the String.
     */
    public String getDistanceLabel(Agent user) {
        double d = getEvacuationDistanceKm(user);
        return d >= 0 ? String.format("%.1f km", d) : "Calcul...";
    }

    /**
     * Returns the eta label.
     * @param user the user.
     * @return the String.
     */
    public String getEtaLabel(Agent user) {
        int eta = getEtaMinutes(user);
        return eta >= 0 ? eta + " min" : "-- min";
    }

    /**
     * Returns the distance from user km.
     * @param user the user.
     * @param zone the zone.
     * @return the double result.
     */
    public double getDistanceFromUserKm(Agent user, Zone zone) {
        if (zone == null) {
            return -1;
        }

        if (user != null && user.getPosition() != null) {
            return distanceKm(
                    user.getPosition().getLat(),
                    user.getPosition().getLng(),
                    zone.getLatitude(),
                    zone.getLongitude()
            );
        }

        Zone nearest = getNearestZone(user);
        if (nearest != null) {
            return distanceKm(
                    nearest.getLatitude(),
                    nearest.getLongitude(),
                    zone.getLatitude(),
                    zone.getLongitude()
            );
        }

        return -1;
    }


    /**
     * Registers evacuation path.
     * @param agent the agent.
     * @param from the from.
     * @param to the to.
     */
    public void registerEvacuationPath(Agent agent, Zone from, Zone to) {
        // Enregistre le chemin dans FloodSimulation pour tracking global
        app.Main.getSharedSimulation().registerCitizenPath(agent, from, to);
    }

    /**
     * Returns the route instructions.
     * @param agent the agent.
     * @param from the from.
     * @param to the to.
     * @return the List<String>.
     */
    public List<String> getRouteInstructions(Agent agent, Zone from, Zone to) {
        List<String> steps = new ArrayList<>();
        if (from == null || to == null) return steps;

        EvacuationPath path = computePath(from, to);


        if (path == null || path.isEmpty()) {
            steps.add("Départ depuis " + from.getName() + " — restez calme et suivez les indications.");
            steps.add("Arrivée au refuge " + to.getName() + " — signalez-vous aux secours.");
            return steps;
        }

        List<Zone> zoneSeq = path.getZones();

        steps.add("Départ depuis " + from.getName() + " — restez calme et suivez les indications.");

        for (int i = 1; i < zoneSeq.size() - 1; i++) {
            Zone z = zoneSeq.get(i);
            String suffix = z.getAltitude() > from.getAltitude() ? " (zone en hauteur, plus sûre)" : "";
            String warning = z.isFlooded() ? " ⚠ zone inondée — traversée rapide !" : "";
            steps.add("Traversez " + z.getName() + suffix + warning + ".");
        }

        long atRiskCount = path.countAtRiskEdges();
        if (atRiskCount > 0)
            steps.add("⚠ Attention : " + atRiskCount + " tronçon(s) à risque sur l'itinéraire — restez vigilant.");

        steps.add("Arrivée au refuge " + to.getName() + " — signalez-vous aux secours.");
        return steps;
    }



    /**
     * Returns the distance from user label.
     * @param user the user.
     * @param zone the zone.
     * @return the String.
     */
    public String getDistanceFromUserLabel(Agent user, Zone zone) {
        double d = getDistanceFromUserKm(user, zone);
        return d >= 0 ? String.format("%.1f km", d) : "Distance inconnue";
    }

    /**
     * Returns the eta to zone minutes.
     * @param user the user.
     * @param zone the zone.
     * @return the int result.
     */
    public int getEtaToZoneMinutes(Agent user, Zone zone) {
        double d = getDistanceFromUserKm(user, zone);
        if (d < 0) return -1;

        double speed = user != null && user.getMaxSpeed() > 0 ? user.getMaxSpeed() : 4.0;
        return Math.max(3, (int) Math.round((d / speed) * 60));
    }

    /**
     * Returns the eta to zone label.
     * @param user the user.
     * @param zone the zone.
     * @return the String.
     */
    public String getEtaToZoneLabel(Agent user, Zone zone) {
        int eta = getEtaToZoneMinutes(user, zone);
        return eta >= 0 ? eta + " min" : "-- min";
    }

    /**
     * Returns the route status label.
     * @param user the user.
     * @return the String.
     */
    public String getRouteStatusLabel(Agent user) {
        return user != null && user.isSaved() ? "Arrivé" : "En cours";
    }

    /**
     * Returns the alert count.
     * @return the int result.
     */
    public int getAlertCount() {
        return simulation.getAlertSystem().getActiveAlerts().size();
    }

    /**
     * Returns the latest critical alert.
     * @return the Alert.
     */
    public Alert getLatestCriticalAlert() {
        return simulation.getAlertSystem().getLatestAlert();
    }

    /**
     * Calculates evacuation route.
     * @param citizenNode the citizenNode.
     * @return the Route.
     */
    public Route calculateEvacuationRoute(Node citizenNode) {
        return new Route();
    }

    /**
     * Computes path.
     * @param from the from.
     * @param to the to.
     * @return the EvacuationPath.
     */
    public EvacuationPath computePath(Zone from, Zone to) {
        if (from == null || to == null) return null;

        model.graph.RouteGraph rg = null;

        if (app.Main.getSharedMapController() != null) {
            rg = app.Main.getSharedMapController().getRouteGraph();
        }

        if (rg == null) {
            // Fallback : construire un RouteGraph temporaire avec les zones de la simulation
            rg = new model.graph.RouteGraph(simulation.getZones());
        }

        return rg.findPathForRescue(from, to);
    }

    /**
     * Compute an evacuation path between two zones using the shared RouteGraph if available.
     *
     * @param from origin zone
     * @param to destination zone
     * @return EvacuationPath or null
     */

    public String shortDescription(Zone zone) {
        if (zone == null || zone.getDescription() == null || zone.getDescription().isBlank()) {
            return "secteur surveillé";
        }

        String d = zone.getDescription();
        return d.length() > 58 ? d.substring(0, 57) + "…" : d;
    }

    /**
     * Performs distance.
     * @param lat1 the lat1.
     * @param lng1 the lng1.
     * @param lat2 the lat2.
     * @param lng2 the lng2.
     * @return the double result.
     */
    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat1 - lat2;
        double dLng = lng1 - lng2;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    /**
     * Performs km.
     * @param lat1 the lat1.
     * @param lon1 the lon1.
     * @param lat2 the lat2.
     * @param lon2 the lon2.
     * @return the double result.
     */
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
