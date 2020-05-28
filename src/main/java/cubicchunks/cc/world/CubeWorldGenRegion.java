package cubicchunks.cc.world;

import static cubicchunks.cc.utils.Coords.blockToCube;

import cubicchunks.cc.chunk.ICube;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.ITickList;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;

public class CubeWorldGenRegion implements IWorld {

    private static final Logger LOGGER = LogManager.getLogger();
    private final List<ICube> cubePrimers;
    private final int mainCubeX;
    private final int mainCubeY;
    private final int mainCubeZ;
    private final int diameter;
    private final ServerWorld world;
    private final long seed;
    private final int seaLevel;
    private final WorldInfo worldInfo;
    private final Random random;
    private final Dimension dimension;
    private final GenerationSettings chunkGenSettings;
//    private final ITickList<Block> pendingBlockTickList = new WorldGenTickList<>((blockPos) -> {
//        return this.getCube(blockPos).getBlocksToBeTicked();
//    });
//    private final ITickList<Fluid> pendingFluidTickList = new WorldGenTickList<>((blockPos) -> {
//        return this.getCube(blockPos).getFluidsToBeTicked();
//    });
    private final BiomeManager biomeManager;

    public CubeWorldGenRegion(ServerWorld worldIn, List<ICube> sectionsIn) {
        int i = MathHelper.floor(Math.cbrt(sectionsIn.size()));
        if (i * i * i != sectionsIn.size()) {
            throw Util.pauseDevMode(new IllegalStateException("Cache size is not a square."));
        } else {
            SectionPos sectionPos = sectionsIn.get(sectionsIn.size() / 2).getSectionPos();
            this.cubePrimers = sectionsIn;
            this.mainCubeX = sectionPos.getX();
            this.mainCubeY = sectionPos.getY();
            this.mainCubeZ = sectionPos.getZ();
            this.diameter = i;
            this.world = worldIn;
            this.seed = worldIn.getSeed();
            this.chunkGenSettings = worldIn.getChunkProvider().getChunkGenerator().getSettings();
            this.seaLevel = worldIn.getSeaLevel();
            this.worldInfo = worldIn.getWorldInfo();
            this.random = worldIn.getRandom();
            this.dimension = worldIn.getDimension();
            this.biomeManager = new BiomeManager(this, WorldInfo.byHashing(this.seed), this.dimension.getType().getMagnifier());
        }
    }

    public ICube getCube(BlockPos blockPos)
    {
        return this.getCube(blockToCube(blockPos.getX()), blockToCube(blockPos.getY()), blockToCube(blockPos.getZ()), ChunkStatus.FULL,
                true);
    }

    public ICube getCube(int x, int y, int z, ChunkStatus requiredStatus, boolean nonnull)
    {
        ICube icube;
        if (this.cubeExists(x, y, z)) {
            SectionPos sectionPos = this.cubePrimers.get(0).getSectionPos();
            int dx = x - sectionPos.getX();
            int dy = y - sectionPos.getY();
            int dz = z - sectionPos.getZ();
            icube = this.cubePrimers.get(dx*this.diameter*this.diameter + dy*this.diameter + dz);
            if (icube.getCubeStatus().isAtLeast(requiredStatus)) {
                return icube;
            }
        } else {
            icube = null;
        }

        if (!nonnull) {
            return null;
        } else {
            ICube icube1 = this.cubePrimers.get(0);
            ICube icube2 = this.cubePrimers.get(this.cubePrimers.size() - 1);
            LOGGER.error("Requested section : {} {} {}", x, y, z);
            LOGGER.error("Region bounds : {} {} {} | {} {} {}",
                    icube1.getSectionPos().getX(), icube1.getSectionPos().getY(), icube1.getSectionPos().getZ(),
                    icube2.getSectionPos().getX(), icube2.getSectionPos().getY(), icube2.getSectionPos().getZ());
            if (icube != null) {
                throw (RuntimeException)Util.pauseDevMode(new RuntimeException(String.format("Section is not of correct status. Expecting %s, got %s "
                        + "| %s %s %s", requiredStatus, icube.getCubeStatus(), x, y, z)));
            } else {
                throw (RuntimeException)Util.pauseDevMode(new RuntimeException(String.format("We are asking a region for a section out of bound | "
                        + "%s %s %s", x, y, z)));
            }
        }
    }

    public boolean cubeExists(int x, int y, int z)
    {
        ICube isection = this.cubePrimers.get(0);
        ICube isection2 = this.cubePrimers.get(this.cubePrimers.size() - 1);
        return x >= isection.getSectionPos().getX() && x <= isection2.getSectionPos().getX() &&
                y >= isection.getSectionPos().getY() && y <= isection2.getSectionPos().getY() &&
                z >= isection.getSectionPos().getZ() && z <= isection2.getSectionPos().getZ();
    }

    @Override public long getSeed() {
        return this.seed;
    }

