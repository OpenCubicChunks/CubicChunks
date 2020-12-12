package io.github.opencubicchunks.cubicchunks.chunk.cube;

import static net.minecraft.world.level.chunk.LevelChunk.EMPTY_SECTION;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.LightHeightmapGetter;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.LightSurfaceTrackerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.SurfaceTrackerSection;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;

//ProtoChunk
public class CubePrimer implements IBigCube, ChunkAccess {

    private final CubePos cubePos;
    private final LevelChunkSection[] sections;
    private final LevelHeightAccessor levelHeightAccessor;
    private ChunkStatus status = ChunkStatus.EMPTY;

    @Nullable
    private CubeBiomeContainer biomes;

    private final Map<Heightmap.Types, SurfaceTrackerSection[]> heightmaps;


    private final List<CompoundTag> entities = Lists.newArrayList();
    private final Map<BlockPos, BlockEntity> tileEntities = Maps.newHashMap();
    private final Map<BlockPos, CompoundTag> deferredTileEntities = Maps.newHashMap();

    //Structures
    private final Map<StructureFeature<?>, StructureStart<?>> structureStarts;
    private final Map<StructureFeature<?>, LongSet> structuresRefences;
    private final Map<GenerationStep.Carving, BitSet> carvingMasks;

    private volatile boolean isDirty;

    private volatile boolean modified = true;

    private final List<BlockPos> lightPositions = Lists.newArrayList();
    private volatile boolean hasLight;
    private LevelLightEngine lightManager;

    private long inhabitedTime;

    private boolean[] lightHeightmapLoaded;

    public CubePrimer(CubePos cubePos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor) {
//        this(cubePos, upgradeData, (ChunkSection[])null, new ChunkPrimerTickList<>((p_205332_0_) -> {
//            return p_205332_0_ == null || p_205332_0_.defaultBlockState().isAir();
//        }, cubePos), new ChunkPrimerTickList<>((p_205766_0_) -> {
//            return p_205766_0_ == null || p_205766_0_ == Fluids.EMPTY;
//        }, cubePos));
        this(cubePos, upgradeData, null, null, null, levelHeightAccessor);
    }

    //TODO: add TickList<Block> and TickList<Fluid>
    public CubePrimer(CubePos cubePosIn, UpgradeData p_i49941_2_, @Nullable LevelChunkSection[] sectionsIn, ProtoTickList<Block> blockTickListIn, ProtoTickList<Fluid> p_i49941_5_,
                      LevelHeightAccessor levelHeightAccessor) {
        this.heightmaps = Maps.newEnumMap(Heightmap.Types.class);
        this.carvingMasks = new Object2ObjectArrayMap<>();

        this.structureStarts = Maps.newHashMap();
        this.structuresRefences = Maps.newHashMap();

        this.cubePos = cubePosIn;
        this.levelHeightAccessor = levelHeightAccessor;

        //        this.upgradeData = upgradeData;
//        this.pendingBlockTicks = blockTickListIn;
//        this.pendingFluidTicks = fluidTickListIn;
        if (sectionsIn == null) {
            this.sections = new LevelChunkSection[IBigCube.SECTION_COUNT];
        } else {
            if (sectionsIn.length == IBigCube.SECTION_COUNT) {
                this.sections = sectionsIn;
            } else {
                throw new IllegalStateException("Number of Sections must equal BigCube.CUBESIZE");
            }
        }

        this.lightHeightmapLoaded = new boolean[IBigCube.CHUNK_COUNT];
    }

