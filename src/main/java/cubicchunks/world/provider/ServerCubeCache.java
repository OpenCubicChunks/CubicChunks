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
package cubicchunks.world.provider;

import cubicchunks.CubicChunks;
import cubicchunks.VanillaCubicChunksWorldType;
import cubicchunks.server.chunkio.CubeIO;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderOverworld;
import net.minecraft.world.gen.ChunkProviderServer;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cubicchunks.world.provider.ServerCubeCache.LoadType.FORCE_LOAD;
import static cubicchunks.world.provider.ServerCubeCache.LoadType.LOAD_ONLY;
import static cubicchunks.world.provider.ServerCubeCache.LoadType.LOAD_OR_GENERATE;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

/**
 * This is CubicChunks equivalent of ChunkProviderServer, it loads and unloads Cubes and Columns.
 * <p>
 * There are a few necessary changes to the way vanilla methods work:
 * * Because loading a Chunk (Column) doesn't make much sense with CubicChunks,
 * all methods that load Chunks, actually load  an empry column with no blocks in it
 * (there may be some entities that are not in any Cube yet).
 * * dropChunk method is not supported. Columns are unloaded automatically when the last cube is unloaded
 */
public class ServerCubeCache extends ChunkProviderServer implements ICubeCache {

	private static final Logger log = CubicChunks.LOGGER;

	public static final int SPAWN_LOAD_RADIUS = 12; // highest render distance is 32

	private ICubicWorldServer worldServer;
	private CubeIO cubeIO;
	private Queue<CubeCoords> cubesToUnload;
	private Queue<ChunkPos> columnsToUnload;

	public ServerCubeCache(ICubicWorldServer worldServer, ICubeGenerator cubeGen, IColumnGenerator columnGen) {
		//TODO: Replace add ChunkGenerator argument and use chunk generator object for generating terrain?
		//ChunkGenerator has to exist for mob spawning to work
		super((WorldServer) worldServer,
				worldServer.getSaveHandler().getChunkLoader(worldServer.getProvider()), // forge uses this in
				null); // safe to null out IChunkGenerator

		this.worldServer = worldServer;
		this.cubeIO = new CubeIO(worldServer);
		this.cubesToUnload = new ArrayDeque<>();
		this.columnsToUnload = new ArrayDeque<>();
	}

	@Override
	public void unload(Chunk chunk) {
		//ignore, ChunkGc unloads cubes
		//note: WorldServer.saveAllChunks()
	}

	@Override
	public void unloadAllChunks() {
		// unload all the cubes in the columns
		for (Chunk column : this.id2ChunkMap.values()) {
			for (Cube cube : ((Column)column).getLoadedCubes()) {
				this.cubesToUnload.add(cube.getCoords());
			}
		}
	}

	/**
	 * Vanilla method, returns a Chunk (Column) only of it's already loaded.
	 */
	@Override
	@Nullable
	public Column getLoadedChunk(int columnX, int columnZ) {
		return (Column)super.getLoadedChunk(columnX, columnZ);
	}

	/**
	 * Loads Chunk (Column) if it can be loaded from disk, or returns already loaded one.
	 * Doesn't generate new Columns.
	 */
	@Override
	@Nullable
	public Column loadChunk(int columnX, int columnZ) {
		return this.loadColumn(columnX, columnZ, LOAD_ONLY);
	}

	/**
	 * Load chunk asynchronously. Currently CubicChunks only loads synchronously.
	 */
	@Override
	@Nullable
	public Column loadChunk(int columnX, int columnZ, Runnable runnable) {
		Column column = this.loadColumn(columnX, columnZ, LOAD_OR_GENERATE);
		if (runnable == null) {
			return column;
		}
		runnable.run();
		return null;
	}

	/**
	 * If this Column is already loaded - returns it.
	 * Loads from disk if possible, otherwise generates new Column.
	 */
	@Override
	public Column provideChunk(int cubeX, int cubeZ) {
		return loadChunk(cubeX, cubeZ, null);
	}

