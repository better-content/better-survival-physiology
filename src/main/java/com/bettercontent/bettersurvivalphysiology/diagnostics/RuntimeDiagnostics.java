package com.bettercontent.bettersurvivalphysiology.diagnostics;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** One process-local budget; never retains world/player/menu references. */
@Mod.EventBusSubscriber(modid = "better_survival_physiology")
public final class RuntimeDiagnostics {
    public static final PlaytestLog LOG = new PlaytestLog("better_survival_physiology");
    private RuntimeDiagnostics() {}
    public static void initialize() {
        var mods = net.minecraftforge.fml.ModList.get();
        var own = mods.getModFileById("better_survival_physiology");
        String source = LOG.sourceIdentity(own == null ? null : own.getFile().findResource("META-INF", "better-content-source.properties"));
        String version = mods.getModContainerById("better_survival_physiology").map(c -> c.getModInfo().getVersion().toString()).orElse("UNKNOWN");
        LOG.status("common", version, source, () -> PlaytestLog.fields("enabled", true, "drinking_water", mods.isLoaded("better_drinking_water"), "diet", mods.isLoaded("diet"), "epicfight", mods.isLoaded("epicfight")));
    }
    public static void event(String category, String event, String result, String reason) {
        if (!LOG.enabled(category)) return;
        FeatureObservation.decision(LOG, category, event, result, reason);
    }
    public static void playerEvent(net.minecraft.server.level.ServerPlayer player, String event, String result, String reason) {
        if (!LOG.enabled("beverage")) return;
        FeatureObservation.playerDecision(LOG, () -> {
            var level = player.level();
            return new PlaytestLog.Context("server", level.dimension().location().toString(), level.getGameTime(), player.getUUID().toString(), "");
        }, event, result, reason);
    }
    public static PlaytestLog.Context context(net.minecraft.world.level.Level level) {
        return new PlaytestLog.Context(level.isClientSide ? "client" : "server", level.dimension().location().toString(), level.getGameTime(), "", "");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) LOG.summary("server");
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LOG.reset("server"); }
}
