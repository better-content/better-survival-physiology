package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetabolicEstimatesTest {
    @Test
    void estimatesMatchNativeTrackerClockMathWithChangingCarbs() {
        for (double initialCarbs : new double[]{.25, .6, 1.0}) {
            double carbs = initialCarbs, alcohol = .8;
            int carbSeconds = NutritionEstimates.sugarSeconds(initialCarbs);
            int alcoholSeconds = NutritionEstimates.alcoholSeconds(alcohol, initialCarbs);
            int observedCarbs = -1, observedAlcohol = -1;
            for (int second = 1; second <= Math.max(carbSeconds, alcoholSeconds); second++) {
                for (int tick = 0; tick < 20; tick++) {
                    double nextAlcohol = MetabolicMath.tickAlcohol(alcohol, carbs);
                    carbs = MetabolicMath.tickSugar(carbs);
                    alcohol = nextAlcohol;
                }
                if (observedCarbs < 0 && carbs < initialCarbs * .5) observedCarbs = second;
                if (observedAlcohol < 0 && alcohol < .5) observedAlcohol = second;
            }
            assertEquals(carbSeconds, observedCarbs);
            assertEquals(alcoholSeconds, observedAlcohol);
        }
        assertTrue(NutritionEstimates.alcoholSeconds(.8, 1) < NutritionEstimates.alcoholSeconds(.8, 0));
        assertEquals(-1, NutritionEstimates.sugarSeconds(0));
        assertEquals(-1, NutritionEstimates.alcoholSeconds(0, 1));
    }
}
