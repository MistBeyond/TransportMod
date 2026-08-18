package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchService;
import com.mistbeyond.transport.api.rail.dispatch.PathfindingOptions;
import com.mistbeyond.transport.api.rail.dispatch.RailControlMode;
import com.mistbeyond.transport.api.rail.dispatch.RailRoute;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainScheduleType;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainStop;
import com.mistbeyond.transport.api.rail.dispatch.RouteRequest;
import com.mistbeyond.transport.api.rail.dispatch.StopPlan;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mistbeyond.transport.api.rail.station.RailStationId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticTrainDriverTest {
    private static final RailStationId STATION_A = new RailStationId("a");
    private static final RailStationId STATION_C = new RailStationId("c");

    @Test
    void oneWayTrainMovesAlongRouteReservesStepwiseAndFinishesAtDestination() {
        TestHost host = lineHost();
        AutomaticTrainDriver driver = new AutomaticTrainDriver();
        RailTrainId trainId = new RailTrainId("auto-1");
        host.addAutomaticTrain(trainId, STATION_A, STATION_C);
        host.startRoute(trainId);
        driver.onScheduleStarted(trainId, List.of(new RailNodeId("c")));

        run(driver, host, 5);

        // Still traveling: somewhere on the line, not yet finished.
        assertTrue(host.dispatch().snapshot().locks().stream().anyMatch(lock -> lock.trainId().equals(trainId)));
        assertTrue(host.train(trainId).position().x() > 0);

        run(driver, host, 200);

        // Finished: parked at the destination and the route/reservations were released.
        assertEquals(GridPos.of(2, 0, 0), host.train(trainId).position());
        assertTrue(host.dispatch().snapshot().reservations().isEmpty());
        assertTrue(host.dispatch().snapshot().locks().stream().noneMatch(lock -> lock.trainId().equals(trainId)));
    }

    @Test
    void trainRetriesRejectedScheduleUntilSectionIsFree() {
        // A block signal at node b splits the line into two sections (a-b and b-c).
        TestHost host = lineHost(
                new Signal(
                        new SignalId("s-b"),
                        GridPos.of(1, 0, 0),
                        GridDirection.EAST,
                        SignalType.BLOCK
                ));
        AutomaticTrainDriver driver = new AutomaticTrainDriver();
        RailTrainId trainId = new RailTrainId("auto-1");
        host.addAutomaticTrain(trainId, STATION_A, STATION_C);
        RailSectionId second = sectionOn(host, "b-c").id();
        // The occupier holds the second section: the initial schedule start is rejected.
        host.dispatch().claimManual(new RailTrainId("occupier"), second);
        host.startRoute(trainId);
        assertTrue(host.train(trainId).route().isEmpty());
        driver.onScheduleStarted(trainId, List.of(new RailNodeId("c")));

        run(driver, host, 60);

        // No reservation could be made while the section was held; the train stayed put.
        assertTrue(host.dispatch().snapshot().reservations().stream()
                .noneMatch(reservation -> reservation.sectionId().equals(second)));
        assertEquals(GridPos.of(0, 0, 0), host.train(trainId).position());

        host.dispatch().releaseManual(new RailTrainId("occupier"), second);
        run(driver, host, 200);

        // The retry eventually started the route and the train reached the destination.
        assertEquals(GridPos.of(2, 0, 0), host.train(trainId).position());
        assertTrue(host.dispatch().snapshot().reservations().isEmpty());
    }

    @Test
    void loopTrainRestartsScheduleAfterCompletingOneLap() {
        // Ring a<->b with one edge per direction; the loop start/end is node a.
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailGraph graph = new RailGraph(
                Set.of(a, b),
                Set.of(
                        edge("a-b", a, b, 0),
                        edge("b-a", b, a, 1)
                ),
                Set.of()
        );
        TestHost host = new TestHost(graph, a.id(), a.id());
        AutomaticTrainDriver driver = new AutomaticTrainDriver();
        RailTrainId trainId = new RailTrainId("auto-loop");
        host.addAutomaticTrain(trainId, RailTrainScheduleType.LOOP, STATION_A, new RailStationId("b"));
        host.startRoute(trainId, List.of(b.id()));
        driver.onScheduleStarted(trainId, List.of(b.id()));

        run(driver, host, 100);

        // One lap completed: the driver asked the host to re-run the schedule.
        assertTrue(host.restartCount() >= 1);
    }

    private static void run(AutomaticTrainDriver driver, TestHost host, int ticks) {
        for (int i = 0; i < ticks; i++) {
            driver.tick(host);
        }
    }

    private static RailSectionView sectionOn(TestHost host, String edgeId) {
        return host.graph().sections().stream()
                .filter(section -> section.segments().stream()
                        .anyMatch(segment -> segment.edgeId().value().equals(edgeId)))
                .findFirst()
                .orElseThrow();
    }

    private static TestHost lineHost(Signal... extraSignals) {
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailNode c = node("c", 2);
        Set<Signal> signals = Set.of(extraSignals);
        RailGraph graph = new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        edge("a-b", a, b, 0),
                        edge("b-c", b, c, 1)
                ),
                signals
        );
        return new TestHost(graph, a.id(), c.id());
    }

    private static RailNode node(String id, int x) {
        return new RailNode(new RailNodeId(id), new GridPos(x, 0, 0));
    }

    private static RailEdge edge(String id, RailNode start, RailNode end, int originX) {
        return new RailEdge(
                new RailEdgeId(id),
                start.id(),
                end.id(),
                new TrackPlacement(new GridPos(originX, 0, 0), GridDirection.EAST, TrackType.STRAIGHT),
                1.0
        );
    }

    /**
     * Test host: real graph + real dispatcher, in-memory train store, restart counter.
     */
    private static final class TestHost implements AutomaticTrainDriver.Host {
        private final RailGraph graph;
        private final RailNodeId start;
        private final RailNodeId destination;
        private final List<RailTrainAggregate> trains = new ArrayList<>();
        private final DispatchService dispatch;
        private int restarts;

        TestHost(RailGraph graph, RailNodeId start, RailNodeId destination) {
            this.graph = graph;
            this.start = start;
            this.destination = destination;
            this.dispatch = new DispatchServiceImpl(graph, new ShortestPathRouter());
        }

        void addAutomaticTrain(RailTrainId id, RailStationId firstStop, RailStationId secondStop) {
            addAutomaticTrain(id, RailTrainScheduleType.ONE_WAY, firstStop, secondStop);
        }

        void addAutomaticTrain(
                RailTrainId id,
                RailTrainScheduleType type,
                RailStationId firstStop,
                RailStationId secondStop
        ) {
            RailTrainSchedule schedule = new RailTrainSchedule(
                    type,
                    List.of(new RailTrainStop(firstStop, 0), new RailTrainStop(secondStop, 0))
            );
            // A schedule without an accepted route: exactly the state after a rejected schedule start.
            RailTrainAggregate train = new RailTrainAggregate(
                    id,
                    RailControlMode.AUTOMATIC,
                    nodePosOf(start),
                    false,
                    java.util.Optional.of(schedule),
                    java.util.Optional.empty(),
                    0
            );
            trains.add(train);
        }

        void startRoute(RailTrainId trainId, List<RailNodeId> stops) {
            RailTrainAggregate train = train(trainId);
            RouteRequest request = new RouteRequest(
                    trainId,
                    start,
                    destination,
                    stops.stream().map(stop -> new StopPlan(stop, 0)).toList(),
                    PathfindingOptions.DEFAULT
            );
            DispatchService.DispatchResult result = dispatch.start(request);
            if (result instanceof DispatchService.DispatchResult.Rejected) {
                // The route is not free: keep the train without a route so the driver retries later.
                return;
            }
            RailRoute route = ((DispatchService.DispatchResult.Accepted) result).lock().route();
            update(train.withSchedule(train.schedule().orElseThrow(), route, 0));
        }

        private GridPos nodePosOf(RailNodeId nodeId) {
            return graph.nodes().stream()
                    .filter(node -> node.id().equals(nodeId))
                    .findFirst()
                    .orElseThrow()
                    .pos();
        }

        void startRoute(RailTrainId trainId) {
            startRoute(trainId, List.of());
        }

        RailTrainAggregate train(RailTrainId id) {
            return trains.stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
        }

        int restartCount() {
            return restarts;
        }

        @Override
        public RailGraphView graph() {
            return graph;
        }

        @Override
        public DispatchService dispatch() {
            return dispatch;
        }

        @Override
        public List<RailTrainAggregate> trains() {
            return List.copyOf(trains);
        }

        @Override
        public void update(RailTrainAggregate train) {
            for (int i = 0; i < trains.size(); i++) {
                if (trains.get(i).id().equals(train.id())) {
                    trains.set(i, train);
                    return;
                }
            }
            trains.add(train);
        }

        @Override
        public void restartSchedule(RailTrainId trainId) {
            restarts++;
            dispatch.release(trainId);
            startRoute(trainId);
        }
    }
}