	@Override
	public boolean saveChunks(boolean alwaysTrue) {

		for (Chunk chunk : this.id2ChunkMap.values()) {
			Column column = (Column)chunk;
			// save the column
			if (column.needsSaving(alwaysTrue)) {
				this.cubeIO.saveColumn(column);
			}

			// save the cubes
			for (Cube cube : column.getLoadedCubes()) {
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
		if (this.worldServer.getDisableLevelSaving()) {
			return false;
		}

		final int maxUnload = 400;
		final int maxColumnsUnload = 40;

		unloadQueuedCubes(maxUnload);
		unloadQueuedColumns(maxColumnsUnload);

		return false;
	}

	private void unloadQueuedColumns(int maxColumnsUnload) {
		int unloaded = 0;
		Iterator<ChunkPos> it = columnsToUnload.iterator();
		while(it.hasNext() && unloaded < maxColumnsUnload) {
			ChunkPos pos = it.next();
			long address = AddressTools.getAddress(pos.chunkXPos, pos.chunkZPos);
			Column column = (Column)id2ChunkMap.get(address);
			it.remove();
			if(column == null) {
				continue;
			}
			if (!column.hasLoadedCubes() && column.unloaded) {

				column.onChunkUnload();
				this.id2ChunkMap.remove(address);
				this.cubeIO.saveColumn(column);
				unloaded++;
			}
		}
	}

	private void unloadQueuedCubes(int maxUnload) {
		Iterator<CubeCoords> iter = this.cubesToUnload.iterator();
		int processed = 0;

		while (iter.hasNext() && processed < maxUnload) {
			CubeCoords coords = iter.next();
			iter.remove();
			++processed;

			long columnAddress = AddressTools.getAddress(coords.getCubeX(), coords.getCubeZ());

			Column column = (Column)this.id2ChunkMap.get(columnAddress);
			if (column == null) {
				continue;
			}
			Cube cube = column.getCube(coords.getCubeY());
			//unload the cube if we are unloading the column
			if (cube != null && (cube.unloaded || column.unloaded)) {
				cube.onUnload(); //TODO: remove this
				column.removeCube(coords.getCubeY());
				this.cubeIO.saveCube(cube);
			}
		}
	}

	@Override
	public String makeString() {
		return "ServerCubeCache: " + this.id2ChunkMap.size() + " columns, Unload: " + this.cubesToUnload.size() +
				" cubes";
	}

	@Override
	public List<Biome.SpawnListEntry> getPossibleCreatures(@Nonnull final EnumCreatureType type, @Nonnull final BlockPos pos) {
		return super.getPossibleCreatures(type, pos);
	}

	@Nullable
	public BlockPos getStrongholdGen(@Nonnull World worldIn, @Nonnull String structureName, @Nonnull BlockPos position) {
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		return this.id2ChunkMap.size();
	}

	@Override
	public boolean chunkExists(int cubeX, int cubeZ) {
		Chunk column = this.id2ChunkMap.get(AddressTools.getAddress(cubeX, cubeZ));
		return column != null && !column.unloaded;
	}

	//==============================
	//=====CubicChunks methods======
	//==============================

	@Override
	public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);
		Column column = (Column)this.id2ChunkMap.get(columnAddress);
		if (column == null || column.unloaded) {
			return false;
		}
		Cube cube = column.getCube(cubeY);
		return cube != null && !cube.unloaded;
	}

	public boolean cubeExists(CubeCoords coords) {
		return this.cubeExists(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
	}

	@Override
	public Cube getCube(int cubeX, int cubeY, int cubeZ) {
		Column column = provideChunk(cubeX, cubeZ);
		if (column == null) {
			return null;
		}
		Cube cube = column.getCube(cubeY);
		if(cube != null) {
			cube.unloaded = false;
		}
		return cube;
	}

	public Cube getCube(CubeCoords coords) {
		return this.getCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
	}

	public void loadCube(int cubeX, int cubeY, int cubeZ, LoadType loadType) {

		if (loadType == FORCE_LOAD) {
			throw new UnsupportedOperationException("Cannot force load a cube");
		}

		// Get the column
		Column column = provideChunk(cubeX, cubeZ);

		// Try loading the column.
		if (column == null) {
			worldServer.getProfiler().startSection("loadColumn");
			column = this.loadColumn(cubeX, cubeZ, loadType);
			worldServer.getProfiler().endSection();
		}

		// If we couldn't load or generate the column - give up.
		if (column == null) {
			if (loadType == LOAD_OR_GENERATE) {
				CubicChunks.LOGGER.error(
						"Loading cube at " + cubeX + ", " + cubeY + ", " + cubeZ + " failed, couldn't load column");
			}
			return;
		}

		// Get the cube.
		long cubeAddress = AddressTools.getAddress(cubeX, cubeY, cubeZ);

		// Is the cube loaded?
		Cube cube = column.getCube(cubeY);
		if (cube != null) {
			cube.unloaded = false;
			return;
		}

		// Try loading the cube.
		try {
			worldServer.getProfiler().startSection("cubeIOLoad");
			cube = this.cubeIO.loadCubeAndAddToColumn(column, cubeAddress);
		} catch (IOException ex) {
			log.error("Unable to load cube ({},{},{})", cubeX, cubeY, cubeZ, ex);
			return;
		} finally {
			worldServer.getProfiler().endSection();
		}

		// If loading it didn't work...
		if (cube == null) {
			// ... and generating has been requested, generate it.
			if (loadType == LoadType.LOAD_OR_GENERATE) {
				worldServer.getProfiler().startSection("createEmptyCube");
				cube = column.getOrCreateCube(cubeY, true);
				worldServer.getProfiler().endStartSection("generateBlocks");
				this.worldServer.getCubeGenerator().generateCube(this, cube);
				worldServer.getProfiler().endSection();
			}
			// ... or quit.
			else {
				return;
			}
		}

		// Init the column.
		if (!column.isLoaded()) {
			column.onChunkLoad();
		}
		column.setTerrainPopulated(true);

		// Init the cube.
		cube.onLoad(); //TODO: remove this
	}

	public void loadCube(CubeCoords coords, LoadType loadType) {
		this.loadCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ(), loadType);
	}

