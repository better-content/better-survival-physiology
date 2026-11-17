package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.nutrition.NativeDietRoster;
import com.illusivesoulworks.diet.api.type.IDietResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.stream.Collectors;

/** Both native single-item and composite consume() reach this one final apply() boundary.
 * Missing or unrecognized groups are a hard configuration failure, not silent omission. */
@Mixin(targets = "com.illusivesoulworks.diet.common.capability.PlayerDietTracker", remap = false)
public abstract class DietApplyGuardMixin {
    @Shadow @Final private Map<String, Float> values;

    @Inject(method = "apply(Lcom/illusivesoulworks/diet/api/type/IDietResult;)V", at = @At("HEAD"), remap = false)
    private void betterContent$rejectMissingGroups(IDietResult result, CallbackInfo ci) {
        var unknown = result.get().keySet().stream().map(group -> group.getName())
                .filter(group -> !NativeDietRoster.GROUPS.contains(group) || !values.containsKey(group))
                .collect(Collectors.toSet());
        if (!unknown.isEmpty() || !values.keySet().equals(NativeDietRoster.GROUPS))
            throw new IllegalStateException("Diet config error: native result groups " + unknown
                    + " cannot be committed to tracker " + values.keySet());
    }
}
