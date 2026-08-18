package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.section.SignalState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Periodically pushes the resolved signal states of each server level to its players, so the client F3 overlay can
 * color signal markers. Broadcasts at most once per second per level, and only when the state set actually changed.
 */
public class RailSignalsBroadcaster {
    private static final int INTERVAL_TICKS = 20;

    private final Map<ResourceKey<Level>, Set<SignalState>> lastSent = new HashMap<>();
    private int ticks;

    public void onServerTick(MinecraftServer server) {
        ticks++;
        if (ticks % INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Set<SignalState> states = RailNetworkManager.of(level).snapshot().signalStates();
            Set<SignalState> previous = lastSent.put(level.dimension(), Set.copyOf(states));
            if (Objects.equals(previous, states)) {
                continue;
            }
            List<SignalState> ordered = states.stream()
                    .sorted(Comparator.comparing(state -> state.id().value()))
                    .toList();
            PacketDistributor.sendToPlayersInDimension(level, new RailSignalsPayload(ordered));
        }
    }
}