package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.api.SpiceEffects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameplayHooksSavingsTest {
    @Test void savedSpiceKeepsHistoricStandaloneResourceSavingWithoutProvider() {
        assertEquals(.85, GameplayHooks.resourceSavings(1, 1, false, SpiceEffects.Terms.EMPTY), 1e-12);
        assertEquals(.25, GameplayHooks.resourceSavings(.5, 0, false, SpiceEffects.Terms.EMPTY), 1e-12);
        assertEquals(0, GameplayHooks.resourceSavings(0, 1, false, SpiceEffects.Terms.EMPTY), 1e-12);
    }

    @Test void presentProviderIsSoleSpicePolicyContributorAtSharedCostBoundary() {
        var contributed = new SpiceEffects.Terms(.4, .2, .37, .4, .2, 4, 8);
        assertEquals(.37, GameplayHooks.resourceSavings(1, 1, true, contributed), 1e-12);
        assertEquals(0, GameplayHooks.resourceSavings(1, 1, true, SpiceEffects.Terms.EMPTY), 1e-12);
    }
}
