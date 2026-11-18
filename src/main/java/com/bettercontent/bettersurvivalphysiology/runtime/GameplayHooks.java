package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.api.SpiceEffects;
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

    /** Preserve the historic standalone cost if a saved Spice balance outlives the provider. */
    static double resourceSavings(double spice, double carbs, boolean spicePresent, SpiceEffects.Terms terms) {
        return spicePresent ? terms.resourceSavings() : NutritionEffects.resourceSavings(spice, carbs);
    }

    private static float costMultiplier(ServerPlayer player) {
        MetabolicState state = MetabolicStateStore.get(player);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        double carbs = nutrition.carbs();
        double savings = resourceSavings(nutrition.spice(), carbs, SpiceEffects.available(),
                SpiceEffects.terms(nutrition.spice(), MetabolicMath.amplification(carbs)));
        double ordinary = state.enduranceReserveTicks > 0 ? 0.0 : 1.0 - savings;
        // Carbs expenditure and Alcohol impairment remain costs; Deep Reserve cannot erase them.
        return (float) (ordinary + MetabolicMath.sugarDrain(carbs) - 1.0
                + 2.0 * MetabolicMath.alcoholImpairment(nutrition.alcohol(), carbs));
    }
}
