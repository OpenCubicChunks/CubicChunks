/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;
import static io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil.cast;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.util.XZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.FirstLightProcessor;
import io.github.opencubicchunks.cubicchunks.core.server.ChunkGc;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.SpawnCubes;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.util.world.CubeSplitTickList;
import io.github.opencubicchunks.cubicchunks.core.util.world.CubeSplitTickSet;
import io.github.opencubicchunks.cubicchunks.core.world.CubeWorldEntitySpawner;
import io.github.opencubicchunks.cubicchunks.core.world.IWorldEntitySpawner;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.CubicChunkManager;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.init.Blocks;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.ForgeChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Implementation of {@link ICubicWorldServer} interface.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(WorldServer.class)
@Implements(@Interface(iface = ICubicWorldServer.class, prefix = "world$"))
public abstract class MixinWorldServer extends MixinWorld implements ICubicWorldInternal.Server {

    @Shadow @Mutable @Final private PlayerChunkMap playerChunkMap;
    @Shadow @Mutable @Final private WorldEntitySpawner entitySpawner;
    @Shadow @Mutable @Final private EntityTracker entityTracker;
    @Shadow public boolean disableLevelSaving;
    private Map<Chunk, Set<ICube>> forcedChunksCubes;
    private XYZMap<ICube> forcedCubes;
    private XZMap<IColumn> forcedColumns;

    private ChunkGc worldChunkGc;
    private SpawnCubes spawnArea;
    private boolean runningCompatibilityGenerator;
    private VanillaNetworkHandler vanillaNetworkHandler;

    @Shadow protected abstract void playerCheckLight();

    @Shadow public abstract boolean spawnEntity(Entity entityIn);

    @Shadow public abstract boolean addWeatherEffect(Entity entityIn);

    @Shadow @Mutable @Final private Set<NextTickListEntry> pendingTickListEntriesHashSet;
    @Shadow @Mutable @Final private List<NextTickListEntry> pendingTickListEntriesThisTick;

    @Shadow public abstract PlayerChunkMap getPlayerChunkMap();

    @Nullable private FirstLightProcessor firstLightProcessor;

    @Override public void initCubicWorldServer(IntRange heightRange, IntRange generationRange) {
        super.initCubicWorld(heightRange, generationRange);
        this.isCubicWorld = true;
        IWorldEntitySpawner spawner = new CubeWorldEntitySpawner();
        IWorldEntitySpawner.Handler spawnHandler = cast(entitySpawner);
        spawnHandler.setEntitySpawner(spawner);

        this.chunkProvider = new CubeProviderServer((WorldServer) (Object) this,
                ((ICubicWorldProvider) this.provider).createCubeGenerator());

        this.vanillaNetworkHandler = new VanillaNetworkHandler((WorldServer) (Object) this);
        this.playerChunkMap = new PlayerCubeMap((WorldServer) (Object) this);

        this.firstLightProcessor = new FirstLightProcessor((WorldServer) (Object) this);

        this.forcedChunksCubes = new HashMap<>();
        this.forcedCubes = new XYZMap<>(0.75f, 64*1024);
        this.forcedColumns = new XZMap<>(0.75f, 2048);

        this.pendingTickListEntriesHashSet = new CubeSplitTickSet();
        this.pendingTickListEntriesThisTick = new CubeSplitTickList();
        this.worldChunkGc = new ChunkGc(getCubeCache());
    }

    @Override public VanillaNetworkHandler getVanillaNetworkHandler() {
        return vanillaNetworkHandler;
    }

    @Override public void setSpawnArea(SpawnCubes spawn) {
        this.spawnArea = spawn;
    }

    @Override public SpawnCubes getSpawnArea() {
        return spawnArea;
    }

    @Override public CubeSplitTickSet getScheduledTicks() {
        return (CubeSplitTickSet) pendingTickListEntriesHashSet;
    }

    @Override public CubeSplitTickList getThisTickScheduledTicks() {
        return (CubeSplitTickList) pendingTickListEntriesThisTick;
    }

