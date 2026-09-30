package com.bettercontent.bettersurvivalphysiology.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** A committed metabolic effect, independent of any teaching consumer. */
public final class MetabolicDiscoveryEvent extends PlayerEvent {
    public enum Kind { SUGAR_CRASH, HEAVY_BLOW, WORK_RHYTHM, DEEP_RESERVE, CLEANSE }
    private final Kind kind;
    private final String detail;
    private final String episode;
    public MetabolicDiscoveryEvent(ServerPlayer player, Kind kind, String detail) {
        super(player);
        this.kind = java.util.Objects.requireNonNull(kind);
        this.detail = java.util.Objects.requireNonNull(detail);
        this.episode = player.getUUID() + ":metabolic:" + kind.name() + ":" + player.level().getGameTime();
    }
    public Kind kind() { return kind; }
    public String detail() { return detail; }
    public String episode() { return episode; }
}
