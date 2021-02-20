package io.github.opencubicchunks.cubicchunks.chunk;

import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class AquiferRandom extends WorldgenRandom {
    // duplicated from Random
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;

    private long seed;

    public AquiferRandom() {
        super(0);
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public int nextInt(int bound) {
        // simplify method for aquifer's case to encourage inlining
        // namely, we remove the check for power of two bounds

        int bits;
        int result;
        int mod = bound - 1;

        do {
            long nextSeed = (this.seed * MULTIPLIER + ADDEND) & MASK;
            this.seed = nextSeed;
            bits = (int) (nextSeed >>> (48 - 31));

            result = bits % bound;
        } while (bits + mod < result);

        return result;
    }

    @Override
    public int next(int bits) {
        long nextSeed = (this.seed * MULTIPLIER + ADDEND) & MASK;
        this.seed = nextSeed;
        return (int) (nextSeed >>> (48 - bits));
    }
}
