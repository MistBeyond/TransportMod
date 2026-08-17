package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchService;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalStateResolverTest {
    @Test
    void manualClaimOnBoundarySectionTurnsSignalRed() {
        RailNode a = new RailNode(new RailNodeId("a"), new GridPos(0, 0, 0));
        RailNode b = new RailNode(new RailNodeId("b"), new GridPos(1, 0, 0));
        RailNode c = new RailNode(new RailNodeId("c"), new GridPos(2, 0, 0));
        Signal signal = new Signal(
                new SignalId("signal"),
                b.pos(),
                GridDirection.EAST,
                SignalType.BLOCK
        );
        RailGraph graph = new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        edge("a-b", a, b),
                        edge("b-c", b, c)
                ),
                Set.of(signal)
        );
        DispatchService dispatch = new DispatchServiceImpl(graph, new ShortestPathRouter());

        assertTrue(SignalStateResolver.resolve(graph, dispatch.snapshot()).stream()
                .allMatch(state -> state.aspect() == SignalAspect.GREEN));

        RailSectionView section = graph.sections().iterator().next();
        dispatch.claimManual(new RailTrainId("manual"), section.id());

        assertEquals(
                SignalAspect.RED,
                SignalStateResolver.resolve(graph, dispatch.snapshot()).stream()
                        .filter(state -> state.id().equals(signal.id()))
                        .findFirst()
                        .orElseThrow()
                        .aspect()
        );
    }

    private static RailEdge edge(String id, RailNode start, RailNode end) {
        return new RailEdge(
                new RailEdgeId(id),
                start.id(),
                end.id(),
                new TrackPlacement(start.pos(), GridDirection.EAST, TrackType.STRAIGHT),
                1.0
        );
    }
}
