package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.betterspicyfarmers.api.event.NativePlacedServingEvent;
import com.bettercontent.bettersurvivalphysiology.api.CulinaryNutritionApi;
import com.bettercontent.bettersurvivalphysiology.nutrition.CommittedServingTokens;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** KC's successful placed bite has no ItemStack Finish. Spice observes the native debit,
 * while Physiology alone interprets and credits its canonical five Diet nutrients. */
public final class NativePlacedIntake {
    private static final Map<UUID, CommittedServingTokens> COMMITTED = new HashMap<>();
    private NativePlacedIntake() {}

    /** Called only from the optional Spice-presence branch in SystemicSalienceMod. */
    public static void register() { MinecraftForge.EVENT_BUS.register(NativePlacedIntake.class); }

    @SubscribeEvent
    public static void completed(NativePlacedServingEvent event) {
        ServerPlayer player = event.player();
        if (player instanceof FakePlayer || player.connection == null || player.hasDisconnected()) return;
        if (!COMMITTED.computeIfAbsent(player.getUUID(), ignored -> new CommittedServingTokens()).claim(event)) return;
        CulinaryNutritionApi.completeNativePlaced(player, event.serving(), event.hunger(),
                event.saturationModifier(), event.fraction(), event.spiceUnits());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        COMMITTED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        COMMITTED.clear();
    }
}
