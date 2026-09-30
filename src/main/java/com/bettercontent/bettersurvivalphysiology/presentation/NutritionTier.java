package com.bettercontent.bettersurvivalphysiology.presentation;

public enum NutritionTier {
    BUILDING, SUPPORTED, PREPARED, FEAST;

    public static NutritionTier of(double value, double ordinary, double prepared, double feast) {
        if (value >= feast) return FEAST;
        if (value >= prepared) return PREPARED;
        if (value >= ordinary) return SUPPORTED;
        return BUILDING;
    }

    public double floor(double ordinary, double prepared, double feast) {
        return switch (this) {
            case FEAST -> feast;
            case PREPARED -> prepared;
            case SUPPORTED -> ordinary;
            case BUILDING -> 0.0;
        };
    }
}
