package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

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
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

//used only in Surface Builders
public class SectionSizeCubeAccessWrapper implements ChunkAccess {
    private final ChunkAccess[] delegates;
    private int dx;
    private int dz;

    public SectionSizeCubeAccessWrapper(IBigCube delegate, IBigCube delegateAbove) {
        this.delegates = new ChunkAccess[2];
        this.delegates[0] = (ChunkAccess) delegate;
        this.delegates[1] = (ChunkAccess) delegateAbove;
    }

    public void setLocalSectionPos(int sectionX, int sectionZ) {
        this.dx = Coords.sectionToMinBlock(sectionX);
        this.dz = Coords.sectionToMinBlock(sectionZ);
    }


    @Nullable @Override public BlockState setBlockState(BlockPos blockPos, BlockState blockState, boolean bl) {
        ChunkAccess delegate = getDelegate(blockPos.getY());
        if (delegate != null) {
            return delegate.setBlockState(blockPos.offset(dx, 0, dz), blockState, bl);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override public void setBlockEntity(BlockEntity blockEntity) {
        ChunkAccess delegate = getDelegate(blockEntity.getBlockPos().getY());
        if (delegate != null) {
            delegate.setBlockEntity(blockEntity);
        }
    }

    @Override public void addEntity(Entity entity) {
        ChunkAccess delegate = getDelegate(entity.getBlockY());
        if (delegate != null) {
            delegate.addEntity(entity);
        }
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
        return delegates[0].getStatus();
    }

    @Override public void removeBlockEntity(BlockPos blockPos) {
        ChunkAccess delegate = getDelegate(blockPos.getY());
        if (delegate != null) {
            delegate.removeBlockEntity(blockPos.offset(dx, 0, dz));
        }
    }

    @Override public void markPosForPostprocessing(BlockPos blockPos) {
        ChunkAccess delegate = getDelegate(blockPos.getY());
        if (delegate != null) {
            delegate.markPosForPostprocessing(blockPos.offset(dx, 0, dz));
        }
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
        ChunkAccess delegate = getDelegate(blockPos.getY());
        if (delegate != null) {
            return delegate.getBlockEntity(blockPos.offset(dx, 0, dz));
        }
        return null;
    }

    @Override public BlockState getBlockState(BlockPos blockPos) {
        ChunkAccess delegate = getDelegate(blockPos.getY());
        if (delegate != null) {
            return delegate.getBlockState(blockPos.offset(dx, 0, dz));
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override public FluidState getFluidState(BlockPos blockPos) {
        ChunkAccess delegate = getDelegate(blockPos.getY());
        if (delegate != null) {
            return delegate.getFluidState(blockPos.offset(dx, 0, dz));
        }
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override public int getLightEmission(BlockPos blockPos) {
        ChunkAccess delegate = getDelegate(blockPos.getY());
        if (delegate != null) {
            return delegate.getLightEmission(blockPos.offset(dx, 0, dz));
        }
        return 0;
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

    @Nullable public ChunkAccess getDelegateCube(int y) {
        int minY = ((IBigCube) delegates[0]).getCubePos().getY();
        if (y < minY) {
            throw StopGeneratingThrowable.INSTANCE;
        }
        if (y > ((IBigCube) delegates[1]).getCubePos().getY()) {
            return null;
        }
        return delegates[y - minY];
    }

    @Nullable public ChunkAccess getDelegate(int blockY) {
        return getDelegateCube(Coords.blockToCube(blockY));
    }

    public static class StopGeneratingThrowable extends RuntimeException {
        public static final StopGeneratingThrowable INSTANCE = new StopGeneratingThrowable();

        public StopGeneratingThrowable() {
            super("Stop the surface builder");
        }
    }
}
