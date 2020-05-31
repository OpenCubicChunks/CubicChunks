package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

import javax.annotation.Nullable;

public interface ICube extends IBlockReader {

    int CUBEDIAMETER = 2;
    int CUBESIZE = CUBEDIAMETER * CUBEDIAMETER * CUBEDIAMETER;
    int BLOCK_SIZE = 32;

    ChunkSection[] getCubeSections();
    CubePos getCubePos();

    void setCubeStatus(ChunkStatus status);
    ChunkStatus getCubeStatus();

    @Nullable BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving);

    void addTileEntity(BlockPos pos, TileEntity tileEntity);
    void removeTileEntity(BlockPos pos);

    boolean isModified();

    void setModified(boolean modified);

    @Override default BlockState getBlockState(BlockPos pos) {
        return getBlockState(Coords.localX(pos), Coords.localY(pos), Coords.localZ(pos));
    }

    BlockState getBlockState(int x, int y, int z);
}
