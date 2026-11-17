package com.bettercontent.bettersurvivalphysiology.nutrition;

/** Native hunger/Diet result already describes one placed bite. Only a whole-item
 * supplemental potency or mirror is apportioned by the actual native bite fraction. */
public final class NativePlacedDose {
    private NativePlacedDose() {}

    public static double perBite(double whole, double fraction) {
        if (!Double.isFinite(whole) || whole < 0 || !Double.isFinite(fraction)
                || fraction <= 0 || fraction > 1)
            throw new IllegalArgumentException("Invalid native placed potency/fraction");
        return whole * fraction;
    }

    /** Native gains are ALREADY one bite: cap the scaled sugar base against those
     * per-bite gains. Capping the whole base first and scaling that cap again would
     * underpay when a small per-bite Carb result is the limiting group. */
    public static double sugarMirror(double wholeBase, double fraction,
                                     java.util.Map<String, Double> nativePerBiteGains) {
        return com.bettercontent.bettersurvivalphysiology.metabolism.NectarGain.capped(
                perBite(wholeBase, fraction), nativePerBiteGains);
    }

    public static boolean eligible(boolean nativeDietActive, boolean creative, boolean spectator) {
        return nativeDietActive && !creative && !spectator;
    }
}
