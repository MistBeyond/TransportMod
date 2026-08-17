package com.mistbeyond.transport.api.rail.graph;

import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SignalPlacement(
        SignalId id,
        GridDirection direction,
        SignalType type
) {
    public static final Codec<SignalPlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SignalId.CODEC.fieldOf("id").forGetter(SignalPlacement::id),
            GridDirection.CODEC.fieldOf("direction").forGetter(SignalPlacement::direction),
            SignalType.CODEC.fieldOf("type").forGetter(SignalPlacement::type)
    ).apply(instance, SignalPlacement::new));
}
