package com.mistbeyond.transport.api.rail.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GridPos(int x, int y, int z) {
    public static final Codec<GridPos> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(GridPos::x),
            Codec.INT.fieldOf("y").forGetter(GridPos::y),
            Codec.INT.fieldOf("z").forGetter(GridPos::z)
    ).apply(instance, GridPos::new));

    public static GridPos of(int x, int y, int z) {
        return new GridPos(x, y, z);
    }

    public GridPos offset(int dx, int dy, int dz) {
        return new GridPos(x + dx, y + dy, z + dz);
    }
}
