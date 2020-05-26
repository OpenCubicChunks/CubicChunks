package cubicchunks.cc.chunk.section;

import cubicchunks.cc.chunk.ISection;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.ITickList;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class SectionPrimerWrapper extends SectionPrimer implements ISection, IChunk {

    WorldSection worldSection;
    public SectionPrimerWrapper(SectionPos pos)
    {
        super(pos, null);
    }

    public SectionPrimerWrapper(WorldSection chunkSection) {
        super(chunkSection.getSectionPos(), chunkSection);
    }

    @Override
    public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {
        this.addTileEntity(pos, tileEntityIn);
    }

    @Override
    public void addEntity(Entity entityIn) {
        this.addEntity(entityIn);
    }

    @Override
    public Set<BlockPos> getTileEntitiesPos() {
        return this.getTileEntitiesPos();
    }

    @Deprecated
    @Override
    public ChunkSection[] getSections() {
        throw new UnsupportedOperationException("This should never be called! Excuse me, wtF?");
    }

    @Override
    public Collection<Map.Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
        return this.getHeightmaps();
    }

    @Override
    public void setHeightmap(Heightmap.Type type, long[] data) {
        this.setHeightmap(type, data);
    }

    @Deprecated
    @Override
    public Heightmap getHeightmap(Heightmap.Type typeIn) {
        throw new UnsupportedOperationException("Not implemented yet :(");
    }

    @Deprecated
    @Override
    public int getTopBlockY(Heightmap.Type heightmapType, int x, int z) {
        throw new UnsupportedOperationException("Not implemented yet :(");
    }

    @Deprecated
    @Override
    public ChunkPos getPos() {
        throw new UnsupportedOperationException("Not implemented yet :(");
    }

    @Deprecated
    @Override
    public void setLastSaveTime(long saveTime) {
        throw new UnsupportedOperationException("Not implemented yet :(");
    }

    @Deprecated
    @Override
    public Map<String, StructureStart> getStructureStarts() {
        throw new UnsupportedOperationException("Not implemented yet :(");
    }

    @Deprecated
    @Override
    public void setStructureStarts(Map<String, StructureStart> structureStartsIn) {
        throw new UnsupportedOperationException("Not implemented yet :(");
    }

    @Nullable
    @Override
    public BiomeContainer getBiomes() {
        return null;
    }

    @Override
    public void setModified(boolean modified) {

    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public ChunkStatus getSectionStatus() {
        return null;
    }

    @Override
    public void removeTileEntity(BlockPos pos) {

    }

    @Override
    public ShortList[] getPackedPositions() {
        return new ShortList[0];
    }

    @Nullable
    @Override
    public CompoundNBT getDeferredTileEntity(BlockPos pos) {
        return null;
    }

    @Nullable
    @Override
    public CompoundNBT getTileEntityNBT(BlockPos pos) {
        return null;
    }

    @Override
    public Stream<BlockPos> getLightSources() {
        return null;
    }

    @Override
    public ITickList<Block> getBlocksToBeTicked() {
        return null;
    }

    @Override
    public ITickList<Fluid> getFluidsToBeTicked() {
        return null;
    }

    @Override
    public UpgradeData getUpgradeData() {
        return null;
    }

    @Override
    public void setInhabitedTime(long newInhabitedTime) {

    }

    @Override
    public long getInhabitedTime() {
        return 0;
    }

    @Override
    public boolean hasLight() {
        return false;
    }

    @Override
    public void setLight(boolean lightCorrectIn) {

    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return null;
    }

    @Override
    public IFluidState getFluidState(BlockPos pos) {
        return null;
    }

    @Nullable
    @Override
    public StructureStart getStructureStart(String stucture) {
        return null;
    }

    @Override
    public void putStructureStart(String structureIn, StructureStart structureStartIn) {

    }

    @Override
    public LongSet getStructureReferences(String structureIn) {
        return null;
    }

    @Override
    public void addStructureReference(String strucutre, long reference) {

    }

    @Override
    public Map<String, LongSet> getStructureReferences() {
        return null;
    }

    @Override
    public void setStructureReferences(Map<String, LongSet> p_201606_1_) {

    }
}
