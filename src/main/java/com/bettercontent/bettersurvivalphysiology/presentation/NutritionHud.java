package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;

import java.util.Arrays;
import java.util.List;

/** Current totals, independent of meal deltas and ability-readiness thresholds. */
public final class NutritionHud {
    private NutritionHud() {}

    public static List<Entry> entries(MetabolicSyncPacket state) {
        return Arrays.stream(AspectIdentity.values())
                .map(aspect -> new Entry(aspect, DietBenefits.value(state, aspect)))
                .filter(entry -> Float.isFinite(entry.value()) && entry.value() > 0)
                .toList();
    }

    public record Entry(AspectIdentity aspect, float value) {
        public String label() {
            // Do not label a positive trace as zero or clamp metabolic loads to 100%.
            return value < .005f ? "<1%" : Math.round(value * 100) + "%";
        }
    }
}
