package com.bettercontent.bettersurvivalphysiology.fixture;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Only in the nonshipping nativeFixture source set: two intentionally untagged edible
 * recipe results to test Diet's real ingredient-derived GENERATED membership and TRAILS. */
@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DietDerivedFixtureItems {
    public static final ResourceLocation DAIRY = new ResourceLocation(SystemicSalienceMod.MOD_ID, "fixture_dairy_only_food");
    public static final ResourceLocation MIXED = new ResourceLocation(SystemicSalienceMod.MOD_ID, "fixture_dairy_meat_food");

    private DietDerivedFixtureItems() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            FoodProperties food = new FoodProperties.Builder().nutrition(4).saturationMod(.3f).build();
            helper.register(DAIRY, new Item(new Item.Properties().food(food)));
            helper.register(MIXED, new Item(new Item.Properties().food(food)));
        });
    }
}
