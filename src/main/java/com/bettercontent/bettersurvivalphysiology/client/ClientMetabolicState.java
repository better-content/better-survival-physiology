package com.bettercontent.bettersurvivalphysiology.client;

import com.bettercontent.bettersurvivalphysiology.network.MetabolicSyncPacket;

public final class ClientMetabolicState {
    private static volatile MetabolicSyncPacket snapshot = MetabolicSyncPacket.EMPTY;
    private static volatile boolean ready;

    private ClientMetabolicState() {}

    public static void accept(MetabolicSyncPacket packet) {
        snapshot = packet;
        ready = true;
    }

    public static MetabolicSyncPacket snapshot() {
        return snapshot;
    }

    public static boolean isReady() {
        return ready;
    }

    public static void reset() {
        snapshot = MetabolicSyncPacket.EMPTY;
        ready = false;
    }
}
