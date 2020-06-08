package io.github.opencubicchunks.cubicchunks.chunk.cube;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.utils.Coords.cubeToSection;
import static net.minecraft.world.chunk.Chunk.EMPTY_SECTION;

import com.google.common.collect.Sets;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkSectionAccess;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraftforge.common.util.Constants;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

public class Cube implements IChunk, ICube {

    private final CubePos cubePos;
    private final ChunkSection[] sections = new ChunkSection[CUBE_SIZE];

    private final HashMap<BlockPos, TileEntity> tileEntities = new HashMap<>();
    private final ClassInheritanceMultiMap<Entity>[] entityLists;
    private final World world;

    private ChunkStatus cubeStatus = ChunkStatus.EMPTY;

    private CubeBiomeContainer cubeBiomeContainer;

    private boolean dirty = true; // todo: change back to false?
    private boolean loaded = false;

    private volatile boolean lightCorrect;

    public Cube(World worldIn, CubePos cubePosIn, CubeBiomeContainer biomeContainerIn)
    {
        this(worldIn, cubePosIn, null, biomeContainerIn);
    }

    public Cube(World worldIn, CubePos cubePosIn, @Nullable ChunkSection[] sectionsIn, CubeBiomeContainer biomeContainerIn)
    {
        this.cubePos = cubePosIn;
        this.world = worldIn;
        this.cubeBiomeContainer = biomeContainerIn;

        if(sectionsIn != null) {
            if (sectionsIn.length != CUBE_SIZE) {
                throw new IllegalStateException("Number of Sections must equal Cube.CUBESIZE");
            }

            for (int i = 0; i < sectionsIn.length; i++) {
                int sectionYPos = cubeToSection(cubePosIn.getY(), Coords.indexToY(i));

                if(sectionsIn[i] != null) {
                    sections[i] = new ChunkSection(sectionYPos,
                            ((ChunkSectionAccess) sectionsIn[i]).getBlockRefCount(),
                            ((ChunkSectionAccess) sectionsIn[i]).getBlockTickRefCount(),
                            ((ChunkSectionAccess) sectionsIn[i]).getFluidRefCount());
                    //noinspection ConstantConditions
                    ((ChunkSectionAccess) sections[i]).setData(sectionsIn[i].getData());
                }
            }
        }

        //noinspection unchecked
        this.entityLists = new ClassInheritanceMultiMap[ICube.CUBE_SIZE];
        for(int i = 0; i < this.entityLists.length; ++i) {
            this.entityLists[i] = new ClassInheritanceMultiMap<>(Entity.class);
        }
    }

    public boolean isEmptyCube() {
        for(ChunkSection section : this.sections) {
            if(section != EMPTY_SECTION && !section.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getSize() {
        int size = MathUtil.ceilDiv(sections.length, Byte.SIZE); // exists flags
        for(ChunkSection section : this.sections)
        {
            if(section != null)
                size += section.getSize();
        }
        return size;
    }

    public void write(PacketBuffer buf) {
        BitSet emptyFlags = new BitSet(sections.length);
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] != null && !sections[i].isEmpty()) {
                emptyFlags.set(i);
            }
        }
        byte[] emptyFlagsBytes = emptyFlags.toByteArray();
        byte[] actualFlagsBytes = new byte[MathUtil.ceilDiv(sections.length, Byte.SIZE)];
        System.arraycopy(emptyFlagsBytes, 0, actualFlagsBytes, 0, emptyFlagsBytes.length);
        buf.writeBytes(actualFlagsBytes);
        for (ChunkSection section : sections) {
            if (section != null && !section.isEmpty()) {
                section.write(buf);
            }
        }
    }

    public void read(@Nullable CubeBiomeContainer biomes, PacketBuffer readBuffer, CompoundNBT nbtTagIn, boolean cubeExists) {
        if (!cubeExists) {
            Arrays.fill(sections, null);
            return;
        }
        byte[] emptyFlagsBytes = new byte[MathUtil.ceilDiv(sections.length, Byte.SIZE)];
        readBuffer.readBytes(emptyFlagsBytes);
        BitSet emptyFlags = BitSet.valueOf(emptyFlagsBytes);

        this.cubeBiomeContainer = biomes;

        Sets.newHashSet(this.tileEntities.keySet()).forEach(this.world::removeTileEntity);
        for (int i = 0; i < ICube.CUBE_SIZE; i++) {
            boolean exists = emptyFlags.get(i);

            //        byte emptyFlags = 0;
            //        for (int i = 0; i < sections.length; i++) {
            //            if (sections[i] != null && !sections[i].isEmpty()) {
            //                emptyFlags |= 1 << i;
            //            }
            //        }
            //        buf.writeByte(emptyFlags);
            //        for (int i = 0; i < sections.length; i++) {
            //            if (sections[i] != null && !sections[i].isEmpty()) {
            //                sections[i].write(buf);
            //            }
            //        }
            //        return false;

            int dx = Coords.indexToX(i);
            int dy = Coords.indexToY(i);
            int dz = Coords.indexToZ(i);

            SectionPos sectionPos = getCubePos().asSectionPos();
            int x = sectionPos.getX() + dx;
            int y = sectionPos.getY() + dy;
            int z = sectionPos.getZ() + dz;

            readSection(i, y, null, readBuffer, nbtTagIn, exists);

            WorldLightManager lightManager = world.getLightManager();
            //lightManager.enableLightSources(new ChunkPos(x, z), true);

            ChunkSection chunksection = sections[i];
            lightManager.updateSectionStatus(sectionPos, ChunkSection.isEmpty(chunksection));

            ((ClientWorld) this.world).onChunkLoaded(x, z);
            // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
        }
    }

