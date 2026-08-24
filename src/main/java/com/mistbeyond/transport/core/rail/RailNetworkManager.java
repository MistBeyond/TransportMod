package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.RailNetworkService;
import com.mistbeyond.transport.api.rail.RailNetworkSnapshot;
import com.mistbeyond.transport.api.rail.RailTrainSnapshot;
import com.mistbeyond.transport.api.rail.dispatch.*;
import com.mistbeyond.transport.api.rail.graph.*;
import com.mistbeyond.transport.api.rail.station.RailStationLocator;
import com.mistbeyond.transport.internal.rail.RailNetworkSavedData;
import com.mistbeyond.transport.internal.rail.RailNetworkState;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class RailNetworkManager implements RailNetworkService, AutomaticTrainDriver.Host {
    private final ServerLevel level;
    private final DispatchService dispatch;
    private final Map<RailTrainId, RailTrainAggregate> trains = new LinkedHashMap<>();
    private final Set<GridPos> dirtyCells = new LinkedHashSet<>();
    private final AutomaticTrainDriver automaticDriver = new AutomaticTrainDriver();
    private final ManagedRailGraphView graph = new ManagedRailGraphView();
    private RailStationLocator stationLocator = ignored -> Optional.empty();
    @Nullable
    private RailNetworkSavedData savedData;
    @Nullable
    private TrackGraphSource source;

    public RailNetworkManager(ServerLevel level) {
        this.level = level;
        RailPathfinder pathfinder = new ShortestPathRouter();
        this.dispatch = new DispatchServiceImpl(this.graph, pathfinder);
    }

    public static RailNetworkManager of(ServerLevel level) {
        return RailNetworkSavedData.managerOf(level);
    }

    /**
     * Advances all automatic trains once per server tick.
     */
    public void tickAutomation() {
        automaticDriver.tick(this);
    }

    public void attachSavedData(RailNetworkSavedData savedData) {
        this.savedData = savedData;
    }

    public void restore(RailNetworkState state) {
        trains.clear();
        for (var snapshot : state.trains()) {
            trains.put(snapshot.id(), new RailTrainAggregate(
                    snapshot.id(),
                    snapshot.controlMode(),
                    snapshot.position(),
                    snapshot.derailed(),
                    snapshot.schedule(),
                    Optional.empty(),
                    0
            ));
        }
    }

    public void setSource(TrackGraphSource source) {
        this.source = source;
        this.graph.update(RailGraph.empty());
        this.dirtyCells.clear();
    }

    public void setStationLocator(RailStationLocator stationLocator) {
        this.stationLocator = stationLocator;
    }

    public ServerLevel level() {
        return level;
    }

    @Override
    public RailGraphView graph() {
        return graph;
    }

    public DispatchService dispatch() {
        return dispatch;
    }

    @Override
    public List<RailTrainAggregate> trains() {
        return List.copyOf(trains.values());
    }

    @Override
    public void update(RailTrainAggregate train) {
        trains.put(train.id(), train);
        markSavedDataDirty();
    }

    @Override
    public void restartSchedule(RailTrainId trainId) {
        RailTrainAggregate train = trains.get(trainId);
        if (train == null || train.schedule().isEmpty()) {
            return;
        }
        dispatch.release(trainId);
        startSchedule(trainId, train.schedule().orElseThrow());
    }

    /**
     * World-space position of an automatic train for entity presentation; empty when the train is not automatic.
     */
    public Optional<AutomaticTrainDriver.TrainPresentation> automaticPresentation(RailTrainId trainId) {
        return automaticDriver.presentation(this, trainId);
    }

    public RailGraphView graphAt(GridPos center) {
        ensureGraph(center);
        return graph;
    }

    @Override
    public RailNetworkSnapshot snapshot() {
        Set<RailTrainSnapshot> trainSnapshots = new LinkedHashSet<>();
        for (RailTrainAggregate train : trains.values()) {
            trainSnapshots.add(train.snapshot());
        }
        return new RailNetworkSnapshot(
                graph,
                dispatch.snapshot(),
                trainSnapshots,
                SignalStateResolver.resolve(graph, dispatch.snapshot())
        );
    }

    @Override
    public Optional<RailTrainSnapshot> train(RailTrainId trainId) {
        return Optional.ofNullable(trains.get(trainId)).map(RailTrainAggregate::snapshot);
    }

    public Optional<String> spawnTrain(RailTrainId trainId, GridPos start) {
        if (trains.containsKey(trainId)) {
            return Optional.of("A train with this id already exists: " + trainId);
        }
        ensureGraph(start);
        if (graph.nodeAt(start).isEmpty()) {
            return Optional.of("No track node at " + start);
        }
        trains.put(trainId, RailTrainAggregate.manual(trainId, start));
        markSavedDataDirty();
        return Optional.empty();
    }

    public Optional<String> updatePosition(RailTrainId trainId, GridPos position) {
        RailTrainAggregate train = trains.get(trainId);
        if (train == null) {
            return Optional.of("Unknown train: " + trainId);
        }
        trains.put(trainId, train.withPosition(position));
        markSavedDataDirty();
        return Optional.empty();
    }

    public Optional<String> setControlMode(RailTrainId trainId, RailControlMode controlMode) {
        RailTrainAggregate train = trains.get(trainId);
        if (train == null) {
            return Optional.of("Unknown train: " + trainId);
        }
        if (train.derailed()) {
            return Optional.of("Derailed trains must be reset before changing control mode");
        }
        if (controlMode == RailControlMode.AUTOMATIC) {
            if (train.schedule().isEmpty()) {
                return Optional.of("Automatic mode requires a schedule");
            }
            return startSchedule(trainId, train.schedule().orElseThrow());
        }

        dispatch.release(trainId);
        automaticDriver.discard(trainId);
        trains.put(trainId, train.withControlMode(RailControlMode.MANUAL));
        markSavedDataDirty();
        return Optional.empty();
    }

    public Optional<String> startSchedule(RailTrainId trainId, RailTrainSchedule schedule) {
        RailTrainAggregate train = trains.get(trainId);
        if (train == null) {
            return Optional.of("Unknown train: " + trainId);
        }
        if (train.derailed()) {
            return Optional.of("Derailed trains cannot start a schedule");
        }

        ensureGraph(train.position());
        RailNodeId start = RailGraphs.nearestReachableNode(graph, train.position()).orElse(null);
        if (start == null) {
            return Optional.of("Train is not on a reachable track node");
        }

        List<RailNodeId> stopNodes = locateStops(schedule);
        if (stopNodes.size() != schedule.stops().size()) {
            return Optional.of("One or more schedule stations are not placed in this world");
        }

        List<StopPlan> stops = new ArrayList<>();
        RailNodeId destination;
        if (schedule.type() == com.mistbeyond.transport.api.rail.dispatch.RailTrainScheduleType.LOOP) {
            for (RailNodeId stop : stopNodes) {
                stops.add(new StopPlan(stop, 0));
            }
            destination = stopNodes.getFirst();
        } else {
            for (int i = 0; i < stopNodes.size() - 1; i++) {
                stops.add(new StopPlan(stopNodes.get(i), 0));
            }
            destination = stopNodes.getLast();
        }

        RouteRequest request = new RouteRequest(
                trainId,
                start,
                destination,
                stops,
                PathfindingOptions.DEFAULT
        );
        DispatchService.DispatchResult result = dispatch.start(request);
        if (result instanceof DispatchService.DispatchResult.Rejected(String reason)) {
            return Optional.of(reason);
        }
        if (result instanceof DispatchService.DispatchResult.Accepted(RouteLock lock)) {
            trains.put(trainId, train.withSchedule(schedule, lock.route(), 0));
            // The train already stands at its starting station; it must not dwell there again.
            List<RailNodeId> driverStops = new ArrayList<>(stopNodes);
            if (driverStops.size() > 1 && driverStops.getFirst().equals(start)) {
                driverStops.removeFirst();
            }
            automaticDriver.onScheduleStarted(trainId, driverStops);
            markSavedDataDirty();
            return Optional.empty();
        }
        return Optional.of("Schedule could not be started");
    }

    public void derail(RailTrainId trainId) {
        RailTrainAggregate train = trains.get(trainId);
        if (train == null) {
            return;
        }
        dispatch.release(trainId);
        automaticDriver.discard(trainId);
        // Derailment pauses the automatic schedule and releases the route/reservations; the train returns to manual
        // so it cannot keep driving on its own after being righted.
        trains.put(trainId, train.asDerailed().withControlMode(RailControlMode.MANUAL));
        markSavedDataDirty();
    }

    public void resetTrain(RailTrainId trainId) {
        RailTrainAggregate train = trains.get(trainId);
        if (train == null) {
            return;
        }
        trains.put(trainId, train.reset());
        markSavedDataDirty();
    }

    public void removeTrain(RailTrainId trainId) {
        dispatch.release(trainId);
        automaticDriver.discard(trainId);
        trains.remove(trainId);
        markSavedDataDirty();
    }

    public void markCellChanged(GridPos cell) {
        dirtyCells.add(cell);
        markSavedDataDirty();
    }

    public void validateNear(GridPos center) {
        if (source == null) {
            return;
        }
        if (!dirtyCells.isEmpty()) {
            dirtyCells.clear();
            graph.update(RailGraph.empty());
        }
        ensureGraph(center);
    }

    private void ensureGraph(GridPos center) {
        if (source == null) {
            graph.update(RailGraph.empty());
            return;
        }
        if (graph.nodeAt(center).isPresent() && dirtyCells.isEmpty()) {
            return;
        }
        RailGraph collected = RailGraphCollector.collect(source, center);
        if (!collected.nodes().isEmpty()) {
            graph.update(collected);
            dirtyCells.removeIf(cell -> graph.nodeAt(cell).isPresent());
        }
    }

    private List<RailNodeId> locateStops(RailTrainSchedule schedule) {
        List<RailNodeId> result = new ArrayList<>();
        for (var stop : schedule.stops()) {
            Optional<GridPos> position = stationLocator.locate(stop.station());
            if (position.isEmpty()) {
                continue;
            }
            Optional<RailNodeView> node = graph.nodeAt(position.orElseThrow());
            node.map(RailNodeView::id).ifPresent(result::add);
        }
        return result;
    }

    private void markSavedDataDirty() {
        if (savedData != null) {
            savedData.updateState(new RailNetworkState(
                    trains.values().stream().map(RailTrainAggregate::snapshot).toList()
            ));
        }
    }
}
