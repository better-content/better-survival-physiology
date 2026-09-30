package com.bettercontent.bettersurvivalphysiology.runtime;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Short-lived milk-use guard. It must never survive an interrupted player lifecycle. */
final class MilkEffectGuard {
    private final Set<UUID> guardedPlayers = new HashSet<>();

    void begin(UUID player) { guardedPlayers.add(player); }

    boolean isGuarding(UUID player) { return guardedPlayers.contains(player); }

    void clear(UUID player) { guardedPlayers.remove(player); }
}
