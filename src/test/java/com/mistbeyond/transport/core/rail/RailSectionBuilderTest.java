package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mistbeyond.transport.core.rail.RailEdge;
import com.mistbeyond.transport.core.rail.RailNode;
import com.mistbeyond.transport.core.rail.Signal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailSectionBuilderTest {
    @Test
    void connectedLineWithoutSignalsFormsOneSection() {
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailNode c = node("c", 2);
        RailGraph graph = new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        edge("a-b", a, b, 0),
                        edge("b-c", b, c, 1)
                ),
                Set.of()
        );

        assertEquals(1, graph.sections().size());
        assertEquals(2, graph.sections().iterator().next().segments().size());
    }

    @Test
    void nodeSignalSplitsConnectedLineIntoTwoSections() {
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
                        edge("a-b", a, b, 0),
                        edge("b-c", b, c, 1)
                ),
                Set.of(signal)
        );

        assertEquals(2, graph.sections().size());
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
}
