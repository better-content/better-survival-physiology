package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Authored, never executed without explicit test-run permission. */
final class ServingVectorsTest {
    @Test
    void explicitSourceContributionsAreMappedOnceAndDairyConservesItsDose() {
        var source = new ServingVectors.SourceServing(Map.of("proteins", .04, "grains", .02,
                "fruits", .03, "sugars", .01, "vegetables", .05, "dairy", .10,
                "spice", .025, "alcohol", .04));
        var canonical = source.canonical(.80);
        assertEquals(NativeDietRoster.GROUPS, canonical.gains().keySet());
        assertEquals(.12, canonical.gains().get("proteins"), 1e-12);
        assertEquals(.08, canonical.gains().get("carbs"), 1e-12);
        assertEquals(.05, canonical.gains().get("vegetables"), 1e-12);
        assertEquals(.025, canonical.gains().get("spice"), 1e-12);
        assertEquals(.04, canonical.gains().get("alcohol"), 1e-12);
        assertEquals(.10, canonical.gains().get("proteins") - .04
                + canonical.gains().get("carbs") - .06, 1e-12);
        assertThrows(IllegalArgumentException.class, () -> new ServingVectors.SourceServing(canonical.gains()),
                "Canonical groups cannot be reinterpreted as source contributions");
        assertEquals(Map.of("carbs", .25), new ServingVectors.CanonicalServing(Map.of("carbs", .25)).gains());
    }

    @Test
    void sugarMirrorDeduplicatesWithinIngredientBeforeDistinctFlourAndDairyAreAdded() {
        var flour = new ServingVectors.SourceServing(Map.of("grains", .02))
                .canonicalIngredient(0, 0).gains();
        var sweetFruit = new ServingVectors.CanonicalServing(Map.of("carbs", .01))
                .resolveNativeMirrors(.03, 0).gains();
        var cheese = new ServingVectors.SourceServing(Map.of("dairy", .10))
                .canonicalIngredient(0, 0).gains();
        assertEquals(.02, flour.get("carbs"), 1e-10);
        assertEquals(.03, sweetFruit.get("carbs"), 1e-10); // not .04 for the same paid ingredient
        assertEquals(.07, flour.get("carbs") + sweetFruit.get("carbs") + cheese.get("carbs"), 1e-10);
        assertEquals(.08, cheese.get("proteins"), 1e-10);
    }

    @Test
    void obsoleteKeysAndInvalidAmountsNeverReachTheCanonicalTracker() {
        for (String id : NativeDietRoster.RETIRED)
            assertThrows(IllegalArgumentException.class, () -> new ServingVectors.CanonicalServing(Map.of(id, .1)));
        assertThrows(IllegalArgumentException.class, () -> new ServingVectors.SourceServing(Map.of("carbs", .1)));
        assertThrows(IllegalArgumentException.class, () -> new ServingVectors.SourceServing(Map.of("dairy", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> new ServingVectors.SourceServing(Map.of("sugar", -1.0)));
        assertThrows(IllegalArgumentException.class, () -> new ServingVectors.SourceServing(Map.of("dairy", .1)).canonical(1.1));
        assertEquals(.08, new ServingVectors.SourceServing(Map.of("dairy", .1))
                .canonical(.80).gains().get("proteins"), 1e-12);
        assertEquals(.02, new ServingVectors.SourceServing(Map.of("dairy", .1))
                .canonical(.80).gains().get("carbs"), 1e-12);
    }
}
