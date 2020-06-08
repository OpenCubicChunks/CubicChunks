package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.meta.EarlyConfig;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.stream.Stream;

import javax.annotation.Nullable;

public interface ICube extends IBlockReader {

    int CUBE_DIAMETER = EarlyConfig.getCubeDiameter();
    int CUBE_SIZE = CUBE_DIAMETER * CUBE_DIAMETER * CUBE_DIAMETER;
    int BLOCK_SIZE = 16 * CUBE_DIAMETER;

    ChunkSection[] getCubeSections();
    CubePos getCubePos();

    void setCubeStatus(ChunkStatus status);
    ChunkStatus getCubeStatus();

    // this can't be setBlockState because the implementations also implement IChunk which already has setBlockState and this breaks obfuscation
    @Nullable BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving);

    // can't be add/removeTileEntity due to obfuscation issues with IChunk
    void addCubeTileEntity(BlockPos pos, TileEntity tileEntity);
    void removeCubeTileEntity(BlockPos pos);

    // can't be isModified due to obfuscation issues with IChunk
    boolean isDirty();

    void setDirty(boolean modified);

    boolean isEmptyCube();

    //can't be set/hasLight due to obfuscation issues with IChunk
    boolean hasCubeLight();
    void setCubeLight(boolean lightCorrectIn);

    @Override default BlockState getBlockState(BlockPos pos) {
        return getBlockState(Coords.localX(pos), Coords.localY(pos), Coords.localZ(pos));
    }

    BlockState getBlockState(int x, int y, int z);

    Stream<BlockPos> getCubeLightSources();
}
