package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeView;

public record RailNode(RailNodeId id, GridPos pos) implements RailNodeView {
}
