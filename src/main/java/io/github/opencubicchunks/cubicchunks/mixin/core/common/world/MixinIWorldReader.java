package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.IWorldReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IWorldReader.class)
public interface MixinIWorldReader extends IWorldReader {

    /**
     * @author OverInfrared
     * @reason (Interface Overwrite) Must overwrite for swimming to work below 0 and above 256
     */
    @Deprecated
    @Overwrite
    default boolean hasChunksAt(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (toY >= CubicChunks.MIN_SUPPORTED_HEIGHT && fromY < CubicChunks.MAX_SUPPORTED_HEIGHT) {
            fromX = fromX >> 4;
            fromZ = fromZ >> 4;
            toX = toX >> 4;
            toZ = toZ >> 4;

            for(int i = fromX; i <= toX; ++i) {
                for(int j = fromZ; j <= toZ; ++j) {
                    if (!this.hasChunk(i, j)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

}