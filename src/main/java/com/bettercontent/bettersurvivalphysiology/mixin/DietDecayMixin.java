package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scale Diet's native hunger-linked decay continuously with Nectar. */
@Mixin(targets = "com.illusivesoulworks.diet.common.capability.PlayerDietTracker", remap = false)
public abstract class DietDecayMixin {
    @Shadow @Final private Player player;

    @ModifyVariable(method = "decay", at = @At("STORE"), index = 6, require = 1)
    private float betterContent$sugarDecay(float nativeDecay) {
        if (!(player instanceof ServerPlayer server)) return nativeDecay;
        return (float) (nativeDecay * MetabolicMath.sugarDrain(MetabolicStateStore.get(server).sugar));
    }
}
