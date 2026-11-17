package com.bettercontent.bettersurvivalphysiology.nutrition;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Objects;

/** Server-session replay protection for one real native callback object; not an item/tick key.
 * No persisted food ledger, and two distinct paid bites in one tick remain independent. */
public final class CommittedServingTokens {
    private static final int RECENT = 128;
    private final IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
    private final ArrayDeque<Object> order = new ArrayDeque<>();

    public boolean claim(Object callback) {
        Objects.requireNonNull(callback, "Actual committed serving callback required");
        if (seen.containsKey(callback)) return false;
        seen.put(callback, Boolean.TRUE);
        order.addLast(callback);
        if (order.size() > RECENT) seen.remove(order.removeFirst());
        return true;
    }
}
