package model.simulation;

public class FloodZone {
    private final String name;
    private final double altitude;
    private final int initialPopulation;
    private int remainingPopulation;
    private boolean flooded;
    private boolean evacuated;

    public FloodZone(String name, double altitude, int population) {
        this.name = name;
        this.altitude = altitude;
        this.initialPopulation = Math.max(population, 0);
        this.remainingPopulation = this.initialPopulation;
        this.flooded = false;
        this.evacuated = false;
    }

    public String getName() {
        return name;
    }

    public double getAltitude() {
        return altitude;
    }

    public boolean isFlooded() {
        return flooded;
    }

    public void setFlooded(boolean flooded) {
        this.flooded = flooded;
    }

    public boolean isEvacuated() {
        return evacuated;
    }

    public int getRemainingPopulation() {
        return remainingPopulation;
    }

    public int evacuate() {
        if (evacuated || !flooded || remainingPopulation <= 0) {
            return 0;
        }
        int evacuatedCount = Math.max(1, remainingPopulation / 2);
        evacuatedCount = Math.min(evacuatedCount, remainingPopulation);
        remainingPopulation -= evacuatedCount;
        evacuated = remainingPopulation == 0;
        return evacuatedCount;
    }

    public void reset() {
        this.remainingPopulation = this.initialPopulation;
        this.flooded = false;
        this.evacuated = false;
    }
}
