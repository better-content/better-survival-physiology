package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionDrain;

public final class NutritionEstimates {
    public static final int OVER_THIRTY_MINUTES = 1801;

    private NutritionEstimates() {}

    public static int nutrientSeconds(double value, double threshold, double sugar) {
        return NutritionDrain.secondsUntilBelow(value, threshold,
                SalienceConfig.value(SalienceConfig.PREPARED_THRESHOLD, 0.75), sugar);
    }

    public static int sugarSeconds(double sugar) {
        double boundary = sugar >= 0.60 ? 0.60 : sugar >= 0.25 ? 0.25 : 0.0;
        if (boundary == 0.0) return -1;
        double current = sugar;
        double retention = Math.pow(MetabolicMath.tickSugar(1.0), 20.0);
        for (int second = 1; second <= 1800; second++) {
            current *= retention;
            if (current < boundary) return second;
        }
        return OVER_THIRTY_MINUTES;
    }

    public static int alcoholSeconds(double alcohol) {
        double boundary = alcohol > 0.65 ? 0.65 : alcohol >= 0.35 ? 0.35 : 0.0;
        if (boundary == 0.0) return -1;
        double current = alcohol;
        double loss = (1.0 - MetabolicMath.tickAlcohol(1.0)) * 20.0;
        for (int second = 1; second <= 1800; second++) {
            current = Math.max(0.0, current - loss);
            if (current < boundary) return second;
        }
        return OVER_THIRTY_MINUTES;
    }

    public static int debtSeconds(double debt, double sugar) {
        if (debt <= 0.05) return -1;
        double currentDebt = debt, currentSugar = sugar;
        double sugarRetention = Math.pow(MetabolicMath.tickSugar(1.0), 20.0);
        double debtRetention = Math.pow(MetabolicMath.tickDebt(1.0, 0.0), 20.0);
        for (int second = 1; second <= 1800; second++) {
            currentSugar *= sugarRetention;
            if (currentSugar < 0.25) currentDebt *= debtRetention;
            if (currentDebt <= 0.05) return second;
        }
        return OVER_THIRTY_MINUTES;
    }
}
