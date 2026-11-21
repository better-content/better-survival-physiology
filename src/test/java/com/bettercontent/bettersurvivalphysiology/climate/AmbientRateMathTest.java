package com.bettercontent.bettersurvivalphysiology.climate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmbientRateMathTest {
    private static final double TARGET = 100D / 72_000D;

    private static double hot(double world, double nativeRate) {
        return AmbientRateMath.adjustHarmfulDrift(nativeRate, 2D, world, 0.4D, 1.5D, 1D, 0D);
    }

    @Test void ambientOnlyAndCancelingSourcesShareTheSameSlowPace() {
        double nativeRate = 0.5D / 7D;
        assertEquals(TARGET, hot(2D, nativeRate), 1e-12);
        // Equal opposing emissions leave final WORLD at 2.0; the net contribution is zero.
        assertEquals(-TARGET, AmbientRateMath.adjustHarmfulDrift(-0.2D, -1D, -1D, 0.4D, 1.5D, 1D, 0D), 1e-12);
    }

    @Test void weakAndStrongAidingSourcesBlendContinuouslyWithoutNativeSpeedSwitch() {
        double ambient = hot(2D, 0.5D / 7D);
        double weakNative = 0.6D / 7D;
        double strongNative = 1.5D / 7D;
        double weak = hot(2.1D, weakNative);
        double stronger = hot(3D, strongNative);
        assertTrue(weak > ambient && weak < weakNative);
        assertTrue(stronger > weak && stronger < strongNative);
        // Local excess stays at native strength; only the ambient share is slowed.
        assertEquals(TARGET + 0.1D / 7D, weak, 1e-12);
        assertEquals(TARGET + 1D / 7D, stronger, 1e-12);
        double dominantNative = 10.5D / 7D;
        double dominant = hot(12D, dominantNative);
        assertTrue(dominant > dominantNative * 0.9D && dominant < dominantNative,
                "a dominant local source should retain nearly all its native strength");
    }

    @Test void opposingSourcesAndEquipmentInsulationCannotAccelerateHarmfulDrift() {
        double ambient = hot(2D, 0.5D / 7D);
        double opposingNative = 0.4D / 7D;
        assertTrue(hot(1.9D, opposingNative) < ambient);
        assertTrue(hot(1.9D, opposingNative) < opposingNative);
        assertEquals(hot(2.1D, 0.6D / 7D) * 0.25D,
                hot(2.1D, 0.6D / 7D * 0.25D), 1e-12,
                "native insulation must be retained when the rate is retimed");
        assertEquals(hot(2D, 0.5D / 7D) * 0.5D,
                hot(2D, 0.5D / 7D * 0.5D), 1e-12,
                "a native ambient rate modifier must still compose with the slowdown");
    }

    @Test void recoverySafeAmbientAndInvalidInputsRemainNative() {
        assertEquals(0.1D, AmbientRateMath.adjustHarmfulDrift(0.1D, 2D, 2D, 0.4D, 1.5D, 1D, -10D));
        assertEquals(-0.1D, AmbientRateMath.adjustHarmfulDrift(-0.1D, 1D, 0.2D, 0.4D, 1.5D, 1D, 10D));
        assertEquals(0.1D, AmbientRateMath.adjustHarmfulDrift(0.1D, 1D, 2D, 0.4D, 1.5D, 1D, 0D));
        assertEquals(0.1D, AmbientRateMath.adjustHarmfulDrift(0.1D, 2D, 1D, 0.4D, 1.5D, 1D, 0D));
        assertEquals(0.1D, AmbientRateMath.adjustHarmfulDrift(0.1D, 2D, 2D, 0.4D, 1.5D, 0D, 0D));
        assertEquals(0.1D, AmbientRateMath.adjustHarmfulDrift(0.1D, Double.NaN, 2D, 0.4D, 1.5D, 1D, 0D));
    }
}
