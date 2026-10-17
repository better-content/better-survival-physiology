package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetabolicEstimatesTest {
    @Test
    void estimatesMatchRuntimeWithChangingSugar() {
        for (double initialSugar : new double[]{.25, .6, 1.0}) {
            MetabolicState state = new MetabolicState();
            state.sugar = initialSugar;
            state.alcohol = .8;
            int sugarSeconds = NutritionEstimates.sugarSeconds(initialSugar);
            int alcoholSeconds = NutritionEstimates.alcoholSeconds(.8, initialSugar);
            int observedSugar = -1;
            int observedAlcohol = -1;
            for (int second = 1; second <= Math.max(sugarSeconds, alcoholSeconds); second++) {
                for (int tick = 0; tick < 20; tick++) state.tickTransient();
                if (observedSugar < 0 && state.sugar < initialSugar * .5) observedSugar = second;
                if (observedAlcohol < 0 && state.alcohol < .5) observedAlcohol = second;
            }
            assertEquals(sugarSeconds, observedSugar);
            assertEquals(alcoholSeconds, observedAlcohol);
        }
        assertTrue(NutritionEstimates.alcoholSeconds(.8, 1) < NutritionEstimates.alcoholSeconds(.8, 0));
        assertEquals(-1, NutritionEstimates.sugarSeconds(0));
        assertEquals(-1, NutritionEstimates.alcoholSeconds(0, 1));
    }
}
