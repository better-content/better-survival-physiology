package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.NutritionEffects;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import net.minecraft.server.level.ServerPlayer;

public final class GameplayHooks {
    private GameplayHooks() {}

    public static float hungerMultiplier(ServerPlayer player) {
        return costMultiplier(player);
    }

    public static float thirstMultiplier(ServerPlayer player) {
        return costMultiplier(player);
    }

    public static float staminaMultiplier(ServerPlayer player) {
        return costMultiplier(player);
    }

    private static float costMultiplier(ServerPlayer player) {
        MetabolicState state = MetabolicStateStore.get(player);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        double fats = nutrition.actual(NutritionSnapshot.Group.FATS);
        double savings = NutritionEffects.resourceSavings(fats, state.sugar);
        double ordinary = state.enduranceReserveTicks > 0 ? 0.0 : 1.0 - savings;
        // Nectar is a separate surcharge: Tallow and its emergency reserve cannot erase it.
        return (float) (ordinary + MetabolicMath.sugarDrain(state.sugar) - 1.0
                + 2.0 * MetabolicMath.alcoholImpairment(state.alcohol, state.sugar));
    }
}
