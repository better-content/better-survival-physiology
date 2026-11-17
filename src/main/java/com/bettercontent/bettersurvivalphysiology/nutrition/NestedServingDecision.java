package com.bettercontent.bettersurvivalphysiology.nutrition;

/** A removed bag slot is paid before the inner native completion. A no-op reusable
 * inner item cannot impersonate a successful item Finish event. */
public final class NestedServingDecision {
    private NestedServingDecision() {}
    public static boolean notifyNativeFinish(boolean server, boolean realPlayer, boolean changedResult) {
        return server && realPlayer && changedResult;
    }
}
