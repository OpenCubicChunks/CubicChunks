/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.asm.mixin.core.common;

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.blockToLocal;

import cubicchunks.lighting.LightingManager;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.NotCubicChunksWorldException;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.provider.ICubicWorldProvider;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Contains implementation of {@link ICubicWorld} interface.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(World.class)
@Implements(@Interface(iface = ICubicWorld.class, prefix = "world$"))
public abstract class MixinWorld implements ICubicWorld {

    @Shadow protected IChunkProvider chunkProvider;
    @Shadow @Final @Mutable public WorldProvider provider;
    @Shadow @Final public Random rand;
    @Shadow @Final public boolean isRemote;
    @Shadow @Final public Profiler profiler;
    @Shadow @Final @Mutable protected ISaveHandler saveHandler;
    @Shadow protected boolean findingSpawnPoint;

    @Shadow protected abstract boolean isChunkLoaded(int i, int i1, boolean allowEmpty);

    @Nullable private LightingManager lightingManager;
    protected boolean isCubicWorld;
    protected int minHeight = 0, maxHeight = 256;

    @Override public void initCubicWorld(int minHeight1, int maxHeight1) {
        // Set the world height boundaries to their highest and lowest values respectively
        this.minHeight = minHeight1;
        this.maxHeight = maxHeight1;
        //has to be created early so that creating BlankCube won't crash
        this.lightingManager = new LightingManager(this);
    }

    @Override public boolean isCubicWorld() {
        return this.isCubicWorld;
    }

    @Override public int getMinHeight() {
        return this.minHeight;
    }

    @Override public int getMaxHeight() {
        return this.maxHeight;
    }

