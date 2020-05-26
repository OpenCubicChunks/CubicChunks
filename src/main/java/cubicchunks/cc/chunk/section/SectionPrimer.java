package cubicchunks.cc.chunk.section;

import cubicchunks.cc.chunk.ISection;
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
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.lighting.WorldLightManager;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class SectionPrimer implements ISection, IChunk {

    private final SectionPos sectionPos;
    private final ChunkSection section;
    private ChunkStatus status = ChunkStatus.EMPTY;


    //TODO: add TickList<Block> and TickList<Fluid>
    public SectionPrimer(SectionPos pos, @Nullable ChunkSection sectionIn)
    {
        this.sectionPos = pos;
        if(sectionIn == null) {
            this.section = new ChunkSection(pos.getY(), (short)0, (short)0, (short)0);
        }
        else {
            this.section = sectionIn;
        }
    }

    public ChunkSection getChunkSection() {
        return this.section;
    }

    @Nullable @Override public TileEntity getTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException("For later implementation");
    }

    public BlockState getBlockState(BlockPos pos) {
        return ChunkSection.isEmpty(this.section) ?
                Blocks.AIR.getDefaultState() :
                this.section.getBlockState(pos.getX() & 15, pos.getX() & 15, pos.getZ() & 15);
    }

    @Override public IFluidState getFluidState(BlockPos pos) {
        return null;
    }

    @Nullable
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.section == Chunk.EMPTY_SECTION && state.getBlock() == Blocks.AIR) {
            return state;
        } else {
            return this.section.setBlockState(x, y, z, state);

            //TODO: finish implementing
            /*if (state.getLightValue(this, pos) > 0) {
                this.lightPositions.add(new BlockPos((x & 15) + this.getPos().getXStart(), y, (z & 15) + this.getPos().getZStart()));
            }

            ChunkSection chunksection = this.getSection(y >> 4);
            BlockState blockstate = chunksection.setBlockState(x & 15, y & 15, z & 15, state);
            if (this.status.isAtLeast(ChunkStatus.FEATURES) && state != blockstate && (state.getOpacity(this, pos) != blockstate.getOpacity(this, pos) || state.getLightValue(this, pos) != blockstate.getLightValue(this, pos) || state.isTransparent() || blockstate.isTransparent())) {
                WorldLightManager worldlightmanager = this.getWorldLightManager();
                worldlightmanager.checkBlock(pos);
            }

            EnumSet<Heightmap.Type> enumset1 = this.getStatus().getHeightMaps();
            EnumSet<Heightmap.Type> enumset = null;

            for(Heightmap.Type heightmap$type : enumset1) {
                Heightmap heightmap = this.heightmaps.get(heightmap$type);
                if (heightmap == null) {
                    if (enumset == null) {
                        enumset = EnumSet.noneOf(Heightmap.Type.class);
                    }

                    enumset.add(heightmap$type);
                }
            }

            if (enumset != null) {
                Heightmap.updateChunkHeightmaps(this, enumset);
            }

            for(Heightmap.Type heightmap$type1 : enumset1) {
                this.heightmaps.get(heightmap$type1).update(x & 15, y, z & 15, state);
            }

            return blockstate;*/
        }
    }

    @Deprecated
    @Override public void addTileEntity(BlockPos pos, TileEntity tileEntityIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public void addEntity(Entity entityIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public Set<BlockPos> getTileEntitiesPos() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public ChunkSection[] getSections() {
        throw new UnsupportedOperationException("How even?");
    }

    @Deprecated
    @Override public Collection<Map.Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public void setHeightmap(Heightmap.Type type, long[] data) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public Heightmap getHeightmap(Heightmap.Type typeIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public int getTopBlockY(Heightmap.Type heightmapType, int x, int z) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public ChunkPos getPos() {
        throw new UnsupportedOperationException("This should never be called!");
    }

    @Deprecated
    @Override public void setLastSaveTime(long saveTime) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public Map<String, StructureStart> getStructureStarts() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Override public void setStructureStarts(Map<String, StructureStart> structureStartsIn) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Deprecated
    @Nullable @Override public BiomeContainer getBiomes() {
        throw new UnsupportedOperationException("For later implementation");
    }


    @Override public void setModified(boolean modified) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public boolean isModified() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public ChunkStatus getStatus() {
        return getSectionStatus();
    }

    @Override public ChunkStatus getSectionStatus() {
        return this.status;
    }

    public void setStatus(ChunkStatus status) {
        this.status = status;
    }

    @Override public void removeTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public ShortList[] getPackedPositions() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Nullable @Override public CompoundNBT getDeferredTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Nullable @Override public CompoundNBT getTileEntityNBT(BlockPos pos) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public Stream<BlockPos> getLightSources() {
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

    @Override public void setInhabitedTime(long newInhabitedTime) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public long getInhabitedTime() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public boolean hasLight() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void setLight(boolean lightCorrectIn) {
        throw new UnsupportedOperationException("For later implementation");
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

    @Override public void addStructureReference(String strucutre, long reference) {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public Map<String, LongSet> getStructureReferences() {
        throw new UnsupportedOperationException("For later implementation");
    }

    @Override public void setStructureReferences(Map<String, LongSet> p_201606_1_) {
        throw new UnsupportedOperationException("For later implementation");
    }
}
