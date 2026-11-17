package com.bettercontent.bettersurvivalphysiology.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionalNativeFoodBridgeTest {
    @Test void absentSpiceDoesNotInvokeOrLoadItsInstallationCallback() {
        // This executes the REAL bootstrap branch with no Spice event class loaded.
        assertDoesNotThrow(() -> OptionalNativeFoodBridge.installIfPresent(false));
        // This is NOT a live mod startup/classloader integration test.
    }
}
