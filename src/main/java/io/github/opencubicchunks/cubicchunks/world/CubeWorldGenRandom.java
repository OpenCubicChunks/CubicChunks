package io.github.opencubicchunks.cubicchunks.world;

import net.minecraft.world.level.levelgen.WorldgenRandom;

public class CubeWorldGenRandom extends WorldgenRandom {

    public long setDecorationSeed(long worldSeed, int x, int y, int z) {
        this.setSeed(worldSeed);
        long sX = this.nextLong() | 1L;
        long sY = this.nextLong() | 1L;
        long sZ = this.nextLong() | 1L;
        long result = (long) x * sX + (long) y * sY + (long) z * sZ ^ worldSeed;
        this.setSeed(result);
        return result;
    }
}
