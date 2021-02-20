package io.github.opencubicchunks.cubicchunks.chunk;

import net.minecraft.world.level.levelgen.WorldgenRandom;

public class NonAtomicWorldgenRandom extends WorldgenRandom {
    // duplicated from Random
    protected static final long MULTIPLIER = 0x5DEECE66DL;
    protected static final long ADDEND = 0xBL;
    protected static final long MASK = (1L << 48) - 1;

    protected long seed;

    public NonAtomicWorldgenRandom() {
        super(0);
    }

    public NonAtomicWorldgenRandom(long seed) {
        super(0);
        this.seed = seed;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public int next(int bits) {
        long nextSeed = (this.seed * MULTIPLIER + ADDEND) & MASK;
        this.seed = nextSeed;
        return (int) (nextSeed >>> (48 - bits));
    }
}
