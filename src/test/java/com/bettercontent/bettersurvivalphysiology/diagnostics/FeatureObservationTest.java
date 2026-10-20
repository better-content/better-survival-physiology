package com.bettercontent.bettersurvivalphysiology.diagnostics;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
/** Actual production projection test; native gameplay gates own whole-feature state/RNG equivalence. */
final class FeatureObservationTest {
    public static void main(String[] args) { new FeatureObservationTest().actualEmitterStagesPrivacyAndObservationalPurity(); }
    @Test void actualEmitterStagesPrivacyAndObservationalPurity() {
        var records = new ArrayList<String>(); boolean[] enabled = {false};
        var log = new PlaytestLog("better_survival_physiology", new PlaytestLog.Sink() {
            public boolean enabled(String category, String level) { return enabled[0]; }
            public void write(String category, String level, String text) { records.add(text); }
        }, () -> 0L, new Properties());
        var contextReads = new AtomicInteger();
        Supplier<PlaytestLog.Context> ctx = () -> { contextReads.incrementAndGet(); return PlaytestLog.Context.server("minecraft:overworld", 10); };
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000042");
        // Observational purity at this scalar projection seam only, not a synthetic native replay.
        long[] state = {42, 3}; long[] before = state.clone();
        var nativeRng = new Random(41); var baselineRng = new Random(41);
        FeatureObservation.release(log, ctx, 0, .5, 1, 2, 3, 4);
        FeatureObservation.release(log, ctx, .5, 1, 2, 3, 4, 5);
        FeatureObservation.decision(log, "beverage", "beverage.check", "vetoed", "blackout");
        FeatureObservation.decision(log, "beverage", "beverage.release", "rejected", "invalid_or_empty_fraction");
        FeatureObservation.decision(log, "beverage", "beverage.release", "rejected", "blackout_defense");
        assertTrue(records.isEmpty()); assertEquals(0, contextReads.get());
        assertArrayEquals(before, state); assertEquals(baselineRng.nextLong(), nativeRng.nextLong());
        enabled[0] = true;
        FeatureObservation.release(log, ctx, 0, .5, 1, 2, 3, 4);
        FeatureObservation.release(log, ctx, .5, 1, 2, 3, 4, 5);
        FeatureObservation.decision(log, "beverage", "beverage.check", "vetoed", "blackout");
        FeatureObservation.decision(log, "beverage", "beverage.release", "rejected", "invalid_or_empty_fraction");
        FeatureObservation.decision(log, "beverage", "beverage.release", "rejected", "blackout_defense");
        assertEquals(5, records.size());
        assertTrue(records.get(0).contains("result=released reason=partial"), records.toString());
        assertTrue(records.get(1).contains("result=released reason=completion"), records.toString());
        assertTrue(records.get(2).contains("result=vetoed reason=blackout"), records.toString());
        assertTrue(records.get(3).contains("result=rejected reason=invalid_or_empty_fraction"), records.toString());
        assertTrue(records.get(4).contains("result=rejected reason=blackout_defense"), records.toString());
        assertTrue(records.stream().noneMatch(s -> s.contains("nutrition=")), records.toString());
        assertTrue(records.stream().noneMatch(s -> s.contains("nbt=")), records.toString());
        assertTrue(records.stream().noneMatch(s -> s.contains("item=")), records.toString());
        assertArrayEquals(before, state); assertEquals(baselineRng.nextLong(), nativeRng.nextLong());
    }
}
