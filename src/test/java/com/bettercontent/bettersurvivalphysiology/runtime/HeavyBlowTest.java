package com.bettercontent.bettersurvivalphysiology.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeavyBlowTest {
    @Test
    void exactExistingCombatBoundariesAreShared() {
        assertTrue(ready(.4, 0, 80, .90f, false));
        assertFalse(ready(Math.nextDown(.4), 0, 80, .90f, false));
        assertFalse(ready(.4, 1, 80, .90f, false));
        assertFalse(ready(.4, 0, 79, .90f, false));
        assertFalse(ready(.4, 0, 80, Math.nextDown(.90f), false));
        assertFalse(ready(.4, 0, 80, 1, true));
        assertTrue(HeavyBlow.ready(.7, .7, 0, 80, 0, 1, false));
        assertFalse(HeavyBlow.ready(.69, .7, 0, 80, 0, 1, false));
    }

    @Test
    void loginAndReconnectRechargeEstablishSilentBaseline() {
        for (int session = 0; session < 3; session++) {
            var feedback = new HeavyBlow.Feedback();
            assertFalse(feedback.update(true, 0));
            assertFalse(feedback.update(true, .5f));
            assertFalse(feedback.update(true, .90f));
            for (int tick = 0; tick < 100; tick++) assertFalse(feedback.update(true, 1));
        }
    }

    @Test
    void swapOnlyRechargeAndChargeThresholdJitterNeverRepeatTheCue() {
        var feedback = new HeavyBlow.Feedback();
        assertFalse(feedback.update(true, 1)); // Silent, eligible login baseline.
        for (int swap = 0; swap < 100; swap++) {
            for (float charge : new float[] {0, .5f, Math.nextDown(.90f), .90f, 1, .89f, .91f}) {
                assertFalse(feedback.update(true, charge));
            }
        }
    }

    @Test
    void eachUnderlyingBlockerRearmsOneCueButWaitsForCharge() {
        boolean[] blocked = {
                eligible(Math.nextDown(.4), 0, 80, false),
                eligible(.4, 1, 80, false),
                eligible(.4, 0, 79, false),
                eligible(.4, 0, 80, true)
        };
        boolean recovered = eligible(.4, 0, 80, false);
        assertTrue(recovered);
        for (boolean eligibility : blocked) {
            assertFalse(eligibility);
            var feedback = new HeavyBlow.Feedback();
            assertFalse(feedback.update(recovered, 1));
            assertFalse(feedback.update(eligibility, 1));
            // A genuine recovery overlapping an item-swap charge dip must wait, not disappear.
            assertFalse(feedback.update(recovered, 0));
            assertFalse(feedback.update(recovered, Math.nextDown(.90f)));
            assertTrue(feedback.update(recovered, .90f));
            assertFalse(feedback.update(recovered, 1));
            assertFalse(feedback.update(recovered, 0));
            assertFalse(feedback.update(recovered, 1));
        }
    }

    @Test
    void ineligibleLoginBaselineIsSilentAndLaterRecoveryWaitsForCharge() {
        var feedback = new HeavyBlow.Feedback();
        assertFalse(feedback.update(false, 0));
        assertFalse(feedback.update(false, .5f));
        assertFalse(feedback.update(false, .90f));
        assertFalse(feedback.update(true, .5f));
        assertTrue(feedback.update(true, .90f));
        assertFalse(feedback.update(true, 1));
    }

    @Test
    void combatPredicateRetainsOriginalBoundariesAcrossAvailabilityAndCharge() {
        for (double proteins : new double[] {0, Math.nextDown(.4), .4, 1}) {
            for (int cooldown : new int[] {0, 1}) {
                for (long elapsed : new long[] {79, 80, 100}) {
                    for (boolean blackout : new boolean[] {false, true}) {
                        boolean expectedEligibility = !blackout && proteins >= .4
                                && cooldown == 0 && elapsed >= 80;
                        assertEquals(expectedEligibility, eligible(proteins, cooldown, elapsed, blackout));
                        for (float charge : new float[] {0, Math.nextDown(.90f), .90f, 1}) {
                            assertEquals(expectedEligibility && charge >= .90f,
                                    ready(proteins, cooldown, elapsed, charge, blackout));
                        }
                    }
                }
            }
        }
    }

    @Test
    void initiallyUnreadyPlayerStillHearsLaterCooldownOrNutritionTransition() {
        var feedback = new HeavyBlow.Feedback();
        assertFalse(feedback.update(false, 1));
        assertTrue(feedback.update(true, 1));
        assertFalse(feedback.update(true, 1));
        assertFalse(feedback.update(false, 1));
        assertTrue(feedback.update(true, 1));
    }

    @Test
    void attackBetweenTicksRearmsExactlyOneCueIncludingBeforeLoginBaseline() {
        var feedback = new HeavyBlow.Feedback();
        feedback.attacked();
        assertFalse(feedback.update(false, 0));
        assertFalse(feedback.update(false, .8f));
        assertFalse(feedback.update(true, .8f));
        assertTrue(feedback.update(true, 1));
        assertFalse(feedback.update(true, 1));
        feedback.attacked();
        assertFalse(feedback.update(true, 0));
        assertTrue(feedback.update(true, 1));
        assertFalse(feedback.update(true, 1));
    }

    private static boolean eligible(double proteins, int cooldown, long elapsed, boolean blackout) {
        return HeavyBlow.eligible(proteins, .4, cooldown, 1000, 1000 - elapsed, blackout);
    }

    private static boolean ready(double proteins, int cooldown, long elapsed, float charge, boolean blackout) {
        return HeavyBlow.ready(proteins, .4, cooldown, 1000, 1000 - elapsed, charge, blackout);
    }
}
