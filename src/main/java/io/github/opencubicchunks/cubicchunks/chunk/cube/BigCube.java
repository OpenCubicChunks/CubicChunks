package io.github.opencubicchunks.cubicchunks.chunk.cube;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.EmptyTickList;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.utils.Coords.cubeToSection;
import static net.minecraft.world.chunk.Chunk.EMPTY_SECTION;

public class BigCube implements IChunk, IBigCube {

    private final CubePos cubePos;
    private final ChunkSection[] sections = new ChunkSection[SECTION_COUNT];

    private final HashMap<BlockPos, TileEntity> tileEntities = new HashMap<>();
    private final ClassInheritanceMultiMap<Entity>[] entityLists;
    private final World world;

    private CubeBiomeContainer cubeBiomeContainer;

    private boolean dirty = true; // todo: change back to false?
    private boolean loaded = false;

    private boolean hasEntities;

    private long lastSaveTime;

    private volatile boolean lightCorrect;
    private final Map<BlockPos, CompoundNBT> deferredTileEntities = Maps.newHashMap();

    private long inhabitedTime;
    @Nullable
    private Consumer<BigCube> postLoadConsumer;

    public BigCube(World worldIn, CubePos cubePosIn, CubeBiomeContainer biomeContainerIn) {
        this(worldIn, cubePosIn, biomeContainerIn, UpgradeData.EMPTY, EmptyTickList.empty(), EmptyTickList.empty(), 0L, null, null);
    }

    public BigCube(World worldIn, CubePos cubePosIn, CubeBiomeContainer biomeContainerIn, UpgradeData upgradeDataIn, ITickList<Block> tickBlocksIn,
                   ITickList<Fluid> tickFluidsIn, long inhabitedTimeIn, @Nullable ChunkSection[] sectionsIn, @Nullable Consumer<BigCube> postLoadConsumerIn) {
        this.world = worldIn;
        this.cubePos = cubePosIn;
//        this.upgradeData = upgradeDataIn;
//
//        for(Heightmap.Type heightmap$type : Heightmap.Type.values()) {
//            if (ChunkStatus.FULL.getHeightMaps().contains(heightmap$type)) {
//                this.heightMap.put(heightmap$type, new Heightmap(this, heightmap$type));
//            }
//        }

        //noinspection unchecked
        this.entityLists = new ClassInheritanceMultiMap[IBigCube.SECTION_COUNT];
        for(int i = 0; i < this.entityLists.length; ++i) {
            this.entityLists[i] = new ClassInheritanceMultiMap<>(Entity.class);
        }

//        this.blockBiomeArray = biomeContainerIn;
//        this.blocksToBeTicked = tickBlocksIn;
//        this.fluidsToBeTicked = tickFluidsIn;
        this.inhabitedTime = inhabitedTimeIn;
        this.postLoadConsumer = postLoadConsumerIn;

        if(sectionsIn != null) {
            if (sectionsIn.length != SECTION_COUNT) {
                throw new IllegalStateException("Number of Sections must equal BigCube.CUBESIZE");
            }

            for (int i = 0; i < sectionsIn.length; i++) {
                int sectionYPos = cubeToSection(cubePosIn.getY(), Coords.indexToY(i));

                if(sectionsIn[i] != null) {
                    sections[i] = new ChunkSection(sectionYPos,
                            ((ChunkSectionAccess) sectionsIn[i]).getNonEmptyBlockCount(),
                            ((ChunkSectionAccess) sectionsIn[i]).getTickingBlockCount(),
                            ((ChunkSectionAccess) sectionsIn[i]).getTickingFluidCount());
                    //noinspection ConstantConditions
                    ((ChunkSectionAccess) sections[i]).setStates(sectionsIn[i].getStates());
                }
            }
        }

//        this.gatherCapabilities();
    }

