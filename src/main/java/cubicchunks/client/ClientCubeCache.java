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
package cubicchunks.client;

import javax.annotation.Nullable;

import cubicchunks.util.CubeCoords;
import cubicchunks.util.ReflectionUtil;
import cubicchunks.world.IColumnProvider;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

//TODO: break off ICubeCache
public class ClientCubeCache extends ChunkProviderClient implements ICubeCache, IColumnProvider {

	private ICubicWorldClient world;
	//private Cube blankCube;

	public ClientCubeCache(ICubicWorldClient world) {
		super((World) world);

		this.world = world;
		ReflectionUtil.setFieldValueSrg(this, "field_73238_a", new BlankColumn(world, 0, 0));
		//this.blankCube = new BlankCube(world, (Column)blankChunk);
	}
	
	@Override
	@Nullable
    public Column getLoadedChunk(int x, int z)
    {
        return (Column)super.getLoadedChunk(x, z);
    }
	
	@Override
	public Column provideChunk(int x, int z){
		return (Column)super.provideChunk(x, z);
	}

	@Override
	public Column loadChunk(int cubeX, int cubeZ) {
		Column column = new Column(this.world, cubeX, cubeZ);         // make a new one
		this.chunkMapping.put(ChunkPos.asLong(cubeX, cubeZ), column); // add it to the cache

		// fire a forge event... make mods happy :)
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(column));
		
		column.setChunkLoaded(true);
		return column;
	}

	
	//===========================
	//========Cube stuff=========
	//===========================
	
	@Override
	public void unloadCube(Cube cube) {
		cube.getColumn().removeCube(cube.getY());
	}

	@Override
	public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
		// cubes always exist on the client
		return true;
	}

	@Override
	public boolean cubeExists(CubeCoords coords) {
		return this.cubeExists(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
	}

	@Override
	public Cube getCube(int cubeX, int cubeY, int cubeZ) {
		return provideChunk(cubeX, cubeZ).getCube(cubeY);
	}

	public Cube getCube(CubeCoords coords) {
		return this.getCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
	}

	@Override
	public String makeString() {
		return "MultiplayerChunkCache: " + this.chunkMapping.values().stream().map(c->((Column)c).getLoadedCubes().size()).reduce((a,b)->a+b).orElse(-1) + "/" + this.chunkMapping.size();
	}
}
