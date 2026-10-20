package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.illusivesoulworks.diet.api.DietApi;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ConsumableProfiles {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final TagKey<Item> DIET_SUGARS = TagKey.create(Registries.ITEM, new ResourceLocation("diet", "sugars"));
    public static final TagKey<Item> DIET_FRUITS = TagKey.create(Registries.ITEM, new ResourceLocation("diet", "fruits"));
    public static final TagKey<Item> SUGAR_MEDIUM = TagKey.create(Registries.ITEM, new ResourceLocation("better_survival_physiology", "sugar_medium"));
    public static final TagKey<Item> SUGAR_HIGH = TagKey.create(Registries.ITEM, new ResourceLocation("better_survival_physiology", "sugar_high"));

    private ConsumableProfiles() {}

    /** Tag eligibility and upper bound only; never use directly as a resolved serving award. */
    public static double baseSugar(ItemStack stack) {
        if (!stack.is(DIET_SUGARS)) return 0.0;
        if (stack.is(SUGAR_HIGH)) return 0.34;
        if (stack.is(SUGAR_MEDIUM)) return 0.22;
        if (stack.is(DIET_FRUITS)) return 0.06;
        return 0.12;
    }

    public static double servingSugar(ItemStack stack, Map<String, Double> dietGains) {
        return NectarGain.capped(baseSugar(stack), dietGains);
    }

    /** Resolve potential gains, not remaining tracker capacity. Failed lookup never means pure Nectar. */
    public static Optional<Map<String, Double>> dietGains(Player player, ItemStack stack) {
        try {
            Map<String, Double> gains = new HashMap<>();
            DietApi.getInstance().get(player, stack).get()
                    .forEach((group, gain) -> gains.put(group.getName(), gain.doubleValue()));
            return Optional.of(gains);
        } catch (RuntimeException failure) {
            LOGGER.error("Cannot resolve Diet serving gains for {}; Nectar withheld", BuiltInRegistries.ITEM.getKey(stack.getItem()), failure);
            return Optional.empty();
        }
    }

    public static double servingSugar(Player player, ItemStack stack) {
        if (baseSugar(stack) == 0) return 0;
        return dietGains(player, stack).map(gains -> servingSugar(stack, gains)).orElse(0.0);
    }

    public static double alcohol(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AlcoholProfiles.potency(id.toString());
    }

    public static boolean isAlcohol(ItemStack stack) {
        return alcohol(stack) > 0.0;
    }

}
