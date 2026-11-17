package com.bettercontent.bettersurvivalphysiology.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spice is a paid dose, not an ordinary ingredient; Alcohol uses actual paid potency,
 * not native food hunger. Keep the source tags available for classification, but prevent
 * Diet's recipe generator and ordinary finish award from fabricating those contributions. */
@Mixin(targets = "com.illusivesoulworks.diet.common.data.group.DietGroup", remap = false)
public abstract class DietPaidGroupMixin {
    @Shadow @Final private String name;

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContent$paidOnly(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if ("spice".equals(name) || "alcohol".equals(name)) cir.setReturnValue(false);
    }
}
