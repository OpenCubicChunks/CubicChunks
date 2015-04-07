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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import net.minecraft.entity.CreatureTypes;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IProgressBar;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ServerChunkCache;

import org.slf4j.Logger;

import com.google.common.collect.Maps;

import cubicchunks.generator.ColumnGenerator;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cuchaz.m3l.util.Logging;

public class ServerCubeCache extends ServerChunkCache implements ICubeCache {
	
	private static final Logger log = Logging.getLogger();
	
	public static final int WorldSpawnChunkDistance = 12;
	
	private WorldServer worldServer;
	private CubeIO cubeIO;
	private ColumnGenerator columnGenerator;
	private HashMap<Long,Column> loadedColumns;
	private BlankColumn blankColumn;
	private Queue<Long> cubesToUnload;
	
	public ServerCubeCache(WorldServer worldServer) {
		super(worldServer, null, null);
		
		this.worldServer = worldServer;
		this.cubeIO = new CubeIO(worldServer.getSaveHandler().getSaveFile(), worldServer.dimension);
		this.columnGenerator = new ColumnGenerator(worldServer);
		this.loadedColumns = Maps.newHashMap();
		this.blankColumn = new BlankColumn(worldServer, 0, 0);
		this.cubesToUnload = new ArrayDeque<Long>();
	}
	
	@Override
	public boolean chunkExists(int cubeX, int cubeZ) {
		return this.loadedColumns.containsKey(AddressTools.getAddress(cubeX, cubeZ));
	}
	
	@Override
	public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
		
