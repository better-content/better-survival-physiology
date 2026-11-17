package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietAwardGuard;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietDairySplit;
import com.bettercontent.bettersurvivalphysiology.nutrition.NativeDietRoster;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.stream.Collectors;

/** Check Diet's actual client-side synced registry, suite and player tracker on load/reload.
 * The three native packets arrive separately; tolerate only a brief transient disagreement. */
@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class DietClientRosterGuard {
    private static String mismatch = "";
    private static long firstMismatchTick;
    private static Object lastLevel;
    private static long levelStartTick;

    private DietClientRosterGuard() {}

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            lastLevel = null;
            mismatch = "";
            return;
        }
        if (lastLevel != client.level) {
            lastLevel = client.level;
            mismatch = "";
            levelStartTick = client.level.getGameTime();
        }
        if (client.player.tickCount % 20 != 0) return;
        var suite = DietSuites.getSuite(client.level, "builtin");
        var tracker = DietBridge.tracker(client.player);
        if (!DietDairySplit.clientReady() || suite.isEmpty() || tracker.isEmpty() || tracker.get().getValues().isEmpty()
                || DietGroups.getGroups(client.level).isEmpty()) {
            if (client.level.getGameTime() - levelStartTick >= 200)
                throw new IllegalStateException("Diet config error: native client registry/suite/tracker sync incomplete");
            return;
        }
        Set<String> groups = DietGroups.getGroups(client.level).stream()
                .map(group -> group.getName()).collect(Collectors.toSet());
        Set<String> suiteGroups = suite.get().getGroups().stream()
                .map(group -> group.getName()).collect(Collectors.toSet());
        Set<String> trackerGroups = tracker.get().getValues().keySet();
        String current = (!groups.equals(NativeDietRoster.GROUPS)
                || !suiteGroups.equals(NativeDietRoster.GROUPS)
                || !trackerGroups.equals(NativeDietRoster.GROUPS)
                || !tracker.get().getSuite().equals("builtin"))
                ? "client groups=" + groups + ", suite=" + suiteGroups + ", tracker=" + trackerGroups
                    + ", suite id=" + tracker.get().getSuite() : "";
        if (current.isEmpty()) {
            mismatch = "";
            DietAwardGuard.validateRoster(groups, suiteGroups);
        } else if (!current.equals(mismatch)) {
            mismatch = current;
            firstMismatchTick = client.level.getGameTime();
        } else if (client.level.getGameTime() - firstMismatchTick >= 40) {
            throw new IllegalStateException("Diet config error after native client sync: " + current);
        }
    }
}
