package com.bettercontent.bettersurvivalphysiology.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NutritionPresentationTest {
    @Test
    void tiersUseFourProgressionMilestones() {
        assertEquals(NutritionTier.BUILDING, NutritionTier.of(.19));
        assertEquals(NutritionTier.FIRST, NutritionTier.of(.20));
        assertEquals(NutritionTier.SECOND, NutritionTier.of(.40));
        assertEquals(NutritionTier.THIRD, NutritionTier.of(.60));
        assertEquals(NutritionTier.FOURTH, NutritionTier.of(.80));
    }

    @Test
    void upperBandCountdownReflectsTheActiveSugarLoad() {
        assertTrue(NutritionEstimates.nutrientSeconds(.95, .80, .70)
                < NutritionEstimates.nutrientSeconds(.95, .80, 0.0));
        assertEquals(-1, NutritionEstimates.nutrientSeconds(.70, .40, 0.0));
    }

    @Test
    void aspectOrderAndDietIdentitiesAreExact() {
        assertEquals(8, AspectIdentity.values().length);
        assertEquals(AspectIdentity.IMPACT, AspectIdentity.fromGroupName("diet:PROTEINS"));
        assertEquals(AspectIdentity.RENEWAL, AspectIdentity.fromGroupName("Dairy"));
        assertEquals(AspectIdentity.ROBUSTNESS, AspectIdentity.fromGroupName("diet:spice"));
        assertNull(AspectIdentity.fromGroupName("diet:earth"), "Legacy Earth must not alias or migrate into Spice");
        assertNull(AspectIdentity.fromGroupName("vegetables"), "Unrelated former crop groups must not become Spice");
        assertEquals("Spice", AspectIdentity.ROBUSTNESS.displayName);
        assertArrayEquals(new int[]{0xFF4055, 0x00A985, 0xF0E2C5, 0xE0B01F, 0x52606A, 0xD85C32, 0x6CCAF0, 0x8E5BB7},
                java.util.Arrays.stream(AspectIdentity.values()).mapToInt(aspect -> aspect.color).toArray());
        for (int index = 0; index < AspectIdentity.values().length; index++) {
            assertEquals(index, AspectIdentity.values()[index].icon);
        }
    }
}
