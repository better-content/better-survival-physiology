package com.bettercontent.bettersurvivalphysiology.compat;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.runtime.GameplayHooks;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.UUID;

public final class EpicFightCompat {
    private static final UUID STAMINA_COST_LISTENER = UUID.fromString("89c31fb6-7fe0-4a17-ae4c-3b7039e67f01");

    private EpicFightCompat() {}

    public static void register(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) return;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        patch.getEventListener().removeListener(PlayerEventListener.EventType.STAMINA_CONSUME_EVENT, STAMINA_COST_LISTENER);
        patch.getEventListener().addEventListener(PlayerEventListener.EventType.STAMINA_CONSUME_EVENT, STAMINA_COST_LISTENER, event -> {
            event.setAmount(event.getAmount() * GameplayHooks.staminaMultiplier(player));
        });
    }

    public static boolean restoreStamina(ServerPlayer player, double fraction) {
        if (!ModList.get().isLoaded("epicfight")) return false;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return false;
        float max = patch.getMaxStamina();
        patch.setStamina(Math.min(max, patch.getStamina() + (float) (max * fraction)));
        return true;
    }

    public static boolean restoreIfLow(ServerPlayer player, double fraction, double threshold) {
        if (!ModList.get().isLoaded("epicfight")) return false;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || patch.getMaxStamina() <= 0.0f || patch.getStamina() / patch.getMaxStamina() > threshold) return false;
        patch.setStamina(Math.min(patch.getMaxStamina(), patch.getStamina() + (float) (patch.getMaxStamina() * fraction)));
        return true;
    }

    public static boolean isStaminaBelow(ServerPlayer player, double threshold) {
        if (!ModList.get().isLoaded("epicfight")) return false;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        return patch != null && patch.getMaxStamina() > 0.0f && patch.getStamina() / patch.getMaxStamina() <= threshold;
    }

    public static void reinforceStunShield(ServerPlayer player, double positive) {
        if (!ModList.get().isLoaded("epicfight") || positive <= 0.0) return;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        patch.setStunShield(Math.min(patch.getMaxStunShield(), patch.getStunShield() + (float) (0.04 * positive)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void applyImpact(LivingEntity target, double amount) {
        tryImpact(target, amount);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean tryImpact(LivingEntity target, double amount) {
        if (!ModList.get().isLoaded("epicfight") || amount <= 0.0) return false;
        HurtableEntityPatch patch = EpicFightCapabilities.getEntityPatch(target, HurtableEntityPatch.class);
        return patch != null && patch.applyStun(StunType.SHORT, (float) amount);
    }

    public static void braceStunShield(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) return;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) patch.setStunShield(patch.getMaxStunShield());
    }

}
