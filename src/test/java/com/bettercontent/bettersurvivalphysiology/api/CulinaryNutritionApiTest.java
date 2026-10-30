package com.bettercontent.bettersurvivalphysiology.api;

import org.junit.jupiter.api.Test;
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
        var base = new HashMap<>(Map.of("grains", .08)); // input provenance already divided by actual yield
        var adjusted = new HashMap<>(Map.of("grains", .104)); // Superb exactly once
        var serving = new CulinaryNutritionApi.Serving(base, adjusted, .02, .03);
        base.put("grains", .9); adjusted.put("grains", .9);
        assertEquals(.08, serving.unscaled().get("grains"));
        assertEquals(.104, serving.adjusted().get("grains"));
        assertEquals(.02, serving.sugar()); assertEquals(.03, serving.alcohol());
        assertThrows(UnsupportedOperationException.class, () -> serving.adjusted().put("fats", 1D));
        assertThrows(IllegalArgumentException.class, () -> new CulinaryNutritionApi.Serving(Map.of("spice", .1), Map.of(), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CulinaryNutritionApi.Serving(Map.of(), Map.of(), Double.NaN, 0));
    }
    @Test void managedFinishDoesNotAutoCompleteAndCancellationNeverCallsComplete() throws Exception {
        String hooks = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/runtime/SalienceEvents.java"));
        assertFalse(hooks.contains("CulinaryNutritionApi.complete("));
        String api = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/api/CulinaryNutritionApi.java"));
        assertFalse(api.contains("@SubscribeEvent"));
        assertTrue(api.contains("serving.adjusted().forEach"));
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
