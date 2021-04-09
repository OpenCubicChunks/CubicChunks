package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

//TODO: Implement this properly for mods. Vanilla is fine.
public class CubeWorldRegionColumn extends ProtoChunk {

    private final CubeWorldGenRegion cubeWorldGenRegion;

    public CubeWorldRegionColumn(ChunkPos chunkPos, UpgradeData upgradeData, CubeWorldGenRegion cubeWorldGenRegion) {
        super(chunkPos, upgradeData, cubeWorldGenRegion);
        this.cubeWorldGenRegion = cubeWorldGenRegion;
    }

    @Override public BlockState getBlockState(BlockPos pos) {
        return this.cubeWorldGenRegion.getCube(pos).getBlockState(pos);
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        return this.cubeWorldGenRegion.getCube(pos).getFluidState(pos);
    }

    @Override public Stream<BlockPos> getLights() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public ShortList[] getPackedLights() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void addLight(short chunkSliceRel, int sectionY) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void addLight(BlockPos pos) {
        ((CubePrimer) this.cubeWorldGenRegion.getCube(pos)).addLight(pos);
    }

    @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        return this.cubeWorldGenRegion.getCube(pos).setBlockState(pos, state, moved);
    }

    @Override public void setBlockEntity(BlockEntity blockEntity) {
        this.cubeWorldGenRegion.getCube(blockEntity.getBlockPos()).setBlockEntity(blockEntity);
    }

    @Override public Set<BlockPos> getBlockEntitiesPos() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Nullable @Override public BlockEntity getBlockEntity(BlockPos pos) {
        return this.cubeWorldGenRegion.getCube(pos).getBlockEntity(pos);
    }

    @Override public Map<BlockPos, BlockEntity> getBlockEntities() {
        return super.getBlockEntities();
    }

    @Override public void addEntity(CompoundTag entityTag) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void addEntity(Entity entity) {
        this.cubeWorldGenRegion.getCube(entity.blockPosition()).addEntity(entity);
    }

    @Override public List<CompoundTag> getEntities() {
        return super.getEntities();
    }

    @Override public void setBiomes(ChunkBiomeContainer biomes) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Nullable @Override public ChunkBiomeContainer getBiomes() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setUnsaved(boolean shouldSave) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public boolean isUnsaved() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public ChunkStatus getStatus() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setStatus(ChunkStatus status) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public LevelChunkSection[] getSections() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setHeightmap(Heightmap.Types type, long[] heightmap) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public int getHeight(Heightmap.Types type, int x, int z) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public BlockPos getHeighestPosition(Heightmap.Types types) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Nullable @Override public StructureStart<?> getStartForFeature(StructureFeature<?> structure) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setStartForFeature(StructureFeature<?> structure, StructureStart<?> start) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setAllStarts(Map<StructureFeature<?>, StructureStart<?>> structureStarts) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public LongSet getReferencesForFeature(StructureFeature<?> structure) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void addReferenceForFeature(StructureFeature<?> structure, long reference) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public Map<StructureFeature<?>, LongSet> getAllReferences() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setAllReferences(Map<StructureFeature<?>, LongSet> structureReferences) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void markPosForPostprocessing(BlockPos pos) {
        this.cubeWorldGenRegion.getCube(pos).markPosForPostprocessing(pos);
    }

    @Override public ShortList[] getPostProcessing() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void addPackedPostProcess(short packedPos, int index) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public ProtoTickList<Block> getBlockTicks() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public ProtoTickList<Fluid> getLiquidTicks() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public UpgradeData getUpgradeData() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setInhabitedTime(long inhabitedTime) {
        super.setInhabitedTime(inhabitedTime);
    }

    @Override public long getInhabitedTime() {
        return super.getInhabitedTime();
    }

    @Override public void setBlockEntityNbt(CompoundTag nbt) {
        super.setBlockEntityNbt(nbt);
    }

    @Override public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return this.cubeWorldGenRegion.getCube(pos).getBlockEntityNbt(pos);
    }

    @Nullable @Override public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        return this.cubeWorldGenRegion.getCube(pos).getBlockEntityNbt(pos);
    }

    @Override public void removeBlockEntity(BlockPos pos) {
        this.cubeWorldGenRegion.getCube(pos).getBlockEntityNbt(pos);
    }

    @Nullable @Override public BitSet getCarvingMask(GenerationStep.Carving carver) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public BitSet getOrCreateCarvingMask(GenerationStep.Carving carver) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setCarvingMask(GenerationStep.Carving carver, BitSet mask) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setLightEngine(LevelLightEngine lightingProvider) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public boolean isLightCorrect() {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public void setLightCorrect(boolean lightOn) {
        throw new UnsupportedOperationException("This is not yet implemented.");
    }

    @Override public int getMinBuildHeight() {
        return this.cubeWorldGenRegion.getMinBuildHeight();
    }

    @Override public int getHeight() {
        return this.cubeWorldGenRegion.getHeight();
    }
}
