package model.graph;

import java.awt.Color;

public enum EdgeState {

    FLOODED   (new Color(0x1A237E), true,  0),

    FLOODING  (new Color(0xF97316), false, 0),

    OVERLOADED(new Color(0xB71C1C), false, 0),

    CONGESTED (new Color(0xE53935), false, 0),

    AT_RISK   (new Color(0xFB8C00), false, 0),

    SAFE      (new Color(0x2E7D32), false, 0);
    public final Color color;
    public final boolean dashed;
    public final int speedKmh;

    EdgeState(Color color, boolean dashed, int speedKmh) {
        this.color = color;
        this.dashed = dashed;
        this.speedKmh = speedKmh;
    }

    public static EdgeState compute(boolean fromFlooded, boolean toFlooded, double flowRatio) {
        if (fromFlooded || toFlooded) return FLOODED;
        if (flowRatio > 1.0) return OVERLOADED;
        if (flowRatio > 0.8) return CONGESTED;
        if (flowRatio > 0.5) return AT_RISK;
        return SAFE;
    }

    public static EdgeState compute(boolean fromFlooded, boolean toFlooded, double altFrom, double altTo) {
        return compute(fromFlooded, toFlooded, 0.0);
    }
}
