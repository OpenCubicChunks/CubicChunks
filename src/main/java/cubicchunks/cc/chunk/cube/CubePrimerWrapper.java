package cubicchunks.cc.chunk.cube;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
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
        super(cubeIn.getSectionPos(), cubeIn);
        this.cube = cubeIn;
    }


    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        return this.cube.getTileEntity(pos);
    }

    @Nullable
    public BlockState getBlockState(BlockPos pos) {
        return this.cube.getBlockState(pos);
    }

    public IFluidState getFluidState(BlockPos pos) {
        return this.cube.getFluidState(pos);
    }

    public int getMaxLightLevel() {
        return this.cube.getMaxLightLevel();
    }

    @Nullable
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return null;
    }

    public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {
    }

    /**
     * Adds an entity to the cube.
     */
    public void addEntity(Entity entityIn) {
    }

    public void setStatus(ChunkStatus status) {
    }

    /**
     * Returns the sections array for this Cube.
     */
    @Deprecated
    public ChunkSection[] getSections() {
        throw new UnsupportedOperationException("This should never be called!");
    }

    @Nullable
    public WorldLightManager getWorldLightManager() {
        throw new UnsupportedOperationException("Not implemented yet!");
        //return this.cube.getWorldLightManager();
    }

    public void setHeightmap(Heightmap.Type type, long[] data) {
    }

    private Heightmap.Type func_209532_c(Heightmap.Type p_209532_1_) {
        if (p_209532_1_ == Heightmap.Type.WORLD_SURFACE_WG) {
            return Heightmap.Type.WORLD_SURFACE;
        } else {
            return p_209532_1_ == Heightmap.Type.OCEAN_FLOOR_WG ? Heightmap.Type.OCEAN_FLOOR : p_209532_1_;
        }
    }

    public int getTopBlockY(Heightmap.Type heightmapType, int x, int z) {
        return this.cube.getTopBlockY(this.func_209532_c(heightmapType), x, z);
    }

    /**
     * Gets a {@link SectionPos } representing the x and z coordinates of this cube.
     */
    public ChunkPos getPos() {
        throw new UnsupportedOperationException("This function shoult never be called!");
    }

    public void setLastSaveTime(long saveTime) {
    }

    @Nullable
    public StructureStart getStructureStart(String stucture) {
        return this.cube.getStructureStart(stucture);
    }

    public void putStructureStart(String structureIn, StructureStart structureStartIn) {
    }

    public Map<String, StructureStart> getStructureStarts() {
        return this.cube.getStructureStarts();
    }

    public void setStructureStarts(Map<String, StructureStart> structureStartsIn) {
    }

    public LongSet getStructureReferences(String structureIn) {
        return this.cube.getStructureReferences(structureIn);
    }

    public void addStructureReference(String strucutre, long reference) {
    }

    public Map<String, LongSet> getStructureReferences() {
        return this.cube.getStructureReferences();
    }

    public void setStructureReferences(Map<String, LongSet> p_201606_1_) {
    }

    public BiomeContainer getBiomes() {
        return this.cube.getBiomes();
    }

    public void setModified(boolean modified) {
    }

    public boolean isModified() {
        return false;
    }

    public ChunkStatus getStatus() {
        return this.cube.getCubeStatus();
    }

    public void removeTileEntity(BlockPos pos) {
    }

    public void markBlockForPostprocessing(BlockPos pos) {
    }

    public void addTileEntity(CompoundNBT nbt) {
    }

    @Nullable
    public CompoundNBT getDeferredTileEntity(BlockPos pos) {
        return this.cube.getDeferredTileEntity(pos);
    }

    @Nullable
    public CompoundNBT getTileEntityNBT(BlockPos pos) {
        return this.cube.getTileEntityNBT(pos);
    }

    public Stream<BlockPos> getLightSources() {
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
    public BitSet getCarvingMask(GenerationStage.Carving type) {
        return this.cube.getCarvingMask(type);
    }

    public Cube func_217336_u() {
        return this.cube;
    }

    public boolean hasLight() {
        return this.cube.hasLight();
    }

    public void setLight(boolean lightCorrectIn) {
        this.cube.setLight(lightCorrectIn);
    }
}
