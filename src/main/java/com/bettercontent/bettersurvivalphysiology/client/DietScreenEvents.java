package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.mixin.DietScreenAccessor;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class DietScreenEvents {
    private DietScreenEvents() {}

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DietScreen original) {
            event.setNewScreen(new SystemicDietScreen(((DietScreenAccessor) original).systemicSalience$fromInventory()));
        }
    }
}
