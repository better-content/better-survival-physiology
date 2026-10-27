package com.bettercontent.bettersurvivalphysiology.recovery;

import java.util.Random;

/** Dependency-free execution of the actual pure policy, not source-string or Minecraft tests. */
public final class RecoveryPolicyPureTest {
    private static int assertions;
    private RecoveryPolicyPureTest() {}

    public static void main(String[] args) { run(); }
    public static void run() {
        assertions = 0;
        hungerFirst();
        actualPayment();
        suppressedAndInvalid();
        ordinaryCadence();
        independentGridOracle();
        System.out.println("RecoveryPolicyPureTest PASS " + assertions + " assertions (pure model, NOT native integration)");
    }

    private static void hungerFirst() {
        var hungry = state(18, 10, 10, 20);
        var one = RecoveryPolicy.plan(hungry, 1, true);
        eq(one.hungerUnits(), 1, "One ordinary budget buys one hunger");
        eq(one.healthUnits(), 0, "No HP before full hunger");
        var direct = RecoveryPolicy.plan(hungry, 6, true);
        eq(direct.hungerUnits(), 2, "Finite earned budget fills hunger first");
        eq(direct.healthUnits(), 4, "Only remaining finite budget may heal");
        var receipt = RecoveryPolicy.settle(direct, 2, 4);
        eq(receipt.saturationCost(), 6, "One saturation per actual restored unit");
        eq(receipt.remainingSaturation(), 4, "Reserve reduced exactly once");
        eq(receipt.unusedBudget(), 0, "No duplicated earned budget");
        var limited = RecoveryPolicy.plan(state(19, 1, 10, 20), 16, true);
        eq(limited.hungerUnits(), 1, "Last saturation goes to last hunger");
        eq(limited.healthUnits(), 0, "No second use of saturation already paid for hunger");
        eq(RecoveryPolicy.plan(state(19, .5, 10, 20), 16, true).maximumSaturationCost(), 0,
                "Insufficient whole hunger payment cannot enable HP");
        eq(RecoveryPolicy.plan(state(19, 10, 10, 20), Math.nextDown(1.0), true).maximumSaturationCost(), 0,
                "A sub-unit direct budget cannot create a whole hunger point");
        var healthyHungry = RecoveryPolicy.plan(state(18, 5, 20, 20), 16, true);
        eq(healthyHungry.hungerUnits(), 2, "Healthy but hungry player may buy hunger");
        eq(healthyHungry.healthUnits(), 0, "Full HP is not a payable gain");
        var fraction = RecoveryPolicy.plan(state(20, .5, 19.75, 20), 16, true);
        eq(fraction.healthUnits(), .25, "Max-health clamp authorizes only missing HP");
        eq(RecoveryPolicy.settle(fraction, 0, .25).saturationCost(), .25, "Clamped quarter-HP costs a quarter, not requested sixteen");
        eq(RecoveryPolicy.plan(state(20, 0, 10, 20), 16, true).healthUnits(), 0, "No free magic healing without saturation");
        eq(RecoveryPolicy.plan(state(20, 20, 20, 20), 16, true).maximumSaturationCost(), 0, "Full player has no payable work");
    }

