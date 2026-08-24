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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void blockSignalWithoutTrackOnFacingSideIsErrorAndRed() {
        // The signal at b faces NORTH, but the line runs east-west: its facing side has no track.
        RailGraph graph = lineWithSignalAtNode(GridDirection.NORTH);

        SignalState state = signalState(resolveStates(graph));
        assertEquals(SignalAspect.RED, state.aspect());
        assertTrue(state.error());
    }

    @Test
    void pathSignalOnDeadEndChainIsErrorAndRed() {
        // s -> n1 -> t: the facing chain dead-ends before any routing node.
        RailNode s = new RailNode(new RailNodeId("s"), new GridPos(0, 0, 0));
        RailNode n1 = new RailNode(new RailNodeId("n1"), new GridPos(1, 0, 0));
        RailNode t = new RailNode(new RailNodeId("t"), new GridPos(2, 0, 0));
        Signal signal = new Signal(SIGNAL, s.pos(), GridDirection.EAST, SignalType.PATH);
        RailGraph graph = new RailGraph(
                Set.of(s, n1, t),
                Set.of(
                        edge("s-n1", s, n1, GridDirection.EAST),
                        edge("n1-t", n1, t, GridDirection.EAST)
                ),
                Set.of(signal)
        );

        SignalState state = signalState(resolveStates(graph));
        assertEquals(SignalAspect.RED, state.aspect());
        assertTrue(state.error());
    }

    @Test
    void pathSignalOnChainReachingJunctionIsNotError() {
        // s -> n1 -> j, with j branching to out1 and out2: j is a routing node (degree 3).
        RailNode s = new RailNode(new RailNodeId("s"), new GridPos(0, 0, 0));
        RailNode n1 = new RailNode(new RailNodeId("n1"), new GridPos(1, 0, 0));
        RailNode j = new RailNode(new RailNodeId("j"), new GridPos(2, 0, 0));
        RailNode out1 = new RailNode(new RailNodeId("out1"), new GridPos(3, 0, 0));
        RailNode out2 = new RailNode(new RailNodeId("out2"), new GridPos(2, 0, 1));
        Signal signal = new Signal(SIGNAL, s.pos(), GridDirection.EAST, SignalType.PATH);
        RailGraph graph = new RailGraph(
                Set.of(s, n1, j, out1, out2),
                Set.of(
                        edge("s-n1", s, n1, GridDirection.EAST),
                        edge("n1-j", n1, j, GridDirection.EAST),
                        edge("j-out1", j, out1, GridDirection.EAST),
                        edge("j-out2", j, out2, GridDirection.SOUTH)
                ),
                Set.of(signal)
        );

        // Nothing is blocked, so the path signal is green and not errored.
        SignalState state = signalState(resolveStates(graph));
        assertEquals(SignalAspect.GREEN, state.aspect());
        assertFalse(state.error());
    }

    private static SignalAspect claimAndResolve(RailGraph graph, RailSectionId sectionId) {
        DispatchService dispatch = new DispatchServiceImpl(graph, new ShortestPathRouter());
        dispatch.claimManual(new RailTrainId("manual"), sectionId);
        return aspect(SignalStateResolver.resolve(graph, dispatch.snapshot()));
    }

    private static Set<SignalState> resolveStates(RailGraph graph) {
        DispatchService dispatch = new DispatchServiceImpl(graph, new ShortestPathRouter());
        return SignalStateResolver.resolve(graph, dispatch.snapshot());
    }

    private static SignalState signalState(Set<SignalState> states) {
        return states.stream()
                .filter(state -> state.id().equals(SIGNAL))
                .findFirst()
                .orElseThrow();
    }

    private static SignalAspect aspect(Set<SignalState> states) {
        return signalState(states).aspect();
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