    private void readSection(int sectionIdx, int sectionY, @Nullable CubeBiomeContainer biomeContainerIn, PacketBuffer packetBufferIn, CompoundNBT nbtIn,
            boolean sectionExists) {

        for (TileEntity tileEntity : tileEntities.values()) {
            tileEntity.updateContainingBlockInfo();
            tileEntity.getBlockState();
        }

        ChunkSection section = this.sections[sectionIdx];
        if (section == EMPTY_SECTION) {
            section = new ChunkSection(sectionY << 4);
            this.sections[sectionIdx] = section;
        }
        if (sectionExists) {
            section.read(packetBufferIn);
        }

        if (biomeContainerIn != null) {
            this.cubeBiomeContainer = biomeContainerIn;
        }

        for (Heightmap.Type type : Heightmap.Type.values()) {
            String typeId = type.getId();
            if (nbtIn.contains(typeId, Constants.NBT.TAG_LONG_ARRAY)) {
                this.setHeightmap(type, nbtIn.getLongArray(typeId));
            }
        }

        for (TileEntity tileentity : this.tileEntities.values()) {
            tileentity.updateContainingBlockInfo();
        }
    }

    public void setCubeBiomeContainer(CubeBiomeContainer biomes) {
        this.cubeBiomeContainer = biomes;
    }

    public Map<BlockPos, TileEntity> getTileEntityMap() {
        return tileEntities;
    }

    public ClassInheritanceMultiMap<Entity>[] getEntityLists() {
        return entityLists;
    }

    private int getIndexFromEntity(Entity entityIn) {
        return (MathHelper.floor(entityIn.getPosX() / 16.0D) * ICube.CUBE_DIAMETER * ICube.CUBE_DIAMETER) +
                (MathHelper.floor(entityIn.getPosY() / 16.0D) * ICube.CUBE_DIAMETER) +
                MathHelper.floor(entityIn.getPosZ() / 16.0D);
    }

    public void removeEntity(Entity entityIn) {
        this.removeEntityAtIndex(entityIn, this.getIndexFromEntity(entityIn));
    }

    public void removeEntityAtIndex(Entity entityIn, int index) {
        if (index < 0) {
            index = 0;
        }

        if (index >= this.entityLists.length) {
            index = this.entityLists.length - 1;
        }

        this.entityLists[index].remove(entityIn);
        this.setModified(true);
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
        int xPos = Coords.indexToX(index);
        int yPos = Coords.indexToY(index);
        int zPos = Coords.indexToZ(index);

        SectionPos sectionPos = this.cubePos.asSectionPos();
        return SectionPos.of(xPos + sectionPos.getX(), yPos + sectionPos.getY(), zPos + sectionPos.getZ());
    }


    public World getWorld() {
        return world;
    }

    @Nullable
    public BlockState setBlockState(int sectionIndex, BlockPos pos, BlockState state, boolean isMoving)
    {
        int i = pos.getX() & 15;
        int j = pos.getY() & 15;
        int k = pos.getZ() & 15;
        ChunkSection chunksection = sections[sectionIndex];

        BlockState blockstate = chunksection.setBlockState(i, j, k, state);
        if (blockstate == state) {
            return null;
        }
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

        if (chunksection.getBlockState(i, j, k).getBlock() != block) {
            return null;
        }
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

    // TODO: obfuscation, this overrides both IChunk and ICube
    @Nullable
    public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        return this.setBlockState(Coords.blockToIndex(pos.getX(), pos.getY(), pos.getZ()), pos, state, isMoving);
    }

