package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.api.CulinaryNutritionApi;
import com.illusivesoulworks.diet.api.DietEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Suppress the native whole-stack Diet award only when a typed serving replaces it. */
@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID)
public final class CulinaryNutritionEvents {
    private CulinaryNutritionEvents() {}
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void suppressNative(DietEvent.ConsumeItemStack event) {
        if (CulinaryNutritionApi.resolve(null, event.getStack()).isPresent()) event.setCanceled(true);
    }
}
