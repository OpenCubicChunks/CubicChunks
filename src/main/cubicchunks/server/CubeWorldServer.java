/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
package cubicchunks.server;

import java.util.Random;
import java.util.Set;

import net.minecraft.entity.CreatureTypes;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldInfo;
import net.minecraft.world.WorldServerAccessor;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.ISaveHandler;
import cubicchunks.CubeProviderTools;
import cubicchunks.generator.biome.CCBiomeManager;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.world.Column;
import cubicchunks.world.CubeCache;
import cuchaz.m3l.util.Util;

public class CubeWorldServer {
	
	// TODO: this class will get deleted
	// but we need to migrate this code to other places first
	
	public CubeWorldServer(MinecraftServer server, ISaveHandler saveHandler, WorldInfo worldInfo, int dimension, Profiler profiler) {
		super(server, saveHandler, worldInfo, dimension, profiler);
		
		// set the player manager
		CubePlayerManager playerManager = new CubePlayerManager(this, server.getConfigurationManager().getViewRadius());
		WorldServerAccessor.setPlayerManager(this, playerManager);
	}
	
	@Override
	public void tick() {
		super.tick();
		
		this.profiler.startSection("generatorPipeline");
		this.generatorPipeline.tick();
		this.profiler.endSection();
		
		this.profiler.startSection("lightingEngine");
		this.lightingManager.tick();
		this.profiler.endSection();
	}
	
	/**
	 * only spawns creatures allowed by the chunkProvider
	 */
	@Override
	@Deprecated
	public Biome.SpawnMob spawnRandomCreature(CreatureTypes creatureType, int par2, int par3, int par4) {
		// List var5 = (List) this.getChunkProvider().getPossibleCreatures(par1EnumCreatureType, par2, par3, par4);
		// return var5 != null && !var5.isEmpty() ? (net.minecraft.world.biome.BiomeGenBase.SpawnListEntry)WeightedRandom.getRandomItem(this.rand, var5) : null;
		return null;
	}
	
	public long getSpawnPointCubeAddress() {
		return AddressTools.getAddress(Coords.blockToCube(this.worldInfo.getSpawnX()), Coords.blockToCube(this.worldInfo.getSpawnY()), Coords.blockToCube(this.worldInfo.getSpawnZ()));
	}
	
	@Override
	public boolean checkBlockRangeIsInWorld(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, boolean flag) {
		return CubeProviderTools.blocksExist((CubeCache)this.chunkCache, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
	}
	
	@Override
	public boolean updateLightingAt(LightType lightType, BlockPos pos) {
		// forward to the new lighting system
		return this.lightingManager.computeDiffuseLighting(pos, lightType);
	}
	
	@Override
	// tick
	@SuppressWarnings("unchecked")
	protected void func_147456_g() {
		super.func_147456_g();
		
		// apply random ticks
		for (ChunkCoordIntPair coords : (Set<ChunkCoordIntPair>)this.activeChunkSet) {
			Column column = (Column)this.chunkCache.getChunk(coords.chunkX, coords.chunkZ);
			column.doRandomTicks();
		}
	}
	
	@Override
	protected void createSpawnPosition(WorldSettings worldSettings) {
		// NOTE: this is called inside the world constructor
		// this is apparently called before the world is generated
		// we'll have to do our own generation to find the spawn point
		
		if (!this.dimension.canRespawnHere()) {
			this.worldInfo.setSpawnPoint(new BlockPos(0,0,0));
			return;
		}
		
		// pick a default fail-safe spawn point
		int spawnBlockX = 0;
		int spawnBlockY = this.dimension.getAverageGroundLevel();
		int spawnBlockZ = 0;
		
		Random rand = new Random(getSeed());
		
		// defer to the column manager to find the x,z part of the spawn point
		CCBiomeManager biomeManager = getCubeWorldProvider().getBiomeMananger();
		BlockPos spawnPosition = biomeManager.getRandomPositionInBiome(0, 0, 256, biomeManager.getSpawnableBiomes(), rand);
		if (spawnPosition != null) {
			spawnBlockX = spawnPosition.getX();
			spawnBlockZ = spawnPosition.getZ();
		} else {
			log.warn("Unable to find spawn biome");
		}
		
		log.info("Searching for suitable spawn point...");
		
		// generate some world around the spawn x,z at sea level
		int spawnCubeX = Coords.blockToCube(spawnBlockX);
		int spawnCubeY = Coords.blockToCube(getCubeWorldProvider().getSeaLevel());
		int spawnCubeZ = Coords.blockToCube(spawnBlockZ);
		
		final int SearchDistance = 4;
		ServerCubeCache cubeCache = getCubeCache();
		for (int cubeX = spawnCubeX - SearchDistance; cubeX <= spawnCubeX + SearchDistance; cubeX++) {
			for (int cubeY = spawnCubeY - SearchDistance; cubeY <= spawnCubeY + SearchDistance; cubeY++) {
				for (int cubeZ = spawnCubeZ - SearchDistance; cubeZ <= spawnCubeZ + SearchDistance; cubeZ++) {
					cubeCache.loadCube(cubeX, cubeY, cubeZ);
				}
			}
		}
		getGeneratorPipeline().generateAll();
		
		// make some effort to find a suitable spawn point, but don't guarantee it
		for (int i = 0; i < 1000 && !this.dimension.canCoordinateBeSpawn(spawnBlockX, spawnBlockZ); i++) {
			spawnBlockX += Util.randRange(rand, -16, 16);
			spawnBlockZ += Util.randRange(rand, -16, 16);
		}
		
		// save the spawn point
		this.worldInfo.setSpawnPoint(new BlockPos(spawnBlockX, spawnBlockY, spawnBlockZ));
		log.info(String.format("Found spawn point at (%d,%d,%d)", spawnBlockX, spawnBlockY, spawnBlockZ));
		
		if (worldSettings.isBonusChestEnabled()) {
			createBonusChest();
		}
	}
}
