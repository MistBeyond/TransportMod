package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchService;
import com.mistbeyond.transport.api.rail.dispatch.PathfindingOptions;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.dispatch.RouteRequest;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mistbeyond.transport.core.rail.RailEdge;
import com.mistbeyond.transport.core.rail.RailNode;
import com.mistbeyond.transport.core.rail.Signal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispatchServiceTest {
    @Test
    void reservesAndReleasesStepByStepAcrossSignalBoundary() {
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailNode c = node("c", 2);
        Signal signal = new Signal(
                new SignalId("node-signal"),
                b.pos(),
                GridDirection.EAST,
                SignalType.BLOCK
        );
        RailGraph graph = new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        new RailEdge(
                                new RailEdgeId("a-b"),
                                a.id(),
                                b.id(),
                                new TrackPlacement(new GridPos(0, 0, 0), GridDirection.EAST, TrackType.STRAIGHT),
                                1.0
                        ),
                        new RailEdge(
                                new RailEdgeId("b-c"),
                                b.id(),
                                c.id(),
                                new TrackPlacement(new GridPos(1, 0, 0), GridDirection.EAST, TrackType.STRAIGHT),
                                1.0
                        )
                ),
                Set.of(signal)
        );
        DispatchService dispatch = new DispatchServiceImpl(graph, new ShortestPathRouter());
        RailTrainId trainId = new RailTrainId("train-1");
        RouteRequest request = new RouteRequest(
                trainId,
                a.id(),
                c.id(),
                List.of(),
                PathfindingOptions.DEFAULT
        );

        DispatchService.DispatchResult start = dispatch.start(request);
        DispatchService.DispatchResult.Accepted accepted = assertInstanceOf(
                DispatchService.DispatchResult.Accepted.class,
                start
        );
        assertEquals(2, accepted.lock().sections().size());
        assertEquals(1, dispatch.snapshot().reservations().size());

        RailSectionId first = accepted.lock().sections().getFirst();
        DispatchService.AdvanceResult moved = dispatch.advance(trainId, first);
        assertInstanceOf(DispatchService.AdvanceResult.Move.class, moved);

        RailSectionId second = accepted.lock().sections().get(1);
        DispatchService.AdvanceResult arrived = dispatch.advance(trainId, second);
        assertInstanceOf(DispatchService.AdvanceResult.Arrived.class, arrived);
        assertTrue(dispatch.snapshot().reservations().isEmpty());
    }

    @Test
    void manualClaimBlocksAutomaticDispatch() {
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailGraph graph = new RailGraph(
                Set.of(a, b),
                Set.of(new RailEdge(
                        new RailEdgeId("a-b"),
                        a.id(),
                        b.id(),
                        new TrackPlacement(new GridPos(0, 0, 0), GridDirection.EAST, TrackType.STRAIGHT),
                        1.0
                )),
                Set.of()
        );
        DispatchService dispatch = new DispatchServiceImpl(graph, new ShortestPathRouter());
        RailSectionView section = graph.sections().iterator().next();
        dispatch.claimManual(new RailTrainId("manual"), section.id());

        DispatchService.DispatchResult result = dispatch.start(new RouteRequest(
                new RailTrainId("auto"),
                a.id(),
                b.id(),
                List.of(),
                PathfindingOptions.DEFAULT
        ));

        assertInstanceOf(DispatchService.DispatchResult.Rejected.class, result);
    }

    private static RailNode node(String id, int x) {
        return new RailNode(new RailNodeId(id), new GridPos(x, 0, 0));
    }
}
