package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.nutrition.DietDairySplit;
import com.illusivesoulworks.diet.api.type.IDietResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Pinned DietApiImpl: get(stack,int,float) delegates to get(list,int,float). Only the two
 * terminal calculation overloads transform, so a nested call cannot repartition twice. */
@Mixin(targets = "com.illusivesoulworks.diet.common.DietApiImpl", remap = false)
public abstract class DietNativeDairyMixin {
    @Inject(method = "get(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lcom/illusivesoulworks/diet/api/type/IDietResult;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private void betterContent$singleDairy(Player player, ItemStack stack, CallbackInfoReturnable<IDietResult> cir) {
        cir.setReturnValue(DietDairySplit.repartition(player, List.of(stack), cir.getReturnValue()));
    }

    @Inject(method = "get(Lnet/minecraft/world/entity/player/Player;Ljava/util/List;IF)Lcom/illusivesoulworks/diet/api/type/IDietResult;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private void betterContent$compositeDairy(Player player, List<ItemStack> stacks, int nutrition, float saturation,
                                               CallbackInfoReturnable<IDietResult> cir) {
        cir.setReturnValue(DietDairySplit.repartition(player, stacks, cir.getReturnValue()));
    }
}
