package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import java.util.function.Predicate;

import io.github.opencubicchunks.cubicchunks.mixin.access.common.HeightmapAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class ClientSurfaceTracker extends Heightmap {

    protected final Predicate<BlockState> isOpaque;

    public ClientSurfaceTracker(ChunkAccess chunkAccess, Types types) {
        super(chunkAccess, types);
        this.isOpaque = ((HeightmapAccess) this).getIsOpaque();
    }

    /**
     * @param x column-local x
     * @param y global y
     * @param z column-local z
     */
    @Override public boolean update(int x, int y, int z, BlockState blockState) {
        int previous = getFirstAvailable(x, z);
        if (y <= previous - 2) {
            return false;
        }
        if (this.isOpaque.test(blockState)) {
            if (y >= previous) {
                ((HeightmapAccess) this).invokeSetHeight(x, z, y + 1);
                return true;
            }
            return true;
        }
        if (previous - 1 == y) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            ChunkAccess chunk = ((HeightmapAccess) this).getChunk();
            int currentY;
            for (currentY = y - 1; currentY >= y - 64; --currentY) {
                pos.set(x, currentY, z);
                if (this.isOpaque.test(chunk.getBlockState(pos))) {
                    ((HeightmapAccess) this).invokeSetHeight(x, z, currentY + 1);
                    return true;
                }
            }
            ((HeightmapAccess) this).invokeSetHeight(x, z, currentY);
            return true;
        }
        return false;
    }
}
