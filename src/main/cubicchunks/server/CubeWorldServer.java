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

import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldSettings;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeTools;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.column.Column;
import cuchaz.m3l.util.Util;

public class CubeWorldServer {
	
	// TODO: this class will get deleted
	// but we need to migrate this code to other places first
	
	@Override
	public boolean checkBlockRangeIsInWorld(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, boolean flag) {
		return CubeTools.blocksExist((ICubeCache)this.chunkCache, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
	}
	
	@Override
	public boolean updateLightingAt(LightType lightType, BlockPos pos) {
		// forward to the new lighting system
		return this.lightingManager.computeDiffuseLighting(pos, lightType);
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
