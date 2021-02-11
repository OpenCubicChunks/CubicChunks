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
    public int next(int bits) {
        long seed = (this.seed * MULTIPLIER + ADDEND) & MASK;
        this.seed = seed;
        return (int) (seed >>> (48 - bits));
    }
}
