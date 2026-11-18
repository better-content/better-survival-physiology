package com.bettercontent.bettersurvivalphysiology.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class SpiceEffectsTest {
    @Test void pendingOwnersDeliverOnceEvenWhenWaterRegistersBeforeSpice() {
        ResourceLocation water = new ResourceLocation("better_drinking_water", "profile");
        ResourceLocation cooking = new ResourceLocation("better_cooking", "food");
        List<ResourceLocation> delivered = new ArrayList<>();
        AtomicInteger delegatedReserve = new AtomicInteger();
        SpiceEffects.registerExternalCompletionOwner(water, stack -> false);
        assertThrows(IllegalArgumentException.class,
                () -> SpiceEffects.registerExternalCompletionOwner(water, stack -> false));
        SpiceEffects.register(new ResourceLocation("better_spicy_farmers", "runtime"), new SpiceEffects.Provider() {
            public double resolveUnits(Player player, ItemStack stack) { return .5; }
            public double dietGain(double units) { return .2 * units; }
            public SpiceEffects.Terms terms(double level, double amp) {
                return new SpiceEffects.Terms(.4 * level, .2 * level, .5 * level * amp, .4, .2, 4, 8);
            }
            public void activateEndurance(net.minecraft.server.level.ServerPlayer player,
                    com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState state,
                    com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot nutrition) {
                delegatedReserve.incrementAndGet();
            }
            public boolean isConsumption(DamageSource source) { return false; }
            public void applyCompletedUnits(Player player, double units) { assertEquals(.5, units); }
            public void registerExternalCompletionOwner(ResourceLocation owner, Predicate<ItemStack> predicate) {
                delivered.add(owner);
            }
        });
        SpiceEffects.registerExternalCompletionOwner(cooking, stack -> false);
        assertEquals(List.of(water, cooking), delivered);
        assertEquals(.1, SpiceEffects.dietGain(.5), 1e-12);
        assertEquals(.2, SpiceEffects.terms(.5, 1).mining(), 1e-12);
        assertEquals(.25, SpiceEffects.terms(.5, 1).resourceSavings(), 1e-12);
        var presentState = new MetabolicState();
        SpiceEffects.activateEndurance(null, presentState, new NutritionSnapshot(0, 1, 1, 0, 0));
        assertEquals(1, delegatedReserve.get(), "Present provider alone owns activation");
        assertEquals(0, presentState.enduranceReserveTicks, "Core fallback cannot also activate");
        assertThrows(IllegalArgumentException.class, () -> SpiceEffects.dietGain(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> SpiceEffects.dietGain(-1));
        assertThrows(IllegalArgumentException.class, () -> new SpiceEffects.Terms(0, -1, 0, 0, 0, 0, 0));
        assertThrows(IllegalStateException.class,
                () -> SpiceEffects.register(new ResourceLocation("duplicate", "runtime"), nullProvider()));
    }
    @Test void absentProviderPreservesSavedSpiceReserveTriggerDurationAndCooldown() {
        float threshold = (float) SalienceConfig.FOURTH;
        var eligible = new NutritionSnapshot(0, 1, threshold, 0, 0);
        var below = new NutritionSnapshot(0, 1, Math.nextDown(threshold), 0, 0);
        assertFalse(SpiceEffects.activateStandaloneEndurance(new MetabolicState(), below, 2, false, false));
        assertFalse(SpiceEffects.activateStandaloneEndurance(new MetabolicState(), eligible, 3, false, false));
        var hungry = new MetabolicState();
        assertTrue(SpiceEffects.activateStandaloneEndurance(hungry, eligible, 2, false, false));
        assertEquals((int) (200 * MetabolicMath.amplification(eligible.carbs())), hungry.enduranceReserveTicks);
        assertEquals(2400, hungry.enduranceReserveCooldown);
        assertFalse(SpiceEffects.activateStandaloneEndurance(hungry, eligible, 2, false, false),
                "Only one trigger before cooldown expires");
        assertEquals(2400, MetabolicState.load(hungry.save()).enduranceReserveCooldown,
                "Historic reserve cooldown survives a save/load without Spice installed");
        hungry.tickTransient();
        assertEquals(2399, hungry.enduranceReserveCooldown);
        assertEquals((int) (200 * MetabolicMath.amplification(eligible.carbs())) - 1,
                hungry.enduranceReserveTicks);
        assertTrue(SpiceEffects.activateStandaloneEndurance(new MetabolicState(), eligible, 20, true, false));
        assertTrue(SpiceEffects.activateStandaloneEndurance(new MetabolicState(), eligible, 20, false, true));
    }

    private static SpiceEffects.Provider nullProvider() {
        return new SpiceEffects.Provider() {
            public double resolveUnits(Player player, ItemStack stack) { return 0; }
            public double dietGain(double units) { return 0; }
            public SpiceEffects.Terms terms(double level, double amp) { return SpiceEffects.Terms.EMPTY; }
            public void activateEndurance(net.minecraft.server.level.ServerPlayer player,
                    com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState state,
                    com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot nutrition) {}
            public boolean isConsumption(DamageSource source) { return false; }
            public void applyCompletedUnits(Player player, double units) {}
            public void registerExternalCompletionOwner(ResourceLocation owner, Predicate<ItemStack> manages) {}
        };
    }
}
