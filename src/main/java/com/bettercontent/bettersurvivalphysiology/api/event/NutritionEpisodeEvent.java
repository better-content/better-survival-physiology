package com.bettercontent.bettersurvivalphysiology.api.event;

import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** A provider-owned nutrition episode boundary with an ID stable from warning through recovery. */
public final class NutritionEpisodeEvent extends Event {
    public enum Kind { WARNING, RECOVERED }

    private final ServerPlayer player;
    private final Kind kind;
    private final NutritionSnapshot before;
    private final NutritionSnapshot after;
    private final String episodeId;

    public NutritionEpisodeEvent(ServerPlayer player, Kind kind, NutritionSnapshot before,
                                 NutritionSnapshot after, String episodeId) {
        this.player = Objects.requireNonNull(player, "player");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.before = Objects.requireNonNull(before, "before");
        this.after = Objects.requireNonNull(after, "after");
        this.episodeId = Objects.requireNonNull(episodeId, "episodeId");
    }

    public ServerPlayer player() { return player; }
    public Kind kind() { return kind; }
    public NutritionSnapshot before() { return before; }
    public NutritionSnapshot after() { return after; }
    public String episodeId() { return episodeId; }
}
