package com.bettercontent.bettersurvivalphysiology.mixin;

import com.bettercontent.bettersurvivalphysiology.nutrition.NestedServingDecision;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native lunch bag extracts ONE actual inner item before consumeOne; its direct
 * finishUsingItem bypasses Forge's usual Finish callback. Replay only that inner
 * completion, never the bag. Native effects/hunger and returned container stay intact. */
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.TransmutationLunchBagItem", remap = false)
public abstract class NativeLunchBagFinishMixin {
    @Redirect(method = "consumeOne", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;", remap = true),
            require = 1, remap = false)
    private static ItemStack salience$innerFinish(ItemStack paid, Level level, LivingEntity actor) {
        ItemStack original = paid.copy();
        ItemStack result = paid.finishUsingItem(level, actor);
        boolean real = actor instanceof ServerPlayer player && !(player instanceof FakePlayer)
                && player.connection != null && !player.hasDisconnected();
        if (NestedServingDecision.notifyNativeFinish(!level.isClientSide(), real,
                !ItemStack.matches(original, result)))
            return ForgeEventFactory.onItemUseFinish(actor, original, 0, result);
        return result;
    }
}
