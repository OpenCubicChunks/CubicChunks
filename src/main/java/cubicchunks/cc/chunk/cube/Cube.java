package cubicchunks.cc.chunk.cube;

import static cubicchunks.cc.utils.Coords.indexTo32X;
import static cubicchunks.cc.utils.Coords.indexTo32Y;
import static cubicchunks.cc.utils.Coords.indexTo32Z;

import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.biome.CubeBiomeContainer;
import cubicchunks.cc.chunk.util.CubePos;
import cubicchunks.cc.mixin.core.common.chunk.ChunkSectionAccess;
import cubicchunks.cc.utils.Coords;
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
import net.minecraft.world.chunk.Chunk;
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
public class Cube implements IChunk, ICube {
    public static int CUBEDIAMETER = 2;
    public static int CUBESIZE = CUBEDIAMETER * CUBEDIAMETER * CUBEDIAMETER;

    private final CubePos cubePos;
    private ChunkSection[] sections = new ChunkSection[CUBESIZE];

    private final HashMap<BlockPos, TileEntity> tileEntities = new HashMap<>();
    private final ClassInheritanceMultiMap<Entity> entities = new ClassInheritanceMultiMap<>(Entity.class);
    private final World world;

    private ChunkStatus cubeStatus;

    private CubeBiomeContainer cubeBiomeContainer;

    private boolean dirty = false;
    private boolean loaded = false;

    public Cube(World worldIn, CubePos cubePosIn, CubeBiomeContainer biomeContainerIn)
    {
        this(worldIn, cubePosIn, null, biomeContainerIn);
    }

    public Cube(World worldIn, CubePos cubePosIn, ChunkSection[] sectionsIn, CubeBiomeContainer biomeContainerIn)
    {
        this.cubePos = cubePosIn;
        this.world = worldIn;
        this.cubeBiomeContainer = biomeContainerIn;

        if(sectionsIn != null) {
            if (sectionsIn.length != CUBESIZE) {
                throw new IllegalStateException("Number of Sections must equal Cube.CUBESIZE");
            }

            for (int i = 0; i < sectionsIn.length; i++) {
                int sectionYPos = (indexTo32Y(i) * 16) + cubePosIn.getX();
                sections[i] = new ChunkSection(sectionYPos,
                        ((ChunkSectionAccess) sectionsIn[i]).getBlockRefCount(),
                        ((ChunkSectionAccess) sectionsIn[i]).getBlockTickRefCount(),
                        ((ChunkSectionAccess) sectionsIn[i]).getFluidRefCount());
                ((ChunkSectionAccess) sections[i]).setData(sectionsIn[i].getData());
            }
        }
    }

    public int getSize()
    {
        int size = 0;
        for(ChunkSection section : this.sections)
        {
            size += section.getSize();
        }
        return size;
    }

    public void setCubeBiomeContainer(CubeBiomeContainer biomes)
    {
        this.cubeBiomeContainer = biomes;
    }

    public Map<BlockPos, TileEntity> getTileEntityMap() {
        return tileEntities;
    }

    public ClassInheritanceMultiMap<Entity> getEntityList() {
        return entities;
    }

    public void removeEntity(Entity entityIn) {
        this.removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
    }

