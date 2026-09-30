package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionDrain;

public final class NutritionEstimates {
    public static final int OVER_THIRTY_MINUTES = 1801;

    private NutritionEstimates() {}

    public static int nutrientSeconds(double value, double threshold, double sugar) {
        return NutritionDrain.secondsUntilBelow(value, threshold, SalienceConfig.FOURTH, sugar);
    }

    public static int sugarSeconds(double sugar) {
        if (sugar <= 0.01) return -1;
        double boundary = sugar * 0.5;
        double current = sugar;
        double retention = Math.pow(MetabolicMath.tickSugar(1.0), 20.0);
        for (int second = 1; second <= 1800; second++) {
            current *= retention;
            if (current < boundary) return second;
        }
        return OVER_THIRTY_MINUTES;
    }

    public static int alcoholSeconds(double alcohol) {
        double boundary = alcohol >= 1.0 ? 0.85 : alcohol >= 0.90 ? 0.90
                : alcohol >= 0.50 ? 0.50 : alcohol > 0.30 ? 0.30 : 0.0;
        if (boundary == 0.0) return -1;
        double current = alcohol;
        double loss = (1.0 - MetabolicMath.tickAlcohol(1.0)) * 20.0;
        for (int second = 1; second <= 1800; second++) {
            current = Math.max(0.0, current - loss);
            if (current < boundary) return second;
        }
        return OVER_THIRTY_MINUTES;
    }

}
