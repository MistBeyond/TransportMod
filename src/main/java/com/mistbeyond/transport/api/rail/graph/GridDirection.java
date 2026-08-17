package com.mistbeyond.transport.api.rail.graph;

import net.minecraft.util.StringRepresentable;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum GridDirection implements StringRepresentable {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0),
    NORTH_EAST(1, -1),
    SOUTH_EAST(1, 1),
    SOUTH_WEST(-1, 1),
    NORTH_WEST(-1, -1);

    public static final Codec<GridDirection> CODEC = Codec.STRING.xmap(GridDirection::valueOf, GridDirection::name);

    private final int dx;
    private final int dz;

    GridDirection(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    public boolean diagonal() {
        return dx != 0 && dz != 0;
    }

    public GridPos step(GridPos from) {
        return from.offset(dx, 0, dz);
    }

    public GridDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case NORTH_EAST -> SOUTH_WEST;
            case SOUTH_EAST -> NORTH_WEST;
            case SOUTH_WEST -> NORTH_EAST;
            case NORTH_WEST -> SOUTH_EAST;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
