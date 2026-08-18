package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mistbeyond.transport.core.rail.RailEdge;
import com.mistbeyond.transport.core.rail.RailGraph;
import com.mistbeyond.transport.core.rail.RailNode;
import com.mistbeyond.transport.core.rail.Signal;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailSectionDebugRendererTest {
    @Test
    void connectedLineFormsOneDefaultSection() {
        RailGraph graph = lineGraph(Set.of());

        var sections = RailSectionDebugRenderer.sectionsOf(graph, 0);

        assertEquals(1, sections.size());
        assertEquals("section-0", sections.getFirst().id());
        assertTracks(sections.getFirst(), pos(0), pos(1), pos(1), pos(2));
    }

    @Test
    void signalAtNodeSplitsLineIntoTwoSections() {
        RailGraph graph = lineGraph(Set.of(new Signal(
                new SignalId("sig-a"),
                pos(1),
                GridDirection.EAST,
                SignalType.BLOCK
        )));

        var sections = RailSectionDebugRenderer.sectionsOf(graph, 0);

        assertEquals(2, sections.size());
        assertEquals("section-0 (1)", sections.get(0).id());
        assertEquals("section-1 (1)", sections.get(1).id());
        assertTracks(sections.get(0), pos(0), pos(1));
        assertTracks(sections.get(1), pos(1), pos(2));
    }

    @Test
    void isolatedNodesFallBackToComponentDots() {
        RailGraph graph = new RailGraph(
                Set.of(node("a", 0), node("b", 5)),
                Set.of(),
                Set.of()
        );

        var sections = RailSectionDebugRenderer.sectionsOf(graph, 3);

        assertEquals(1, sections.size());
        assertEquals("component-3", sections.getFirst().id());
        List<RailSectionDebugRenderer.TrackVisual> tracks = sections.getFirst().tracks();
        assertEquals(2, tracks.size());
        assertEquals(pos(0), tracks.get(0).start());
        assertEquals(pos(0), tracks.get(0).end());
        assertEquals(pos(5), tracks.get(1).start());
        assertEquals(pos(5), tracks.get(1).end());
    }

    @Test
    void signalsAreMappedToMarkers() {
        RailGraph graph = lineGraph(Set.of(new Signal(
                new SignalId("sig-b"),
                pos(1),
                GridDirection.WEST,
                SignalType.PATH
        )));

        var signals = RailSectionDebugRenderer.signalsOf(graph);

        assertEquals(1, signals.size());
        assertEquals("sig-b", signals.getFirst().id());
        assertEquals(GridDirection.WEST, signals.getFirst().direction());
        assertEquals("PATH", signals.getFirst().type());
        assertEquals(1, signals.getFirst().cell().getX());
        assertEquals(0, signals.getFirst().cell().getY());
        assertEquals(0, signals.getFirst().cell().getZ());
    }

    @Test
    void cardinalEdgeRendersAsSingleBar() {
        List<AABB> boxes = RailSectionDebugRenderer.trackBoxes(pos(0), pos(1));

        assertEquals(1, boxes.size());
        AABB box = boxes.getFirst();
        assertEquals(0.5, box.minX, 1.0E-6);
        assertEquals(1.5, box.maxX, 1.0E-6);
        assertEquals(0.25, box.minZ, 1.0E-6);
        assertEquals(0.75, box.maxZ, 1.0E-6);
    }

    @Test
    void diagonalEdgeRendersAsSingleRotatedBar() {
        List<AABB> boxes = RailSectionDebugRenderer.trackBoxes(pos(0), new GridPos(1, 0, 1));

        assertEquals(1, boxes.size());
        AABB box = boxes.getFirst();
        assertEquals(0.5 - 0.25 / Math.sqrt(2.0), box.minX, 1.0E-6);
        assertEquals(1.5 + 0.25 / Math.sqrt(2.0), box.maxX, 1.0E-6);
        assertEquals(0.5 - 0.25 / Math.sqrt(2.0), box.minZ, 1.0E-6);
        assertEquals(1.5 + 0.25 / Math.sqrt(2.0), box.maxZ, 1.0E-6);
        assertEquals(0.35, box.minY, 1.0E-6);
        assertEquals(0.55, box.maxY, 1.0E-6);
    }

    @Test
    void isolatedNodeRendersAsDot() {
        List<AABB> boxes = RailSectionDebugRenderer.trackBoxes(pos(4), pos(4));

        assertEquals(1, boxes.size());
        AABB box = boxes.getFirst();
        assertEquals(4.0 + 0.5 - 0.175, box.minX, 1.0E-6);
        assertEquals(4.0 + 0.5 + 0.175, box.maxX, 1.0E-6);
    }

    private static RailGraph lineGraph(Set<Signal> signals) {
        RailNode a = node("a", 0);
        RailNode b = node("b", 1);
        RailNode c = node("c", 2);
        return new RailGraph(
                Set.of(a, b, c),
                Set.of(
                        edge("a-b", a, b, 0),
                        edge("b-c", b, c, 1)
                ),
                signals
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

    private static void assertTracks(RailSectionDebugRenderer.RenderedSection section, GridPos... endpoints) {
        assertEquals(endpoints.length / 2, section.tracks().size());
        for (int i = 0; i < section.tracks().size(); i++) {
            RailSectionDebugRenderer.TrackVisual track = section.tracks().get(i);
            assertTrue(equalsPos(endpoints[i * 2], track.start()),
                    "track " + i + " start: expected " + endpoints[i * 2] + " but was " + track.start());
            assertTrue(equalsPos(endpoints[i * 2 + 1], track.end()),
                    "track " + i + " end: expected " + endpoints[i * 2 + 1] + " but was " + track.end());
        }
    }

    private static boolean equalsPos(GridPos expected, GridPos actual) {
        return expected.x() == actual.x() && expected.y() == actual.y() && expected.z() == actual.z();
    }
}
