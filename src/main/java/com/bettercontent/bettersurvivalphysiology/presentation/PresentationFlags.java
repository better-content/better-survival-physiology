package com.bettercontent.bettersurvivalphysiology.presentation;

public final class PresentationFlags {
    public static final int IMPACT_READY = 1;
    public static final int MOBILITY_STRIDE = 1 << 1;
    public static final int ENDURANCE_READY = 1 << 2;
    public static final int ENDURANCE_ACTIVE = 1 << 3;
    public static final int ROBUSTNESS_READY = 1 << 4;
    public static final int RENEWAL_READY = 1 << 5;
    public static final int TEMPO_ONE = 1 << 6;
    public static final int TEMPO_TWO = 1 << 7;
    public static final int TEMPO_CRASH = 1 << 8;
    public static final int CONTROL_COMPOSED = 1 << 9;
    public static final int CONTROL_IMPAIRED = 1 << 10;
    public static final int CONTROL_BLACKOUT = 1 << 11;

    private PresentationFlags() {}

    public static boolean has(int flags, int mask) { return (flags & mask) != 0; }
}
