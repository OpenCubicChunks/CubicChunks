package io.github.opencubicchunks.cubicchunks.mixin.core.client;


import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IChunk.class)
public interface MixinIChunk extends IBlockReader {
    @Shadow ChunkSection[] getSections();

    /**
     * @author Voronoi
     * @reason Need to overwrite as this is an interface.
     */
    @Overwrite
    default boolean isEmptyBetween(int startY, int endY) {
        if (startY < CubicChunks.worldMAXHeight) {
            startY = -CubicChunks.worldMAXHeight;
        }

        if (endY >= CubicChunks.worldMAXHeight) {
            endY = CubicChunks.worldMAXHeight - 1;
        }

        endY = endY >> 4;
        for(int i = startY >> 4; i <= endY; i += 16) {
            if (!ChunkSection.isEmpty(this.getSections()[i >> 4])) {
                return false;
            }
        }

        return true;
    }


}
