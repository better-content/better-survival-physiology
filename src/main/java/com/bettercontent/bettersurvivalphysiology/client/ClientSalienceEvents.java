package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity;
import com.bettercontent.bettersurvivalphysiology.presentation.DietBenefits;
import com.bettercontent.bettersurvivalphysiology.presentation.PresentationFlags;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSalienceEvents {
    private static final ResourceLocation ICONS = new ResourceLocation(SystemicSalienceMod.MOD_ID, "textures/gui/aspect_badges.png");
    private static final ResourceLocation ASPECT_FONT = new ResourceLocation(SystemicSalienceMod.MOD_ID, "aspects");

    private ClientSalienceEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) MealRecap.tick();
    }

    @SubscribeEvent
    public static void onHud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        List<Icon> active = activeIcons(state);
        int y = event.getWindow().getGuiScaledHeight() - 72;
        if (!active.isEmpty()) {
            int width = active.size() * 20 - 2;
            int x = (event.getWindow().getGuiScaledWidth() - width) / 2;
            for (Icon icon : active) {
                event.getGuiGraphics().blit(ICONS, x, y, icon.aspect.icon * 18, 0, 18, 18, 144, 18);
                if (icon.overlay != null) event.getGuiGraphics().drawString(minecraft.font, icon.overlay, x + 12, y + 10, 0xffffffff, true);
                x += 20;
            }
        }
        MealRecap.render(event.getGuiGraphics(), minecraft, y);
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
            String name = identity == AspectIdentity.TEMPO ? "Sugar" : identity == AspectIdentity.CONTROL ? "Alcohol"
                    : identity.representative.substring(0, 1).toUpperCase() + identity.representative.substring(1);
            line.append(Component.literal(name + " — ").withStyle(style -> style.withColor(identity.color)))
                    .append(Component.literal(identity.badge()).withStyle(style -> style.withFont(ASPECT_FONT)))
                    .append(Component.literal(" "))
                    .append(Component.literal(identity.glyph + " " + identity.displayName)
                            .withStyle(style -> style.withColor(identity.color)));
        }
        event.getToolTip().add(line);
        AspectIdentity first = identities.get(0);
        String promise = DietBenefits.preparedPromise(first);
        event.getToolTip().add(Component.literal(promise + (identities.size() > 1 ? " · See Diet for all effects" : ""))
                .withStyle(style -> style.withColor(0xD8D5CE)));
    }

    private static List<Icon> activeIcons(MetabolicSyncPacket state) {
        List<Icon> result = new ArrayList<>(); int flags = state.flags();
        if (PresentationFlags.has(flags, PresentationFlags.IMPACT_READY)) result.add(new Icon(AspectIdentity.IMPACT, null));
        if (state.workSequence() > 0) result.add(new Icon(AspectIdentity.WORK, Integer.toString(state.workSequence())));
        if (PresentationFlags.has(flags, PresentationFlags.MOBILITY_STRIDE)) result.add(new Icon(AspectIdentity.MOBILITY, null));
        if (PresentationFlags.has(flags, PresentationFlags.ENDURANCE_ACTIVE)) result.add(new Icon(AspectIdentity.ENDURANCE, "!"));
        else if (PresentationFlags.has(flags, PresentationFlags.ENDURANCE_READY)) result.add(new Icon(AspectIdentity.ENDURANCE, null));
        if (PresentationFlags.has(flags, PresentationFlags.ROBUSTNESS_READY)) result.add(new Icon(AspectIdentity.ROBUSTNESS, null));
        if (PresentationFlags.has(flags, PresentationFlags.RENEWAL_READY)) result.add(new Icon(AspectIdentity.RENEWAL, null));
        if (PresentationFlags.has(flags, PresentationFlags.TEMPO_TWO)) result.add(new Icon(AspectIdentity.TEMPO, "2"));
        else if (PresentationFlags.has(flags, PresentationFlags.TEMPO_ONE)) result.add(new Icon(AspectIdentity.TEMPO, "1"));
        else if (PresentationFlags.has(flags, PresentationFlags.TEMPO_CRASH)) result.add(new Icon(AspectIdentity.TEMPO, "↓"));
        if (PresentationFlags.has(flags, PresentationFlags.CONTROL_IMPAIRED)) result.add(new Icon(AspectIdentity.CONTROL, "!"));
        else if (PresentationFlags.has(flags, PresentationFlags.CONTROL_COMPOSED)) result.add(new Icon(AspectIdentity.CONTROL, null));
        result.sort(Comparator.comparingInt(icon -> icon.aspect.icon));
        return result;
    }

    private record Icon(AspectIdentity aspect, String overlay) {}
}
