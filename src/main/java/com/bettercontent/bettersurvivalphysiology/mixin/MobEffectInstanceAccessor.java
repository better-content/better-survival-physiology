package com.bettercontent.bettersurvivalphysiology.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
    @Accessor("duration")
    int systemicSalience$getDuration();

    @Accessor("duration")
    void systemicSalience$setDuration(int duration);
}
