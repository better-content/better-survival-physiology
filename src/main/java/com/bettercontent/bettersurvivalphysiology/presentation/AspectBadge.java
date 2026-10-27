package com.bettercontent.bettersurvivalphysiology.presentation;

/** Source-image coordinates shared by the HUD and its resource contract tests. */
public record AspectBadge(String namespace, String path, int u, int v, int width, int height,
                          int textureWidth, int textureHeight) {
    public static AspectBadge of(AspectIdentity aspect) { return of(aspect, false); }
    public static AspectBadge of(AspectIdentity aspect, boolean spiceAvailable) {
        if (aspect == AspectIdentity.ROBUSTNESS) { // Existing slot 5 now represents Spice, glyph U+E105.
            return new AspectBadge(spiceAvailable ? "better_spicy_farmers" : "better_survival_physiology",
                    "textures/gui/spice_badge.png", 0, 0, 18, 18, 18, 18);
        }
        return new AspectBadge("better_survival_physiology", "textures/gui/aspect_badges.png",
                aspect.icon * 18, 0, 18, 18, 144, 18);
    }
}
