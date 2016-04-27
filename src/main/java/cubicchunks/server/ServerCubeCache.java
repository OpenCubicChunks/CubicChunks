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
package cubicchunks.server;

import com.google.common.collect.Maps;
import cubicchunks.CubicChunks;
import cubicchunks.generator.ColumnGenerator;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import static cubicchunks.server.ServerCubeCache.LoadType.*;

/**
 * Thic is CubicChunks equivalent of ChunkProviderServer, it loads and unloads Cubes and Columns.
 *
 * There are a few necessary changes to the way vanilla methods work:
 *  * Because loading a Chunk (Column) doesn't make much sense with CubicChunks,
 *    all methods that load Chunks, actually load  an empry column with no blocks in it
 *    (there may be some entities that are not in any Cube yet).
 *  * dropChunk method is not supported. Columns are unloaded automatically when the last cube is unloaded
 *
 */
public class ServerCubeCache extends ChunkProviderServer implements ICubeCache {

	private static final Logger log = CubicChunks.LOGGER;

	public static final int WorldSpawnChunkDistance = 12; // highest render distance is 32

	private WorldServer worldServer;
	private CubeIO cubeIO;
	private ColumnGenerator columnGenerator;
	private HashMap<Long, Column> loadedColumns;
	private Queue<Long> cubesToUnload;

	public ServerCubeCache(WorldServer worldServer) {
		super(worldServer, worldServer.getSaveHandler().getChunkLoader(worldServer.provider), null);

		this.worldServer = worldServer;
		this.cubeIO = new CubeIO(worldServer);
		this.columnGenerator = new ColumnGenerator(worldServer);
		this.loadedColumns = Maps.newHashMap();
		this.cubesToUnload = new ArrayDeque<>();
	}

	@Override
	public List<Chunk> getLoadedChunks() {
		return this.loadedChunks;
	}

