package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.betterspicyfarmers.core.SpiceMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.NectarGain;
import java.util.Map;

/** Dependency-free executable assertions against actual production math/snapshot/Nectar code. */
public final class SpiceBehaviorPureTest {
    private static int assertions;
    public static void main(String[] args) {
        run();
        System.out.println("SpiceBehaviorPureTest PASS assertions=" + assertions);
    }
    public static void run() {
        assertions = 0;
        var n = new NutritionSnapshot(.1f, .2f, .3f, .4f, .5f, .6f);
        near(n.actual(NutritionSnapshot.Group.SPICE), .5);
        check(NutritionSnapshot.Group.SPICE.id.equals("spice"), "Own Spice persistence identity");
        for (var group : NutritionSnapshot.Group.values()) check(!group.id.equals("vegetables"), "Earth not remapped");
        near(NectarGain.capped(.5, Map.of("spice", .2)), .1);
        near(NectarGain.capped(.5, Map.of("vegetables", .01, "spice", .2)), .1);
        near(NectarGain.capped(.5, Map.of("grains", .04, "spice", .2)), .02);
        near(NectarGain.capped(.5, Map.of("vegetables", .01)), .5);
        for (int i = 0; i <= 100; i++) {
            double level = i / 100.0;
            for (double amp : new double[]{1, 1.5, 3, 10}) {
                double mine = SpiceMath.miningBonus(level, amp);
                double move = SpiceMath.movementBonus(level, amp);
                near(mine, Math.min(.4, .4 * level * amp));
                near(move, Math.min(.2, .2 * level * amp));
                check(mine <= .4 && move <= .2, "Nectar remains within final caps");
                near(SpiceMath.miningBonus(level, amp, .15), Math.min(.15, .15 * level * amp));
                near(SpiceMath.movementBonus(level, amp, .07), Math.min(.07, .07 * level * amp));
            }
            double units = i / 10.0;
            near(SpiceMath.hpCost(units), Math.min(8, 4 * units));
            near(SpiceMath.hpCost(units, 2, 5), Math.min(5, 2 * units));
            near(SpiceMath.dietGain(units), Math.min(1, .2 * units));
        }
        for (double units : new double[]{.05, .25, 1, 4, 10}) {
            double hp = 0, gain = 0;
            for (int portion = 0; portion < 10; portion++) {
                double from = portion / 10.0, to = (portion + 1) / 10.0;
                hp += SpiceMath.hpCost(units * (to - from));
                gain += .2 * units * (to - from);
            }
            near(hp, 10 * SpiceMath.hpCost(units / 10)); near(gain, .2 * units);
        }
        for (double[] invalid : new double[][]{{0, 0}, {.5, .4}, {-.1, .5}, {0, 1.1}, {0, Double.NaN}}) {
            near(BeverageNutrition.award(.8, invalid[0], invalid[1]), 0);
            near(8 * BeverageNutrition.award(1, invalid[0], invalid[1]), 0);
        }
        near(SpiceMath.hpCost(1), 4); near(SpiceMath.hpCost(4), 8);
        near(4 * SpiceMath.hpCost(4 * .25), 16); // Per-serving caps are deliberately nonlinear.
        for (double dose : new double[]{0, Double.MIN_VALUE, Double.MIN_NORMAL, .05, .1, .2, 1, 4, 10, 1e100, Double.MAX_VALUE}) {
            double mantissa = BeverageSpiceDose.mantissa(dose), exponent = BeverageSpiceDose.exponent(dose);
            check(mantissa >= 0 && mantissa <= 1 && exponent >= 0 && exponent <= 1, "Native bounded-profile ABI");
            check(Double.doubleToLongBits(dose) == Double.doubleToLongBits(BeverageSpiceDose.decode(mantissa, exponent)),
                    "Canonical dose survives paid-profile capture bit exactly");
        }
        var random = new java.util.Random(0x51c3);
        for (int trial = 0; trial < 10000; trial++) {
            double dose = Double.longBitsToDouble(random.nextLong() & Long.MAX_VALUE);
            if (Double.isFinite(dose)) check(Double.doubleToLongBits(dose) == Double.doubleToLongBits(
                    BeverageSpiceDose.decode(BeverageSpiceDose.mantissa(dose), BeverageSpiceDose.exponent(dose))),
                    "Captured finite random dose roundtrips exactly");
        }
        rejects(() -> BeverageSpiceDose.mantissa(Double.NaN));
        rejects(() -> BeverageSpiceDose.exponent(-1));
        rejects(() -> BeverageSpiceDose.decode(1, 1));
        check(SpiceMath.canEat(19, 19, 0), "Missing hunger");
        check(SpiceMath.canEat(20, 19, 0), "Room for saturation");
        check(SpiceMath.canEat(20, 20, .01), "Positive canonical Spice");
        check(!SpiceMath.canEat(20, 20, 0), "No warning/forced consumption of full plain food");
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> SpiceMath.hpCost(invalid));
            rejects(() -> SpiceMath.dietGain(invalid));
            rejects(() -> SpiceMath.miningBonus(invalid, 1));
            rejects(() -> SpiceMath.movementBonus(1, invalid));
        }
    }
    private static void rejects(Runnable action) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Invalid quantity rejected");
    }
    private static void near(double actual, double expected) {
        check(Math.abs(actual - expected) < .000001, "Expected " + expected + ", got " + actual);
    }
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
}
