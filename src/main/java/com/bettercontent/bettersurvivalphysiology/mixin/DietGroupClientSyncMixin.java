package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.nutrition.DietDairySplit;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native group NBT sync precedes our missing native-derived trail bitset on login/reload.
 * Invalidate stale client classification at the first packet, not on the server loader. */
@Mixin(targets = "com.illusivesoulworks.diet.common.data.group.DietGroups", remap = false)
public abstract class DietGroupClientSyncMixin {
    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"), remap = false)
    private void betterContent$beginNativeClientReload(CompoundTag data, CallbackInfo ci) {
        if ((Object) this == DietGroups.CLIENT) DietDairySplit.clearClient();
    }
}
