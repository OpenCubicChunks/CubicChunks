package cubicchunks.cc.mixin.core.client.interfaces;


import cubicchunks.cc.CubicChunks;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IChunk.class)
public interface IMixinChunk extends IBlockReader {
    @Shadow ChunkSection[] getSections();

    /**
     * @author Voronoi
     * @reason Need to overwrite as this is an interface.
     */
    @Overwrite
    default boolean isEmptyBetween(int startY, int endY) {
        if (startY < 0) {
            startY = 0;
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
