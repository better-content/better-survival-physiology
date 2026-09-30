package com.bettercontent.bettersurvivalphysiology.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import umpaz.brewinandchewin.common.capability.TipsyNumbedHeartsCapability;

public final class BrewingCompat {
    private BrewingCompat() {}

    public static void suppressNumbedHearts(ServerPlayer player) {
        if (!ModList.get().isLoaded("brewinandchewin")) return;
        player.getCapability(TipsyNumbedHeartsCapability.INSTANCE).ifPresent(capability -> {
            if (capability.getNumbedHealth() == 0.0f && capability.getTicksUntilDamage() == 0) return;
            capability.setNumbedHealth(0.0f);
            capability.setTicksUntilDamage(0);
            capability.syncToPlayer(player);
        });
    }
}
