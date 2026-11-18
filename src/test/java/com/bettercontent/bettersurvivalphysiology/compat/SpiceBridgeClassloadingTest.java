package com.bettercontent.bettersurvivalphysiology.compat;

/** Executed in separate actual-JVM classpaths, never mocked provider classes or source-string checks.
 * The present-provider run invokes the actual provider policy directly because standalone JVMs
 * have no Forge registration; native qualification checks provider-initiated registration. */
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
            SpiceBridge.registerExternalCompletionOwner(
                    new net.minecraft.resources.ResourceLocation("better_survival_physiology", "absent_fixture"),
                    stack -> false);
            System.out.println("SpiceBridgeClassloadingTest PASS absent: actual no-newmod classpath, all public fallbacks invoked");
        } else if (args[0].equals("present")) {
            Class<?> provider = Class.forName("com.bettercontent.betterspicyfarmers.core.SpiceMath", true, loader);
            require(provider.getProtectionDomain().getCodeSource() != null, "Actual provider code source required");
            Class<?> policy = Class.forName("com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig", true, loader);
            near((double) provider.getMethod("dietGain", double.class).invoke(null, 1d), .2);
            near((double) provider.getMethod("dietGain", double.class).invoke(null, 10d), 1);
            near((double) policy.getMethod("miningBonus", double.class, double.class).invoke(null, 1d, 3d), .4);
            near((double) policy.getMethod("movementBonus", double.class, double.class).invoke(null, 1d, 3d), .2);
            near((double) policy.getMethod("hpPerUnit").invoke(null), 4);
            near((double) policy.getMethod("hpCap").invoke(null), 8);
            near((double) policy.getMethod("miningCap").invoke(null), .4);
            near((double) policy.getMethod("movementCap").invoke(null), .2);
            require(!SpiceBridge.available(), "Classpath alone cannot register a provider or mutate Diet");
            zero(SpiceBridge.resolveUnits(null, null));
            System.out.println("SpiceBridgeClassloadingTest PASS present: actual provider="
                    + provider.getProtectionDomain().getCodeSource().getLocation()
                    + "; real provider config/math invoked; no Forge launch or inferred payment");
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
