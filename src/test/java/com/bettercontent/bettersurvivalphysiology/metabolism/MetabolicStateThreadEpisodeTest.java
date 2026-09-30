package com.bettercontent.bettersurvivalphysiology.metabolism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MetabolicStateThreadEpisodeTest {
    @Test
    void nutritionEpisodeTokenSurvivesStatePersistence() {
        MetabolicState state = new MetabolicState();
        state.nutritionThreadToken = "player:nutrition:120";
        state.nutritionWarningPublished = true;

        MetabolicState restored = MetabolicState.load(state.save());
        assertEquals(state.nutritionThreadToken, restored.nutritionThreadToken);
        assertTrue(restored.nutritionWarningPublished);
    }

    @Test
    void invalidNutritionEpisodeTokenIsNotLoaded() {
        MetabolicState state = new MetabolicState();
        state.nutritionThreadToken = "bad token with spaces";
        state.nutritionWarningPublished = true;

        MetabolicState restored = MetabolicState.load(state.save());
        assertEquals("", restored.nutritionThreadToken);
        assertFalse(restored.nutritionWarningPublished);
    }
}
