package com.bettercontent.bettersurvivalphysiology.api;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.Objects;

/** Provider-initiated, pure beverage classification. Core never loads Water classes or
 * guesses a completed payment from a preview item. Water registers during its mod setup. */
public final class BeverageProfiles {
    public interface Provider {
        boolean manages(ItemStack stack);
        boolean completedExternally(ItemStack stack);
        Map<String, Double> plantGains(ItemStack stack);
        /** Existing synchronized Physiology config keys are a compatibility source;
         * Water is the only runtime interpreter of its alcohol item profiles. */
        double alcoholPotency(String itemId);
        double alcoholReduction(double alcohol, double carbs);
        double alcoholImpairment(double alcohol, double carbs);
        double tickAlcohol(double alcohol, double carbs);
        void updateBlackout(ServerPlayer player, MetabolicState state, float load);
        void clearBlackout(MetabolicState state, float load);
        void applyNativeAlcohol(ServerPlayer player, ItemStack consumed, MetabolicState state);
        void suppressBrewing(ServerPlayer player);
    }
    private static volatile Provider provider;
    private BeverageProfiles() {}

    public static synchronized void register(ResourceLocation owner, Provider value) {
        Objects.requireNonNull(owner); Objects.requireNonNull(value);
        if (provider != null) throw new IllegalStateException("Beverage authority already registered");
        provider = value;
    }
    public static boolean available() { return provider != null; }
    public static boolean manages(ItemStack stack) {
        var current = provider;
        return current != null && current.manages(stack);
    }
    public static boolean completedExternally(ItemStack stack) {
        var current = provider;
        return current != null && current.completedExternally(stack);
    }
    public static Map<String, Double> plantGains(ItemStack stack) {
        var current = provider;
        if (current == null || !current.manages(stack)) return Map.of();
        return Map.copyOf(current.plantGains(stack));
    }
    public static double plantSugar(ItemStack stack) {
        return plantGains(stack).getOrDefault("carbs", 0.0);
    }
    public static double alcoholPotency(String itemId) {
        var current = provider;
        // Preserve standalone core's existing minimum config-backed defaults.
        return current == null
                ? com.bettercontent.bettersurvivalphysiology.config.SalienceConfig.alcoholPotencies()
                        .getOrDefault(itemId, 0.0)
                : current.alcoholPotency(itemId);
    }
    private static Provider required() {
        var current = provider;
        if (current == null) throw new IllegalStateException("No Water alcohol authority");
        return current;
    }
    public static double alcoholReduction(double alcohol, double carbs) {
        return required().alcoholReduction(alcohol, carbs);
    }
    public static double alcoholImpairment(double alcohol, double carbs) {
        return required().alcoholImpairment(alcohol, carbs);
    }
    public static double tickAlcohol(double alcohol, double carbs) {
        return required().tickAlcohol(alcohol, carbs);
    }
    public static void updateBlackout(ServerPlayer player, MetabolicState state, float load) {
        required().updateBlackout(player, state, load);
    }
    public static void clearBlackout(MetabolicState state, float load) {
        required().clearBlackout(state, load);
    }
    public static void applyNativeAlcohol(ServerPlayer player, ItemStack consumed, MetabolicState state) {
        required().applyNativeAlcohol(player, consumed, state);
    }
    public static void suppressBrewing(ServerPlayer player) { required().suppressBrewing(player); }
}
