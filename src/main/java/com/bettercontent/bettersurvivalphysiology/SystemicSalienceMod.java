package com.bettercontent.bettersurvivalphysiology;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.network.SalienceNetwork;
import com.bettercontent.bettersurvivalphysiology.presentation.ModSounds;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SystemicSalienceMod.MOD_ID)
public final class SystemicSalienceMod {
    public static final String MOD_ID = "better_survival_physiology";

    public SystemicSalienceMod() {
        com.bettercontent.bettersurvivalphysiology.diagnostics.RuntimeDiagnostics.initialize();
        ModSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        com.bettercontent.bettersurvivalphysiology.clothing.WoolClothing.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SalienceConfig.SPEC);
        SalienceNetwork.init();
        if (net.minecraftforge.fml.ModList.get().isLoaded("cold_sweat"))
            com.bettercontent.bettersurvivalphysiology.climate.AmbientRateSeam.register();
        if (com.bettercontent.bettersurvivalphysiology.compat.BeverageAvailability.typed())
            com.bettercontent.bettersurvivalphysiology.compat.BeverageCompat.register();
        // Optional Spice API: never load its event class (even for Forge subscriber
        // reflection) when Spice is absent. Cooking-present source cutover is guarded
        // inside the event adapter until the atomic active-mod retirement.
        com.bettercontent.bettersurvivalphysiology.runtime.OptionalNativeFoodBridge.installIfPresent(
                net.minecraftforge.fml.ModList.get().isLoaded("better_spicy_farmers"));
    }
}
