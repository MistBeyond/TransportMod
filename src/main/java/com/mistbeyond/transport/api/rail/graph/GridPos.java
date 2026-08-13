package com.mistbeyond.transport.api.rail.graph;

public record GridPos(int x, int y, int z) {

    public static GridPos of(int x, int y, int z) {
        return new GridPos(x, y, z);
    }

    public GridPos offset(int dx, int dy, int dz) {
        return new GridPos(x + dx, y + dy, z + dz);
    }
}
