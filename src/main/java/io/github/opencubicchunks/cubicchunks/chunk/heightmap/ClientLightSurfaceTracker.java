package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.HeightmapAccess;
import io.github.opencubicchunks.cubicchunks.world.lighting.ISkyLightColumnChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClientLightSurfaceTracker extends ClientSurfaceTracker {
    private final int bitsPerColumn;
    private final int minHeight;
    public ClientLightSurfaceTracker(ChunkAccess chunkAccess) {
        // type shouldn't matter
        super(chunkAccess, Types.WORLD_SURFACE);
        bitsPerColumn = Mth.ceillog2(((HeightmapAccess) this).getChunk().getHeight() + 1);
        minHeight = ((HeightmapAccess) this).getChunk().getMinBuildHeight();
    }

    @Override public boolean update(int x, int y, int z, BlockState blockState) {
        return false;/*
        // TODO is it safe to do this or are we risking causing cube loading, etc?
        int previous = getFirstAvailable(x, z);
        if (y <= previous - 2) {
            return false;
        }
        ChunkAccess chunk = ((HeightmapAccess) this).getChunk();
        BlockPos blockPos = new BlockPos(x, y, z);
        BlockPos abovePos = new BlockPos(x, y+1, z);
        BlockState above = chunk.getBlockState(abovePos);
        int lightBlock = blockState.getLightBlock(chunk, blockPos);
        if (lightBlock > 0 || (above != null && Shapes.faceShapeOccludes(getShape(above, abovePos, Direction.DOWN), getShape(blockState, blockPos, Direction.UP)))) {
            if (y >= previous) {
                ((HeightmapAccess) this).invokeSetHeight(x, z, y + 1);
                return true;
            }
            return true;
        }
        if (previous - 1 == y) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            int currentY;
            for (currentY = y - 1; currentY >= y - 64; --currentY) {
                pos.set(x, currentY, z);
                // TODO copy the face shape occlusion logic here too - I'm too lazy for now.
                if (blockState.getLightBlock(chunk, pos) > 0) {
                    ((HeightmapAccess) this).invokeSetHeight(x, z, currentY + 1);
                    return true;
                }
            }
            ((HeightmapAccess) this).invokeSetHeight(x, z, currentY);
            return true;
        }
        return false;*/
    }

    protected VoxelShape getShape(BlockState blockState, BlockPos pos, Direction facing) {
        return blockState.canOcclude() && blockState.useShapeForLightOcclusion() ? blockState.getFaceOcclusionShape(((HeightmapAccess) this).getChunk(), pos, facing) : Shapes.empty();
    }

    private static int getIndex(int x, int z) {
        return x + z * 16;
    }

    @Override
    public void setRawData(long[] heightmap) {
    	throw new UnsupportedOperationException("this shouldn't be called");
	}
    public void setRawData(long[] heightmap, LevelChunk chunk) {
        // We need to compare the old and new data here, hence the inefficiencies with making a new bitstorage
        BitStorage storage = ((HeightmapAccess) this).getData();
        BitStorage oldStorage = new BitStorage(bitsPerColumn, 256, storage.getRaw().clone());
        System.arraycopy(heightmap, 0, storage.getRaw(), 0, heightmap.length);
//        ChunkAccess chunk = ((HeightmapAccess) this).getChunk();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int index = getIndex(x, z);
                int oldHeight = oldStorage.get(index) + minHeight;
                int newHeight = storage.get(index) + minHeight;
                if (oldHeight != newHeight) {
                    ((ISkyLightColumnChecker) Minecraft.getInstance().level.getLightEngine()).checkSkyLightColumn(chunk, baseX + x, baseZ + z, oldHeight, newHeight);
                }
            }
        }
    }
}
