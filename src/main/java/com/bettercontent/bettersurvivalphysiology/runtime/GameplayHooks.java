package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import net.minecraft.server.level.ServerPlayer;

public final class GameplayHooks {
    private GameplayHooks() {}

    public static float exhaustionMultiplier(ServerPlayer player) {
        MetabolicState state = MetabolicStateStore.get(player);
        if (state.enduranceReserveTicks > 0) return 0.0f;
        float multiplier = 1.0f;
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        double fats = nutrition.effective(NutritionSnapshot.Group.FATS, state);
        if (fats >= SalienceConfig.FEAST_THRESHOLD.get()) multiplier *= 0.15f;
        else if (fats >= SalienceConfig.PREPARED_THRESHOLD.get()) multiplier *= 0.30f;
        else if (fats >= SalienceConfig.ORDINARY_THRESHOLD.get()) multiplier *= 0.90f;
        if (state.sugar < SalienceConfig.SUGAR_DEBT_GATE.get()) multiplier *= (float) (1.0 + 0.5 * state.debt);
        return multiplier;
    }

    public static int cooldown(int baseTicks, MetabolicState state) {
        return MetabolicMath.adjustedCooldown(baseTicks, state.sugar, state.debt);
    }
}
