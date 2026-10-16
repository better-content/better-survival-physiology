package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity;
import com.bettercontent.bettersurvivalphysiology.presentation.DietBenefits;
import com.bettercontent.bettersurvivalphysiology.presentation.PresentationFlags;
import com.bettercontent.bettersurvivalphysiology.presentation.NutritionHud;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSalienceEvents {
    private static final ResourceLocation ICONS = new ResourceLocation(SystemicSalienceMod.MOD_ID, "textures/gui/aspect_badges.png");
    private static final ResourceLocation ASPECT_FONT = new ResourceLocation(SystemicSalienceMod.MOD_ID, "aspects");

    private ClientSalienceEvents() {}

    @SubscribeEvent
    public static void onHud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null || !ClientMetabolicState.isReady()) return;
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        if (PresentationFlags.has(state.flags(), PresentationFlags.CONTROL_BLACKOUT)) {
            int width = event.getWindow().getGuiScaledWidth(), height = event.getWindow().getGuiScaledHeight();
            event.getGuiGraphics().fill(0, 0, width, height, 0xE0000000);
            event.getGuiGraphics().drawCenteredString(minecraft.font, "Draught blackout", width / 2, height / 2, 0xFFE8DCC4);
        }
        var entries = NutritionHud.entries(state);
        if (entries.isEmpty()) return;
        int cellWidth = Math.max(26, entries.stream().mapToInt(entry -> minecraft.font.width(entry.label()) + 6).max().orElse(26));
        int columns = Math.min(entries.size(), Math.max(1, (event.getWindow().getGuiScaledWidth() - 16) / cellWidth));
        int rows = (entries.size() + columns - 1) / columns;
        int top = event.getWindow().getGuiScaledHeight() - 72 - (rows - 1) * 30;
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            int row = index / columns;
            int rowSize = Math.min(columns, entries.size() - row * columns);
            int x = (event.getWindow().getGuiScaledWidth() - rowSize * cellWidth) / 2 + (index % columns) * cellWidth;
            int y = top + row * 30;
            event.getGuiGraphics().blit(ICONS, x + (cellWidth - 18) / 2, y, entry.aspect().icon * 18, 0, 18, 18, 144, 18);
            event.getGuiGraphics().drawString(minecraft.font, entry.label(), x + (cellWidth - minecraft.font.width(entry.label())) / 2, y + 19, 0xffffffff, true);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.isSameThread()) return;
        List<AspectIdentity> identities = new ArrayList<>();
        try {
            for (IDietGroup group : DietApi.getInstance().getGroups(minecraft.player, event.getItemStack())) {
                AspectIdentity identity = AspectIdentity.fromGroupName(group.getName());
                if (identity != null && !identities.contains(identity)) identities.add(identity);
            }
        } catch (RuntimeException ignored) { return; }
        if (ConsumableProfiles.sugar(event.getItemStack()) > 0 && !identities.contains(AspectIdentity.TEMPO)) identities.add(AspectIdentity.TEMPO);
        if (ConsumableProfiles.isAlcohol(event.getItemStack()) && !identities.contains(AspectIdentity.CONTROL)) identities.add(AspectIdentity.CONTROL);
        if (identities.isEmpty()) return;
        identities.sort(AspectIdentity.displayOrder());
        MutableComponent line = Component.literal("Nourishes: ").withStyle(style -> style.withColor(0x999999));
        for (int index = 0; index < identities.size(); index++) {
            AspectIdentity identity = identities.get(index);
            if (index > 0) line.append(Component.literal(" · ").withStyle(style -> style.withColor(0x777777)));
            line.append(Component.literal(identity.displayName + " — ").withStyle(style -> style.withColor(identity.color)))
                    .append(Component.literal(identity.badge()).withStyle(style -> style.withFont(ASPECT_FONT)))
                    .append(Component.literal(" " + identity.glyph).withStyle(style -> style.withColor(identity.color)));
        }
        event.getToolTip().add(line);
        AspectIdentity first = identities.get(0);
        String promise = DietBenefits.preparedPromise(first);
        event.getToolTip().add(Component.literal(promise + (identities.size() > 1 ? " · See Diet for all effects" : ""))
                .withStyle(style -> style.withColor(0xD8D5CE)));
    }

}
