package com.mistbeyond.transport.api.rail.graph;

import com.mojang.serialization.Codec;

public enum TrackType {
    STRAIGHT,
    DIAGONAL_45,
    CURVE,
    CURVE_RAMP;

    public static final Codec<TrackType> CODEC = Codec.STRING.xmap(TrackType::valueOf, TrackType::name);
}
