package com.bettercontent.bettersurvivalphysiology.metabolism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumableProfilesTest {
    @Test
    void everyBrewingDrinkHasAnExplicitNonMonotonicLoadInput() {
        assertEquals(16, AlcoholProfiles.all().size());
        assertTrue(AlcoholProfiles.all().values().stream().allMatch(value -> value > 0.0 && value < 0.5));
    }
}
