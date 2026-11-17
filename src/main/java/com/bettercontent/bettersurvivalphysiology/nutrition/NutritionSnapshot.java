package com.bettercontent.bettersurvivalphysiology.nutrition;

/** The five authoritative native Diet tracker values; no parallel sugar/alcohol balances. */
public record NutritionSnapshot(float proteins, float carbs, float spice, float vegetables, float alcohol) {
    public static final NutritionSnapshot EMPTY = new NutritionSnapshot(0, 0, 0, 0, 0);

    public float actual(Group group) {
        return switch (group) {
            case PROTEINS -> proteins;
            case CARBS -> carbs;
            case SPICE -> spice;
            case VEGETABLES -> vegetables;
            case ALCOHOL -> alcohol;
        };
    }

    public enum Group {
        PROTEINS("proteins"), CARBS("carbs"), SPICE("spice"), VEGETABLES("vegetables"), ALCOHOL("alcohol");
        public final String id;
        Group(String id) { this.id = id; }
    }
}
