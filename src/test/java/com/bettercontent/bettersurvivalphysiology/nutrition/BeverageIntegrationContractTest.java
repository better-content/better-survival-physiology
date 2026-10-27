package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

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
        assertTrue(integration.contains("if (MetabolicStateStore.get(event.player()).blackout) {\n            event.setCanceled(true);"));
        assertTrue(integration.contains("RuntimeDiagnostics.playerEvent(event.player(), \"beverage.check\", \"vetoed\", \"blackout\")"));
    }

    @Test
    void typedAbiAdmissionRejectsLegacyAndEveryPartialProvider() {
        String plants = "com/bettercontent/betterdrinkingwater/PlantDrinks.class";
        String events = "com/bettercontent/betterdrinkingwater/api/BeverageEvents.class";
        Set<String> complete = new HashSet<>(Set.of(plants, events,
                events.replace(".class", "$Profile.class"),
                events.replace(".class", "$Check.class"),
                events.replace(".class", "$Sip.class")));
        assertFalse(com.bettercontent.bettersurvivalphysiology.compat.BeverageAbi.supportsTyped(path -> false));
        assertTrue(com.bettercontent.bettersurvivalphysiology.compat.BeverageAbi.supportsTyped(complete::contains));
        for (String missing : complete) {
            Set<String> partial = new HashSet<>(complete);
            partial.remove(missing);
            assertThrows(IllegalStateException.class,
                    () -> com.bettercontent.bettersurvivalphysiology.compat.BeverageAbi.supportsTyped(partial::contains),
                    "Partial Water ABI must explicitly fail: " + missing);
        }
    }

    @Test
    void optionalIntegrationIsGuardedAndProviderIsNotBundled() throws Exception {
        String entry = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/SystemicSalienceMod.java"));
        assertTrue(entry.contains("compat.BeverageAvailability.typed()"));
        assertTrue(entry.contains("BeverageCompat.register()"));
        String availability = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/compat/BeverageAvailability.java"));
        assertTrue(availability.contains("ModList.get().isLoaded(\"better_drinking_water\") && Typed.PRESENT"));
        assertTrue(availability.contains("BeverageAbi.supportsTyped(path -> loader.getResource(path) != null)"));
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
