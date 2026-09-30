package com.bettercontent.bettersurvivalphysiology.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MilkEffectGuardContractTest {
    @Test
    void milkCancelsTypedBeneficialRemovalWithoutReaddingEffects() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/runtime/SalienceEvents.java"));
        assertTrue(source.contains("MobEffectEvent.Remove"));
        assertTrue(source.contains("event.setCanceled(true)"));
        assertTrue(source.contains("LivingEntityUseItemEvent.Stop"));
        assertFalse(source.contains("player.addEffect(exact)"));
    }
}
