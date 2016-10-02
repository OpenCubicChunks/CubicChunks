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

import cubicchunks.CubicChunks;
import cubicchunks.VanillaCubicChunksWorldType;
import cubicchunks.server.chunkio.CubeIO;
import cubicchunks.server.chunkio.async.AsyncWorldIOExecutor;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ReportedException;
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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import static cubicchunks.server.ServerCubeCache.LoadType.FORCE_LOAD;
import static cubicchunks.server.ServerCubeCache.LoadType.LOAD_ONLY;
import static cubicchunks.server.ServerCubeCache.LoadType.LOAD_OR_GENERATE;

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
	private HashMap<Long, Column> loadedColumns;
	private Queue<CubeCoords> cubesToUnload;
	private Queue<ChunkPos> columnsToUnload;

	public ServerCubeCache(ICubicWorldServer worldServer) {
		//TODO: Replace add ChunkGenerator argument and use chunk generator object for generating terrain?
		//ChunkGenerator has to exist for mob spawning to work
		super((WorldServer) worldServer, worldServer.getSaveHandler().getChunkLoader(worldServer.getProvider()),
				new ChunkProviderOverworld((World) worldServer, worldServer.getSeed(), false, null));

		this.worldServer = worldServer;
		this.cubeIO = new CubeIO(worldServer);
		this.loadedColumns = new HashMap<>();
		this.cubesToUnload = new ArrayDeque<>();
		this.columnsToUnload = new ArrayDeque<>();
	}

	@Override
	public Collection<Chunk> getLoadedChunks() {
		return (Collection<Chunk>) (Object) this.loadedColumns.values();
	}

	@Override
	public void unload(Chunk chunk) {
		//ignore, ChunkGc unloads cubes
	}

	@Override
	public void unloadAllChunks() {
		// unload all the cubes in the columns
		for (Column column : this.loadedColumns.values()) {
			for (Cube cube : column.getAllCubes()) {
				this.cubesToUnload.add(cube.getCoords());
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
	 * Load a chunk. If nullable is passed, the chunk is loaded asynchronously and the callback is called
	 * when the load finishes. Otherwise, the chunk is loaded synchronously and returned.
	 *
	 * @param cubeX Column x position
	 * @param cubeZ Column z position
	 * @param runnable The callback, optionally to be called when the load is finished
	 *
	 * @return The chunk, if loaded synchronously. <code>null</code> otherwise
	 */
	@Override
	@Nullable
	public Column loadChunk(int cubeX, int cubeZ, @Nullable Runnable runnable) {
		if (runnable == null) {
			return loadColumn(cubeX, cubeZ, LOAD_OR_GENERATE);
		}

		asyncLoadColumn(cubeX, cubeZ, col -> runnable.run());
		return null;
	}

	/**
	 * If this Column is already loaded - returns it.
	 * Loads from disk if possible, otherwise generates new Column.
	 */
	@Override
	@Nonnull // Explicit nonnull annotation because super is annotated
	public Column provideChunk(int cubeX, int cubeZ) {
		Column column = loadChunk(cubeX, cubeZ, null);
		if (column == null) {
			// Forge throws an exception here, so do we
			CrashReport crashreport = CrashReport.makeCrashReport(new Exception(), "Failed generating a column");
			CrashReportCategory crashreportcategory = crashreport.makeCategory("Column to be generated");
			crashreportcategory.addCrashSection("Location", String.format("%d,%d", cubeX, cubeZ));
			crashreportcategory.addCrashSection("Position hash", AddressTools.getAddress(cubeX, cubeZ));
			crashreportcategory.addCrashSection("Generator", this.chunkGenerator);
			throw new ReportedException(crashreport);
		}
		return column;
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

		final int maxUnload = 400;
		final int maxColumnsUnload = 40;

		unloadQueuedCubes(maxUnload);
		unloadQueuedColumns(maxColumnsUnload);

		return false;
	}

	private void unloadQueuedColumns(int maxColumnsUnload) {
		int unloaded = 0;
		Iterator<ChunkPos> it = columnsToUnload.iterator();
		while (it.hasNext() && unloaded < maxColumnsUnload) {
			ChunkPos pos = it.next();
			long address = AddressTools.getAddress(pos.chunkXPos, pos.chunkZPos);
			Column column = loadedColumns.get(address);
			it.remove();
			if (column == null) {
				continue;
			}
			if (!column.hasCubes() && column.unloaded) {

				column.onChunkUnload();
				this.loadedColumns.remove(address);
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

			Column column = this.loadedColumns.get(columnAddress);
			if (column == null) {
				continue;
			}
			Cube cube = column.getCube(coords.getCubeY());
			//unload the cube if we are unloading the column
			if (cube != null && (cube.unloaded || column.unloaded)) {
				cube.onUnload();
				column.removeCube(coords.getCubeY());
				this.cubeIO.saveCube(cube);
			}
		}
	}

	@Override
	public String makeString() {
		return "ServerCubeCache: " + this.loadedColumns.size() + " columns, Unload: " + this.cubesToUnload.size() +
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
		return this.loadedColumns.size();
	}

	@Override
	public boolean chunkExists(int cubeX, int cubeZ) {
		Column column = this.loadedColumns.get(AddressTools.getAddress(cubeX, cubeZ));
		return column != null && !column.unloaded;
	}

	//==============================
	//=====CubicChunks methods======
	//==============================

	@Override
	public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
		long columnAddress = AddressTools.getAddress(cubeX, cubeZ);
		Column column = this.loadedColumns.get(columnAddress);
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
	public Column getColumn(int columnX, int columnZ) {
		Column column = this.loadedColumns.get(AddressTools.getAddress(columnX, columnZ));
		if (column != null) {
			column.unloaded = false;
		}
		return column;
	}

	/**
	 * Retrieve the cube at the specified coodinates, if present
	 * @param cubeX cube x position
	 * @param cubeY cube y position
	 * @param cubeZ cube z position
	 * @return The cube, or <code>null</code> if not loaded
	 */
	@Override
	public Cube getCube(int cubeX, int cubeY, int cubeZ) {
		Column column = getColumn(cubeX, cubeZ);
		if (column == null) {
			return null;
		}
		Cube cube = column.getCube(cubeY);
		if (cube != null) {
			cube.unloaded = false;
		}
		return cube;
	}

	public Cube getCube(CubeCoords coords) {
		return this.getCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
	}


	/**
	 * Load a cube asynchronously
	 *
	 * @param cubeX cube x position
	 * @param cubeY cube y position
	 * @param cubeZ cube z position
	 * @param callback Callback to be called on load finish. Passed null if load fails
	 */
	public void asyncLoadCube(int cubeX, int cubeY, int cubeZ, Consumer<Cube> callback) {
		// Fast return if both is already loaded
		Column column = getColumn(cubeX, cubeZ);
		if (column != null) {
			Cube cube = column.getCube(cubeY);

			if (cube != null) {
				callback.accept(cube);
			} else {
				AsyncWorldIOExecutor.queueCubeLoad(worldObj, cubeIO, column, this, cubeY,
						result -> callback.accept(postprocessLoadedCube(cubeX, cubeY, cubeZ, true, result, column)));
			}

			return;
		}

		AsyncWorldIOExecutor.queueCubeLoad(worldObj, cubeIO, this, cubeX, cubeY, cubeZ,
				result -> callback.accept(postprocessLoadedCube(cubeX, cubeY, cubeZ, true, result, null)));
	}

	/**
	 * Load a cube. Fails spectacularly if the column can't be loaded, otherwise returns null on failure for some reason.
	 *
	 * @param cubeX cube x position
	 * @param cubeY cube y position
	 * @param cubeZ cube z position
	 * @param loadType Loading behavior - Force a reload {@link LoadType#FORCE_LOAD}, load from disk if not present
	 * {@link LoadType#LOAD_ONLY}, or generate a cube if it does not exist at all {@link LoadType#LOAD_OR_GENERATE}
	 */
	public Cube loadCube(int cubeX, int cubeY, int cubeZ, @Nonnull LoadType loadType) {

		if (loadType == FORCE_LOAD) {
			throw new UnsupportedOperationException("Cannot force load a cube");
		}

		// Get the column
		// Column column = getColumn(cubeX, cubeZ);
		// Handled in loadColumn


		worldServer.getProfiler().startSection("loadColumn");
		Column column = this.loadColumn(cubeX, cubeZ, loadType);
		worldServer.getProfiler().endSection();

		// If we couldn't load or generate the column - give up.
		if (column == null) {
			failOnColumnFailure(cubeX, cubeY, cubeZ, loadType);
		}

		// Is the cube loaded?
		Cube cube = column.getCube(cubeY);


		// Try loading the cube.
		if (cube == null) {
			worldServer.getProfiler().startSection("cubeIOLoad");
			cube = AsyncWorldIOExecutor.syncCubeLoad(worldObj, cubeIO, cubeY, column, this);
			worldServer.getProfiler().endSection();
		}

		return postprocessLoadedCube(cubeX, cubeY, cubeZ, loadType == LOAD_OR_GENERATE, cube, column);
	}


	@Nullable
	private Cube postprocessLoadedCube(int cubeX, int cubeY, int cubeZ, boolean allowedToGenerate, Cube cube, Column column) {
		if (cube == null && allowedToGenerate) {
			if (column == null) {
				failOnColumnFailure(cubeX, cubeY, cubeZ, LOAD_OR_GENERATE);
			}

			worldServer.getProfiler().startSection("createEmptyCube");
			cube = column.getOrCreateCube(cubeY, true);
			worldServer.getProfiler().endStartSection("generateBlocks");
			this.worldServer.getCubeGenerator().generateCube(this, cube);
			worldServer.getProfiler().endSection();
		}


		if (cube != null) {
			// Init the column.
			if (!column.isLoaded()) {
				column.onChunkLoad();
			}
			column.setTerrainPopulated(true);

			// Init the cube.
			cube.onLoad();
			cube.unloaded = false;
		}

		return cube;
	}

	/**
	 * Raise an exception on cube load if the column failed to load. Always fails!
	 *
	 * @param cubeX cube x position
	 * @param cubeY cube y position
	 * @param cubeZ cube z position
	 * @param loadType Load type to report
	 */
	private void failOnColumnFailure(int cubeX, int cubeY, int cubeZ, @Nonnull LoadType loadType) {
		// Roughly mirroring forge behavior for chunks
		CrashReport crashreport = CrashReport.makeCrashReport(new Exception(), "Exception generating new cube");
		CrashReportCategory crashreportcategory = crashreport.makeCategory("Cube to be generated");
		crashreportcategory.addCrashSection("Location", String.format("x=%d,y=%d,z=%d", cubeX, cubeY, cubeZ));
		crashreportcategory.addCrashSection("Position hash", AddressTools.getAddress(cubeX, cubeY, cubeZ));
		crashreportcategory.addCrashSection("Load type", loadType);
		crashreportcategory.addCrashSection("Generator", this.chunkGenerator);
		crashreportcategory.addCrashSection("Failure type", "Unable to load or generate column");
		throw new ReportedException(crashreport);
	}

	public void loadCube(CubeCoords coords, LoadType loadType) {
		this.loadCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ(), loadType);
	}

	/**
	 * Load a column asynchronously, generating it if it doesn't exist
	 *
	 * @param cubeX column x position
	 * @param cubeZ column z position
	 * @param callback The callback to be executed when the column is loaded
	 */
	public void asyncLoadColumn(int cubeX, int cubeZ, Consumer<Column> callback) {
		Column column = getColumn(cubeX, cubeZ);
		if (column != null) {
			callback.accept(column);
			return;
		}

		AsyncWorldIOExecutor.queueColumnLoad(worldObj, cubeIO, cubeX, cubeZ, col -> {
			col = postprocessLoadedColumn(cubeX, cubeZ, true, col);
			// TODO Up to three callbacks deep at this point
			callback.accept(col);
		});
	}

	/**
	 * Load a column synchronously
	 *
	 * @param cubeX column x position
	 * @param cubeZ column z position
	 * @param loadType Type of load behavior - Force a reload {@link LoadType#FORCE_LOAD}, load if necessary, returning
	 * null if the column does not exist {@link LoadType#LOAD_ONLY}, or try to load, generating the column if it doesn't
	 * exist {@link LoadType#LOAD_OR_GENERATE}
	 *
	 * @return The generated column, or <code>null</code> if the loading process failed
	 */
	@Nullable
	public Column loadColumn(int cubeX, int cubeZ, LoadType loadType) {
		Column column = null;
		//if we are not forced to load from disk - try to get it first
		if (loadType != FORCE_LOAD) {
			column = getColumn(cubeX, cubeZ);
		}
		if (column != null) {
			return column;
		}

		column = AsyncWorldIOExecutor.syncColumnLoad(worldObj, cubeIO, cubeX, cubeZ);
		// TODO we swallow IOExceptions here - is that ok?

		column = postprocessLoadedColumn(cubeX, cubeZ, loadType == LOAD_OR_GENERATE, column);
		if (column == null) {
			throw new RuntimeException("column total fail");
		}
		return column;
	}

	/**
	 * Postprocess a loaded column, assigning last-load times etc. Necessary step after loading the column
	 *
	 * @param cubeX column x position
	 * @param cubeZ column z position
	 * @param allowedToGenerate can the column be generated if it wasn't loaded
	 * @param column The loaded column
	 *
	 * @return The loaded or generated column, or <code>null</code> if the process failed
	 */
	@Nullable
	private Column postprocessLoadedColumn(int cubeX, int cubeZ, boolean allowedToGenerate, @Nullable Column column) {
		if (column == null && allowedToGenerate) {
			// there wasn't a column, generate a new one (if allowed to generate)
			column = this.worldServer.getColumnGenerator().generateColumn(cubeX, cubeZ);
		}
		// Column.setLastSaveTime moved to async load
		if (column != null) {
			this.loadedColumns.put(AddressTools.getAddress(cubeX, cubeZ), column);
			column.onChunkLoad();
			column.unloaded = false;
		}
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

	public void unloadCube(Cube cube) {
		if (this.worldServer.getWorldInfo().getTerrainType() instanceof VanillaCubicChunksWorldType) {
			final int bufferSize = 1;
			if (cube.getY() >= 0 - bufferSize && cube.getY() < 16 + bufferSize) {
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
		column.getAllCubes().forEach(this::unloadCubeIgnoreVanilla);
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
