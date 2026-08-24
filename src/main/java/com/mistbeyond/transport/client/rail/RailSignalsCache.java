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
 * debug overlay reads it to color signal markers and report ERROR indicators; the value is swapped atomically so
 * concurrent reads are safe.
 */
public class RailSignalsCache {
    private static volatile Map<SignalId, SignalState> states = Map.of();

    private RailSignalsCache() {
    }

    public static void update(List<SignalState> received) {
        Map<SignalId, SignalState> map = new HashMap<>();
        for (SignalState state : received) {
            map.put(state.id(), state);
        }
        states = Map.copyOf(map);
    }

    public static void clear() {
        states = Map.of();
    }

    public static @Nullable SignalAspect aspectOf(SignalId id) {
        SignalState state = states.get(id);
        return state == null ? null : state.aspect();
    }

    public static boolean errorOf(SignalId id) {
        SignalState state = states.get(id);
        return state != null && state.error();
    }
}