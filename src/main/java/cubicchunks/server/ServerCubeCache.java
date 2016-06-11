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
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.ColumnGenerator;
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.world.dependency.DependencyManager;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static cubicchunks.server.ServerCubeCache.LoadType.FORCE_LOAD;
import static cubicchunks.server.ServerCubeCache.LoadType.LOAD_ONLY;
import static cubicchunks.server.ServerCubeCache.LoadType.LOAD_OR_GENERATE;

/**
 * Thic is CubicChunks equivalent of ChunkProviderServer, it loads and unloads Cubes and Columns.
 * <p>
 * There are a few necessary changes to the way vanilla methods work:
 * * Because loading a Chunk (Column) doesn't make much sense with CubicChunks,
 * all methods that load Chunks, actually load  an empry column with no blocks in it
 * (there may be some entities that are not in any Cube yet).
 * * dropChunk method is not supported. Columns are unloaded automatically when the last cube is unloaded
 */
public class ServerCubeCache extends ChunkProviderServer implements ICubeCache {

	private static final Logger log = CubicChunks.LOGGER;

	public static final int WorldSpawnChunkDistance = 12; // highest render distance is 32

	private ICubicWorldServer worldServer;
	private CubeIO cubeIO;
	private ColumnGenerator columnGenerator;
	private HashMap<Long, Column> loadedColumns;
	private Queue<Long> cubesToUnload;
	private DependencyManager dependencyManager;

	/**
	 * Cube generator can add cubes into world that are "linked" with other cube -
	 * Usually when generating one cube requires generating more than just neighbors.
	 * <p>
	 * This is a mapping of which cubes are linked with which other cubes,
	 * allows to automatically unload these additional cubes.
	 */
	private Map<Cube, Set<Cube>> forceAdded;
	private Map<Cube, Set<Cube>> forceAddedReverse;

	public ServerCubeCache(ICubicWorldServer worldServer) {
		super((WorldServer) worldServer, worldServer.getSaveHandler().getChunkLoader(worldServer.getProvider()), null);

		this.worldServer = worldServer;
		this.cubeIO = new CubeIO(worldServer);
		this.columnGenerator = new ColumnGenerator(worldServer);
		this.loadedColumns = Maps.newHashMap();
		this.cubesToUnload = new ArrayDeque<>();
		this.forceAdded = new HashMap<>();		
		this.forceAddedReverse = new HashMap<>();
		this.dependencyManager = new DependencyManager(this);
	}

	public DependencyManager getDependencyManager() {
		return this.dependencyManager;
	}

	@Override
	public Collection<Chunk> getLoadedChunks() {
		return (Collection<Chunk>) (Object) this.loadedColumns.values();
	}

	@Override
	public void unload(Chunk chunk) {
		Column column = (Column) chunk;
		for(Cube cube : column.getAllCubes()) {
			this.unloadCube(cube.getX(), cube.getY(), cube.getZ());
		}
	}

	@Override
	public void unloadAllChunks() {
		// unload all the cubes in the columns
		for (Column column : this.loadedColumns.values()) {
			for (Cube cube : column.getAllCubes()) {
				this.cubesToUnload.add(cube.getAddress());
			}
		}
	}

	/**
	 * Vanilla method, returns a Chunk (Column) only of it's already loaded.
	 * Same as getColumn(cubeX, cubeZ)
	 */
	@Override
	@Nullable
	public Chunk getLoadedChunk(int cubeX, int cubeZ) {
		return this.getColumn(cubeX, cubeZ);
	}

	/**
	 * Loads Chunk (Column) if it can be loaded from disk, or returns already loaded one.
	 * Doesn't generate new Columns.
	 */
	@Override
	@Nullable
	public Column loadChunk(int cubeX, int cubeZ) {
		return this.loadColumn(cubeX, cubeZ, LOAD_ONLY);
	}

	/**
	 * Load chunk asynchronously. Currently CubicChunks only loads synchronously.
	 */
	@Override
	@Nullable
	public Column loadChunk(int cubeX, int cubeZ, Runnable runnable) {
		Column column = this.loadColumn(cubeX, cubeZ, LOAD_OR_GENERATE);
		if (runnable == null) {
			return column;
		}
		runnable.run();
		return null;
	}

