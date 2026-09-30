package com.bettercontent.bettersurvivalphysiology.presentation;

public enum NutritionTier {
    BUILDING, FIRST, SECOND, THIRD, FOURTH;

    public static NutritionTier of(double value) {
        if (value >= 0.80) return FOURTH;
        if (value >= 0.60) return THIRD;
        if (value >= 0.40) return SECOND;
        if (value >= 0.20) return FIRST;
        return BUILDING;
    }

    public double floor() {
        return switch (this) {
            case FOURTH -> 0.80;
            case THIRD -> 0.60;
            case SECOND -> 0.40;
            case FIRST -> 0.20;
            case BUILDING -> 0.0;
        };
    }
}
