package com.bettercontent.bettersurvivalphysiology.metabolism;

import net.minecraft.nbt.CompoundTag;

public final class MetabolicState {
    private static final String SUGAR = "sugar";
    private static final String DEBT = "debt";
    private static final String ALCOHOL = "alcohol";
    private static final String NUTRITION_THREAD_TOKEN = "nutrition_thread_token";
    private static final String NUTRITION_WARNING_PUBLISHED = "nutrition_warning_published";

    public double sugar;
    public double debt;
    public double alcohol;
    public String nutritionThreadToken = "";
    public boolean nutritionWarningPublished;

    public int heavyBlowCooldown;
    public int enduranceReserveCooldown;
    public int enduranceReserveTicks;
    public int weatheredCooldown;
    public int dairyCleanseCooldown;
    public int sprintTicks;
    public int workSequence;
    public long lastBreakTick = -100L;
    public long lastAttackTick = -100L;
    public transient java.util.UUID heavyBlowTarget;
    public transient long heavyBlowAttackTick = -1L;
    public transient int lastPresentationFlags = -1;
    public transient byte[] lastNutritionTiers = {-1, -1, -1, -1, -1, -1};

    public void tickTransient() {
        sugar = MetabolicMath.tickSugar(sugar);
        alcohol = MetabolicMath.tickAlcohol(alcohol);
        debt = MetabolicMath.tickDebt(debt, sugar);
        heavyBlowCooldown = decrement(heavyBlowCooldown);
        enduranceReserveCooldown = decrement(enduranceReserveCooldown);
        enduranceReserveTicks = decrement(enduranceReserveTicks);
        weatheredCooldown = decrement(weatheredCooldown);
        dairyCleanseCooldown = decrement(dairyCleanseCooldown);
    }

    public void addSugar(double amount) {
        sugar = MetabolicMath.clamp01(sugar + Math.max(0.0, amount));
    }

    public void addAlcohol(double amount) {
        alcohol = MetabolicMath.clamp01(alcohol + Math.max(0.0, amount));
    }

    public void addDebt(double amount) {
        debt = MetabolicMath.clamp01(debt + Math.max(0.0, amount));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(SUGAR, sugar);
        tag.putDouble(DEBT, debt);
        tag.putDouble(ALCOHOL, alcohol);
        if (validToken(nutritionThreadToken)) tag.putString(NUTRITION_THREAD_TOKEN, nutritionThreadToken);
        tag.putBoolean(NUTRITION_WARNING_PUBLISHED, nutritionWarningPublished);
        tag.putInt("heavy_blow_cd", heavyBlowCooldown);
        tag.putInt("endurance_reserve_cd", enduranceReserveCooldown);
        tag.putInt("weathered_cd", weatheredCooldown);
        tag.putInt("dairy_cleanse_cd", dairyCleanseCooldown);
        return tag;
    }

    public static MetabolicState load(CompoundTag tag) {
        MetabolicState state = new MetabolicState();
        state.sugar = MetabolicMath.clamp01(tag.getDouble(SUGAR));
        state.debt = MetabolicMath.clamp01(tag.getDouble(DEBT));
        state.alcohol = MetabolicMath.clamp01(tag.getDouble(ALCOHOL));
        String nutritionThreadToken = tag.getString(NUTRITION_THREAD_TOKEN);
        state.nutritionThreadToken = validToken(nutritionThreadToken) ? nutritionThreadToken : "";
        state.nutritionWarningPublished = !state.nutritionThreadToken.isBlank()
                && tag.getBoolean(NUTRITION_WARNING_PUBLISHED);
        state.heavyBlowCooldown = nonnegative(tag.getInt("heavy_blow_cd"));
        state.enduranceReserveCooldown = nonnegative(tag.getInt("endurance_reserve_cd"));
        state.weatheredCooldown = nonnegative(tag.getInt("weathered_cd"));
        state.dairyCleanseCooldown = nonnegative(tag.getInt("dairy_cleanse_cd"));
        return state;
    }

    private static int decrement(int ticks) { return Math.max(0, ticks - 1); }
    private static int nonnegative(int value) { return Math.max(0, value); }
    private static boolean validToken(String value) {
        return value != null && !value.isBlank() && value.length() <= 128
                && value.chars().allMatch(character -> character >= 0x21 && character <= 0x7e);
    }
}
