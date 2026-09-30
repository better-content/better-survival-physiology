package com.bettercontent.bettersurvivalphysiology.metabolism;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ConsumableProfiles {
    public static final TagKey<Item> DIET_SUGARS = TagKey.create(Registries.ITEM, new ResourceLocation("diet", "sugars"));
    public static final TagKey<Item> DIET_FRUITS = TagKey.create(Registries.ITEM, new ResourceLocation("diet", "fruits"));
    public static final TagKey<Item> SUGAR_MEDIUM = TagKey.create(Registries.ITEM, new ResourceLocation("better_survival_physiology", "sugar_medium"));
    public static final TagKey<Item> SUGAR_HIGH = TagKey.create(Registries.ITEM, new ResourceLocation("better_survival_physiology", "sugar_high"));

    private ConsumableProfiles() {}

    public static double sugar(ItemStack stack) {
        if (!stack.is(DIET_SUGARS)) return 0.0;
        if (stack.is(SUGAR_HIGH)) return 0.34;
        if (stack.is(SUGAR_MEDIUM)) return 0.22;
        if (stack.is(DIET_FRUITS)) return 0.06;
        return 0.12;
    }

    public static double alcohol(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AlcoholProfiles.potency(id.toString());
    }

    public static boolean isAlcohol(ItemStack stack) {
        return alcohol(stack) > 0.0;
    }

}
