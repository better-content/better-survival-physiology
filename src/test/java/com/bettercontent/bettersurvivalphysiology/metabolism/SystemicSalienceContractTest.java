package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemicSalienceContractTest {
    @Test
    void dietUsesTheFullGuideScreen() throws IOException {
        Path clientRoot = Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/client");
        String opening = Files.readString(clientRoot.resolve("DietScreenEvents.java"));

        assertTrue(Files.exists(clientRoot.resolve("SystemicDietScreen.java")));
        assertTrue(opening.contains("ScreenEvent.Opening"));
        assertTrue(opening.contains("setNewScreen"));
        assertFalse(Files.exists(clientRoot.resolve("DietBenefitsOverlay.java")));
    }

    @Test
    void thresholdsPreservePreparationOrdering() {
        assertTrue(SalienceConfig.FIRST < SalienceConfig.SECOND);
        assertTrue(SalienceConfig.SECOND < SalienceConfig.THIRD);
        assertTrue(SalienceConfig.THIRD < SalienceConfig.FOURTH);
    }

    @Test
    void readinessStripContainsExactlyEightSquareCells() throws IOException {
        try (var stream = getClass().getResourceAsStream("/assets/better_survival_physiology/textures/gui/nutrition_states.png")) {
            assertTrue(stream != null);
            var image = ImageIO.read(stream);
            assertTrue(image != null);
            assertTrue(image.getHeight() == 18);
            assertTrue(image.getWidth() == image.getHeight() * 8);
        }
    }
}
