package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;

public final class MetabolicMath {
    public static final int SUGAR_HALF_LIFE_TICKS = 4 * 60 * 20;
    public static final int ALCOHOL_CLEAR_TICKS = 20 * 60 * 20;

    private MetabolicMath() {}

    public static double amplification(double sugar) {
        return 1.0 + clamp01(sugar);
    }

    public static double sugarDrain(double sugar) {
        return Math.pow(8.0, clamp01(sugar));
    }

    public static double alcoholReduction(double alcohol, double sugar) {
        return Math.min(0.40, 0.40 * clamp01(alcohol) * amplification(sugar));
    }

    public static double alcoholImpairment(double alcohol, double sugar) {
        return Math.min(1.0, Math.max(0.0, (clamp01(alcohol) - 0.30) / 0.70) * amplification(sugar));
    }

    public static double tickSugar(double sugar) {
        int halfLife = ticks(configured(SalienceConfig.SUGAR_HALF_LIFE_MINUTES, 4.0));
        return clamp01(sugar * Math.pow(0.5, 1.0 / halfLife));
    }

    public static double tickAlcohol(double alcohol) {
        return clamp01(alcohol - 1.0 / ticks(configured(SalienceConfig.ALCOHOL_CLEAR_MINUTES, 20.0)));
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
