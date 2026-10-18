package com.bettercontent.bettersurvivalphysiology.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MilkEffectGuardContractTest {
    @Test
    void nutritionLeavesBeneficialMilkPreservationToRpg() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/runtime/SalienceEvents.java"));
        assertFalse(source.contains("preserveMilkEffects("));
        assertFalse(source.contains("MILK_EFFECT_GUARD"));
        assertFalse(source.contains("player.addEffect(exact)"));
        assertTrue(source.contains("DietBridge.awardMilk(player)"));
    }
}
