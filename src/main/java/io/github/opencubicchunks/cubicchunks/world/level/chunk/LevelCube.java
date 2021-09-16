package io.github.opencubicchunks.cubicchunks.world.level.chunk;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.*;
import static net.minecraft.world.level.chunk.LevelChunk.EMPTY_SECTION;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.LevelChunkSectionAccess;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.levelgen.heightmap.LightSurfaceTrackerSection;
import io.github.opencubicchunks.cubicchunks.world.level.levelgen.heightmap.SurfaceTrackerSection;
import io.github.opencubicchunks.cubicchunks.world.level.levelgen.heightmap.SurfaceTrackerWrapper;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeProtoTickList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ChunkTickList;
import net.minecraft.world.level.EmptyTickList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LevelCube implements ChunkAccess, CubeAccess, CubicLevelHeightAccessor {

    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
        public void tick() {
        }

        public boolean isRemoved() {
            return true;
        }

        public BlockPos getPos() {
            return BlockPos.ZERO;
        }

        public String getType() {
            return "<null>";
        }
    };
    private static final Logger LOGGER = LogManager.getLogger(LevelCube.class);

    private final CubePos cubePos;
    private final UpgradeData upgradeData;
    private TickList<Block> blockTicks;
    private TickList<Fluid> fluidTicks;
    private final LevelChunkSection[] sections = new LevelChunkSection[SECTION_COUNT];
    private final ShortList[] postProcessing;

    private final HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private final Map<BlockPos, RebindableTickingBlockEntityWrapper> tickersInLevel = new HashMap<>();
    private final ClassInstanceMultiMap<Entity>[] entityLists;
    private final Level level;

    private final Map<StructureFeature<?>, StructureStart<?>> structureStarts;
    private final Map<StructureFeature<?>, LongSet> structuresRefences;

    private final Map<Heightmap.Types, SurfaceTrackerSection[]> heightmaps;
    private final LightSurfaceTrackerSection[] lightHeightmaps = new LightSurfaceTrackerSection[CubeAccess.DIAMETER_IN_SECTIONS * CubeAccess.DIAMETER_IN_SECTIONS];

    private ChunkBiomeContainer cubeBiomeContainer;

    private boolean dirty = true; // todo: change back to false?
    private boolean loaded = false;

    private volatile boolean lightCorrect;
    private final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();

    private long inhabitedTime;
    @Nullable private Consumer<LevelCube> postLoad;
    @Nullable private Supplier<ChunkHolder.FullChunkStatus> fullStatus;

    private final boolean isCubic;
    private final boolean generates2DChunks;
    private final WorldStyle worldStyle;

    public LevelCube(Level level, CubePos cubePos, ChunkBiomeContainer biomeContainer) {
        this(level, cubePos, biomeContainer, UpgradeData.EMPTY, EmptyTickList.empty(), EmptyTickList.empty(), 0L, null, null);
    }

    public LevelCube(Level level, CubePos cubePos, ChunkBiomeContainer biomeContainer, UpgradeData upgradeData, TickList<Block> blockTicks,
                     TickList<Fluid> fluidTicks, long inhabitedTime, @Nullable LevelChunkSection[] sections, @Nullable Consumer<LevelCube> postLoad) {
        this.level = level;
        this.cubePos = cubePos;
        this.upgradeData = upgradeData;
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
        this.heightmaps = Maps.newEnumMap(Heightmap.Types.class);
//        this.upgradeData = upgradeDataIn;

//        for(Heightmap.Type heightmap$type : Heightmap.Type.values()) {
//            if (ChunkStatus.FULL.getHeightMaps().contains(heightmap$type)) {
//                this.heightMap.put(heightmap$type, new Heightmap(this, heightmap$type));
//            }
//        }

        this.structureStarts = Maps.newHashMap();
        this.structuresRefences = Maps.newHashMap();

        //noinspection unchecked
        this.entityLists = new ClassInstanceMultiMap[CubeAccess.SECTION_COUNT];
        for (int i = 0; i < this.entityLists.length; ++i) {
            this.entityLists[i] = new ClassInstanceMultiMap<>(Entity.class);
        }

        this.cubeBiomeContainer = biomeContainer;
//        this.blockBiomeArray = biomeContainer;
//        this.blocksToBeTicked = tickBlocksIn;
//        this.fluidsToBeTicked = tickFluidsIn;
        this.inhabitedTime = inhabitedTime;
        this.postLoad = postLoad;

        if (sections != null) {
            if (sections.length != SECTION_COUNT) {
                throw new IllegalStateException("Number of Sections must equal BigCube.CUBESIZE");
            }

            for (int i = 0; i < sections.length; i++) {
                int sectionYPos = cubeToSection(cubePos.getY(), indexToY(i));

                if (sections[i] != null) {
                    this.sections[i] = new LevelChunkSection(sectionYPos,
                        ((LevelChunkSectionAccess) sections[i]).getNonEmptyBlockCount(),
                        ((LevelChunkSectionAccess) sections[i]).getTickingBlockCount(),
                        ((LevelChunkSectionAccess) sections[i]).getTickingFluidCount());
                    //noinspection ConstantConditions
                    ((LevelChunkSectionAccess) this.sections[i]).setStates(sections[i].getStates());
                }
            }
        }
        this.postProcessing = new ShortList[CubeAccess.SECTION_COUNT];

        isCubic = ((CubicLevelHeightAccessor) level).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) level).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) level).worldStyle();

