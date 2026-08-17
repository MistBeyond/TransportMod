package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.SignalPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackGraphSource;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailGraphCollectorTest {
    @Test
    void collectsConnectedCardinalTrackIntoGraph() {
        Set<GridPos> cells = Set.of(pos(0), pos(1), pos(2));
        RailGraph graph = RailGraphCollector.collect(
                gridSource(cells, Set.of(GridDirection.EAST, GridDirection.WEST)),
                pos(0)
        );

        assertEquals(3, graph.nodes().size());
        assertEquals(2, graph.edges().size());
        assertEquals(1, graph.sections().size());
    }

    @Test
    void collectsDiagonal45TrackAsSingleEdge() {
        Set<GridPos> cells = Set.of(pos(0), diagonalNeighbor());
        RailGraph graph = RailGraphCollector.collect(
                gridSource(cells, Set.of(GridDirection.SOUTH_EAST, GridDirection.NORTH_WEST)),
                pos(0)
        );

        assertEquals(1, graph.edges().size());
        RailEdgeView edge = graph.edges().iterator().next();
        assertEquals(Math.sqrt(2.0), edge.lengthMeters(), 1.0E-6);
    }

    @Test
    void farthestReachableNodeReturnsLineEnd() {
        Set<GridPos> cells = Set.of(pos(0), pos(1), pos(2));
        RailGraph graph = RailGraphCollector.collect(
                gridSource(cells, Set.of(GridDirection.EAST, GridDirection.WEST)),
                pos(0)
        );
        RailNodeId start = graph.nodeAt(pos(0)).orElseThrow().id();

        Optional<RailNodeId> farthest = RailGraphs.farthestReachableNode(graph, start);

        assertTrue(farthest.isPresent());
        assertEquals(graph.nodeAt(pos(2)).orElseThrow().id(), farthest.orElseThrow());
    }

    @Test
    void connectsOnlyWhenBothEndsPointAtEachOther() {
        GridPos a = pos(0);
        GridPos b = pos(1);
        GridPos c = new GridPos(0, 0, -1);
        TrackGraphSource source = cell -> {
            if (cell.equals(a)) {
                return Set.of(
                        new TrackPlacement(a, GridDirection.EAST, TrackType.STRAIGHT),
                        new TrackPlacement(a, GridDirection.NORTH, TrackType.STRAIGHT)
                );
            }
            if (cell.equals(b)) {
                return Set.of(
                        new TrackPlacement(b, GridDirection.WEST, TrackType.STRAIGHT),
                        new TrackPlacement(b, GridDirection.NORTH, TrackType.STRAIGHT)
                );
            }
            if (cell.equals(c)) {
                return Set.of(new TrackPlacement(c, GridDirection.EAST, TrackType.STRAIGHT));
            }
            return Set.of();
        };

        RailGraph graph = RailGraphCollector.collect(source, b);

        assertEquals(2, graph.nodes().size());
        assertTrue(graph.nodeAt(a).isPresent());
        assertTrue(graph.nodeAt(b).isPresent());
        assertTrue(graph.nodeAt(c).isEmpty());
        assertEquals(1, graph.edges().size());
    }

    @Test
    void collectsSignalFromTrackCellData() {
        GridPos a = pos(0);
        GridPos b = pos(1);
        TrackGraphSource source = new TrackGraphSource() {
            @Override
            public Set<TrackPlacement> placementsAt(GridPos cell) {
                if (cell.equals(a)) {
                    return Set.of(new TrackPlacement(a, GridDirection.EAST, TrackType.STRAIGHT));
                }
                if (cell.equals(b)) {
                    return Set.of(new TrackPlacement(b, GridDirection.WEST, TrackType.STRAIGHT));
                }
                return Set.of();
            }

            @Override
            public Optional<SignalPlacement> signalAt(GridPos cell) {
                if (cell.equals(b)) {
                    return Optional.of(new SignalPlacement(
                            new SignalId("signal"),
                            GridDirection.EAST,
                            SignalType.BLOCK
                    ));
                }
                return Optional.empty();
            }
        };

        RailGraph graph = RailGraphCollector.collect(source, a);

        assertEquals(1, graph.signals().size());
        assertEquals("signal", graph.signals().iterator().next().id().value());
    }

    private static TrackGraphSource gridSource(Set<GridPos> cells, Set<GridDirection> directions) {
        return cell -> {
            if (!cells.contains(cell)) {
                return Set.of();
            }
            Set<TrackPlacement> placements = new HashSet<>();
            for (GridDirection direction : directions) {
                GridPos neighbor = direction.step(cell);
                if (cells.contains(neighbor)) {
                    placements.add(new TrackPlacement(
                            cell,
                            direction,
                            direction.diagonal() ? TrackType.DIAGONAL_45 : TrackType.STRAIGHT
                    ));
                }
            }
            return placements;
        };
    }

    private static GridPos pos(int x) {
        return new GridPos(x, 0, 0);
    }

    private static GridPos diagonalNeighbor() {
        return new GridPos(1, 0, 1);
    }
}
