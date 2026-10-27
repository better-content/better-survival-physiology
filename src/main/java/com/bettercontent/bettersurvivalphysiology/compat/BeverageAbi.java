package com.bettercontent.bettersurvivalphysiology.compat;

import java.util.function.Predicate;

/** Pure resource-level ABI admission, deliberately not reflective class loading or error recovery. */
public final class BeverageAbi {
    private BeverageAbi() {}
    public static boolean supportsTyped(Predicate<String> hasResource) {
        boolean plants = hasResource.test("com/bettercontent/betterdrinkingwater/PlantDrinks.class");
        boolean events = hasResource.test("com/bettercontent/betterdrinkingwater/api/BeverageEvents.class");
        if (plants != events) throw new IllegalStateException("Partial Better Drinking Water typed API; unsupported provider");
        if (!plants) return false; // Explicit legacy ABI, not an exception fallback.
        for (String nested : new String[]{"Profile", "Check", "Sip"}) {
            if (!hasResource.test("com/bettercontent/betterdrinkingwater/api/BeverageEvents$" + nested + ".class"))
                throw new IllegalStateException("Incomplete Better Drinking Water typed API: " + nested);
        }
        return true;
    }
}
