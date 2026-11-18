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
        // Water registers its own profile/paid-sip adapter when present; the core
        // never discovers or classloads a provider implementation.
        // Spice initiates its optional event adapter after registration; this core
        // never loads Spice's event class or supplies a second placed-bite subscriber.
    }
}