    @Override public ICubeProvider getCubeCache() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        return (ICubeProvider) this.chunkProvider;
    }

    @Override public LightingManager getLightingManager() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        assert this.lightingManager != null;
        return this.lightingManager;
    }

    @Override
    public boolean testForCubes(CubePos start, CubePos end, Predicate<Cube> cubeAllowed) {
        // convert block bounds to chunk bounds
        int minCubeX = start.getX();
        int minCubeY = start.getY();
        int minCubeZ = start.getZ();
        int maxCubeX = end.getX();
        int maxCubeY = end.getY();
        int maxCubeZ = end.getZ();

        for (int cubeX = minCubeX; cubeX <= maxCubeX; cubeX++) {
            for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
                for (int cubeZ = minCubeZ; cubeZ <= maxCubeZ; cubeZ++) {
                    Cube cube = this.getCubeCache().getLoadedCube(cubeX, cubeY, cubeZ);
                    if (!cubeAllowed.test(cube)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override public Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ) {
        return this.getCubeCache().getCube(cubeX, cubeY, cubeZ);
    }

    @Override public Cube getCubeFromBlockCoords(BlockPos pos) {
        return this.getCubeFromCubeCoords(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()));
    }

    @Override public int getEffectiveHeight(int blockX, int blockZ) {
        return this.chunkProvider.provideChunk(blockToCube(blockX), blockToCube(blockZ))
                .getHeightValue(blockToLocal(blockX), blockToLocal(blockZ));
    }

    // suppress mixin warning when running with -Dmixin.checks.interfaces=true
    @Override public void tickCubicWorld() {
        // pretend this method doesn't exist
        throw new NoSuchMethodError("World.tickCubicWorld: Classes extending World need to implement tickCubicWorld in CubicChunks");
    }

    //vanilla field accessors

    @Override public WorldProvider getProvider() {
        return this.provider;
    }

    @Override public Random getRand() {
        return this.rand;
    }

    @Override public boolean isRemote() {
        return this.isRemote;
    }

    @Override public List<EntityPlayer> getPlayerEntities() {
        return ((World) (Object) this).playerEntities;
    }

    @Override public Profiler getProfiler() {
        return this.profiler;
    }

    /**
     * @author Foghrye4
     * @reason Original {@link World#markChunkDirty(BlockPos, TileEntity)}
     *         called by TileEntities whenever they need to force Chunk to save
     *         valuable info they changed. Because now we store TileEntities in
     *         Cubes instead of Chunks, it will be quite reasonable to force
     *         Cubes to save themselves.
     */
    @Inject(method = "markChunkDirty", at = @At("HEAD"), cancellable = true)
    public void onMarkChunkDirty(BlockPos pos, TileEntity unusedTileEntity, CallbackInfo ci) {
        if (this.isCubicWorld()) {
            Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos));
            if (cube != null) {
                cube.markDirty();
            }
            ci.cancel();
        }
    }

    @Override public boolean isBlockColumnLoaded(BlockPos pos) {
        return isBlockColumnLoaded(pos, true);
    }

    @Override public boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty) {
        return this.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4, allowEmpty);
    }

    //vanilla methods

    //==============================================
    @Shadow public abstract boolean isValid(BlockPos pos);

    @Intrinsic public boolean world$isValid(BlockPos pos) {
        return this.isValid(pos);
    }

    //==============================================
    @Shadow public abstract boolean isBlockLoaded(BlockPos pos);

    @Intrinsic public boolean world$isBlockLoaded(BlockPos pos) {
        return this.isBlockLoaded(pos);
    }

    //==============================================
    @Shadow public abstract boolean isBlockLoaded(BlockPos pos, boolean allowEmpty);

    @Intrinsic public boolean world$isBlockLoaded(BlockPos pos, boolean allowEmpty) {
        return this.isBlockLoaded(pos, allowEmpty);
    }

    //==============================================
    @Shadow public abstract void loadEntities(Collection<Entity> entities);

    @Intrinsic public void world$loadEntities(Collection<Entity> entities) {
        this.loadEntities(entities);
    }

    //==============================================
    @Shadow public abstract void addTileEntities(Collection<TileEntity> values);

    @Intrinsic public void world$addTileEntities(Collection<TileEntity> values) {
        this.addTileEntities(values);
    }

    //==============================================
    @Shadow public abstract void unloadEntities(Collection<Entity> entities);

    @Intrinsic public void world$unloadEntities(Collection<Entity> entities) {
        this.unloadEntities(entities);
    }

    //==============================================
    @Shadow public abstract void removeTileEntity(BlockPos pos);

    @Intrinsic public void world$removeTileEntity(BlockPos pos) {
        this.removeTileEntity(pos);
    }

    //==============================================
    @Shadow public abstract long getTotalWorldTime();

    @Intrinsic public long world$getTotalWorldTime() {
        return this.getTotalWorldTime();
    }
    //==============================================

    @Shadow public abstract void setTileEntity(BlockPos blockpos, @Nullable TileEntity tileentity);

    @Intrinsic public void world$setTileEntity(BlockPos blockpos, @Nullable TileEntity tileentity) {
        this.setTileEntity(blockpos, tileentity);
    }
    //==============================================

    @Shadow public abstract void markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1);

    @Intrinsic public void world$markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1) {
        this.markBlockRangeForRenderUpdate(blockpos, blockpos1);
    }
    //==============================================

    @Shadow public abstract boolean addTileEntity(TileEntity tileEntity);

    @Intrinsic public boolean world$addTileEntity(TileEntity tileEntity) {
        return this.addTileEntity(tileEntity);
    }
    //==============================================

    @Shadow
    public abstract void markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ);

    @Intrinsic
    public void world$markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {
        this.markBlockRangeForRenderUpdate(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
    }
    //==============================================

    @Shadow public abstract long getSeed();

    @Intrinsic public long world$getSeed() {
        return this.getSeed();
    }
    //==============================================

    @Shadow public abstract boolean checkLightFor(EnumSkyBlock sky, BlockPos pos);

    @Intrinsic public boolean world$checkLightFor(EnumSkyBlock type, BlockPos pos) {
        return this.checkLightFor(type, pos);
    }
    //==============================================

    @Shadow public abstract ISaveHandler getSaveHandler();

    @Intrinsic public ISaveHandler world$getSaveHandler() {
        return this.getSaveHandler();
    }
    //==============================================

    @Shadow public abstract MinecraftServer getMinecraftServer();

    @Intrinsic public MinecraftServer world$getMinecraftServer() {
        return this.getMinecraftServer();
    }
    //==============================================

    @Shadow public abstract void addBlockEvent(BlockPos blockPos, Block i, int t, int p);

    @Intrinsic public void world$addBlockEvent(BlockPos blockPos, Block i, int t, int p) {
        this.addBlockEvent(blockPos, i, t, p);
    }
    //==============================================

    @Shadow public abstract void scheduleBlockUpdate(BlockPos blockPos, Block i, int t, int p);

    @Intrinsic public void world$scheduleBlockUpdate(BlockPos blockPos, Block i, int t, int p) {
        this.scheduleBlockUpdate(blockPos, i, t, p);
    }
    //==============================================

    @Shadow public abstract GameRules getGameRules();

    @Intrinsic public GameRules world$getGameRules() {
        return this.getGameRules();
    }
    //==============================================

    @Shadow public abstract WorldInfo getWorldInfo();

    @Intrinsic public WorldInfo world$getWorldInfo() {
        return this.getWorldInfo();
    }
    //==============================================

    @Nullable @Shadow public abstract TileEntity getTileEntity(BlockPos pos);

    @Nullable @Intrinsic public TileEntity world$getTileEntity(BlockPos pos) {
        return this.getTileEntity(pos);
    }
    //==============================================

    @Shadow public abstract boolean setBlockState(BlockPos blockPos, IBlockState blockState, int i);

    @Intrinsic public boolean world$setBlockState(BlockPos blockPos, IBlockState blockState, int i) {
        return this.setBlockState(blockPos, blockState, i);
    }
    //==============================================

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    @Intrinsic public IBlockState world$getBlockState(BlockPos pos) {
        return this.getBlockState(pos);
    }
    //==============================================

    @Shadow public abstract boolean isAirBlock(BlockPos randomPos);

    @Intrinsic public boolean world$isAirBlock(BlockPos pos) {
        return this.isAirBlock(pos);
    }
    //==============================================

    @Shadow public abstract Biome getBiome(BlockPos cubeCenter);

    @Intrinsic public Biome world$getBiome(BlockPos pos) {
        return this.getBiome(pos);
    }
    //==============================================

    @Shadow public abstract BiomeProvider getBiomeProvider();

    @Intrinsic public BiomeProvider world$getBiomeProvider() {
        return this.getBiomeProvider();
    }
    //==============================================

    @Shadow public abstract BlockPos getSpawnPoint();

    @Intrinsic public BlockPos world$getSpawnPoint() {
        return this.getSpawnPoint();
    }
    //==============================================

    @Shadow public abstract WorldBorder getWorldBorder();

    @Intrinsic public WorldBorder world$getWorldBorder() {
        return this.getWorldBorder();
    }
    //==============================================

    @Shadow(remap = false) public abstract int countEntities(EnumCreatureType type, boolean flag);

    @Intrinsic public int world$countEntities(EnumCreatureType type, boolean flag) {
        return this.countEntities(type, flag);
    }
    //==============================================

    @Shadow public abstract boolean isAnyPlayerWithinRangeAt(double f, double i3, double f1, double v);

    @Intrinsic public boolean world$isAnyPlayerWithinRangeAt(double f, double i3, double f1, double v) {
        return this.isAnyPlayerWithinRangeAt(f, i3, f1, v);
    }
    //==============================================

    @Shadow public abstract DifficultyInstance getDifficultyForLocation(BlockPos pos);

    @Intrinsic public DifficultyInstance world$getDifficultyForLocation(BlockPos pos) {
        return this.getDifficultyForLocation(pos);
    }
    //==============================================

    @Shadow public abstract boolean spawnEntity(Entity entity);

    @Intrinsic public boolean world$spawnEntity(Entity entity) {
        return this.spawnEntity(entity);
    }
    //==============================================

    @Shadow public abstract boolean isAreaLoaded(BlockPos start, BlockPos end);

    @Intrinsic public boolean world$isAreaLoaded(BlockPos start, BlockPos end) {
        return this.isAreaLoaded(start, end);
    }
    //==============================================

    @Shadow public abstract int getActualHeight();

    @Intrinsic public int world$getActualHeight() {
        return this.getActualHeight();
    }
    //==============================================

    @Shadow public abstract void notifyLightSet(BlockPos pos);

    @Intrinsic public void world$notifyLightSet(BlockPos pos) {
        this.notifyLightSet(pos);
    }
    //==============================================

    @Shadow public abstract WorldType getWorldType();

    @Intrinsic public WorldType world$getWorldType() {
        return this.getWorldType();
    }
    //==============================================

    @Shadow public abstract boolean canBlockFreezeWater(BlockPos topBlock);

    @Intrinsic public boolean world$canBlockFreezeWater(BlockPos topBlock) {
        return canBlockFreezeWater(topBlock);
    }
    //==============================================

    @Shadow public abstract boolean canSnowAt(BlockPos aboveTop, boolean flag);

    @Intrinsic public boolean world$canSnowAt(BlockPos aboveTop, boolean flag) {
        return canSnowAt(aboveTop, flag);
    }
    //==============================================
}
