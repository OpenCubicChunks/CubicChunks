package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.Map;

import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ProtoChunkAccess;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.DummyHeightmap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class RegionChunk extends ProtoChunk {
    private final CubePrimer currentlyGenerating;
    private final CubeWorldGenRegion region;

    public RegionChunk(CubePrimer currentlyGenerating, CubeWorldGenRegion region) {
        super(currentlyGenerating.getPos(), UpgradeData.EMPTY, region.getLevel());
        this.currentlyGenerating = currentlyGenerating;
        this.region = region;
    }


    public CubePrimer getCube(BlockPos pos) {
        return (CubePrimer) this.region.getCube(pos);
    }

    @Override public int getSectionIndex(int y) {
        return Coords.blockToCubeLocalSection(y) + IBigCube.DIAMETER_IN_SECTIONS * getDelegateIndex(Coords.blockToCube(y));
    }

    public int getDelegateIndex(int y) {
        int minY = Coords.blockToCube(getMinBuildHeight());
        if (y < minY) {
            return -1;
        }
        if (y > Coords.blockToCube(getMaxBuildHeight())) {
            return -1;
        }
        return y - minY;
    }

//    @Override public int getSectionYFromSectionIndex(int sectionIndex) {
//        int delegateIDX = sectionIndex / IBigCube.DIAMETER_IN_SECTIONS;
//        int cubeSectionIDX = sectionIndex % IBigCube.DIAMETER_IN_SECTIONS;
//        return getDelegateByIndex(delegateIDX).getCubePos().asSectionPos().getY() + cubeSectionIDX;
//    }

    @Override public BlockState getBlockState(BlockPos pos) {
        return getCube(pos).getBlockState(pos);
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        return getCube(pos).getFluidState(pos);
    }

    @Override public void addLight(BlockPos pos) {
        getCube(pos).addLight(pos);
    }

    @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        return getCube(pos).setBlockState(pos, state, moved);
    }

    @Override public void setBlockEntity(BlockEntity blockEntity) {
        getCube(blockEntity.getBlockPos()).setBlockEntity(blockEntity);
    }

    @Nullable @Override public BlockEntity getBlockEntity(BlockPos pos) {
        return getCube(pos).getBlockEntity(pos);
    }

    @Override public void addEntity(Entity entity) {
        getCube(entity.blockPosition()).addEntity(entity);
    }

    @Override public void setBiomes(ChunkBiomeContainer biomes) {
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return ((ProtoChunkAccess) this).getHeightmaps().computeIfAbsent(type, (typex) -> {
            return new DummyHeightmap(this, typex); //Essentially do nothing here.
        });
    }

    @Override public int getHeight(Heightmap.Types type, int x, int z) {
        return currentlyGenerating.getHeight(type, x, z);
    }

    @Override public ChunkPos getPos() {
        return currentlyGenerating.getPos();
    }

    @Nullable @Override public StructureStart<?> getStartForFeature(StructureFeature<?> structure) {
        return currentlyGenerating.getStartForFeature(structure);
    }

    @Override public void setStartForFeature(StructureFeature<?> structure, StructureStart<?> start) {
        currentlyGenerating.setStartForFeature(structure, start);
    }

    @Override public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
        return currentlyGenerating.getAllStarts();
    }

    @Override public void setAllStarts(Map<StructureFeature<?>, StructureStart<?>> structureStarts) {
        currentlyGenerating.setAllStarts(structureStarts);
    }

    @Override public LongSet getReferencesForFeature(StructureFeature<?> structure) {
        return currentlyGenerating.getReferencesForFeature(structure);
    }

    @Override public void addReferenceForFeature(StructureFeature<?> structure, long reference) {
        currentlyGenerating.addReferenceForFeature(structure, reference);
    }

    @Override public Map<StructureFeature<?>, LongSet> getAllReferences() {
        return currentlyGenerating.getAllReferences();
    }

    @Override public void setAllReferences(Map<StructureFeature<?>, LongSet> structureReferences) {
        currentlyGenerating.setAllReferences(structureReferences);
    }

    @Override public void markPosForPostprocessing(BlockPos pos) {
        getCube(pos).markPosForPostprocessing(pos);
    }


    @Override public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return getCube(pos).getBlockEntityNbt(pos);
    }

    @Nullable @Override public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        return getCube(pos).getBlockEntityNbtForSaving(pos);
    }

    @Override public void removeBlockEntity(BlockPos pos) {
        getCube(pos).removeBlockEntity(pos);
    }

    @Override public int getMinBuildHeight() {
        return currentlyGenerating.getCubePos().minCubeY() - 2;
    }

    @Override public int getHeight() {
        return IBigCube.DIAMETER_IN_BLOCKS + 8;
    }
}