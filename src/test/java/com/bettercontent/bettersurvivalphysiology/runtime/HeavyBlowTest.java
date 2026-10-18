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
            assertFalse(feedback.update(false, 0));
            assertFalse(feedback.update(false, .5f));
            assertFalse(feedback.update(true, .90f));
            for (int tick = 0; tick < 100; tick++) assertFalse(feedback.update(true, 1));
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
        assertTrue(feedback.update(true, 1));
        assertFalse(feedback.update(true, 1));
        feedback.attacked();
        assertTrue(feedback.update(true, 1));
        assertFalse(feedback.update(true, 1));
    }

    private static boolean ready(double proteins, int cooldown, long elapsed, float charge, boolean blackout) {
        return HeavyBlow.ready(proteins, .4, cooldown, 1000, 1000 - elapsed, charge, blackout);
    }
}
