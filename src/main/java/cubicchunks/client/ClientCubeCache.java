/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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

import cubicchunks.generator.GeneratorStage;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class ClientCubeCache extends ChunkProviderClient implements ICubeCache {

	private World world;
	private BlankColumn blankColumn;
	private Cube blankCube;

	public ClientCubeCache(World world) {
		super(world);

		this.world = world;
		this.blankColumn = new BlankColumn(world, 0, 0);
		this.blankCube = new BlankCube(world, blankColumn);
	}

	@Override
	public Column loadChunk(int cubeX, int cubeZ) {

		// is this chunk already loaded?
		Column column = (Column)this.chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ));
		if (column != null) {
			return column;
		}

		// make a new one
		column = new Column(this.world, cubeX, cubeZ);

		this.chunkMapping.add(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ), column);
		this.chunkListing.add(column);

		column.setChunkLoaded(true);
		return column;
	}

	@Override
	public void unloadCube(int cubeX, int cubeY, int cubeZ) {
		
		// is this column loaded?
		Column column = (Column)this.chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ));
		if (column == null) {
			//TallWorldsMod.log.warn("Unloading cube from non-existing column: ({}, {}, {})", cubeX, cubeY, cubeZ);
			return;
		}
		
		// is this cube loaded?
		if (column.getCube(cubeY) == null) {
			//TallWorldsMod.log.warn("Unloading non-existing cube: ({}, {}, {})", cubeX, cubeY, cubeZ);
			return;
		}
		
		// unload the cube
		column.removeCube(cubeY);
	}
	
	public void unloadColumn(int columnX, int columnZ) {
		//unload even if not empty
		//server sends unload packets, it must be right.
		
		//TODO: Unload cubes before removing column?
		Column column = (Column) this.chunkMapping.remove(ChunkCoordIntPair.chunkXZ2Int(columnX, columnZ));
		this.chunkListing.remove(column);
	}

	@Override
	public Column getColumn(int columnX, int columnZ) {
		return provideChunk(columnX, columnZ);
	}

	@Override//I hope it was provideChunk
	public Column provideChunk(int cubeX, int cubeZ) {
		
		// is this chunk already loaded?
		Column column = (Column)this.chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(cubeX, cubeZ));
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
		Cube cube = getColumn(cubeX, cubeZ).getCube(cubeY);
		if(cube == null){
			return this.blankCube;
		}

		// cubes are always live on the client
		cube.setGeneratorStage(GeneratorStage.getLastStage());
		
		return cube;
	}
}
