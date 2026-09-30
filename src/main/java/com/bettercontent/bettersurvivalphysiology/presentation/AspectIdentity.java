package com.bettercontent.bettersurvivalphysiology.presentation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public enum AspectIdentity {
    IMPACT(0, "proteins", "✦", "Impact", 0xFF4055),
    TEMPO(1, "sugar", "»", "Tempo", 0x00A985),
    WORK(2, "grains", "⚒", "Work", 0xF0E2C5),
    MOBILITY(3, "fruits", "➜", "Mobility", 0xE0B01F),
    ENDURANCE(4, "fats", "∞", "Endurance", 0x52606A),
    ROBUSTNESS(5, "vegetables", "◆", "Robustness", 0xAF6A2F),
    RENEWAL(6, "dairy", "✚", "Renewal", 0x6CCAF0),
    CONTROL(7, "alcohol", "⊕", "Control", 0x8E5BB7);

    public final int icon;
    public final String representative;
    public final String glyph;
    public final String displayName;
    public final int color;

    AspectIdentity(int icon, String representative, String glyph, String displayName, int color) {
        this.icon = icon;
        this.representative = representative;
        this.glyph = glyph;
        this.displayName = displayName;
        this.color = color;
    }

    public static AspectIdentity fromGroupName(String name) {
        String normalized = (name.contains(":") ? name.substring(name.indexOf(':') + 1) : name).toLowerCase(Locale.ROOT);
        String identity = normalized.equals("sugars") ? "sugar" : normalized; // Diet's native group is plural.
        return Arrays.stream(values()).filter(value -> value.representative.equals(identity)).findFirst().orElse(null);
    }

    public static Comparator<AspectIdentity> displayOrder() { return Comparator.comparingInt(value -> value.icon); }
    public String badge() { return Character.toString(0xE100 + icon); }
}
