package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.api.event.NutritionEpisodeEvent;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

/** Authoritative server-side Diet transitions exposed as optional learning-surface evidence. */
public final class NutritionThreadBoundary {
    private NutritionThreadBoundary() {}

    /** Observes the post-decay Diet snapshot; called once per authoritative server second. */
    public static void onAuthoritativeTick(ServerPlayer player, NutritionSnapshot nutrition, double ordinaryThreshold) {
        if (player.getFoodData().getFoodLevel() < 20 || !isNutritionLow(nutrition, ordinaryThreshold)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (state.nutritionThreadToken.isBlank()) {
            state.nutritionThreadToken = player.getUUID() + ":nutrition:" + player.server.getTickCount();
            state.nutritionWarningPublished = false;
        }
        if (!state.nutritionWarningPublished) {
            MinecraftForge.EVENT_BUS.post(new NutritionEpisodeEvent(
                    player, NutritionEpisodeEvent.Kind.WARNING, nutrition, nutrition, state.nutritionThreadToken));
            state.nutritionWarningPublished = true;
            MetabolicStateStore.save(player);
        }
    }

    /** Completes only when a finished edible use moves the settled Diet snapshot into balance. */
    public static void onMealSettled(ServerPlayer player, NutritionSnapshot before,
                                     NutritionSnapshot after, double ordinaryThreshold) {
        if (!isQualifyingRecovery(before, after, ordinaryThreshold)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        String token = state.nutritionThreadToken;
        if (token.isBlank()) return;
        MinecraftForge.EVENT_BUS.post(new NutritionEpisodeEvent(
                player, NutritionEpisodeEvent.Kind.RECOVERED, before, after, token));
        state.nutritionThreadToken = "";
        state.nutritionWarningPublished = false;
        MetabolicStateStore.save(player);
    }

    public static boolean isNutritionLow(NutritionSnapshot nutrition, double ordinaryThreshold) {
        for (NutritionSnapshot.Group group : NutritionSnapshot.Group.values()) {
            if (nutrition.actual(group) < ordinaryThreshold) return true;
        }
        return false;
    }

    public static boolean isBalanced(NutritionSnapshot nutrition, double ordinaryThreshold) {
        return !isNutritionLow(nutrition, ordinaryThreshold);
    }

    public static boolean isQualifyingRecovery(NutritionSnapshot before, NutritionSnapshot after,
                                               double ordinaryThreshold) {
        return isNutritionLow(before, ordinaryThreshold) && isBalanced(after, ordinaryThreshold);
    }
}
