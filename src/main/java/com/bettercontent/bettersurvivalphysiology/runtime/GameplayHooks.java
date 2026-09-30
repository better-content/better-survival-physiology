package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import net.minecraft.server.level.ServerPlayer;

public final class GameplayHooks {
    private GameplayHooks() {}

    public static float hungerMultiplier(ServerPlayer player) {
        return costMultiplier(player, 0.20);
    }

    public static float thirstMultiplier(ServerPlayer player) {
        return costMultiplier(player, 0.40);
    }

    public static float staminaMultiplier(ServerPlayer player) {
        return costMultiplier(player, 0.60);
    }

    private static float costMultiplier(ServerPlayer player, double threshold) {
        MetabolicState state = MetabolicStateStore.get(player);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        double fats = nutrition.actual(NutritionSnapshot.Group.FATS);
        double savings = fats >= threshold ? Math.min(0.85, 0.40 * fats * MetabolicMath.amplification(state.sugar)) : 0.0;
        double ordinary = state.enduranceReserveTicks > 0 ? 0.0 : 1.0 - savings;
        // Sweetness is a separate surcharge: Richness and its emergency reserve cannot erase it.
        return (float) (ordinary + MetabolicMath.sugarDrain(state.sugar) - 1.0
                + 2.0 * MetabolicMath.alcoholImpairment(state.alcohol, state.sugar));
    }
}