		// is the column loaded?
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);
		Column column = this.loadedColumns.get(columnAddress);
		if (column == null) {
			return false;
		}
		
		// is the cube loaded?
		return column.getCube(cubeY) != null;
	}
	
	@Override
	public Column loadChunk(int cubeX, int cubeZ) {
		// in the tall worlds scheme, load and provide columns/chunks are semantically the same thing
		// but load/provide cube do actually do different things
		return getChunk(cubeX, cubeZ);
	}
	
	@Override
	public Column getColumn(int columnX, int columnZ) {
		return getChunk(columnX, columnZ);
	}
	
	@Override
	public Column getChunk(int cubeX, int cubeZ) {
		// check for the column
		Column column = this.loadedColumns.get(AddressTools.getAddress(cubeX, cubeZ));
		if (column != null) {
			return column;
		}
		
		return this.blankColumn;
	}
	
	@Override
	public Cube getCube(int cubeX, int cubeY, int cubeZ) {
		// is the column loaded?
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);
		Column column = this.loadedColumns.get(columnAddress);
		if (column == null) {
			return null;
		}
		
		return column.getCube(cubeY);
	}
	
	public void loadCubeAndNeighbors(int cubeX, int cubeY, int cubeZ) {
		// load the requested cube
		loadCube(cubeX, cubeY, cubeZ);
		
		// load the neighbors
		loadCube(cubeX - 1, cubeY - 1, cubeZ - 1);
		loadCube(cubeX - 1, cubeY - 1, cubeZ + 0);
		loadCube(cubeX - 1, cubeY - 1, cubeZ + 1);
		loadCube(cubeX + 0, cubeY - 1, cubeZ - 1);
		loadCube(cubeX + 0, cubeY - 1, cubeZ + 0);
		loadCube(cubeX + 0, cubeY - 1, cubeZ + 1);
		loadCube(cubeX + 1, cubeY - 1, cubeZ - 1);
		loadCube(cubeX + 1, cubeY - 1, cubeZ + 0);
		loadCube(cubeX + 1, cubeY - 1, cubeZ + 1);
		
		loadCube(cubeX - 1, cubeY + 0, cubeZ - 1);
		loadCube(cubeX - 1, cubeY + 0, cubeZ + 0);
		loadCube(cubeX - 1, cubeY + 0, cubeZ + 1);
		loadCube(cubeX + 0, cubeY + 0, cubeZ - 1);
		loadCube(cubeX + 0, cubeY + 0, cubeZ + 1);
		loadCube(cubeX + 1, cubeY + 0, cubeZ - 1);
		loadCube(cubeX + 1, cubeY + 0, cubeZ + 0);
		loadCube(cubeX + 1, cubeY + 0, cubeZ + 1);
		
		loadCube(cubeX - 1, cubeY + 1, cubeZ - 1);
		loadCube(cubeX - 1, cubeY + 1, cubeZ + 0);
		loadCube(cubeX - 1, cubeY + 1, cubeZ + 1);
		loadCube(cubeX + 0, cubeY + 1, cubeZ - 1);
		loadCube(cubeX + 0, cubeY + 1, cubeZ + 0);
		loadCube(cubeX + 0, cubeY + 1, cubeZ + 1);
		loadCube(cubeX + 1, cubeY + 1, cubeZ - 1);
		loadCube(cubeX + 1, cubeY + 1, cubeZ + 0);
		loadCube(cubeX + 1, cubeY + 1, cubeZ + 1);
	}
	
	public void loadCube(int cubeX, int cubeY, int cubeZ) {
		
		long cubeAddress = AddressTools.getAddress(cubeX, cubeY, cubeZ);
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);
		
		// step 1: get a column
		
		// is the column already loaded?
		Column column = this.loadedColumns.get(columnAddress);
		if (column == null) {
			// try loading it
			try {
				column = this.cubeIO.loadColumn(this.worldServer, cubeX, cubeZ);
			} catch (IOException ex) {
				log.error("Unable to load column ({},{})", cubeX, cubeZ, ex);
				return;
			}
			
			if (column == null) {
				// there wasn't a column, generate a new one
				column = this.columnGenerator.generateColumn(cubeX, cubeZ);
			} else {
				// the column was loaded
				column.setLastSaveTime(this.worldServer.getGameTime());
			}
		}
		assert (column != null);
		
		// step 2: get a cube
		
		// is the cube already loaded?
		Cube cube = column.getCube(cubeY);
		if (cube != null) {
			return;
		}
		
		// try to load the cube
		try {
			cube = this.cubeIO.loadCubeAndAddToColumn(this.worldServer, column, cubeAddress);
		} catch (IOException ex) {
			log.error("Unable to load cube ({},{},{})", cubeX, cubeY, cubeZ, ex);
			return;
		}
		
		if (cube == null) {
			// start the cube generation process with an empty cube
			cube = column.getOrCreateCube(cubeY, true);
			cube.setGeneratorStage(GeneratorStage.getFirstStage());
		}
		
		if (!cube.getGeneratorStage().isLastStage()) {
			// queue the cube to finish generation
			WorldServerContext.get(this.worldServer).getGeneratorPipeline().generate(cube);
		} else {
			// queue the cube for re-lighting
			WorldServerContext.get(this.worldServer).getLightingManager().queueFirstLightCalculation(cubeAddress);
		}
		
		// add the column to the cache
		this.loadedColumns.put(columnAddress, column);
		
		// init the column
		if (!column.isChunkLoaded()) {
			column.onChunkLoad();
		}
		column.setTerrainPopulated(true);
		column.resetPrecipitationHeight();
		
		// init the cube
		cube.onLoad();
	}
	
	@Override
	public void unloadChunk(int cubeX, int cubeZ) {
		// don't call this, unload cubes instead
		throw new UnsupportedOperationException();
	}
	
	public void unloadCube(Cube cube) {
		// NOTE: this is the main unload method for block data!
		unloadCube(cube.getX(), cube.getY(), cube.getZ());
	}
	
	@Override
	public void unloadCube(int cubeX, int cubeY, int cubeZ) {
		
		// don't unload cubes near the spawn
		if (cubeIsNearSpawn(cubeX, cubeY, cubeZ)) {
			return;
		}
		
		// queue the cube for unloading
		this.cubesToUnload.add(AddressTools.getAddress(cubeX, cubeY, cubeZ));
	}
	
	@Override
	public void unloadAllChunks() {
		// unload all the cubes in the columns
		for (Column column : this.loadedColumns.values()) {
			for (Cube cube : column.getCubes()) {
				this.cubesToUnload.add(cube.getAddress());
			}
		}
	}
	
	@Override
	public boolean tick() {
		
		// NOTE: the return value is completely ignored
		
		if (this.worldServer.disableSaving) {
			return false;
		}
		
		final int MaxNumToUnload = 400;
		
		// unload cubes
		for (int i = 0; i < MaxNumToUnload && !this.cubesToUnload.isEmpty(); i++) {
			long cubeAddress = this.cubesToUnload.poll();
			long columnAddress = AddressTools.getAddress(AddressTools.getX(cubeAddress), AddressTools.getZ(cubeAddress));
			
			// get the cube
			Column column = this.loadedColumns.get(columnAddress);
			if (column == null) {
				// already unloaded
				continue;
			}
			
			// unload the cube
			int cubeY = AddressTools.getY(cubeAddress);
			Cube cube = column.removeCube(cubeY);
			if (cube != null) {
				// tell the cube it has been unloaded
				cube.onUnload();
				
				// save the cube
				this.cubeIO.saveCube(cube);
			}
			
			// unload empty columns
			if (!column.hasCubes()) {
				column.onChunkLoad();
				this.loadedColumns.remove(columnAddress);
				this.cubeIO.saveColumn(column);
			}
		}
		
		return false;
	}
	
	@Override
	public boolean saveAllChunks(boolean alwaysTrue, IProgressBar progress) {
		
		for (Column column : this.loadedColumns.values()) {
			// save the column
			if (column.needsSaving(alwaysTrue)) {
				this.cubeIO.saveColumn(column);
			}
			
			// save the cubes
			for (Cube cube : column.getCubes()) {
				if (cube.needsSaving()) {
					this.cubeIO.saveCube(cube);
				}
			}
		}
		
		return true;
	}
	
	@Override
	public String getName() {
		return "ServerCubeCache: " + this.loadedColumns.size() + " columns, Unload: " + this.cubesToUnload.size() + " cubes";
	}
	
	@Override
	public int getLoadedChunkCount() {
		return this.loadedColumns.size();
	}
	
	@Override
    public List<Biome.SpawnMob> getSpawnableAtPos(final CreatureTypes a1, final BlockPos a2) {
		return null;
	}
	
	private boolean cubeIsNearSpawn(int cubeX, int cubeY, int cubeZ) {
		
		if (!this.worldServer.dimension.canRespawnHere()) {
			// no spawn points
			return false;
		}
		
		BlockPos spawnPoint = this.worldServer.getSpawnPoint();
		int spawnCubeX = Coords.blockToCube(spawnPoint.getX());
		int spawnCubeY = Coords.blockToCube(spawnPoint.getY());
		int spawnCubeZ = Coords.blockToCube(spawnPoint.getZ());
		int dx = Math.abs(spawnCubeX - cubeX);
		int dy = Math.abs(spawnCubeY - cubeY);
		int dz = Math.abs(spawnCubeZ - cubeZ);
		return dx <= WorldSpawnChunkDistance && dy <= WorldSpawnChunkDistance && dz <= WorldSpawnChunkDistance;
	}
}
