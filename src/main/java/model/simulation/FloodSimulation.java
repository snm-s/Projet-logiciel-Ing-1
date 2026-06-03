package model.simulation;

import java.util.ArrayList;
import java.util.List;

import model.agent.Agent;
import model.agent.RescueTeam;
import model.alert.AlertSystem;
import model.graph.Graph;

public class FloodSimulation {
    private Graph graph;
    private List<Agent> agents;
    private AlertSystem alertSystem;

    public FloodSimulation() {
        this.graph = new Graph();
        this.agents = new ArrayList<>();
        this.alertSystem = new AlertSystem();
    }

    public Graph getGraph() { return graph; }
    public List<Agent> getAgents() { return agents; }
    public AlertSystem getAlertSystem() { return alertSystem; }

    public int getActiveAgentsCount() {
        return agents.size();
    }

    public int getRescuedVictimsCount() {
        return (int) agents.stream().filter(Agent::isSaved).count();
    }

    public int getActiveMissionsCount() {
        return (int) agents.stream()
            .filter(a -> a instanceof RescueTeam && !((RescueTeam) a).isIdle())
            .count();
    }
}