package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.runtime.GameplayHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void systemicSalience$scaleActionExhaustion(float amount, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        float multiplier = GameplayHooks.exhaustionMultiplier(player);
        if (multiplier == 1.0f) return;
        if (multiplier > 0.0f) player.getFoodData().addExhaustion(amount * multiplier);
        ci.cancel();
    }
}
