package cubicchunks.lighting;

import cubicchunks.util.CubePos;
import net.minecraft.util.math.ChunkPos;

public interface ILightChunkAccess {
    /**
     * Returns ILightBlockAccess for the given CubePos
     */
    ILightBlockAccess getLightBlockAccess(CubePos pos);

    /**
     * Returns cube positions in the given position range that can be updated (loaded && first light already done)
     */
    Iterable<CubePos> getCubesToUpdate(ChunkPos pos, int minBlockY, int maxBlockY);
}
