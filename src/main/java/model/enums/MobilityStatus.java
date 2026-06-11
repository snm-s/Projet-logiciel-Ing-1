package model.enums;

public enum MobilityStatus {
    NORMAL(1.0),
    CHILD(0.7),
    ELDERY(0.5),
    PMR(0.3);

    private final double speedMultiplier;

    MobilityStatus(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }
}
