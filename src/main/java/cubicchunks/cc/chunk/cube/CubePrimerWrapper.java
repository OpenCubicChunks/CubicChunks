package cubicchunks.cc.chunk.cube;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.lighting.WorldLightManager;

import java.util.BitSet;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public class CubePrimerWrapper extends CubePrimer {

    Cube cube;

    public CubePrimerWrapper(Cube cubeIn) {
        super(cubeIn.getCubePos(), cubeIn.getCubeSections());
        this.cube = cubeIn;
    }

    public Cube getCube()
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

    @Override @Nullable
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
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return null;
    }

    @Override public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {
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

    @Nullable
    public WorldLightManager getWorldLightManager() {
        throw new UnsupportedOperationException("Not implemented yet!");
        //return this.cube.getWorldLightManager();
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

    /**
     * Gets a {@link SectionPos } representing the x and z coordinates of this cube.
     */
    @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException("This function shoult never be called!");
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

    @Override public BiomeContainer getBiomes() {
        return this.cube.getBiomes();
    }

    @Override public void setModified(boolean modified) {
        this.cube.setModified(modified);
    }

    @Override public boolean isModified() {
        return this.cube.isModified();
    }

    @Override public ChunkStatus getStatus() {
        return this.cube.getCubeStatus();
    }

    @Override public void removeTileEntity(BlockPos pos) {
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
    @Override public BitSet getCarvingMask(GenerationStage.Carving type) {
        return this.cube.getCarvingMask(type);
    }

    public Cube func_217336_u() {
        return this.cube;
    }

    @Override public boolean hasLight() {
        return this.cube.hasLight();
    }

    @Override public void setLight(boolean lightCorrectIn) {
        this.cube.setLight(lightCorrectIn);
    }
}
