package com.bettercontent.bettersurvivalphysiology.metabolism;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NectarGainTest {
    @Test
    void capsAtHalfTheSmallestLastingGainWithoutIncreasingTheBase() {
        assertEquals(0, NectarGain.capped(0, Map.of("fruits", .04)));
        assertEquals(.02, NectarGain.capped(.06, Map.of("fruits", .04)));
        assertEquals(.02, NectarGain.capped(.22, Map.of("dairy", .10, "grains", .04)));
        assertEquals(.01, NectarGain.capped(.01, Map.of("fruits", .04)));
        for (String group : new String[]{"proteins", "grains", "fruits", "fats", "spice", "dairy"})
            assertEquals(.02, NectarGain.capped(.34, Map.of(group, .04)), group);
    }

    @Test
    void ignoresNonLastingAndInvalidGainsButPreservesPureNectar() {
        assertEquals(.22, NectarGain.capped(.22, Map.of()));
        assertEquals(.22, NectarGain.capped(.22, Map.of("sugars", .001, "alcohol", .001,
                "unknown", .001, "proteins", -1.0, "grains", 0.0, "fruits", Double.NaN,
                "dairy", Double.POSITIVE_INFINITY, "fats", Double.NEGATIVE_INFINITY)));
        assertEquals(.000005, NectarGain.capped(.06, Map.of("fruits", .00001)));
        assertEquals(0, NectarGain.capped(Double.NaN, Map.of("fruits", .04)));
    }

    @Test
    void representativeCappedPercentagesStayVisiblyBelowTheirComparator() {
        for (double fruit : new double[]{.04, .001}) {
            var gains = Map.of("fruits", fruit, "sugars", .5);
            var entries = com.bettercontent.bettersurvivalphysiology.presentation.FoodNutrients.entries(
                    gains, NectarGain.capped(.06, gains), 0, false);
            var nectar = entries.stream().filter(entry -> entry.aspect() ==
                    com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity.TEMPO).findFirst().orElseThrow();
            var nutrient = entries.stream().filter(entry -> entry.aspect() ==
                    com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity.MOBILITY).findFirst().orElseThrow();
            assertEquals(fruit * .5, nectar.gain());
            assertNotEquals(nectar.percentage(), nutrient.percentage());
        }
    }

    @Test
    void allDeliveryPathsUseResolvedServingAndPaidSipsDoNotRecalculate() throws Exception {
        Path root = Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology");
        String profiles = Files.readString(root.resolve("metabolism/ConsumableProfiles.java"));
        assertTrue(profiles.contains("NectarGain.capped(baseSugar(stack), dietGains)"));
        assertTrue(profiles.contains(".map(gains -> servingSugar(stack, gains)).orElse(0.0)"));
        assertTrue(profiles.contains("LOGGER.error("));
        String finish = Files.readString(root.resolve("runtime/SalienceEvents.java"));
        assertTrue(finish.contains("double sugar = ConsumableProfiles.servingSugar(player, stack);"));
        String tooltip = Files.readString(root.resolve("client/ClientSalienceEvents.java"));
        assertTrue(tooltip.contains("ConsumableProfiles.servingSugar(event.getItemStack(), gains)"));
        String beverage = Files.readString(root.resolve("compat/BeverageCompat.java"));
        assertTrue(beverage.contains("ConsumableProfiles.servingSugar(stack, serving)"));
        assertTrue(beverage.contains(".orElse(0.0)"));
        String sip = beverage.substring(beverage.indexOf("public static void sip("));
        assertFalse(sip.contains("servingSugar"));
        assertTrue(sip.contains("BeverageNutrition.award(nutrition.getDouble(SUGAR), event.from(), event.to())"));
        assertFalse(profiles.contains("getValue("), "Potential serving calculation must not read current nutrition capacity");
    }
}
