package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.NutritionEffects;
import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import java.util.ArrayList;
import java.util.List;

/** One native Diet meter per food identity; shared aspect names are explanatory only. */
public final class DietBenefits {
    private DietBenefits() {}

    public record GuideTier(String threshold, float value, String effect, boolean current) {}

    public static float value(MetabolicSyncPacket state, AspectIdentity aspect) {
        return switch (aspect) {
            case POWER -> state.proteins();
            case MOTION -> state.carbs();
            case ENDURANCE -> state.spice();
            case RENEWAL -> state.vegetables();
            case CONTROL -> state.alcohol();
        };
    }

    public static String current(AspectIdentity aspect, MetabolicSyncPacket state) {
        double n = value(state, aspect), carbs = state.carbs();
        return switch (aspect) {
            case POWER -> String.format("Melee +%.1f%%; correct-tool mining +%.1f%%",
                    100 * NutritionEffects.meleeDamage(n, carbs), 100 * NutritionEffects.miningSpeed(n, carbs));
            case MOTION -> String.format("Move/swim +%.1f%%; benefits & Alcohol penalties ×%.2f; drain ×%.2f",
                    100 * NutritionEffects.movementSpeed(n, n), MetabolicMath.amplification(n), MetabolicMath.sugarDrain(n));
            case ENDURANCE -> String.format("Resource costs -%.1f%%; correct-tool mining +%.1f%%; move/swim +%.1f%%",
                    100 * NutritionEffects.resourceSavings(n, carbs),
                    100 * SpiceBridge.miningBonus(n, MetabolicMath.amplification(carbs)),
                    100 * SpiceBridge.movementBonus(n, MetabolicMath.amplification(carbs)));
            case RENEWAL -> String.format("Harmful effects fade ×%.2f faster", 1 + NutritionEffects.harmfulExpiryBonus(n, carbs));
            case CONTROL -> PresentationFlags.has(state.flags(), PresentationFlags.CONTROL_BLACKOUT)
                    ? "Blackout: no actions or movement. Clears below 85%."
                    : String.format("Damage taken -%.0f%%; move -%.0f%%; attack/use -%.0f%%; costs +%.0f%%",
                    100 * MetabolicMath.alcoholReduction(n, carbs),
                    75 * MetabolicMath.alcoholImpairment(n, carbs),
                    90 * MetabolicMath.alcoholImpairment(n, carbs),
                    200 * MetabolicMath.alcoholImpairment(n, carbs));
        };
    }

    public static String effect(AspectIdentity aspect, NutritionTier tier) {
        return switch (aspect) {
            case POWER -> "Melee damage up to +40%; correct-tool mining up to +100%; charged strike at 40%, cleave at 80%";
            case MOTION -> "Move/swim up to +25%; safer falls at 20%, leap at 80%; amplifies nutrients and Alcohol, increases expenditure";
            case ENDURANCE -> String.format("Resource savings up to 50%% (85%% amplified cap), Deep Reserve at 80%%; "
                    + "Spice costs HP when eaten; mining up to +%.0f%%, move/swim up to +%.0f%%",
                    100 * SpiceBridge.miningCap(), 100 * SpiceBridge.movementCap());
            case RENEWAL -> "Harmful-effect expiry up to 2×; meal recovery at 40%, cleanse at 80%";
            case CONTROL -> "Optional: damage reduction up to 30%; impairment above 30%, blackout at 100%; Carbs amplifies and hastens clearance";
        };
    }

    public static String preparedPromise(AspectIdentity aspect) { return effect(aspect, NutritionTier.SECOND); }

    public static List<GuideTier> guide(AspectIdentity aspect, MetabolicSyncPacket state) {
        if (aspect == AspectIdentity.CONTROL) return List.of(
                continuousAlcohol(.20f, state), continuousAlcohol(.40f, state),
                continuousAlcohol(.60f, state), continuousAlcohol(.80f, state));
        List<GuideTier> result = new ArrayList<>();
        result.add(new GuideTier("Now", -1, current(aspect, state), true));
        switch (aspect) {
            case POWER -> {
                perk(result, state, aspect, .40f, "No attacks for 4s: charged hit +50% damage. Cooldown 12s.");
                perk(result, state, aspect, .80f, "Charged hit: 35% splash to up to 2 nearby foes.");
            }
            case MOTION -> {
                perk(result, state, aspect, .20f, "Fall damage -35% × Carbs level");
                perk(result, state, aspect, .80f, "Sprint-jump: forward leap. Cooldown 12s.");
            }
            case ENDURANCE -> {
                perk(result, state, aspect, .80f, "Low food/thirst/stamina: 10s reserve. Cooldown 2m. Loads still cost extra.");
                result.add(new GuideTier("Spice intake", -1, String.format("Paid Spice: +%.0f%% level/unit; %.1f HP/unit, max %.1f HP. No standing meter cost.",
                        100 * SpiceBridge.dietGain(1), SpiceBridge.hpPerUnit(), SpiceBridge.hpCap()), true));
            }
            case RENEWAL -> {
                perk(result, state, aspect, .40f, "Eat: heal 1 heart. Cooldown 30s.");
                perk(result, state, aspect, .80f, "Clear 1 timed harmful effect. Cooldown 20s.");
            }
            default -> throw new IllegalArgumentException("Unknown native nutrient");
        }
        result.add(new GuideTier("Carbs", -1,
                "Carbs amplifies benefits and Alcohol penalties, not unlock thresholds. Spice bonuses remain capped.", true));
        return List.copyOf(result);
    }

    /** Highest active behavioural unlock; continuous effects have no invented threshold. */
    public static double activePerkFloor(AspectIdentity aspect, double value) {
        if (aspect == AspectIdentity.CONTROL) return 0;
        if (value >= .80) return .80;
        if ((aspect == AspectIdentity.POWER || aspect == AspectIdentity.RENEWAL) && value >= .40) return .40;
        if (aspect == AspectIdentity.MOTION && value >= .20) return .20;
        return 0;
    }

    private static void perk(List<GuideTier> result, MetabolicSyncPacket state, AspectIdentity aspect, float threshold, String effect) {
        result.add(new GuideTier(Math.round(threshold * 100) + "%", threshold, effect,
                value(state, aspect) >= threshold));
    }

    private static GuideTier continuousAlcohol(float sample, MetabolicSyncPacket state) {
        double impairment = MetabolicMath.alcoholImpairment(sample, state.carbs());
        return new GuideTier(Math.round(sample * 100) + "%", sample,
                String.format("Damage taken -%.0f%%; move -%.0f%%; attack/use -%.0f%%; costs +%.0f%%",
                        100 * MetabolicMath.alcoholReduction(sample, state.carbs()),
                        75 * impairment, 90 * impairment, 200 * impairment), state.alcohol() >= sample);
    }
}
