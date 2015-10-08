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
package cubicchunks;

import cubicchunks.client.ClientCubeCache;
import cubicchunks.client.WorldClientContext;
import cubicchunks.generator.GeneratorPipeline;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.server.CubePlayerManager;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.WorldServerContext;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.MutableBlockPos;
import cubicchunks.util.WorldAccess;
import cubicchunks.world.WorldContext;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.*;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import java.util.Random;

import static cubicchunks.generator.terrain.GlobalGeneratorConfig.SEA_LEVEL;

//TODO: Get rid of this class, move it's parts to other classes
public class CubicChunkSystem {

	private ClassInheritanceMultiMap m_emptyEntitySet;

	public CubicChunkSystem() {
		m_emptyEntitySet = new ClassInheritanceMultiMap(Entity.class);
	}
	public ChunkProviderServer getServerChunkCacheAndInitWorld(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			ServerCubeCache serverCubeCache = new ServerCubeCache(worldServer);
			WorldServerContext.put(worldServer, new WorldServerContext(worldServer, serverCubeCache));
			return serverCubeCache;
			
		}
		return null;
	}

	//@ClientOnly
	public ChunkProviderClient getClientChunkCacheAndInitWorld(WorldClient worldClient) {
		if (isTallWorld(worldClient)) {
			ClientCubeCache clientCubeCache = new ClientCubeCache(worldClient);
			WorldClientContext.put(worldClient, new WorldClientContext(worldClient, clientCubeCache));
			return clientCubeCache;
		}
		return null;
	}

	public WorldChunkManager getBiomeManager(World world) {

		/*
		 * TEMP: disable fancy generation // for now, only muck with the overworld if (world.dimension.getId() == 0) {
		 * DimensionType dimensionType = world.getWorldInfo().getDimensionType(); if (dimensionType !=
		 * DimensionType.FLAT && dimensionType != DimensionType.DEBUG_ALL_BLOCK_STATES) { return new
		 * CCBiomeManager(world); } }
		 */

		return null;
	}

	public Integer getMinBlockY(World world) {
		if (isTallWorld(world)) {
			return Coords.cubeToMinBlock(AddressTools.MinY);
		}
		return null;
	}

	public Integer getMaxBlockY(World world) {
		if (isTallWorld(world)) {
			return Coords.cubeToMaxBlock(AddressTools.MaxY);
		}
		return null;
	}

	public PlayerManager getPlayerManager(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			return new CubePlayerManager(worldServer);
		}
		return null;
	}

	public void processChunkLoadQueue(EntityPlayerMP player) {
		WorldServer worldServer = (WorldServer) player.getServerForPlayer();
		if (isTallWorld(worldServer)) {
			CubePlayerManager playerManager = (CubePlayerManager) worldServer.getPlayerManager();
			playerManager.processCubeQueues(player);
		}
	}

	public boolean isTallWorld(World world) {
		// for now, only tall-ify the overworld
		return world.provider.getDimensionId() == 0 && world.getWorldType() == CubicChunks.CC_WORLD_TYPE;
	}

	//@ClientOnly
	public void onWorldClientTick(WorldClient worldClient) {
		if (isTallWorld(worldClient)) {
			WorldClientContext context = WorldClientContext.get(worldClient);

			// tick all the things!
			//worldClient.profiler.startSection("lightingEngine");
			context.getLightingManager().tick();
			//worldClient.profiler.endSection();
		}
	}

	public void onWorldServerTick(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			WorldServerContext context = WorldServerContext.get(worldServer);

			// tick all the things!
			//worldServer.profiler.startSection("generatorPipeline");
			context.getGeneratorPipeline().tick();

			//worldServer.profiler.addSection("lightingEngine");
			context.getLightingManager().tick();

			//worldServer.profiler.addSection("randomCubeTicks");
			ServerCubeCache cubeCache = context.getCubeCache();
			for (ChunkCoordIntPair coords : WorldAccess.getActiveChunkSet(worldServer)) {
				Column column = cubeCache.provideChunk(coords.chunkXPos, coords.chunkZPos);
				column.doRandomTicks();
			}
			//worldServer.profiler.endSection();
		}
	}

	public Integer getRandomBlockYForMobSpawnAttempt(Random rand, int upper, World world, int cubeX, int cubeZ) {
		// need to return a random blockY between the "bottom" of the world and upper
		// TEMP: well... we don't really have a bottom, so just clamp the val to [15,upper] for now
		return rand.nextInt(Math.max(15, upper));
	}

	public void generateWorld(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			WorldServerContext context = WorldServerContext.get(worldServer);
			ServerCubeCache serverCubeCache = context.getCubeCache();

			// load the cubes around the spawn point
			//CubicChunks.LOGGER.info("Loading cubes for spawn...");
			final int Distance = ServerCubeCache.WorldSpawnChunkDistance;
			BlockPos spawnPoint = worldServer.getSpawnPoint();
			int spawnCubeX = Coords.blockToCube(spawnPoint.getX());
			int spawnCubeY = Coords.blockToCube(spawnPoint.getY());
			int spawnCubeZ = Coords.blockToCube(spawnPoint.getZ());
			for (int cubeX = spawnCubeX - Distance; cubeX <= spawnCubeX + Distance; cubeX++) {
				for (int cubeY = spawnCubeY - Distance; cubeY <= spawnCubeY + Distance; cubeY++) {
					for (int cubeZ = spawnCubeZ - Distance; cubeZ <= spawnCubeZ + Distance; cubeZ++) {
						serverCubeCache.loadCubeAndNeighbors(cubeX, cubeY, cubeZ);
					}
				}
			}

			// wait for the cubes to be loaded
			GeneratorPipeline pipeline = context.getGeneratorPipeline();
			int numCubesTotal = pipeline.getNumCubes();
			if (numCubesTotal > 0) {
				long timeStart = System.currentTimeMillis();
				//CubicChunks.LOGGER.info("Generating {} cubes for spawn at block ({},{},{}) cube ({},{},{})...",
				//		numCubesTotal, spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), spawnCubeX, spawnCubeY,
				//		spawnCubeZ);
				pipeline.generateAll();
				long timeDiff = System.currentTimeMillis() - timeStart;
				//CubicChunks.LOGGER.info("Done in {} ms", timeDiff);
			}
			
			// save the cubes now
			serverCubeCache.saveAllChunks();
		}
	}

	public boolean calculateSpawn(WorldServer worldServer, WorldSettings worldSettings) {

		if (!isTallWorld(worldServer)) {
			return false;
		}

		WorldServerContext context = WorldServerContext.get(worldServer);
		ServerCubeCache serverCubeCache = context.getCubeCache();

		// NOTE: this is called inside the world constructor
		// this is apparently called before the world is generated
		// we'll have to do our own generation to find the spawn point

		if (!worldServer.provider.canRespawnHere()) {
			worldServer.getWorldInfo().setSpawn(BlockPos.ORIGIN.up());
			return false;
		}

		// pick a default fail-safe spawn point
		MutableBlockPos spawnPos = new MutableBlockPos(0, worldServer.provider.getAverageGroundLevel(), 0);

		Random rand = new Random(worldServer.getSeed());

		// defer to the column manager to find the x,z part of the spawn point
		WorldChunkManager biomeManager = worldServer.provider.getWorldChunkManager();
		BlockPos spawnPosition = biomeManager.findBiomePosition(0, 0, 256, biomeManager.getBiomesToSpawnIn(),
				rand);
		if (spawnPosition != null) {
			spawnPos.x = spawnPosition.getX();
			spawnPos.z = spawnPosition.getZ();
		} else {
			//CubicChunks.LOGGER.warn("Unable to find spawn biome");
		}

		//CubicChunks.LOGGER.info("Searching for suitable spawn point...");

		// generate some world around the spawn x,z at sea level
		int spawnCubeX = Coords.blockToCube(spawnPos.getX());
		int spawnCubeY = Coords.blockToCube(worldServer.provider.getAverageGroundLevel());
		int spawnCubeZ = Coords.blockToCube(spawnPos.getZ());

		final int SearchDistance = 4;
		for (int cubeX = spawnCubeX - SearchDistance; cubeX <= spawnCubeX + SearchDistance; cubeX++) {
			for (int cubeY = spawnCubeY - SearchDistance; cubeY <= spawnCubeY + SearchDistance; cubeY++) {
				for (int cubeZ = spawnCubeZ - SearchDistance; cubeZ <= spawnCubeZ + SearchDistance; cubeZ++) {
					serverCubeCache.loadCube(cubeX, cubeY, cubeZ);
				}
			}
		}
		context.getGeneratorPipeline().generateAll();

		// make some effort to find a suitable spawn point, but don't guarantee it
		for (int i = 0; i < 1000 && !worldServer.provider.canCoordinateBeSpawn(spawnPos.getX(), spawnPos.getZ()); i++) {
			spawnPos.x += cubicchunks.util.MathHelper.randRange(rand, -16, 16);
			spawnPos.z += cubicchunks.util.MathHelper.randRange(rand, -16, 16);
		}

		// save the spawn point
		worldServer.getWorldInfo().setSpawn(spawnPos);
		//CubicChunks.LOGGER.info("Found spawn point at ({},{},{})", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

		if (worldSettings.isBonusChestEnabled()) {
			//worldServer.generateBonusChests();
		}

		// spawn point calculated successfully
		return true;
	}

	public Integer getSeaLevel(World world) {
		if (isTallWorld(world)) {
			return SEA_LEVEL;
		}
		return null;
	}

	public Double getHorizonLevel(World world) {
		if (isTallWorld(world)) {
			// tall worlds will eventually have the horizon level at 0
			// for now, let's just do the vanilla thing
			return 63.0;
		}
		return null;
	}

	public Boolean updateLightingAt(World world, EnumSkyBlock type, BlockPos pos) {
		/* TEMP: for now, the vanilla lighting implementation is much faster than mine
		 * let's just use that for now
		if (isTallWorld(world)) {
			LightingManager lightingManager = WorldContext.get(world).getLightingManager();
			return lightingManager.computeDiffuseLighting(pos, type);
		}
		*/
		return null;
	}

	public Boolean checkBlockRangeIsInWorld(World world, int minBlockX, int minBlockY, int minBlockZ, int maxBlockX,
			int maxBlockY, int maxBlockZ, boolean allowEmptyColumns) {
		if (isTallWorld(world)) {
			WorldContext context = WorldContext.get(world);
			// the min stage here has to be at least lighting, since the lighting system in World checks for blocks, but doesn't know about stages
			final GeneratorStage minCubeStage = GeneratorStage.LIGHTING;
			return context.blocksExist(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ, allowEmptyColumns, minCubeStage);
		}
		return null;
	}

	public Boolean checkEntityIsInWorld(World world, Entity entity, int minBlockX, int minBlockZ, int maxBlockX,
			int maxBlockZ, boolean allowEmptyColumns) {
		if (isTallWorld(world)) {
			WorldContext context = WorldContext.get(world);

			final int blockDist = 32;
			int blockY = MathHelper.floor_double(entity.posY);
			
			// the min cube stage can be live here since players should be in fully-generated cubes
			final GeneratorStage minCubeStage = GeneratorStage.LIVE;
			return context.blocksExist(minBlockX, blockY - blockDist, minBlockZ, maxBlockX, blockY + blockDist, maxBlockZ, allowEmptyColumns, minCubeStage);
		}
		return null;
	}

	//@ClientOnly
	public boolean frustumViewUpdateChunkPositions(ViewFrustum renderers) {
		if (!isTallWorld(renderers.world)) return false;

		Entity view = Minecraft.getMinecraft().getRenderViewEntity();
		double x = view.posX;
		double y = view.posY;
		double z = view.posZ;

		// treat the y dimension the same as all the rest
		int viewX = MathHelper.floor_double(x) - 8;
		int viewY = MathHelper.floor_double(y) - 8;
		int viewZ = MathHelper.floor_double(z) - 8;

		int xSizeInBlocks = renderers.countChunksX * 16;
		int ySizeInBlocks = renderers.countChunksY * 16;
		int zSizeInBlocks = renderers.countChunksZ * 16;

		for (int xIndex = 0; xIndex < renderers.countChunksX; xIndex++) {
			//getRendererBlockCoord
			int blockX = renderers.func_178157_a(viewX, xSizeInBlocks, xIndex);

			for (int yIndex = 0; yIndex < renderers.countChunksY; yIndex++) {
				int blockY = renderers.func_178157_a(viewY, ySizeInBlocks, yIndex);

				for (int zIndex = 0; zIndex < renderers.countChunksZ; zIndex++) {
					int blockZ = renderers.func_178157_a(viewZ, zSizeInBlocks, zIndex);

					// get the renderer
					int rendererIndex = (zIndex * renderers.countChunksY + yIndex) * renderers.countChunksX
							+ xIndex;
					RenderChunk renderer = renderers.renderChunks[rendererIndex];

					// update the position if needed
					BlockPos oldPos = renderer.getPosition();
					if (oldPos.getX() != blockX || oldPos.getY() != blockY || oldPos.getZ() != blockZ) {
						renderer.setPosition(new BlockPos(blockX, blockY, blockZ));
					}
				}
			}
		}
		return true;
	}

	//@ClientOnly
	public RenderChunk getChunkSectionRenderer(ViewFrustum renderers, BlockPos pos) {

		if (!isTallWorld(renderers.world)) {
			return null;
		}
		// treat the y dimension the same as all the rest
		int x = MathHelper.bucketInt(pos.getX(), 16);
		int y = MathHelper.bucketInt(pos.getY(), 16);
		int z = MathHelper.bucketInt(pos.getZ(), 16);
		x %= renderers.countChunksX;
		if (x < 0) {
			x += renderers.countChunksX;
		}
		y %= renderers.countChunksY;
		if (y < 0) {
			y += renderers.countChunksY;
		}
		z %= renderers.countChunksZ;
		if (z < 0) {
			z += renderers.countChunksZ;
		}
		final int index = (z * renderers.countChunksY + y) * renderers.countChunksX + x;
		return renderers.renderChunks[index];
	}

	//@ClientOnly
	public boolean initChunkSectionRendererCounts(ViewFrustum renderers, int viewDistance) {

		if (!isTallWorld(renderers.world)) {
			return false;
		}

		// treat the y dimension the same as all the rest
		int size = viewDistance * 2 + 1;
		renderers.countChunksX = size;
		renderers.countChunksY = size;
		renderers.countChunksZ = size;
		return true;
	}

	//In 1.8.11 MCP mappings it's called getRenderChunkOffset
	//it actually makes some sort of twisted sense... (get RenderChunk (for) offset (offset == Facing)?)
	//@ClientOnly

	public RenderChunk getChunkSectionRendererNeighbor(RenderGlobal worldRenderer, BlockPos poseye,
	                                                   BlockPos posChunk, EnumFacing facing) {
/*
		if (!isTallWorld(worldRenderer.theWorld)) {
			return null;
		}

		// treat the y dimension the same as all the rest
		final BlockPos neighborPos = posChunk.offset(facing);
		if (MathHelper.abs(poseye.getX() - neighborPos.getX()) > worldRenderer.renderDistanceChunks * 16) {
			return null;
		}
		if (MathHelper.abs(poseye.getY() - neighborPos.getY()) > worldRenderer.renderDistanceChunks * 16) {
			return null;
		}
		if (MathHelper.abs(poseye.getZ() - neighborPos.getZ()) > worldRenderer.renderDistanceChunks * 16) {
			return null;
		}
		return worldRenderer.viewFrustum.getRenderChunk(neighborPos);*/ return null;
	}

	public ClassInheritanceMultiMap getEntityStore(Chunk chunk, int chunkSectionIndex) {
		if (chunk instanceof Column) {
			Column column = (Column) chunk;
			int cubeY = chunkSectionIndex;

			Cube cube = column.getCube(cubeY);
			if (cube != null) {
				return cube.getEntityContainer().getEntitySet();
			} else {
				return m_emptyEntitySet;
			}
		}
		return null;
	}

	public void onServerStop() {
		WorldServerContext.clear();
	}

	//@ClientOnly
	public void unloadClientWorld() {
		WorldClientContext.clear();
	}
}
