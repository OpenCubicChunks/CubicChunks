package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.meta.EarlyConfig;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import org.apache.logging.log4j.LogManager;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public interface IBigCube extends IBlockReader {

    int SECTION_DIAMETER = 16;
    int DIAMETER_IN_SECTIONS = EarlyConfig.getDiameterInSections();
    int SECTION_COUNT = DIAMETER_IN_SECTIONS * DIAMETER_IN_SECTIONS * DIAMETER_IN_SECTIONS;
    int CHUNK_COUNT = DIAMETER_IN_SECTIONS * DIAMETER_IN_SECTIONS;
    int DIAMETER_IN_BLOCKS = SECTION_DIAMETER * DIAMETER_IN_SECTIONS;
    int BLOCK_COUNT = DIAMETER_IN_BLOCKS * DIAMETER_IN_BLOCKS * DIAMETER_IN_BLOCKS;
    int BLOCK_COLUMNS_PER_SECTION = SECTION_DIAMETER * SECTION_DIAMETER;

    CubePos getCubePos();
    ChunkSection[] getCubeSections();

    //STATUS
    //TODO: remove setCubeStatus from IBigCube to match IChunk
    void setCubeStatus(ChunkStatus status);
    ChunkStatus getCubeStatus();

    //BLOCK
    // this can't be setBlockState because the implementations also implement IChunk which already has setBlockState and this breaks obfuscation
    @Nullable BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving);

    @Override default BlockState getBlockState(BlockPos pos) {
        return getBlockState(Coords.localX(pos), Coords.localY(pos), Coords.localZ(pos));
    }
    //TODO: remove this getBlockState from IBigCube to match IChunk
    BlockState getBlockState(int x, int y, int z);

    //ENTITY
    void addCubeEntity(Entity entityIn);

    //TILE ENTITY
    // can't be add/removeTileEntity due to obfuscation issues with IChunk
    default void addCubeTileEntity(CompoundNBT nbt) {
        LogManager.getLogger().warn("Trying to set a BlockEntity, but this operation is not supported.");
    }
    void addCubeTileEntity(BlockPos pos, TileEntity tileEntity);
    void removeCubeTileEntity(BlockPos pos);

    Set<BlockPos> getCubeTileEntitiesPos();

    @Nullable CompoundNBT getCubeTileEntityNBT(BlockPos pos);

    @Nullable CompoundNBT getCubeDeferredTileEntity(BlockPos pos);

    //LIGHTING
    //can't be set/hasLight due to obfuscation issues with IChunk
    boolean hasCubeLight();
    void setCubeLight(boolean lightCorrectIn);

    Stream<BlockPos> getCubeLightSources();

    //MISC
    // can't be isModified due to obfuscation issues with IChunk
    void setDirty(boolean modified);
    boolean isDirty();

    void setCubeLastSaveTime(long time);

    //TODO: remove isEmptyCube from IBigCube to match IChunk
    boolean isEmptyCube();

    void setCubeInhabitedTime(long newCubeInhabitedTime);
    long getCubeInhabitedTime();

    @Nullable CubeBiomeContainer getCubeBiomes();
}