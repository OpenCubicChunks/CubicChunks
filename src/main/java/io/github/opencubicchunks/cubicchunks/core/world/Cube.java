package io.github.opencubicchunks.cubicchunks.core.world;

import net.minecraft.world.chunk.ChunkSection;

public class Cube {

    private final int x;
    private final int y;
    private final int z;

    private ChunkSection section;

    public Cube(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public ChunkSection getSection() {
        return section;
    }

    public void setSection(ChunkSection section) {
        this.section = section;
    }
}
