package io.github.opencubicchunks.cubicchunks.chunk.carver;

import java.util.BitSet;

import io.github.opencubicchunks.cubicchunks.CubicChunks;

public class CubicCarverBitSet extends BitSet {
    private final int maxY;

    public CubicCarverBitSet(int maxY, int nbits) {
        super(nbits);
        this.maxY = maxY;
    }

    @Override public void set(int bitIndex) {
        int x = bitIndex & 15;
        int y = bitIndex >> 8;
        int z = bitIndex >> 4 & 15;

        if (y < maxY) {
            super.set(bitIndex);
        } else {
            int i = 0;
            CubicChunks.LOGGER.warn("Bit index was out of y bounds: " + bitIndex + ", MaxY: " + maxY);
        }
    }

    @Override public boolean get(int bitIndex) {
        super.get(bitIndex);
        return false;
    }
}
