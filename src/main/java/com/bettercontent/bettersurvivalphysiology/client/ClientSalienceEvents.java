package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity;
import com.bettercontent.bettersurvivalphysiology.presentation.AspectBadge;
import com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge;
import com.bettercontent.bettersurvivalphysiology.presentation.FoodNutrients;
import com.bettercontent.bettersurvivalphysiology.presentation.PresentationFlags;
import com.bettercontent.bettersurvivalphysiology.presentation.NutritionHud;
import com.illusivesoulworks.diet.api.DietApi;
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

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSalienceEvents {
    private static final ResourceLocation ICONS = new ResourceLocation(SystemicSalienceMod.MOD_ID, "textures/gui/aspect_badges.png");
    private static final ResourceLocation SPICE_ICON = new ResourceLocation("better_spicy_farmers", "textures/gui/spice_badge.png");
    private static final ResourceLocation OWN_SPICE_ICON = new ResourceLocation(SystemicSalienceMod.MOD_ID, "textures/gui/spice_badge.png");
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
            event.getGuiGraphics().drawCenteredString(minecraft.font, AspectIdentity.CONTROL.displayName + " blackout", width / 2, height / 2, 0xFFE8DCC4);
        }
        var entries = NutritionHud.entries(state);
        if (entries.isEmpty()) return;
        int cellWidth = Math.max(26, entries.stream().mapToInt(entry -> minecraft.font.width(entry.label()) + 6).max().orElse(26));
        int columns = Math.min(entries.size(), Math.max(1, (event.getWindow().getGuiScaledWidth() - 16) / cellWidth));
        int rows = (entries.size() + columns - 1) / columns;
        int top = event.getWindow().getGuiScaledHeight() - 72 - (rows - 1) * 30;
        boolean spiceAvailable = SpiceBridge.available();
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            int row = index / columns;
            int rowSize = Math.min(columns, entries.size() - row * columns);
            int x = (event.getWindow().getGuiScaledWidth() - rowSize * cellWidth) / 2 + (index % columns) * cellWidth;
            int y = top + row * 30;
            var badge = AspectBadge.of(entry.aspect(), spiceAvailable);
            ResourceLocation texture = entry.aspect() == AspectIdentity.ROBUSTNESS
                    ? (spiceAvailable ? SPICE_ICON : OWN_SPICE_ICON) : ICONS;
            event.getGuiGraphics().blit(texture, x + (cellWidth - badge.width()) / 2, y,
                    badge.u(), badge.v(), badge.width(), badge.height(), badge.textureWidth(), badge.textureHeight());
            event.getGuiGraphics().drawString(minecraft.font, entry.label(), x + (cellWidth - minecraft.font.width(entry.label())) / 2, y + 19, 0xffffffff, true);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.isSameThread()) return;
        Map<String, Double> gains = new HashMap<>();
        if (com.bettercontent.bettersurvivalphysiology.compat.BeverageAvailability.manages(event.getItemStack())) {
            gains.putAll(com.bettercontent.bettersurvivalphysiology.compat.BeverageCompat.plantGains(event.getItemStack()));
        } else {
            try {
                DietApi.getInstance().get(minecraft.player, event.getItemStack()).get()
                        .forEach((group, gain) -> gains.put(group.getName(), gain.doubleValue()));
            } catch (RuntimeException ignored) { return; }
        }
        var entries = FoodNutrients.entries(gains, ConsumableProfiles.servingSugar(event.getItemStack(), gains),
                ConsumableProfiles.alcohol(event.getItemStack()), event.getItemStack().is(net.minecraft.world.item.Items.MILK_BUCKET));
        if (entries.isEmpty()) return;
        MutableComponent line = Component.empty();
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            AspectIdentity identity = entry.aspect();
            if (index > 0) line.append(Component.literal(" · ").withStyle(style -> style.withColor(0x777777)));
            line.append(Component.literal(identity.badge()).withStyle(style -> style.withFont(ASPECT_FONT)))
                    .append(Component.literal(" " + identity.displayName).withStyle(style -> style.withColor(identity.color)))
                    .append(Component.literal(" " + entry.percentage()).withStyle(style -> style.withColor(0xD8D5CE)));
        }
        // The oversized badge font extends above its row; reserve a full line of clearance.
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(line);
    }

}
