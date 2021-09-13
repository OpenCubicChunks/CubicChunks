package io.github.opencubicchunks.cubicchunks.levelgen;

import io.github.opencubicchunks.cubicchunks.levelgen.util.NonAtomicWorldgenRandom;

public class CubeWorldGenRandom extends NonAtomicWorldgenRandom {
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