	@Override
	public Column originalLoadChunk(int cubeX, int cubeZ) {
		return this.loadColumn(cubeX, cubeZ, FORCE_LOAD);
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
	public boolean saveChunks(boolean alwaysTrue) {

		for (Column column : this.loadedColumns.values()) {
			// save the column
			if (column.needsSaving(alwaysTrue)) {
				this.cubeIO.saveColumn(column);
			}

			// save the cubes
			for (Cube cube : column.getAllCubes()) {
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

		final int MaxNumToUnload = 400;

		// unload cubes
		for (int i = 0; i < MaxNumToUnload && !this.cubesToUnload.isEmpty(); i++) {
			long cubeAddress = this.cubesToUnload.poll();

			// Skip unloading the cube if it is required.
			if (this.dependencyManager.isRequired(new CubeCoords(cubeAddress))) {
				continue;
			}

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
				this.recursivelyRemoveForceLoadedCube(cube);
				
				// tell the cube it has been unloaded
				cube.onUnload();

				// Make sure the cube does not keep any other cubes around.
				this.worldServer.getGeneratorPipeline().getDependentCubeManager().unregister(cube);

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
		return "ServerCubeCache: " + this.loadedColumns.size() + " columns, Unload: " + this.cubesToUnload.size() +
				" cubes";
	}

	@Override
	public List<Biome.SpawnListEntry> getPossibleCreatures(@Nonnull final EnumCreatureType a1, @Nonnull final BlockPos a2) {
		return null;
	}

	@Nullable
	public BlockPos getStrongholdGen(@Nonnull World worldIn, @Nonnull String structureName, @Nonnull BlockPos position) {
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		return this.loadedColumns.size();
	}

	@Override
	public boolean chunkExists(int cubeX, int cubeZ) {
		//columnAccessStats(cubeX, cubeZ);
		return this.loadedColumns.containsKey(AddressTools.getAddress(cubeX, cubeZ));
	}

	//==============================
	//=====CubicChunks methods======
	//==============================

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

	public boolean cubeExists(CubeCoords coords) {
		return this.cubeExists(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
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

	public Cube getCube(CubeCoords coords) {
		return this.getCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
	}

	public void loadCube(int cubeX, int cubeY, int cubeZ, LoadType loadType, GeneratorStage targetStage) {

		if (loadType == FORCE_LOAD) {
			throw new UnsupportedOperationException("Cannot force load a cube");
		}

		// Get the column
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);
		
		// Is it loaded?
		Column column = this.loadedColumns.get(columnAddress);

		// Try loading the column.
		if (column == null) {
			column = this.loadColumn(cubeX, cubeZ, loadType);
		}

		// If we couldn't load or generate the column - give up.
		if (column == null) {
			return;
		}

		// Get the cube.
		long cubeAddress = AddressTools.getAddress(cubeX, cubeY, cubeZ);
		
		// Is the cube loaded?
		Cube cube = column.getCube(cubeY);
		if (cube != null) {

			// Resume/continue generation if necessary.
			if (cube.getCurrentStage().precedes(targetStage)) {
				this.worldServer.getGeneratorPipeline().generate(cube, targetStage);
			}

			return;
		}

		// Try loading the cube.
		try {
			cube = this.cubeIO.loadCubeAndAddToColumn(column, cubeAddress);
		} catch (IOException ex) {
			log.error("Unable to load cube ({},{},{})", cubeX, cubeY, cubeZ, ex);
			return;
		}

		// If loading it didn't work and generating it was not requested, quit.
		if (cube == null && loadType != LoadType.LOAD_OR_GENERATE) {
			return;
		}

		// Have the column generate a new cube object and configure it for generation.
		cube = column.getOrCreateCube(cubeY, true);
		cube.setCurrentStage(this.worldServer.getGeneratorPipeline().getFirstStage());
		cube.setTargetStage(targetStage);

		// If the cube has yet to reach the target stage, resume generation.
		if (cube.isBeforeStage(targetStage)) {
			this.worldServer.getGeneratorPipeline().generate(cube);
		}

		// Init the column.
		if (!column.isLoaded()) {
			column.onChunkLoad();
		}
		column.setTerrainPopulated(true);
		
		// Init the cube.
		cube.onLoad();
		this.dependencyManager.onLoad(cube);
	}
	
	public void loadCube(int cubeX, int cubeY, int cubeZ, LoadType loadType) {
		this.loadCube(cubeX, cubeY, cubeZ, loadType, GeneratorStage.LIVE);
	}

	public void loadCube(CubeCoords coords, LoadType loadType, GeneratorStage targetStage) {
		this.loadCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ(), loadType, targetStage);
	}
	
	public Column loadColumn(int cubeX, int cubeZ, LoadType loadType) {
		Column column = null;
		//if we are not forced to load from disk - try to get it first
		if (loadType != FORCE_LOAD) {
			column = getColumn(cubeX, cubeZ);
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
				column = this.columnGenerator.generateColumn(cubeX, cubeZ);
			}
		} else {
			// the column was loaded
			column.setLastSaveTime(this.worldServer.getTotalWorldTime());
		}
		if (column == null) {
			return null;
		}
		this.loadedColumns.put(AddressTools.getAddress(cubeX, cubeZ), column);
		column.onChunkLoad();
		return column;
	}

	@Override
	public void unloadCube(int cubeX, int cubeY, int cubeZ) {
		
		// TODO: Change to use CubeCoords instead.

		// don't unload cubes near the spawn
		if (cubeIsNearSpawn(cubeX, cubeY, cubeZ)) {
			return;
		}
		
		// Do not unload cubes which are required for generating currently loaded cubes.
		if (dependencyManager.isRequired(new CubeCoords(cubeX, cubeY, cubeZ))) {
			return;
		}

		// queue the cube for unloading
		this.cubesToUnload.add(AddressTools.getAddress(cubeX, cubeY, cubeZ));
	}

	public Cube forceLoadCube(Cube forcedBy, int cubeX, int cubeY, int cubeZ) {

		this.loadCube(cubeX, cubeY, cubeZ, LOAD_ONLY);
		Cube cube = getCube(cubeX, cubeY, cubeZ);
		if (cube != null) {
			addForcedByMapping(forcedBy, cube);
			return cube;
		}
		Column column = this.loadColumn(cubeX, cubeZ, LOAD_OR_GENERATE);
		cube = column.getOrCreateCube(cubeY, true);
		addForcedByMapping(forcedBy, cube);
		
		//set generator stage, technically shouldn't be needed because it's set in worldgen code
		//but in case not all cubes are saved - it would crash.
		cube.setCurrentStage(this.worldServer.getGeneratorPipeline().getFirstStage());
		cube.setTargetStage(GeneratorStage.LIVE);
		return cube;
	}

	private void addForcedByMapping(Cube forcedBy, Cube cube) {
		Set<Cube> forcedCubes = this.forceAdded.get(forcedBy);

		if (forcedCubes == null) { 
			forcedCubes = new HashSet<Cube>();
			this.forceAdded.put(forcedBy, forcedCubes);
		}
		Set<Cube> forcedReverse = this.forceAddedReverse.get(cube);
		if (forcedReverse == null) {
			forcedReverse = new HashSet<>();
			this.forceAddedReverse.put(cube, forcedReverse);
		}

		forcedCubes.add(cube);
		forcedReverse.add(forcedBy);
	}

	private boolean recursivelyRemoveForceLoadedCube(Cube cube) {
		//TODO: unload force-loaded cubes
		return true;
	}

	public void saveAllChunks() {
		saveChunks(true);
	}

	private boolean cubeIsNearSpawn(int cubeX, int cubeY, int cubeZ) {

		if (!this.worldServer.getProvider().canRespawnHere()) {
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

	public String dumpLoadedCubes() {
		StringBuilder sb = new StringBuilder(10000).append("\n");
		for (Column column : this.loadedColumns.values()) {
			if (column == null) {
				sb.append("column = null\n");
				continue;
			}
			sb.append("Column[").append(column.getX()).append(", ").append(column.getZ()).append("] {");
			boolean isFirst = true;
			for (Cube cube : column.getAllCubes()) {
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

	public enum LoadType {
		LOAD_ONLY, LOAD_OR_GENERATE, FORCE_LOAD
	}

	public void setDependencyManager(DependencyManager dependencyManager) {
		this.dependencyManager = dependencyManager;
	}

}
