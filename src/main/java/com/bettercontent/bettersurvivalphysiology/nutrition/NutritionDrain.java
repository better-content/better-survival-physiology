package com.bettercontent.bettersurvivalphysiology.nutrition;

/** The short-lived upper band is an additional, clock-based cost on Diet's stored values. */
public final class NutritionDrain {
    public static final double UPPER_BAND_PER_SECOND = 0.03 / 60.0;

    private NutritionDrain() {}

    public static int sugarMultiplier(double sugar) {
        if (sugar >= 0.60) return 5;
        if (sugar >= 0.25) return 2;
        return 1;
    }

    public static float next(float value, double prepared, double sugar) {
        if (value < prepared) return value;
        return (float) Math.max(0.0, value - UPPER_BAND_PER_SECOND * sugarMultiplier(sugar));
    }

    public static int secondsUntilBelow(double value, double threshold, double prepared, double sugar) {
        if (value < prepared || threshold < prepared) return -1;
        return Math.max(1, (int) Math.floor((value - threshold) /
                (UPPER_BAND_PER_SECOND * sugarMultiplier(sugar))) + 1);
    }

    public static double debtPerSecond(double sugar) {
        if (sugar >= 0.60) return 0.25 / 60.0;
        if (sugar >= 0.25) return 0.10 / 60.0;
        return 0.0;
    }
}
