package com.mistbeyond.transport.api.rail.section;

import com.mojang.serialization.Codec;

public record SignalId(String value) {
    public static final Codec<SignalId> CODEC = Codec.STRING.xmap(SignalId::new, SignalId::value);
}
