package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NestedServingDecisionTest {
    @Test void onlyRealServerPaidChangedInnerCompletionCanNotify() {
        assertTrue(NestedServingDecision.notifyNativeFinish(true, true, true));
        assertFalse(NestedServingDecision.notifyNativeFinish(false, true, true));
        assertFalse(NestedServingDecision.notifyNativeFinish(true, false, true));
        assertFalse(NestedServingDecision.notifyNativeFinish(true, true, false));
    }
}
