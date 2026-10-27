package com.bettercontent.bettersurvivalphysiology.recovery;

import java.util.Objects;

/** Pure paid recovery accounting. Not registered with Minecraft yet.
 *
 * Planning never debits anything. A future native adapter must first establish admission
 * (in particular, reject Death's Door maim-mending), carry out only the authorized native
 * changes, and settle their OBSERVED gains. Canceled or clamped HP is not a saturation cost.
 * This model does not grant respawn privileges, identify heal sources, dispatch events,
 * simulate native float writes, or assume that an accepted heal actually changed health.
 */
public final class RecoveryPolicy {
    public static final int FULL_HUNGER = 20;
    public static final int ORDINARY_INTERVAL_TICKS = 80;
    public static final double ORDINARY_BUDGET = 1;

    private RecoveryPolicy() {}

    /** Food/saturation use the ordinary native cap, not an oversaturation reserve. */
    public record State(int hunger, double saturation, double health, double maximumHealth) {
        public State {
            if (hunger < 0 || hunger > FULL_HUNGER) throw new IllegalArgumentException("Hunger outside native range");
            nonnegativeFinite(saturation, "saturation");
            nonnegativeFinite(health, "health");
            nonnegativeFinite(maximumHealth, "maximum health");
            if (saturation > hunger) throw new IllegalArgumentException("Saturation exceeds native food cap");
            if (maximumHealth == 0 || health > maximumHealth) throw new IllegalArgumentException("Health outside native range");
        }
    }

    /** Only this policy can create authorization; an unspent request is never stored credit. */
    public static final class Plan {
        private final State before;
        private final double budget;
        private final int hungerUnits;
        private final double healthUnits;

        private Plan(State before, double budget, int hungerUnits, double healthUnits) {
            this.before = before;
            this.budget = budget;
            this.hungerUnits = hungerUnits;
            this.healthUnits = healthUnits;
        }
        public State before() { return before; }
        public double budget() { return budget; }
        public int hungerUnits() { return hungerUnits; }
        public double healthUnits() { return healthUnits; }
        public double maximumSaturationCost() { return hungerUnits + healthUnits; }
    }

    /** Authorization for an immediate finite earned-heal request, or a single ordinary pulse.
     * Admission=false means no hunger, health OR saturation operation is authorized.
     * Hunger is integral; a sub-unit budget cannot buy a fraction of a hunger point.
     * HP may be fractional and is authorized only after hunger can actually reach twenty.
     */
    public static Plan plan(State before, double budget, boolean admitted) {
        Objects.requireNonNull(before, "before");
        nonnegativeFinite(budget, "budget");
        if (!admitted || before.health() == 0 || budget == 0)
            return new Plan(before, budget, 0, 0);
        int food = Math.min(FULL_HUNGER - before.hunger(),
                (int) Math.floor(Math.min(before.saturation(), budget)));
        double hp = before.hunger() + food == FULL_HUNGER
                ? Math.min(before.maximumHealth() - before.health(),
                        Math.min(before.saturation() - food, budget - food)) : 0;
        return new Plan(before, budget, food, hp);
    }

    /** A native controller must reset its bounded elapsed counter after an ordinary pulse.
     * This does not advance time or accumulate missed pulses into a burst of free recovery.
     */
    public static Plan ordinary(State before, int elapsedNativeTicks, boolean admitted) {
        if (elapsedNativeTicks < 0) throw new IllegalArgumentException("Negative elapsed ticks");
        return plan(before, elapsedNativeTicks >= ORDINARY_INTERVAL_TICKS ? ORDINARY_BUDGET : 0, admitted);
    }

    /** Validate observed native gains and charge only what really happened.
     * The adapter must prevent increases above this authorization BEFORE native writes;
     * discovering an over-budget gain here is a hard error, not permission or a rollback.
     * Native float quantization, concurrent event changes and debit application remain
     * adapter responsibilities; a Plan is neither an HP write nor an up-front debit.
     */
    public static Settlement settle(Plan plan, int actualHungerGain, double actualHealthGain) {
        Objects.requireNonNull(plan, "plan");
        nonnegativeFinite(actualHealthGain, "actual health gain");
        if (actualHungerGain < 0 || actualHungerGain > plan.hungerUnits())
            throw new IllegalArgumentException("Unapproved actual hunger gain");
        if (actualHealthGain > plan.healthUnits())
            throw new IllegalArgumentException("Unapproved actual health gain");
        if (actualHealthGain > 0 && plan.before().hunger() + actualHungerGain != FULL_HUNGER)
            throw new IllegalArgumentException("HP recovery requires actual full hunger");
        double paid = actualHungerGain + actualHealthGain;
        if (paid > plan.before().saturation() || paid > plan.budget())
            throw new IllegalArgumentException("Actual gain exceeds paid reserve or budget");
        return new Settlement(actualHungerGain, actualHealthGain, paid,
                plan.before().saturation() - paid, plan.budget() - paid);
    }

    /** A receipt, not an instruction to restore unspent budget on a later tick. */
    public record Settlement(int hungerGain, double healthGain, double saturationCost,
                             double remainingSaturation, double unusedBudget) {}

    private static void nonnegativeFinite(double value, String label) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid " + label);
    }
}
