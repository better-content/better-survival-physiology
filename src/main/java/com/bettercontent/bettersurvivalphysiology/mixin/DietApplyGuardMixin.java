package com.bettercontent.bettersurvivalphysiology.mixin;

import com.illusivesoulworks.diet.api.type.IDietResult;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.stream.Collectors;

/** Covers both single-item and composite Diet consume() paths before native apply() dereferences values. */
@Mixin(targets = "com.illusivesoulworks.diet.common.capability.PlayerDietTracker", remap = false)
public abstract class DietApplyGuardMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow @Final private Map<String, Float> values;

    @Inject(method = "apply(Lcom/illusivesoulworks/diet/api/type/IDietResult;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContent$rejectMissingGroups(IDietResult result, CallbackInfo ci) {
        var unknown = result.get().keySet().stream().map(group -> group.getName())
                .filter(group -> !values.containsKey(group)).collect(Collectors.toSet());
        if (unknown.isEmpty()) return;
        LOGGER.error("Blocked native Diet award: groups {} are missing from the active suite {}. Repair pack Diet resources before playing.", unknown, values.keySet());
        ci.cancel();
    }
}
