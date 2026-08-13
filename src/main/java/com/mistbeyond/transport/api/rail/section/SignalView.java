package com.mistbeyond.transport.api.rail.section;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;

public interface SignalView {
    SignalId id();

    GridPos cell();

    GridDirection direction();

    SignalType type();
}
