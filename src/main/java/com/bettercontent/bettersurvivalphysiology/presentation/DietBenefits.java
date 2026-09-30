package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import java.util.List;

/** Player-facing guide to the four ordinary-food milestones and two temporary loads. */
public final class DietBenefits {
    private DietBenefits() {}

    public record GuideTier(String threshold, float value, String effect, boolean current) {}

    public static float value(MetabolicSyncPacket state, AspectIdentity aspect) {
        return switch (aspect) {
            case IMPACT -> state.proteins();
            case TEMPO -> state.sugar();
            case WORK -> state.grains();
            case MOBILITY -> state.fruits();
            case ENDURANCE -> state.fats();
            case ROBUSTNESS -> state.vegetables();
            case RENEWAL -> state.dairy();
            case CONTROL -> state.alcohol();
        };
    }

    public static String current(AspectIdentity aspect, MetabolicSyncPacket state) {
        if (aspect == AspectIdentity.TEMPO) return String.format("Other effects ×%.2f; hunger and nutrition drain ×%.2f",
                MetabolicMath.amplification(state.sugar()), MetabolicMath.sugarDrain(state.sugar()));
        if (aspect == AspectIdentity.CONTROL) return PresentationFlags.has(state.flags(), PresentationFlags.CONTROL_BLACKOUT)
                ? "Blacked out until Draught falls to 85%"
                : String.format("Damage reduction %.0f%%; impairment starts above 30%%",
                MetabolicMath.alcoholReduction(state.alcohol(), state.sugar()) * 100.0);
        return effect(aspect, NutritionTier.of(value(state, aspect)));
    }

    public static String effect(AspectIdentity aspect, NutritionTier tier) {
        if (tier == NutritionTier.BUILDING) return "First ability at 20%";
        int stage = tier.ordinal();
        return switch (aspect) {
            case IMPACT -> new String[]{"", "Full-charge impact", "Heavy strike after 4s idle", "Heavy strike staggers", "Heavy strike cleaves two nearby foes"}[stage];
            case WORK -> new String[]{"", "Correct-tool work rhythm", "Rhythm stacks mining speed", "Rhythm lasts through a 5s pause", "Five stacks grant a 5s work burst"}[stage];
            case MOBILITY -> new String[]{"", "Softer landings", "Half-block stepping", "Stride after 3s sprint", "Sprinting jump gains a burst"}[stage];
            case ENDURANCE -> new String[]{"", "Hunger costs fall", "Thirst costs fall", "Stamina costs fall", "10s emergency reserve when depleted"}[stage];
            case ROBUSTNESS -> new String[]{"", "Resist heat and cold drift", "Resist knockback", "Extreme temperature slows less", "Guard one exposure or knockback / 30s"}[stage];
            case RENEWAL -> new String[]{"", "Meals recover one heart / 30s", "Milk preserves beneficial effects", "Harmful effects fade faster", "Cleanse one harmful effect / 20s"}[stage];
            case TEMPO, CONTROL -> throw new IllegalArgumentException("Loads use continuous effects");
        };
    }

    public static String preparedPromise(AspectIdentity aspect) {
        return switch (aspect) {
            case TEMPO -> "Sweetness amplifies other effects and burns food faster";
            case CONTROL -> "Draught reduces damage but steadily impairs control";
            default -> aspect.displayName + ": " + effect(aspect, NutritionTier.SECOND);
        };
    }

    public static List<GuideTier> guide(AspectIdentity aspect, MetabolicSyncPacket state) {
        float value = value(state, aspect);
        if (aspect == AspectIdentity.TEMPO) return List.of(
                new GuideTier("Benefit", -1, String.format("All unlocked effects ×%.2f", MetabolicMath.amplification(value)), true),
                new GuideTier("Cost", -1, String.format("Hunger and nutrition drain ×%.2f", MetabolicMath.sugarDrain(value)), true));
        if (aspect == AspectIdentity.CONTROL) return List.of(
                new GuideTier("Guard", -1, String.format("Damage reduction %.0f%% (40%% cap)", MetabolicMath.alcoholReduction(value, state.sugar()) * 100), true),
                new GuideTier("30%", .30f, "Control penalties begin above 30%", value > .30f),
                new GuideTier("50%", .50f, "Penalties become significant", value >= .50f),
                new GuideTier("90%", .90f, "Severe control loss", value >= .90f),
                new GuideTier("100%", 1.0f, "Blackout until load falls to 85%", value >= 1.0f));
        NutritionTier tier = NutritionTier.of(value);
        return List.of(
                new GuideTier("20%", .20f, effect(aspect, NutritionTier.FIRST), tier == NutritionTier.FIRST),
                new GuideTier("40%", .40f, effect(aspect, NutritionTier.SECOND), tier == NutritionTier.SECOND),
                new GuideTier("60%", .60f, effect(aspect, NutritionTier.THIRD), tier == NutritionTier.THIRD),
                new GuideTier("80%", .80f, effect(aspect, NutritionTier.FOURTH), tier == NutritionTier.FOURTH));
    }
}
