package com.bettercontent.bettersurvivalphysiology.api;

import java.util.*;
import java.util.function.BiFunction;

/** Setup-time single-authority resolution, independent of Minecraft for deterministic tests. */
final class ServingProviders<P, S, V> {
    private final Map<String, BiFunction<P, S, Optional<V>>> providers = new LinkedHashMap<>();
    private boolean frozen;
    synchronized void register(String owner, BiFunction<P, S, Optional<V>> provider) {
        if (frozen) throw new IllegalStateException("Serving registration has closed");
        if (providers.putIfAbsent(Objects.requireNonNull(owner), Objects.requireNonNull(provider)) != null)
            throw new IllegalArgumentException("Duplicate serving provider: " + owner);
    }
    synchronized Optional<V> resolve(P player, S stack) {
        frozen = true;
        V result = null;
        for (var provider : providers.values()) {
            var candidate = Objects.requireNonNull(provider.apply(player, stack));
            if (candidate.isEmpty()) continue;
            if (result != null) throw new IllegalStateException("Overlapping serving providers");
            result = candidate.get();
        }
        return Optional.ofNullable(result);
    }
}
