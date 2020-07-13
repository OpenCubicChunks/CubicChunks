package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.meta.EarlyConfig;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import org.apache.logging.log4j.LogManager;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public interface IBigCube extends IBlockReader {

    int CUBE_DIAMETER = EarlyConfig.getCubeDiameter();
    int CUBE_SIZE = CUBE_DIAMETER * CUBE_DIAMETER * CUBE_DIAMETER;
    int BLOCK_SIZE = 16 * CUBE_DIAMETER;

    ChunkSection[] getCubeSections();
    CubePos getCubePos();

    void setCubeStatus(ChunkStatus status);
    ChunkStatus getCubeStatus();

    // this can't be setBlockState because the implementations also implement IChunk which already has setBlockState and this breaks obfuscation
    @Nullable BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving);

    void addCubeEntity(Entity entityIn);

    // can't be add/removeTileEntity due to obfuscation issues with IChunk
    default void addCubeTileEntity(CompoundNBT nbt) {
        LogManager.getLogger().warn("Trying to set a BlockEntity, but this operation is not supported.");
    }
    void addCubeTileEntity(BlockPos pos, TileEntity tileEntity);
    void removeCubeTileEntity(BlockPos pos);

    @Nullable CompoundNBT getCubeTileEntityNBT(BlockPos pos);
    Set<BlockPos> getCubeTileEntitiesPos();

    @Nullable CompoundNBT getCubeDeferredTileEntity(BlockPos pos);

    // can't be isModified due to obfuscation issues with IChunk
    boolean isDirty();

    void setDirty(boolean modified);

    boolean isEmptyCube();

    //can't be set/hasLight due to obfuscation issues with IChunk
    boolean hasCubeLight();
    void setCubeLight(boolean lightCorrectIn);

    void setCubeInhabitedTime(long newCubeInhabitedTime);
    long getCubeInhabitedTime();

    @Override default BlockState getBlockState(BlockPos pos) {
        return getBlockState(Coords.localX(pos), Coords.localY(pos), Coords.localZ(pos));
    }

    BlockState getBlockState(int x, int y, int z);

    @Nullable CubeBiomeContainer getCubeBiomes();

    Stream<BlockPos> getCubeLightSources();
}
