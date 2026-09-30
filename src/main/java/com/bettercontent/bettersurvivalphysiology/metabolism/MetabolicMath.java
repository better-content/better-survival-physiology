package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;

public final class MetabolicMath {
    public static final int SUGAR_HALF_LIFE_TICKS = 4 * 60 * 20;
    public static final int DEBT_HALF_LIFE_TICKS = 10 * 60 * 20;
    public static final int ALCOHOL_CLEAR_TICKS = 20 * 60 * 20;

    private MetabolicMath() {}

    public static double thresholdPotency(double sugar) {
        return 1.0;
    }

    public static double cooldownMultiplier(double sugar, double debt) {
        double s = clamp01(sugar);
        if (s >= 0.60) return 0.50;
        if (s >= 0.25) return 0.67;
        return 1.0 + 0.5 * clamp01(debt);
    }

    public static int adjustedCooldown(int baseTicks, double sugar, double debt) {
        return Math.max(1, (int) Math.round(baseTicks * cooldownMultiplier(sugar, debt)));
    }

    public static double effectiveNutrient(double nutrient, double sugar, double debt) {
        return clamp01(nutrient);
    }

    public static double alcoholPositive(double alcohol) {
        double a = clamp01(alcohol);
        if (a < 0.35 || a > 0.65) return 0.0;
        return Math.max(0.0, 1.0 - Math.abs(a - 0.5) / 0.15);
    }

    public static double alcoholImpairment(double alcohol) {
        double a = clamp01(alcohol);
        if (a <= 0.65) return 0.0;
        double excess = (a - 0.65) / 0.35;
        return excess * excess;
    }

    public static double tickSugar(double sugar) {
        int halfLife = ticks(configured(SalienceConfig.SUGAR_HALF_LIFE_MINUTES, 4.0));
        return clamp01(sugar * Math.pow(0.5, 1.0 / halfLife));
    }

    public static double tickAlcohol(double alcohol) {
        return clamp01(alcohol - 1.0 / ticks(configured(SalienceConfig.ALCOHOL_CLEAR_MINUTES, 20.0)));
    }

    public static double tickDebt(double debt, double sugar) {
        if (sugar >= configured(SalienceConfig.SUGAR_DEBT_GATE, 0.25)) return clamp01(debt);
        int halfLife = ticks(configured(SalienceConfig.DEBT_HALF_LIFE_MINUTES, 10.0));
        return clamp01(debt * Math.pow(0.5, 1.0 / halfLife));
    }

    public static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int ticks(double minutes) {
        return Math.max(1, (int) Math.round(minutes * 60.0 * 20.0));
    }

    private static double configured(net.minecraftforge.common.ForgeConfigSpec.DoubleValue value, double fallback) {
        return SalienceConfig.value(value, fallback);
    }
}
