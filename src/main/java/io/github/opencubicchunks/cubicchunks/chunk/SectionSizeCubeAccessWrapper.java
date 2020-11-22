package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

//used only in Surface Builders
public class SectionSizeCubeAccessWrapper implements ChunkAccess {
    private final ChunkAccess[] delegates;
    private final CubePos pos;
    private int dx;
    private int dz;

    public SectionSizeCubeAccessWrapper(IBigCube delegate, IBigCube delegateAbove) {
        this.delegates = new ChunkAccess[2];
        this.delegates[0] = (ChunkAccess) delegate;
        this.delegates[1] = (ChunkAccess) delegateAbove;
        this.pos = delegate.getCubePos();
    }

    public void setLocalSectionPos(int sectionX, int sectionZ) {
        this.dx = Coords.sectionToMinBlock(sectionX);
        this.dz = Coords.sectionToMinBlock(sectionZ);
    }


    @Nullable @Override public BlockState setBlockState(BlockPos blockPos, BlockState blockState, boolean bl) {
        return getDelegate(blockPos.getY()).setBlockState(blockPos.offset(dx, 0, dz), blockState, bl);
    }

    @Override public void setBlockEntity(BlockEntity blockEntity) {
        getDelegate(blockEntity.getBlockPos().getY()).setBlockEntity(blockEntity);
    }

    @Override public void addEntity(Entity entity) {
        getDelegate(entity.getBlockY()).addEntity(entity);
    }

    @Override @Nullable public LevelChunkSection getHighestSection() {
        throw new UnsupportedOperationException();
    }

    @Override public int getHighestSectionPosition() {
        throw new UnsupportedOperationException();
    }

    @Override public Set<BlockPos> getBlockEntitiesPos() {
        throw new UnsupportedOperationException();
    }

    @Override public LevelChunkSection[] getSections() {
        throw new UnsupportedOperationException();
    }

