package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchService;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalState;
import com.mistbeyond.transport.api.rail.section.SignalType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalStateResolverTest {
    private static final SignalId SIGNAL = new SignalId("signal");

    @Test
    void signalFacingEastTurnsRedOnlyWhenAheadSectionIsBlocked() {
        RailGraph graph = lineWithSignalAtNode(GridDirection.EAST);

        // The east-facing signal protects the section beyond itself (edge e2, towards c).
        assertEquals(SignalAspect.RED, claimAndResolve(graph, sectionOn(graph, "e2").id()));
        // A blocked section behind the signal (edge e1, towards a) keeps it green.
        assertEquals(SignalAspect.GREEN, claimAndResolve(graph, sectionOn(graph, "e1").id()));
    }

    @Test
    void signalFacingWestProtectsTheOtherDirection() {
        RailGraph graph = lineWithSignalAtNode(GridDirection.WEST);

        assertEquals(SignalAspect.GREEN, claimAndResolve(graph, sectionOn(graph, "e2").id()));
        assertEquals(SignalAspect.RED, claimAndResolve(graph, sectionOn(graph, "e1").id()));
    }

    @Test
    void signalAtCrossingIgnoresSectionsOnNonFacingEdges() {
        GridPos origin = new GridPos(1, 0, 0);
        RailNode a = new RailNode(new RailNodeId("a"), new GridPos(0, 0, 0));
        RailNode o = new RailNode(new RailNodeId("o"), origin);
        RailNode b = new RailNode(new RailNodeId("b"), new GridPos(2, 0, 0));
        RailNode c = new RailNode(new RailNodeId("c"), new GridPos(1, 0, -1));
        RailNode d = new RailNode(new RailNodeId("d"), new GridPos(1, 0, 1));
        Signal signal = new Signal(SIGNAL, origin, GridDirection.EAST, SignalType.BLOCK);
        RailGraph graph = new RailGraph(
                Set.of(a, o, b, c, d),
                Set.of(
                        edge("w", a, o, GridDirection.EAST),
                        edge("e", o, b, GridDirection.EAST),
                        edge("n", c, o, GridDirection.SOUTH),
                        edge("s", o, d, GridDirection.SOUTH)
                ),
                Set.of(signal)
        );

        // Sections on the crossing edges are not protected by the east-facing signal.
        assertEquals(SignalAspect.GREEN, claimAndResolve(graph, sectionOn(graph, "w").id()));
        assertEquals(SignalAspect.GREEN, claimAndResolve(graph, sectionOn(graph, "n").id()));
        assertEquals(SignalAspect.GREEN, claimAndResolve(graph, sectionOn(graph, "s").id()));
        // The section ahead on the facing edge (east of the signal) is the protected one.
        assertEquals(SignalAspect.RED, claimAndResolve(graph, sectionOn(graph, "e").id()));
    }

    private static SignalAspect claimAndResolve(RailGraph graph, RailSectionId sectionId) {
        DispatchService dispatch = new DispatchServiceImpl(graph, new ShortestPathRouter());
        dispatch.claimManual(new RailTrainId("manual"), sectionId);
        return aspect(SignalStateResolver.resolve(graph, dispatch.snapshot()));
    }

    private static SignalAspect aspect(Set<SignalState> states) {
        return states.stream()
                .filter(state -> state.id().equals(SIGNAL))
                .findFirst()
                .orElseThrow()
                .aspect();
    }

    private static RailSectionView sectionOn(RailGraph graph, String edgeId) {
        return graph.sections().stream()
                .filter(section -> section.segments().stream()
                        .anyMatch(segment -> segment.edgeId().value().equals(edgeId)))
                .findFirst()
                .orElseThrow();
    }

    private static RailGraph lineWithSignalAtNode(GridDirection facing) {
        RailNode a = new RailNode(new RailNodeId("a"), new GridPos(0, 0, 0));
        RailNode b = new RailNode(new RailNodeId("b"), new GridPos(1, 0, 0));
        RailNode c = new RailNode(new RailNodeId("c"), new GridPos(2, 0, 0));
        Signal signal = new Signal(SIGNAL, b.pos(), facing, SignalType.BLOCK);
        return new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        edge("e1", a, b, GridDirection.EAST),
                        edge("e2", b, c, GridDirection.EAST)
                ),
                Set.of(signal)
        );
    }

    private static RailEdge edge(String id, RailNode start, RailNode end, GridDirection direction) {
        return new RailEdge(
                new RailEdgeId(id),
                start.id(),
                end.id(),
                new TrackPlacement(start.pos(), direction, TrackType.STRAIGHT),
                1.0
        );
    }
}