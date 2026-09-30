package com.bettercontent.bettersurvivalphysiology.runtime;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MilkEffectGuardTest {
    @Test
    void interruptedUseCanClearTheSameUuidUsedByADeathClone() {
        MilkEffectGuard guard = new MilkEffectGuard();
        UUID player = UUID.randomUUID();

        guard.begin(player);
        assertTrue(guard.isGuarding(player));

        guard.clear(player);
        assertFalse(guard.isGuarding(player));
    }
}
