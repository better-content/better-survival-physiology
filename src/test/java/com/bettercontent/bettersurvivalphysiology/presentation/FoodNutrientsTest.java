package com.bettercontent.bettersurvivalphysiology.presentation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FoodNutrientsTest {
    @Test
    void nativeGainsUsePackNamesAndStableHorizontalOrder() {
        var entries = FoodNutrients.entries(Map.of("diet:proteins", .025, "fruits", .06, "grains", .1), .06, 0, false);
        assertEquals(List.of("Sinew", "Nectar", "Grain", "Berry"),
                entries.stream().map(entry -> entry.aspect().displayName).toList());
        assertEquals(List.of("+2.5%", "+6%", "+10%", "+6%"),
                entries.stream().map(FoodNutrients.Entry::percentage).toList());
    }

    @Test
    void metabolicGainsReplaceDietValuesInsteadOfDoubleCounting() {
        var entries = FoodNutrients.entries(Map.of("sugars", .01, "alcohol", .02), .34, .15, false);
        assertEquals(List.of("+34%", "+15%"), entries.stream().map(FoodNutrients.Entry::percentage).toList());
        assertTrue(FoodNutrients.entries(Map.of("sugars", .1), 0, 0, false).isEmpty());
    }

    @Test
    void milkMatchesTheNativeConsumptionAward() {
        var entries = FoodNutrients.entries(Map.of(), 0, 0, true);
        assertEquals(List.of(AspectIdentity.ENDURANCE, AspectIdentity.RENEWAL),
                entries.stream().map(FoodNutrients.Entry::aspect).toList());
        assertEquals(List.of("+6%", "+6%"), entries.stream().map(FoodNutrients.Entry::percentage).toList());
    }

    @Test
    void zeroInvalidAndUnknownGainsAreOmittedButTracesRemainVisible() {
        var entries = FoodNutrients.entries(Map.of("proteins", 0.00001, "grains", 0.0,
                "fruits", -1.0, "dairy", Double.NaN, "fats", Double.POSITIVE_INFINITY, "unknown", .5), 0, 0, false);
        assertEquals(1, entries.size());
        assertEquals("+<0.1%", entries.get(0).percentage());
    }

    @Test
    void pinnedDietStillOwnsTheNarrowReplacedMethodAndCalculatesPercentageGains() throws Exception {
        Path jar = Path.of(System.getProperty("salience.nativeDiet"));
        assertEquals("2e3b631cd9773996c7eaefe3c1cd086045b9b9a193a8e4ab29cc8e64d937e5bd",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        Path output = Files.createTempDirectory(Path.of("build"), "native-food-tooltip-").resolve("DietClientEvents.javap.txt");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javap").toString(),
                "-p", "-c", "-classpath", jar.toString(), "com.illusivesoulworks.diet.client.DietClientEvents")
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "native javap timed out: " + output);
            assertEquals(0, process.exitValue());
            String bytecode = Files.readString(output);
            assertTrue(bytecode.contains("public static void renderItemTooltip(net.minecraft.world.entity.player.Player, net.minecraft.world.item.ItemStack, java.util.List<net.minecraft.network.chat.Component>);"));
            int start = bytecode.indexOf("public static void renderItemTooltip(");
            String method = bytecode.substring(start, bytecode.indexOf("private static java.lang.Boolean lambda$renderItemTooltip", start));
            assertTrue(method.contains("DietApi.get:"));
            assertTrue(method.contains("IDietResult.get:"));
            assertTrue(method.contains("float 100.0f"));
            assertTrue(method.contains("String tooltip.diet.eaten"));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "javap cleanup failed");
            }
        }
    }

    @Test
    void tooltipKeepsGraphicalBadgesWithoutGlyphsOrBenefitProse() throws Exception {
        Path root = Path.of("src/main");
        String client = Files.readString(root.resolve("java/com/bettercontent/bettersurvivalphysiology/client/ClientSalienceEvents.java"));
        assertTrue(client.contains("identity.badge()"));
        assertTrue(client.contains("withFont(ASPECT_FONT)"));
        assertTrue(client.contains("entry.percentage()"));
        assertFalse(client.contains("identity.glyph"));
        assertFalse(client.contains("preparedPromise"));
        assertFalse(client.contains("See Diet for all effects"));
        String mixin = Files.readString(root.resolve("java/com/bettercontent/bettersurvivalphysiology/mixin/DietFoodTooltipMixin.java"));
        assertTrue(mixin.contains("method = \"renderItemTooltip\""));
        assertTrue(mixin.contains("ci.cancel()"));
        assertTrue(Files.readString(root.resolve("resources/better_survival_physiology.mixins.json")).contains("DietFoodTooltipMixin"));
    }
}
