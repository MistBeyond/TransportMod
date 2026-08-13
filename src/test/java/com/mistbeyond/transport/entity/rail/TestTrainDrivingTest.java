package com.mistbeyond.transport.entity.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.core.rail.RailEdge;
import com.mistbeyond.transport.core.rail.RailGraph;
import com.mistbeyond.transport.core.rail.RailNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestTrainDrivingTest {
    @Test
    void firstEdgeStartsAtSpawnNode() {
        RailGraph graph = lineGraph();

        TestTrainEntity.EdgeState edge = TestTrainEntity.firstEdge(graph, pos(0));

        assertNotNull(edge);
        assertEquals(0.0, edge.progress, 1.0E-6);
        assertEquals(pos(1), edge.toPos);
    }

    @Test
    void forwardAtNodePicksNextEdge() {
        RailGraph graph = lineGraph();
        Map<RailNodeId, java.util.List<TestTrainEntity.EdgeEnd>> adjacency =
                TestTrainEntity.buildAdjacency(graph);
        TestTrainEntity.EdgeState first = TestTrainEntity.firstEdge(graph, pos(0));
        assertNotNull(first);
        first.progress = first.length;

        TestTrainEntity.EdgeState next = TestTrainEntity.nextEdge(adjacency, first, false);

        assertNotNull(next);
        assertEquals(pos(1), next.fromPos);
        assertEquals(pos(2), next.toPos);
        assertEquals(0.0, next.progress, 1.0E-6);
    }

    @Test
    void backwardAtNodeReversesOntoPreviousEdge() {
        RailGraph graph = lineGraph();
        Map<RailNodeId, java.util.List<TestTrainEntity.EdgeEnd>> adjacency =
                TestTrainEntity.buildAdjacency(graph);
        TestTrainEntity.EdgeState towardEnd = new TestTrainEntity.EdgeState(
                new RailEdgeId("b-c"),
                new RailNodeId("b"),
                new RailNodeId("c"),
                pos(1),
                pos(2),
                1.0
        );

        TestTrainEntity.EdgeState reversed = TestTrainEntity.nextEdge(adjacency, towardEnd, true);

        assertNotNull(reversed);
        assertEquals(pos(0), reversed.fromPos);
        assertEquals(pos(1), reversed.toPos);
        assertEquals(1.0, reversed.progress, 1.0E-6);
    }

    private static RailGraph lineGraph() {
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailNode c = node("c", 2);
        return new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        edge("a-b", a, b, 0),
                        edge("b-c", b, c, 1)
                ),
                Set.of()
        );
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

    private static GridPos pos(int x) {
        return new GridPos(x, 0, 0);
    }
}
