package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NutritionHudTest {
    private static MetabolicSyncPacket state(float proteins, float grains, float fruits, float fats,
                                             float vegetables, float dairy, float sugar, float alcohol) {
        return new MetabolicSyncPacket(proteins, grains, fruits, fats, vegetables, dairy, sugar, alcohol,
                .2f, .4f, .8f, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1);
    }

    @Test
    void everyPositiveTotalAppearsEvenWithoutReadinessFlags() {
        var entries = NutritionHud.entries(state(.01f, .02f, .03f, .04f, .05f, .06f, .07f, .08f));
        assertEquals(List.of(AspectIdentity.values()), entries.stream().map(NutritionHud.Entry::aspect).toList());
        assertEquals(List.of("1%", "7%", "2%", "3%", "4%", "5%", "6%", "8%"),
                entries.stream().map(NutritionHud.Entry::label).toList());
    }

    @Test
    void zeroValuesAreOmittedAndCurrentTotalsReplaceOldValues() {
        assertTrue(NutritionHud.entries(MetabolicSyncPacket.EMPTY).isEmpty());
        var first = NutritionHud.entries(state(.5f, 0, 0, 0, 0, 0, 0, 0));
        assertEquals("50%", first.get(0).label());
        var next = NutritionHud.entries(state(0, 0, .1f, 0, 0, 0, 0, 0));
        assertEquals(1, next.size());
        assertEquals(AspectIdentity.MOBILITY, next.get(0).aspect());
        assertEquals("10%", next.get(0).label());
    }

    @Test
    void tracesAndLoadsAboveOneRemainVisible() {
        var entries = NutritionHud.entries(state(.00001f, 0, 0, 0, 0, 0, 1.5f, 2f));
        assertEquals(List.of("<1%", "150%", "200%"), entries.stream().map(NutritionHud.Entry::label).toList());
        assertTrue(NutritionHud.entries(state(-1, Float.NaN, Float.POSITIVE_INFINITY, 0, 0, 0, 0, 0)).isEmpty());
    }

    @Test
    void textRecapAndItsTransportAreRemoved() throws Exception {
        Path root = Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology");
        assertFalse(Files.exists(root.resolve("client/MealRecap.java")));
        assertFalse(Files.exists(root.resolve("network/MealFeedbackPacket.java")));
        String client = Files.readString(root.resolve("client/ClientSalienceEvents.java"));
        assertFalse(client.contains("MealRecap"));
        assertTrue(client.contains("NutritionHud.entries(state)"));
        assertTrue(client.contains("minecraft.screen != null"));
    }
}
