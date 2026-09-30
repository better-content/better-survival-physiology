package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientMetabolicStateTest {
    @AfterEach
    void clearState() {
        ClientMetabolicState.reset();
    }

    @Test
    void readinessDoesNotExposeStaleServerStateAcrossDisconnects() {
        ClientMetabolicState.reset();
        assertFalse(ClientMetabolicState.isReady());

        ClientMetabolicState.accept(MetabolicSyncPacket.EMPTY);
        assertTrue(ClientMetabolicState.isReady());
        assertSame(MetabolicSyncPacket.EMPTY, ClientMetabolicState.snapshot());

        ClientMetabolicState.reset();
        assertFalse(ClientMetabolicState.isReady());
        assertSame(MetabolicSyncPacket.EMPTY, ClientMetabolicState.snapshot());
    }
}
