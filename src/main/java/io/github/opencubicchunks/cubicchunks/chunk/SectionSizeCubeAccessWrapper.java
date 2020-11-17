package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SectionSizeCubeAccessWrapper implements ChunkAccess, IBigCube {
    private final ChunkAccess delegate;
    private final IBigCube delegateCube;
    private int dx;
    private int dz;

    public SectionSizeCubeAccessWrapper(ChunkAccess delegate) {
        this.delegate = delegate;
        this.delegateCube = (IBigCube) delegate;
    }

    public void setLocalSectionPos(int sectionX, int sectionZ) {
        this.dx = Coords.sectionToMinBlock(sectionX);
        this.dz = Coords.sectionToMinBlock(sectionZ);
    }

    @Override public CubePos getCubePos() {
        return delegateCube.getCubePos();
    }

    @Override public LevelChunkSection[] getCubeSections() {
        return delegateCube.getCubeSections();
    }

    @Override public void setCubeStatus(ChunkStatus status) {
        delegateCube.setCubeStatus(status);
    }

    @Override public ChunkStatus getCubeStatus() {
        return delegateCube.getCubeStatus();
    }

    @Override @javax.annotation.Nullable public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        return delegateCube.setBlock(pos.offset(dx, 0, dz), state, isMoving);
    }

    @Override public BlockState getBlockState(int x, int y, int z) {
        return delegateCube.getBlockState(x + dx, y, z + dz);
    }

    @Override public void setCubeBlockEntity(CompoundTag nbt) {
        delegateCube.setCubeBlockEntity(nbt);
    }

    @Override public void setCubeBlockEntity(BlockEntity tileEntity) {
        delegateCube.setCubeBlockEntity(tileEntity);
    }

    @Override public void removeCubeBlockEntity(BlockPos pos) {
        delegateCube.removeCubeBlockEntity(pos.offset(dx, 0, dz));
    }

    @Override public Set<BlockPos> getCubeTileEntitiesPos() {
        return delegateCube.getCubeTileEntitiesPos();
    }

    @Override @javax.annotation.Nullable public CompoundTag getCubeBlockEntityNbtForSaving(BlockPos pos) {
        return delegateCube.getCubeBlockEntityNbtForSaving(pos.offset(dx, 0, dz));
    }

    @Override @javax.annotation.Nullable public CompoundTag getCubeDeferredTileEntity(BlockPos pos) {
        return delegateCube.getCubeDeferredTileEntity(pos.offset(dx, 0, dz));
    }

    @Override public boolean hasCubeLight() {
        return delegateCube.hasCubeLight();
    }

    @Override public void setCubeLight(boolean lightCorrectIn) {
        delegateCube.setCubeLight(lightCorrectIn);
    }

    @Override public Stream<BlockPos> getCubeLightSources() {
        return delegateCube.getCubeLightSources();
    }

    @Override public void setDirty(boolean modified) {
        delegateCube.setDirty(modified);
    }

    @Override public boolean isDirty() {
        return delegateCube.isDirty();
    }

    @Override public boolean isEmptyCube() {
        return delegateCube.isEmptyCube();
    }

    @Override public void setCubeInhabitedTime(long newCubeInhabitedTime) {
        delegateCube.setCubeInhabitedTime(newCubeInhabitedTime);
    }

    @Override public long getCubeInhabitedTime() {
        return delegateCube.getCubeInhabitedTime();
    }

    @Override @javax.annotation.Nullable public CubeBiomeContainer getCubeBiomes() {
        return delegateCube.getCubeBiomes();
    }

    @Override @Nullable public BlockState setBlockState(BlockPos blockPos,
                                                        BlockState blockState, boolean bl) {
        return delegate.setBlockState(blockPos.offset(dx, 0, dz), blockState, bl);
    }

    @Override public void setBlockEntity(BlockEntity blockEntity) {
        delegate.setBlockEntity(blockEntity);
    }

    @Override public void addEntity(Entity entity) {
        delegate.addEntity(entity);
    }

    @Override @Nullable public LevelChunkSection getHighestSection() {
        return delegate.getHighestSection();
    }

    @Override public int getHighestSectionPosition() {
        return delegate.getHighestSectionPosition();
    }

    @Override public Set<BlockPos> getBlockEntitiesPos() {
        return delegate.getBlockEntitiesPos();
    }

    @Override public LevelChunkSection[] getSections() {
        return delegate.getSections();
    }

    @Override public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return delegate.getHeightmaps();
    }

    @Override public void setHeightmap(Heightmap.Types types, long[] ls) {
        delegate.setHeightmap(types, ls);
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types types) {
        return delegate.getOrCreateHeightmapUnprimed(types);
    }

    @Override public int getHeight(Heightmap.Types types, int i, int j) {
        return delegate.getHeight(types, i + dx, j + dz);
    }

    @Override public ChunkPos getPos() {
        return delegate.getPos();
    }

    @Override public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
        return delegate.getAllStarts();
    }

    @Override public void setAllStarts(
            Map<StructureFeature<?>, StructureStart<?>> map) {
        delegate.setAllStarts(map);
    }

    @Override public boolean isYSpaceEmpty(int i, int j) {
        return delegate.isYSpaceEmpty(i + dx, j + dz);
    }

    @Override @Nullable public ChunkBiomeContainer getBiomes() {
        return delegate.getBiomes();
    }

    @Override public void setUnsaved(boolean bl) {
        delegate.setUnsaved(bl);
    }

    @Override public boolean isUnsaved() {
        return delegate.isUnsaved();
    }

    @Override public ChunkStatus getStatus() {
        return delegate.getStatus();
    }

    @Override public void removeBlockEntity(BlockPos blockPos) {
        delegate.removeBlockEntity(blockPos.offset(dx, 0, dz));
    }

    @Override public void markPosForPostprocessing(BlockPos blockPos) {
        delegate.markPosForPostprocessing(blockPos.offset(dx, 0, dz));
    }

    @Override public ShortList[] getPostProcessing() {
        return delegate.getPostProcessing();
    }

    @Override public void addPackedPostProcess(short s, int i) {
        delegate.addPackedPostProcess(s, i);
    }

    @Override public void setBlockEntityNbt(CompoundTag compoundTag) {
        delegate.setBlockEntityNbt(compoundTag);
    }

    @Override @Nullable public CompoundTag getBlockEntityNbt(BlockPos blockPos) {
        return delegate.getBlockEntityNbt(blockPos);
    }

    @Override @Nullable public CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos) {
        return delegate.getBlockEntityNbtForSaving(blockPos);
    }

    @Override public Stream<BlockPos> getLights() {
        return delegate.getLights();
    }

    @Override public TickList<Block> getBlockTicks() {
        return delegate.getBlockTicks();
    }

    @Override public TickList<Fluid> getLiquidTicks() {
        return delegate.getLiquidTicks();
    }

    @Override public UpgradeData getUpgradeData() {
        return delegate.getUpgradeData();
    }

    @Override public void setInhabitedTime(long l) {
        delegate.setInhabitedTime(l);
    }

    @Override public long getInhabitedTime() {
        return delegate.getInhabitedTime();
    }

    public static ShortList getOrCreateOffsetList(ShortList[] shortLists, int i) {
        return ChunkAccess.getOrCreateOffsetList(shortLists, i);
    }

    @Override public boolean isLightCorrect() {
        return delegate.isLightCorrect();
    }

    @Override public void setLightCorrect(boolean bl) {
        delegate.setLightCorrect(bl);
    }

    @Override @Nullable public BlockEntity getBlockEntity(BlockPos blockPos) {
        return delegate.getBlockEntity(blockPos.offset(dx, 0, dz));
    }

    @Override public BlockState getBlockState(BlockPos blockPos) {
        return delegate.getBlockState(blockPos.offset(dx, 0, dz));
    }

    @Override public FluidState getFluidState(BlockPos blockPos) {
        return delegate.getFluidState(blockPos.offset(dx, 0, dz));
    }

    @Override public int getLightEmission(BlockPos blockPos) {
        return delegate.getLightEmission(blockPos.offset(dx, 0, dz));
    }

    @Override public int getMaxLightLevel() {
        return delegate.getMaxLightLevel();
    }

    @Override public Stream<BlockState> getBlockStates(AABB aABB) {
        return delegate.getBlockStates(aABB);
    }

    @Override public BlockHitResult clip(ClipContext clipContext) {
        return delegate.clip(clipContext);
    }

    @Override @Nullable public BlockHitResult clipWithInteractionOverride(Vec3 vec3, Vec3 vec32,
                                                                          BlockPos blockPos, VoxelShape voxelShape,
                                                                          BlockState blockState) {
        return delegate.clipWithInteractionOverride(vec3, vec32, blockPos, voxelShape, blockState);
    }

    @Override public double getBlockFloorHeight(VoxelShape voxelShape,
                                                Supplier<VoxelShape> supplier) {
        return delegate.getBlockFloorHeight(voxelShape, supplier);
    }

    @Override public double getBlockFloorHeight(BlockPos blockPos) {
        return delegate.getBlockFloorHeight(blockPos.offset(dx, 0, dz));
    }

    public static <T> T traverseBlocks(ClipContext clipContext,
                                       BiFunction<ClipContext, BlockPos, T> biFunction,
                                       Function<ClipContext, T> function) {
        return BlockGetter.traverseBlocks(clipContext, biFunction, function);
    }

    @Override public int getSectionsCount() {
        return delegate.getSectionsCount();
    }

    @Override public int getMinSection() {
        return delegate.getMinSection();
    }

    @Override public int getMaxSection() {
        return delegate.getMaxSection();
    }

    @Override public int getHeight() {
        return delegate.getHeight();
    }

    @Override public int getMinBuildHeight() {
        return delegate.getMinBuildHeight();
    }

    @Override public int getMaxBuildHeight() {
        return delegate.getMaxBuildHeight();
    }

    @Override public boolean isOutsideBuildHeight(BlockPos blockPos) {
        return delegate.isOutsideBuildHeight(blockPos);
    }

    @Override public boolean isOutsideBuildHeight(int i) {
        return delegate.isOutsideBuildHeight(i);
    }

    @Override public int getSectionIndex(int i) {
        return delegate.getSectionIndex(i);
    }

    @Override public int getSectionIndexFromSectionY(int i) {
        return delegate.getSectionIndexFromSectionY(i);
    }

    @Override public int getSectionYFromSectionIndex(int i) {
        return delegate.getSectionYFromSectionIndex(i);
    }

    @Override @Nullable public StructureStart<?> getStartForFeature(
            StructureFeature<?> structureFeature) {
        return delegate.getStartForFeature(structureFeature);
    }

    @Override public void setStartForFeature(StructureFeature<?> structureFeature,
                                             StructureStart<?> structureStart) {
        delegate.setStartForFeature(structureFeature, structureStart);
    }

    @Override public LongSet getReferencesForFeature(StructureFeature<?> structureFeature) {
        return delegate.getReferencesForFeature(structureFeature);
    }

    @Override public void addReferenceForFeature(StructureFeature<?> structureFeature, long l) {
        delegate.addReferenceForFeature(structureFeature, l);
    }

    @Override public Map<StructureFeature<?>, LongSet> getAllReferences() {
        return delegate.getAllReferences();
    }

    @Override public void setAllReferences(Map<StructureFeature<?>, LongSet> map) {
        delegate.setAllReferences(map);
    }
}
