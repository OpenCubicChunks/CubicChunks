package io.github.opencubicchunks.cubicchunks.server;

public interface CubicLevelHeightAccessor {

    default WorldStyle worldStyle() {
        return WorldStyle.CUBIC;
    }

    default boolean isCubic() {
        return worldStyle().isCubic();
    }

    default boolean generates2DChunks() {
        return worldStyle().generates2DChunks();
    }

    default void setWorldStyle(WorldStyle worldStyle) {
    }

    enum WorldStyle {
        CUBIC(true, false), // Primary Implementation (Generate Chunks 3D, infinite world height)
        HYBRID(true, true), // Soft implementation (Vanilla Chunk Generation, infinite world height)
        CHUNK(false, true); //Vanilla (2D Generation, World Height is NOT infinite)

        private final boolean isCubic;
        private final boolean generates2DChunks;

        WorldStyle(boolean isCubic, boolean generates2DChunks) {

            this.isCubic = isCubic;
            this.generates2DChunks = generates2DChunks;
        }

        public boolean isCubic() {
            return isCubic;
        }

        public boolean generates2DChunks() {
            return generates2DChunks;
        }
    }
}
