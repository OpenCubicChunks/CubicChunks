package io.github.opencubicchunks.cubicchunks.world;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.blockToCube;

public class CubeWorldGenRegion implements ISeedReader {

    private static final Logger LOGGER = LogManager.getLogger();
    private final List<IBigCube> cubePrimers;
    private final int mainCubeX;
    private final int mainCubeY;
    private final int mainCubeZ;
    private final int diameter;
    private final ServerWorld world;
    private final long seed;
    private final int seaLevel;
    private final IWorldInfo worldInfo;
    private final Random random;
    private final DimensionType dimension;
    //    private final ITickList<Block> pendingBlockTickList = new WorldGenTickList<>((blockPos) -> {
    //        return this.getCube(blockPos).getBlocksToBeTicked();
    //    });
    //    private final ITickList<Fluid> pendingFluidTickList = new WorldGenTickList<>((blockPos) -> {
    //        return this.getCube(blockPos).getFluidsToBeTicked();
    //    });
    private final BiomeManager biomeManager;

    public CubeWorldGenRegion(ServerWorld worldIn, List<IBigCube> cubesIn) {
        int i = MathHelper.floor(Math.cbrt(cubesIn.size()));
        if (i * i * i != cubesIn.size()) {
            throw Util.pauseInIde(new IllegalStateException("Cache size is not a square."));
        } else {
            CubePos cubePos = cubesIn.get(cubesIn.size() / 2).getCubePos();
            this.cubePrimers = cubesIn;
            this.mainCubeX = cubePos.getX();
            this.mainCubeY = cubePos.getY();
            this.mainCubeZ = cubePos.getZ();
            this.diameter = i;
            this.world = worldIn;
            this.seed = worldIn.getSeed();
            this.seaLevel = worldIn.getSeaLevel();
            this.worldInfo = worldIn.getLevelData();
            this.random = worldIn.getRandom();
            this.dimension = worldIn.dimensionType();
            this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed), worldIn.dimensionType().getBiomeZoomer());
        }
    }

    public int getMainCubeX() {
        return this.mainCubeX;
    }

    public int getMainCubeY() {
        return this.mainCubeY;
    }

    public int getMainCubeZ() {
        return this.mainCubeZ;
    }

    public IBigCube getCube(BlockPos blockPos) {
        return this.getCube(blockToCube(blockPos.getX()), blockToCube(blockPos.getY()), blockToCube(blockPos.getZ()), ChunkStatus.EMPTY,
                true);
    }

    public IBigCube getCube(int x, int y, int z, ChunkStatus requiredStatus, boolean nonnull) {
        IBigCube icube;
        if (this.cubeExists(x, y, z)) {
            CubePos cubePos = this.cubePrimers.get(0).getCubePos();
            int dx = x - cubePos.getX();
            int dy = y - cubePos.getY();
            int dz = z - cubePos.getZ();
            icube = this.cubePrimers.get(dx * this.diameter * this.diameter + dy * this.diameter + dz);
            if (icube.getCubeStatus().isOrAfter(requiredStatus)) {
                return icube;
            }
        } else {
            icube = null;
        }

        if (!nonnull) {
            return null;
        } else {
            IBigCube icube1 = this.cubePrimers.get(0);
            IBigCube icube2 = this.cubePrimers.get(this.cubePrimers.size() - 1);
            LOGGER.error("Requested section : {} {} {}", x, y, z);
            LOGGER.error("Region bounds : {} {} {} | {} {} {}",
                    icube1.getCubePos().getX(), icube1.getCubePos().getY(), icube1.getCubePos().getZ(),
                    icube2.getCubePos().getX(), icube2.getCubePos().getY(), icube2.getCubePos().getZ());
            if (icube != null) {
                throw Util.pauseInIde(new RuntimeException(String.format("Section is not of correct status. Expecting %s, got %s "
                        + "| %s %s %s", requiredStatus, icube.getCubeStatus(), x, y, z)));
            } else {
                throw Util.pauseInIde(new RuntimeException(String.format("We are asking a region for a section out of bound | "
                        + "%s %s %s", x, y, z)));
            }
        }
    }

    public boolean cubeExists(int x, int y, int z) {
        IBigCube isection = this.cubePrimers.get(0);
        IBigCube isection2 = this.cubePrimers.get(this.cubePrimers.size() - 1);
        return x >= isection.getCubePos().getX() && x <= isection2.getCubePos().getX() &&
                y >= isection.getCubePos().getY() && y <= isection2.getCubePos().getY() &&
                z >= isection.getCubePos().getZ() && z <= isection2.getCubePos().getZ();
    }

    @Override public long getSeed() {
        return this.seed;
    }

    @Override public ITickList<Block> getBlockTicks() {
        return new EmptyTickList<>();
    }

    @Override public ITickList<Fluid> getLiquidTicks() {
        return new EmptyTickList<>();
    }

    @Override public ServerWorld getLevel() {
        return this.world;
    }

    @Override public IWorldInfo getLevelData() {
        return this.worldInfo;
    }

    @Override public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        if (!this.cubeExists(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        } else {
            return new DifficultyInstance(this.world.getDifficulty(), this.world.getDayTime(), 0L, this.world.getMoonBrightness());
        }
    }

    @Override public AbstractChunkProvider getChunkSource() {
        return world.getChunkSource();
    }

    @Override public Random getRandom() {
        return this.random;
    }

    @Override
    public void playSound(@Nullable PlayerEntity player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public void addParticle(IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public void levelEvent(@Nullable PlayerEntity player, int type, BlockPos pos, int data) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public WorldBorder getWorldBorder() {
        return world.getWorldBorder();
    }

    @Nullable @Override public TileEntity getBlockEntity(BlockPos pos) {
        IBigCube icube = this.getCube(pos);
        TileEntity tileentity = icube.getBlockEntity(pos);
        if (tileentity != null) {
            return tileentity;
        } else {
            CompoundNBT compoundnbt = null;// = icube.getDeferredTileEntity(pos);
            BlockState state = this.getBlockState(pos);
            if (compoundnbt != null) {
                if ("DUMMY".equals(compoundnbt.getString("id"))) {
                    if (!state.hasTileEntity()) {
                        return null;
                    }
                    tileentity = state.createTileEntity(this.world);
                } else {
                    tileentity = TileEntity.loadStatic(state, compoundnbt);
                }

                if (tileentity != null) {
                    icube.addCubeTileEntity(pos, tileentity);
                    return tileentity;
                }
            }

            if (icube.getBlockState(pos).hasTileEntity()) {
                LOGGER.warn("Tried to access a block entity before it was created. {}", (Object) pos);
            }

            return null;
        }
    }

    @Override public BlockState getBlockState(BlockPos pos) {
        return this.getCube(pos).getBlockState(pos);
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        return this.getCube(pos).getFluidState(pos);
    }

    @Override public List<Entity> getEntities(@Nullable Entity entityIn, AxisAlignedBB boundingBox,
            @Nullable Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<? extends T> clazz, AxisAlignedBB aabb, @Nullable Predicate<? super T> filter) {
        return Collections.emptyList();
    }

    @Override public List<? extends PlayerEntity> players() {
        return world.players();
    }

    @Deprecated
    @Nullable @Override public IChunk getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {
        throw new UnsupportedOperationException("This should never be called!");
    }

    @Override public int getHeight(Heightmap.Type heightmapType, int x, int z) {
        int yStart = Coords.cubeToMinBlock(mainCubeY + 1);
        int yEnd = Coords.cubeToMinBlock(mainCubeY);
        BlockPos pos = new BlockPos(x, yStart, z);

        if (heightmapType.isOpaque().test(getBlockState(pos))) {
            return yStart + 2;
        }
        for (int y = yStart - 1; y >= yEnd; y--) {
            pos = new BlockPos(x, y, z);
            if (heightmapType.isOpaque().test(getBlockState(pos))) {
                return y + 1;
            }
        }
        return yEnd - 1;
    }

    @Override public int getSkyDarken() {
        return 0;
    }

    @Override public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override public Biome getUncachedNoiseBiome(int x, int y, int z) {
        return this.world.getUncachedNoiseBiome(x, y, z);
    }

    @Override public boolean isClientSide() {
        return false;
    }

    @Override public int getSeaLevel() {
        return this.seaLevel;
    }

    @Override public DimensionType dimensionType() {
        return this.dimension;
    }

    @Override public float getShade(Direction direction, boolean b) {
        return 1f;
    }

    @Override public WorldLightManager getLightEngine() {
        return world.getLightEngine();
    }

    // setBlockState
    @Override public boolean setBlock(BlockPos pos, BlockState newState, int flags, int recursionLimit) {
        IBigCube icube = this.getCube(pos);
        BlockState blockstate = icube.setBlock(pos, newState, false);
        if (blockstate != null) {
            this.world.onBlockStateChange(pos, blockstate, newState);
        }
        if (newState.hasTileEntity()) {
            if (icube.getCubeStatus().getChunkType() == ChunkStatus.Type.LEVELCHUNK) {
                icube.addCubeTileEntity(pos, newState.createTileEntity(this));
            } else {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putInt("x", pos.getX());
                compoundnbt.putInt("y", pos.getY());
                compoundnbt.putInt("z", pos.getZ());
                compoundnbt.putString("id", "DUMMY");
                //icube.addTileEntity(compoundnbt);
            }
        } else if (blockstate != null && blockstate.hasTileEntity()) {
            icube.removeCubeTileEntity(pos);
        }

        if (newState.hasPostProcess(this, pos)) {
            //TODO: reimplement postprocessing
            //this.markBlockForPostprocessing(pos);
        }

        return true;
    }

    @Override public boolean removeBlock(BlockPos pos, boolean isMoving) {
        return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    // destroyBlock
    @Override public boolean destroyBlock(BlockPos pos, boolean isPlayerInCreative, @Nullable Entity droppedEntities, int recursionLimit) {
        BlockState blockstate = this.getBlockState(pos);
        if (blockstate.isAir(this, pos)) {
            return false;
        } else {
            if (isPlayerInCreative) {
                TileEntity tileentity = blockstate.hasTileEntity() ? this.getBlockEntity(pos) : null;
                Block.dropResources(blockstate, this.world, pos, tileentity, droppedEntities, ItemStack.EMPTY);
            }

            return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3, recursionLimit);
        }
    }

    @Override public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> blockstate) {
        return blockstate.test(this.getBlockState(pos));
    }

    //TODO: DOUBLE CHECK THESE

    @Override
    public DynamicRegistries registryAccess() {
        return this.world.registryAccess();
    }

    @Override
    public Stream<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos, Structure<?> structure) {
        return this.world.startsForFeature(sectionPos, structure);
    }
}