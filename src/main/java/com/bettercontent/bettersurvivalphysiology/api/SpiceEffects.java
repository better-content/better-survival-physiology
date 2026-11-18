package com.bettercontent.bettersurvivalphysiology.api;

import com.bettercontent.bettersurvivalphysiology.compat.EpicFightCompat;
import com.bettercontent.bettersurvivalphysiology.compat.ThirstCompat;
import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

/** Optional provider-initiated Spice integration. Core owns the final Diet/attribute
 * combination, not dose discovery, health charge, or Spice policy configuration. */
public final class SpiceEffects {
    public record Terms(double mining, double movement, double resourceSavings,
                        double miningCap, double movementCap, double hpPerUnit, double hpCap) {
        public static final Terms EMPTY = new Terms(0, 0, 0, 0, 0, 0, 0);
        public Terms {
            for (double value : new double[]{mining, movement, resourceSavings,
                    miningCap, movementCap, hpPerUnit, hpCap})
                if (!Double.isFinite(value) || value < 0)
                    throw new IllegalArgumentException("Invalid Spice effect contribution");
        }
    }
    public interface Provider {
        /** Preview-only item classification: NOT evidence that a health charge was paid. */
        double resolveUnits(Player player, ItemStack stack);
        double dietGain(double completedUnits);
        Terms terms(double spiceLevel, double carbAmplification);
        /** Called at the same post-transient core tick position as the old reserve rule. */
        void activateEndurance(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition);
        boolean isConsumption(DamageSource source);
        void applyCompletedUnits(Player player, double completedUnits);
        void registerExternalCompletionOwner(ResourceLocation owner, Predicate<ItemStack> manages);
    }
    private static volatile Provider provider;
    private static final java.util.Map<ResourceLocation, Predicate<ItemStack>> PENDING_OWNERS =
            new java.util.LinkedHashMap<>();
    private SpiceEffects() {}

    public static synchronized void register(ResourceLocation owner, Provider value) {
        Objects.requireNonNull(owner); Objects.requireNonNull(value);
        if (provider != null) throw new IllegalStateException("Spice authority already registered");
        // Forge may construct optional mods on parallel threads. Hold registrations
        // until the provider is ready; never silently drop a Water completion owner.
        PENDING_OWNERS.forEach(value::registerExternalCompletionOwner);
        PENDING_OWNERS.clear();
        provider = value;
    }
    public static boolean available() { return provider != null; }
    public static double resolveUnits(Player player, ItemStack stack) {
        var current = provider;
        return current == null ? 0 : current.resolveUnits(player, stack);
    }
    public static double dietGain(double paidUnits) {
        if (!Double.isFinite(paidUnits) || paidUnits < 0 || paidUnits > 10000)
            throw new IllegalArgumentException("Invalid paid Spice dose");
        var current = provider;
        return current == null ? 0 : current.dietGain(paidUnits);
    }
    public static Terms terms(double spiceLevel, double carbAmplification) {
        var current = provider;
        return current == null ? Terms.EMPTY : Objects.requireNonNull(current.terms(spiceLevel, carbAmplification));
    }
    public static void activateEndurance(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        var current = provider;
        if (current != null) {
            current.activateEndurance(player, state, nutrition);
            return; // Spice is the sole transition owner while loaded.
        }
        // An already-saved Spice balance still worked after the optional mod was removed.
        // Preserve that standalone behavior without loading any Spice implementation.
        if (nutrition.spice() < SalienceConfig.FOURTH || state.enduranceReserveCooldown > 0) return;
        if (activateStandaloneEndurance(state, nutrition, player.getFoodData().getFoodLevel(),
                ThirstCompat.isLow(player), EpicFightCompat.isStaminaBelow(player, .10)))
            SalienceEvents.deepReserveActivated(player);
    }

    /** Historic core transition, reached only without the optional Spice provider. */
    static boolean activateStandaloneEndurance(MetabolicState state, NutritionSnapshot nutrition,
                                               int foodLevel, boolean lowThirst, boolean lowStamina) {
        if (nutrition.spice() < SalienceConfig.FOURTH || state.enduranceReserveCooldown > 0
                || (foodLevel > 2 && !lowThirst && !lowStamina)) return false;
        state.enduranceReserveTicks = (int) (10 * 20 * MetabolicMath.amplification(nutrition.carbs()));
        state.enduranceReserveCooldown = 2 * 60 * 20;
        return true;
    }
    public static boolean isConsumption(DamageSource source) {
        var current = provider;
        return current != null && current.isConsumption(source);
    }
    public static void applyCompletedUnits(Player player, double units) {
        if (units <= 0) return;
        if (!Double.isFinite(units) || units > 10000) throw new IllegalArgumentException("Invalid paid Spice dose");
        var current = provider;
        if (current != null) current.applyCompletedUnits(player, units);
    }
    public static synchronized void registerExternalCompletionOwner(ResourceLocation owner, Predicate<ItemStack> manages) {
        Objects.requireNonNull(owner); Objects.requireNonNull(manages);
        var current = provider;
        if (current != null) current.registerExternalCompletionOwner(owner, manages);
        else if (PENDING_OWNERS.putIfAbsent(owner, manages) != null)
            throw new IllegalArgumentException("Duplicate pending Spice completion owner: " + owner);
    }
}
