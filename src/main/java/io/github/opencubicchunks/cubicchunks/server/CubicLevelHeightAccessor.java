package io.github.opencubicchunks.cubicchunks.server;

public interface CubicLevelHeightAccessor {

    default WorldStyle worldStyle() {
        return WorldStyle.CUBIC;
    }

    default Boolean isCubic() {
        return worldStyle().isCubic();
    }

    default Boolean generates2DChunks() {
        return worldStyle().generates2DChunks();
    }

    enum WorldStyle {
        CUBIC(true, false), // Primary Implementation (Generate Chunks 3D, infinite world height)
        HYBRID(true, true), // Soft implementation (Vanilla Chunk Generation, infinite world height)
        CHUNK(false, true); //Vanilla (2D Generation, World Height is NOT infinite)

        private final Boolean isCubic;
        private final Boolean generates2DChunks;

        WorldStyle(Boolean isCubic, Boolean generates2DChunks) {

            this.isCubic = isCubic;
            this.generates2DChunks = generates2DChunks;
        }

        public Boolean isCubic() {
                    if (isCubic == null)
            new Error().printStackTrace();
        return isCubic;
        }

        public Boolean generates2DChunks() {
                    if (generates2DChunks == null)
            new Error().printStackTrace();
        return generates2DChunks;
        }
    }
}
