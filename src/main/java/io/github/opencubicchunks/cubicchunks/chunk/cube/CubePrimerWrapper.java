package io.github.opencubicchunks.cubicchunks.chunk.cube;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;

import java.util.BitSet;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class CubePrimerWrapper extends CubePrimer {

    private final BigCube cube;

    public CubePrimerWrapper(BigCube cubeIn) {
        super(cubeIn.getCubePos(), null, cubeIn.getCubeSections(), null, null);
        this.cube = cubeIn;
    }

    public BigCube getCube()
    {
        return this.cube;
    }

    @Deprecated @Override public ChunkPos getPos() { throw new UnsupportedOperationException("This function should never be called!"); }
    @Override public CubePos getCubePos() {
        return this.cube.getCubePos();
    }

    @Deprecated @Override public ChunkSection[] getSections() { throw new UnsupportedOperationException("This should never be called!"); }
    @Override public ChunkSection[] getCubeSections() {
        return cube.getCubeSections();
    }

    //STATUS
    @Deprecated @Override public ChunkStatus getStatus() { return this.cube.getCubeStatus(); }
    @Override public ChunkStatus getCubeStatus() {
        return cube.getCubeStatus();
    }

    //BLOCK
    @Override @Nullable public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        return null;
    }

    @Override public BlockState getBlockState(int x, int y, int z) {
        return this.cube.getBlockState(x, y, z);
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        return this.cube.getFluidState(pos);
    }

    //ENTITY
    @Deprecated @Override public void addEntity(Entity entityIn) { }
    @Override public void addCubeEntity(Entity entityIn) { }

    //TILE ENTITY
    @Deprecated @Override public void addTileEntity(CompoundNBT nbt) { }
    @Override public void addCubeTileEntity(CompoundNBT nbt) { }

    @Deprecated @Override public void removeTileEntity(BlockPos pos) { }
    @Override public void removeCubeTileEntity(BlockPos pos) { }

    @Deprecated @Override public void addTileEntity(BlockPos pos, TileEntity tileEntity) { }
    @Override public void addCubeTileEntity(BlockPos pos, TileEntity tileEntityIn) { }

    @Override @Nullable public TileEntity getTileEntity(BlockPos pos) {
        return this.cube.getTileEntity(pos);
    }

    @Deprecated @Override @Nullable public CompoundNBT getTileEntityNBT(BlockPos pos) { return this.getCubeTileEntityNBT(pos); }
    @Override @Nullable public CompoundNBT getCubeTileEntityNBT(BlockPos pos) {
        return this.cube.getCubeTileEntityNBT(pos);
    }

    @Deprecated @Override @Nullable public CompoundNBT getDeferredTileEntity(BlockPos pos) { return this.getCubeDeferredTileEntity(pos); }
    @Override @Nullable public CompoundNBT getCubeDeferredTileEntity(BlockPos pos) {
        return this.cube.getCubeDeferredTileEntity(pos);
    }

    //LIGHTING
    @Deprecated @Override public void setLight(boolean lightCorrectIn) { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public void setCubeLight(boolean lightCorrectIn)
    {
        this.cube.setCubeLight(lightCorrectIn);
    }

    @Deprecated @Override public boolean hasLight() { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public boolean hasCubeLight() {
        return this.cube.hasCubeLight();
    }

    @Deprecated @Override public Stream<BlockPos> getLightSources() { return this.getCubeLightSources(); }
    @Override public Stream<BlockPos> getCubeLightSources() {
        return this.cube.getCubeLightSources();
    }

    @Override public int getMaxLightLevel() {
        return this.cube.getMaxLightLevel();
    }

    //MISC
    @Deprecated @Override public void setModified(boolean modified) { this.setDirty(modified); }
    @Override public void setDirty(boolean modified) {
        this.cube.setDirty(modified);
    }

    @Deprecated @Override public boolean isModified() { return this.isDirty(); }
    @Override public boolean isDirty() {
        return this.cube.isDirty();
    }

    @Override public boolean isEmptyCube() {
        return this.cube.isEmptyCube();
    }

    @Deprecated @Override public BiomeContainer getBiomes() { return this.getCubeBiomes(); }
    @Override public CubeBiomeContainer getCubeBiomes() {
        return this.cube.getBiomes();
    }

    @Override public void setHeightmap(Heightmap.Type type, long[] data) {
    }

    private Heightmap.Type func_209532_c(Heightmap.Type p_209532_1_) {
        if (p_209532_1_ == Heightmap.Type.WORLD_SURFACE_WG) {
            return Heightmap.Type.WORLD_SURFACE;
        } else {
            return p_209532_1_ == Heightmap.Type.OCEAN_FLOOR_WG ? Heightmap.Type.OCEAN_FLOOR : p_209532_1_;
        }
    }

    @Override public int getTopBlockY(Heightmap.Type heightmapType, int x, int z) {
        return this.cube.getTopBlockY(this.func_209532_c(heightmapType), x, z);
    }

    @Override public void setLastSaveTime(long saveTime) {
    }

    // getStructureStart
    @Override @Nullable public StructureStart<?> func_230342_a_(Structure<?> var1) {
         return this.cube.func_230342_a_(var1);
    }

    @Override public void func_230344_a_(Structure<?> structureIn, StructureStart<?> structureStartIn) {
    }

    @Override public Map<Structure<?>, StructureStart<?>> getStructureStarts() {
        return this.cube.getStructureStarts();
    }

    @Override public void setStructureStarts(Map<Structure<?>, StructureStart<?>> structureStartsIn) {
    }

    @Override public LongSet func_230346_b_(Structure<?> structureIn) {
        return this.cube.func_230346_b_(structureIn);
    }

    @Override public void func_230343_a_(Structure<?> structure, long reference) {
    }

    @Override public Map<Structure<?>, LongSet> getStructureReferences() {
        return this.cube.getStructureReferences();
    }

    @Override public void setStructureReferences(Map<Structure<?>, LongSet> p_201606_1_) {
    }

    @Override public void markBlockForPostprocessing(BlockPos pos) {
    }

    /*
    public CubePrimerTickList<Block> getBlocksToBeTicked() {
        return new CubePrimerTickList<>((p_209219_0_) -> {
            return p_209219_0_.getDefaultState().isAir();
        }, this.getPos());
    }

    public CubePrimerTickList<Fluid> getFluidsToBeTicked() {
        return new CubePrimerTickList<>((p_209218_0_) -> {
            return p_209218_0_ == Fluids.EMPTY;
        }, this.getPos());
    }
    */

    @Override public BitSet getCarvingMask(GenerationStage.Carving type) {
        throw Util.pauseDevMode(new UnsupportedOperationException("Meaningless in this context"));
    }
    @Override public BitSet setCarvingMask(GenerationStage.Carving type) {
        throw Util.pauseDevMode(new UnsupportedOperationException("Meaningless in this context"));
    }
}
