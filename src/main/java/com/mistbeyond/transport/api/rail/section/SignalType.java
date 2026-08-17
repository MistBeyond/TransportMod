package com.mistbeyond.transport.api.rail.section;

import com.mojang.serialization.Codec;

public enum SignalType {
    BLOCK,
    PATH;

    public static final Codec<SignalType> CODEC = Codec.STRING.xmap(SignalType::valueOf, SignalType::name);
}
