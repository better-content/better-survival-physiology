package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DietGuideTest {
    @Test
    void nutritionGuideUsesFourProgressionMilestones() {
        var guide = DietBenefits.guide(AspectIdentity.IMPACT, state(.69f, 0, 0, 0, 0, .40f, .70f, .95f));
        assertEquals("20%", guide.get(0).threshold());
        assertEquals("40%", guide.get(1).threshold());
        assertEquals("60%", guide.get(2).threshold());
        assertEquals("80%", guide.get(3).threshold());
        assertTrue(guide.get(2).current());
        assertFalse(guide.get(3).current());
    }

    @Test
    void sugarAndAlcoholShowFutureEffectsAndTheirRisks() {
        var sugar = DietBenefits.guide(AspectIdentity.TEMPO, state(0, .30f, .30f, 0, 0, .5f, .75f, .9f));
        assertTrue(sugar.get(0).current());
        assertTrue(sugar.get(1).current());
        assertTrue(sugar.get(1).effect().contains("Hunger"));

        var alcohol = DietBenefits.guide(AspectIdentity.CONTROL, state(0, 0, 0, .70f, 0, .5f, .75f, .9f));
        assertTrue(alcohol.get(0).current());
        assertTrue(alcohol.get(1).current());
        assertTrue(alcohol.get(2).current());
    }

    private static MetabolicSyncPacket state(float proteins, float sugar, float debt, float alcohol,
                                             float grains, float ordinary, float prepared, float feast) {
        return new MetabolicSyncPacket(proteins, grains, 0, 0, 0, 0, sugar, alcohol,
                ordinary, prepared, feast, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1);
    }
}
