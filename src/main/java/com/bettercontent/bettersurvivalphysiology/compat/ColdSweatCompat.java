package com.bettercontent.bettersurvivalphysiology.compat;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ColdSweatCompat {
    private static final Map<UUID, Double> LAST_BODY = new HashMap<>();

    private ColdSweatCompat() {}

    public static void dampenDrift(ServerPlayer player, double resistance) {
        if (!ModList.get().isLoaded("cold_sweat")) return;
        double current = Temperature.get(player, Temperature.Trait.BODY);
        Double previous = LAST_BODY.put(player.getUUID(), current);
        if (previous == null) return;
        boolean movingAway = Math.signum(current) == Math.signum(previous) && Math.abs(current) > Math.abs(previous);
        if (!movingAway) return;
        double corrected = previous + (current - previous) * Math.max(0.0, 1.0 - resistance);
        Temperature.set(player, Temperature.Trait.BODY, corrected);
        LAST_BODY.put(player.getUUID(), corrected);
    }

    public static void pullSafe(ServerPlayer player) {
        if (!ModList.get().isLoaded("cold_sweat")) return;
        double body = Temperature.get(player, Temperature.Trait.BODY);
        Temperature.set(player, Temperature.Trait.BODY, Math.copySign(Math.min(Math.abs(body), 0.25), body));
        LAST_BODY.put(player.getUUID(), Temperature.get(player, Temperature.Trait.BODY));
    }

    public static boolean isTemperatureDamage(net.minecraft.world.damagesource.DamageSource source) {
        return source.typeHolder().unwrapKey()
                .map(key -> key.location().getNamespace().equals("cold_sweat"))
                .orElse(false);
    }

    public static void unload(ServerPlayer player) {
        LAST_BODY.remove(player.getUUID());
    }
}
