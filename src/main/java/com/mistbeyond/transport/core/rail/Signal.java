package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mistbeyond.transport.api.rail.section.SignalView;

public record Signal(
        SignalId id,
        GridPos cell,
        GridDirection direction,
        SignalType type
) implements SignalView {
}
