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
package cubicchunks.world;

import cubicchunks.lighting.LightingManager;
import cubicchunks.util.CubePos;
import cubicchunks.world.cube.Cube;
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
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ICubicWorld extends IMinMaxHeight {

    /**
     * Updates the world
     */
    void tickCubicWorld();

    /**
     * Initializes the world to be a CubicChunks world. Must be done before any players are online and before any chunks
     * are loaded. Cannot be used more than once.
     * @param maxHeight
     * @param minHeight
     */
    void initCubicWorld(int minHeight, int maxHeight);

    boolean isCubicWorld();

    /**
     * Returns the {@link ICubeProvider} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     */
    ICubeProvider getCubeCache();

    /**
     * Returns the {@link LightingManager} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     */
    LightingManager getLightingManager();

    /**
     * Returns true iff the given Predicate evaluates to true for all cubes for block positions within blockRadius from
     * centerPos. Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     */
    default boolean testForCubes(BlockPos centerPos, int blockRadius, Predicate<Cube> test) {
        return testForCubes(
                centerPos.getX() - blockRadius, centerPos.getY() - blockRadius, centerPos.getZ() - blockRadius,
                centerPos.getX() + blockRadius, centerPos.getY() + blockRadius, centerPos.getZ() + blockRadius,
                test
        );
    }

    /**
     * Returns true iff the given Predicate evaluates to true for all cubes for block positions between
     * BlockPos(minBlockX, minBlockY, minBlockZ) and BlockPos(maxBlockX, maxBlockY, maxBlockZ) (including the specified
     * positions). Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     */
    default boolean testForCubes(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, Predicate<Cube> test) {
        return testForCubes(
                CubePos.fromBlockCoords(minBlockX, minBlockY, minBlockZ),
                CubePos.fromBlockCoords(maxBlockX, maxBlockY, maxBlockZ),
                test
        );
    }

    /**
     * Returns true iff the given Predicate evaluates to true for given cube and neighbors.
     * Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     */
    boolean testForCubes(CubePos start, CubePos end, Predicate<Cube> test);

    /**
     * Return the actual world height for this world. Typically this is 256 for worlds with a sky, and 128 for worlds
     * without.
     *
     * @return The actual world height
     */
    int getActualHeight();

    Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ);

    Cube getCubeFromBlockCoords(BlockPos pos);

    int getEffectiveHeight(int blockX, int blockZ);

    boolean isBlockColumnLoaded(BlockPos pos);

    boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty);

    //vanilla part

    //field accessors
    WorldProvider getProvider();

    Random getRand();

    List<EntityPlayer> getPlayerEntities();

    //true if it's clientside
    boolean isRemote();

    Profiler getProfiler();

    //methods

    boolean isValid(BlockPos pos);

    boolean isBlockLoaded(BlockPos pos);

    boolean isBlockLoaded(BlockPos pos, boolean allowEmpty);

    void loadEntities(Collection<Entity> entities);

    void addTileEntities(Collection<TileEntity> values);

    void unloadEntities(Collection<Entity> entities);

    void removeTileEntity(BlockPos pos);

    long getTotalWorldTime();

    void setTileEntity(BlockPos blockpos, @Nullable TileEntity tileentity);

    void markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1);

    boolean addTileEntity(TileEntity tileEntity);

    void markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ);

    long getSeed();

    boolean checkLightFor(EnumSkyBlock sky, BlockPos pos);

    ISaveHandler getSaveHandler();

    MinecraftServer getMinecraftServer();

    void addBlockEvent(BlockPos blockPos, Block i, int t, int p);

    void scheduleBlockUpdate(BlockPos blockPos, Block i, int t, int p);

    GameRules getGameRules();

    WorldInfo getWorldInfo();

    @Nullable TileEntity getTileEntity(BlockPos pos);

    boolean setBlockState(BlockPos blockPos, IBlockState blockState, int i);

    IBlockState getBlockState(BlockPos pos);

    boolean isAirBlock(BlockPos randomPos);

    Biome getBiome(BlockPos blockPos);

    BiomeProvider getBiomeProvider();

    BlockPos getSpawnPoint();

    WorldBorder getWorldBorder();

    int countEntities(EnumCreatureType type, boolean flag);

    boolean isAnyPlayerWithinRangeAt(double f, double i3, double f1, double v);

    DifficultyInstance getDifficultyForLocation(BlockPos pos);

    boolean spawnEntity(Entity entityliving);

    boolean isAreaLoaded(BlockPos startPos, BlockPos endPos);

    void notifyLightSet(BlockPos pos);

    WorldType getWorldType();
}
