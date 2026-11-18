package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.nutrition.DietDairySplit;
import com.illusivesoulworks.diet.api.type.IDietResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import java.util.List;

/** Pinned DietApiImpl: get(stack,int,float) delegates to get(list,int,float). Only the two
 * terminal calculation overloads transform, so a nested call cannot repartition twice. */
@Mixin(targets = "com.illusivesoulworks.diet.common.DietApiImpl", remap = false)
public abstract class DietNativeDairyMixin {
    @ModifyReturnValue(method = "get(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lcom/illusivesoulworks/diet/api/type/IDietResult;",
            at = @At("RETURN"), remap = false)
    private IDietResult betterContent$singleDairy(IDietResult original, Player player, ItemStack stack) {
        return DietDairySplit.repartition(player, List.of(stack), original);
    }

    @ModifyReturnValue(method = "get(Lnet/minecraft/world/entity/player/Player;Ljava/util/List;IF)Lcom/illusivesoulworks/diet/api/type/IDietResult;",
            at = @At("RETURN"), remap = false)
    private IDietResult betterContent$compositeDairy(IDietResult original, Player player, List<ItemStack> stacks,
                                                      int nutrition, float saturation) {
        return DietDairySplit.repartition(player, stacks, original);
    }
}
