package io.github.opencubicchunks.cubicchunks.levelgen.util;

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
        this.seed = scramble(seed);
    }

    public static long scramble(long seed) {
        return (seed ^ MULTIPLIER) & MASK;
    }

    public static long nextSeed(long seed) {
        return (seed * MULTIPLIER + ADDEND) & MASK;
    }

    public static int sample(long seed, int bits) {
        return (int) (seed >>> (48 - bits));
    }

    public static float sampleFloat(long seed) {
        return sample(seed, 24) / (float) (1 << 24);
    }

    @Override
    public void setSeed(long seed) {
        this.seed = scramble(seed);
    }

    @Override
    public int next(int bits) {
        long nextSeed = nextSeed(this.seed);
        this.seed = nextSeed;
        return sample(nextSeed, bits);
    }
}
