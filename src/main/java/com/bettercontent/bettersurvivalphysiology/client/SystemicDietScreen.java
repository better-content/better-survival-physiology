package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity;
import com.bettercontent.bettersurvivalphysiology.presentation.DietBenefits;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/** One always-visible guide for current Diet progress and every effect tier. */
public final class SystemicDietScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int GAP = 8;
    private static final int TOP = 37;
    private static final int BOTTOM = 34;
    private final boolean fromInventory;
    private int scroll;
    private int maxScroll;

    public SystemicDietScreen(boolean fromInventory) {
        super(Component.translatable("gui.diet.title"));
        this.fromInventory = fromInventory;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.diet.close"), button -> onClose())
                .bounds(width / 2 - 50, height - 27, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF10151C);
        graphics.drawString(font, title, MARGIN, 10, 0xFFF0E8D8, false);
        graphics.drawString(font, "Current progress and effects at each level", MARGIN, 23, 0xFFB9C8CF, false);

        if (!ClientMetabolicState.isReady()) {
            graphics.drawCenteredString(font, "Waiting for diet data…", width / 2, height / 2, 0xFFECE8E1);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        List<AspectIdentity> aspects = Arrays.asList(AspectIdentity.values());
        boolean twoColumns = width >= 650;
        int columns = twoColumns ? 2 : 1;
        int cardWidth = (width - MARGIN * 2 - GAP * (columns - 1)) / columns;
        int rows = (aspects.size() + columns - 1) / columns;
        int[] heights = new int[rows];
        for (int index = 0; index < aspects.size(); index++) {
            int row = index / columns;
            heights[row] = Math.max(heights[row], cardHeight(aspects.get(index), state, cardWidth));
        }
        int contentHeight = 0;
        for (int rowHeight : heights) contentHeight += rowHeight + GAP;
        contentHeight -= GAP;
        maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
        scroll = Math.min(scroll, maxScroll);

        graphics.enableScissor(0, TOP, width, height - BOTTOM);
        int y = TOP - scroll;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index < aspects.size() && y + heights[row] >= TOP && y < height - BOTTOM) {
                    drawCard(graphics, aspects.get(index), state, MARGIN + column * (cardWidth + GAP), y, cardWidth, heights[row]);
                }
            }
            y += heights[row] + GAP;
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            graphics.drawString(font, "Scroll for more", width - MARGIN - font.width("Scroll for more"), height - 24, 0xFFB9C8CF, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int cardHeight(AspectIdentity aspect, MetabolicSyncPacket state, int width) {
        int effectWidth = Math.max(60, width - 83);
        int result = 50;
        for (DietBenefits.GuideTier tier : DietBenefits.guide(aspect, state)) {
            result += Math.max(11, font.split(Component.literal(tier.effect()), effectWidth).size() * 10) + 2;
        }
        return result + 5;
    }

    private void drawCard(GuiGraphics graphics, AspectIdentity aspect, MetabolicSyncPacket state,
                          int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF202A33);
        graphics.fill(x, y, x + 3, y + height, 0xFF000000 | aspect.color);
        graphics.drawString(font, aspect.displayName, x + 9, y + 7, 0xFFF0E8D8, false);
        graphics.drawString(font, aspect.sources, x + 9, y + 19, 0xFFB9C8CF, false);
        float value = DietBenefits.value(state, aspect);
        String percent = Math.round(value * 100) + "%";
        graphics.drawString(font, percent, x + width - 9 - font.width(percent), y + 7, 0xFFF0E8D8, false);

        int barX = x + 9;
        int barY = y + 35;
        int barWidth = width - 18;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, 0xFF111820);
        graphics.fill(barX, barY, barX + Math.round(Math.max(0, Math.min(1, value)) * barWidth), barY + 5,
                0xFF000000 | aspect.color);
        for (DietBenefits.GuideTier tier : DietBenefits.guide(aspect, state)) {
            if (tier.value() < 0) continue;
            int marker = barX + Math.round(tier.value() * barWidth);
            graphics.fill(marker, barY - 2, marker + 1, barY + 7, 0xFFECE8E1);
        }

        int lineY = y + 47;
        int effectWidth = Math.max(60, width - 83);
        for (DietBenefits.GuideTier tier : DietBenefits.guide(aspect, state)) {
            int color = tier.current() ? 0xFFF4ECD4 : 0xFFB9C8CF;
            graphics.drawString(font, tier.threshold(), x + 9, lineY, tier.current() ? 0xFF9EE1AE : 0xFFB9C8CF, false);
            var lines = font.split(Component.literal(tier.effect()), effectWidth);
            for (int index = 0; index < lines.size(); index++) {
                graphics.drawString(font, lines.get(index), x + 74, lineY + index * 10, color, false);
            }
            lineY += Math.max(11, lines.size() * 10) + 2;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll == 0) return super.mouseScrolled(mouseX, mouseY, delta);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * 24));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (maxScroll > 0 && (keyCode == 264 || keyCode == 265 || keyCode == 266 || keyCode == 267)) {
            int distance = keyCode == 266 || keyCode == 267 ? height - TOP - BOTTOM : 24;
            scroll = Math.max(0, Math.min(maxScroll, scroll + (keyCode == 264 || keyCode == 267 ? distance : -distance)));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        if (fromInventory && minecraft.player != null) minecraft.setScreen(new InventoryScreen(minecraft.player));
        else minecraft.setScreen(null);
    }
}
