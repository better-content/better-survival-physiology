package com.bettercontent.bettersurvivalphysiology.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Do not load the typed bridge against accepted legacy Water, which does not publish this optional API. */
public final class BeverageAvailability {
    private BeverageAvailability() {}
    private static final class Typed {
        private static final boolean PRESENT = detect();
        private static boolean detect() {
            ClassLoader loader = BeverageAvailability.class.getClassLoader();
            return BeverageAbi.supportsTyped(path -> loader.getResource(path) != null);
        }
    }
    public static boolean typed() { return ModList.get().isLoaded("better_drinking_water") && Typed.PRESENT; }
    public static boolean manages(ItemStack stack) { return typed() && BeverageCompat.manages(stack); }
}
