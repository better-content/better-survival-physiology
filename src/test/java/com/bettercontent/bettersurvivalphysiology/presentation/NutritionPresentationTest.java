package com.bettercontent.bettersurvivalphysiology.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NutritionPresentationTest {
    @Test
    void tiersUseTheServerThresholdContract() {
        assertEquals(NutritionTier.BUILDING, NutritionTier.of(.49, .50, .75, .90));
        assertEquals(NutritionTier.SUPPORTED, NutritionTier.of(.50, .50, .75, .90));
        assertEquals(NutritionTier.PREPARED, NutritionTier.of(.75, .50, .75, .90));
        assertEquals(NutritionTier.FEAST, NutritionTier.of(.90, .50, .75, .90));
    }

    @Test
    void upperBandCountdownReflectsTheActiveSugarLoad() {
        assertTrue(Math.abs(NutritionEstimates.nutrientSeconds(.95, .90, 0.0) - 100) <= 1);
        assertTrue(Math.abs(NutritionEstimates.nutrientSeconds(.95, .90, .70) - 20) <= 1);
        assertEquals(-1, NutritionEstimates.nutrientSeconds(.70, .50, 0.0));
    }

    @Test
    void aspectOrderAndDietIdentitiesAreExact() {
        assertEquals(8, AspectIdentity.values().length);
        assertEquals(AspectIdentity.IMPACT, AspectIdentity.fromGroupName("diet:PROTEINS"));
        assertEquals(AspectIdentity.RENEWAL, AspectIdentity.fromGroupName("Dairy"));
        assertArrayEquals(new int[]{0xFF4055, 0x00A985, 0xF0E2C5, 0xE0B01F, 0x52606A, 0xAF6A2F, 0x6CCAF0, 0x8E5BB7},
                java.util.Arrays.stream(AspectIdentity.values()).mapToInt(aspect -> aspect.color).toArray());
        for (int index = 0; index < AspectIdentity.values().length; index++) {
            assertEquals(index, AspectIdentity.values()[index].icon);
        }
    }
}
