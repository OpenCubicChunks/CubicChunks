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
package cubicchunks.client;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.gen.ClientChunkCache;
import cubicchunks.CubeCache;
import cubicchunks.accessors.ChunkProviderClientAccessor;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.world.BlankColumn;
import cubicchunks.world.Column;
import cubicchunks.world.Cube;

public class CubeProviderClient extends ClientChunkCache implements CubeCache {
	
	private World world;
	private BlankColumn blankColumn;
	
	public CubeProviderClient(World world) {
		super(world);
		
		this.world = world;
		this.blankColumn = new BlankColumn(world, 0, 0);
	}
	
	@Override
	public Column loadChunk(int cubeX, int cubeZ) {
		// is this chunk already loaded?
		LongHashMap chunkMapping = (LongHashMap) ChunkProviderClientAccessor.getChunkMapping(this);
		Column column = (Column)chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ));
		if (column != null) {
			return column;
		}
		
		// make a new one
		column = new Column(this.world, cubeX, cubeZ);
		
		chunkMapping.add(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ), column);
		ChunkProviderClientAccessor.getChunkListing(this).add(column);
		
		column.setChunkLoaded(true);
		return column;
	}
	
	@Override
	public Column getChunk(int cubeX, int cubeZ) {
		// is this chunk already loaded?
		LongHashMap chunkMapping = ChunkProviderClientAccessor.getChunkMapping(this);
		Column column = (Column)chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ));
		if (column != null) {
			return column;
		}
		
		return this.blankColumn;
	}
	
	@Override
	public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
		// cubes always exist on the client
		return true;
	}
	
	@Override
	public Cube getCube(int cubeX, int cubeY, int cubeZ) {
		Cube cube = loadChunk(cubeX, cubeZ).getOrCreateCube(cubeY, false);
		
		// cubes are always live on the client
		cube.setGeneratorStage(GeneratorStage.getLastStage());
		
		return cube;
	}
}
