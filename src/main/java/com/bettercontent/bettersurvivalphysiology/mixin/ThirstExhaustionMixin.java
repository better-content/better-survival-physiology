package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.runtime.GameplayHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keep Thirst Was Taken's normal exhaustion logic while scaling its incoming cost. */
@Pseudo
@Mixin(targets = "dev.ghen.thirst.content.thirst.PlayerThirst", remap = false)
abstract class ThirstExhaustionMixin {
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float systemicSalience$scaleThirstExhaustion(float amount, Player player) {
        return player instanceof ServerPlayer server ? amount * GameplayHooks.thirstMultiplier(server) : amount;
    }
}
