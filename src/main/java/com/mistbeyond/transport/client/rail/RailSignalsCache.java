package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cache of the latest signal states received from the server (see {@code RailSignalsPayload}). The F3
 * debug overlay reads it to color signal markers; the value is swapped atomically so concurrent reads are safe.
 */
public class RailSignalsCache {
    private static volatile Map<SignalId, SignalAspect> aspects = Map.of();

    private RailSignalsCache() {
    }

    public static void update(List<SignalState> states) {
        Map<SignalId, SignalAspect> map = new HashMap<>();
        for (SignalState state : states) {
            map.put(state.id(), state.aspect());
        }
        aspects = Map.copyOf(map);
    }

    public static void clear() {
        aspects = Map.of();
    }

    public static @Nullable SignalAspect aspectOf(SignalId id) {
        return aspects.get(id);
    }
}