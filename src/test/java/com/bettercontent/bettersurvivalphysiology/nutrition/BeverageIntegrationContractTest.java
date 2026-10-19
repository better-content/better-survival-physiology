package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BeverageIntegrationContractTest {
    @Test
    void fractionalFeedbackDoesNotTurnEachSipIntoAWholeMeal() throws Exception {
        String hooks = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/runtime/SalienceEvents.java"));
        int start = hooks.indexOf("public static void beverageFeedback(");
        String feedback = hooks.substring(start, hooks.indexOf("private static void finishMealFeedback", start));
        assertTrue(feedback.contains("edible && completed"));
        assertTrue(feedback.contains("NutritionThreadBoundary.onMealSettled"));
        assertTrue(feedback.contains("SalienceNetwork.sync"));
        assertFalse(feedback.contains("player.heal("));
        String integration = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/compat/BeverageCompat.java"));
        assertTrue(integration.contains("event.to() == 1.0"));
        assertTrue(integration.contains("MetabolicStateStore.save(player)"));
        assertTrue(integration.contains("state.blackoutX = player.getX()"));
        assertTrue(integration.contains("MetabolicStateStore.get(event.player()).blackout) event.setCanceled(true)"));
    }

    @Test
    void optionalIntegrationIsGuardedAndProviderIsNotBundled() throws Exception {
        String entry = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/SystemicSalienceMod.java"));
        assertTrue(entry.contains("isLoaded(\"better_drinking_water\")"));
        assertTrue(entry.contains("BeverageCompat.register()"));
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        assertTrue(metadata.contains("modId=\"better_drinking_water\"\nmandatory=false"));
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("compileOnly fg.deobf(beverageDependency)"));
        assertTrue(build.contains("runtimeOnly fg.deobf(beverageDependency)"));
        assertTrue(build.contains("flatDir { dirs beverageDirectory"));
        assertTrue(build.contains("MessageDigest.getInstance('SHA-256')"));
        assertFalse(build.contains("fg.deobf(files("));
        assertFalse(build.contains("implementation fg.deobf(beverageDependency)"));
    }
}