    private static void actualPayment() {
        var p = RecoveryPolicy.plan(state(20, 10, 16, 20), 16, true);
        eq(p.healthUnits(), 4, "Only four missing HP authorized");
        eq(RecoveryPolicy.settle(p, 0, 0).saturationCost(), 0, "Canceled Door/native heal spends nothing");
        eq(RecoveryPolicy.settle(p, 0, 0).remainingSaturation(), 10, "Failed heal preserves reserve");
        var reduced = RecoveryPolicy.settle(p, 0, 1.5);
        eq(reduced.saturationCost(), 1.5, "Reduced actual heal pays actual amount only");
        eq(reduced.remainingSaturation(), 8.5, "No up-front four/sixteen-unit debit");
        eq(reduced.unusedBudget(), 14.5, "Unspent request is reported, not automatically reissued");
        var mixed = RecoveryPolicy.plan(state(18, 10, 10, 20), 6, true);
        eq(RecoveryPolicy.settle(mixed, 0, 0).saturationCost(), 0, "Completely failed mixed recovery has no cost");
        eq(RecoveryPolicy.settle(mixed, 1, 0).saturationCost(), 1, "Only actually restored hunger is charged");
        eq(RecoveryPolicy.settle(mixed, 2, 0).saturationCost(), 2, "Canceled HP does not refund real restored hunger or charge absent HP");
        reject(() -> RecoveryPolicy.settle(mixed, 1, 1), "Failed hunger fill cannot precede HP recovery");
        reject(() -> RecoveryPolicy.settle(mixed, 3, 0), "No unapproved hunger");
        reject(() -> RecoveryPolicy.settle(mixed, 2, 4.125), "No unapproved HP");
        reject(() -> RecoveryPolicy.settle(p, 0, -1), "No negative observed gain");
        reject(() -> RecoveryPolicy.settle(p, -1, 0), "No negative hunger gain");
        var tiny = RecoveryPolicy.plan(state(20, .5, 19.5, 20), Double.MIN_VALUE, true);
        eq(tiny.healthUnits(), Double.MIN_VALUE, "Finite tiny request is not rounded up into free HP");
        eq(RecoveryPolicy.settle(tiny, 0, 0).saturationCost(), 0, "Native precision loss that restores no HP has zero charge");
        var huge = RecoveryPolicy.plan(state(18, 18, 1, 20), Double.MAX_VALUE, true);
        eq(huge.hungerUnits(), 2, "Huge finite budget remains native-food bounded");
        eq(huge.healthUnits(), 16, "Huge finite budget remains saturation bounded");
        eq(RecoveryPolicy.settle(huge, 2, 16).saturationCost(), 18, "Huge request cannot create or double-spend saturation");
    }