    public void removeEntityAtIndex(Entity entityIn, int index) {
        /*
        if (index < 0) {
            index = 0;
        }

        if (index >= this.entityLists.length) {
            index = this.entityLists.length - 1;
        }

        this.entityLists[index].remove(entityIn);
        this.markDirty(); // Forge - ensure chunks are marked to save after entity removals
        */
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public ChunkStatus getCubeStatus() {
        return this.cubeStatus;
    }

    @Override
    public void setCubeStatus(ChunkStatus status)
    {
        this.cubeStatus = status;
    }

    public CubePos getCubePos()
    {
        return this.cubePos;
    }

    @Deprecated
    public SectionPos getSectionPosition(int index)
    {
        int xPos = (indexTo32X(index) * 16) + this.cubePos.getX();
        int yPos = (indexTo32Y(index) * 16) + this.cubePos.getY();
        int zPos = (indexTo32Z(index) * 16) + this.cubePos.getY();

        return SectionPos.of(xPos, yPos, zPos);
    }
    @Deprecated
    @Override public SectionPos[] getSectionPositions() {
        SectionPos[] positions = new SectionPos[Cube.CUBESIZE];
        for(int i = 0; i < Cube.CUBESIZE; i++)
        {
            positions[i] = this.getSectionPosition(i);
        }
        return positions;
    }


    public World getWorld() {
        return world;
    }

    public BlockState setBlockState(int sectionIndex, BlockPos pos, BlockState state, boolean isMoving)
    {
        int i = pos.getX() & 15;
        int j = pos.getY() & 15;
        int k = pos.getZ() & 15;
        ChunkSection chunksection = sections[sectionIndex];

        BlockState blockstate = chunksection.setBlockState(i, j, k, state);
        if (blockstate == state) {
            return null;
        } else {
            Block block = state.getBlock();
            Block block1 = blockstate.getBlock();
            //            this.heightMap.get(Heightmap.Type.MOTION_BLOCKING).update(i, j, k, state);
            //            this.heightMap.get(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES).update(i, j, k, state);
            //            this.heightMap.get(Heightmap.Type.OCEAN_FLOOR).update(i, j, k, state);
            //            this.heightMap.get(Heightmap.Type.WORLD_SURFACE).update(i, j, k, state);

            if (!this.world.isRemote) {
                blockstate.onReplaced(this.world, pos, state, isMoving);
            } else if ((block1 != block || !state.hasTileEntity()) && blockstate.hasTileEntity()) {
                this.world.removeTileEntity(pos);
            }

            if (chunksection.getBlockState(i, j & 15, k).getBlock() != block) {
                return null;
            } else {
                if (blockstate.hasTileEntity()) {
                    TileEntity tileentity = this.getTileEntity(pos, Chunk.CreateEntityType.CHECK);
                    if (tileentity != null) {
                        tileentity.updateContainingBlockInfo();
                    }
                }

                if (!this.world.isRemote) {
                    state.onBlockAdded(this.world, pos, blockstate, isMoving);
                }

                if (state.hasTileEntity()) {
                    TileEntity tileentity1 = this.getTileEntity(pos, Chunk.CreateEntityType.CHECK);
                    if (tileentity1 == null) {
                        tileentity1 = state.createTileEntity(this.world);
                        this.world.setTileEntity(pos, tileentity1);
                    } else {
                        tileentity1.updateContainingBlockInfo();
                    }
                }

                this.dirty = true;
                return blockstate;
            }
        }
    }
    @Nullable
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return this.setBlockState(Coords.blockToIndex32(pos.getX(), pos.getY(), pos.getZ()), pos, state, isMoving);
    }

    @Override public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {
        if (this.getBlockState(pos).hasTileEntity()) {
            tileEntityIn.setWorldAndPos(this.world, pos);
            tileEntityIn.validate();
            TileEntity tileentity = this.tileEntities.put(pos.toImmutable(), tileEntityIn);
            if (tileentity != null && tileentity != tileEntityIn) {
                tileentity.remove();
            }
        }
    }

    @Override public void addEntity(Entity entityIn) {

    }

    public TileEntity getTileEntity(BlockPos pos, Chunk.CreateEntityType creationMode) {
        TileEntity tileentity = this.tileEntities.get(pos);
        if (tileentity != null && tileentity.isRemoved()) {
            tileEntities.remove(pos);
            tileentity = null;
        }
        //TODO: reimplement for NBT stuff
//        if (tileentity == null) {
//            CompoundNBT compoundnbt = this.deferredTileEntities.remove(pos);
//            if (compoundnbt != null) {
//                TileEntity tileentity1 = this.setDeferredTileEntity(pos, compoundnbt);
//                if (tileentity1 != null) {
//                    return tileentity1;
//                }
//            }
//        }

        if (tileentity == null) {
            if (creationMode == Chunk.CreateEntityType.IMMEDIATE) {
                tileentity = this.createNewTileEntity(pos);
                this.world.setTileEntity(pos, tileentity);
            }
        }

        return tileentity;
    }

    @Nullable
    private TileEntity createNewTileEntity(BlockPos pos) {
        BlockState blockstate = this.getBlockState(pos);
        Block block = blockstate.getBlock();
        return !blockstate.hasTileEntity() ? null : blockstate.createTileEntity(this.world);
    }

    @Override public Set<BlockPos> getTileEntitiesPos() {
        return null;
    }

    @Override public ChunkSection[] getSections() {
        return this.sections;
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

    @Deprecated
    @Override public ChunkStatus getStatus() {
        return this.cubeStatus;
    }

    @Override public void removeTileEntity(BlockPos pos) {
        if (this.loaded || this.world.isRemote()) {
            TileEntity tileentity = this.tileEntities.remove(pos);
            if (tileentity != null) {
                tileentity.remove();
            }
        }
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

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
