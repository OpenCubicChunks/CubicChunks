/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
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
package cubicchunks.world;

import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import cubicchunks.client.WorldClientContext;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.lighting.LightingManager;
import cubicchunks.server.WorldServerContext;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.client.multiplayer.WorldClient;

public abstract class WorldContext {
	
	public static WorldContext get(World world) {
		
		//CodeAnnotation.startClientOnly();
		if (world instanceof WorldClient) {
			return WorldClientContext.get((WorldClient)world);
		}
		//CodeAnnotation.stopClientOnly();
		
		if (world instanceof WorldServer) {
			return WorldServerContext.get((WorldServer)world);
		}
		
		throw new Error("Unknown world type!");
	}

	private World m_world;
	private ICubeCache m_cubeCache;
	private LightingManager m_lightingManager;
	
	protected WorldContext(World world, ICubeCache cubeCache) {
		m_world = world;
		m_cubeCache = cubeCache;
		m_lightingManager = new LightingManager(world, cubeCache);
	}
	
	public World getWorld() {
		return m_world;
	}
	
	public ICubeCache getCubeCache() {
		return m_cubeCache;
	}
	
	public LightingManager getLightingManager() {
		return m_lightingManager;
	}
	
	public long getSpawnPointCubeAddress() {
		return AddressTools.getAddress(
			Coords.blockToCube(m_world.getWorldInfo().getSpawnX()),
			Coords.blockToCube(m_world.getWorldInfo().getSpawnY()),
			Coords.blockToCube(m_world.getWorldInfo().getSpawnZ())
		);
	}
	
	public boolean blocksExist(BlockPos pos, int dist, boolean allowEmptyCubes, GeneratorStage minStageAllowed) {
		return blocksExist(
			pos.getX() - dist, pos.getY() - dist, pos.getZ() - dist,
			pos.getX() + dist, pos.getY() + dist, pos.getZ() + dist,
			allowEmptyCubes,
			minStageAllowed
		);
	}
	
	public boolean blocksExist(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, boolean allowEmptyColumns, GeneratorStage minStageAllowed) {
		
		// convert block bounds to chunk bounds
		int minCubeX = Coords.blockToCube(minBlockX);
		int minCubeY = Coords.blockToCube(minBlockY);
		int minCubeZ = Coords.blockToCube(minBlockZ);
		int maxCubeX = Coords.blockToCube(maxBlockX);
		int maxCubeY = Coords.blockToCube(maxBlockY);
		int maxCubeZ = Coords.blockToCube(maxBlockZ);
		
		return cubesExist(minCubeX, minCubeY, minCubeZ, maxCubeX, maxCubeY, maxCubeZ, allowEmptyColumns, minStageAllowed);
	}
	
	public boolean cubeAndNeighborsExist(Cube cube, boolean allowEmptyCubes, GeneratorStage minStageAllowed) {
		return cubeAndNeighborsExist(cube.getX(), cube.getY(), cube.getZ(), allowEmptyCubes, minStageAllowed);
	}
	
	public boolean cubeAndNeighborsExist(int cubeX, int cubeY, int cubeZ, boolean allowEmptyCubes, GeneratorStage minStageAllowed) {
		// TODO: optimize this with loop unrolling
		return cubesExist(cubeX - 1, cubeY - 1, cubeZ - 1, cubeX + 1, cubeY + 1, cubeZ + 1, allowEmptyCubes, minStageAllowed);
	}
	
	public boolean cubesExist(int minCubeX, int minCubeY, int minCubeZ, int maxCubeX, int maxCubeY, int maxCubeZ, boolean allowEmptyColumns, GeneratorStage minStageAllowed) {
		for (int cubeX = minCubeX; cubeX <= maxCubeX; cubeX++) {
			for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
				for (int cubeZ = minCubeZ; cubeZ <= maxCubeZ; cubeZ++) {
					if (!m_cubeCache.cubeExists(cubeX, cubeY, cubeZ)) {
						return false;
					}
					Cube cube = m_cubeCache.getCube(cubeX, cubeY, cubeZ);
					Column column = cube.getColumn();
					if ((!allowEmptyColumns && column instanceof BlankColumn)
						|| (minStageAllowed != null && cube.getGeneratorStage().isLessThan(minStageAllowed))) {
						return false;
					}
				}
			}
		}
		return true;
	}
}
