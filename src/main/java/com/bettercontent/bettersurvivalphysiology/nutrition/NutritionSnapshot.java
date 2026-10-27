package com.bettercontent.bettersurvivalphysiology.nutrition;


public record NutritionSnapshot(float proteins, float grains, float fruits, float fats, float spice, float dairy) {
    public static final NutritionSnapshot EMPTY = new NutritionSnapshot(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

    public float actual(Group group) {
        return switch (group) {
            case FRUITS -> fruits;
            case GRAINS -> grains;
            case PROTEINS -> proteins;
            case FATS -> fats;
            case SPICE -> spice;
            case DAIRY -> dairy;
        };
    }

    public enum Group {
        PROTEINS("proteins"),
        GRAINS("grains"),
        FRUITS("fruits"),
        FATS("fats"),
        SPICE("spice"),
        DAIRY("dairy");

        public final String id;

        Group(String id) {
            this.id = id;
        }
    }
}
