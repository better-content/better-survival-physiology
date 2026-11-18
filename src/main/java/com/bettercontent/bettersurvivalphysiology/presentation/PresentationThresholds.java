package com.bettercontent.bettersurvivalphysiology.presentation;

/** Server-owned unlock thresholds used when constructing the trusted synced countdown. */
public final class PresentationThresholds {
    private PresentationThresholds() {}

    public static double activePerkFloor(AspectIdentity aspect, double value) {
        if (aspect == AspectIdentity.CONTROL) return 0;
        if (value >= .80) return .80;
        if ((aspect == AspectIdentity.POWER || aspect == AspectIdentity.RENEWAL) && value >= .40) return .40;
        if (aspect == AspectIdentity.MOTION && value >= .20) return .20;
        return 0;
    }
}