	public Column loadColumn(int cubeX, int cubeZ, LoadType loadType) {
		Column column = null;
		//if we are not forced to load from disk - try to get it first
		if (loadType != FORCE_LOAD) {
			column = getLoadedChunk(cubeX, cubeZ);
		}
		if (column != null) {
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
			if (loadType == LOAD_OR_GENERATE) {
				column = this.worldServer.getColumnGenerator().generateColumn(cubeX, cubeZ);
			}
		} else {
			// the column was loaded
			column.setLastSaveTime(this.worldServer.getTotalWorldTime());
		}
		if (column == null) {
			return null;
		}
		this.id2ChunkMap.put(AddressTools.getAddress(cubeX, cubeZ), column);
		column.onChunkLoad();
		return column;
	}

	private boolean cubeIsNearSpawn(Cube cube) {

		if (!this.worldServer.getProvider().canRespawnHere()) {
			// no spawn points
			return false;
		}

		BlockPos spawnPoint = this.worldServer.getSpawnPoint();
		int spawnCubeX = Coords.blockToCube(spawnPoint.getX());
		int spawnCubeY = Coords.blockToCube(spawnPoint.getY());
		int spawnCubeZ = Coords.blockToCube(spawnPoint.getZ());
		int dx = Math.abs(spawnCubeX - cube.getX());
		int dy = Math.abs(spawnCubeY - cube.getY());
		int dz = Math.abs(spawnCubeZ - cube.getZ());
		return dx <= SPAWN_LOAD_RADIUS && dy <= SPAWN_LOAD_RADIUS && dz <= SPAWN_LOAD_RADIUS;
	}

	public String dumpLoadedCubes() {
		StringBuilder sb = new StringBuilder(10000).append("\n");
		for (Chunk chunk : this.id2ChunkMap.values()) {
			Column column = (Column)chunk;
			if (column == null) {
				sb.append("column = null\n");
				continue;
			}
			sb.append("Column[").append(column.getX()).append(", ").append(column.getZ()).append("] {");
			boolean isFirst = true;
			for (Cube cube : column.getLoadedCubes()) {
				if (!isFirst) {
					sb.append(", ");
				}
				isFirst = false;
				if (cube == null) {
					sb.append("cube = null");
					continue;
				}
				sb.append("Cube[").append(cube.getY()).append("]");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void flush() {
		this.cubeIO.flush();
	}

	public void unloadCube(Cube cube) {
		if(this.worldServer.getWorldInfo().getTerrainType() instanceof VanillaCubicChunksWorldType) {
			final int bufferSize = 1;
			if(cube.getY() >= 0 - bufferSize && cube.getY() < 16 + bufferSize) {
				return;//don't unload
			}
		}

		unloadCubeIgnoreVanilla(cube);
	}

	private void unloadCubeIgnoreVanilla(Cube cube) {
		// don't unload cubes near the spawn
		if (cubeIsNearSpawn(cube)) {
			return;
		}

		// queue the cube for unloading
		this.cubesToUnload.add(cube.getCoords());
		cube.unloaded = true;
	}

	public void unloadColumn(Column column) {
		//unload all cubes in that column
		//since it's unloading the whole column - ignore vanilla
		//this allows to special-case 0-255 height range with VanillaCubic
		column.getLoadedCubes().forEach(this::unloadCubeIgnoreVanilla);
		//unload that column
		//TODO: columns that have cubes near spawn will never be removed from unload queue
		//there is only a finite amount of them (about 1000) so it's not a big issue
		columnsToUnload.add(column.getChunkCoordIntPair());
		column.unloaded = true;
	}

	public enum LoadType {
		LOAD_ONLY, LOAD_OR_GENERATE, FORCE_LOAD
	}
}
