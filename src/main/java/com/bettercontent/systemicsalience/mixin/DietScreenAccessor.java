package com.bettercontent.systemicsalience.mixin;

import com.illusivesoulworks.diet.client.screen.DietScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Keep Diet's inventory return behavior when replacing its screen. */
@Mixin(value = DietScreen.class, remap = false)
public interface DietScreenAccessor {
    @Accessor("fromInventory")
    boolean systemicSalience$fromInventory();
}