    @Override public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException();
    }

    @Override public void setHeightmap(Heightmap.Types types, long[] ls) {
        throw new UnsupportedOperationException("Why are we setting heightmaps here?");
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types types) {
        throw new UnsupportedOperationException();
    }

    @Override public int getHeight(Heightmap.Types types, int x, int z) {
        IBigCube cube1 = (IBigCube) delegates[1];
        int localHeight = cube1.getCubeLocalHeight(types, dx, dz);
        return localHeight == getMinBuildHeight() ? ((IBigCube) delegates[0]).getCubeLocalHeight(types, dx, dz) : localHeight;
    }

    @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException();
    }

    @Override public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
        throw new UnsupportedOperationException();
    }

    @Override public void setAllStarts(Map<StructureFeature<?>, StructureStart<?>> map) {
        throw new UnsupportedOperationException("Why are we setting structure starts here?");
    }

    @Override public boolean isYSpaceEmpty(int i, int j) {
        throw new UnsupportedOperationException();
    }

    @Override @Nullable public ChunkBiomeContainer getBiomes() {
        throw new UnsupportedOperationException();
    }

    @Override public void setUnsaved(boolean bl) {
        throw new UnsupportedOperationException();
    }

    @Override public boolean isUnsaved() {
        throw new UnsupportedOperationException();
    }

    @Override public ChunkStatus getStatus() {
        return getDelegateCube(pos.getY()).getStatus();
    }

    @Override public void removeBlockEntity(BlockPos blockPos) {
        getDelegate(blockPos.getY()).removeBlockEntity(blockPos.offset(dx, 0, dz));
    }

    @Override public void markPosForPostprocessing(BlockPos blockPos) {
        getDelegate(blockPos.getY()).markPosForPostprocessing(blockPos.offset(dx, 0, dz));
    }

    @Override public ShortList[] getPostProcessing() {
        throw new UnsupportedOperationException();
    }

    @Override public void addPackedPostProcess(short s, int i) {
        throw new UnsupportedOperationException();
    }

    @Override public void setBlockEntityNbt(CompoundTag compoundTag) {
        throw new UnsupportedOperationException();
    }

    @Override @Nullable public CompoundTag getBlockEntityNbt(BlockPos blockPos) {
        throw new UnsupportedOperationException();
    }

    @Override @Nullable public CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos) {
        throw new UnsupportedOperationException();
    }

    @Override public Stream<BlockPos> getLights() {
        throw new UnsupportedOperationException();
    }

    @Override public TickList<Block> getBlockTicks() {
        throw new UnsupportedOperationException();
    }

    @Override public TickList<Fluid> getLiquidTicks() {
        throw new UnsupportedOperationException();
    }

    @Override public UpgradeData getUpgradeData() {
        throw new UnsupportedOperationException();
    }

    @Override public void setInhabitedTime(long l) {
        throw new UnsupportedOperationException();
    }

    @Override public long getInhabitedTime() {
        throw new UnsupportedOperationException();
    }

    @Override public boolean isLightCorrect() {
        throw new UnsupportedOperationException();
    }

    @Override public void setLightCorrect(boolean bl) {
        throw new UnsupportedOperationException();
    }

    @Override @Nullable public BlockEntity getBlockEntity(BlockPos blockPos) {
        return getDelegate(blockPos.getY()).getBlockEntity(blockPos.offset(dx, 0, dz));
    }

    @Override public BlockState getBlockState(BlockPos blockPos) {
        return getDelegate(blockPos.getY()).getBlockState(blockPos.offset(dx, 0, dz));
    }

    @Override public FluidState getFluidState(BlockPos blockPos) {
        return getDelegate(blockPos.getY()).getFluidState(blockPos.offset(dx, 0, dz));
    }

    @Override public int getLightEmission(BlockPos blockPos) {
        return getDelegate(blockPos.getY()).getLightEmission(blockPos.offset(dx, 0, dz));
    }

    @Override public int getMaxLightLevel() {
        throw new UnsupportedOperationException();
    }

    @Override public Stream<BlockState> getBlockStates(AABB aABB) {
        throw new UnsupportedOperationException();
    }

    @Override public BlockHitResult clip(ClipContext clipContext) {
        throw new UnsupportedOperationException();
    }

    @Override @Nullable public BlockHitResult clipWithInteractionOverride(Vec3 vec3, Vec3 vec32,
                                                                          BlockPos blockPos, VoxelShape voxelShape,
                                                                          BlockState blockState) {
        throw new UnsupportedOperationException();
    }

    @Override public double getBlockFloorHeight(VoxelShape voxelShape,
                                                Supplier<VoxelShape> supplier) {
        throw new UnsupportedOperationException();
    }

    @Override public double getBlockFloorHeight(BlockPos blockPos) {
        throw new UnsupportedOperationException();
    }

    @Override public int getSectionsCount() {
        return delegates[0].getSectionsCount();
    }

    @Override public int getMinSection() {
        return delegates[0].getMinSection();
    }

    @Override public int getMaxSection() {
        return delegates[0].getMaxSection();
    }

    @Override public int getHeight() {
        return delegates[0].getHeight();
    }

    @Override public int getMinBuildHeight() {
        return delegates[0].getMinBuildHeight();
    }

    @Override public int getMaxBuildHeight() {
        return delegates[0].getMaxBuildHeight();
    }

    @Override public boolean isOutsideBuildHeight(BlockPos blockPos) {
        return delegates[0].isOutsideBuildHeight(blockPos);
    }

    @Override public boolean isOutsideBuildHeight(int i) {
        return delegates[0].isOutsideBuildHeight(i);
    }

    @Override public int getSectionIndex(int i) {
        return delegates[0].getSectionIndex(i);
    }

    @Override public int getSectionIndexFromSectionY(int i) {
        return delegates[0].getSectionIndexFromSectionY(i);
    }

    @Override public int getSectionYFromSectionIndex(int i) {
        return delegates[0].getSectionYFromSectionIndex(i);
    }

    @Override @Nullable public StructureStart<?> getStartForFeature(StructureFeature<?> structureFeature) {
        throw new UnsupportedOperationException();
    }

    @Override public void setStartForFeature(StructureFeature<?> structureFeature, StructureStart<?> structureStart) {
        throw new UnsupportedOperationException("Why are we setting structure starts here?");
    }

    @Override public LongSet getReferencesForFeature(StructureFeature<?> structureFeature) {
        throw new UnsupportedOperationException();
    }

    @Override public void addReferenceForFeature(StructureFeature<?> structureFeature, long l) {
        throw new UnsupportedOperationException("Why are we adding structure references here?");
    }

    @Override public Map<StructureFeature<?>, LongSet> getAllReferences() {
        throw new UnsupportedOperationException();
    }

    @Override public void setAllReferences(Map<StructureFeature<?>, LongSet> map) {
        throw new UnsupportedOperationException();
    }

    public ChunkAccess getDelegateCube(int y) {
        return delegates[y & 1];
    }

    public ChunkAccess getDelegate(int blockY) {
        return getDelegateCube(Coords.blockToCube(blockY));
    }
}