    public BigCube(World worldIn, CubePrimer cubePrimerIn) {
        //TODO: reimplement full BigCube constructor from CubePrimer
//        this(worldIn, cubePrimerIn.getCubePos(), cubePrimerIn.getCubeBiomes(), cubePrimerIn.getUpgradeData(), cubePrimerIn.getBlocksToBeTicked(),
//            cubePrimerIn.getFluidsToBeTicked(), cubePrimerIn.getInhabitedTime(), cubePrimerIn.getSections(), (Consumer<BigCube>)null);
        this(worldIn, cubePrimerIn.getCubePos(), cubePrimerIn.getCubeBiomes(), null, null,
                null, cubePrimerIn.getInhabitedTime(), cubePrimerIn.getCubeSections(), null);

        for(CompoundNBT compoundnbt : cubePrimerIn.getCubeEntities()) {
            EntityType.loadEntityRecursive(compoundnbt, worldIn, (p_217325_1_) -> {
                this.addEntity(p_217325_1_);
                return p_217325_1_;
            });
        }

        for(TileEntity tileentity : cubePrimerIn.getCubeTileEntities().values()) {
            this.addCubeTileEntity(tileentity);
        }

        this.deferredTileEntities.putAll(cubePrimerIn.getDeferredTileEntities());

        //TODO: reimplement missing BigCube methods
//        for(int i = 0; i < cubePrimerIn.getPackedPositions().length; ++i) {
//            this.packedBlockPositions[i] = cubePrimerIn.getPackedPositions()[i];
//        }

        //this.setStructureStarts(cubePrimerIn.getStructureStarts());
        //this.setStructureReferences(cubePrimerIn.getStructureReferences());

//        for(Map.Entry<Heightmap.Type, Heightmap> entry : cubePrimerIn.getHeightmaps()) {
//            if (ChunkStatus.FULL.getHeightMaps().contains(entry.getKey())) {
//                this.getHeightmap(entry.getKey()).setDataArray(entry.getValue().getDataArray());
//            }
//        }

        this.setCubeLight(cubePrimerIn.hasCubeLight());
        this.dirty = true;
    }

