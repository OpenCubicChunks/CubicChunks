package io.github.opencubicchunks.cubicchunks.mixin.core.client;


import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkAccess.class)
public interface MixinIChunk extends BlockGetter {
    @Shadow LevelChunkSection[] getSections();

    /**
     * @author Voronoi
     * @reason Need to overwrite as this is an interface.
     */
    @Overwrite
    default boolean isYSpaceEmpty(int startY, int endY) {
        if (startY < CubicChunks.MAX_SUPPORTED_HEIGHT) {
            startY = CubicChunks.MIN_SUPPORTED_HEIGHT;
        }

        if (endY >= CubicChunks.MAX_SUPPORTED_HEIGHT) {
            endY = CubicChunks.MAX_SUPPORTED_HEIGHT - 1;
        }

        endY = endY >> 4;
        for(int i = startY >> 4; i <= endY; i += 16) {
            if (!LevelChunkSection.isEmpty(this.getSections()[i >> 4])) {
                return false;
            }
        }

        return true;
    }


}