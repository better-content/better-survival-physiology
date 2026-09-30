package com.bettercontent.bettersurvivalphysiology.metabolism;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MetabolicStateStore {
    public static final String ROOT_KEY = "better_survival_physiology";
    private static final Map<UUID, MetabolicState> STATES = new HashMap<>();

    private MetabolicStateStore() {}

    public static MetabolicState get(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> readPersistent(player));
    }

    public static void save(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persistent.put(ROOT_KEY, get(player).save());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
    }

    public static void copy(Player original, Player replacement) {
        MetabolicState copy = MetabolicState.load(get(original).save());
        STATES.put(replacement.getUUID(), copy);
        if (replacement instanceof ServerPlayer serverPlayer) save(serverPlayer);
    }

    public static void reset(Player player) {
        STATES.put(player.getUUID(), new MetabolicState());
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persistent.remove(ROOT_KEY);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
    }

    public static void unload(Player player) {
        STATES.remove(player.getUUID());
    }

    private static MetabolicState readPersistent(Player player) {
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persistent.contains(ROOT_KEY) ? MetabolicState.load(persistent.getCompound(ROOT_KEY)) : new MetabolicState();
    }
}
