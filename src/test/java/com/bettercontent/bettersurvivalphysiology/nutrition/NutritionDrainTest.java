package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NutritionDrainTest {
    @Test
    void upperBandLastsAboutFiveIdleMinutesButSugarShortensIt() {
        float plain = .90f, tempoOne = .90f, tempoTwo = .90f;
        for (int second = 0; second < 20; second++) {
            plain = NutritionDrain.next(plain, .80, 0.0);
            tempoOne = NutritionDrain.next(tempoOne, .80, .30);
            tempoTwo = NutritionDrain.next(tempoTwo, .80, .70);
        }
        assertTrue(tempoTwo <= tempoOne && tempoOne <= plain);
        assertEquals(.60f, NutritionDrain.next(.60f, .80, .70));
    }
}
