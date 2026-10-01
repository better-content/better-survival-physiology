package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.network.MealFeedbackPacket;
import com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity;
import com.bettercontent.bettersurvivalphysiology.presentation.DietBenefits;
import com.bettercontent.bettersurvivalphysiology.presentation.NutritionEstimates;
import com.bettercontent.bettersurvivalphysiology.presentation.NutritionTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MealRecap {
    private static final net.minecraft.resources.ResourceLocation ASPECT_FONT =
            new net.minecraft.resources.ResourceLocation("better_survival_physiology", "aspects");
    private static final net.minecraft.resources.ResourceLocation DEFAULT_FONT =
            new net.minecraft.resources.ResourceLocation("minecraft", "default");
    private static final int SETTLE_TICKS = 25;
    private static final int MINIMUM_DISPLAY_TICKS = 80;
    private static final Map<AspectIdentity, Entry> ENTRIES = new LinkedHashMap<>();
    private static int settle;
    private static int display;

    private MealRecap() {}

    public static void accept(MealFeedbackPacket packet) {
        if (settle == 0 && display == 0) ENTRIES.clear();
        AspectIdentity[] nutrientAspects = {AspectIdentity.IMPACT, AspectIdentity.WORK, AspectIdentity.MOBILITY,
                AspectIdentity.ENDURANCE, AspectIdentity.ROBUSTNESS, AspectIdentity.RENEWAL};
        for (int index = 0; index < 6; index++) if ((packet.changedMask() & (1 << index)) != 0) {
            NutritionTier tier = NutritionTier.of(packet.nutrients()[index]);
            ENTRIES.put(nutrientAspects[index], new Entry(nutrientAspects[index], tier, packet.nutrients()[index], packet.seconds()[index], State.NUTRIENT));
        }
        if (packet.sugarChanged()) {
            State state = State.SUGAR_ONE;
            ENTRIES.put(AspectIdentity.TEMPO, new Entry(AspectIdentity.TEMPO, NutritionTier.BUILDING,
                    packet.sugar(), packet.sugarSeconds(), state));
        }
        if (packet.alcoholChanged()) {
            State state = packet.alcohol() >= 1f ? State.ALCOHOL_IMPAIRED : State.ALCOHOL_LOW;
            ENTRIES.put(AspectIdentity.CONTROL, new Entry(AspectIdentity.CONTROL, NutritionTier.BUILDING,
                    packet.alcohol(), packet.alcoholSeconds(), state));
        }
        settle = SETTLE_TICKS;
        display = 0;
    }

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.options.hideGui || minecraft.player == null) return;
        if (settle > 0) {
            if (--settle == 0) display = Math.min(200, Math.max(MINIMUM_DISPLAY_TICKS, 40 + ENTRIES.size() * 20));
        } else if (display > 0 && --display == 0) ENTRIES.clear();
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, int anchorY) {
        if (minecraft.screen != null || minecraft.options.hideGui || settle > 0 || display <= 0 || ENTRIES.isEmpty()) return;
        int fadeTicks = 30;
        float opacity = display < fadeTicks ? display / (float) fadeTicks : 1.0f;
        int rise = display < fadeTicks ? Math.round((1.0f - opacity) * 10.0f) : 0;
        int lineHeight = 11;
        int textWidth = Math.max(1, graphics.guiWidth() - 36);
        var lines = ENTRIES.values().stream().flatMap(entry -> minecraft.font.split(entry.component(), textWidth).stream()).toList();
        int width = Math.min(graphics.guiWidth() - 24,
                ENTRIES.values().stream().map(Entry::component).mapToInt(component -> minecraft.font.width(component)).max().orElse(80) + 12);
        int height = lines.size() * lineHeight + 8;
        int x = (graphics.guiWidth() - width) / 2;
        int y = Math.max(8, anchorY - height - 7 - rise);
        int alpha = Math.max(4, Math.round(224 * opacity));
        graphics.fill(x, y, x + width, y + height, alpha << 24 | 0x101216);
        int textAlpha = Math.max(4, Math.round(255 * opacity)) << 24;
        int rowY = y + 4;
        for (var line : lines) {
            graphics.drawString(minecraft.font, line, x + 6, rowY, textAlpha | 0xffffff, false);
            rowY += lineHeight;
        }
    }

    public static String duration(int seconds) {
        if (seconds == NutritionEstimates.OVER_THIRTY_MINUTES) return ">30m";
        if (seconds < 0) return "";
        if (seconds < 60) return "~" + Math.max(5, Math.round(seconds / 5.0f) * 5) + "s";
        return "~" + Math.max(1, Math.round(seconds / 60.0f)) + "m";
    }

    private enum State { NUTRIENT, SUGAR_ONE, ALCOHOL_LOW, ALCOHOL_IMPAIRED }

    private record Entry(AspectIdentity aspect, NutritionTier tier, float value, int seconds, State state) {
        MutableComponent component() {
            MutableComponent result = Component.literal(aspect.badge()).withStyle(style -> style.withFont(ASPECT_FONT))
                    .append(Component.literal(" ").withStyle(style -> style.withFont(DEFAULT_FONT)))
                    .append(Component.literal(aspect.glyph + " " + label())
                            .withStyle(style -> style.withFont(DEFAULT_FONT).withColor(0xeee8d8)));
            String time = duration(seconds);
            if (!time.isEmpty()) result.append(Component.literal(" · " + time)
                    .withStyle(style -> style.withFont(DEFAULT_FONT).withColor(0xaaaaaa)));
            return result;
        }

        String label() {
            return switch (state) {
                case NUTRIENT -> aspect.displayName
                        + " — "
                        + (tier == NutritionTier.BUILDING ? "Undernourished · " + Math.round(value * 100) + "%"
                        : tier.name().substring(0, 1) + tier.name().substring(1).toLowerCase()
                        + " · " + DietBenefits.effect(aspect, tier));
                case SUGAR_ONE -> "Nectar — benefits ×" + String.format("%.1f", 1 + value)
                        + ", appetite ×" + String.format("%.1f", Math.pow(8, value));
                case ALCOHOL_LOW -> "Draught — damage reduction " + Math.round(40 * value) + "% · impairment rises";
                case ALCOHOL_IMPAIRED -> "Draught — blackout until 85%";
            };
        }
    }

}
