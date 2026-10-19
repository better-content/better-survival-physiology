package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.NutritionEffects;
import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import java.util.ArrayList;
import java.util.List;

/** Continuous primary benefits and group-specific behavioural unlocks. */
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
        double n = value(state, aspect), nectar = state.sugar();
        return switch (aspect) {
            case IMPACT -> String.format("Melee damage +%.1f%%", 100 * NutritionEffects.meleeDamage(n, nectar));
            case WORK -> String.format("Right-tool mining +%.1f%%", 100 * NutritionEffects.miningSpeed(n, nectar));
            case MOBILITY -> String.format("Move/swim speed +%.1f%%", 100 * NutritionEffects.movementSpeed(n, nectar));
            case ENDURANCE -> String.format("Hunger/thirst/stamina costs -%.1f%%; loads extra", 100 * NutritionEffects.resourceSavings(n, nectar));
            case ROBUSTNESS -> String.format("Heat/cold drift -%.1f%%", 100 * NutritionEffects.temperatureResistance(n, nectar));
            case RENEWAL -> String.format("Harmful effects fade ×%.2f faster", 1 + NutritionEffects.harmfulExpiryBonus(n, nectar));
            case TEMPO -> String.format("Benefits & Draught penalties ×%.2f; hunger/nutrient drain ×%.2f",
                    MetabolicMath.amplification(n), MetabolicMath.sugarDrain(n));
            case CONTROL -> PresentationFlags.has(state.flags(), PresentationFlags.CONTROL_BLACKOUT)
                    ? "Blackout: no actions or movement. Clears below 85%."
                    : String.format("Damage taken -%.0f%%; move -%.0f%%; attack/use -%.0f%%; costs +%.0f%%",
                    100 * MetabolicMath.alcoholReduction(n, nectar),
                    75 * MetabolicMath.alcoholImpairment(n, nectar),
                    90 * MetabolicMath.alcoholImpairment(n, nectar),
                    200 * MetabolicMath.alcoholImpairment(n, nectar));
        };
    }

    public static String effect(AspectIdentity aspect, NutritionTier tier) {
        return switch (aspect) {
            case IMPACT -> "Melee damage scales from 0 to +40%; charged strike at 40%, cleave at 80%";
            case WORK -> "Correct-tool mining scales from 0 to +100%; work sequence at 40%, grace at 60%, burst at 80%";
            case MOBILITY -> "Movement/swimming scale from 0 to +25%; safer falls at 20%, sprint stride at 40%, leap at 80%";
            case ENDURANCE -> "All resource costs fall from 0 to -50%; emergency reserve at 80%";
            case ROBUSTNESS -> "Temperature drift falls from 0 to -70%; temperature guard at 80%";
            case RENEWAL -> "Harmful-effect expiry scales from 1× to 2×; meal recovery at 40%, cleanse at 80%";
            case TEMPO -> "Amplifies nutrients and Draught's benefits AND penalties; consumption grows exponentially";
            case CONTROL -> "Damage reduction scales from 0 to 30%; impairment above 30%, blackout at 100%";
        };
    }

    public static String preparedPromise(AspectIdentity aspect) {
        return effect(aspect, NutritionTier.SECOND);
    }

    public static List<GuideTier> guide(AspectIdentity aspect, MetabolicSyncPacket state) {
        if (aspect == AspectIdentity.TEMPO || aspect == AspectIdentity.CONTROL) return List.of(
                continuousGuide(aspect, .20f, state), continuousGuide(aspect, .40f, state),
                continuousGuide(aspect, .60f, state), continuousGuide(aspect, .80f, state));
        List<GuideTier> result = new ArrayList<>();
        result.add(new GuideTier("Now", -1, current(aspect, state), true));
        switch (aspect) {
            case IMPACT -> {
                perk(result, state, aspect, .40f, "No attacks for 4s: charged hit +50% damage. Cooldown 12s.");
                perk(result, state, aspect, .80f, "Charged hit: 35% splash to up to 2 nearby foes.");
            }
            case WORK -> {
                perk(result, state, aspect, .40f, "Right-tool breaks: +10% mining speed each, up to +50%.");
                perk(result, state, aspect, .60f, "Mining streak lasts 5s between breaks (was 3s).");
                perk(result, state, aspect, .80f, "5 valid breaks: +100% mining speed for 5s.");
            }
            case MOBILITY -> {
                perk(result, state, aspect, .20f, "Fall damage -35% × Berry level");
                perk(result, state, aspect, .40f, "Sprint 3s: +12% extra move/swim speed.");
                perk(result, state, aspect, .80f, "Sprint-jump: forward leap. Cooldown 12s.");
            }
            case ENDURANCE -> perk(result, state, aspect, .80f, "Low food/thirst/stamina: 10s reserve. Cooldown 2m. Loads still cost extra.");
            case ROBUSTNESS -> perk(result, state, aspect, .80f, "Block 1 heat/cold hit; reset temperature to safe. Cooldown 30s.");
            case RENEWAL -> {
                perk(result, state, aspect, .40f, "Eat: heal 1 heart. Cooldown 30s.");
                perk(result, state, aspect, .80f, "Clear 1 timed harmful effect. Cooldown 20s.");
            }
            default -> throw new IllegalArgumentException("Not an ordinary nutrient");
        }
        result.add(new GuideTier("Nectar", -1, "Nectar boosts strength/timing, not unlocks. Cost/temperature savings cap at 85%.", true));
        return List.copyOf(result);
    }

    /** Floor of the highest active behavioural unlock, not a fictitious shared tier. */
    public static double activePerkFloor(AspectIdentity aspect, double value) {
        if (value >= .80) return .80;
        if (aspect == AspectIdentity.WORK && value >= .60) return .60;
        if ((aspect == AspectIdentity.IMPACT || aspect == AspectIdentity.WORK
                || aspect == AspectIdentity.MOBILITY || aspect == AspectIdentity.RENEWAL) && value >= .40) return .40;
        if (aspect == AspectIdentity.MOBILITY && value >= .20) return .20;
        return 0;
    }

    private static void perk(List<GuideTier> result, MetabolicSyncPacket state, AspectIdentity aspect, float threshold, String effect) {
        result.add(new GuideTier(Math.round(threshold * 100) + "%", threshold, effect,
                value(state, aspect) >= threshold));
    }

    private static GuideTier continuousGuide(AspectIdentity aspect, float sample, MetabolicSyncPacket state) {
        String text;
        if (aspect == AspectIdentity.TEMPO) {
            text = String.format("Benefits & Draught penalties ×%.2f; hunger/nutrient drain ×%.2f",
                    MetabolicMath.amplification(sample), MetabolicMath.sugarDrain(sample));
        } else {
            double impairment = MetabolicMath.alcoholImpairment(sample, state.sugar());
            text = String.format("Damage taken -%.0f%%; move -%.0f%%; attack/use -%.0f%%; costs +%.0f%%",
                    100 * MetabolicMath.alcoholReduction(sample, state.sugar()),
                    75 * impairment, 90 * impairment, 200 * impairment);
        }
        return new GuideTier(Math.round(sample * 100) + "%", sample, text,
                value(state, aspect) >= sample);
    }
}
