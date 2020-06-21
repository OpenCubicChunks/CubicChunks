package io.github.opencubicchunks.cubicchunks.chunk.cube;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureStart;

import java.util.BitSet;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class CubePrimerWrapper extends CubePrimer {

    private final BigCube cube;

    public CubePrimerWrapper(BigCube cubeIn) {
        super(cubeIn.getCubePos(), cubeIn.getCubeSections());
        this.cube = cubeIn;
    }

    @Override public ChunkStatus getCubeStatus() {
        return cube.getCubeStatus();
    }

    public BigCube getCube()
    {
        return this.cube;
    }

    @Override
    public ChunkSection[] getCubeSections() {
        return cube.getCubeSections();
    }

    @Override @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        return this.cube.getTileEntity(pos);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return this.cube.getBlockState(x, y, z);
    }

    @Override public IFluidState getFluidState(BlockPos pos) {
        return this.cube.getFluidState(pos);
    }

    @Override public int getMaxLightLevel() {
        return this.cube.getMaxLightLevel();
    }

    @Override @Nullable
    public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        return null;
    }

    @Override public void addCubeTileEntity(BlockPos pos, TileEntity tileEntityIn) {
    }

    /**
     * Adds an entity to the cube.
     */
    @Override public void addEntity(Entity entityIn) {
    }

    @Override public void setStatus(ChunkStatus status) {
    }

    /**
     * Returns the sections array for this Cube.
     */
    @Override @Deprecated
    public ChunkSection[] getSections() {
        throw new UnsupportedOperationException("This should never be called!");
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

    @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException("This function should never be called!");
    }

    @Override public void setLastSaveTime(long saveTime) {
    }

    @Override @Nullable
    public StructureStart getStructureStart(String stucture) {
        return this.cube.getStructureStart(stucture);
    }

    @Override public void putStructureStart(String structureIn, StructureStart structureStartIn) {
    }

    @Override public Map<String, StructureStart> getStructureStarts() {
        return this.cube.getStructureStarts();
    }

    @Override public void setStructureStarts(Map<String, StructureStart> structureStartsIn) {
    }

    @Override public LongSet getStructureReferences(String structureIn) {
        return this.cube.getStructureReferences(structureIn);
    }

    @Override public void addStructureReference(String strucutre, long reference) {
    }

    @Override public Map<String, LongSet> getStructureReferences() {
        return this.cube.getStructureReferences();
    }

    @Override public void setStructureReferences(Map<String, LongSet> p_201606_1_) {
    }

    @Override public CubeBiomeContainer getBiomes() {
        return this.cube.getBiomes();
    }

    @Override public void setDirty(boolean modified) {
        this.cube.setDirty(modified);
    }

    @Override public boolean isDirty() {
        return this.cube.isDirty();
    }

    @Override public ChunkStatus getStatus() {
        return this.cube.getCubeStatus();
    }

    @Override public void removeCubeTileEntity(BlockPos pos) {
    }

    @Override public void markBlockForPostprocessing(BlockPos pos) {
    }

    @Override public void addTileEntity(CompoundNBT nbt) {
    }

    @Override @Nullable
    public CompoundNBT getDeferredTileEntity(BlockPos pos) {
        return this.cube.getDeferredTileEntity(pos);
    }

    @Override @Nullable
    public CompoundNBT getTileEntityNBT(BlockPos pos) {
        return this.cube.getTileEntityNBT(pos);
    }

    @Override public Stream<BlockPos> getLightSources() {
        return this.cube.getLightSources();
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

    @Override
    public boolean isEmptyCube() {
        return this.cube.isEmptyCube();
    }

    @Override public BitSet getCarvingMask(GenerationStage.Carving type) {
        return this.cube.getCarvingMask(type);
    }

    @Override public boolean hasLight() {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }
    @Override public void setLight(boolean lightCorrectIn) {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public boolean hasCubeLight() {
        return this.cube.hasCubeLight();
    }
    @Override public void setCubeLight(boolean lightCorrectIn)
    {
        this.cube.setCubeLight(lightCorrectIn);
    }

    @Override
    public Stream<BlockPos> getCubeLightSources() {
        return this.cube.getCubeLightSources();
    }
}
