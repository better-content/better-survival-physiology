package com.bettercontent.bettersurvivalphysiology.api;

import org.junit.jupiter.api.Test;
import com.bettercontent.bettersurvivalphysiology.nutrition.ServingVectors;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CulinaryNutritionApiTest {
    @Test void absentProviderRetainsFallback() {
        var providers = new ServingProviders<Object, String, Integer>();
        assertTrue(providers.resolve(null, "vanilla").isEmpty());
    }
    @Test void duplicateAndLateRegistrationRejected() {
        var providers = new ServingProviders<Object, String, Integer>();
        providers.register("cooking", (p, s) -> Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> providers.register("cooking", (p, s) -> Optional.of(1)));
        providers.resolve(null, "food");
        assertThrows(IllegalStateException.class, () -> providers.register("late", (p, s) -> Optional.of(1)));
    }
    @Test void overlappingOwnersFailInsteadOfDoubleIntake() {
        var providers = new ServingProviders<Object, String, Integer>();
        providers.register("a", (p, s) -> Optional.of(1));
        providers.register("b", (p, s) -> Optional.of(2));
        assertThrows(IllegalStateException.class, () -> providers.resolve(null, "food"));
    }
    @Test void resolvedProvenanceAndQualityAreNotRescaledOrMergedWithTags() {
        var base = new HashMap<>(Map.of("carbs", .08)); // already canonical and divided by paid yield
        var adjusted = new HashMap<>(Map.of("carbs", .104)); // Superb exactly once
        var serving = new CulinaryNutritionApi.Serving(new ServingVectors.CanonicalServing(base),
                new ServingVectors.CanonicalServing(adjusted), .02, .03);
        base.put("carbs", .9); adjusted.put("carbs", .9);
        assertEquals(.08, serving.unscaled().gains().get("carbs"));
        assertEquals(.104, serving.adjusted().gains().get("carbs"));
        assertEquals(.02, serving.sugar()); assertEquals(.03, serving.alcohol());
        assertThrows(UnsupportedOperationException.class, () -> serving.adjusted().gains().put("fats", 1D));
        assertThrows(IllegalArgumentException.class, () -> new ServingVectors.CanonicalServing(Map.of("grains", .1)));
        assertThrows(IllegalArgumentException.class, () -> new CulinaryNutritionApi.Serving(
                new ServingVectors.CanonicalServing(Map.of()), new ServingVectors.CanonicalServing(Map.of()), Double.NaN, 0));
    }
    @Test void managedFinishDoesNotAutoCompleteAndCancellationNeverCallsComplete() throws Exception {
        String hooks = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/runtime/SalienceEvents.java"));
        assertFalse(hooks.contains("CulinaryNutritionApi.complete("));
        String api = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/api/CulinaryNutritionApi.java"));
        assertFalse(api.contains("@SubscribeEvent"));
        assertTrue(api.contains("serving.adjusted().gains().forEach"));
        assertFalse(api.contains("DietApi.getInstance")); // no fallback/tag credit after provenance
    }
    public static void main(String[] args) throws Exception {
        var tests = new CulinaryNutritionApiTest();
        tests.absentProviderRetainsFallback(); tests.duplicateAndLateRegistrationRejected();
        tests.overlappingOwnersFailInsteadOfDoubleIntake();
        tests.resolvedProvenanceAndQualityAreNotRescaledOrMergedWithTags();
        tests.managedFinishDoesNotAutoCompleteAndCancellationNeverCallsComplete();
        System.out.println("CulinaryNutritionApiTest: 5 PASS (pure/provider and source-boundary; not native completion acceptance)");
    }
}
