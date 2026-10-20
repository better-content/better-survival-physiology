package com.bettercontent.bettersurvivalphysiology.runtime;

/** Shared eligibility for combat and presentation. No Epic Fight-specific readiness clock. */
public final class HeavyBlow {
    private HeavyBlow() {}

    public static boolean ready(double proteins, double threshold, int cooldown,
                                long now, long lastAttackTick, float attackStrength, boolean blackout) {
        return eligible(proteins, threshold, cooldown, now, lastAttackTick, blackout)
                && charged(attackStrength);
    }

    /** Underlying availability, independent of native item-swap attack recharge. */
    public static boolean eligible(double proteins, double threshold, int cooldown,
                                   long now, long lastAttackTick, boolean blackout) {
        return !blackout && proteins >= threshold && cooldown == 0
                && now - lastAttackTick >= 80L;
    }

    public static boolean charged(float attackStrength) {
        return attackStrength >= 0.90f;
    }

    /** Session-local announcement latch. Charge-only interruptions do not re-arm the cue. */
    public static final class Feedback {
        private boolean initialized;
        private boolean announcedForEligibilityCycle;

        public boolean update(boolean eligible, float attackStrength) {
            if (!initialized) {
                // A newly joined player's attack ticker starts empty. Wait for its first charge
                // before taking the silent baseline, otherwise reconnect would manufacture an edge.
                if (charged(attackStrength)) {
                    initialized = true;
                    announcedForEligibilityCycle = eligible;
                }
                return false;
            }
            if (!eligible) {
                announcedForEligibilityCycle = false;
                return false;
            }
            if (!charged(attackStrength) || announcedForEligibilityCycle) return false;
            announcedForEligibilityCycle = true;
            return true;
        }

        public void attacked() {
            // An actual attack establishes an unready baseline even before the first tick/charge.
            initialized = true;
            announcedForEligibilityCycle = false;
        }
    }
}
