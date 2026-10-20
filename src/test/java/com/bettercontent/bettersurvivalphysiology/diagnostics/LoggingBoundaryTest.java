package com.bettercontent.bettersurvivalphysiology.diagnostics;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Source/privacy invariant only; native behavior is covered by the owning full gate. */
final class LoggingBoundaryTest {
    @Test void authoritativeFeatureBoundaryAndPrivacyRemainExplicit() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/compat/BeverageCompat.java"));
        source += Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/diagnostics/FeatureObservation.java"));
        assertTrue(source.contains("beverage.check"), "missing authored boundary beverage.check");
        assertTrue(source.contains("beverage.release"), "missing authored boundary beverage.release");
        assertTrue(source.contains("blackout_defense"), "missing authored boundary blackout_defense");
        assertTrue(source.contains("sugar_before"), "missing authored boundary sugar_before");
        assertTrue(source.contains("sugar_after"), "missing authored boundary sugar_after");
        assertFalse(source.contains("PlaytestLog.fields(\"nutrition\""), "private/unobserved field in diagnostic source");
        assertFalse(source.contains("PlaytestLog.fields(\"item\""), "private/unobserved field in diagnostic source");
        assertTrue(source.contains("enabled("), "guard before native diagnostic fields");
    }
}
