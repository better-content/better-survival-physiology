package com.bettercontent.bettersurvivalphysiology.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import java.util.function.Predicate;

/** Optional boundary: neither this class's signatures nor its constant pool link to newmod classes.
 * Loaded is resolved only behind the actual Forge mod-presence guard. Missing integration means no
 * Spice intake/cost/benefit, not a synthetic nutrition tracker or restoration of the removed Earth. */
public final class SpiceBridge {
    private SpiceBridge() {}
    public static boolean available() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded("better_spicy_farmers");
    }
    public static double resolveUnits(Player player, ItemStack stack) {
        return available() ? Loaded.resolveUnits(player, stack) : 0;
    }
    public static double dietGain(double units) { return available() ? Loaded.dietGain(units) : 0; }
    public static double miningBonus(double level, double amplification) {
        return available() ? Loaded.miningBonus(level, amplification) : 0;
    }
    public static double movementBonus(double level, double amplification) {
        return available() ? Loaded.movementBonus(level, amplification) : 0;
    }
    public static double hpPerUnit() { return available() ? Loaded.hpPerUnit() : 0; }
    public static double hpCap() { return available() ? Loaded.hpCap() : 0; }
    public static double miningCap() { return available() ? Loaded.miningCap() : 0; }
    public static double movementCap() { return available() ? Loaded.movementCap() : 0; }
    public static boolean isConsumption(DamageSource source) {
        return available() && Loaded.isConsumption(source);
    }
    public static void applyCompletedUnits(Player player, double units) {
        if (available()) Loaded.applyCompletedUnits(player, units);
    }
    public static void registerExternalCompletionOwner(ResourceLocation owner, Predicate<ItemStack> manages) {
        if (available()) Loaded.registerExternalCompletionOwner(owner, manages);
    }

    /** No event subscriber/static registration: absence never loads or initializes this adapter. */
    static final class Loaded {
        private Loaded() {}
        static double resolveUnits(Player player, ItemStack stack) {
            return com.bettercontent.betterspicyfarmers.api.SpiceApi.resolveUnits(player, stack);
        }
        static double dietGain(double units) {
            return com.bettercontent.betterspicyfarmers.core.SpiceMath.dietGain(units);
        }
        static double miningBonus(double level, double amplification) {
            return com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig.miningBonus(level, amplification);
        }
        static double movementBonus(double level, double amplification) {
            return com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig.movementBonus(level, amplification);
        }
        static double hpPerUnit() { return com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig.hpPerUnit(); }
        static double hpCap() { return com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig.hpCap(); }
        static double miningCap() { return com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig.miningCap(); }
        static double movementCap() { return com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig.movementCap(); }
        static boolean isConsumption(DamageSource source) {
            return com.bettercontent.betterspicyfarmers.runtime.SpiceRuntime.isConsumption(source);
        }
        static void applyCompletedUnits(Player player, double units) {
            com.bettercontent.betterspicyfarmers.runtime.SpiceRuntime.applyCompletedUnits(player, units);
        }
        static void registerExternalCompletionOwner(ResourceLocation owner, Predicate<ItemStack> manages) {
            com.bettercontent.betterspicyfarmers.runtime.SpiceRuntime.registerExternalCompletionOwner(owner, manages);
        }
    }
}
