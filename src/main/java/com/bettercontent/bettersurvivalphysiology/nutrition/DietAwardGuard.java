package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.DietEvent;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

/** Diet's data define nutrition; never let a group missing from its active suite reach native apply(). */
@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID)
public final class DietAwardGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> EXPECTED = Set.of(
            "proteins", "sugars", "grains", "fruits", "vegetables", "spice", "dairy", "alcohol");
    private DietAwardGuard() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) { validate(event.getServer()); }

    @SubscribeEvent
    public static void reloaded(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) validate(event.getPlayerList().getServer());
    }

    private static void validate(MinecraftServer server) {
        var level = server.overworld();
        var suite = DietSuites.getSuite(level, "builtin");
        var defined = DietGroups.getGroups(level).stream().map(g -> g.getName()).collect(Collectors.toSet());
        var active = suite.map(s -> s.getGroups().stream().map(g -> g.getName()).collect(Collectors.toSet())).orElse(Set.of());
        if (!defined.equals(EXPECTED) || !active.equals(EXPECTED))
            LOGGER.error("Diet roster mismatch: defined={}, active={}, expected={}; unmatched food awards are blocked. Install matching Diet/Physiology pack resources before play.", defined, active, EXPECTED);
        var inactive = DietGroups.getGroups(level).stream().filter(g -> !active.contains(g.getName())).toList();
        if (!inactive.isEmpty()) {
            var examples = BuiltInRegistries.ITEM.stream().filter(item -> new net.minecraft.world.item.ItemStack(item).isEdible())
                    .filter(item -> inactive.stream().anyMatch(g -> g.contains(new net.minecraft.world.item.ItemStack(item))))
                    .limit(20).map(BuiltInRegistries.ITEM::getKey).toList();
            LOGGER.error("Diet groups outside active suite can classify edible items (first 20): {}", examples);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventUnsupportedAward(DietEvent.ConsumeItemStack event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) return;
        var diet = DietBridge.tracker(player);
        if (diet.isEmpty() || !diet.get().isActive()) return;
        try {
            var result = DietApi.getInstance().get(player, event.getStack()).get();
            var available = diet.get().getValues().keySet();
            var unknown = result.keySet().stream().map(g -> g.getName())
                    .filter(name -> !available.contains(name)).collect(Collectors.toSet());
            if (unknown.isEmpty()) return;
            LOGGER.error("Blocked Diet award for {}: unknown groups {} are not in active suite {}. Correct the group data; do not silently drop an ingredient.",
                    BuiltInRegistries.ITEM.getKey(event.getStack().getItem()), unknown, available);
        } catch (RuntimeException failure) {
            LOGGER.error("Blocked unresolved Diet award for {}", BuiltInRegistries.ITEM.getKey(event.getStack().getItem()), failure);
        }
        event.setCanceled(true);
    }
}
