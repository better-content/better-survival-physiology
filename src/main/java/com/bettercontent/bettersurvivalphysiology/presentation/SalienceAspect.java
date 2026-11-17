package com.bettercontent.bettersurvivalphysiology.presentation;

/** Five shared teaching identities; never the authority for Diet groups or RPG builds. */
public enum SalienceAspect {
    POWER("Power", 0, "✦", 0xFF4055, "impact"),
    MOTION("Motion", 3, "➜", 0xE0B01F, "mobility"),
    ENDURANCE("Endurance", 4, "∞", 0x52606A, "endurance"),
    RENEWAL("Renewal", 6, "✚", 0x6CCAF0, "renewal"),
    CONTROL("Control", 7, "⊕", 0x8E5BB7, "control");

    public final String displayName;
    public final int badgeSlot;
    public final String glyph;
    public final int color;
    public final String soundFamily;

    SalienceAspect(String displayName, int badgeSlot, String glyph, int color, String soundFamily) {
        this.displayName = displayName;
        this.badgeSlot = badgeSlot;
        this.glyph = glyph;
        this.color = color;
        this.soundFamily = soundFamily;
    }
}
