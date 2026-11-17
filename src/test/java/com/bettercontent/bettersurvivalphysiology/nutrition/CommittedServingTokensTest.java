package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommittedServingTokensTest {
    @Test void oneNativeCallbackCannotReplayButTwoSameItemSameTickActionsRemainIndependent() {
        var tokens = new CommittedServingTokens();
        Object first = new Object(), second = new Object();
        assertTrue(tokens.claim(first));
        assertFalse(tokens.claim(first));
        assertTrue(tokens.claim(second));
        assertFalse(tokens.claim(second));
    }

    @Test void usesObjectIdentityRatherThanValueEqualityAndBoundsTheSessionWindow() {
        var tokens = new CommittedServingTokens();
        Object first = new String("same"), distinct = new String("same");
        assertTrue(tokens.claim(first));
        assertTrue(tokens.claim(distinct));
        assertFalse(tokens.claim(first));
        for (int i = 0; i < 128; i++) assertTrue(tokens.claim(new Object()));
        // A stale native event is no longer held forever; the same session does not
        // promise protection against an impossible callback replay after 128 commits.
        assertTrue(tokens.claim(first));
        assertThrows(NullPointerException.class, () -> tokens.claim(null));
    }
}
