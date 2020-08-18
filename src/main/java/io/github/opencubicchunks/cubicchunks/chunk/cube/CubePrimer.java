package io.github.opencubicchunks.cubicchunks.chunk.cube;

import static net.minecraft.world.chunk.Chunk.EMPTY_SECTION;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubicHeightmap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.lighting.WorldLightManager;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public class CubePrimer implements IBigCube, IChunk {

    private final CubePos cubePos;
    private final ChunkSection[] sections;
    private final IChunk[] chunks;
    private ChunkStatus status = ChunkStatus.EMPTY;

    @Nullable
    private CubeBiomeContainer biomes;

    private final List<CompoundNBT> entities = Lists.newArrayList();
    private final Map<BlockPos, TileEntity> tileEntities = Maps.newHashMap();
    private final Map<BlockPos, CompoundNBT> deferredTileEntities = Maps.newHashMap();
    private volatile boolean modified = true;

    private final List<BlockPos> lightPositions = Lists.newArrayList();
    private volatile boolean hasLight;
    private WorldLightManager lightManager;

    private long inhabitedTime;

    //TODO: add TickList<Block> and TickList<Fluid>
    public CubePrimer(CubePos cubePosIn, World world, UpgradeData p_i49941_2_, @Nullable ChunkSection[] sectionsIn, ChunkPrimerTickList<Block> blockTickListIn, ChunkPrimerTickList<Fluid> p_i49941_5_) {
        this.cubePos = cubePosIn;
//        this.upgradeData = p_i49941_2_;
//        this.pendingBlockTicks = blockTickListIn;
//        this.pendingFluidTicks = p_i49941_5_;

        this.chunks = new IChunk[IBigCube.CHUNK_COUNT];
        for (int x = 0; x < DIAMETER_IN_SECTIONS; ++x) {
            for (int z = 0; z < DIAMETER_IN_SECTIONS; ++z) {
                this.chunks[x << 1 | z] = world.getChunk(cubePos.getX() + x, cubePos.getZ() + z, ChunkStatus.EMPTY, true);
            }
        }

        if(sectionsIn == null) {
            this.sections = new ChunkSection[IBigCube.SECTION_COUNT];
            for(int i = 0; i < IBigCube.SECTION_COUNT; i++) {
                this.sections[i] = new ChunkSection(cubePos.getY(), (short) 0, (short) 0, (short) 0);
            }
        }
        else {
            if(sectionsIn.length == IBigCube.SECTION_COUNT)
                this.sections = sectionsIn;
            else
            {
                throw new IllegalStateException("Number of Sections must equal BigCube.CUBESIZE");
            }
        }
    }

    @Deprecated @Override public ChunkPos getPos() { throw new UnsupportedOperationException("This should never be called!"); }
    @Override public CubePos getCubePos() {
        return this.cubePos;
    }

    @Deprecated @Override public ChunkSection[] getSections() { throw new UnsupportedOperationException("This should never be called!"); }
    @Override public ChunkSection[] getCubeSections() {
        return this.sections;
    }

    //STATUS
    @Override public void setCubeStatus(ChunkStatus status)
    {
        this.status = status;
    }
    @Deprecated @Override public ChunkStatus getStatus() { return getCubeStatus(); }
    @Override public ChunkStatus getCubeStatus() {
        return this.status;
    }

    //BLOCK
    @Deprecated @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) { return setBlock(pos, state, isMoving); }
    @Override @Nullable public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        int x = pos.getX() & 0xF;
        int y = pos.getY() & 0xF;
        int z = pos.getZ() & 0xF;
        int index = Coords.blockToIndex(pos.getX(), pos.getY(), pos.getZ());

        if (this.sections[index] == Chunk.EMPTY_SECTION && state.getBlock() == Blocks.AIR) {
            return state;
        } else {
            if(this.sections[index] == Chunk.EMPTY_SECTION) {
                this.sections[index] = new ChunkSection(Coords.cubeToMinBlock(this.cubePos.getY() + Coords.sectionToMinBlock(Coords.indexToY(index))));
            }

            if (state.getLightValue(this, pos) > 0) {
                SectionPos sectionPosAtIndex = Coords.sectionPosByIndex(this.cubePos, index);
                this.lightPositions.add(new BlockPos(
                        x + sectionPosAtIndex.getX(),
                        y + sectionPosAtIndex.getY(),
                        z + sectionPosAtIndex.getZ())
                );
            }

            ChunkSection chunksection = this.sections[index];
            BlockState blockstate = chunksection.setBlockState(x, y, z, state);
            if (this.status.isAtLeast(ChunkStatus.FEATURES) && state != blockstate && (state.getOpacity(this, pos) != blockstate.getOpacity(this, pos) || state.getLightValue(this, pos) != blockstate.getLightValue(this, pos) || state.isTransparent() || blockstate.isTransparent())) {
                lightManager.checkBlock(pos);
            }


            EnumSet<Heightmap.Type> enumset1 = this.getStatus().getHeightMaps();
            EnumSet<Heightmap.Type> enumset = null;

            int chunkX = Coords.blockToSection(x);
            int chunkZ = Coords.blockToSection(z);
            IChunk chunk = chunks[Coords.blockToChunkIndex(chunkX, chunkZ)];

            for(Heightmap.Type heightmap$type : enumset1) {
                Heightmap heightmap = chunk.getHeightmap(heightmap$type);
                if (heightmap == null) {
                    if (enumset == null) {
                        enumset = EnumSet.noneOf(Heightmap.Type.class);
                    }

                    enumset.add(heightmap$type);
                }
            }

            if (enumset != null) {
                CubicHeightmap.updateChunkHeightmaps(chunk, enumset, Coords.cubeToMinBlock(cubePos.getY()), Coords.cubeToMaxBlock(cubePos.getY()));
            }

            for(Heightmap.Type heightmap$type1 : enumset1) {
                chunk.getHeightmap(heightmap$type1).update(x, y, z, state);
            }

            return blockstate;
        }
    }

    @Override public BlockState getBlockState(int x, int y, int z) {
        int index = Coords.blockToIndex(x, y, z);
        return ChunkSection.isEmpty(this.sections[index]) ?
                Blocks.AIR.getDefaultState() :
                this.sections[index].getBlockState(x & 15, y & 15, z & 15);
    }

    //ENTITY
    @Deprecated @Override public void addEntity(Entity entityIn) { this.addCubeEntity(entityIn); }
    @Override public void addCubeEntity(Entity entityIn) {
        CompoundNBT compoundnbt = new CompoundNBT();
        entityIn.writeUnlessPassenger(compoundnbt);
        this.addCubeEntity(compoundnbt);
    }
    public void addCubeEntity(CompoundNBT entityCompound) {
        this.entities.add(entityCompound);
    }

    public List<CompoundNBT> getCubeEntities() {
        return this.entities;
    }

    //TILE ENTITY

    @Deprecated @Override public void addTileEntity(CompoundNBT nbt) { this.addCubeTileEntity(nbt); }
    @Override public void addCubeTileEntity(CompoundNBT nbt) {
        this.deferredTileEntities.put(new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt);
    }

    @Deprecated @Override public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) { this.addCubeTileEntity(pos, tileEntityIn); }
    @Override public void addCubeTileEntity(BlockPos pos, TileEntity tileEntityIn) {
        tileEntityIn.setPos(pos);
        this.tileEntities.put(pos, tileEntityIn);
    }

    @Deprecated @Override public void removeTileEntity(BlockPos pos) { this.removeCubeTileEntity(pos); }
    @Override public void removeCubeTileEntity(BlockPos pos) {
        this.tileEntities.remove(pos);
        this.deferredTileEntities.remove(pos);
    }

    @Nullable @Override public TileEntity getTileEntity(BlockPos pos) {
        return this.tileEntities.get(pos);
    }

    @Deprecated @Override public Set<BlockPos> getTileEntitiesPos() { return this.getCubeTileEntitiesPos(); }
    @Override public Set<BlockPos> getCubeTileEntitiesPos() {
        Set<BlockPos> set = Sets.newHashSet(this.deferredTileEntities.keySet());
        set.addAll(this.tileEntities.keySet());
        return set;
    }

    @Deprecated @Nullable @Override public CompoundNBT getTileEntityNBT(BlockPos pos) { return this.getCubeTileEntityNBT(pos); }
    @Nullable @Override public CompoundNBT getCubeTileEntityNBT(BlockPos pos) {
        TileEntity tileEntity = this.getTileEntity(pos);
        return tileEntity != null ? tileEntity.write(new CompoundNBT()) : this.deferredTileEntities.get(pos);
    }

    @Deprecated @Nullable @Override public CompoundNBT getDeferredTileEntity(BlockPos pos) { return this.getCubeDeferredTileEntity(pos); }
    @Nullable @Override public CompoundNBT getCubeDeferredTileEntity(BlockPos pos) {
        return this.deferredTileEntities.get(pos);
    }

    public Map<BlockPos, TileEntity> getTileEntities() {
        return this.getCubeTileEntities();
    }
    public Map<BlockPos, TileEntity> getCubeTileEntities() {
        return this.tileEntities;
    }

    public Map<BlockPos, CompoundNBT> getDeferredTileEntities() {
        return Collections.unmodifiableMap(this.deferredTileEntities);
    }

    //LIGHTING
    @Deprecated @Override public boolean hasLight() { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public boolean hasCubeLight() {
        return this.hasLight;
    }

    @Deprecated @Override public void setLight(boolean lightCorrectIn) { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public void setCubeLight(boolean lightCorrectIn) {
        this.hasLight = lightCorrectIn;
        this.setDirty(true);
    }

    @Deprecated @Override public Stream<BlockPos> getLightSources() {
        throw new UnsupportedOperationException("Chunk method called on a cube!");
    }
    @Override public Stream<BlockPos> getCubeLightSources() {
        return this.lightPositions.stream();
    }

    public void addCubeLightValue(short packedPosition, int lightValue) {
        this.addCubeLightPosition(unpackToWorld(packedPosition, lightValue, this.cubePos));
    }

    public void addCubeLightPosition(BlockPos lightPos) {
        this.lightPositions.add(lightPos.toImmutable());
    }

    public void setCubeLightManager(WorldLightManager lightManager) {
        this.lightManager = lightManager;
    }
    @Nullable private WorldLightManager getCubeWorldLightManager() {
        return this.lightManager;
    }

    //MISC
    @Deprecated @Override public void setModified(boolean modified) { setDirty(modified); }
    @Override public void setDirty(boolean modified) {
        this.modified = modified;
    }

    @Deprecated @Override public boolean isModified() { return isDirty(); }
    @Override public boolean isDirty() {
        return modified;
    }

    @Override public boolean isEmptyCube() {
        for(ChunkSection section : this.sections) {
            if(section != EMPTY_SECTION && !section.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override public void setInhabitedTime(long newInhabitedTime) { this.setCubeInhabitedTime(newInhabitedTime); }
    @Override public void setCubeInhabitedTime(long newInhabitedTime) {
        this.inhabitedTime = newInhabitedTime;
    }

    @Deprecated @Override public long getInhabitedTime() { return this.getCubeInhabitedTime(); }
    @Override public long getCubeInhabitedTime() {
        return this.inhabitedTime;
    }

    @Deprecated public void setBiomes(BiomeContainer biomes) { this.setCubeBiomes((CubeBiomeContainer) biomes); }
    public void setCubeBiomes(CubeBiomeContainer biomesIn) {
        this.biomes = biomesIn;
    }

    @Deprecated @Nullable @Override public BiomeContainer getBiomes() { return this.getCubeBiomes(); }
    @Nullable @Override public CubeBiomeContainer getCubeBiomes() {
        return this.biomes;
    }

    @Override public IFluidState getFluidState(BlockPos pos) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override public Collection<Map.Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void setHeightmap(Heightmap.Type type, long[] data) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public Heightmap getHeightmap(Heightmap.Type typeIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public int getTopBlockY(Heightmap.Type heightmapType, int x, int z) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void setLastSaveTime(long saveTime) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated @Override public Map<String, StructureStart> getStructureStarts() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated @Override public void setStructureStarts(Map<String, StructureStart> structureStartsIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public ShortList[] getPackedPositions() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public ITickList<Block> getBlocksToBeTicked() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public ITickList<Fluid> getFluidsToBeTicked() {
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

    @Nullable @Override public StructureStart getStructureStart(String stucture) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void putStructureStart(String structureIn, StructureStart structureStartIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public LongSet getStructureReferences(String structureIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void addStructureReference(String structure, long reference) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public Map<String, LongSet> getStructureReferences() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void setStructureReferences(Map<String, LongSet> p_201606_1_) {
        throw new UnsupportedOperationException("For later implementation");
    }

    public BitSet getCarvingMask(GenerationStage.Carving type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setCarvingMask(GenerationStage.Carving type, BitSet mask) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
