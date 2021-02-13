package io.github.opencubicchunks.cubicchunks.server;

public interface CubicLevelHeightAccessor {

    default WorldStyle worldStyle() {
        return WorldStyle.CHUNK;
    }

    default boolean isCubic() {
        return worldStyle() == WorldStyle.CUBIC || worldStyle() == WorldStyle.HYBRID;
    }

    default boolean generates2DChunks() {
        return worldStyle() == WorldStyle.CHUNK || worldStyle() == WorldStyle.HYBRID;
    }

    enum WorldStyle {
        CUBIC, // Primary Implementation (Generate Chunks 3D, infinite world height)
        HYBRID, // Soft implementation (Vanilla Chunk Generation, infinite world height)
        CHUNK //Vanilla (2D Generation, World Height is NOT infinite)
    }
}
