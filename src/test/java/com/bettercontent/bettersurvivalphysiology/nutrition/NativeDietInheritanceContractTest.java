package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contract only; native recipe behavior remains Diet's responsibility. */
final class NativeDietInheritanceContractTest {
    @Test
    void nutritionInheritanceOverridesAreRemovedRatherThanJustDisabled() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/better_survival_physiology.mixins.json"));
        Path mixins = Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/mixin");
        for (String removed : new String[]{"DietValueGeneratorMixin", "DietApiImplMixin"}) {
            assertFalse(config.contains(removed), removed + " must not be registered");
            assertFalse(Files.exists(mixins.resolve(removed + ".java")), removed + " must not remain as dormant code");
        }
        // Removing inheritance must not disable the independent metabolism hooks.
        assertTrue(config.contains("\"DietDecayMixin\""));
        assertTrue(config.contains("\"PlayerExhaustionMixin\""));
    }
}
