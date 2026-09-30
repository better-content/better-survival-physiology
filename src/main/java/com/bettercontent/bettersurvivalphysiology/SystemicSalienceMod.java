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
        ModSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SalienceConfig.SPEC);
        SalienceNetwork.init();
    }
}
