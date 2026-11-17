package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DietGuideTest {
    @Test
    void guideShowsContinuousProteinBenefitsAndOnlyRealPerks() {
        var snapshot = state(.69f, 0, 0, .40f, .95f);
        var guide = DietBenefits.guide(AspectIdentity.POWER, snapshot);
        assertEquals("Now", guide.get(0).threshold());
        assertTrue(guide.get(0).effect().contains("27.6%"));
        assertEquals("40%", guide.get(1).threshold());
        assertEquals("80%", guide.get(2).threshold());
        assertTrue(guide.get(1).current()); assertFalse(guide.get(2).current());
        assertEquals(1, DietBenefits.guide(AspectIdentity.ENDURANCE, snapshot).stream()
                .filter(tier -> tier.value() >= 0).count());
        assertEquals(2, DietBenefits.guide(AspectIdentity.RENEWAL, snapshot).stream()
                .filter(tier -> tier.value() >= 0).count());
    }

    @Test
    void timersUseRealGroupPerksRatherThanUniversalTierLadder() {
        assertEquals(.40, DietBenefits.activePerkFloor(AspectIdentity.POWER, .69), 1e-9);
        assertEquals(0, DietBenefits.activePerkFloor(AspectIdentity.ENDURANCE, .69), 1e-9);
        assertEquals(.20, DietBenefits.activePerkFloor(AspectIdentity.MOTION, .30), 1e-9);
        assertEquals(0, DietBenefits.activePerkFloor(AspectIdentity.RENEWAL, .30), 1e-9);
        assertEquals(0, DietBenefits.activePerkFloor(AspectIdentity.CONTROL, 1), 1e-9);
    }

    @Test
    void carbsAmplificationAndOptionalAlcoholRisksAreVisible() {
        var carbs = state(0, .30f, 0, 0, 0);
        assertTrue(DietBenefits.current(AspectIdentity.MOTION, carbs).contains("Alcohol penalties"));
        assertTrue(DietBenefits.current(AspectIdentity.MOTION, carbs).contains("drain"));
        assertTrue(DietBenefits.effect(AspectIdentity.MOTION, NutritionTier.SECOND).contains("expenditure"));
        assertTrue(DietBenefits.guide(AspectIdentity.MOTION, carbs).get(1).effect().contains("Carbs level"));
        var alcohol = DietBenefits.guide(AspectIdentity.CONTROL, state(0, 0, 0, 0, .70f));
        assertTrue(alcohol.get(0).current()); assertTrue(alcohol.get(1).current());
        assertTrue(alcohol.get(2).current()); assertFalse(alcohol.get(3).current());
        assertTrue(DietBenefits.effect(AspectIdentity.CONTROL, NutritionTier.SECOND).contains("Optional"));
    }

    @Test
    void journalKeepsCriticalQualifiersAndShortCurrentGuides() {
        for (float level : new float[]{0, .4f, .8f, 1f, 1.25f}) {
            var snapshot = state(level, level, level, level, level);
            for (var aspect : AspectIdentity.values()) {
                String current = DietBenefits.current(aspect, snapshot);
                assertTrue(current.length() <= 100, aspect + ": " + current);
                for (var row : DietBenefits.guide(aspect, snapshot))
                    assertTrue(row.effect().length() <= 100, aspect + ": " + row.effect());
            }
        }
        var snapshot = state(.8f, .8f, .8f, .8f, .8f);
        assertTrue(DietBenefits.current(AspectIdentity.POWER, snapshot).contains("correct-tool"));
        assertTrue(DietBenefits.current(AspectIdentity.ENDURANCE, snapshot).contains("Resource costs"));
        assertTrue(DietBenefits.current(AspectIdentity.CONTROL, snapshot).contains("Damage taken"));
        assertTrue(DietBenefits.guide(AspectIdentity.POWER, snapshot).get(1).effect().contains("Cooldown 12s"));
        assertTrue(DietBenefits.guide(AspectIdentity.ENDURANCE, snapshot).get(1).effect().contains("Loads still cost extra"));
        assertTrue(DietBenefits.guide(AspectIdentity.RENEWAL, snapshot).get(2).effect().contains("timed harmful effect"));
    }

    private static MetabolicSyncPacket state(float proteins, float carbs, float spice, float vegetables, float alcohol) {
        return new MetabolicSyncPacket(proteins, carbs, spice, vegetables, alcohol,
                .2f, .4f, .8f, 0, 0, -1, -1, -1, -1, -1);
    }
}
