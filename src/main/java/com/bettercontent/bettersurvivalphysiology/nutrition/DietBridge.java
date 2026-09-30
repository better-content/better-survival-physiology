package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.platform.Services;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class DietBridge {
    private DietBridge() {}

    public static Optional<? extends IDietTracker> tracker(Player player) {
        return Services.CAPABILITY.get(player);
    }

    public static NutritionSnapshot snapshot(Player player) {
        return tracker(player).map(DietBridge::snapshot).orElse(NutritionSnapshot.EMPTY);
    }

    /** Diet keeps the values and persistence; the temporary upper band has a real-time cost. */
    public static boolean drainUpperBand(Player player, double prepared, double sugar) {
        return tracker(player).map(diet -> {
            boolean changed = false;
            for (NutritionSnapshot.Group group : NutritionSnapshot.Group.values()) {
                float before = diet.getValue(group.id);
                float after = NutritionDrain.next(before, prepared, sugar);
                if (after >= before) continue;
                diet.setValue(group.id, after);
                changed = true;
            }
            if (changed) diet.sync();
            return changed;
        }).orElse(false);
    }

    /** Expose the two optional metabolic loads through Diet's native tracker and screen. */
    public static void syncMetabolic(Player player, MetabolicState state) {
        tracker(player).ifPresent(diet -> {
            boolean changed = mirror(diet, "sugars", (float) state.sugar);
            changed |= mirror(diet, "alcohol", (float) state.alcohol);
            if (changed) diet.sync();
        });
    }

    private static boolean mirror(IDietTracker diet, String group, float value) {
        if (!diet.getValues().containsKey(group) || Math.abs(diet.getValue(group) - value) < 0.005f) return false;
        diet.setValue(group, value);
        return true;
    }


    private static NutritionSnapshot snapshot(IDietTracker tracker) {
        return new NutritionSnapshot(
                tracker.getValue(NutritionSnapshot.Group.PROTEINS.id),
                tracker.getValue(NutritionSnapshot.Group.GRAINS.id),
                tracker.getValue(NutritionSnapshot.Group.FRUITS.id),
                tracker.getValue(NutritionSnapshot.Group.FATS.id),
                tracker.getValue(NutritionSnapshot.Group.VEGETABLES.id),
                tracker.getValue(NutritionSnapshot.Group.DAIRY.id)
        );
    }
}
