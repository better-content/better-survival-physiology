package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.nutrition.NativeDietRoster;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/** Pinned Diet 4813904: apply(Map, ResourceManager, ProfilerFiller) iterates diet resources,
 * then non-diet resources; filtering the argument avoids both passes and reload resurrection. */
@Mixin(targets = "com.illusivesoulworks.diet.common.data.group.DietGroups", remap = false)
public abstract class DietGroupLoaderMixin {
    @ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private Map<ResourceLocation, JsonElement> betterContent$excludeRetired(Map<ResourceLocation, JsonElement> resources) {
        return NativeDietRoster.excludeRetired(resources);
    }
}
