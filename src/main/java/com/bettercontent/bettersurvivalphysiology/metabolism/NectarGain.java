package com.bettercontent.bettersurvivalphysiology.metabolism;

import java.util.Map;
import java.util.Set;

/** Potential whole-serving gains, independent of current tracker levels and saturation. */
public final class NectarGain {
    private static final Set<String> LASTING = Set.of("proteins", "grains", "fruits", "fats", "vegetables", "dairy");

    private NectarGain() {}

    public static double capped(double base, Map<String, Double> gains) {
        if (!Double.isFinite(base) || base <= 0) return 0;
        double smallest = Double.POSITIVE_INFINITY;
        for (var entry : gains.entrySet()) {
            Double gain = entry.getValue();
            if (LASTING.contains(entry.getKey()) && gain != null && Double.isFinite(gain) && gain > 0)
                smallest = Math.min(smallest, gain);
        }
        return Math.min(base, smallest * .5);
    }
}
