package com.bettercontent.bettersurvivalphysiology.compat;

/** Executed in separate actual-JVM classpaths, never mocked provider classes or source-string checks.
 * The present-provider run exercises Loaded directly because standalone JVMs have no Forge ModList;
 * actual mod-list/bootstrap registration remains a separate native qualification gate. */
public final class SpiceBridgeClassloadingTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("absent or present");
        ClassLoader loader = SpiceBridgeClassloadingTest.class.getClassLoader();
        if (args[0].equals("absent")) {
            boolean absent = false;
            try { Class.forName("com.bettercontent.betterspicyfarmers.core.SpiceMath", false, loader); }
            catch (ClassNotFoundException expected) { absent = true; }
            require(absent, "Actual newmod classes MUST be absent from this process");
            require(!SpiceBridge.available(), "No Forge-loaded optional mod in standalone classloading check");
            zero(SpiceBridge.resolveUnits(null, null)); zero(SpiceBridge.dietGain(1));
            zero(SpiceBridge.miningBonus(1, 3)); zero(SpiceBridge.movementBonus(1, 3));
            zero(SpiceBridge.hpPerUnit()); zero(SpiceBridge.hpCap());
            zero(SpiceBridge.miningCap()); zero(SpiceBridge.movementCap());
            require(!SpiceBridge.isConsumption(null), "Absent damage-source check never links newmod");
            SpiceBridge.applyCompletedUnits(null, 1);
            SpiceBridge.registerExternalCompletionOwner(null, null);
            System.out.println("SpiceBridgeClassloadingTest PASS absent: actual no-newmod classpath, all public fallbacks invoked");
        } else if (args[0].equals("present")) {
            Class<?> provider = Class.forName("com.bettercontent.betterspicyfarmers.core.SpiceMath", true, loader);
            require(provider.getProtectionDomain().getCodeSource() != null, "Actual provider code source required");
            near(SpiceBridge.Loaded.dietGain(1), .2);
            near(SpiceBridge.Loaded.dietGain(10), 1);
            near(SpiceBridge.Loaded.miningBonus(1, 3), .4);
            near(SpiceBridge.Loaded.movementBonus(1, 3), .2);
            near(SpiceBridge.Loaded.hpPerUnit(), 4); near(SpiceBridge.Loaded.hpCap(), 8);
            near(SpiceBridge.Loaded.miningCap(), .4); near(SpiceBridge.Loaded.movementCap(), .2);
            zero(SpiceBridge.Loaded.resolveUnits(null, null));
            System.out.println("SpiceBridgeClassloadingTest PASS present: actual provider="
                    + provider.getProtectionDomain().getCodeSource().getLocation()
                    + "; Loaded config/math/API delegates invoked, not a Forge launch");
        } else throw new IllegalArgumentException(args[0]);
    }
    private static void zero(double value) { near(value, 0); }
    private static void near(double actual, double expected) {
        require(Math.abs(actual - expected) < 1e-9, "Expected " + expected + ", got " + actual);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