    private static void suppressedAndInvalid() {
        var s = state(18, 10, 10, 20);
        var suppressed = RecoveryPolicy.plan(s, 16, false);
        eq(suppressed.hungerUnits(), 0, "Suppressed maim-mending cannot grant hunger");
        eq(suppressed.healthUnits(), 0, "Suppressed maim-mending cannot grant HP");
        eq(RecoveryPolicy.settle(suppressed, 0, 0).saturationCost(), 0, "Suppressed action has no debit");
        reject(() -> RecoveryPolicy.settle(suppressed, 1, 0), "Suppression cannot be bypassed by an observed hunger write");
        reject(() -> RecoveryPolicy.settle(suppressed, 0, 1), "Suppression cannot be bypassed by an observed HP write");
        eq(RecoveryPolicy.plan(state(20, 20, 0, 20), 16, true).maximumSaturationCost(), 0,
                "Ordinary/earned policy never revives dead player or impersonates final respawn");
        eq(RecoveryPolicy.plan(s, 0, true).maximumSaturationCost(), 0, "Zero input has zero authorization");
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1}) {
            reject(() -> RecoveryPolicy.plan(s, bad, true), "Invalid earned budget rejected");
            reject(() -> RecoveryPolicy.settle(RecoveryPolicy.plan(s, 4, true), 0, bad), "Invalid actual HP rejected");
            reject(() -> state(20, bad, 10, 20), "Invalid saturation rejected");
            reject(() -> state(20, 10, bad, 20), "Invalid health rejected");
            reject(() -> state(20, 10, 10, bad), "Invalid maximum health rejected");
        }
        reject(() -> state(-1, 0, 10, 20), "Negative hunger rejected");
        reject(() -> state(21, 0, 10, 20), "Above-native hunger rejected");
        reject(() -> state(10, 11, 10, 20), "Oversaturation not silently admitted");
        reject(() -> state(20, 20, 21, 20), "Above-max HP rejected");
        reject(() -> state(20, 20, 0, 0), "Zero maximum health rejected");
        reject(() -> RecoveryPolicy.ordinary(s, -1, true), "Negative clock rejected");
    }

    private static void ordinaryCadence() {
        var s = state(18, 18, 10, 20);
        for (int ticks = 0; ticks < 80; ticks++)
            eq(RecoveryPolicy.ordinary(s, ticks, true).maximumSaturationCost(), 0, "No fast ten-tick parallel branch");
        var pulse = RecoveryPolicy.ordinary(s, 80, true);
        eq(pulse.hungerUnits(), 1, "Tick eighty authorizes one hunger unit");
        eq(pulse.healthUnits(), 0, "Same ordinary pulse cannot also buy HP");
        eq(RecoveryPolicy.ordinary(s, Integer.MAX_VALUE, true).budget(), 1, "Missed intervals do not become catch-up bursts");
        eq(RecoveryPolicy.ordinary(s, 80, false).maximumSaturationCost(), 0, "Due cadence cannot override suppression");
        int elapsed = 0, pulses = 0;
        double paid = 0;
        // Pure controller exercise only: NOT a claim that an actual native tick controller exists.
        for (int tick = 1; tick <= 320; tick++) {
            var plan = RecoveryPolicy.ordinary(s, ++elapsed, true);
            if (elapsed < 80) {
                eq(plan.maximumSaturationCost(), 0, "No work between ordinary pulses");
                continue;
            }
            elapsed = 0; pulses++;
            var receipt = RecoveryPolicy.settle(plan, plan.hungerUnits(), plan.healthUnits());
            s = state(s.hunger() + receipt.hungerGain(), receipt.remainingSaturation(), s.health() + receipt.healthGain(), s.maximumHealth());
            paid += receipt.saturationCost();
        }
        eq(pulses, 4, "One pulse every eighty ticks, both phases");
        eq(s.hunger(), 20, "First two pulses buy missing hunger");
        eq(s.health(), 12, "Next two pulses buy two actual HP");
        eq(paid, 4, "Four restored units cost exactly four saturation");
        eq(s.saturation(), 14, "No second exhaustion-style debit");
    }

    private static void independentGridOracle() {
        var random = new Random(0x5265636f76657279L);
        for (int i = 0; i < 20_000; i++) {
            int food = random.nextInt(21);
            double sat = random.nextInt(food * 8 + 1) / 8.0;
            double max = 1 + random.nextInt(40);
            double health = random.nextInt((int) max * 8 + 1) / 8.0;
            double budget = random.nextInt(513) / 8.0;
            boolean admitted = random.nextBoolean();
            var before = state(food, sat, health, max);
            var plan = RecoveryPolicy.plan(before, budget, admitted);
            // Independent iterative paid-hunger reference, then bounded remaining HP.
            int expectedFood = 0, currentFood = food;
            double reserve = sat, remaining = budget, expectedHP = 0;
            if (admitted && health > 0) {
                while (currentFood < 20 && reserve >= 1 && remaining >= 1) {
                    currentFood++; expectedFood++; reserve--; remaining--;
                }
                if (currentFood == 20) expectedHP = Math.min(max - health, Math.min(reserve, remaining));
            }
            eq(plan.hungerUnits(), expectedFood, "Independent exact hunger oracle");
            eq(plan.healthUnits(), expectedHP, "Independent exact HP oracle");
            check(plan.maximumSaturationCost() <= sat && plan.maximumSaturationCost() <= budget, "Plan cannot mint a reserve");
            var receipt = RecoveryPolicy.settle(plan, expectedFood, expectedHP);
            eq(receipt.saturationCost(), expectedFood + expectedHP, "Exact observed 1:1 conservation");
            eq(receipt.remainingSaturation() + receipt.saturationCost(), sat, "Reserve conservation");
            eq(receipt.unusedBudget() + receipt.saturationCost(), budget, "Budget conservation");
            if (expectedHP > 0) eq(food + expectedFood, 20, "Actual full hunger prerequisite");
            var canceled = RecoveryPolicy.settle(plan, 0, 0);
            eq(canceled.saturationCost(), 0, "Complete cancellation is always free of debit");
            eq(canceled.remainingSaturation(), sat, "Complete cancellation leaves all reserve");
        }
    }

    private static RecoveryPolicy.State state(int food, double sat, double hp, double max) {
        return new RecoveryPolicy.State(food, sat, hp, max);
    }
    private static void eq(double actual, double expected, String message) { check(actual == expected, message + ": " + actual + " != " + expected); }
    private static void check(boolean condition, String message) { assertions++; if (!condition) throw new AssertionError(message); }
    private static void reject(Runnable operation, String message) {
        assertions++;
        try { operation.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }
}
