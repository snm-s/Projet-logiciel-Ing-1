package model.strategy;

import java.util.Comparator;
import java.util.List;

import model.agent.Agent;
import model.graph.Node;
import model.zone.Zone;

public interface Strategy {
    Node chooseDestination(Agent agent, List<Zone> zones);

    default void apply(Agent agent, List<Zone> zones) {
        if (agent == null || zones == null || zones.isEmpty()) {
            return;
        }

        Node destination = chooseDestination(agent, zones);
        if (destination != null) {
            agent.setDestination(destination);
        }
    }

    static boolean isSafe(Zone zone) {
        return zone != null && !zone.isFlooded();
    }

    static Node createNodeFromZone(Zone zone) {
        if (zone == null) {
            return null;
        }
        return new Node(zone.getLatitude(), zone.getLongitude());
    }

    static double computeDistance(Node a, Node b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.getLat() - b.getLat();
        double dy = a.getLng() - b.getLng();
        return Math.sqrt(dx * dx + dy * dy);
    }

    static Node findHighestSafeZone(List<Zone> zones) {
        if (zones == null || zones.isEmpty()) {
            return null;
        }
        return zones.stream()
                .filter(Strategy::isSafe)
                .max(Comparator.comparingDouble(Zone::getAltitude))
                .map(Strategy::createNodeFromZone)
                .orElse(null);
    }

    static Node findNearestSafeZone(Node reference, List<Zone> zones) {
        if (reference == null || zones == null || zones.isEmpty()) {
            return null;
        }
        return zones.stream()
                .filter(Strategy::isSafe)
                .min(Comparator.comparingDouble(zone -> computeDistance(reference, createNodeFromZone(zone))))
                .map(Strategy::createNodeFromZone)
                .orElse(null);
    }

    static Node findNearestFloodedZone(Node reference, List<Zone> zones) {
        if (reference == null || zones == null || zones.isEmpty()) {
            return null;
        }
        return zones.stream()
                .filter(zone -> zone.isFlooded() && !zone.isEvacuated())
                .min(Comparator.comparingDouble(zone -> computeDistance(reference, createNodeFromZone(zone))))
                .map(Strategy::createNodeFromZone)
                .orElse(null);
    }
}