	@Override
	public void dropChunk(int cubeX, int cubeZ) {
		// don't call this, unload cubes instead
		throw new UnsupportedOperationException();
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

	/**
	 * Vanilla method, returns a Chunk (Column) only of it's already loaded.
	 * Same as getColumn(cubeX, cubeZ)
	 */
	@Override
	public Chunk getLoadedChunk(int cubeX, int cubeZ) {
		return this.getColumn(cubeX, cubeZ);
	}

	/**
	 * Loads Chunk (Column) if it can be loaded from disk, or returns already loaded one.
	 * Doesn't generate new Columns.
	 */
	@Override
	public Column loadChunk(int cubeX, int cubeZ) {
		return this.loadColumn(cubeX, cubeZ, LOAD_ONLY);
	}

	/**
	 * If this Column is already loaded - returns it.
	 * Loads from disk if possible, otherwise generated new Column.
	 */
	@Override
	public Column provideChunk(int cubeX, int cubeZ) {
		return loadChunk(cubeX, cubeZ, null);
	}

	@Override
	public Column loadChunk(int cubeX, int cubeZ, Runnable runnable) {
		Column column = this.loadColumn(cubeX, cubeZ, LOAD_OR_GENERATE);
		if(runnable == null) {
			return column;
		}
		runnable.run();
		return null;
	}

	@Override
	public Column originalLoadChunk(int cubeX, int cubeZ) {
		return this.loadColumn(cubeX, cubeZ, FORCE_LOAD);
	}

	@Override
	public boolean saveChunks(boolean alwaysTrue) {

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
	public boolean unloadQueuedChunks() {
		// NOTE: the return value is completely ignored

		if (this.worldServer.disableLevelSaving) {
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
				column.onChunkUnload();
				this.loadedColumns.remove(columnAddress);
				this.cubeIO.saveColumn(column);
			}
		}

		return false;
	}

	@Override
	public String makeString() {
		return "ServerCubeCache: " + this.loadedColumns.size() + " columns, Unload: " + this.cubesToUnload.size() + " cubes";
	}

	@Override
	public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(final EnumCreatureType a1, final BlockPos a2) {
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		return this.loadedColumns.size();
	}


	//==============================
	//=====CubicChunks methods======
	//==============================

	@Override
	public boolean chunkExists(int cubeX, int cubeZ) {
		//columnAccessStats(cubeX, cubeZ);
		return this.loadedColumns.containsKey(AddressTools.getAddress(cubeX, cubeZ));
	}

	@Override
	public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
		//cubeAccessStats(cubeX, cubeY, cubeZ);
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
	public Column getColumn(int columnX, int columnZ) {
		return this.loadedColumns.get(AddressTools.getAddress(columnX, columnZ));
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
		loadCube(cubeX, cubeY, cubeZ, LOAD_OR_GENERATE);

		for(int dx = -1; dx <=1; dx++) {
			for(int dy = -1; dy <= 1; dy++) {
				for(int dz = -1; dz <= 1; dz++) {
					loadCube(cubeX + dx, cubeY + dy, cubeZ + dz, LOAD_OR_GENERATE);
				}
			}
		}
	}

	public void loadCube(int cubeX, int cubeY, int cubeZ, LoadType loadType) {
		//TODO: clean up loadCube(int, int, int, LoadType)
		if(loadType == FORCE_LOAD) {
			throw new UnsupportedOperationException("Cannot Force Load a cube");
		}

		long cubeAddress = AddressTools.getAddress(cubeX, cubeY, cubeZ);
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);

		// step 1: get a column

		// is the column already loaded?
		Column column = this.loadedColumns.get(columnAddress);
		if (column == null) {
			// try loading it
			column = this.loadColumn(cubeX, cubeZ, loadType);
		}
		//if we couldn't load or generate the column - give up
		if(column == null) {
			return;
		}

		// step 2: get a cube

		// is the cube already loaded?
		Cube cube = column.getCube(cubeY);
		if (cube != null) {
			return;
		}

		// try to load the cube
		try {
			cube = this.cubeIO.loadCubeAndAddToColumn(column, cubeAddress);
		} catch (IOException ex) {
			log.error("Unable to load cube ({},{},{})", cubeX, cubeY, cubeZ, ex);
			return;
		}
		if (loadType == LOAD_OR_GENERATE && cube == null) {
			// start the cube generation process with an empty cube
			cube = column.getOrCreateCube(cubeY, true);
			cube.setGeneratorStage(GeneratorStage.getFirstStage());
		}
		//if couldn't generate it - return
		if(cube == null) {
			return;
		}
		if (!cube.getGeneratorStage().isLastStage()) {
			// queue the cube to finish generation
			WorldServerContext.get(this.worldServer).getGeneratorPipeline().generate(cube);
		} else if (cube.needsRelightAfterLoad()) {
			// queue the cube for re-lighting
			WorldServerContext.get(this.worldServer).getLightingManager().queueFirstLightCalculation(cubeAddress);
		}

		// init the column
		if (!column.isLoaded()) {
			column.onChunkLoad();
		}
		column.setTerrainPopulated(true);

		// init the cube
		cube.onLoad();
	}

	public Column loadColumn(int cubeX, int cubeZ, LoadType loadType) {
		Column column = null;
		//if we are not forced to load from disk - try to get it first
		if(loadType != FORCE_LOAD) {
			column = getColumn(cubeX, cubeZ);
		}
		if(column != null) {
			this.loadedColumns.put(AddressTools.getAddress(cubeX, cubeZ), column);
			return column;
		}
		try {
			column = this.cubeIO.loadColumn(cubeX, cubeZ);
		} catch (IOException ex) {
			log.error("Unable to load column ({},{})", cubeX, cubeZ, ex);
			return null;
		}

		if (column == null) {
			// there wasn't a column, generate a new one (if allowed to generate)
			if(loadType == LOAD_OR_GENERATE) {
				column = this.columnGenerator.generateColumn(cubeX, cubeZ);
			}
		} else {
			// the column was loaded
			column.setLastSaveTime(this.worldServer.getTotalWorldTime());
		}
		if(column == null) {
			return null;
		}
		this.loadedColumns.put(AddressTools.getAddress(cubeX, cubeZ), column);
		column.onChunkLoad();
		return column;
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

	public void saveAllChunks() {
		saveChunks(true);
	}

	private boolean cubeIsNearSpawn(int cubeX, int cubeY, int cubeZ) {

		if (!this.worldServer.provider.canRespawnHere()) {
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

	public enum LoadType {
		LOAD_ONLY, LOAD_OR_GENERATE, FORCE_LOAD
	}
}
