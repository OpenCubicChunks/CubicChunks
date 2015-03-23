/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.client;

import cubicchunks.CubeCache;
import cubicchunks.accessors.ChunkProviderClientAccessor;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.world.BlankColumn;
import cubicchunks.world.Column;
import cubicchunks.world.Cube;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public class CubeProviderClient extends ChunkProviderClient implements CubeCache {
	
	private World m_world;
	private BlankColumn m_blankColumn;
	
	public CubeProviderClient(World world) {
		super(world);
		
		m_world = world;
		m_blankColumn = new BlankColumn(world, 0, 0);
	}
	
	@Override
	public Column loadChunk(int cubeX, int cubeZ) {
		// is this chunk already loaded?
		LongHashMap chunkMapping = ChunkProviderClientAccessor.getChunkMapping(this);
		Column column = (Column)chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ));
		if (column != null) {
			return column;
		}
		
		// make a new one
		column = new Column(m_world, cubeX, cubeZ);
		
		chunkMapping.add(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ), column);
		ChunkProviderClientAccessor.getChunkListing(this).add(column);
		
		column.isChunkLoaded = true;
		return column;
	}
	
	@Override
	public Column provideChunk(int cubeX, int cubeZ) {
		// is this chunk already loaded?
		LongHashMap chunkMapping = ChunkProviderClientAccessor.getChunkMapping(this);
		Column column = (Column)chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ));
		if (column != null) {
			return column;
		}
		
		return m_blankColumn;
	}
	
	@Override
	public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
		// cubes always exist on the client
		return true;
	}
	
	@Override
	public Cube provideCube(int cubeX, int cubeY, int cubeZ) {
		Cube cube = loadChunk(cubeX, cubeZ).getOrCreateCube(cubeY, false);
		
		// cubes are always live on the client
		cube.setGeneratorStage(GeneratorStage.getLastStage());
		
		return cube;
	}
}
