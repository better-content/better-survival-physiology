package com.bettercontent.bettersurvivalphysiology.climate;

/** Pinned Cold Sweat harmful drift formula; pure math to keep source weights testable without Forge. */
final class AmbientRateMath {
    private static final double TARGET_TICKS = 72_000D;

    private AmbientRateMath() {}

    /** Scale Cold Sweat's already-computed RATE, retaining native insulation and equipment effects.
     *  Local emissions remain configured in Cold Sweat. Their excess harmful drift is restored
     *  continuously; opposing sources reduce the final native drift without accelerating it.
     */
    static double adjustHarmfulDrift(double nativeRate, double ambient, double world,
                                     double freezing, double burning, double configuredRate, double body) {
        if (!Double.isFinite(nativeRate) || nativeRate == 0 || !Double.isFinite(ambient)
                || !Double.isFinite(world) || !Double.isFinite(freezing) || !Double.isFinite(burning)
                || freezing >= burning || !Double.isFinite(configuredRate) || configuredRate <= 0
                || !Double.isFinite(body)) return nativeRate;
        double ambientDelta = ambient < freezing ? ambient - freezing
                : ambient > burning ? ambient - burning : 0;
        double worldDelta = world < freezing ? world - freezing
                : world > burning ? world - burning : 0;
        // Recovery, safe ambient, or a source that changes the direction of exposure
        // belongs to Cold Sweat. Only slow sustained harmful ambient drift.
        if (ambientDelta == 0 || worldDelta == 0
                || Math.signum(ambientDelta) != Math.signum(worldDelta)
                || Math.signum(worldDelta) != Math.signum(nativeRate)
                || Math.signum(body) == -Math.signum(nativeRate)) return nativeRate;
        double ambientNaked = Math.max(Math.abs(ambientDelta) * configuredRate / 7D, configuredRate / 50D);
        double worldNaked = Math.max(Math.abs(worldDelta) * configuredRate / 7D, configuredRate / 50D);
        if (!Double.isFinite(ambientNaked) || !Double.isFinite(worldNaked) || worldNaked <= 0)
            return nativeRate;
        double ambientFraction = Math.min(1D, (100D / TARGET_TICKS) / ambientNaked);
        // The configured block/item/entity emissions set source strengths. A weak source
        // adds only its excess native harmful drift, never switching to full native RATE.
        double localShare = Math.max(0D, 1D - ambientNaked / worldNaked);
        double fraction = ambientFraction + (1D - ambientFraction) * localShare;
        return nativeRate * fraction;
    }
}
