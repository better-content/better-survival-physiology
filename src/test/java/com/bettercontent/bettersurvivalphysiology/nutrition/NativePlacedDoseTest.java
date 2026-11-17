package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativePlacedDoseTest {
    @Test void onlyWholeItemSupplementalMirrorsArePortioned() {
        assertEquals(.08, NativePlacedDose.perBite(.32, .25), 1e-10);
        assertEquals(.015, NativePlacedDose.sugarMirror(.06, .25, java.util.Map.of("carbs", .08)), 1e-10);
        assertEquals(.005, NativePlacedDose.sugarMirror(.06, .25, java.util.Map.of("carbs", .01)), 1e-10);
        // The old whole-cap→fraction order would incorrectly yield .00125 here.
        assertNotEquals(.00125, NativePlacedDose.sugarMirror(.06, .25, java.util.Map.of("carbs", .01)), 1e-10);
        assertEquals(.0125, NativePlacedDose.perBite(.05, .25), 1e-10);
        assertEquals(.32, NativePlacedDose.perBite(.32, 1.0), 1e-10);
        // The native Diet result/hunger are not arguments to this function: they
        // already describe one actual bite and must never be multiplied again.
        assertThrows(IllegalArgumentException.class, () -> NativePlacedDose.perBite(.32, 0));
        assertThrows(IllegalArgumentException.class, () -> NativePlacedDose.perBite(.32, 1.01));
        assertThrows(IllegalArgumentException.class, () -> NativePlacedDose.perBite(Double.NaN, .25));
    }

    @Test void nativeActiveAndPlayerEligibilityArePreserved() {
        assertTrue(NativePlacedDose.eligible(true, false, false));
        assertFalse(NativePlacedDose.eligible(false, false, false));
        assertFalse(NativePlacedDose.eligible(true, true, false));
        assertFalse(NativePlacedDose.eligible(true, false, true));
    }
}
