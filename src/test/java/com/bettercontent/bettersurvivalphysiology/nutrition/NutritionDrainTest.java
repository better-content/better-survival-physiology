package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NutritionDrainTest {
    @Test
    void upperBandLastsAboutFiveIdleMinutesButSugarShortensIt() {
        float plain = .90f, tempoOne = .90f, tempoTwo = .90f;
        for (int second = 0; second < 302; second++) {
            plain = NutritionDrain.next(plain, .75, 0.0);
            tempoOne = NutritionDrain.next(tempoOne, .75, .30);
            tempoTwo = NutritionDrain.next(tempoTwo, .75, .70);
            if (second == 60) assertTrue(tempoTwo < .75f);
            if (second == 150) assertTrue(tempoOne < .75f);
        }
        assertTrue(plain < .75f);
        assertEquals(.60f, NutritionDrain.next(.60f, .75, .70));
        assertEquals(5, NutritionDrain.sugarMultiplier(.60));
        assertTrue(NutritionDrain.debtPerSecond(.70) > NutritionDrain.debtPerSecond(.30));
    }
}
