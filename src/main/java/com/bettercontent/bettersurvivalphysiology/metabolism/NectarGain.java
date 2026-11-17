package com.bettercontent.bettersurvivalphysiology.metabolism;

import java.util.Map;
import java.util.Set;

/** Potential whole-serving gains, independent of current tracker levels and saturation. */
public final class NectarGain {
    // Input is the native potential serving, never legacy source tags. Alcohol is
    // optional and must not cap a lasting food gain or become a second award.
    private static final Set<String> LASTING = Set.of("proteins", "carbs", "vegetables", "spice");

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
