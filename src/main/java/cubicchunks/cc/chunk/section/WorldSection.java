package cubicchunks.cc.chunk.section;

import cubicchunks.cc.chunk.ISection;
import cubicchunks.cc.mixin.core.common.chunk.ChunkSectionAccess;
import cubicchunks.cc.utils.Coords;
import cubicchunks.cc.chunk.biome.SectionBiomeContainer;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

//ChunkSection is the simple section, with only basic information in it.
//WorldSection is the complete section, with all entity, fluid, etc information in it.
public class WorldSection extends ChunkSection implements IChunk, ISection {
    private final SectionPos sectionPos;

    private final HashMap<BlockPos, TileEntity> tileEntities = new HashMap<>();
    private final ClassInheritanceMultiMap<Entity> entities = new ClassInheritanceMultiMap<>(Entity.class);
    private final ServerWorld world;

    private ChunkStatus sectionStatus;

    private SectionBiomeContainer sectionBiomeContainer;

    public WorldSection(ServerWorld world, ChunkSection section, SectionPos pos) {
        super(pos.getWorldStartY(), ((ChunkSectionAccess) section).getBlockRefCount(),
                ((ChunkSectionAccess) section).getBlockTickRefCount(),
                ((ChunkSectionAccess) section).getFluidRefCount());
        ((ChunkSectionAccess) section).setData(section.getData());
        this.sectionPos = pos;
        this.world = world;
    }

    public void setSectionBiomeContainer(SectionBiomeContainer biomes)
    {
        this.sectionBiomeContainer = biomes;
    }

    public Map<BlockPos, TileEntity> getTileEntityMap() {
        return tileEntities;
    }

    public ClassInheritanceMultiMap<Entity> getEntityList() {
        return entities;
    }

    public void removeEntity(Entity entity) {
        // todo: implement
    }

    @Override public ChunkStatus getSectionStatus() {
        return this.sectionStatus;
    }

    @Override
    public void setSectionStatus(ChunkStatus status)
    {
        this.sectionStatus = status;
    }

    public SectionPos getSectionPos() {
        return sectionPos;
    }


    public World getWorld() {
        return world;
    }

    // vanilla stuff


    @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return null;
    }

    @Override public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {

    }

    @Override public void addEntity(Entity entityIn) {

    }

    @Override public Set<BlockPos> getTileEntitiesPos() {
        return null;
    }

    @Override public ChunkSection[] getSections() {
        return new ChunkSection[0];
    }

    @Override public Collection<Map.Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
        return null;
    }

    @Override public void setHeightmap(Heightmap.Type type, long[] data) {

    }

    @Override public Heightmap getHeightmap(Heightmap.Type typeIn) {
        return null;
    }

    @Override public int getTopBlockY(Heightmap.Type heightmapType, int x, int z) {
        return 0;
    }

    @Override public ChunkPos getPos() {
        return null;
    }

    @Override public void setLastSaveTime(long saveTime) {

    }

    @Override public Map<String, StructureStart> getStructureStarts() {
        return null;
    }

    @Override public void setStructureStarts(Map<String, StructureStart> structureStartsIn) {

    }

    @Nullable @Override public BiomeContainer getBiomes() {
        return null;
    }

    @Override public void setModified(boolean modified) {

    }

    @Override public boolean isModified() {
        return false;
    }

    @Override public ChunkStatus getStatus() {
        return null;
    }

    @Override public void removeTileEntity(BlockPos pos) {

    }

    @Override public ShortList[] getPackedPositions() {
        return new ShortList[0];
    }

    @Nullable @Override public CompoundNBT getDeferredTileEntity(BlockPos pos) {
        return null;
    }

    @Nullable @Override public CompoundNBT getTileEntityNBT(BlockPos pos) {
        return null;
    }

    @Override public Stream<BlockPos> getLightSources() {
        return null;
    }

    @Override public ITickList<Block> getBlocksToBeTicked() {
        return null;
    }

    @Override public ITickList<Fluid> getFluidsToBeTicked() {
        return null;
    }

    @Override public UpgradeData getUpgradeData() {
        return null;
    }

    @Override public void setInhabitedTime(long newInhabitedTime) {

    }

    @Override public long getInhabitedTime() {
        return 0;
    }

    @Override public boolean hasLight() {
        return false;
    }

    @Override public void setLight(boolean lightCorrectIn) {

    }

    @Nullable @Override public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }

    @Override public BlockState getBlockState(BlockPos pos) {
        return null;
    }

    @Override public IFluidState getFluidState(BlockPos pos) {
        return null;
    }

    @Nullable @Override public StructureStart getStructureStart(String stucture) {
        return null;
    }

    @Override public void putStructureStart(String structureIn, StructureStart structureStartIn) {

    }

    @Override public LongSet getStructureReferences(String structureIn) {
        return null;
    }

    @Override public void addStructureReference(String strucutre, long reference) {

    }

    @Override public Map<String, LongSet> getStructureReferences() {
        return null;
    }

    @Override public void setStructureReferences(Map<String, LongSet> p_201606_1_) {

    }
}
