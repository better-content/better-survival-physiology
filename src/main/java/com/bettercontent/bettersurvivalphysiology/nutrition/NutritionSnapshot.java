package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;

public record NutritionSnapshot(float proteins, float grains, float fruits, float fats, float vegetables, float dairy) {
    public static final NutritionSnapshot EMPTY = new NutritionSnapshot(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

    public double effective(Group group, MetabolicState state) {
        return MetabolicMath.effectiveNutrient(actual(group), state.sugar, state.debt);
    }

    public float actual(Group group) {
        return switch (group) {
            case FRUITS -> fruits;
            case GRAINS -> grains;
            case PROTEINS -> proteins;
            case FATS -> fats;
            case VEGETABLES -> vegetables;
            case DAIRY -> dairy;
        };
    }

    public enum Group {
        PROTEINS("proteins"),
        GRAINS("grains"),
        FRUITS("fruits"),
        FATS("fats"),
        VEGETABLES("vegetables"),
        DAIRY("dairy");

        public final String id;

        Group(String id) {
            this.id = id;
        }
    }
}
