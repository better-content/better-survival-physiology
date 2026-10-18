package com.bettercontent.bettersurvivalphysiology.metabolism;

/** Continuous nutrition potency. Thresholds unlock behaviours, never these baseline benefits. */
public final class NutritionEffects {
    private NutritionEffects() {}

    public static double potency(double nutrient, double nectar) {
        return MetabolicMath.clamp01(nutrient) * MetabolicMath.amplification(nectar);
    }

    public static double meleeDamage(double nutrient, double nectar) { return .40 * potency(nutrient, nectar); }
    public static double miningSpeed(double nutrient, double nectar) { return potency(nutrient, nectar); }
    public static double movementSpeed(double nutrient, double nectar) { return .25 * potency(nutrient, nectar); }
    public static double resourceSavings(double nutrient, double nectar) { return Math.min(.85, .50 * potency(nutrient, nectar)); }
    public static double temperatureResistance(double nutrient, double nectar) { return Math.min(.85, .70 * potency(nutrient, nectar)); }
    public static double harmfulExpiryBonus(double nutrient, double nectar) { return potency(nutrient, nectar); }
}
