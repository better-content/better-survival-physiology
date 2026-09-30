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
    void sugarOwnsTempoWithoutAmplifyingOtherIdentities() {
        assertEquals(1.0, MetabolicMath.effectiveNutrient(1.0, 1.0, 0.0), 1.0e-9);
        assertEquals(1.0, MetabolicMath.thresholdPotency(1.0), 1.0e-9);
        assertEquals(0.50, MetabolicMath.cooldownMultiplier(1.0, 0.0), 1.0e-9);
    }

    @Test
    void debtSlowsTempoWithoutSuppressingOtherIdentities() {
        assertEquals(1.0, MetabolicMath.effectiveNutrient(1.0, 0.0, 1.0), 1.0e-9);
        assertEquals(1.5, MetabolicMath.cooldownMultiplier(0.0, 1.0), 1.0e-9);
        assertEquals(1.0, MetabolicMath.tickDebt(1.0, 0.25), 1.0e-9);
        assertTrue(MetabolicMath.tickDebt(1.0, 0.24) < 1.0);
    }

    @Test
    void alcoholHasMidpointPeakAndBrutalTopEnd() {
        assertEquals(1.0, MetabolicMath.alcoholPositive(0.5), 1.0e-9);
        assertEquals(0.0, MetabolicMath.alcoholPositive(0.85), 1.0e-9);
        assertEquals(1.0, MetabolicMath.alcoholImpairment(1.0), 1.0e-9);
        assertTrue(MetabolicMath.alcoholImpairment(0.9) > MetabolicMath.alcoholImpairment(0.5));
    }

    @Test
    void halfLivesMatchTheDesignContract() {
        double sugar = 1.0;
        for (int i = 0; i < MetabolicMath.SUGAR_HALF_LIFE_TICKS; i++) sugar = MetabolicMath.tickSugar(sugar);
        assertEquals(0.5, sugar, 1.0e-8);

        double debt = 1.0;
        for (int i = 0; i < MetabolicMath.DEBT_HALF_LIFE_TICKS; i++) debt = MetabolicMath.tickDebt(debt, 0.0);
        assertEquals(0.5, debt, 1.0e-8);
    }

    private static boolean hasPublicMethod(String source, String name) {
        return source.matches("(?s).*\\bpublic\\s+(?:static\\s+)?[^;{}]*\\b" + name + "\\s*\\(.*");
    }

    private static boolean hasPublicField(String source, String name) {
        return source.matches("(?s).*\\bpublic\\s+(?:static\\s+)?[^;{}()]*\\b" + name + "\\s*(?:=|;|,).*");
    }
}
