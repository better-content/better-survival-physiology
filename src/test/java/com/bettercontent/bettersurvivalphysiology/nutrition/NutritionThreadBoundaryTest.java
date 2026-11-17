package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.api.event.NutritionEpisodeEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NutritionThreadBoundaryTest {
    private static final double ORDINARY = 0.50;

    @Test
    void anyLowNourishingGroupMakesNutritionLow() {
        NutritionSnapshot lowVegetables = new NutritionSnapshot(.75f, .75f, .75f, .49f, 1f);
        assertTrue(NutritionThreadBoundary.isNutritionLow(lowVegetables, ORDINARY));
        assertFalse(NutritionThreadBoundary.isBalanced(lowVegetables, ORDINARY));
    }

    @Test
    void allFourNourishingGroupsAtThresholdQualifyWithZeroAlcohol() {
        NutritionSnapshot boundary = new NutritionSnapshot(.50f, .50f, .50f, .50f, 0);
        assertFalse(NutritionThreadBoundary.isNutritionLow(boundary, ORDINARY));
        assertTrue(NutritionThreadBoundary.isBalanced(boundary, ORDINARY));
        assertTrue(NutritionThreadBoundary.isBalanced(new NutritionSnapshot(.50f, .50f, .50f, .50f, 1f), ORDINARY));
    }

    @Test
    void recoveryCrossesFromLowToFourBalancedWithoutRequiringAlcohol() {
        NutritionSnapshot low = new NutritionSnapshot(.75f, .75f, .75f, .49f, 0);
        NutritionSnapshot balanced = new NutritionSnapshot(.75f, .75f, .75f, .50f, 0);
        assertTrue(NutritionThreadBoundary.isQualifyingRecovery(low, balanced, ORDINARY));
        assertFalse(NutritionThreadBoundary.isQualifyingRecovery(balanced, balanced, ORDINARY));
        assertFalse(NutritionThreadBoundary.isQualifyingRecovery(low, low, ORDINARY));
        assertFalse(NutritionThreadBoundary.isQualifyingRecovery(balanced,
                new NutritionSnapshot(.75f, .75f, .75f, .50f, 1f), ORDINARY));
    }

    @Test
    void providerEventExposesBothDomainBoundaries() {
        assertTrue(java.util.Set.of(NutritionEpisodeEvent.Kind.values()).containsAll(
                java.util.Set.of(NutritionEpisodeEvent.Kind.WARNING, NutritionEpisodeEvent.Kind.RECOVERED)));
    }
}