    @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return setBlock(pos, state, isMoving);
    }

    @Override public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {
        addCubeTileEntity(pos, tileEntityIn);
    }

    @Override public void addCubeTileEntity(BlockPos pos, TileEntity tileEntityIn) {
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
        //This needs to be blockToCube instead of getCubeXForEntity because of the `/ 16`
        int xFloor = blockToCube(MathHelper.floor(entityIn.getPosX() / 16.0D));
        int yFloor = blockToCube(MathHelper.floor(entityIn.getPosY() / 16.0D));
        int zFloor = blockToCube(MathHelper.floor(entityIn.getPosZ() / 16.0D));
        if (xFloor != this.cubePos.getX() || yFloor != this.cubePos.getY() || zFloor != this.cubePos.getZ()) {
            CubicChunks.LOGGER.warn("Wrong location! ({}, {}, {}) should be ({}, {}, {}), {}", xFloor, yFloor, zFloor, this.cubePos.getX(),
                    this.cubePos.getY(), this.cubePos.getZ(), entityIn);
            entityIn.removed = true;
        }

        int idx = this.getIndexFromEntity(entityIn);

        //TODO: reimplement forge EntityEvent#EnteringChunk
        //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityEvent.EnteringChunk(entityIn, this.pos.x, this.pos.z, entityIn.chunkCoordX, entityIn.chunkCoordZ));
        entityIn.addedToChunk = true;
        entityIn.chunkCoordX = cubeToSection(this.cubePos.getX(), 0);
        entityIn.chunkCoordY = cubeToSection(this.cubePos.getY(), 0);
        entityIn.chunkCoordZ = cubeToSection(this.cubePos.getZ(), 0);
        this.entityLists[idx].add(entityIn);
        this.setModified(true); // Forge - ensure chunks are marked to save after an entity add
    }

    @Nullable @Override public TileEntity getTileEntity(BlockPos pos) {
        return getTileEntity(pos, Chunk.CreateEntityType.CHECK);
    }

    @Nullable
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
        return !blockstate.hasTileEntity() ? null : blockstate.createTileEntity(this.world);
    }

    @Override public Set<BlockPos> getTileEntitiesPos() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Deprecated
    @Override public ChunkSection[] getSections() {
        return this.sections;
    }

    @Override public ChunkSection[] getCubeSections() {
        return this.sections;
    }

    @Override public Collection<Map.Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setHeightmap(Heightmap.Type type, long[] data) {

    }

    @Override public Heightmap getHeightmap(Heightmap.Type typeIn) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public int getTopBlockY(Heightmap.Type heightmapType, int x, int z) {
        return 0;
    }

    @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setLastSaveTime(long saveTime) {

    }

    @Override public Map<String, StructureStart> getStructureStarts() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setStructureStarts(Map<String, StructureStart> structureStartsIn) {

    }

    @Nullable @Override public CubeBiomeContainer getBiomes() {
        return this.cubeBiomeContainer;
    }

    @Override public void setModified(boolean modified) {
        setDirty(modified);
    }

    @Override public boolean isModified() {
        return isDirty();
    }

    @Override public void setDirty(boolean modified) {
        this.dirty = modified;
    }

    @Override public boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean hasCubeLight() {
        return this.lightCorrect;
    }

    @Override
    public void setCubeLight(boolean lightCorrectIn) {
        this.lightCorrect = lightCorrectIn;
        this.setModified(true);
    }

    @Deprecated
    @Override public ChunkStatus getStatus() {
        return this.cubeStatus;
    }

    @Override public void removeCubeTileEntity(BlockPos pos) {
        if (this.loaded || this.world.isRemote()) {
            TileEntity tileentity = this.tileEntities.remove(pos);
            if (tileentity != null) {
                tileentity.remove();
            }
        }
    }

    @Override public void removeTileEntity(BlockPos pos) {
        removeCubeTileEntity(pos);
    }

    @Override public ShortList[] getPackedPositions() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable @Override public CompoundNBT getDeferredTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable @Override public CompoundNBT getTileEntityNBT(BlockPos pos) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public Stream<BlockPos> getLightSources() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public ITickList<Block> getBlocksToBeTicked() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public ITickList<Fluid> getFluidsToBeTicked() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public UpgradeData getUpgradeData() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setInhabitedTime(long newInhabitedTime) {

    }

    @Override public long getInhabitedTime() {
        return 0;
    }

    @Override public boolean hasLight() {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public void setLight(boolean lightCorrectIn) {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public BlockState getBlockState(int x, int y, int z) {
        int index = Coords.blockToIndex(x, y, z);
        return ChunkSection.isEmpty(this.sections[index]) ?
                Blocks.AIR.getDefaultState() :
                this.sections[index].getBlockState(x & 15, y & 15, z & 15);
    }

    @Override public IFluidState getFluidState(BlockPos pos) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable @Override public StructureStart getStructureStart(String stucture) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void putStructureStart(String structureIn, StructureStart structureStartIn) {

    }

    @Override public LongSet getStructureReferences(String structureIn) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void addStructureReference(String strucutre, long reference) {

    }

    @Override public Map<String, LongSet> getStructureReferences() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setStructureReferences(Map<String, LongSet> p_201606_1_) {

    }

    @Override
    public Stream<BlockPos> getCubeLightSources() {
        return StreamSupport
                .stream(BlockPos.getAllInBoxMutable(this.cubePos.minCubeX(), this.cubePos.minCubeY(), this.cubePos.minCubeZ(),
                        this.cubePos.maxCubeX(), this.cubePos.maxCubeY(), this.cubePos.maxCubeZ())
                        .spliterator(), false).filter((blockPos) -> this.getBlockState(blockPos).getLightValue(getWorld(), blockPos) != 0);
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
    public boolean getLoaded()
    {
        return this.loaded;
    }
}
