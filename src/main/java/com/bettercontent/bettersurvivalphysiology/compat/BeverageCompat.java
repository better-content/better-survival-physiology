package com.bettercontent.bettersurvivalphysiology.compat;

import com.bettercontent.betterdrinkingwater.api.BeverageEvents;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.BeverageNutrition;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Loaded only when Better Drinking Water is present. Never runs native whole-item completion. */
public final class BeverageCompat {
    public static final String DIET = "diet";
    public static final String SUGAR = "sugar";
    public static final String ALCOHOL = "alcohol";
    public static final String EDIBLE = "edible";
    public static final String ITEM = "item";

    private BeverageCompat() {}

    public static void register() { MinecraftForge.EVENT_BUS.register(BeverageCompat.class); }

    @SubscribeEvent
    public static void profile(BeverageEvents.Profile event) {
        var stack = event.stack();
        CompoundTag gains = new CompoundTag();
        event.nutrition().putDouble(SUGAR, 0.0);
        if (stack.is(Items.MILK_BUCKET)) {
            // Explicit physiology award, not an additional native Diet award.
            gains.putDouble("dairy", .06);
            gains.putDouble("fats", .06);
        } else {
            var resolved = ConsumableProfiles.dietGains(event.player(), stack);
            resolved.ifPresent(serving -> {
                for (NutritionSnapshot.Group ordinary : NutritionSnapshot.Group.values()) {
                    if (serving.containsKey(ordinary.id))
                        gains.putDouble(ordinary.id, BeverageNutrition.boundedGain(serving.get(ordinary.id)));
                }
            });
            // Capture once before payment; paid profiles keep this entitlement across saves/sips.
            event.nutrition().putDouble(SUGAR, resolved.map(serving ->
                    BeverageNutrition.boundedGain(ConsumableProfiles.servingSugar(stack, serving))).orElse(0.0));
        }
        event.nutrition().put(DIET, gains);
        event.nutrition().putDouble(ALCOHOL, BeverageNutrition.boundedGain(ConsumableProfiles.alcohol(stack)));
        event.nutrition().putBoolean(EDIBLE, stack.isEdible());
        event.nutrition().putString(ITEM, net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString());
    }

    @SubscribeEvent
    public static void check(BeverageEvents.Check event) {
        if (MetabolicStateStore.get(event.player()).blackout) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void sip(BeverageEvents.Sip event) {
        if (BeverageNutrition.fraction(event.from(), event.to()) == 0.0) return;
        var player = event.player();
        var state = MetabolicStateStore.get(player);
        if (state.blackout) return; // Defensive: engine checks before purchase and paid release too.
        var before = DietBridge.snapshot(player);
        double sugarBefore = state.sugar, alcoholBefore = state.alcohol;
        CompoundTag nutrition = event.nutrition();
        CompoundTag gains = nutrition.getCompound(DIET);
        DietBridge.tracker(player).ifPresent(diet -> {
            if (!diet.isActive()) return;
            for (NutritionSnapshot.Group group : NutritionSnapshot.Group.values()) {
                double amount = BeverageNutrition.award(gains.getDouble(group.id), event.from(), event.to());
                if (amount > 0.0 && diet.getValues().containsKey(group.id))
                    diet.setValue(group.id, (float) Math.min(1.0, diet.getValue(group.id) + amount));
            }
            if (event.to() == 1.0 && !gains.isEmpty()) {
                var id = net.minecraft.resources.ResourceLocation.tryParse(nutrition.getString(ITEM));
                if (id != null) net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id)
                        .ifPresent(diet::addEaten);
            }
            diet.sync();
        });
        state.addSugar(BeverageNutrition.award(nutrition.getDouble(SUGAR), event.from(), event.to()));
        state.addAlcohol(BeverageNutrition.award(nutrition.getDouble(ALCOHOL), event.from(), event.to()));
        if (state.blackout) {
            state.blackoutX = player.getX(); state.blackoutY = player.getY(); state.blackoutZ = player.getZ();
        }
        DietBridge.syncMetabolic(player, state);
        SalienceEvents.beverageFeedback(player, before, sugarBefore, alcoholBefore,
                nutrition.getBoolean(EDIBLE), event.to() == 1.0);
        MetabolicStateStore.save(player);
    }
}
