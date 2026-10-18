package com.bettercontent.bettersurvivalphysiology.runtime;

/** Shared eligibility for combat and presentation. No Epic Fight-specific readiness clock. */
public final class HeavyBlow {
    private HeavyBlow() {}

    public static boolean ready(double proteins, double threshold, int cooldown,
                                long now, long lastAttackTick, float attackStrength, boolean blackout) {
        return !blackout && proteins >= threshold && cooldown == 0
                && now - lastAttackTick >= 80L && charged(attackStrength);
    }

    public static boolean charged(float attackStrength) {
        return attackStrength >= 0.90f;
    }

    /** Session-local edge detector. Initial login/reconnect attack recharge is not an activation. */
    public static final class Feedback {
        private boolean initialized;
        private boolean wasReady;

        public boolean update(boolean ready, float attackStrength) {
            if (!initialized) {
                // A newly joined player's attack ticker starts empty. Wait for its first charge
                // before taking the silent baseline, otherwise reconnect would manufacture an edge.
                if (charged(attackStrength)) {
                    initialized = true;
                    wasReady = ready;
                }
                return false;
            }
            boolean rising = ready && !wasReady;
            wasReady = ready;
            return rising;
        }

        public void attacked() {
            // An actual attack establishes an unready baseline even before the first tick/charge.
            initialized = true;
            wasReady = false;
        }
    }
}
