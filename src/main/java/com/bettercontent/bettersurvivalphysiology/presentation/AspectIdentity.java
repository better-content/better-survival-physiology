package com.bettercontent.bettersurvivalphysiology.presentation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/** Eight Diet-group presentation identities (legacy API name), mapped onto five aspects.
 * Group ordinals and names are not shared-aspect IDs; never merge their nutritional state.
 */
public enum AspectIdentity {
    IMPACT(0, "proteins", "Protein", "meat, eggs, legumes", SalienceAspect.POWER),
    TEMPO(1, "sugars", "Sugar", "fruit, honey, sweets", SalienceAspect.MOTION),
    WORK(2, "grains", "Grains", "grain, bread, porridge", SalienceAspect.POWER),
    MOBILITY(3, "fruits", "Fruit", "fruit, berries", SalienceAspect.MOTION),
    ENDURANCE(4, "vegetables", "Vegetables", "vegetables, greens, roots", SalienceAspect.ENDURANCE),
    ROBUSTNESS(5, "spice", "Spice", "spices, seasoned foods", SalienceAspect.ENDURANCE),
    RENEWAL(6, "dairy", "Dairy", "milk, cheese, cultured dairy", SalienceAspect.RENEWAL),
    CONTROL(7, "alcohol", "Alcohol", "fermented drinks", SalienceAspect.CONTROL);

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
