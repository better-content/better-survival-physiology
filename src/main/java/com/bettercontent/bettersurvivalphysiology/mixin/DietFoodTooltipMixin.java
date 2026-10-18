package com.bettercontent.bettersurvivalphysiology.mixin;

import com.illusivesoulworks.diet.client.DietClientEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** The horizontal, pack-named gain row replaces only Diet's native food tooltip. */
@Mixin(value = DietClientEvents.class, remap = false)
public abstract class DietFoodTooltipMixin {
    @Inject(method = "renderItemTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private static void better_survival_physiology$replaceFoodTooltip(Player player, ItemStack stack,
                                                                      List<Component> tooltip, CallbackInfo ci) {
        ci.cancel();
    }
}
