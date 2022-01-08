package io.github.opencubicchunks.cubicchunks.core.lighting.phosphor;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IBlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class LightingEngineHelpers {
    private static final IBlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();

    // Avoids some additional logic in Chunk#getBlockState... 0 is always air
    static IBlockState posToState(final BlockPos pos, final ICube chunk) {
        return posToState(pos, chunk.getStorage());
    }

    static IBlockState posToState(final BlockPos pos, final ExtendedBlockStorage section) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        if (section != Chunk.NULL_BLOCK_STORAGE) {
            IBlockStateContainer data = (IBlockStateContainer) section.getData();
            int i = data.getStorage().getAt((y & 15) << 8 | (z & 15) << 4 | x & 15);

            if (i != 0) {
                IBlockState state = data.getPalette().getBlockState(i);
                if (state != null) {
                    return state;
                }
            }
        }

        return DEFAULT_BLOCK_STATE;
    }
}
