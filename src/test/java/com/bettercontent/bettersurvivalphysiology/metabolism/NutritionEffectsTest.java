package com.bettercontent.bettersurvivalphysiology.metabolism;

import org.junit.jupiter.api.Test;
import java.util.function.DoubleBinaryOperator;
import static org.junit.jupiter.api.Assertions.*;

class NutritionEffectsTest {
    private static final DoubleBinaryOperator[] BENEFITS = {
            NutritionEffects::meleeDamage, NutritionEffects::miningSpeed,
            NutritionEffects::movementSpeed, NutritionEffects::resourceSavings,
            NutritionEffects::temperatureResistance, NutritionEffects::harmfulExpiryBonus
    };

    @Test
    void everyPrimaryStartsAtZeroAndScalesFromTheFirstTrace() {
        double[] caps = {.40, 1, .25, .50, .70, 1};
        for (int i = 0; i < BENEFITS.length; i++) {
            var effect = BENEFITS[i];
            assertEquals(0, effect.applyAsDouble(0, 0), 1e-12);
            assertEquals(0, effect.applyAsDouble(0, 1), 1e-12);
            assertEquals(caps[i] * .01, effect.applyAsDouble(.01, 0), 1e-12);
            assertEquals(caps[i] * .1, effect.applyAsDouble(.1, 0), 1e-12);
            assertEquals(caps[i] * .5, effect.applyAsDouble(.5, 0), 1e-12);
            assertEquals(caps[i], effect.applyAsDouble(1, 0), 1e-12);
            assertEquals(caps[i] * .2, effect.applyAsDouble(.1, 1), 1e-12);
            for (double threshold : new double[]{.2, .4, .6, .8}) {
                assertTrue(Math.abs(effect.applyAsDouble(threshold + 1e-7, 0)
                        - effect.applyAsDouble(threshold - 1e-7, 0)) < 1e-6);
            }
        }
    }

    @Test
    void nectarAmplifiesProtectionAndImpairmentButCannotCreateImmunity() {
        assertEquals(.8, NutritionEffects.meleeDamage(1, 1), 1e-12);
        assertEquals(2, NutritionEffects.miningSpeed(1, 1), 1e-12);
        assertEquals(.5, NutritionEffects.movementSpeed(1, 1), 1e-12);
        assertEquals(.85, NutritionEffects.resourceSavings(1, 1), 1e-12);
        assertEquals(.85, NutritionEffects.temperatureResistance(1, 1), 1e-12);
        assertEquals(2, NutritionEffects.harmfulExpiryBonus(1, 1), 1e-12);
        assertEquals(.3, MetabolicMath.alcoholReduction(1, 1), 1e-12);
        assertEquals(2 * MetabolicMath.alcoholReduction(.2, 0), MetabolicMath.alcoholReduction(.2, 1), 1e-12);
        assertEquals(2 * MetabolicMath.alcoholImpairment(.5, 0), MetabolicMath.alcoholImpairment(.5, 1), 1e-12);
        assertEquals(1, MetabolicMath.alcoholImpairment(1, 1), 1e-12);
    }

    @Test
    void outOfRangeNutritionCannotIncreasePrimaryCaps() {
        for (var effect : BENEFITS) {
            assertEquals(0, effect.applyAsDouble(-1, 1), 1e-12);
            assertEquals(effect.applyAsDouble(1, 1), effect.applyAsDouble(5, 5), 1e-12);
        }
    }

    @Test
    void fractionalCreamDoesNotRoundUpToOneExtraTickEveryTick() {
        var state = new MetabolicState();
        int removed = 0;
        for (int tick = 0; tick < 1000; tick++) {
            removed += state.renewalExtraTicks(.01);
        }
        assertEquals(10, removed);
    }
}
