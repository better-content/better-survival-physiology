package com.bettercontent.bettersurvivalphysiology.api;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NativeDietRoster;
import com.bettercontent.bettersurvivalphysiology.nutrition.ServingVectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Optional culinary entitlement bridge. Providers own provenance/yield/quality resolution;
 * physiology owns intake. Neither registration nor resolution consumes anything. */
public final class CulinaryNutritionApi {
    public record Serving(ServingVectors.CanonicalServing unscaled, ServingVectors.CanonicalServing adjusted,
                          double sugar, double alcohol) {
        public Serving {
            Objects.requireNonNull(unscaled);
            Objects.requireNonNull(adjusted);
            check(sugar); check(alcohol);
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
    /** KC placed bites have no native ItemStack Finish; the Spice owner invokes this ONLY
     * after verifying a real native bite transition. Ordinary item meals remain native Diet.
     * The supplied nutrition/saturation are the EXACT post-quality native FoodData.eat
     * arguments, not a second inferred quality or an entire block-item entitlement. */
    public static void completeNativePlaced(ServerPlayer player, ItemStack capturedPortion,
                                            int nativeHunger, float nativeSaturation,
                                            double actualFraction, double paidSpiceUnits) {
        var diet = DietBridge.tracker(player).orElseThrow(() ->
                new IllegalStateException("Cannot commit placed nutrition: native Diet tracker unavailable"));
        if (!com.bettercontent.bettersurvivalphysiology.nutrition.NativePlacedDose.eligible(
                diet.isActive(), player.isCreative(), player.isSpectator())) return;
        // No preview/provider resolution may claim paid Spice. Existing providers must not
        // simultaneously claim this exact placed serving; refuse rather than double award.
        if (resolve(player, capturedPortion).isPresent())
            throw new IllegalStateException("Placed native serving overlaps a registered food provider");
        double alcohol = com.bettercontent.bettersurvivalphysiology.nutrition.NativePlacedDose.perBite(
                com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles.alcohol(capturedPortion), actualFraction);
        var result = com.illusivesoulworks.diet.api.DietApi.getInstance()
                .get(player, capturedPortion, nativeHunger, nativeSaturation);
        var nativeGains = ServingVectors.CanonicalServing.fromNative(result);
        // Native group-set result is ALREADY one post-quality bite; do not scale it.
        // The separate whole-item sugar/alcohol mirrors alone take the bite fraction.
        double sugar = com.bettercontent.bettersurvivalphysiology.nutrition.NativePlacedDose.sugarMirror(
                com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles.baseSugar(capturedPortion),
                actualFraction, nativeGains.gains());
        var canonical = nativeGains.resolveNativeMirrors(sugar, alcohol);
        apply(player, capturedPortion, new Serving(canonical, canonical, sugar, alcohol), paidSpiceUnits);
    }

    private static void apply(ServerPlayer player, ItemStack stack, Serving serving) {
        apply(player, stack, serving, com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge.resolveUnits(player, stack));
    }

    private static void apply(ServerPlayer player, ItemStack stack, Serving serving, double units) {
        var before = DietBridge.snapshot(player);
        var state = MetabolicStateStore.get(player);
        double sugarBefore = before.carbs(), alcoholBefore = before.alcohol();
        var diet = DietBridge.tracker(player).orElseThrow(() ->
                new IllegalStateException("Cannot commit paid culinary nutrition: Diet tracker unavailable"));
        var available = diet.getValues().keySet();
        if (!available.containsAll(serving.adjusted().gains().keySet()))
            throw new IllegalStateException("Diet suite has no culinary nutrients " + serving.adjusted().gains().keySet());
        // Validate the separately resolved paid Spice dose before committing any of the serving.
        if (!Double.isFinite(units) || units < 0 || units > 10000)
            throw new IllegalArgumentException("Invalid actual paid Spice dose");
        if (units > 0 && !available.contains("spice"))
            throw new IllegalStateException("Cannot commit paid culinary Spice: Diet suite has no spice group");
        serving.adjusted().gains().forEach((group, gain) -> {
            double paid = group.equals("spice") ? Math.max(gain,
                    com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge.dietGain(units)) : gain;
            diet.setValue(group, (float) Math.min(1, diet.getValue(group) + paid));
        });
        if (units > 0 && !serving.adjusted().gains().containsKey("spice"))
            diet.setValue("spice", (float) Math.min(1, diet.getValue("spice")
                    + com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge.dietGain(units)));
        diet.sync();
        // Cooking's paid per-ingredient source conversion already merged sugar/alcohol
        // mirrors into this one canonical serving. No second hidden metabolic award.
        if (DietBridge.snapshot(player).alcohol() >= 1.0f) {
            state.blackout = true;
            state.blackoutX = player.getX(); state.blackoutY = player.getY(); state.blackoutZ = player.getZ();
        }
        com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents.culinaryEffectsCompleted(player);
        com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents.beverageFeedback(
                player, before, sugarBefore, alcoholBefore, stack.isEdible(), true);
        MetabolicStateStore.save(player);
    }
}
