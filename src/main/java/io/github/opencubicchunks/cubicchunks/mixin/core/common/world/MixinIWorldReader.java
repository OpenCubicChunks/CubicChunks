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
    default boolean isAreaLoaded(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (toY >= -CubicChunks.worldMAXHeight && fromY < CubicChunks.worldMAXHeight) {
            fromX = fromX >> 4;
            fromZ = fromZ >> 4;
            toX = toX >> 4;
            toZ = toZ >> 4;

            for(int i = fromX; i <= toX; ++i) {
                for(int j = fromZ; j <= toZ; ++j) {
                    if (!this.chunkExists(i, j)) {
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
