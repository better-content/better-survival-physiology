package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetabolicMathTest {
    @Test
    void ordinaryNutritionDecayRemainsDietOwned() throws Exception {
        String math = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/metabolism/MetabolicMath.java"));
        String config = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/config/SalienceConfig.java"));
        String bridge = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/nutrition/DietBridge.java"));
        for (String method : new String[] {"nutrientDecayPerTick", "baselineNutrientDecayPerTick", "nutrientDecayMultiplier"}) {
            assertFalse(hasPublicMethod(math, method), method);
        }
        assertFalse(hasPublicField(math, "BASE_NUTRIENT_DECAY_PER_MINUTE"));
        assertFalse(hasPublicField(config, "BASE_DECAY_PER_MINUTE"));
        assertFalse(hasPublicField(config, "DECAY_EXPONENT"));
        assertTrue(hasPublicMethod(bridge, "snapshot"));
    }

    @Test
    void sugarAmplifiesBenefitsAndExponentiallyDrainsNutrition() {
        assertEquals(2.0, MetabolicMath.amplification(1.0), 1.0e-9);
        assertEquals(8.0, MetabolicMath.sugarDrain(1.0), 1.0e-9);
        assertTrue(MetabolicMath.sugarDrain(.8) > 2 * MetabolicMath.sugarDrain(.4));
    }

    @Test
    void alcoholProtectionIsLinearAndImpairmentRises() {
        assertEquals(.2, MetabolicMath.alcoholReduction(.5, 0), 1.0e-9);
        assertEquals(.4, MetabolicMath.alcoholReduction(1, 1), 1.0e-9);
        assertEquals(1.0, MetabolicMath.alcoholImpairment(1.0, 0), 1.0e-9);
        assertTrue(MetabolicMath.alcoholImpairment(0.9, 0) > MetabolicMath.alcoholImpairment(0.5, 0));
    }

    @Test
    void halfLivesMatchTheDesignContract() {
        double sugar = 1.0;
        for (int i = 0; i < MetabolicMath.SUGAR_HALF_LIFE_TICKS; i++) sugar = MetabolicMath.tickSugar(sugar);
        assertEquals(0.5, sugar, 1.0e-8);

    }

    private static boolean hasPublicMethod(String source, String name) {
        return source.matches("(?s).*\\bpublic\\s+(?:static\\s+)?[^;{}]*\\b" + name + "\\s*\\(.*");
    }

    private static boolean hasPublicField(String source, String name) {
        return source.matches("(?s).*\\bpublic\\s+(?:static\\s+)?[^;{}()]*\\b" + name + "\\s*(?:=|;|,).*");
    }
}
