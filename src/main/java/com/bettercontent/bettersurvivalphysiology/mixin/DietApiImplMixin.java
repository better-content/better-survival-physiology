package com.bettercontent.bettersurvivalphysiology.mixin;

import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.common.DietApiImpl;
import com.illusivesoulworks.diet.common.util.DietValueGenerator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/** Add generated ingredient ancestry even when Diet finds a direct tag first. */
@Mixin(value = DietApiImpl.class, remap = false)
public abstract class DietApiImplMixin {
    @Inject(method = "getGroups(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Ljava/util/Set;",
            at = @At("RETURN"), cancellable = true)
    private void betterContent$unionLineage(Player player, ItemStack stack, CallbackInfoReturnable<Set<IDietGroup>> cir) {
        Set<IDietGroup> result = new HashSet<>(cir.getReturnValue());
        DietValueGenerator.get(stack.getItem()).ifPresent(result::addAll);
        cir.setReturnValue(result);
    }
}
