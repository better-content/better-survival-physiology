package com.bettercontent.bettersurvivalphysiology.presentation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/** Five Diet food identities, each teaching its corresponding (secondary) shared aspect. */
public enum AspectIdentity {
    POWER(0, "proteins", "Protein", "meat, eggs, legumes", SalienceAspect.POWER),
    MOTION(1, "carbs", "Carbs", "grain, fruit, honey, dairy", SalienceAspect.MOTION),
    ENDURANCE(2, "spice", "Spice", "spices, seasoned foods", SalienceAspect.ENDURANCE),
    RENEWAL(3, "vegetables", "Vegetables", "vegetables, greens, roots", SalienceAspect.RENEWAL),
    CONTROL(4, "alcohol", "Alcohol", "fermented drinks (optional)", SalienceAspect.CONTROL);

    // Binary/source compatibility for non-authoritative sound/feedback names. These are NOT
    // additional enum values, Diet groups, tracker values, or independent nutrition balances.
    @Deprecated public static final AspectIdentity IMPACT = POWER, WORK = POWER;
    @Deprecated public static final AspectIdentity TEMPO = MOTION, MOBILITY = MOTION;
    @Deprecated public static final AspectIdentity ROBUSTNESS = ENDURANCE;

    private final int groupOrder;
    public final SalienceAspect aspect;
    public final int icon;
    public final String representative;
    public final String glyph;
    public final String displayName;
    public final String sources;
    public final int color;

    AspectIdentity(int groupOrder, String representative, String displayName, String sources, SalienceAspect aspect) {
        this.groupOrder = groupOrder;
        this.aspect = aspect;
        this.icon = aspect.badgeSlot;
        this.representative = representative;
        this.glyph = aspect.glyph;
        this.displayName = displayName;
        this.sources = sources;
        this.color = aspect.color;
    }

    public static AspectIdentity fromGroupName(String name) {
        String normalized = (name.contains(":") ? name.substring(name.indexOf(':') + 1) : name).toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(value -> value.representative.equals(normalized)).findFirst().orElse(null);
    }

    public static Comparator<AspectIdentity> displayOrder() { return Comparator.comparingInt(value -> value.groupOrder); }
    public String badge() { return Character.toString(0xE100 + icon); }
}
