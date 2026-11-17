package com.bettercontent.bettersurvivalphysiology.runtime;

/** Class-free optional-mod boundary: the caller's installation callback must not be
 * invoked (or resolve its Spice event parameter) when Spice is absent. */
public final class OptionalNativeFoodBridge {
    private OptionalNativeFoodBridge() {}
    public static void installIfPresent(boolean spicePresent) {
        // This outer class has no reference to Spice's event class. Even the
        // NativePlacedIntake call lives in a nested class loaded only on TRUE.
        if (spicePresent) SpicePresent.install();
    }

    private static final class SpicePresent {
        private static void install() { NativePlacedIntake.register(); }
    }
}
