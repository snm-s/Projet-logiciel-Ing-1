package controller.CitizenPage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import model.agent.Agent;
import model.agent.Citizen;
import model.alert.Alert;
import model.alert.AlertSystem;
import model.graph.Node;
import model.graph.Route;
import model.simulation.FloodSimulation;
import model.zone.Shelter;
import model.zone.Zone;
import model.zone.ZoneManager;

public class CitizenController {

    private final FloodSimulation simulation;

    public CitizenController(FloodSimulation simulation) {
        this.simulation = simulation;
    }

    public FloodSimulation getSimulation() {
        return simulation;
    }

    public AlertSystem getAlertSystem() {
        return simulation.getAlertSystem();
    }

    public List<Zone> getZones() {
        return new ZoneManager().getZones();
    }

    public List<Shelter> getShelters() {
        return getZones().stream()
                .filter(z -> z instanceof Shelter)
                .map(z -> (Shelter) z)
                .collect(Collectors.toList());
    }

    public List<Shelter> getSheltersSortedByDistance(Agent user) {
        return getShelters().stream()
                .sorted(Comparator.comparingDouble(shelter -> getDistanceFromUserKm(user, shelter)))
                .collect(Collectors.toList());
    }

    public String getFirstName(Agent user) {
        return user != null && user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName()
                : "Citoyen";
    }

    public String getFullName(Agent user) {
        String firstName = getFirstName(user);
        String lastName = user != null && user.getLastName() != null ? user.getLastName() : "";
        return lastName.isBlank() ? firstName : firstName + " " + lastName;
    }

    public String getCitizenState(Agent user) {
        if (user instanceof Citizen c) {
            return c.getState() != null ? c.getState().name() : "CALME";
        }
        return "CALME";
    }

    public boolean isCitizenInPanic(Agent user) {
        String state = getCitizenState(user);
        return "PANIQUE".equalsIgnoreCase(state) || "FOLIE".equalsIgnoreCase(state);
    }

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

    public Zone getNearestSafeRefuge(Agent user) {
        return getShelters().stream()
                .filter(shelter -> !shelter.isFlooded() && !shelter.isEvacuated())
                .min(Comparator.comparingDouble(shelter -> getDistanceFromUserKm(user, shelter)))
                .orElse(null);
    }

    public String getPositionLabel(Agent user) {
        Zone z = getNearestZone(user);
        if (z != null) {
            return z.getName();
        }
        return "Zone inconnue";
    }

    public String getDetailedPositionLabel(Agent user) {
        Zone z = getNearestZone(user);
        if (z != null) {
            return z.getName() + " — " + shortDescription(z);
        }
        return "Position inconnue";
    }

    public String getTargetRefugeLabel(Agent user) {
        Zone refuge = getNearestSafeRefuge(user);
        if (refuge != null) {
            return refuge.getName();
        }
        return "Aucun refuge assigné";
    }

    public String getTargetRefugeDetails(Agent user) {
        Zone refuge = getNearestSafeRefuge(user);
        if (refuge != null) {
            return refuge.getName() + " — " + shortDescription(refuge);
        }
        return "Aucun refuge assigné";
    }

    public double getEvacuationDistanceKm(Agent user) {
        Zone to = getNearestSafeRefuge(user);
        if (to == null) {
            return -1;
        }
        return getDistanceFromUserKm(user, to);
    }

    public int getEtaMinutes(Agent user) {
        double d = getEvacuationDistanceKm(user);
        if (d < 0) return -1;

        double speed = user != null && user.getMaxSpeed() > 0 ? user.getMaxSpeed() : 4.0;
        return Math.max(3, (int) Math.round((d / speed) * 60));
    }

    public String getDistanceLabel(Agent user) {
        double d = getEvacuationDistanceKm(user);
        return d >= 0 ? String.format("%.1f km", d) : "Calcul...";
    }

    public String getEtaLabel(Agent user) {
        int eta = getEtaMinutes(user);
        return eta >= 0 ? eta + " min" : "-- min";
    }

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

    public String getDistanceFromUserLabel(Agent user, Zone zone) {
        double d = getDistanceFromUserKm(user, zone);
        return d >= 0 ? String.format("%.1f km", d) : "Distance inconnue";
    }

    public int getEtaToZoneMinutes(Agent user, Zone zone) {
        double d = getDistanceFromUserKm(user, zone);
        if (d < 0) return -1;

        double speed = user != null && user.getMaxSpeed() > 0 ? user.getMaxSpeed() : 4.0;
        return Math.max(3, (int) Math.round((d / speed) * 60));
    }

    public String getEtaToZoneLabel(Agent user, Zone zone) {
        int eta = getEtaToZoneMinutes(user, zone);
        return eta >= 0 ? eta + " min" : "-- min";
    }

    public String getRouteStatusLabel(Agent user) {
        return user != null && user.isSaved() ? "Arrivé" : "En cours";
    }

    public int getAlertCount() {
        return simulation.getAlertSystem().getActiveAlerts().size();
    }

    public Alert getLatestCriticalAlert() {
        return simulation.getAlertSystem().getLatestAlert();
    }

    public Route calculateEvacuationRoute(Node citizenNode) {
        return new Route();
    }

    public String shortDescription(Zone zone) {
        if (zone == null || zone.getDescription() == null || zone.getDescription().isBlank()) {
            return "secteur surveillé";
        }

        String d = zone.getDescription();
        return d.length() > 58 ? d.substring(0, 57) + "…" : d;
    }

    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat1 - lat2;
        double dLng = lng1 - lng2;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

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
