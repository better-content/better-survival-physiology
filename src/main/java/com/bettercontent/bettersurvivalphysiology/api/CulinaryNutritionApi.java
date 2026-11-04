package com.bettercontent.bettersurvivalphysiology.api;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Optional culinary entitlement bridge. Providers own provenance/yield/quality resolution;
 * physiology owns intake. Neither registration nor resolution consumes anything. */
public final class CulinaryNutritionApi {
    public record Serving(Map<String, Double> unscaled, Map<String, Double> adjusted,
                          double sugar, double alcohol) {
        private static final Set<String> ORDINARY = Set.of("proteins", "grains", "fruits", "vegetables", "dairy");
        public Serving {
            unscaled = checked(unscaled);
            adjusted = checked(adjusted);
            check(sugar); check(alcohol);
        }
        private static Map<String, Double> checked(Map<String, Double> values) {
            values.forEach((group, value) -> {
                if (!ORDINARY.contains(group)) throw new IllegalArgumentException("Not an ordinary nutrient: " + group);
                check(value);
            });
            return Map.copyOf(values);
        }
        private static void check(double value) {
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid serving dose");
        }
    }
    @FunctionalInterface public interface Provider {
        /** Return empty for unmanaged stacks. Must be deterministic and side-effect free; player may be null. */
        Optional<Serving> resolve(Player player, ItemStack stack);
    }
    private static final ServingProviders<Player, ItemStack, Serving> PROVIDERS = new ServingProviders<>();
    private CulinaryNutritionApi() {}
    public static synchronized void register(ResourceLocation owner, Provider provider) {
        Objects.requireNonNull(provider);
        PROVIDERS.register(Objects.requireNonNull(owner).toString(), provider::resolve);
    }
    public static synchronized Optional<Serving> resolve(Player player, ItemStack stack) {
        return PROVIDERS.resolve(player, stack);
    }
    /** Explicit adapters call ONCE after a real committed portion, never preview/start/cancel.
     * There is NO automatic Finish award for managed foods. The registering consumer owns
     * completion dispatch and deduplication for ordinary Finish and explicit custom/placed servings. */
    public static boolean complete(ServerPlayer player, ItemStack stack) {
        var serving = resolve(player, stack);
        if (serving.isEmpty()) return false;
        apply(player, stack, serving.get());
        return true;
    }
    private static void apply(ServerPlayer player, ItemStack stack, Serving serving) {
        var before = DietBridge.snapshot(player);
        var state = MetabolicStateStore.get(player);
        double sugarBefore = state.sugar, alcoholBefore = state.alcohol;
        var diet = DietBridge.tracker(player).orElseThrow(() ->
                new IllegalStateException("Cannot commit paid culinary nutrition: Diet tracker unavailable"));
        var available = diet.getValues().keySet();
        if (!available.containsAll(serving.adjusted().keySet()))
            throw new IllegalStateException("Diet suite has no culinary nutrients " + serving.adjusted().keySet());
        // Validate the separately resolved paid Spice dose before committing any of the serving.
        double units = com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge.resolveUnits(player, stack);
        if (units > 0 && !available.contains("spice"))
            throw new IllegalStateException("Cannot commit paid culinary Spice: Diet suite has no spice group");
        serving.adjusted().forEach((group, gain) ->
                diet.setValue(group, (float) Math.min(1, diet.getValue(group) + gain)));
        if (units > 0)
            diet.setValue("spice", (float) Math.min(1, diet.getValue("spice")
                    + com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge.dietGain(units)));
        diet.sync();
        state.addSugar(serving.sugar());
        state.addAlcohol(serving.alcohol());
        if (state.blackout) {
            state.blackoutX = player.getX(); state.blackoutY = player.getY(); state.blackoutZ = player.getZ();
        }
        DietBridge.syncMetabolic(player, state);
        com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents.culinaryEffectsCompleted(player);
        com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents.beverageFeedback(
                player, before, sugarBefore, alcoholBefore, stack.isEdible(), true);
        MetabolicStateStore.save(player);
    }
}