//        this.gatherCapabilities();
    }

    public LevelCube(Level level, ProtoCube protoCube, @Nullable Consumer<LevelCube> postLoad) {
        //TODO: reimplement full BigCube constructor from CubePrimer
//        this(level, cubePrimer.getCubePos(), cubePrimer.getCubeBiomes(), cubePrimer.getUpgradeData(), cubePrimer.getBlocksToBeTicked(),
//            cubePrimer.getFluidsToBeTicked(), cubePrimer.getInhabitedTime(), cubePrimer.getSections(), (Consumer<BigCube>)null);
        this(level, protoCube.getCubePos(), protoCube.getBiomes(), null, protoCube.getBlockTicks(),
            protoCube.getLiquidTicks(), protoCube.getCubeInhabitedTime(), protoCube.getCubeSections(), postLoad);

        for (CompoundTag tag : protoCube.getCubeEntities()) {
            EntityType.loadEntityRecursive(tag, level, (entity) -> {
                this.addEntity(entity);
                return entity;
            });
        }

        for (BlockEntity blockEntity : protoCube.getCubeBlockEntities().values()) {
            this.setBlockEntity(blockEntity);
        }

        this.pendingBlockEntities.putAll(protoCube.getCubeBlockEntityNbts());

        for (int i = 0; i < protoCube.getPostProcessing().length; ++i) {
            this.postProcessing[i] = protoCube.getPostProcessing()[i];
        }

        this.setAllStarts(protoCube.getAllCubeStructureStarts());
        this.setAllReferences(protoCube.getAllReferences());

        LightSurfaceTrackerSection[] protoCubeLightHeightmaps = protoCube.getLightHeightmaps();
        for (int i = 0; i < CubeAccess.CHUNK_COUNT; i++) {
            this.lightHeightmaps[i] = protoCubeLightHeightmaps[i];
            if (this.lightHeightmaps[i] == null) {
                System.out.println("Got a null light heightmap while upgrading from CubePrimer at " + this.cubePos);
            } else {
                this.lightHeightmaps[i].upgradeCube(this);
            }
        }

        this.setCubeLight(protoCube.hasCubeLight());
        this.dirty = true;
    }

    @Override public void setLightHeightmapSection(LightSurfaceTrackerSection section, int localSectionX, int localSectionZ) {
        int idx = localSectionX + localSectionZ * DIAMETER_IN_SECTIONS;
        this.lightHeightmaps[idx] = section;
    }

    @Deprecated @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public CubePos getCubePos() {
        return this.cubePos;
    }

    @Deprecated @Override public LevelChunkSection[] getSections() {
        return this.sections;
    }

    @Override public LevelChunkSection[] getCubeSections() {
        return this.sections;
    }

    @Deprecated @Override public ChunkStatus getStatus() {
        return this.getCubeStatus();
    }

    @Override public ChunkStatus getCubeStatus() {
        return ChunkStatus.FULL;
    }

    public ChunkHolder.FullChunkStatus getFullStatus() {
        return this.fullStatus == null ? ChunkHolder.FullChunkStatus.BORDER : this.fullStatus.get();
    }

    public void setFullStatus(Supplier<ChunkHolder.FullChunkStatus> supplier) {
        this.fullStatus = supplier;
    }

    //BLOCK
    @Deprecated @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return setBlock(pos, state, isMoving);
    }

    @Override @Nullable public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        return this.setBlock(blockToIndex(pos.getX(), pos.getY(), pos.getZ()), pos, state, isMoving);
    }

    @Nullable public BlockState setBlock(int sectionIndex, BlockPos pos, BlockState newState, boolean isMoving) {
        int x = pos.getX() & 15;
        int y = pos.getY() & 15;
        int z = pos.getZ() & 15;
        LevelChunkSection section = sections[sectionIndex];

        BlockState oldState = section.setBlockState(x, y, z, newState);
        if (oldState == newState) {
            return null;
        }
        Block newBlock = newState.getBlock();
        int localX = blockToLocal(pos.getX());
        int localZ = blockToLocal(pos.getZ());

        if (!this.heightmaps.isEmpty()) {
            int xSection = blockToCubeLocalSection(pos.getX());
            int zSection = blockToCubeLocalSection(pos.getZ());

            int idx = xSection + zSection * DIAMETER_IN_SECTIONS;

            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING)[idx].onSetBlock(localX, pos.getY(), localZ, newState);
            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)[idx].onSetBlock(localX, pos.getY(), localZ, newState);
            this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR)[idx].onSetBlock(localX, pos.getY(), localZ, newState);
            this.heightmaps.get(Heightmap.Types.WORLD_SURFACE)[idx].onSetBlock(localX, pos.getY(), localZ, newState);
        }

        boolean hadBlockEntity = oldState.hasBlockEntity();
        if (!this.level.isClientSide) {
            oldState.onRemove(this.level, pos, newState, isMoving);
        } else if (!oldState.is(newBlock) && hadBlockEntity) {
            this.removeBlockEntity(pos);
        }

        if (section.getBlockState(x, y, z).getBlock() != newBlock) {
            return null;
        }

        if (!this.level.isClientSide) {
            newState.onPlace(this.level, pos, oldState, isMoving);
        }

        if (newState.hasBlockEntity()) {
            BlockEntity blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
            if (blockEntity == null) {
                blockEntity = ((EntityBlock) newBlock).newBlockEntity(pos, newState);
                if (blockEntity != null) {
                    this.addAndRegisterBlockEntity(blockEntity);
                }
            } else {
                blockEntity.setBlockState(newState);
                this.updateBlockEntityTicker(blockEntity);
            }
        }

        this.dirty = true;
        return oldState;
    }

    @Override public BlockState getBlockState(int x, int y, int z) {
        // TODO: crash report generation
        int index = blockToIndex(x, y, z);
        return LevelChunkSection.isEmpty(this.sections[index]) ?
            Blocks.AIR.defaultBlockState() :
            this.sections[index].getBlockState(x & 15, y & 15, z & 15);
    }

    //ENTITY
    @Deprecated @Override public void addEntity(Entity entity) {
        // empty in vanilla too
    }

    public ClassInstanceMultiMap<Entity>[] getCubeEntityLists() {
        return entityLists;
    }

    public ClassInstanceMultiMap<Entity>[] getEntityLists() {
        return this.getCubeEntityLists();
    }

    private int getIndexFromEntity(Entity entity) {
        return blockToIndex((int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
    }

    public void removeEntity(Entity entity) {
        this.removeEntityAtIndex(entity, this.getIndexFromEntity(entity));
    }

    public void removeEntityAtIndex(Entity entity, int index) {
        if (index < 0) {
            index = 0;
        }
        if (index >= this.entityLists.length) {
            index = this.entityLists.length - 1;
        }
        this.entityLists[index].remove(entity);
        this.setDirty(true);
    }

    //TILEENTITY
    @Deprecated @Override public void setBlockEntityNbt(CompoundTag nbt) {
        this.setCubeBlockEntity(nbt);
    }

    @Override public void setCubeBlockEntity(CompoundTag nbt) {
        this.pendingBlockEntities.put(new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt);
    }

    @Deprecated @Override public void setBlockEntity(BlockEntity blockEntity) {
        this.setCubeBlockEntity(blockEntity);
    }

    @Override public BlockPos getHeighestPosition(Heightmap.Types type) {
        BlockPos.MutableBlockPos mutableBlockPos = null;

        for (int x = this.cubePos.minCubeX(); x <= this.cubePos.maxCubeX(); ++x) {
            for (int z = this.cubePos.minCubeZ(); z <= this.cubePos.maxCubeZ(); ++z) {
                int height = this.getHeight(type, x & 15, z & 15);
                if (mutableBlockPos == null) {
                    mutableBlockPos = new BlockPos.MutableBlockPos().set(x, height, z);
                }

                if (mutableBlockPos.getY() < height) {
                    mutableBlockPos.set(x, height, z);
                }
            }
        }

        return mutableBlockPos != null ? mutableBlockPos.immutable() : new BlockPos.MutableBlockPos().set(this.cubePos.minCubeX(), this.getHeight(type, this.cubePos.minCubeX() & 15,
            this.cubePos.minCubeZ() & 15), this.cubePos.minCubeZ() & 15);
    }

    @Override public void setCubeBlockEntity(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (this.getBlockState(pos).hasBlockEntity()) {
            blockEntity.setLevel(this.level);
            blockEntity.clearRemoved();
            BlockEntity old = this.blockEntities.put(pos.immutable(), blockEntity);
            if (old != null && old != blockEntity) {
                old.setRemoved();
            }
        }
    }

    public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
        this.setBlockEntity(blockEntity);
        if (isInLevel()) {
            this.updateBlockEntityTicker(blockEntity);
        }
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.blockEntities.values().forEach(this::updateBlockEntityTicker);
    }

    public <T extends BlockEntity> void updateBlockEntityTicker(T blockEntity) {
        BlockState blockState = blockEntity.getBlockState();
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> blockEntityTicker = (BlockEntityTicker<T>) blockState.getTicker(this.level, blockEntity.getType());
        if (blockEntityTicker == null) {
            this.removeBlockEntityTicker(blockEntity.getBlockPos());
        } else {
            this.tickersInLevel.compute(blockEntity.getBlockPos(), (blockPos, wrapper) -> {
                TickingBlockEntity tickingBlockEntity = this.createTicker(blockEntity, blockEntityTicker);
                if (wrapper != null) {
                    wrapper.rebind(tickingBlockEntity);
                    return wrapper;
                } else if (this.isInLevel()) {
                    RebindableTickingBlockEntityWrapper newWrapper = new RebindableTickingBlockEntityWrapper(tickingBlockEntity);
                    this.level.addBlockEntityTicker(newWrapper);
                    return newWrapper;
                } else {
                    return null;
                }
            });
        }
    }

    private <T extends BlockEntity> TickingBlockEntity createTicker(T blockEntity, BlockEntityTicker<T> blockEntityTicker) {
        return new BoundTickingBlockEntity<T>(blockEntity, blockEntityTicker);
    }

    // NOTE: this should not be in API
    public void removeBlockEntityTicker(BlockPos blockPos) {
        RebindableTickingBlockEntityWrapper wrapper = this.tickersInLevel.remove(blockPos);
        if (wrapper != null) {
            wrapper.rebind(NULL_TICKER);
        }

    }

    @Deprecated @Override public void removeBlockEntity(BlockPos pos) {
        this.removeCubeBlockEntity(pos);
    }

    @Override public void removeCubeBlockEntity(BlockPos pos) {
        if (isInLevel()) {
            BlockEntity blockEntity = this.blockEntities.remove(pos);
            if (blockEntity != null) {
                blockEntity.setRemoved();
            }
        }
        this.removeBlockEntityTicker(pos);
    }

    @Nullable @Override public BlockEntity getBlockEntity(BlockPos pos) {
        return getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
    }

    @Nullable public BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationMode) {
        BlockEntity blockEntity = this.blockEntities.get(pos);
        if (blockEntity == null) {
            CompoundTag nbt = this.pendingBlockEntities.remove(pos);
            if (nbt != null) {
                BlockEntity pendingPromoted = this.promotePendingBlockEntity(pos, nbt);
                if (pendingPromoted != null) {
                    return pendingPromoted;
                }
            }
        }

        if (blockEntity == null) {
            if (creationMode == LevelChunk.EntityCreationType.IMMEDIATE) {
                blockEntity = this.createNewBlockEntity(pos);
                if (blockEntity != null) {
                    this.addAndRegisterBlockEntity(blockEntity);
                }
            }
        } else if (blockEntity.isRemoved()) {
            blockEntities.remove(pos);
            return null;
        }
        return blockEntity;
    }

    @Nullable private BlockEntity createNewBlockEntity(BlockPos pos) {
        BlockState state = this.getBlockState(pos);
        return !state.hasBlockEntity() ? null : ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
    }

    @Nullable private BlockEntity promotePendingBlockEntity(BlockPos pos, CompoundTag compound) {
        BlockEntity blockEntity;
        BlockState state = this.getBlockState(pos);
        if ("DUMMY".equals(compound.getString("id"))) {
            if (state.hasBlockEntity()) {
                blockEntity = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
            } else {
                blockEntity = null;
                CubicChunks.LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, this.getBlockState(pos));
            }
        } else {
            blockEntity = BlockEntity.loadStatic(pos, state, compound);
        }

        if (blockEntity != null) {
            blockEntity.setLevel(this.level);
            this.addAndRegisterBlockEntity(blockEntity);
        } else {
            CubicChunks.LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", state, pos);
        }

        return blockEntity;
    }

    public Map<BlockPos, BlockEntity> getTileEntityMap() {
        return blockEntities;
    }

    public Map<BlockPos, CompoundTag> getPendingBlockEntities() {
        return this.pendingBlockEntities;
    }

    @Deprecated @Override public Set<BlockPos> getBlockEntitiesPos() {
        return this.getCubeBlockEntitiesPos();
    }

    @Override public Set<BlockPos> getCubeBlockEntitiesPos() {
        Set<BlockPos> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
        set.addAll(this.blockEntities.keySet());
        return set;
    }

    @Deprecated @Nullable @Override public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        return this.getCubeBlockEntityNbtForSaving(pos);
    }

    @Nullable @Override public CompoundTag getCubeBlockEntityNbtForSaving(BlockPos pos) {
        BlockEntity blockEntities = this.getBlockEntity(pos);
        if (blockEntities != null && !blockEntities.isRemoved()) {
            CompoundTag tag = blockEntities.save(new CompoundTag());
            tag.putBoolean("keepPacked", false);
            return tag;
        } else {
            CompoundTag tag = this.pendingBlockEntities.get(pos);
            if (tag != null) {
                tag = tag.copy();
                tag.putBoolean("keepPacked", true);
            }
            return tag;
        }
    }

    public void postProcessGeneration() {
        for (int i = 0; i < this.postProcessing.length; ++i) {
            if (this.postProcessing[i] != null) {
                for (Short sectionRel : this.postProcessing[i]) {
                    BlockPos blockPos = ProtoCube.unpackToWorld(sectionRel, this.getSectionYFromSectionIndex(i), this.cubePos);
                    BlockState blockState = this.getBlockState(blockPos);
                    BlockState blockState2 = Block.updateFromNeighbourShapes(blockState, this.level, blockPos);
                    this.level.setBlock(blockPos, blockState2, 20);
                }
                this.postProcessing[i].clear();
            }
        }
        this.unpackTicks();

        for (BlockPos blockPos : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
            this.getBlockEntity(blockPos);
        }

        this.pendingBlockEntities.clear();
//        this.upgradeData.upgrade(this); //TODO: DFU
    }

    @Deprecated @Nullable @Override public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return this.getCubeBlockEntityNbt(pos);
    }

    @Nullable @Override public CompoundTag getCubeBlockEntityNbt(BlockPos pos) {
        return this.pendingBlockEntities.get(pos);
    }

    //LIGHTING
    @Deprecated @Override public boolean isLightCorrect() {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public boolean hasCubeLight() {
        return this.lightCorrect;
    }

    @Deprecated @Override public void setLightCorrect(boolean lightCorrectIn) {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public void setCubeLight(boolean lightCorrectIn) {
        this.lightCorrect = lightCorrectIn;
        this.setDirty(true);
    }

    @Deprecated @Override public Stream<BlockPos> getLights() {
        return this.getCubeLights();
    }

    @Override public Stream<BlockPos> getCubeLights() {
        return StreamSupport.stream(
                BlockPos.betweenClosed(
                    this.cubePos.minCubeX(), this.cubePos.minCubeY(), this.cubePos.minCubeZ(),
                    this.cubePos.maxCubeX(), this.cubePos.maxCubeY(), this.cubePos.maxCubeZ()
                ).spliterator(), false)
            .filter((blockPos) -> this.getBlockState(blockPos).getLightEmission() != 0);
    }

    //MISC
    public boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    private boolean isTicking(BlockPos blockPos) {
        return (this.level.isClientSide() || this.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING)) && this.level.getWorldBorder().isWithinBounds(blockPos);
    }

    @Deprecated @Override public void setUnsaved(boolean modified) {
        setDirty(modified);
    }

    @Override public void setDirty(boolean modified) {
        this.dirty = modified;
    }

    @Deprecated @Override public boolean isUnsaved() {
        return isDirty();
    }

    @Override public boolean isDirty() {
        return this.dirty;
    }

    @Override public boolean isEmptyCube() {
        for (LevelChunkSection section : this.sections) {
            if (section != EMPTY_SECTION && !section.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Deprecated @Override public long getInhabitedTime() {
        return this.getCubeInhabitedTime();
    }

    @Override public long getCubeInhabitedTime() {
        return this.inhabitedTime;
    }

    @Deprecated @Override public void setInhabitedTime(long newInhabitedTime) {
        this.setCubeInhabitedTime(newInhabitedTime);
    }

    @Override public void setCubeInhabitedTime(long newInhabitedTime) {
        this.inhabitedTime = newInhabitedTime;
    }

    @Deprecated @Nullable @Override public ChunkBiomeContainer getBiomes() {
        return this.cubeBiomeContainer;
    }

    public int getSize() {
        int size = MathUtil.ceilDiv(sections.length, Byte.SIZE); // exists flags
        for (LevelChunkSection section : this.sections) {
            if (section != null) {
                size += section.getSerializedSize();
            }
        }
        return size;
    }

    public void write(FriendlyByteBuf buf) {
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
        for (LevelChunkSection section : sections) {
            if (section != null && !section.isEmpty()) {
                section.write(buf);
            }
        }
    }

    public void read(@Nullable ChunkBiomeContainer biomes, FriendlyByteBuf readBuffer, CompoundTag tag, boolean cubeExists) {
        if (!cubeExists) {
            Arrays.fill(sections, null);
            return;
        }
        byte[] emptyFlagsBytes = new byte[MathUtil.ceilDiv(sections.length, Byte.SIZE)];
        readBuffer.readBytes(emptyFlagsBytes);
        BitSet emptyFlags = BitSet.valueOf(emptyFlagsBytes);

        if (biomes != null) {
            this.cubeBiomeContainer = biomes;
        }

        // TODO: support partial updates
        this.blockEntities.values().forEach(this::onBlockEntityRemove);
        this.blockEntities.clear();

        for (int i = 0; i < CubeAccess.SECTION_COUNT; i++) {
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

            int dy = indexToY(i);

            SectionPos sectionPos = getCubePos().asSectionPos();
            int y = sectionPos.getY() + dy;

            readSection(i, y, null, readBuffer, tag, exists);
        }
    }

    private void onBlockEntityRemove(BlockEntity blockEntity) {
        blockEntity.setRemoved();
        this.tickersInLevel.remove(blockEntity.getBlockPos());
    }

    private void readSection(int sectionIdx, int sectionY, @Nullable ChunkBiomeContainer biomeContainer, FriendlyByteBuf byteBuf, CompoundTag nbt, boolean sectionExists) {
        LevelChunkSection section = this.sections[sectionIdx];
        if (section == EMPTY_SECTION) {
            section = new LevelChunkSection(sectionY << 4);
            this.sections[sectionIdx] = section;
        }
        if (sectionExists) {
            section.read(byteBuf);
        }
        if (biomeContainer != null) {
            this.cubeBiomeContainer = biomeContainer;
        }
        for (Heightmap.Types type : Heightmap.Types.values()) {
            String typeId = type.getSerializationKey();
            if (nbt.contains(typeId, 12)) { // NBT TAG_LONG_ARRAY
                this.setHeightmap(type, nbt.getLongArray(typeId));
            }
        }
    }

    @Deprecated
    public SectionPos getSectionPosition(int index) {
        int xPos = indexToX(index);
        int yPos = indexToY(index);
        int zPos = indexToZ(index);

        SectionPos sectionPos = this.cubePos.asSectionPos();
        return SectionPos.of(xPos + sectionPos.getX(), yPos + sectionPos.getY(), zPos + sectionPos.getZ());
    }


    public Level getLevel() {
        return level;
    }

    @Override public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void setHeightmap(Heightmap.Types type, long[] data) {
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public void loadHeightmapSection(SurfaceTrackerSection section, int localSectionX, int localSectionZ) {
        int idx = localSectionX + localSectionZ * DIAMETER_IN_SECTIONS;

        this.heightmaps.computeIfAbsent(section.getType(), t -> new SurfaceTrackerSection[CubeAccess.DIAMETER_IN_SECTIONS * CubeAccess.DIAMETER_IN_SECTIONS])[idx] = section;
    }

    @Override public int getCubeLocalHeight(Heightmap.Types type, int x, int z) {
        SurfaceTrackerSection[] surfaceTrackerSections = this.heightmaps.get(type);
        if (surfaceTrackerSections == null) {
            throw new IllegalStateException("Trying to access heightmap of type " + type + " for cube " + cubePos + " before it's loaded!");
        }
        int xSection = blockToCubeLocalSection(x);
        int zSection = blockToCubeLocalSection(z);

        int idx = xSection + zSection * DIAMETER_IN_SECTIONS;

        SurfaceTrackerSection surfaceTrackerSection = surfaceTrackerSections[idx];
        return surfaceTrackerSection.getHeight(blockToLocal(x), blockToLocal(z));
    }

    @Override public int getHeight(Heightmap.Types type, int x, int z) { //TODO: Use heightmap sections from column instead.
        SurfaceTrackerSection[] surfaceTrackerSections = this.heightmaps.get(type);
        if (surfaceTrackerSections == null) {
            throw new IllegalStateException("Trying to access heightmap of type " + type + " for cube " + cubePos + " before it's loaded!");
        }
        int xSection = blockToCubeLocalSection(x);
        int zSection = blockToCubeLocalSection(z);

        int idx = xSection + zSection * DIAMETER_IN_SECTIONS;

        SurfaceTrackerSection surfaceTrackerSection = surfaceTrackerSections[idx];

        while (surfaceTrackerSection.getParent() != null) {
            surfaceTrackerSection = surfaceTrackerSection.getParent();
        }
        return surfaceTrackerSection.getHeight(blockToLocal(x), blockToLocal(z));
    }

    @Override public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    @Override public TickList<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override public TickList<Fluid> getLiquidTicks() {
        return this.fluidTicks;
    }

    @Override public UpgradeData getUpgradeData() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        try {
            int index = blockToIndex(x, y, z);
            if (!LevelChunkSection.isEmpty(this.sections[index])) {
                return this.sections[index].getFluidState(x & 15, y & 15, z & 15);
            }
            return Fluids.EMPTY.defaultFluidState();
        } catch (Throwable var7) {
            CrashReport crashReport = CrashReport.forThrowable(var7, "Getting fluid state");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
            crashReportCategory.setDetail("Location", () -> {
                return CrashReportCategory.formatLocation(this, x, y, z);
            });
            throw new ReportedException(crashReport);
        }
    }


    @Nullable
    public StructureStart<?> getStartForFeature(StructureFeature<?> structureFeature) {
        return this.structureStarts.get(structureFeature);
    }

    public void setStartForFeature(StructureFeature<?> structureFeature, StructureStart<?> structureStart) {
        this.structureStarts.put(structureFeature, structureStart);
    }

    @Override
    public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
        return getAllCubeStructureStarts();
    }

    public Map<StructureFeature<?>, StructureStart<?>> getAllCubeStructureStarts() {
        return this.structureStarts;
    }

    public void setAllStarts(Map<StructureFeature<?>, StructureStart<?>> map) {
        this.structureStarts.clear();
        this.structureStarts.putAll(map);
    }

    public LongSet getReferencesForFeature(StructureFeature<?> structureFeature) {
        return this.structuresRefences.computeIfAbsent(structureFeature, x -> new LongOpenHashSet());
    }

    public void addReferenceForFeature(StructureFeature<?> structureFeature, long cubeLong) {
        this.structuresRefences.computeIfAbsent(structureFeature, x -> new LongOpenHashSet()).add(cubeLong);
    }

    public Map<StructureFeature<?>, LongSet> getAllReferences() {
        return this.structuresRefences;
    }

    @Override
    public void setAllReferences(Map<StructureFeature<?>, LongSet> map) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(map);
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean getLoaded() {
        return this.loaded;
    }

    public void packTicks(ServerLevel world) {
        if (this.blockTicks == EmptyTickList.<Block>empty()) {
            this.blockTicks = new ChunkTickList<>(Registry.BLOCK::getKey, world.getBlockTicks().fetchTicksInChunk(new ImposterChunkPos(this.cubePos), true, false), world.getGameTime());
            this.setUnsaved(true);
        }

        if (this.fluidTicks == EmptyTickList.<Fluid>empty()) {
            this.fluidTicks = new ChunkTickList<>(Registry.FLUID::getKey, world.getLiquidTicks().fetchTicksInChunk(new ImposterChunkPos(this.cubePos), true, false), world.getGameTime());
            this.setUnsaved(true);
        }
    }

    public void unpackTicks() {
        if (this.blockTicks instanceof CubeProtoTickList) {
            ((CubeProtoTickList<Block>) this.blockTicks).copyOut(this.level.getBlockTicks(), (pos) -> {
                return this.getBlockState(pos).getBlock();
            });
            this.blockTicks = EmptyTickList.empty();
        } else if (this.blockTicks instanceof ChunkTickList) {
            ((ChunkTickList<Block>) this.blockTicks).copyOut(this.level.getBlockTicks());
            this.blockTicks = EmptyTickList.empty();
        }

        if (this.fluidTicks instanceof CubeProtoTickList) {
            ((CubeProtoTickList<Fluid>) this.fluidTicks).copyOut(this.level.getLiquidTicks(), (pos) -> {
                return this.getFluidState(pos).getType();
            });
            this.fluidTicks = EmptyTickList.empty();
        } else if (this.fluidTicks instanceof ChunkTickList) {
            ((ChunkTickList<Fluid>) this.fluidTicks).copyOut(this.level.getLiquidTicks());
            this.fluidTicks = EmptyTickList.empty();
        }
    }

    public void postLoad() {
        if (this.postLoad != null) {
            this.postLoad.accept(this);
            this.postLoad = null;
        }
        // TODO heightmap stuff should probably be elsewhere rather than here.
        ChunkPos pos = this.cubePos.asChunkPos();
        for (int x = 0; x < CubeAccess.DIAMETER_IN_SECTIONS; x++) {
            for (int z = 0; z < CubeAccess.DIAMETER_IN_SECTIONS; z++) {

                // This force-loads the column, but it shouldn't matter if column-cube load order is working properly.
                LevelChunk chunk = this.level.getChunk(pos.x + x, pos.z + z);
                ((ColumnCubeMapGetter) chunk).getCubeMap().markLoaded(this.cubePos.getY());
                for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
                    Heightmap heightmap = entry.getValue();
                    SurfaceTrackerWrapper tracker = (SurfaceTrackerWrapper) heightmap;
                    tracker.loadCube(this);
                }

                if (!this.level.isClientSide) {
                    // TODO probably don't want to do this if the cube was already loaded as a CubePrimer
                    ((LightHeightmapGetter) chunk).getServerLightHeightmap().loadCube(this);
                }
            }
        }
    }

    public void invalidateAllBlockEntities() {
        this.blockEntities.values().forEach(this::onBlockEntityRemove);
    }

    @Override
    public void markPosForPostprocessing(BlockPos blockPos) {
        // TODO: why?
        if (System.currentTimeMillis() % 15000 == 0) {
            LogManager.getLogger().warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", blockPos);
        }
    }

    @Override public int getHeight() {
        return level.getHeight();
    }

    @Override public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }

    @Override public WorldStyle worldStyle() {
        return worldStyle;
    }

    @Override public boolean isCubic() {
        return isCubic;
    }

    @Override public boolean generates2DChunks() {
        return generates2DChunks;
    }

    public static class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
        private TickingBlockEntity ticker;

        private RebindableTickingBlockEntityWrapper(TickingBlockEntity tickingBlockEntity) {
            this.ticker = tickingBlockEntity;
        }

        private void rebind(TickingBlockEntity tickingBlockEntity) {
            this.ticker = tickingBlockEntity;
        }

        public void tick() {
            this.ticker.tick();
        }

        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        public BlockPos getPos() {
            return this.ticker.getPos();
        }

        public String getType() {
            return this.ticker.getType();
        }

        public String toString() {
            return this.ticker.toString() + " <wrapped>";
        }
    }

    public class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;

        private BoundTickingBlockEntity(T blockEntity, BlockEntityTicker<T> blockEntityTicker) {
            this.blockEntity = blockEntity;
            this.ticker = blockEntityTicker;
        }

        public void tick() {
            if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
                BlockPos blockPos = this.blockEntity.getBlockPos();
                if (LevelCube.this.isTicking(blockPos)) {
                    try {
                        ProfilerFiller profilerFiller = LevelCube.this.level.getProfiler();
                        profilerFiller.push(this::getType);
                        BlockState blockState = LevelCube.this.getBlockState(blockPos);
                        if (this.blockEntity.getType().isValid(blockState)) {
                            this.ticker.tick(LevelCube.this.level, this.blockEntity.getBlockPos(), blockState, this.blockEntity);
                            this.loggedInvalidBlockState = false;
                        } else if (!this.loggedInvalidBlockState) {
                            this.loggedInvalidBlockState = true;
                            LevelCube.LOGGER.warn("Block entity {} @ {} state {} invalid for ticking:", this::getType, this::getPos, () -> blockState);
                        }

                        profilerFiller.pop();
                    } catch (Throwable var5) {
                        CrashReport crashReport = CrashReport.forThrowable(var5, "Ticking block entity");
                        CrashReportCategory crashReportCategory = crashReport.addCategory("Block entity being ticked");
                        this.blockEntity.fillCrashReportCategory(crashReportCategory);
                        throw new ReportedException(crashReport);
                    }
                }
            }

        }

        public boolean isRemoved() {
            return this.blockEntity.isRemoved();
        }

        public BlockPos getPos() {
            return this.blockEntity.getBlockPos();
        }

        public String getType() {
            return BlockEntityType.getKey(this.blockEntity.getType()).toString();
        }

        public String toString() {
            return "Level ticker for " + this.getType() + "@" + this.getPos();
        }
    }
}