    @Override public ITickList<Block> getPendingBlockTicks() {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public ITickList<Fluid> getPendingFluidTicks() {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public World getWorld() {
        return this.world;
    }

    @Override public WorldInfo getWorldInfo() {
        return this.worldInfo;
    }

    @Override public DifficultyInstance getDifficultyForLocation(BlockPos pos) {
        if (!this.cubeExists(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        } else {
            return new DifficultyInstance(this.world.getDifficulty(), this.world.getDayTime(), 0L, this.world.getCurrentMoonPhaseFactor());
        }
    }

    @Override public AbstractChunkProvider getChunkProvider() {
        return world.getChunkProvider();
    }

    @Override public Random getRandom() {
        return this.random;
    }

    @Override public void notifyNeighbors(BlockPos pos, Block blockIn) {

    }

    @Override public BlockPos getSpawnPoint() {
        return world.getSpawnPoint();
    }

    @Override
    public void playSound(@Nullable PlayerEntity player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public void addParticle(IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public void playEvent(@Nullable PlayerEntity player, int type, BlockPos pos, int data) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public WorldBorder getWorldBorder() {
        return world.getWorldBorder();
    }

    @Nullable @Override public TileEntity getTileEntity(BlockPos pos) {
        ICube icube = this.getCube(pos);
        TileEntity tileentity = icube.getTileEntity(pos);
        if (tileentity != null) {
            return tileentity;
        } else {
            CompoundNBT compoundnbt = null;// = icube.getDeferredTileEntity(pos);
            if (compoundnbt != null) {
                if ("DUMMY".equals(compoundnbt.getString("id"))) {
                    BlockState state = this.getBlockState(pos);
                    if (!state.hasTileEntity()) {
                        return null;
                    }

                    tileentity = state.createTileEntity(this.world);
                } else {
                    tileentity = TileEntity.create(compoundnbt);
                }

                if (tileentity != null) {
                    icube.addTileEntity(pos, tileentity);
                    return tileentity;
                }
            }

            if (icube.getBlockState(pos).hasTileEntity()) {
                LOGGER.warn("Tried to access a block entity before it was created. {}", (Object)pos);
            }

            return null;
        }
    }

    @Override public BlockState getBlockState(BlockPos pos) {
        return this.getCube(pos).getBlockState(pos);
    }

    @Override public IFluidState getFluidState(BlockPos pos) {
        return this.getCube(pos).getFluidState(pos);
    }

    @Override public List<Entity> getEntitiesInAABBexcluding(@Nullable Entity entityIn, AxisAlignedBB boundingBox,
            @Nullable Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> clazz, AxisAlignedBB aabb, @Nullable Predicate<? super T> filter) {
        return Collections.emptyList();
    }

    @Override public List<? extends PlayerEntity> getPlayers() {
        return world.getPlayers();
    }

    @Deprecated
    @Nullable @Override public IChunk getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {
        throw new UnsupportedOperationException("This should never be called!");
    }

    @Override public int getHeight(Heightmap.Type heightmapType, int x, int z) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public int getSkylightSubtracted() {
        return 0;
    }

    @Override public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override public Biome getNoiseBiomeRaw(int x, int y, int z) {
        return this.world.getNoiseBiomeRaw(x, y, z);
    }

    @Override public boolean isRemote() {
        return false;
    }

    @Override public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override public Dimension getDimension() {
        return world.dimension;
    }

    @Override public WorldLightManager getLightManager() {
        return world.getLightManager();
    }

    @Override public boolean setBlockState(BlockPos pos, BlockState newState, int flags) {
        ICube icube = this.getCube(pos);
        BlockState blockstate = icube.setBlockState(pos, newState, false);
        if (blockstate != null) {
            this.world.onBlockStateChange(pos, blockstate, newState);
        }
        if (newState.hasTileEntity()) {
            if (icube.getCubeStatus().getType() == ChunkStatus.Type.LEVELCHUNK) {
                icube.addTileEntity(pos, newState.createTileEntity(this));
            } else {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putInt("x", pos.getX());
                compoundnbt.putInt("y", pos.getY());
                compoundnbt.putInt("z", pos.getZ());
                compoundnbt.putString("id", "DUMMY");
                //icube.addTileEntity(compoundnbt);
            }
        } else if (blockstate != null && blockstate.hasTileEntity()) {
            icube.removeTileEntity(pos);
        }

        if (newState.blockNeedsPostProcessing(this, pos)) {
            //TODO: reimplement
            //this.markBlockForPostprocessing(pos);
        }

        return true;
    }

    @Override public boolean removeBlock(BlockPos pos, boolean isMoving) {
        return this.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
    }

    @Override public boolean destroyBlock(BlockPos pos, boolean isPlayerInCreative, @Nullable Entity droppedEntities) {
        BlockState blockstate = this.getBlockState(pos);
        if (blockstate.isAir(this, pos)) {
            return false;
        } else {
            if (isPlayerInCreative) {
                TileEntity tileentity = blockstate.hasTileEntity() ? this.getTileEntity(pos) : null;
                Block.spawnDrops(blockstate, this.world, pos, tileentity, droppedEntities, ItemStack.EMPTY);
            }

            return this.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }

    @Override public boolean hasBlockState(BlockPos pos, Predicate<BlockState> blockstate) {
        return blockstate.test(this.getBlockState(pos));
    }
}
