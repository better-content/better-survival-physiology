package com.bettercontent.bettersurvivalphysiology.compat;

import dev.ghen.thirst.api.ThirstHelper;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class ThirstCompat {
    private ThirstCompat() {}

    public static boolean isDrink(ItemStack stack) {
        return ModList.get().isLoaded("thirst") && ThirstHelper.isDrink(stack);
    }

    public static void addQuenched(ServerPlayer player, int amount) {
        if (!ModList.get().isLoaded("thirst") || amount <= 0) return;
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(thirst -> {
            thirst.setQuenched(Math.min(20, thirst.getQuenched() + amount));
            thirst.updateThirstData(player);
        });
    }

    public static void addExhaustion(ServerPlayer player, float amount) {
        if (!ModList.get().isLoaded("thirst") || amount <= 0.0f) return;
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(thirst -> thirst.addExhaustion(player, amount));
    }

    public static boolean isLow(ServerPlayer player) {
        if (!ModList.get().isLoaded("thirst")) return false;
        return player.getCapability(ModCapabilities.PLAYER_THIRST).map(thirst -> thirst.getThirst() <= 6).orElse(false);
    }

    public static boolean needsQuenched(ServerPlayer player) {
        if (!ModList.get().isLoaded("thirst")) return false;
        return player.getCapability(ModCapabilities.PLAYER_THIRST).map(thirst -> thirst.getQuenched() <= 16).orElse(false);
    }
}