    @Deprecated @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException("Not implemented");
    }
    @Override public CubePos getCubePos()
    {
        return this.cubePos;
    }

    @Deprecated @Override public ChunkSection[] getSections() {
        return this.sections;
    }
    @Override public ChunkSection[] getCubeSections() {
        return this.sections;
    }

    //STATUS
    @Override public void setCubeStatus(ChunkStatus status) { throw new UnsupportedOperationException("BigCube does not have a setter for setCubeStatus"); }
    @Deprecated @Override public ChunkStatus getStatus() { return this.getCubeStatus(); }
    @Override public ChunkStatus getCubeStatus() {
        return ChunkStatus.FULL;
    }

    //BLOCK
    @Deprecated @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return setBlock(pos, state, isMoving);
    }
    @Override @Nullable public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        return this.setBlock(Coords.blockToIndex(pos.getX(), pos.getY(), pos.getZ()), pos, state, isMoving);
    }
    @Nullable public BlockState setBlock(int sectionIndex, BlockPos pos, BlockState state, boolean isMoving) {
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

        if (!this.world.isClientSide) {
            blockstate.onRemove(this.world, pos, state, isMoving);
        } else if ((block1 != block || !state.hasTileEntity()) && blockstate.hasTileEntity()) {
            this.world.removeBlockEntity(pos);
        }

        if (chunksection.getBlockState(i, j, k).getBlock() != block) {
            return null;
        }
        if (blockstate.hasTileEntity()) {
            TileEntity tileentity = this.getTileEntity(pos, Chunk.CreateEntityType.CHECK);
            if (tileentity != null) {
                tileentity.clearCache();
            }
        }

        if (!this.world.isClientSide) {
            state.onPlace(this.world, pos, blockstate, isMoving);
        }

        if (state.hasTileEntity()) {
            TileEntity tileentity1 = this.getTileEntity(pos, Chunk.CreateEntityType.CHECK);
            if (tileentity1 == null) {
                tileentity1 = state.createTileEntity(this.world);
                this.world.setBlockEntity(pos, tileentity1);
            } else {
                tileentity1.clearCache();
            }
        }

        this.dirty = true;
        return blockstate;
    }
    @Override public BlockState getBlockState(int x, int y, int z) {
        int index = Coords.blockToIndex(x, y, z);
        return ChunkSection.isEmpty(this.sections[index]) ?
                Blocks.AIR.defaultBlockState() :
                this.sections[index].getBlockState(x & 15, y & 15, z & 15);
    }

    //ENTITY
    @Deprecated @Override public void addEntity(Entity entityIn) { this.addCubeEntity(entityIn); }
    @Override public void addCubeEntity(Entity entityIn) {
        this.hasEntities = true;
        //This needs to be blockToCube instead of getCubeXForEntity because of the `/ 16`

        int xFloor = Coords.getCubeXForEntity(entityIn);
        int yFloor = Coords.getCubeYForEntity(entityIn);
        int zFloor = Coords.getCubeZForEntity(entityIn);
        if (xFloor != this.cubePos.getX() || yFloor != this.cubePos.getY() || zFloor != this.cubePos.getZ()) {
            CubicChunks.LOGGER.warn("Wrong location! ({}, {}, {}) should be ({}, {}, {}), {}", xFloor, yFloor, zFloor, this.cubePos.getX(),
                    this.cubePos.getY(), this.cubePos.getZ(), entityIn);
            //noinspection deprecation
            entityIn.removed = true;
        }

        int idx = this.getIndexFromEntity(entityIn);

        //TODO: reimplement forge EntityEvent#EnteringChunk
        //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityEvent.EnteringChunk(entityIn, this.pos.x, this.pos.z, entityIn.chunkCoordX, entityIn.chunkCoordZ));
        entityIn.inChunk = true;
        entityIn.xChunk = cubeToSection(this.cubePos.getX(), 0);
        entityIn.yChunk = cubeToSection(this.cubePos.getY(), 0);
        entityIn.zChunk = cubeToSection(this.cubePos.getZ(), 0);
        this.entityLists[idx].add(entityIn);
        this.setDirty(true);
    }

    public ClassInheritanceMultiMap<Entity>[] getCubeEntityLists() {
        return entityLists;
    }
    public ClassInheritanceMultiMap<Entity>[] getEntityLists() {
        return this.getCubeEntityLists();
    }

    private int getIndexFromEntity(Entity entityIn) {
        return Coords.blockToIndex((int)entityIn.getX(), (int)entityIn.getY(), (int)entityIn.getZ());
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
        this.setDirty(true);
    }

    public void setHasEntities(boolean hasEntitiesIn) {
        this.hasEntities = hasEntitiesIn;
    }

    //TILEENTITY
    @Deprecated @Override public void setBlockEntityNbt(CompoundNBT nbt) {
        this.addCubeTileEntity(nbt);
    }
    @Override public void addCubeTileEntity(CompoundNBT nbt) {
        this.deferredTileEntities.put(new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt);
    }

    @Deprecated @Override public void setBlockEntity(BlockPos pos, TileEntity tileEntityIn) { this.addCubeTileEntity(pos, tileEntityIn); }
    @Override public void addCubeTileEntity(BlockPos pos, TileEntity tileEntityIn) {
        if (this.getBlockState(pos).hasTileEntity()) {
            tileEntityIn.setLevelAndPosition(this.world, pos);
            tileEntityIn.clearRemoved();
            TileEntity tileentity = this.tileEntities.put(pos.immutable(), tileEntityIn);
            if (tileentity != null && tileentity != tileEntityIn) {
                tileentity.setRemoved();
            }
        }
    }

    public void addCubeTileEntity(TileEntity tileEntityIn) {
        this.setBlockEntity(tileEntityIn.getBlockPos(), tileEntityIn);
        if (this.loaded || this.world.isClientSide()) {
            this.world.setBlockEntity(tileEntityIn.getBlockPos(), tileEntityIn);
        }
    }

    @Deprecated @Override public void removeBlockEntity(BlockPos pos) { this.removeCubeTileEntity(pos); }
    @Override public void removeCubeTileEntity(BlockPos pos) {
        if (this.loaded || this.world.isClientSide()) {
            TileEntity tileentity = this.tileEntities.remove(pos);
            if (tileentity != null) {
                tileentity.setRemoved();
            }
        }
    }

    @Nullable @Override public TileEntity getBlockEntity(BlockPos pos) {
        return getTileEntity(pos, Chunk.CreateEntityType.CHECK);
    }

    @Nullable public TileEntity getTileEntity(BlockPos pos, Chunk.CreateEntityType creationMode) {
        TileEntity tileentity = this.tileEntities.get(pos);
        if (tileentity != null && tileentity.isRemoved()) {
            tileEntities.remove(pos);
            tileentity = null;
        }
        if (tileentity == null) {
            CompoundNBT compoundnbt = this.deferredTileEntities.remove(pos);
            if (compoundnbt != null) {
                TileEntity tileentity1 = this.setDeferredTileEntity(pos, compoundnbt);
                if (tileentity1 != null) {
                    return tileentity1;
                }
            }
        }

        if (tileentity == null) {
            if (creationMode == Chunk.CreateEntityType.IMMEDIATE) {
                tileentity = this.createNewTileEntity(pos);
                this.world.setBlockEntity(pos, tileentity);
            }
        }

        return tileentity;
    }

    @Nullable private TileEntity createNewTileEntity(BlockPos pos) {
        BlockState blockstate = this.getBlockState(pos);
        return !blockstate.hasTileEntity() ? null : blockstate.createTileEntity(this.world);
    }

    @Nullable private TileEntity setDeferredTileEntity(BlockPos pos, CompoundNBT compound) {
        TileEntity tileentity;
        BlockState state = this.getBlockState(pos);
        if ("DUMMY".equals(compound.getString("id"))) {
            if (state.hasTileEntity()) {
                tileentity = state.createTileEntity(this.world);
            } else {
                tileentity = null;
                CubicChunks.LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, this.getBlockState(pos));
            }
        } else {
            tileentity = TileEntity.loadStatic(state, compound);
        }

        if (tileentity != null) {
            tileentity.setLevelAndPosition(this.world, pos);
            this.addCubeTileEntity(tileentity);
        } else {
            CubicChunks.LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", state, pos);
        }

        return tileentity;
    }

    public Map<BlockPos, TileEntity> getTileEntityMap() {
        return tileEntities;
    }
    public Map<BlockPos, CompoundNBT> getDeferredTileEntityMap() {
        return this.deferredTileEntities;
    }

    @Deprecated @Override public Set<BlockPos> getBlockEntitiesPos() {
        return this.getCubeTileEntitiesPos();
    }
    @Override public Set<BlockPos> getCubeTileEntitiesPos() {
        Set<BlockPos> set = Sets.newHashSet(this.deferredTileEntities.keySet());
        set.addAll(this.tileEntities.keySet());
        return set;
    }

    @Deprecated @Nullable @Override public CompoundNBT getBlockEntityNbtForSaving(BlockPos pos) { return this.getCubeTileEntityNBT(pos); }
    @Nullable @Override public CompoundNBT getCubeTileEntityNBT(BlockPos pos) {
        TileEntity tileentity = this.getBlockEntity(pos);
        if (tileentity != null && !tileentity.isRemoved()) {
            try {
                CompoundNBT compoundnbt1 = tileentity.save(new CompoundNBT());
                compoundnbt1.putBoolean("keepPacked", false);
                return compoundnbt1;
            } catch (Exception e) {
                LogManager.getLogger().error("A TileEntity type {} has thrown an exception trying to write state. It will not persist, Report this to the mod author", tileentity.getClass().getName(), e);
                return null;
            }
        } else {
            CompoundNBT compoundnbt = this.deferredTileEntities.get(pos);
            if (compoundnbt != null) {
                compoundnbt = compoundnbt.copy();
                compoundnbt.putBoolean("keepPacked", true);
            }

            return compoundnbt;
        }
    }

    @Deprecated @Nullable @Override public CompoundNBT getBlockEntityNbt(BlockPos pos) { return this.getCubeDeferredTileEntity(pos); }
    @Nullable @Override public CompoundNBT getCubeDeferredTileEntity(BlockPos pos) {
        return this.deferredTileEntities.get(pos);
    }

    //LIGHTING
    @Deprecated @Override public boolean isLightCorrect() { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public boolean hasCubeLight() {
        return this.lightCorrect;
    }

    @Deprecated @Override public void setLightCorrect(boolean lightCorrectIn) { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public void setCubeLight(boolean lightCorrectIn) {
        this.lightCorrect = lightCorrectIn;
        this.setDirty(true);
    }

    @Deprecated @Override public Stream<BlockPos> getLights() { return this.getCubeLightSources(); }
    @Override public Stream<BlockPos> getCubeLightSources() {
        return StreamSupport
                .stream(BlockPos.betweenClosed(this.cubePos.minCubeX(), this.cubePos.minCubeY(), this.cubePos.minCubeZ(),
                        this.cubePos.maxCubeX(), this.cubePos.maxCubeY(), this.cubePos.maxCubeZ())
                        .spliterator(), false).filter((blockPos) -> this.getBlockState(blockPos).getLightValue(getWorld(), blockPos) != 0);
    }

    //MISC
    @Deprecated @Override public void setUnsaved(boolean modified) { setDirty(modified); }
    @Override public void setDirty(boolean modified) {
        this.dirty = modified;
    }

    @Deprecated @Override public boolean isUnsaved() { return isDirty(); }
    @Override public boolean isDirty() {
        return this.dirty || this.hasEntities && this.world.getGameTime() != this.lastSaveTime;
    }

    @Override public boolean isEmptyCube() {
        for(ChunkSection section : this.sections) {
            if(section != EMPTY_SECTION && !section.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Deprecated @Override public long getInhabitedTime() { return this.getCubeInhabitedTime(); }
    @Override public long getCubeInhabitedTime() {
        return this.inhabitedTime;
    }

    @Deprecated @Override public void setInhabitedTime(long newInhabitedTime) { this.setCubeInhabitedTime(newInhabitedTime); }
    @Override public void setCubeInhabitedTime(long newInhabitedTime) {
        this.inhabitedTime = newInhabitedTime;
    }

    @Deprecated @Nullable @Override public CubeBiomeContainer getBiomes() { return this.getCubeBiomes(); }
    @Nullable @Override public CubeBiomeContainer getCubeBiomes() {
        return this.cubeBiomeContainer;
    }

    public int getSize() {
        int size = MathUtil.ceilDiv(sections.length, Byte.SIZE); // exists flags
        for(ChunkSection section : this.sections)
        {
            if(section != null)
                size += section.getSerializedSize();
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

        Sets.newHashSet(this.tileEntities.keySet()).forEach(this.world::removeBlockEntity);
        for (int i = 0; i < IBigCube.SECTION_COUNT; i++) {
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

            int dy = Coords.indexToY(i);

            SectionPos sectionPos = getCubePos().asSectionPos();
            int y = sectionPos.getY() + dy;

            readSection(i, y, null, readBuffer, nbtTagIn, exists);
        }
    }

    private void readSection(int sectionIdx, int sectionY, @Nullable CubeBiomeContainer biomeContainerIn, PacketBuffer packetBufferIn, CompoundNBT nbtIn,
                             boolean sectionExists) {

        for (TileEntity tileEntity : tileEntities.values()) {
            tileEntity.clearCache();
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
            String typeId = type.getSerializationKey();
            if (nbtIn.contains(typeId, Constants.NBT.TAG_LONG_ARRAY)) {
                this.setHeightmap(type, nbtIn.getLongArray(typeId));
            }
        }

        for (TileEntity tileentity : this.tileEntities.values()) {
            tileentity.clearCache();
        }
    }

    public void setCubeBiomeContainer(CubeBiomeContainer biomes) {
        this.cubeBiomeContainer = biomes;
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

    @Override public Collection<Map.Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setHeightmap(Heightmap.Type type, long[] data) {

    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Type typeIn) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public int getHeight(Heightmap.Type heightmapType, int x, int z) {
        return 0;
    }

    @Override public void setLastSaveTime(long saveTime) {

    }
    @Override public void setCubeLastSaveTime(long saveTime) {
        lastSaveTime = saveTime;
    }

    @Deprecated @Override public Map<Structure<?>, StructureStart<?>> getAllStarts() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Deprecated @Override public void setAllStarts(Map<Structure<?>, StructureStart<?>> structureStartsIn) {

    }

    @Override public ShortList[] getPostProcessing() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public ITickList<Block> getBlockTicks() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public ITickList<Fluid> getLiquidTicks() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public UpgradeData getUpgradeData() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // getStructureStart
    @Nullable @Override public StructureStart<?> getStartForFeature(Structure<?> var1) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // putStructureStart
    @Override public void setStartForFeature(Structure<?> structureIn, StructureStart<?> structureStartIn) {

    }

    // getStructureReferences
    @Override public LongSet getReferencesForFeature(Structure<?> structureIn) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // addStructureReference
    @Override public void addReferenceForFeature(Structure<?> structure, long reference) {

    }

    @Override public Map<Structure<?>, LongSet> getAllReferences() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setAllReferences(Map<Structure<?>, LongSet> p_201606_1_) {

    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
    public boolean getLoaded()
    {
        return this.loaded;
    }
    public void postLoad() {
        if (this.postLoadConsumer != null) {
            this.postLoadConsumer.accept(this);
            this.postLoadConsumer = null;
        }
    }
}