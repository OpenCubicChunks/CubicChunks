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
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import java.util.Collection;
import java.util.Random;

public interface ICubicWorld {

	void initCubicWorld();

	boolean isCubicWorld();

	/**
	 * Returns Y position of the bottom block in the world
	 */
	int getMinHeight();

	/**
	 * Returns Y position of block above the top block in the world,
	 */
	int getMaxHeight();

	ICubeCache getCubeCache();

	LightingManager getLightingManager();

	boolean blocksExist(BlockPos pos, int dist, boolean allowEmptyCubes, GeneratorStage minStageAllowed);

	boolean blocksExist(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, boolean allowEmptyColumns, GeneratorStage minStageAllowed);

	boolean cubeAndNeighborsExist(Cube cube, boolean allowEmptyCubes, GeneratorStage minStageAllowed);

	Cube getCubeForAddress(long address);

	Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ);

	Cube getCubeFromBlockCoords(BlockPos pos);

	void setGeneratingWorld(boolean b);

	//vanilla part

	//field accessors
	WorldProvider getProvider();

	Random getRand();

	boolean isRemote();

	//methods
	void loadEntities(Collection<Entity> entities);

	void addTileEntities(Collection<TileEntity> values);

	void unloadEntities(Collection<Entity> entities);

	void removeTileEntity(BlockPos pos);

	long getTotalWorldTime();

	void setTileEntity(BlockPos blockpos, TileEntity tileentity);

	void markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1);

	boolean addTileEntity(TileEntity tileEntity);

	void markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ);

	long getSeed();

	boolean checkLightFor(EnumSkyBlock sky, BlockPos pos);

	ISaveHandler getSaveHandler();

	MinecraftServer getMinecraftServer();

	void addBlockEvent(BlockPos blockPos, Block i, int t, int p);

	GameRules getGameRules();

	WorldInfo getWorldInfo();

	TileEntity getTileEntity(BlockPos pos);

	boolean setBlockState(BlockPos blockPos, IBlockState blockState, int i);

	IBlockState getBlockState(BlockPos pos);

	boolean isAirBlock(BlockPos randomPos);

	Biome getBiome(BlockPos blockPos);

	BiomeProvider getBiomeProvider();

	BlockPos getSpawnPoint();
}
