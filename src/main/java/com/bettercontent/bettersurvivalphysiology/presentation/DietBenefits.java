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
        if (aspect == AspectIdentity.TEMPO) return String.format("All active nutrition effects ×%.2f; hunger + nutrition drain ×%.2f",
                MetabolicMath.amplification(state.sugar()), MetabolicMath.sugarDrain(state.sugar()));
        if (aspect == AspectIdentity.CONTROL) return PresentationFlags.has(state.flags(), PresentationFlags.CONTROL_BLACKOUT)
                ? "Blackout: no movement or actions until Draught falls below 85%"
                : String.format("Damage −%.0f%%; move −%.0f%%; attack/use −%.0f%%; resource costs +%.0f%%",
                MetabolicMath.alcoholReduction(state.alcohol(), state.sugar()) * 100.0,
                75 * MetabolicMath.alcoholImpairment(state.alcohol(), state.sugar()),
                90 * MetabolicMath.alcoholImpairment(state.alcohol(), state.sugar()),
                200 * MetabolicMath.alcoholImpairment(state.alcohol(), state.sugar()));
        return effect(aspect, NutritionTier.of(value(state, aspect)));
    }

    public static String effect(AspectIdentity aspect, NutritionTier tier) {
        if (tier == NutritionTier.BUILDING) return "First ability at 20%";
        int stage = tier.ordinal();
        return switch (aspect) {
            case IMPACT -> new String[]{"", "Melee damage +25% × Sinew level; hit knockback +0.15", "After 4s without attacking, a charged hit deals +50% damage; 12s cooldown", "Charged hits add a second stagger impact", "Charged hits splash 35% damage to two foes within 2.5 blocks"}[stage];
            case WORK -> new String[]{"", "Correct tool: mining speed +60% × Grain level", "Consecutive valid breaks add +10% mining speed each, up to five", "The break sequence survives a 5s pause", "Five valid breaks grant a 5s mining speed burst"}[stage];
            case MOBILITY -> new String[]{"", "Fall damage falls by 35% × Berry level; movement +20% × level", "Step height rises by half a block × Nectar amplification", "After 3s sprinting, gain another 12% movement speed", "A sprinting jump launches forward; 12s cooldown"}[stage];
            case ENDURANCE -> new String[]{"", "Hunger cost −40% × Tallow level, capped at 85%", "Thirst cost −40% × Tallow level, capped at 85%", "Stamina cost −40% × Tallow level, capped at 85%", "At low food, thirst, or stamina: 10s reserve; 2m cooldown"}[stage];
            case ROBUSTNESS -> new String[]{"", "Heat/cold drift falls by up to 50% × Root level", "Knockback resistance +50% × Root level", "Extreme temperature adds movement speed +50% × level", "Block one temperature hit or knockback; 30s cooldown"}[stage];
            case RENEWAL -> new String[]{"", "Eating heals one heart; 30s cooldown; healing gains +25% × Cream level", "Milk keeps beneficial potion effects", "Harmful potion effects expire two ticks faster per tick", "Remove one harmful effect; 20s cooldown"}[stage];
            case TEMPO, CONTROL -> throw new IllegalArgumentException("Loads use continuous effects");
        };
    }

    public static String preparedPromise(AspectIdentity aspect) {
        return switch (aspect) {
            case TEMPO -> "Nectar amplifies active nutrition effects and drains food exponentially";
            case CONTROL -> "Draught reduces damage but steadily impairs control";
            default -> aspect.displayName + ": " + effect(aspect, NutritionTier.SECOND);
        };
    }

    public static List<GuideTier> guide(AspectIdentity aspect, MetabolicSyncPacket state) {
        float value = value(state, aspect);
        if (aspect == AspectIdentity.TEMPO || aspect == AspectIdentity.CONTROL) return List.of(
                continuousGuide(aspect, .20f, state), continuousGuide(aspect, .40f, state),
                continuousGuide(aspect, .60f, state), continuousGuide(aspect, .80f, state));
        NutritionTier tier = NutritionTier.of(value);
        return List.of(
                new GuideTier("20%", .20f, effect(aspect, NutritionTier.FIRST), tier == NutritionTier.FIRST),
                new GuideTier("40%", .40f, effect(aspect, NutritionTier.SECOND), tier == NutritionTier.SECOND),
                new GuideTier("60%", .60f, effect(aspect, NutritionTier.THIRD), tier == NutritionTier.THIRD),
                new GuideTier("80%", .80f, effect(aspect, NutritionTier.FOURTH), tier == NutritionTier.FOURTH));
    }

    private static GuideTier continuousGuide(AspectIdentity aspect, float sample, MetabolicSyncPacket state) {
        String effect;
        if (aspect == AspectIdentity.TEMPO) {
            effect = String.format("Effects ×%.2f; Hunger + nutrition drain ×%.2f",
                    MetabolicMath.amplification(sample), MetabolicMath.sugarDrain(sample));
        } else {
            double impairment = MetabolicMath.alcoholImpairment(sample, state.sugar());
            effect = String.format("Damage −%.0f%%; move −%.0f%%; attack/use −%.0f%%; costs +%.0f%%",
                    100 * MetabolicMath.alcoholReduction(sample, state.sugar()),
                    75 * impairment, 90 * impairment, 200 * impairment);
        }
        return new GuideTier(Math.round(sample * 100) + "%", sample, effect,
                value(state, aspect) >= sample);
    }
}