    @Override public void tickCubicWorld() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        getLightingManager().onTick();
        if (this.spawnArea != null) {
            this.spawnArea.update((World) (Object) this);
        }
    }

    @Override public CubeProviderServer getCubeCache() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        return (CubeProviderServer) this.chunkProvider;
    }

    @Override public ICubeGenerator getCubeGenerator() {
        return getCubeCache().getCubeGenerator();
    }

    @Override public FirstLightProcessor getFirstLightProcessor() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        assert this.firstLightProcessor != null;
        return this.firstLightProcessor;
    }
    
    @Override public void removeForcedCube(ICube cube) {
        if (!forcedChunksCubes.get(cube.getColumn()).remove(cube)) {
            CubicChunks.LOGGER.error("Trying to remove forced cube " + cube.getCoords() + ", but it's not forced!");
        }
        forcedCubes.remove(cube);
        if (forcedChunksCubes.get(cube.getColumn()).isEmpty()) {
            forcedChunksCubes.remove(cube.getColumn());
            forcedColumns.remove(cube.getColumn());
        }
    }

    @Override public void addForcedCube(ICube cube) {
        if (!forcedChunksCubes.computeIfAbsent(cube.getColumn(), chunk -> new HashSet<>()).add(cube)) {
            CubicChunks.LOGGER.error("Trying to add forced cube " + cube.getCoords() + ", but it's already forced!");
        }
        forcedCubes.put(cube);
        forcedColumns.put(cube.getColumn());
    }

    @Override public XYZMap<ICube> getForcedCubes() {
        return forcedCubes;
    }

    @Override public XZMap<IColumn> getForcedColumns() {
        return forcedColumns;
    }

    @Override public void unloadOldCubes() {
        worldChunkGc.chunkGc();
    }


    /**
     * CubicChunks equivalent of {@link ForgeChunkManager#forceChunk(ForgeChunkManager.Ticket, ChunkPos)}.
     *
     * Can accept tickets from different worlds.
     */
    @Override
    public void forceChunk(ForgeChunkManager.Ticket ticket, CubePos chunk) {
        CubicChunkManager.forceChunk(ticket, chunk);
    }

    /**
     * CubicChunks equivalent of {@link ForgeChunkManager#reorderChunk(ForgeChunkManager.Ticket, ChunkPos)}
     *
     * Can accept tickets from different worlds.
     */
    @Override
    public void reorderChunk(ForgeChunkManager.Ticket ticket, CubePos chunk) {
        CubicChunkManager.reorderChunk(ticket, chunk);
    }

    /**
     * CubicChunks equivalent of {@link ForgeChunkManager#unforceChunk(ForgeChunkManager.Ticket, ChunkPos)}
     *
     * Can accept tickets from different worlds.
     */
    @Override
    public void unforceChunk(ForgeChunkManager.Ticket ticket, CubePos chunk) {
        CubicChunkManager.unforceChunk(ticket, chunk);
    }


    @Override
    public CompatGenerationScope doCompatibilityGeneration() {
        runningCompatibilityGenerator = true;
        return () -> runningCompatibilityGenerator = false;
    }

    @Override
    public boolean isCompatGenerationScope() {
        return runningCompatibilityGenerator;
    }

    /**
     * Handles cubic chunks world block updates.
     *
     * @param cbi callback info
     * @author Barteks2x
     */
    @Inject(method = "updateBlocks", at = @At("HEAD"), cancellable = true)
    protected void updateBlocksCubicChunks(CallbackInfo cbi) {
        if (!isCubicWorld()) {
            return;
        }
        cbi.cancel();
        this.playerCheckLight();

        int tickSpeed = this.getGameRules().getInt("randomTickSpeed");
        boolean raining = this.isRaining();
        boolean thundering = this.isThundering();
        this.profiler.startSection("pollingChunks");

        // CubicChunks - iterate over PlayerCubeMap.TickableChunkContainer instead of Chunks, getTickableChunks already includes forced chunks
        PlayerCubeMap.TickableChunkContainer chunks = ((PlayerCubeMap) this.playerChunkMap).getTickableChunks();
        for (Chunk chunk : chunks.columns()) {
            tickColumn(raining, thundering, chunk);
        }
        this.profiler.endStartSection("pollingCubes");

        if (tickSpeed > 0) {
            long worldTime = worldInfo.getWorldTotalTime();
            // CubicChunks - iterate over cubes instead of storage array from Chunk
            for (ICube cube : chunks.forcedCubes()) {
                tickCube(tickSpeed, cube, worldTime);
            }
            for (ICube cube : chunks.playerTickableCubes()) {
                if (cube == null) { // this is the internal array from the arraylist, anything beyond the size is null
                    break;
                }
                tickCube(tickSpeed, cube, worldTime);
            }
        }

        this.profiler.endSection();
    }

    private void tickCube(int tickSpeed, ICube cube, long worldTime) {
        if (!((Cube) cube).checkAndUpdateTick(worldTime)) {
            return;
        }
        int chunkBlockX = cubeToMinBlock(cube.getX());
        int chunkBlockZ = cubeToMinBlock(cube.getZ());

        this.profiler.startSection("tickBlocks");
        ExtendedBlockStorage ebs = cube.getStorage();
        if (ebs != Chunk.NULL_BLOCK_STORAGE && ebs.needsRandomTick()) {
            for (int i = 0; i < tickSpeed; ++i) {
                tickNextBlock(chunkBlockX, chunkBlockZ, ebs);
            }
        }
        this.profiler.endSection();
    }

    private void tickNextBlock(int chunkBlockX, int chunkBlockZ, ExtendedBlockStorage ebs) {
        this.updateLCG = this.updateLCG * 3 + 1013904223;
        int rand = this.updateLCG >> 2;
        int localX = rand & 15;
        int localZ = rand >> 8 & 15;
        int localY = rand >> 16 & 15;
        IBlockState state = ebs.get(localX, localY, localZ);
        Block block = state.getBlock();
        this.profiler.startSection("randomTick");

        if (block.getTickRandomly()) {
            block.randomTick((World) (Object) this,
                    new BlockPos(localX + chunkBlockX, localY + ebs.getYLocation(), localZ + chunkBlockZ), state, this.rand);
        }

        this.profiler.endSection();
    }

    private void tickColumn(boolean raining, boolean thundering, Chunk chunk) {
        int chunkBlockX = chunk.x * 16;
        int chunkBlockZ = chunk.z * 16;
        this.profiler.startSection("checkNextLight");
        chunk.enqueueRelightChecks();
        this.profiler.endStartSection("tickChunk");
        chunk.onTick(false);
        this.profiler.endStartSection("thunder");

        if (this.provider.canDoLightning(chunk) && raining && thundering && this.rand.nextInt(100000) == 0) {
            this.updateLCG = this.updateLCG * 3 + 1013904223;
            int rand = this.updateLCG >> 2;
            BlockPos strikePos =
                    this.adjustPosToNearbyEntityCubicChunks(new BlockPos(chunkBlockX + (rand & 15), 0, chunkBlockZ + (rand >> 8 & 15)));

            if (strikePos != null && this.isRainingAt(strikePos)) {
                DifficultyInstance difficultyinstance = this.getDifficultyForLocation(strikePos);

                if (this.getGameRules().getBoolean("doMobSpawning")
                        && this.rand.nextDouble() < (double) difficultyinstance.getAdditionalDifficulty() * 0.01D) {
                    EntitySkeletonHorse skeletonHorse = new EntitySkeletonHorse((World) (Object) this);
                    skeletonHorse.setTrap(true);
                    skeletonHorse.setGrowingAge(0);
                    skeletonHorse.setPosition((double) strikePos.getX(), (double) strikePos.getY(), (double) strikePos.getZ());
                    this.spawnEntity(skeletonHorse);
                    this.addWeatherEffect(new EntityLightningBolt((World) (Object) this,
                            (double) strikePos.getX(), (double) strikePos.getY(), (double) strikePos.getZ(), true));
                } else {
                    this.addWeatherEffect(new EntityLightningBolt((World) (Object) this,
                            (double) strikePos.getX(), (double) strikePos.getY(), (double) strikePos.getZ(), false));
                }
            }
        }

        this.profiler.endStartSection("iceandsnow");

        if (this.provider.canDoRainSnowIce(chunk) && this.rand.nextInt(16) == 0) {
            this.updateLCG = this.updateLCG * 3 + 1013904223;
            int j2 = this.updateLCG >> 2;
            BlockPos block = this.getPrecipitationHeight(new BlockPos(chunkBlockX + (j2 & 15), 0, chunkBlockZ + (j2 >> 8 & 15)));
            BlockPos blockBelow = block.down();

            if (this.isAreaLoaded(blockBelow, 1)) { // Forge: check area to avoid loading neighbors in unloaded chunks
                if (this.canBlockFreezeNoWater(blockBelow)) {
                    this.setBlockState(blockBelow, Blocks.ICE.getDefaultState());
                }
            }

            // CubicChunks - isBlockLoaded check
            if (raining && isBlockLoaded(block) && this.canSnowAt(block, true)) {
                this.setBlockState(block, Blocks.SNOW_LAYER.getDefaultState());
            }

            // CubicChunks - isBlockLoaded check
            if (raining && isBlockLoaded(blockBelow) && this.getBiome(blockBelow).canRain()) {
                this.getBlockState(blockBelow).getBlock().fillWithRain((World) (Object) this, blockBelow);
            }
        }
        this.profiler.endSection();
    }

    private BlockPos adjustPosToNearbyEntityCubicChunks(BlockPos strikeTarget) {
        Chunk column = this.getCubeCache().getColumn(Coords.blockToCube(strikeTarget.getX()), Coords.blockToCube(strikeTarget.getZ()),
                ICubeProviderServer.Requirement.GET_CACHED);
        strikeTarget = column.getPrecipitationHeight(strikeTarget);
        Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(strikeTarget));
        if (cube == null) {
            return null;
        }
        AxisAlignedBB aabb = (new AxisAlignedBB(strikeTarget)).grow(3.0D);

        Iterable<EntityLivingBase> setOfLiving = cube.getEntityContainer().getEntitySet().getByClass(EntityLivingBase.class);
        for (EntityLivingBase entity : setOfLiving) {
            if (!entity.isEntityAlive()) {
                continue;
            }
            BlockPos entityPos = entity.getPosition();
            if (entityPos.getY() < column.getHeightValue(Coords.blockToLocal(entityPos.getX()), Coords.blockToLocal(entityPos.getZ()))) {
                continue;
            }
            if (entity.getEntityBoundingBox().intersects(aabb)) {
                return entityPos;
            }
        }
        return strikeTarget;
    }
}
