package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** The one committed-serving writer. Providers verify the actual paid portion and pass an
 * already-canonical, non-consuming entitlement; resolution alone never awards or charges. */
public final class CommittedDietIntake {
    private CommittedDietIntake() {}

    public record Portion(ServingVectors.CanonicalServing nutrients, double paidSpiceUnits) {
        public Portion {
            Objects.requireNonNull(nutrients);
            if (!Double.isFinite(paidSpiceUnits) || paidSpiceUnits < 0 || paidSpiceUnits > 10000)
                throw new IllegalArgumentException("Invalid actual paid Spice dose");
        }
    }

    /** Must only be called after the provider has observed a completed debit/use. Never
     * infer paid Spice from a preview stack. Spice itself owns the separate health charge. */
    public static void commit(ServerPlayer player, ItemStack stack, Portion portion, boolean completedMeal) {
        Objects.requireNonNull(stack);
        commit(player, portion, stack.isEdible(), completedMeal);
    }

    /** The beverage provider carries its paid profile's edible flag, not an inferred
     * whole-stack FoodProperties value (placed/partial drinks may have no item). */
    public static void commit(ServerPlayer player, Portion portion, boolean edible, boolean completedMeal) {
        commit(player, portion, edible, completedMeal, null);
    }

    /** A fully depleted paid beverage also enters Diet's eaten-item history once. */
    public static void commit(ServerPlayer player, Portion portion, boolean edible, boolean completedMeal,
                              net.minecraft.resources.ResourceLocation eatenItem) {
        Objects.requireNonNull(player); Objects.requireNonNull(portion);
        var diet = DietBridge.tracker(player).orElseThrow(() ->
                new IllegalStateException("Cannot commit paid nutrition: native Diet tracker unavailable"));
        var before = DietBridge.snapshot(player);
        var available = diet.getValues().keySet();
        Map<String, Double> gains = new HashMap<>(portion.nutrients().gains());
        if (portion.paidSpiceUnits() > 0) {
            if (!SpiceBridge.available()) throw new IllegalStateException("Paid Spice provider unavailable");
            gains.merge("spice", SpiceBridge.dietGain(portion.paidSpiceUnits()), Math::max);
        }
        if (!available.containsAll(gains.keySet()))
            throw new IllegalStateException("Diet suite has no paid nutrients " + gains.keySet());
        // All validation precedes the first tracker write; a malformed provider cannot
        // partially award the five-group ledger or publish an incomplete sync.
        gains.forEach((group, gain) -> {
            if (!Double.isFinite(gain) || gain < 0) throw new IllegalArgumentException("Invalid gain " + group);
        });
        gains.forEach((group, gain) -> diet.setValue(group, (float) Math.min(1, diet.getValue(group) + gain)));
        if (eatenItem != null)
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(eatenItem).ifPresent(diet::addEaten);
        diet.sync();
        var state = MetabolicStateStore.get(player);
        float alcohol = DietBridge.snapshot(player).alcohol();
        if (com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.available()) {
            com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.updateBlackout(player, state, alcohol);
        } else if (alcohol >= 1.0f) {
            // Standalone Physiology fallback retains the existing persisted flag/anchor.
            state.blackout = true;
            state.blackoutX = player.getX(); state.blackoutY = player.getY(); state.blackoutZ = player.getZ();
        }
        SalienceEvents.culinaryEffectsCompleted(player);
        SalienceEvents.beverageFeedback(player, before, before.carbs(), before.alcohol(),
                edible, completedMeal);
        MetabolicStateStore.save(player);
    }
}
