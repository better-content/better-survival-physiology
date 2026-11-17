package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NativePaidBatchTest {
    @Test void sumsDistinctCanonicalSourcesBeforeYieldDivisionAndFinalCap() {
        var whole = NativePaidBatch.add(Map.of(), Map.of("carbs", .8));
        whole = NativePaidBatch.add(whole, Map.of("carbs", .8, "proteins", .06));
        assertEquals(1.6, whole.get("carbs"), 1e-10);
        var fourth = NativePaidBatch.perCup(whole, 4);
        assertEquals(.4, fourth.get("carbs"), 1e-10); // NOT clamp(1.6)/4 = .25
        assertEquals(.015, fourth.get("proteins"), 1e-10);
        assertEquals(1, NativePaidBatch.perCup(Map.of("carbs", 24.0), 12).get("carbs"), 1e-10);
        assertThrows(IllegalArgumentException.class,
                () -> NativePaidBatch.add(Map.of(), Map.of("grains", .6)));
        assertThrows(IllegalArgumentException.class,
                () -> NativePaidBatch.perCup(Map.of("carbs", .2), 0));
    }
}
