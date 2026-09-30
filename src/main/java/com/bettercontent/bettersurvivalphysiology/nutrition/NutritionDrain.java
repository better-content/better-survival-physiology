package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;

/** The short-lived upper band is an additional, clock-based cost on Diet's stored values. */
public final class NutritionDrain {
    public static final double UPPER_BAND_PER_SECOND = 0.03 / 60.0;

    private NutritionDrain() {}

    public static float next(float value, double prepared, double sugar) {
        if (value < prepared) return value;
        return (float) Math.max(0.0, value - UPPER_BAND_PER_SECOND * MetabolicMath.sugarDrain(sugar));
    }

    public static int secondsUntilBelow(double value, double threshold, double prepared, double sugar) {
        if (value < prepared || threshold < prepared) return -1;
        return Math.max(1, (int) Math.floor((value - threshold) /
                (UPPER_BAND_PER_SECOND * MetabolicMath.sugarDrain(sugar))) + 1);
    }
}