    @Deprecated @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException("This should never be called!");
    }

    @Override public CubePos getCubePos() {
        return this.cubePos;
    }

    @Deprecated @Override public LevelChunkSection[] getSections() {
        throw new UnsupportedOperationException("This should never be called!");
    }

    @Override public LevelChunkSection[] getCubeSections() {
        return this.sections;
    }

    //STATUS
    @Override public void setCubeStatus(ChunkStatus newStatus) {
        this.status = newStatus;
    }

    @Deprecated @Override public ChunkStatus getStatus() {
        return getCubeStatus();
    }

    @Override public ChunkStatus getCubeStatus() {
        return this.status;
    }

    //BLOCK
    @Deprecated @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return setBlock(pos, state, isMoving);
    }

    @Override @Nullable public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        int x = pos.getX() & 0xF;
        int y = pos.getY() & 0xF;
        int z = pos.getZ() & 0xF;
        int index = Coords.blockToIndex(pos.getX(), pos.getY(), pos.getZ());

        if (this.sections[index] == EMPTY_SECTION && state.getBlock() == Blocks.AIR) {
            return state;
        } else {
            if (this.sections[index] == EMPTY_SECTION) {
                this.sections[index] = new LevelChunkSection(Coords.cubeToMinBlock(this.cubePos.getY() + Coords.sectionToMinBlock(Coords.indexToY(index))));
            }

            if (state.getLightEmission() > 0) {
                SectionPos sectionPosAtIndex = Coords.sectionPosByIndex(this.cubePos, index);
                this.lightPositions.add(new BlockPos(
                    x + Coords.sectionToMinBlock(sectionPosAtIndex.getX()),
                    y + Coords.sectionToMinBlock(sectionPosAtIndex.getY()),
                    z + Coords.sectionToMinBlock(sectionPosAtIndex.getZ()))
                );
            }

            LevelChunkSection chunksection = this.sections[index];
            BlockState blockstate = chunksection.setBlockState(x, y, z, state, false);
            if (this.status.isOrAfter(ChunkStatus.FEATURES) && state != blockstate && (state.getLightBlock(this, pos) != blockstate.getLightBlock(this, pos)
                    || state.getLightEmission() != blockstate.getLightEmission() || state.useShapeForLightOcclusion() || blockstate.useShapeForLightOcclusion())) {
                ChunkSource chunkSource;
                if (this.levelHeightAccessor instanceof CubeWorldGenRegion) {
                    chunkSource = ((CubeWorldGenRegion) this.levelHeightAccessor).getChunkSource();
                } else {
                    chunkSource = ((ServerLevel) this.levelHeightAccessor).getChunkSource();
                }

                ChunkPos chunkPos = this.cubePos.asChunkPos();
                for (int dx = 0; dx < IBigCube.DIAMETER_IN_SECTIONS; dx++) {
                    for (int dz = 0; dz < IBigCube.DIAMETER_IN_SECTIONS; dz++) {
                        // TODO this can return null, leading to incorrect light heightmap values -> incorrect sky lighting on worldgen.
                        //      not really feasible to fix until we get a "columns before cubes" invariant.
                        BlockGetter chunk = chunkSource.getChunkForLighting(chunkPos.x + dx, chunkPos.z + dz);
                        if (chunk instanceof LightHeightmapGetter) {
                            LightSurfaceTrackerWrapper lightHeightmap = ((LightHeightmapGetter) chunk).getLightHeightmap();
                            if (lightHeightmap != null) {
                                // TODO this stuff is awful for performance; need to optimize it
                                //      probably want to do the thing we do for other scale0 sections and store a reference to it
                                if (!lightHeightmapLoaded[dx + dz * IBigCube.DIAMETER_IN_SECTIONS]) {
                                    lightHeightmapLoaded[dx + dz * IBigCube.DIAMETER_IN_SECTIONS] = true;
                                    lightHeightmap.loadCube(this);
                                }
                                // Not sure if this is the right blockstate to pass in, but it doesn't actually matter since we don't use it
                                lightHeightmap.update(x, pos.getY(), z, state);
                            }
                        }
                    }
                }

                lightManager.checkBlock(pos);
            }

            EnumSet<Heightmap.Types> heightMapsAfter = this.getStatus().heightmapsAfter();
            EnumSet<Heightmap.Types> toInitialize = null;

            for (Heightmap.Types type : heightMapsAfter) {
                SurfaceTrackerSection[] heightmapArray = this.heightmaps.get(type);

                if (heightmapArray == null) {
                    if (toInitialize == null) {
                        toInitialize = EnumSet.noneOf(Heightmap.Types.class);
                    }

                    toInitialize.add(type);
                }
            }

            if (toInitialize != null) {
                primeHeightMaps(toInitialize);
            }

            for (Heightmap.Types types : heightMapsAfter) {

                int xSection = Coords.blockToCubeLocalSection(pos.getX());
                int zSection = Coords.blockToCubeLocalSection(pos.getZ());

                int idx = xSection + zSection * DIAMETER_IN_SECTIONS;

                SurfaceTrackerSection surfaceTrackerSection = this.heightmaps.get(types)[idx];
                surfaceTrackerSection.markDirty(x, z);
            }

            return blockstate;
        }
    }

    private void primeHeightMaps(EnumSet<Heightmap.Types> toInitialize) {
        for (Heightmap.Types type : toInitialize) {
            SurfaceTrackerSection[] surfaceTrackerSections = new SurfaceTrackerSection[IBigCube.DIAMETER_IN_SECTIONS * IBigCube.DIAMETER_IN_SECTIONS];

            for (int dx = 0; dx < IBigCube.DIAMETER_IN_SECTIONS; dx++) {
                for (int dz = 0; dz < IBigCube.DIAMETER_IN_SECTIONS; dz++) {
                    int idx = dx + dz * IBigCube.DIAMETER_IN_SECTIONS;
                    surfaceTrackerSections[idx] = new SurfaceTrackerSection(0, cubePos.getY(), null, this, type);
                    surfaceTrackerSections[idx].loadCube(dx, dz, this, true);
                }
            }
            this.heightmaps.put(type, surfaceTrackerSections);
        }
    }

    @Override public BlockState getBlockState(int x, int y, int z) {
        int index = Coords.blockToIndex(x, y, z);
        return LevelChunkSection.isEmpty(this.sections[index]) ?
            Blocks.AIR.defaultBlockState() :
            this.sections[index].getBlockState(x & 15, y & 15, z & 15);
    }

    //ENTITY
    @Deprecated @Override public void addEntity(Entity entityIn) {
        this.addCubeEntity(entityIn);
    }

    public void addCubeEntity(Entity entityIn) {
        CompoundTag compoundnbt = new CompoundTag();
        entityIn.save(compoundnbt);
        this.addCubeEntity(compoundnbt);
    }

    public void addCubeEntity(CompoundTag entityCompound) {
        this.entities.add(entityCompound);
    }

    public List<CompoundTag> getCubeEntities() {
        return this.entities;
    }

    //TILE ENTITY

    @Deprecated @Override public void setBlockEntityNbt(CompoundTag nbt) {
        this.setCubeBlockEntity(nbt);
    }

    @Override public void setCubeBlockEntity(CompoundTag nbt) {
        this.deferredTileEntities.put(new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt);
    }

    @Deprecated @Override public void setBlockEntity(BlockEntity tileEntityIn) {
        this.setCubeBlockEntity(tileEntityIn);
    }

    @Override public void setCubeBlockEntity(BlockEntity tileEntityIn) {
        this.tileEntities.put(tileEntityIn.getBlockPos(), tileEntityIn);
    }

    @Deprecated @Override public void removeBlockEntity(BlockPos pos) {
        this.removeCubeBlockEntity(pos);
    }

    @Override public void removeCubeBlockEntity(BlockPos pos) {
        this.tileEntities.remove(pos);
        this.deferredTileEntities.remove(pos);
    }

    @Nullable @Override public BlockEntity getBlockEntity(BlockPos pos) {
        return this.tileEntities.get(pos);
    }

    @Deprecated @Override public Set<BlockPos> getBlockEntitiesPos() {
        return this.getCubeTileEntitiesPos();
    }

    @Override public Set<BlockPos> getCubeTileEntitiesPos() {
        Set<BlockPos> set = Sets.newHashSet(this.deferredTileEntities.keySet());
        set.addAll(this.tileEntities.keySet());
        return set;
    }

    @Deprecated @Nullable @Override public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        return this.getCubeBlockEntityNbtForSaving(pos);
    }

    @Nullable @Override public CompoundTag getCubeBlockEntityNbtForSaving(BlockPos pos) {
        BlockEntity tileEntity = this.getBlockEntity(pos);
        return tileEntity != null ? tileEntity.save(new CompoundTag()) : this.deferredTileEntities.get(pos);
    }

    @Deprecated @Nullable @Override public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return this.getCubeDeferredTileEntity(pos);
    }

    @Nullable @Override public CompoundTag getCubeDeferredTileEntity(BlockPos pos) {
        return this.deferredTileEntities.get(pos);
    }

    public Map<BlockPos, BlockEntity> getTileEntities() {
        return this.getCubeTileEntities();
    }

    public Map<BlockPos, BlockEntity> getCubeTileEntities() {
        return this.tileEntities;
    }

    public Map<BlockPos, CompoundTag> getDeferredTileEntities() {
        return Collections.unmodifiableMap(this.deferredTileEntities);
    }

    //LIGHTING
    @Deprecated @Override public boolean isLightCorrect() {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public boolean hasCubeLight() {
        return this.hasLight;
    }

    @Deprecated @Override public void setLightCorrect(boolean lightCorrectIn) {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public void setCubeLight(boolean lightCorrectIn) {
        this.hasLight = lightCorrectIn;
        this.setDirty(true);
    }

    @Deprecated @Override public Stream<BlockPos> getLights() {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }

    @Override public Stream<BlockPos> getCubeLightSources() {
        return this.lightPositions.stream();
    }

    public void addCubeLightValue(short packedPosition, int lightValue) {
        this.addCubeLightPosition(unpackToWorld(packedPosition, lightValue, this.cubePos));
    }

    public void addCubeLightPosition(BlockPos lightPos) {
        this.lightPositions.add(lightPos.immutable());
    }

    public void setCubeLightManager(LevelLightEngine newLightEngine) {
        this.lightManager = newLightEngine;
    }

    @Nullable private LevelLightEngine getCubeWorldLightManager() {
        return this.lightManager;
    }

    //MISC
    @Deprecated @Override public void setUnsaved(boolean newUnsaved) {
        setDirty(newUnsaved);
    }

    @Override public void setDirty(boolean newUnsaved) {
        this.modified = newUnsaved;
    }

    @Deprecated @Override public boolean isUnsaved() {
        return isDirty();
    }

    @Override public boolean isDirty() {
        return modified;
    }

    @Override public boolean isEmptyCube() {
        for (LevelChunkSection section : this.sections) {
            if (section != EMPTY_SECTION && !section.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override public void setInhabitedTime(long newInhabitedTime) {
        this.setCubeInhabitedTime(newInhabitedTime);
    }

    @Override public void setCubeInhabitedTime(long newInhabitedTime) {
        this.inhabitedTime = newInhabitedTime;
    }

    @Deprecated @Override public long getInhabitedTime() {
        return this.getCubeInhabitedTime();
    }

    @Override public long getCubeInhabitedTime() {
        return this.inhabitedTime;
    }

    @Deprecated public void setBiomes(ChunkBiomeContainer biomes) {
        throw new UnsupportedOperationException("Chunk method called on a cube");
    }

    public void setCubeBiomes(CubeBiomeContainer biomesIn) {
        this.biomes = biomesIn;
    }

    @Deprecated @Nullable @Override public ChunkBiomeContainer getBiomes() {
        throw new UnsupportedOperationException("Chunk method called on a cube");
    }

    @Nullable @Override public CubeBiomeContainer getCubeBiomes() {
        return this.biomes;
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        try {
            int index = Coords.blockToIndex(x, y, z);
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

    @Override public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void setHeightmap(Heightmap.Types type, long[] data) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types typeIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public int getCubeLocalHeight(Heightmap.Types type, int x, int z) {
        SurfaceTrackerSection[] surfaceTrackerSections = this.heightmaps.get(type);
        if (surfaceTrackerSections == null) {
            primeHeightMaps(EnumSet.of(type));
            surfaceTrackerSections = this.heightmaps.get(type);
        }
        int xSection = Coords.blockToCubeLocalSection(x);
        int zSection = Coords.blockToCubeLocalSection(z);

        int idx = xSection + zSection * DIAMETER_IN_SECTIONS;

        SurfaceTrackerSection surfaceTrackerSection = surfaceTrackerSections[idx];
        return surfaceTrackerSection.getHeight(Coords.blockToLocal(x), Coords.blockToLocal(z));
    }

    @Override public int getHeight(Heightmap.Types types, int x, int z) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @org.jetbrains.annotations.Nullable
    public StructureStart<?> getStartForFeature(StructureFeature<?> structureFeature) {
        return this.structureStarts.get(structureFeature);
    }

    @Override
    public void setStartForFeature(StructureFeature<?> structureFeature, StructureStart<?> structureStart) {
        this.structureStarts.put(structureFeature, structureStart);
        this.isDirty = true;
    }

    @Override
    public Map<StructureFeature<?>, StructureStart<?>> getAllCubeStructureStarts() {
        return Collections.unmodifiableMap(this.structureStarts);
    }

    @Override
    public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
        return getAllCubeStructureStarts();
    }

    @Override
    public void setAllStarts(Map<StructureFeature<?>, StructureStart<?>> map) {
        this.structureStarts.clear();
        this.structureStarts.putAll(map);
        this.isDirty = true;
    }

    @Override
    public LongSet getReferencesForFeature(StructureFeature<?> structureFeature) {
        return this.structuresRefences.computeIfAbsent(structureFeature, (structureFeaturex) -> {
            return new LongOpenHashSet();
        });
    }

    public void addReferenceForFeature(StructureFeature<?> structureFeature, long l) {
        ((LongSet) this.structuresRefences.computeIfAbsent(structureFeature, (structureFeaturex) -> {
            return new LongOpenHashSet();
        })).add(l);
        this.isDirty = true;
    }

    public Map<StructureFeature<?>, LongSet> getAllReferences() {
        return Collections.unmodifiableMap(this.structuresRefences);
    }

    @Override public ShortList[] getPostProcessing() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public TickList<Block> getBlockTicks() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public TickList<Fluid> getLiquidTicks() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public UpgradeData getUpgradeData() {
        throw new UnsupportedOperationException("For later implementation");
    }

    public static BlockPos unpackToWorld(short packedPos, int yOffset, CubePos cubePosIn) {
        BlockPos pos = cubePosIn.asBlockPos();
        int xPos = (packedPos & 15) + pos.getX();
        int yPos = (packedPos >>> 4 & 15) + pos.getY();
        int zPos = (packedPos >>> 8 & 15) + pos.getZ();
        return new BlockPos(xPos, yPos, zPos);
    }

    @Override
    public void setAllReferences(Map<StructureFeature<?>, LongSet> map) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(map);
        this.isDirty = true;
    }

    @Nullable
    public BitSet getCarvingMask(GenerationStep.Carving type) {
        return this.carvingMasks.get(type);
    }

    public BitSet getOrSetCarvingMask(GenerationStep.Carving type) {
        return this.carvingMasks.computeIfAbsent(type, (carvingx) -> {
            return new BitSet(IBigCube.BLOCK_COUNT);
        });
    }

    public void setCarvingMask(GenerationStep.Carving type, BitSet mask) {
        this.carvingMasks.put(type, mask);
    }

    @Override
    public void markPosForPostprocessing(BlockPos blockPos) {
        if (System.currentTimeMillis() % 15000 == 0) {
            LogManager.getLogger().warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", blockPos);

        }
    }

    @Override public int getHeight() {
        return levelHeightAccessor.getHeight();
    }

    @Override public int getMinBuildHeight() {
        return levelHeightAccessor.getMinBuildHeight();
    }
}