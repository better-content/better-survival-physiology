package com.bettercontent.bettersurvivalphysiology.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server balance surface for the eight categorical nutrition identities. */
public final class SalienceConfig {
    public static final List<String> DEFAULT_ALCOHOL_POTENCIES = List.of(
            "brewinandchewin:kombucha=0.08", "brewinandchewin:beer=0.14", "brewinandchewin:pale_jane=0.16",
            "brewinandchewin:strongroot_ale=0.18", "brewinandchewin:steel_toe_stout=0.20",
            "brewinandchewin:mead=0.22", "brewinandchewin:rice_wine=0.22", "brewinandchewin:egg_grog=0.24",
            "brewinandchewin:bloody_mary=0.24", "brewinandchewin:salty_folly=0.25",
            "brewinandchewin:glittering_grenadine=0.26", "brewinandchewin:saccharine_rum=0.28",
            "brewinandchewin:red_rum=0.30", "brewinandchewin:dread_nog=0.32",
            "brewinandchewin:vodka=0.38", "brewinandchewin:withering_dross=0.42"
    );

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SUGAR_HALF_LIFE_MINUTES;
    public static final ForgeConfigSpec.DoubleValue ALCOHOL_CLEAR_MINUTES;
    public static final double FIRST = 0.20;
    public static final double SECOND = 0.40;
    public static final double THIRD = 0.60;
    public static final double FOURTH = 0.80;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALCOHOL_POTENCIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Sweetness and Draught load tuning.").push("metabolism");
        SUGAR_HALF_LIFE_MINUTES = range(builder, "sugarHalfLifeMinutes", 4.0, 0.1, 120.0);
        ALCOHOL_CLEAR_MINUTES = range(builder, "alcoholClearMinutes", 20.0, 0.1, 240.0);
        ALCOHOL_POTENCIES = builder.comment("item_id=load entries; replace this list to tune drinks or add compat drinks.")
                .defineList("alcoholPotencies", DEFAULT_ALCOHOL_POTENCIES, SalienceConfig::validAlcoholEntry);
        builder.pop();

        SPEC = builder.build();
    }

    private SalienceConfig() {}

    public static double value(ForgeConfigSpec.DoubleValue configured, double fallback) {
        try { return configured.get(); }
        catch (IllegalStateException ignored) { return fallback; }
    }

    public static Map<String, Double> alcoholPotencies() {
        List<? extends String> entries;
        try { entries = ALCOHOL_POTENCIES.get(); }
        catch (IllegalStateException ignored) { entries = DEFAULT_ALCOHOL_POTENCIES; }
        Map<String, Double> parsed = new LinkedHashMap<>();
        entries.forEach(entry -> {
            int split = entry.lastIndexOf('=');
            if (split <= 0) return;
            try { parsed.put(entry.substring(0, split), Double.parseDouble(entry.substring(split + 1))); }
            catch (NumberFormatException ignored) { }
        });
        return Map.copyOf(parsed);
    }

    private static ForgeConfigSpec.DoubleValue range(ForgeConfigSpec.Builder builder, String key, double value, double min, double max) {
        return builder.defineInRange(key, value, min, max);
    }

    private static boolean validAlcoholEntry(Object value) {
        if (!(value instanceof String entry)) return false;
        int split = entry.lastIndexOf('=');
        if (split <= 0 || split == entry.length() - 1) return false;
        try {
            double load = Double.parseDouble(entry.substring(split + 1));
            return entry.substring(0, split).contains(":") && load > 0.0 && load <= 1.0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
