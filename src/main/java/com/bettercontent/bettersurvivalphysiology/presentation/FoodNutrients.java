package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.nutrition.ServingVectors;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One canonical serving vector; Sugar/Alcohol are never hidden second balances. */
public final class FoodNutrients {
    public record Entry(AspectIdentity aspect, double gain) {
        public String percentage() {
            double percent = gain * 100.0;
            if (percent < 0.1) return "+<0.1%";
            return "+" + String.format(Locale.ROOT, "%.1f", percent).replaceFirst("\\.0$", "") + "%";
        }
    }

    private FoodNutrients() {}

    /** The supplemental scalars describe only paid contributions absent from native results.
     * Existing canonical Carbs/Alcohol gains always win, preventing preview double counting. */
    public static List<Entry> entries(Map<String, Double> canonical, double optionalCarbs,
                                      double optionalAlcohol, boolean milk) {
        Map<AspectIdentity, Double> gains = new EnumMap<>(AspectIdentity.class);
        canonical.forEach((name, gain) -> {
            AspectIdentity identity = AspectIdentity.fromGroupName(name);
            if (identity != null) gains.merge(identity, gain, Double::sum);
        });
        if (!canonical.containsKey("carbs") && optionalCarbs > 0) gains.put(AspectIdentity.MOTION, optionalCarbs);
        if (!canonical.containsKey("alcohol") && optionalAlcohol > 0) gains.put(AspectIdentity.CONTROL, optionalAlcohol);
        if (milk) {
            ServingVectors.milk().gains().forEach((group, dose) ->
                    gains.merge(AspectIdentity.fromGroupName(group), dose, Double::sum));
        }
        List<Entry> entries = new ArrayList<>();
        for (AspectIdentity identity : AspectIdentity.values()) {
            double gain = gains.getOrDefault(identity, 0.0);
            if (Double.isFinite(gain) && gain > 0) entries.add(new Entry(identity, gain));
        }
        return List.copyOf(entries);
    }
}
