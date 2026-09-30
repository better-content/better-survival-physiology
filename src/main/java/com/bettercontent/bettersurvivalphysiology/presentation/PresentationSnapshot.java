package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import net.minecraft.server.level.ServerPlayer;

public record PresentationSnapshot(int flags, int workSequence, int[] nutrientSeconds,
                                   int sugarSeconds, int alcoholSeconds,
                                   float ordinary, float prepared, float feast) {
    public static PresentationSnapshot create(ServerPlayer player, NutritionSnapshot nutrition, MetabolicState state) {
        double ordinary = SalienceConfig.FIRST;
        double prepared = SalienceConfig.SECOND;
        double feast = SalienceConfig.FOURTH;
        int flags = 0;
        if (nutrition.proteins() >= prepared && state.heavyBlowCooldown == 0
                && player.level().getGameTime() - state.lastAttackTick >= 80L
                && player.getAttackStrengthScale(0.5f) >= 0.90f) flags |= PresentationFlags.IMPACT_READY;
        if (nutrition.fruits() >= SalienceConfig.THIRD && state.sprintTicks >= 60) flags |= PresentationFlags.MOBILITY_STRIDE;
        if (nutrition.fats() >= feast && state.enduranceReserveCooldown == 0) flags |= PresentationFlags.ENDURANCE_READY;
        if (state.enduranceReserveTicks > 0) flags |= PresentationFlags.ENDURANCE_ACTIVE;
        if (nutrition.vegetables() >= feast && state.weatheredCooldown == 0) flags |= PresentationFlags.ROBUSTNESS_READY;
        if (nutrition.dairy() >= feast && state.dairyCleanseCooldown == 0) flags |= PresentationFlags.RENEWAL_READY;
        if (state.sugar > 0.01) flags |= PresentationFlags.TEMPO_ONE;
        if (state.blackout) flags |= PresentationFlags.CONTROL_BLACKOUT;
        if (state.alcohol > 0.30) flags |= PresentationFlags.CONTROL_IMPAIRED;

        double[] values = {nutrition.proteins(), nutrition.grains(), nutrition.fruits(), nutrition.fats(), nutrition.vegetables(), nutrition.dairy()};
        int[] estimates = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            NutritionTier tier = NutritionTier.of(values[index]);
            estimates[index] = NutritionEstimates.nutrientSeconds(values[index], tier.floor(), state.sugar);
        }
        return new PresentationSnapshot(flags, state.workSequence, estimates,
                NutritionEstimates.sugarSeconds(state.sugar),
                NutritionEstimates.alcoholSeconds(state.alcohol), (float) ordinary, (float) prepared, (float) feast);
    }
}
