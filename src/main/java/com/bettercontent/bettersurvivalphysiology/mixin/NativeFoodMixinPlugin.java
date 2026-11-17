package com.bettercontent.bettersurvivalphysiology.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Optional KC nested Finish bridge; no alternate Cooking owner in the active pack. */
public final class NativeFoodMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String name) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!mixin.endsWith("NativeLunchBagFinishMixin")) return true;
        var loader = getClass().getClassLoader();
        return loader.getResource("com/github/ysbbbbbb/kaleidoscopecookery/item/TransmutationLunchBagItem.class") != null;
    }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
