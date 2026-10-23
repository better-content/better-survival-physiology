package com.bettercontent.bettersurvivalphysiology.diagnostics;
import java.util.function.Supplier;
/** Pure owner-specific projection of already computed native outcomes; no gameplay writes or RNG. */
public final class FeatureObservation {
    private FeatureObservation() {}
    public static void release(PlaytestLog log, Supplier<PlaytestLog.Context> context, double from, double to, double sugarBefore, double sugarAfter, double alcoholBefore, double alcoholAfter) {
        if (!log.enabled("beverage")) return;
        log.debug("beverage", "beverage.release", context.get(), "released", to == 1.0 ? "completion" : "partial", () -> PlaytestLog.fields("from", from, "to", to, "sugar_before", sugarBefore, "sugar_after", sugarAfter, "alcohol_before", alcoholBefore, "alcohol_after", alcoholAfter));
    }
    public static void playerDecision(PlaytestLog log, Supplier<PlaytestLog.Context> context, String event, String result, String reason) {
        if (!log.enabled("beverage")) return;
        log.debug("beverage", event, context.get(), result, reason, () -> "");
    }
    public static void decision(PlaytestLog log, String category, String event, String result, String reason) {
        if (!log.enabled(category)) return;
        log.event(category, event, "server", result, reason, () -> "");
    }
}
