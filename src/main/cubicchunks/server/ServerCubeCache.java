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

import com.google.common.collect.Maps;

import cubicchunks.CubeCache;
import cubicchunks.generator.ColumnGenerator;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.world.BlankColumn;
import cubicchunks.world.Column;
import cubicchunks.world.Cube;

public class ServerCubeCache extends ServerChunkCache implements CubeCache {
	
	private static final int WorldSpawnChunkDistance = 12;
	
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
		// TODO
		//m_columnGenerator = new ColumnGenerator(worldServer);
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
		
		/* TODO
		long cubeAddress = AddressTools.getAddress(cubeX, cubeY, cubeZ);
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);
		
		// step 1: get a column
		
		// is the column already loaded?
		Column column = m_loadedColumns.get(columnAddress);
		if (column == null) {
			// try loading it
			try {
				column = m_io.loadColumn(m_worldServer, cubeX, cubeZ);
			} catch (IOException ex) {
				log.error(String.format("Unable to load column (%d,%d)", cubeX, cubeZ), ex);
				return;
			}
			
			if (column == null) {
				// there wasn't a column, generate a new one
				column = m_columnGenerator.generateColumn(cubeX, cubeZ);
			} else {
				// the column was loaded
				column.lastSaveTime = m_worldServer.getTotalWorldTime();
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
			cube = m_io.loadCubeAndAddToColumn(m_worldServer, column, cubeAddress);
		} catch (IOException ex) {
			log.error(String.format("Unable to load cube (%d,%d,%d)", cubeX, cubeY, cubeZ), ex);
			return;
		}
		
		if (cube == null) {
			// start the cube generation process with an empty cube
			cube = column.getOrCreateCube(cubeY, true);
			cube.setGeneratorStage(GeneratorStage.getFirstStage());
		}
		
		if (!cube.getGeneratorStage().isLastStage()) {
			// queue the cube to finish generation
			m_worldServer.getGeneratorPipeline().generate(cube);
		} else {
			// queue the cube for re-lighting
			m_worldServer.getLightingManager().queueFirstLightCalculation(cubeAddress);
		}
		
		// add the column to the cache
		m_loadedColumns.put(columnAddress, column);
		
		// init the column
		if (!column.isChunkLoaded) {
			column.onChunkLoad();
		}
		column.isTerrainPopulated = true;
		column.resetPrecipitationHeight();
		
		// init the cube
		cube.onLoad();
		*/
	}
	
	@Override
	public void unloadChunk(int cubeX, int cubeZ) {
		throw new UnsupportedOperationException();
	}
	
	public void unloadCube(Cube cube) {
		// NOTE: this is the main unload method for block data!
		unloadCube(cube.getX(), cube.getY(), cube.getZ());
	}
	
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
	public boolean unloadQueuedChunks() {
		
		/* TODO
		// NOTE: the return value is completely ignored
		
		// don't unload if we're saving
		if (m_worldServer.levelSaving) {
			return false;
		}
		
		final int MaxNumToUnload = 400;
		
		// unload cubes
		for (int i = 0; i < MaxNumToUnload && !m_cubesToUnload.isEmpty(); i++) {
			long cubeAddress = m_cubesToUnload.poll();
			long columnAddress = AddressTools.getAddress(AddressTools.getX(cubeAddress), AddressTools.getZ(cubeAddress));
			
			// get the cube
			Column column = m_loadedColumns.get(columnAddress);
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
				m_io.saveCube(cube);
			}
			
			// unload empty columns
			if (!column.hasCubes()) {
				column.onChunkLoad();
				m_loadedColumns.remove(columnAddress);
				m_io.saveColumn(column);
			}
		}
		*/
		
		return false;
	}
	
	@Override
	public boolean saveAllChunks(boolean alwaysTrue, IProgressBar progress) {
		
		/* TODO
		for (Column column : m_loadedColumns.values()) {
			// save the column
			if (column.needsSaving(alwaysTrue)) {
				m_io.saveColumn(column);
			}
			
			// save the cubes
			for (Cube cube : column.getCubes()) {
				if (cube.needsSaving()) {
					m_io.saveCube(cube);
				}
			}
		}
		*/
		
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
