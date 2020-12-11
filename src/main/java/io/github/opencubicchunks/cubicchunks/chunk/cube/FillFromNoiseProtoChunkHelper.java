package io.github.opencubicchunks.cubicchunks.chunk.cube;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.DummyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class FillFromNoiseProtoChunkHelper extends ProtoChunk {

    public final CubePrimer cubePrimer;
    private int columnX;
    private int columnZ;
    private final Map<Heightmap.Types, Heightmap> heightmaps;


    public FillFromNoiseProtoChunkHelper(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, CubePrimer cubePrimer) {
        super(chunkPos, upgradeData, levelHeightAccessor);
        this.cubePrimer = cubePrimer;
        this.heightmaps = Maps.newEnumMap(Heightmap.Types.class);
    }

    public void moveColumn(int columnX, int columnZ) {
        this.columnX = columnX;
        this.columnZ = columnZ;
    }

    @Override public ChunkPos getPos() {
        return cubePrimer.getCubePos().asChunkPos(columnX, columnZ);
    }

    @Override public int getHeight(Heightmap.Types type, int x, int z) {
        int blockX = Coords.localToBlock(cubePrimer.getCubePos().getX(), x) + (columnX * 16);
        int blockZ = Coords.localToBlock(cubePrimer.getCubePos().getX(), z) + (columnZ * 16);
        return this.cubePrimer.getHeight(type, blockX, blockZ);
    }

    @Override public LevelChunkSection getOrCreateSection(int sectionIndex) {
        LevelChunkSection[] cubeSections = this.cubePrimer.getCubeSections();

        if (cubeSections[sectionIndex] == LevelChunk.EMPTY_SECTION) {
            cubeSections[sectionIndex] = new LevelChunkSection(this.getSectionYFromSectionIndex(sectionIndex));
        }
        return cubeSections[sectionIndex];
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.heightmaps.computeIfAbsent(type, (typex) -> {
            return new DummyHeightmap(this, typex); //Essentially do nothing here.
        });
    }

    @Override public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    @Override public int getSectionIndex(int y) {
        return Coords.sectionToIndex(getPos().x, Coords.blockToSection(y), getPos().z);
    }

    @Override public int getMinBuildHeight() {
        return cubePrimer.getCubePos().minCubeY();
    }

    @Override public int getSectionYFromSectionIndex(int sectionIndex) {
        return cubePrimer.getCubePos().asSectionPos().getY() + Coords.indexToY(sectionIndex);
    }

    @Override public int getHeight() {
        return IBigCube.DIAMETER_IN_BLOCKS;
    }

    @Override public void addLight(BlockPos pos) {
        //TODO
    }

    @Override public BlockState getBlockState(BlockPos pos) {
        BlockPos blockPos = new BlockPos(Coords.localToBlock(cubePrimer.getCubePos().getX(), pos.getX()) + (columnX * 16), pos.getY(),
            Coords.localToBlock(cubePrimer.getCubePos().getZ(), pos.getZ()) + (columnZ * 16));

        return this.cubePrimer.getBlockState(blockPos);
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        BlockPos blockPos = new BlockPos(Coords.localToBlock(cubePrimer.getCubePos().getX(), pos.getX()) + (columnX * 16), pos.getY(),
            Coords.localToBlock(cubePrimer.getCubePos().getZ(), pos.getZ() + (columnZ * 16)));
        return cubePrimer.getFluidState(blockPos);
    }


    @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        BlockPos blockPos = new BlockPos(Coords.localToBlock(cubePrimer.getCubePos().getX(), pos.getX()) + (columnX * 16), pos.getY(),
            Coords.localToBlock(cubePrimer.getCubePos().getZ(), pos.getZ() + (columnZ * 16)));

        return cubePrimer.setBlock(blockPos, state, moved);
    }
}
