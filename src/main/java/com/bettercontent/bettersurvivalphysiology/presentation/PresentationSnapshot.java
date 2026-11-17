package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.bettercontent.bettersurvivalphysiology.runtime.HeavyBlow;
import net.minecraft.server.level.ServerPlayer;

// workSequence is a reserved wire slot, always zero; no work-sequence gameplay state remains.
public record PresentationSnapshot(int flags, int workSequence, int[] nutrientSeconds,
                                   float ordinary, float prepared, float feast) {
    public static PresentationSnapshot create(ServerPlayer player, NutritionSnapshot nutrition, MetabolicState state) {
        double ordinary = SalienceConfig.FIRST;
        double prepared = SalienceConfig.SECOND;
        double feast = SalienceConfig.FOURTH;
        int flags = 0;
        if (HeavyBlow.ready(nutrition.proteins(), prepared, state.heavyBlowCooldown,
                player.level().getGameTime(), state.lastAttackTick,
                player.getAttackStrengthScale(0.5f), state.blackout)) flags |= PresentationFlags.IMPACT_READY;
        if (nutrition.spice() >= feast && state.enduranceReserveCooldown == 0) flags |= PresentationFlags.ENDURANCE_READY;
        if (state.enduranceReserveTicks > 0) flags |= PresentationFlags.ENDURANCE_ACTIVE;
        if (nutrition.vegetables() >= feast && state.dairyCleanseCooldown == 0) flags |= PresentationFlags.RENEWAL_READY;
        if (nutrition.carbs() > 0.01) flags |= PresentationFlags.TEMPO_ONE;
        if (state.blackout) flags |= PresentationFlags.CONTROL_BLACKOUT;
        if (nutrition.alcohol() > 0.30) flags |= PresentationFlags.CONTROL_IMPAIRED;

        double[] values = {nutrition.proteins(), nutrition.carbs(), nutrition.spice(),
                nutrition.vegetables(), nutrition.alcohol()};
        int[] estimates = new int[values.length];
        AspectIdentity[] aspects = AspectIdentity.values();
        for (int index = 0; index < values.length; index++) {
            if (aspects[index] == AspectIdentity.MOTION) {
                estimates[index] = NutritionEstimates.sugarSeconds(values[index]);
            } else if (aspects[index] == AspectIdentity.CONTROL) {
                estimates[index] = NutritionEstimates.alcoholSeconds(values[index], nutrition.carbs());
            } else {
                double floor = DietBenefits.activePerkFloor(aspects[index], values[index]);
                estimates[index] = floor > 0 ? NutritionEstimates.nutrientSeconds(values[index], floor, nutrition.carbs()) : -1;
            }
        }
        return new PresentationSnapshot(flags, 0, estimates, (float) ordinary, (float) prepared, (float) feast);
    }
}
