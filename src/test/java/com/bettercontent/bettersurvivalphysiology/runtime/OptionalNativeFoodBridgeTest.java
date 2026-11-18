package com.bettercontent.bettersurvivalphysiology.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionalNativeFoodBridgeTest {
    @Test void coreStartupDoesNotLoadOptionalSpiceEventOrDoubleRegisterPlacedBites() throws Exception {
        String startup = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/bettercontent/bettersurvivalphysiology/SystemicSalienceMod.java"));
        String provider = java.nio.file.Files.readString(java.nio.file.Path.of(
                "../better-spicy-farmers/src/main/java/com/bettercontent/betterspicyfarmers/api/SpiceContributions.java"));
        assertFalse(startup.contains("OptionalNativeFoodBridge.installIfPresent("));
        assertFalse(java.nio.file.Files.exists(java.nio.file.Path.of(
                "src/main/java/com/bettercontent/bettersurvivalphysiology/runtime/NativePlacedIntake.java")));
        assertTrue(provider.contains("public static void installPhysiology() { PhysiologyPresent.install(); }"));
        assertTrue(provider.contains("new com.bettercontent.bettersurvivalphysiology.nutrition.CommittedServingTokens()"));
        assertTrue(provider.contains(".claim(event)"));
        // Source invariant only; actual isolated no-Spice/core and exact native bites are separate gates.
    }
}
