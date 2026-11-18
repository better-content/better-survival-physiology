package com.bettercontent.bettersurvivalphysiology.compat;

import com.bettercontent.bettersurvivalphysiology.api.SpiceEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Compatibility facade over provider-initiated, explicit contributions. No Spice
 * implementation class is discovered, loaded or referenced by core production. */
public final class SpiceBridge {
    private SpiceBridge() {}
    public static boolean available() { return SpiceEffects.available(); }
    public static double resolveUnits(Player player, ItemStack stack) { return SpiceEffects.resolveUnits(player, stack); }
    public static double dietGain(double units) { return SpiceEffects.dietGain(units); }
    public static double miningBonus(double level, double amplification) {
        return SpiceEffects.terms(level, amplification).mining();
    }
    public static double movementBonus(double level, double amplification) {
        return SpiceEffects.terms(level, amplification).movement();
    }
    public static double hpPerUnit() { return SpiceEffects.terms(0, 1).hpPerUnit(); }
    public static double hpCap() { return SpiceEffects.terms(0, 1).hpCap(); }
    public static double miningCap() { return SpiceEffects.terms(0, 1).miningCap(); }
    public static double movementCap() { return SpiceEffects.terms(0, 1).movementCap(); }
    public static boolean isConsumption(DamageSource source) { return SpiceEffects.isConsumption(source); }
    public static void applyCompletedUnits(Player player, double units) {
        SpiceEffects.applyCompletedUnits(player, units);
    }
    public static void registerExternalCompletionOwner(ResourceLocation owner, Predicate<ItemStack> manages) {
        SpiceEffects.registerExternalCompletionOwner(owner, manages);
    }
}
