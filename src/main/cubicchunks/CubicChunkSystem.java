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
package cubicchunks;

import java.io.IOException;
import java.util.Random;

import net.minecraft.client.renderers.ChunkSectionRenderer;
import net.minecraft.client.renderers.ChunkSectionRenderers;
import net.minecraft.client.renderers.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.main.Minecraft;
import net.minecraft.network.play.packet.clientbound.PacketChunkData.EncodedChunk;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ClientChunkCache;
import net.minecraft.world.gen.ServerChunkCache;
import cubicchunks.client.ClientCubeCache;
import cubicchunks.client.WorldClientContext;
import cubicchunks.generator.GeneratorPipeline;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.lighting.LightingManager;
import cubicchunks.server.CubePlayerManager;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.WorldServerContext;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.world.WorldContext;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cuchaz.m3l.api.chunks.ChunkSystem;
import cuchaz.m3l.util.Util;

public class CubicChunkSystem implements ChunkSystem {
	
	private EntitySet<Entity> m_emptyEntitySet;
	
	public CubicChunkSystem() {
		m_emptyEntitySet = new EntitySet<Entity>(Entity.class);
	}
	
	@Override
	public ServerChunkCache getServerChunkCache(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			ServerCubeCache serverCubeCache = new ServerCubeCache(worldServer);
			WorldServerContext.put(worldServer, new WorldServerContext(worldServer, serverCubeCache));
			return serverCubeCache;
		}
		return null;
	}
	
	@Override
	public ClientChunkCache getClientChunkCache(WorldClient worldClient) {
		if (isTallWorld(worldClient)) {
			ClientCubeCache clientCubeCache = new ClientCubeCache(worldClient);
			WorldClientContext.put(worldClient, new WorldClientContext(worldClient, clientCubeCache));
			return clientCubeCache;
		}
		return null;
	}
	
	@Override
	public BiomeManager getBiomeManager(World world) {
		
		/* TEMP: disable fancy generation
		// for now, only muck with the overworld
		if (world.dimension.getId() == 0) {
			DimensionType dimensionType = world.getWorldInfo().getDimensionType();
			if (dimensionType != DimensionType.FLAT && dimensionType != DimensionType.DEBUG_ALL_BLOCK_STATES) {
				return new CCBiomeManager(world);
			}
		}
		*/
		
		return null;
	}

	@Override
	public Integer getMinBlockY(World world) {
		if (isTallWorld(world)) {
			return AddressTools.MinY;
		}
		return null;
	}

	@Override
	public Integer getMaxBlockY(World world) {
		if (isTallWorld(world)) {
			return AddressTools.MaxY;
		}
		return null;
	}

	@Override
	public PlayerManager getPlayerManager(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			return new CubePlayerManager(worldServer);
		}
		return null;
	}

	@Override
	public void processChunkLoadQueue(EntityPlayerMP player) {
		WorldServer worldServer = (WorldServer)player.getWorld();
		if (isTallWorld(worldServer)) {
			CubePlayerManager playerManager = (CubePlayerManager)worldServer.getPlayerManager();
			playerManager.processCubeLoadQueue(player);
		}
	}
	
	private boolean isTallWorld(World world) {
		// for now, only tall-ify the overworld
		return world.dimension.getId() == 0;
	}
	
	@Override
	public void onWorldClientTick(WorldClient worldClient) {
		if (isTallWorld(worldClient)) {
			WorldClientContext context = WorldClientContext.get(worldClient);
			
			// tick all the things!
			worldClient.profiler.startSection("lightingEngine");
			context.getLightingManager().tick();
			worldClient.profiler.endSection();
		}
	}

	@Override
	public void onWorldServerTick(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			WorldServerContext context = WorldServerContext.get(worldServer);
			
			// tick all the things!
			worldServer.profiler.startSection("generatorPipeline");
			context.getGeneratorPipeline().tick();
			worldServer.profiler.endSection();
			
			worldServer.profiler.startSection("lightingEngine");
			context.getLightingManager().tick();
			worldServer.profiler.endSection();
			
			worldServer.profiler.startSection("randomCubeTicks");
			ServerCubeCache cubeCache = context.getCubeCache();
			for (ChunkCoordIntPair coords : worldServer.activeChunkSet) {
				Column column = cubeCache.getChunk(coords.chunkX, coords.chunkZ);
				column.doRandomTicks();
			}
			worldServer.profiler.endSection();
		}
	}

	@Override
	public Integer getRandomBlockYForMobSpawnAttempt(Random rand, int upper, World world, int cubeX, int cubeZ) {
		// need to return a random blockY between the "bottom" of the world and upper
		// TEMP: well... we don't really have a bottom, so just clamp the val to [15,upper] for now
		return rand.nextInt(Math.max(15, upper));
	}

	@Override
	public EncodedChunk encodeChunk(Chunk chunk, boolean isFirstTime, boolean hasSky, int sectionFlags) {
		if (isTallWorld(chunk.getWorld())) {
			if (chunk instanceof Column) {
				Column column = (Column)chunk;
				try {
					return column.encode(isFirstTime, hasSky, sectionFlags);
				} catch (IOException ex) {
					throw new Error(ex);
				}
			}
		}
		return null;
	}

	@Override
	public void generateWorld(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			WorldServerContext context = WorldServerContext.get(worldServer);
			ServerCubeCache serverCubeCache = context.getCubeCache();
			
			// load the cubes around the spawn point
			TallWorldsMod.log.info("Loading cubes for spawn...");
			final int Distance = 12;
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
				TallWorldsMod.log.info("Generating {} cubes for spawn at block ({},{},{}) cube ({},{},{})...",
					numCubesTotal,
					spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(),
					spawnCubeX, spawnCubeY, spawnCubeZ
				);
				pipeline.generateAll();
				long timeDiff = System.currentTimeMillis() - timeStart;
				TallWorldsMod.log.info("Done in {} ms", timeDiff);
			}
		}
	}
	
	@Override
	public boolean calculateSpawn(WorldServer worldServer, WorldSettings worldSettings) {
		
		if (!isTallWorld(worldServer)) {
			return false;
		}
		
		WorldServerContext context = WorldServerContext.get(worldServer);
		ServerCubeCache serverCubeCache = context.getCubeCache();
		
		// NOTE: this is called inside the world constructor
		// this is apparently called before the world is generated
		// we'll have to do our own generation to find the spawn point
		
		if (!worldServer.dimension.canRespawnHere()) {
			worldServer.worldInfo.setSpawnPoint(BlockPos.ZEROED.above());
			return false;
		}
		
		// pick a default fail-safe spawn point
		BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos(
			0,
			worldServer.dimension.getAverageGroundLevel(),
			0
		);
		
		Random rand = new Random(worldServer.getSeed());
		
		// defer to the column manager to find the x,z part of the spawn point
		BiomeManager biomeManager = worldServer.dimension.getBiomeManager();
		BlockPos spawnPosition = biomeManager.getRandomPositionInBiome(0, 0, 256, biomeManager.getSpawnableBiomes(), rand);
		if (spawnPosition != null) {
			spawnPos.x = spawnPosition.getX();
			spawnPos.z = spawnPosition.getZ();
		} else {
			TallWorldsMod.log.warn("Unable to find spawn biome");
		}
		
		TallWorldsMod.log.info("Searching for suitable spawn point...");
		
		// generate some world around the spawn x,z at sea level
		int spawnCubeX = Coords.blockToCube(spawnPos.getX());
		int spawnCubeY = Coords.blockToCube(worldServer.getSeaLevel());
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
		for (int i = 0; i < 1000 && !worldServer.dimension.canCoordinateBeSpawn(spawnPos.getX(), spawnPos.getZ()); i++) {
			spawnPos.x += Util.randRange(rand, -16, 16);
			spawnPos.z += Util.randRange(rand, -16, 16);
		}
		
		// save the spawn point
		worldServer.worldInfo.setSpawnPoint(spawnPos);
		TallWorldsMod.log.info("Found spawn point at ({},{},{})", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
		
		if (worldSettings.isBonusChestEnabled()) {
			worldServer.generateBonusChests();
		}
		
		// spawn point calculated successfully
		return true;
	}

	@Override
	public Integer getSeaLevel(World world) {
		if (isTallWorld(world)) {
			// tall worlds will eventually have the sea level at 0
			// for now, let's just do the vanilla thing
			return 63;
		}
		return null;
	}
	
	@Override
	public Double getHorizonLevel(World world) {
		if (isTallWorld(world)) {
			// tall worlds will eventually have the horizon level at 0
			// for now, let's just do the vanilla thing
			return 63.0;
		}
		return null;
	}

	@Override
	public Boolean updateLightingAt(World world, BlockPos pos) {
		if (isTallWorld(world)) {
			LightingManager lightingManager = WorldContext.get(world).getLightingManager();
			boolean success = false;
			if (!world.dimension.hasNoSky()) {
				success |= lightingManager.computeDiffuseLighting(pos, LightType.SKY);
			}
			success |= lightingManager.computeDiffuseLighting(pos, LightType.BLOCK);
			return success;
		}
		return null;
	}

	@Override
	public Boolean checkBlockRangeIsInWorld(World world, int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, boolean allowEmptyChunks) {
		if (isTallWorld(world)) {
			WorldContext context = WorldContext.get(world);
			context.blocksExist(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ, allowEmptyChunks, GeneratorStage.Live);
		}
		return null;
	}

	@Override
	public Boolean checkEntityIsInWorld(World world, Entity entity, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ, boolean allowEmptyChunks) {
		if (isTallWorld(world)) {
			WorldContext context = WorldContext.get(world);
			
			final int blockDist = 32;
			int blockY = MathHelper.floor(entity.yPos);
			context.blocksExist(minBlockX, blockY - blockDist, minBlockZ, maxBlockX, blockY + blockDist, maxBlockZ, allowEmptyChunks, GeneratorStage.Live);
		}
		return null;
	}

	@Override
	public boolean setChunkSectionRendererPositions(ChunkSectionRenderers renderers) {
		
		if (!isTallWorld(renderers.world)) {
			return false;
		}
		
		// get the position of the view entity
		Entity view = Minecraft.instance.getViewEntity();
		
		// treat the y dimension the same as all the rest
		int viewX = MathHelper.floor(view.xPos) - 8;
		int viewY = MathHelper.floor(view.yPos) - 8;
		int viewZ = MathHelper.floor(view.zPos) - 8;
		
		int xSizeInBlocks = renderers.numXBlockRenderers*16;
		int ySizeInBlocks = renderers.numYBlockRenderers*16;
		int zSizeInBlocks = renderers.numZBlockRenderers*16;
		
		for (int xIndex=0; xIndex<renderers.numXBlockRenderers; xIndex++) {
			int blockX = renderers.getRendererBlockCoord(viewX, xSizeInBlocks, xIndex);
			
			for (int yIndex=0; yIndex<renderers.numYBlockRenderers; yIndex++) {
				int blockY = renderers.getRendererBlockCoord(viewY, ySizeInBlocks, yIndex);
				
				for (int zIndex=0; zIndex<renderers.numZBlockRenderers; zIndex++) {	
					int blockZ = renderers.getRendererBlockCoord(viewZ, zSizeInBlocks, zIndex);
					
					// get the renderer
					int rendererIndex = (zIndex * renderers.numYBlockRenderers + yIndex) * renderers.numXBlockRenderers + xIndex;
					ChunkSectionRenderer renderer = renderers.blockRenderers[rendererIndex];
					
					// update the position if needed
					BlockPos oldPos = renderer.getBlockPos();
					if (oldPos.getX() != blockX || oldPos.getY() != blockY || oldPos.getZ() != blockZ) {
						renderer.setBlockPos(new BlockPos(blockX, blockY, blockZ));
					}
				}
			}
		}
		
		return true;
	}

	@Override
	public ChunkSectionRenderer getChunkSectionRenderer(ChunkSectionRenderers renderers, BlockPos pos) {
		
		if (!isTallWorld(renderers.world)) {
			return null;
		}
		
		// treat the y dimension the same as all the rest
		int x = MathHelper.intFloorDiv(pos.getX(), 16);
		int y = MathHelper.intFloorDiv(pos.getY(), 16);
		int z = MathHelper.intFloorDiv(pos.getZ(), 16);
		x %= renderers.numXBlockRenderers;
		if (x < 0) {
			x += renderers.numXBlockRenderers;
		}
		y %= renderers.numYBlockRenderers;
		if (y < 0) {
			y += renderers.numYBlockRenderers;
		}
		z %= renderers.numZBlockRenderers;
		if (z < 0) {
			z += renderers.numZBlockRenderers;
		}
		final int index = (z * renderers.numYBlockRenderers + y) * renderers.numXBlockRenderers + x;
		return renderers.blockRenderers[index];
	}

	@Override
	public boolean initChunkSectionRendererCounts(ChunkSectionRenderers renderers, int viewDistance) {
		
		if (!isTallWorld(renderers.world)) {
			return false;
		}

		// treat the y dimension the same as all the rest
		int size = viewDistance*2 + 1;
		renderers.numXBlockRenderers = size;
		renderers.numYBlockRenderers = size;
		renderers.numZBlockRenderers = size;
		return true;
	}

	@Override
	public ChunkSectionRenderer getChunkSectionRendererNeighbor(WorldRenderer worldRenderer, BlockPos pos, ChunkSectionRenderer chunkSectionRenderer, Facing facing) {
		
		if (!isTallWorld(worldRenderer.worldClient)) {
			return null;
		}
		
		// treat the y dimension the same as all the rest
		final BlockPos neighborPos = chunkSectionRenderer.getBlockPosNeighbor(facing);
		if (MathHelper.abs(pos.getX() - neighborPos.getX()) > worldRenderer.renderDistance*16) {
			return null;
		}
		if (MathHelper.abs(pos.getY() - neighborPos.getY()) > worldRenderer.renderDistance*16) {
			return null;
		}
		if (MathHelper.abs(pos.getZ() - neighborPos.getZ()) > worldRenderer.renderDistance*16) {
			return null;
		}
		return worldRenderer.chunkSectionRenderers.getRenderer(neighborPos);
	}

	@Override
	public EntitySet<Entity> getEntityStore(Chunk chunk, int chunkSectionIndex) {
		if (chunk instanceof Column) {
			Column column = (Column)chunk;
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
}
