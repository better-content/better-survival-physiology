package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.network.DietDairySyncPacket;
import com.bettercontent.bettersurvivalphysiology.network.SalienceNetwork;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.stream.Collectors;

/** Invalid native registry/suite/tracker data is a configuration error, never an award to
 * silently discard. Do NOT call DietApi.get() from a preconsume guard: the optional Spice
 * integration decorates that lookup once and could claim a dose before real consumption. */
@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID)
public final class DietAwardGuard {
    private DietAwardGuard() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void started(ServerStartedEvent event) {
        validate(event.getServer());
        DietDairySplit.refreshServer();
        event.getServer().getPlayerList().getPlayers().forEach(DietAwardGuard::validateTracker);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void reloaded(OnDatapackSyncEvent event) {
        MinecraftServer server = event.getPlayerList().getServer();
        validate(server);
        // LOWEST runs after pinned DietEvents.syncDatapack: the native generator has just
        // rebuilt its trails; its group packet carried GENERATED group names, not trails.
        DietDairySyncPacket classification = DietDairySyncPacket.fromNative();
        if (event.getPlayer() != null) {
            SalienceNetwork.syncDerivedDairy(event.getPlayer(), classification);
            return;
        }
        // Diet's tracker may have retained values from the previous suite across reload.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tracker(player).initSuite();
            validateTracker(player);
            tracker(player).sync();
            SalienceNetwork.syncDerivedDairy(player, classification);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) validateTracker(player);
    }

    public static void validate(MinecraftServer server) {
        var level = server.overworld();
        var suite = DietSuites.getSuite(level, "builtin").orElseThrow(() ->
                new IllegalStateException("Diet config error: builtin suite missing"));
        var defined = DietGroups.getGroups(level).stream().map(g -> g.getName()).collect(Collectors.toSet());
        var active = suite.getGroups().stream().map(g -> g.getName()).collect(Collectors.toSet());
        validateRoster(defined, active);
        for (var group : DietGroups.getGroups(level)) {
            double multiplier = group.getDecayMultiplier();
            boolean timed = group.getName().equals("carbs") || group.getName().equals("alcohol");
            if (!Double.isFinite(multiplier) || (timed ? multiplier != 0.0 : multiplier <= 0.0))
                throw new IllegalStateException("Diet config error: " + group.getName()
                        + " decay_multiplier=" + multiplier + (timed ? " (requires zero; native clock owns decay)" : " (requires positive)"));
        }
    }

    public static void validateRoster(Set<String> defined, Set<String> active) {
        if (!defined.equals(NativeDietRoster.GROUPS) || !active.equals(NativeDietRoster.GROUPS))
            throw new IllegalStateException("Diet config error: defined=" + defined + ", active=" + active
                    + ", expected exactly=" + NativeDietRoster.GROUPS + ". Correct the data pack before play.");
    }

    public static void validateTracker(ServerPlayer player) {
        IDietTracker tracker = tracker(player);
        validateRoster(DietGroups.getGroups(player.level()).stream().map(g -> g.getName()).collect(Collectors.toSet()),
                DietSuites.getSuite(player.level(), tracker.getSuite()).orElseThrow(() ->
                        new IllegalStateException("Diet config error: missing player suite " + tracker.getSuite()))
                        .getGroups().stream().map(g -> g.getName()).collect(Collectors.toSet()));
        if (!tracker.getSuite().equals("builtin") || !tracker.getValues().keySet().equals(NativeDietRoster.GROUPS))
            throw new IllegalStateException("Diet config error: player " + player.getUUID() + " suite="
                    + tracker.getSuite() + ", tracker=" + tracker.getValues().keySet());
    }

    private static IDietTracker tracker(ServerPlayer player) {
        return DietBridge.tracker(player).orElseThrow(() ->
                new IllegalStateException("Diet config error: missing tracker for " + player.getUUID()));
    }
}
