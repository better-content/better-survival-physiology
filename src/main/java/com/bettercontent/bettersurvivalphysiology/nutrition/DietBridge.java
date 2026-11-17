package com.bettercontent.bettersurvivalphysiology.nutrition;


import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.platform.Services;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/** Native Diet tracker owns all five balances and their persistence/sync. */
public final class DietBridge {
    private DietBridge() {}

    public static Optional<? extends IDietTracker> tracker(Player player) {
        return Services.CAPABILITY.get(player);
    }

    public static NutritionSnapshot snapshot(Player player) {
        return tracker(player).map(DietBridge::snapshot).orElse(NutritionSnapshot.EMPTY);
    }

    public static void awardMilk(Player player) {
        tracker(player).ifPresent(diet -> {
            if (!diet.getValues().keySet().containsAll(NativeDietRoster.GROUPS))
                throw new IllegalStateException("Native five-group Diet suite unavailable for milk");
            ServingVectors.milk().gains().forEach((group, dose) ->
                    diet.setValue(group, (float) Math.min(1.0, diet.getValue(group) + dose)));
            diet.sync();
        });
    }

    /** The three ordinary nutrients pay upper-band decay; Carbs/Alcohol have their own clocks. */
    public static boolean drainUpperBand(Player player, double prepared, double carbs) {
        return tracker(player).map(diet -> {
            boolean changed = false;
            for (NutritionSnapshot.Group group : NutritionSnapshot.Group.values()) {
                if (group == NutritionSnapshot.Group.CARBS || group == NutritionSnapshot.Group.ALCOHOL) continue;
                float before = diet.getValue(group.id);
                float after = NutritionDrain.next(before, prepared, carbs);
                if (after >= before) continue;
                diet.setValue(group.id, after);
                changed = true;
            }
            if (changed) diet.sync();
            return changed;
        }).orElse(false);
    }

    /** Timed decay operates on native balances directly, never on mirrored metabolic state.
     * Diet's native hunger decay is excluded for these two groups by DietDecayMixin. */
    public static void tickTimed(Player player) {
        tracker(player).ifPresent(diet -> {
            if (!diet.isActive()) return;
            float carbs = diet.getValue("carbs"), alcohol = diet.getValue("alcohol");
            float nextCarbs = (float) MetabolicMath.tickSugar(carbs);
            float nextAlcohol = (float) MetabolicMath.tickAlcohol(alcohol, carbs);
            if (nextCarbs != carbs) diet.setValue("carbs", nextCarbs);
            if (nextAlcohol != alcohol) diet.setValue("alcohol", nextAlcohol);
            if (player.tickCount % 20 == 0 && (nextCarbs != carbs || nextAlcohol != alcohol)) diet.sync();
        });
    }

    /** Apply paid optional Alcohol potency once; native food results cannot directly award it. */
    public static void awardAlcohol(Player player, double dose) {
        if (!Double.isFinite(dose) || dose < 0) throw new IllegalArgumentException("Invalid Alcohol dose");
        if (dose == 0) return;
        tracker(player).ifPresent(diet -> {
            if (!diet.getValues().containsKey("alcohol")) throw new IllegalStateException("Native Alcohol group missing");
            diet.setValue("alcohol", (float) Math.min(1.0, diet.getValue("alcohol") + dose));
            diet.sync();
        });
    }

    private static NutritionSnapshot snapshot(IDietTracker tracker) {
        return new NutritionSnapshot(
                tracker.getValue("proteins"), tracker.getValue("carbs"), tracker.getValue("spice"),
                tracker.getValue("vegetables"), tracker.getValue("alcohol"));
    }
}
