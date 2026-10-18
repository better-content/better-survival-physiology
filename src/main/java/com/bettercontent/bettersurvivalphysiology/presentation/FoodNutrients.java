package com.bettercontent.bettersurvivalphysiology.presentation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Per-serving gains, not the player's current totals. Diet owns the ordinary six. */
public final class FoodNutrients {
    public record Entry(AspectIdentity aspect, double gain) {
        public String percentage() {
            double percent = gain * 100.0;
            if (percent < 0.1) return "+<0.1%";
            return "+" + String.format(Locale.ROOT, "%.1f", percent).replaceFirst("\\.0$", "") + "%";
        }
    }

    private FoodNutrients() {}

    public static List<Entry> entries(Map<String, Double> dietGains, double sugar, double alcohol, boolean milk) {
        Map<AspectIdentity, Double> gains = new EnumMap<>(AspectIdentity.class);
        dietGains.forEach((name, gain) -> {
            AspectIdentity identity = AspectIdentity.fromGroupName(name);
            if (identity != null && identity != AspectIdentity.TEMPO && identity != AspectIdentity.CONTROL)
                gains.merge(identity, gain, Double::sum);
        });
        // These loads are awarded by the consumption handler, not Diet's food calculation.
        gains.put(AspectIdentity.TEMPO, sugar);
        gains.put(AspectIdentity.CONTROL, alcohol);
        if (milk) {
            gains.put(AspectIdentity.RENEWAL, 0.06);
            gains.put(AspectIdentity.ENDURANCE, 0.06);
        }
        List<Entry> entries = new ArrayList<>();
        for (AspectIdentity identity : AspectIdentity.values()) {
            double gain = gains.getOrDefault(identity, 0.0);
            if (Double.isFinite(gain) && gain > 0) entries.add(new Entry(identity, gain));
        }
        return List.copyOf(entries);
    }
}
