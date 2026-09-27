package com.bettercontent.systemicsalience.presentation;

import com.bettercontent.systemicsalience.network.MetabolicSyncPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DietGuideTest {
    @Test
    void nutritionGuideUsesSyncedThresholds() {
        var guide = DietBenefits.guide(AspectIdentity.IMPACT, state(.69f, 0, 0, 0, 0, .40f, .70f, .95f));
        assertEquals("40%", guide.get(0).threshold());
        assertEquals("70%", guide.get(1).threshold());
        assertEquals("95%", guide.get(2).threshold());
        assertTrue(guide.get(0).current());
        assertFalse(guide.get(1).current());
    }

    @Test
    void sugarAndAlcoholShowFutureEffectsAndTheirRisks() {
        var sugar = DietBenefits.guide(AspectIdentity.TEMPO, state(0, .30f, .30f, 0, 0, .5f, .75f, .9f));
        assertTrue(sugar.get(0).current());
        assertFalse(sugar.get(1).current());
        assertTrue(sugar.get(2).effect().contains("crash"));

        var alcohol = DietBenefits.guide(AspectIdentity.CONTROL, state(0, 0, 0, .70f, 0, .5f, .75f, .9f));
        assertFalse(alcohol.get(0).current());
        assertTrue(alcohol.get(1).current());
        assertFalse(alcohol.get(2).current());
    }

    private static MetabolicSyncPacket state(float proteins, float sugar, float debt, float alcohol,
                                             float grains, float ordinary, float prepared, float feast) {
        return new MetabolicSyncPacket(proteins, grains, 0, 0, 0, 0, sugar, debt, alcohol,
                ordinary, prepared, feast, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1);
    }
}
