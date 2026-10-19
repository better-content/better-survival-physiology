package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DietGuideTest {
    @Test
    void nutritionGuideShowsContinuousPrimaryAndOnlyRealPerks() {
        var snapshot = state(.69f, 0, 0, 0, 0, .40f, .70f, .95f);
        var guide = DietBenefits.guide(AspectIdentity.IMPACT, snapshot);
        assertEquals("Now", guide.get(0).threshold());
        assertTrue(guide.get(0).effect().contains("27.6%"));
        assertEquals("40%", guide.get(1).threshold());
        assertEquals("80%", guide.get(2).threshold());
        assertTrue(guide.get(1).current());
        assertFalse(guide.get(2).current());
        assertEquals(1, DietBenefits.guide(AspectIdentity.ROBUSTNESS, snapshot).stream()
                .filter(tier -> tier.value() >= 0).count());
        assertEquals(3, DietBenefits.guide(AspectIdentity.WORK, snapshot).stream()
                .filter(tier -> tier.value() >= 0).count());
    }

    @Test
    void timersUseRealGroupPerksRatherThanAUniversalTierLadder() {
        assertEquals(.40, DietBenefits.activePerkFloor(AspectIdentity.IMPACT, .69), 1e-9);
        assertEquals(.60, DietBenefits.activePerkFloor(AspectIdentity.WORK, .69), 1e-9);
        assertEquals(0, DietBenefits.activePerkFloor(AspectIdentity.ROBUSTNESS, .69), 1e-9);
        assertEquals(.20, DietBenefits.activePerkFloor(AspectIdentity.MOBILITY, .30), 1e-9);
        assertEquals(0, DietBenefits.activePerkFloor(AspectIdentity.RENEWAL, .30), 1e-9);
    }

    @Test
    void sugarAndAlcoholShowFutureEffectsAndTheirRisks() {
        var sugar = DietBenefits.guide(AspectIdentity.TEMPO, state(0, .30f, .30f, 0, 0, .5f, .75f, .9f));
        assertEquals("20%", sugar.get(0).threshold());
        assertEquals("80%", sugar.get(3).threshold());
        assertTrue(sugar.get(0).current());
        assertFalse(sugar.get(1).current());
        assertTrue(sugar.get(1).effect().contains("hunger/nutrient drain"));
        assertTrue(sugar.get(1).effect().contains("Draught penalties"));

        var alcohol = DietBenefits.guide(AspectIdentity.CONTROL, state(0, 0, 0, .70f, 0, .5f, .75f, .9f));
        assertTrue(alcohol.get(0).current());
        assertTrue(alcohol.get(1).current());
        assertTrue(alcohol.get(2).current());
        assertFalse(alcohol.get(3).current());
    }

    @Test
    void journalDescriptionsStayShortAndKeepCriticalQualifiers() {
        for (float level : new float[]{0, .4f, .8f, 1f, 1.25f}) {
            var snapshot = state(level, level, 0, level, level, .5f, .75f, .9f);
            for (var aspect : AspectIdentity.values()) {
                String current = DietBenefits.current(aspect, snapshot);
                assertTrue(current.length() <= 100, aspect + ": " + current);
                for (var row : DietBenefits.guide(aspect, snapshot)) {
                    assertTrue(row.effect().length() <= 100, aspect + ": " + row.effect());
                }
            }
        }
        var snapshot = state(.8f, 0, 0, .8f, .8f, .5f, .75f, .9f);
        assertTrue(DietBenefits.current(AspectIdentity.WORK, snapshot).contains("Right-tool"));
        assertTrue(DietBenefits.current(AspectIdentity.ENDURANCE, snapshot).contains("loads extra"));
        assertTrue(DietBenefits.current(AspectIdentity.CONTROL, snapshot).contains("Damage taken"));
        assertTrue(DietBenefits.guide(AspectIdentity.IMPACT, snapshot).get(1).effect().contains("Cooldown 12s"));
        assertTrue(DietBenefits.guide(AspectIdentity.ENDURANCE, snapshot).get(1).effect().contains("Loads still cost extra"));
        assertTrue(DietBenefits.guide(AspectIdentity.RENEWAL, snapshot).get(2).effect().contains("timed harmful effect"));
    }

    private static MetabolicSyncPacket state(float proteins, float sugar, float debt, float alcohol,
                                             float grains, float ordinary, float prepared, float feast) {
        return new MetabolicSyncPacket(proteins, grains, 0, 0, 0, 0, sugar, alcohol,
                ordinary, prepared, feast, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1);
    }
}
