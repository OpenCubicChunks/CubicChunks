package io.github.opencubicchunks.cubicchunks.world;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyTickList;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.blockToCube;

public class CubeWorldGenRegion implements WorldGenLevel {

    private static final Logger LOGGER = LogManager.getLogger();
    private final List<IBigCube> cubePrimers;
    private final int mainCubeX;
    private final int mainCubeY;
    private final int mainCubeZ;
    private final int diameter;
    private final ServerLevel level;
    private final long seed;
    private final int seaLevel;
    private final LevelData worldInfo;
    private final Random random;
    private final DimensionType dimension;
    //    private final ITickList<Block> pendingBlockTickList = new WorldGenTickList<>((blockPos) -> {
    //        return this.getCube(blockPos).getBlocksToBeTicked();
    //    });
    //    private final ITickList<Fluid> pendingFluidTickList = new WorldGenTickList<>((blockPos) -> {
    //        return this.getCube(blockPos).getFluidsToBeTicked();
    //    });
    private final BiomeManager biomeManager;

    public CubeWorldGenRegion(ServerLevel worldIn, List<IBigCube> cubesIn) {
        int i = Mth.floor(Math.cbrt(cubesIn.size()));
        if (i * i * i != cubesIn.size()) {
            throw Util.pauseInIde(new IllegalStateException("Cache size is not a square."));
        } else {
            CubePos cubePos = cubesIn.get(cubesIn.size() / 2).getCubePos();
            this.cubePrimers = cubesIn;
            this.mainCubeX = cubePos.getX();
            this.mainCubeY = cubePos.getY();
            this.mainCubeZ = cubePos.getZ();
            this.diameter = i;
            this.level = worldIn;
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

    @Override public TickList<Block> getBlockTicks() {
        return new EmptyTickList<>();
    }

    @Override public TickList<Fluid> getLiquidTicks() {
        return new EmptyTickList<>();
    }

    @Override public ServerLevel getLevel() {
        return this.level;
    }

    @Override public LevelData getLevelData() {
        return this.worldInfo;
    }

    @Override public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        if (!this.cubeExists(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        } else {
            return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(), 0L, this.level.getMoonBrightness());
        }
    }

    @Override public ChunkSource getChunkSource() {
        return level.getChunkSource();
    }

    @Override public Random getRandom() {
        return this.random;
    }

    @Override
    public void playSound(@Nullable Player player, BlockPos pos, SoundEvent soundIn, SoundSource category, float volume, float pitch) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override public WorldBorder getWorldBorder() {
        return level.getWorldBorder();
    }

    @Nullable @Override public BlockEntity getBlockEntity(BlockPos pos) {
        IBigCube icube = this.getCube(pos);
        BlockEntity tileentity = icube.getBlockEntity(pos);
        if (tileentity != null) {
            return tileentity;
        } else {
            CompoundTag compoundnbt = null;// = icube.getDeferredTileEntity(pos);
            BlockState state = this.getBlockState(pos);
            if (compoundnbt != null) {
                if ("DUMMY".equals(compoundnbt.getString("id"))) {
                    if (!state.hasBlockEntity()) {
                        return null;
                    }
                    tileentity = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
                } else {
                    tileentity = BlockEntity.loadStatic(pos, state, compoundnbt);
                }

                if (tileentity != null) {
                    icube.setCubeBlockEntity(tileentity);
                    return tileentity;
                }
            }

            if (icube.getBlockState(pos).hasBlockEntity()) {
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

    @Override public List<Entity> getEntities(@Nullable Entity entityIn, AABB boundingBox,
            @Nullable Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> clazz, AABB aabb, @Nullable Predicate<? super T> filter) {
        return Collections.emptyList();
    }

    @Override public List<? extends Player> players() {
        return level.players();
    }

    @Deprecated
    @Nullable @Override public ChunkAccess getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {
        throw new UnsupportedOperationException("This should never be called!");
    }

    @Override public int getHeight(Heightmap.Types heightmapType, int x, int z) {
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
        return this.level.getUncachedNoiseBiome(x, y, z);
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

    @Override public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    // setBlockState
    @Override public boolean setBlock(BlockPos pos, BlockState newState, int flags, int recursionLimit) {
        IBigCube icube = this.getCube(pos);
        BlockState blockstate = icube.setBlock(pos, newState, false);
        if (blockstate != null) {
            this.level.onBlockStateChange(pos, blockstate, newState);
        }
        if (newState.hasBlockEntity()) {
            if (icube.getCubeStatus().getChunkType() == ChunkStatus.ChunkType.LEVELCHUNK) {
                icube.setCubeBlockEntity(((EntityBlock) newState.getBlock()).newBlockEntity(pos, newState));
            } else {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putInt("x", pos.getX());
                compoundnbt.putInt("y", pos.getY());
                compoundnbt.putInt("z", pos.getZ());
                compoundnbt.putString("id", "DUMMY");
                //icube.addTileEntity(compoundnbt);
            }
        } else if (blockstate != null && blockstate.hasBlockEntity()) {
            icube.removeCubeBlockEntity(pos);
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
        if (blockstate.isAir()) {
            return false;
        } else {
            if (isPlayerInCreative) {
                BlockEntity tileentity = blockstate.hasBlockEntity() ? this.getBlockEntity(pos) : null;
                Block.dropResources(blockstate, this.level, pos, tileentity, droppedEntities, ItemStack.EMPTY);
            }

            return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3, recursionLimit);
        }
    }

    @Override public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> blockstate) {
        return blockstate.test(this.getBlockState(pos));
    }

    //TODO: DOUBLE CHECK THESE

    @Override
    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public Stream<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos, StructureFeature<?> structure) {
        return this.level.startsForFeature(sectionPos, structure);
    }

    @Override public int getSectionsCount() {
        return this.level.getSectionsCount();
    }

    @Override public int getMinSection() {
        return this.level.getMinSection();
    }
}