package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import java.util.List;

/** Player-facing promises for the eight Diet rows; gameplay thresholds use the same tiers. */
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
        float value = value(state, aspect);
        if (aspect == AspectIdentity.TEMPO) {
            if (value >= .60f) return "Tempo II: cadence +100%; upper nutrition burns 5x";
            if (value >= .25f) return "Tempo I: cadence +50%; upper nutrition burns 2x";
            return state.debt() >= .25f ? "Crash: slower recovery and greater exhaustion" : "No active Tempo";
        }
        if (aspect == AspectIdentity.CONTROL) {
            if (value >= .90f) return "Severe impairment: slow attacks and stumbles";
            if (value > .65f) return "Impaired: slower attacks and poor handling";
            if (value >= .35f) return "Composed: up to 75% steadier aim";
            return "No active composure";
        }
        NutritionTier tier = NutritionTier.of(value, state.ordinary(), state.prepared(), state.feast());
        return effect(aspect, tier);
    }

    public static String effect(AspectIdentity aspect, NutritionTier tier) {
        if (tier == NutritionTier.BUILDING) return "Below 50%: eat this group";
        return switch (aspect) {
            case IMPACT -> tier == NutritionTier.FEAST ? "Melee +75%; charged blow +200%" :
                    tier == NutritionTier.PREPARED ? "Melee damage +75%" : "Stronger knockback";
            case WORK -> tier == NutritionTier.FEAST ? "Mining +100%; rhythm up to +200%" :
                    tier == NutritionTier.PREPARED ? "Mining speed +100%" : "Mining speed +10%";
            case MOBILITY -> tier == NutritionTier.FEAST ? "Move +50%; sprint surge up to +100%" :
                    tier == NutritionTier.PREPARED ? "Move and swim +50%; step +1" : "Move and swim +5%";
            case ENDURANCE -> tier == NutritionTier.FEAST ? "Costs -85%; 10s emergency reserve" :
                    tier == NutritionTier.PREPARED ? "Hunger, thirst, stamina costs -70%" : "Bodily resource costs -10%";
            case ROBUSTNESS -> tier == NutritionTier.FEAST ? "Drift -80%; guard one hit per 30s" :
                    tier == NutritionTier.PREPARED ? "Drift -80%; knockback resist +50%" : "Temperature drift -15%";
            case RENEWAL -> tier == NutritionTier.FEAST ? "Harm clears 4x; cleanse every 20s" :
                    tier == NutritionTier.PREPARED ? "Harm clears 4x; milk keeps buffs" : "Harm clears faster";
            case TEMPO, CONTROL -> throw new IllegalArgumentException("Loads have their own thresholds");
        };
    }

    public static String preparedPromise(AspectIdentity aspect) {
        return switch (aspect) {
            case TEMPO -> "Sugar: +50% cadence; upper nutrition burns 2x";
            case CONTROL -> "Alcohol: moderate load steadies aim; excess impairs";
            default -> aspect.representative + ": " + effect(aspect, NutritionTier.PREPARED);
        };
    }

    /** All visible steps for one Diet group, including the current step. */
    public static List<GuideTier> guide(AspectIdentity aspect, MetabolicSyncPacket state) {
        float value = value(state, aspect);
        if (aspect == AspectIdentity.TEMPO) return List.of(
                new GuideTier("25%", .25f, "Tempo I: cadence +50%; upper nutrition burns 2x", value >= .25f && value < .60f),
                new GuideTier("60%", .60f, "Tempo II: cadence +100%; upper nutrition burns 5x", value >= .60f),
                new GuideTier("Debt", -1, "Sugar builds debt; a crash slows recovery", value < .25f && state.debt() >= .25f));
        if (aspect == AspectIdentity.CONTROL) return List.of(
                new GuideTier("35–65%", .35f, "Composed: steadier aim", value >= .35f && value <= .65f),
                new GuideTier(">65%", .65f, "Impaired: slower attacks and poor handling", value > .65f && value < .90f),
                new GuideTier("90%", .90f, "Severe impairment: slow attacks and stumbles", value >= .90f));
        NutritionTier tier = NutritionTier.of(value, state.ordinary(), state.prepared(), state.feast());
        return List.of(
                new GuideTier(percent(state.ordinary()), state.ordinary(), effect(aspect, NutritionTier.SUPPORTED), tier == NutritionTier.SUPPORTED),
                new GuideTier(percent(state.prepared()), state.prepared(), effect(aspect, NutritionTier.PREPARED), tier == NutritionTier.PREPARED),
                new GuideTier(percent(state.feast()), state.feast(), effect(aspect, NutritionTier.FEAST), tier == NutritionTier.FEAST));
    }

    private static String percent(float value) { return Math.round(value * 100) + "%"; }
}
