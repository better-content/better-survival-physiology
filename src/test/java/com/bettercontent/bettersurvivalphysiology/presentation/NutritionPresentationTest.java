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
        assertEquals(5, AspectIdentity.values().length);
        assertEquals(AspectIdentity.POWER, AspectIdentity.fromGroupName("diet:PROTEINS"));
        assertEquals(AspectIdentity.MOTION, AspectIdentity.fromGroupName("Carbs"));
        assertEquals(AspectIdentity.ENDURANCE, AspectIdentity.fromGroupName("diet:spice"));
        assertEquals(AspectIdentity.RENEWAL, AspectIdentity.fromGroupName("vegetables"));
        assertEquals(AspectIdentity.CONTROL, AspectIdentity.fromGroupName("alcohol"));
        for (String retired : new String[]{"earth", "grains", "fruits", "dairy", "sugars"})
            assertNull(AspectIdentity.fromGroupName("diet:" + retired), "Retired source cannot masquerade as native group");
        assertEquals("Spice", AspectIdentity.ENDURANCE.displayName);
        assertArrayEquals(new int[]{0xFF4055, 0xE0B01F, 0x52606A, 0x6CCAF0, 0x8E5BB7},
                java.util.Arrays.stream(AspectIdentity.values()).mapToInt(aspect -> aspect.color).toArray());
        assertArrayEquals(new int[]{0, 3, 5, 4, 7},
                java.util.Arrays.stream(AspectIdentity.values()).mapToInt(aspect -> aspect.icon).toArray());
    }
}
