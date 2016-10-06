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
import cubicchunks.server.chunkio.CubeIO;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.provider.ICubeCache;
import cubicchunks.world.provider.IProviderExtras;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.ICubePrimer;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class ServerCubeCache extends ChunkProviderServer implements ICubeCache, IProviderExtras{

	private static final Logger log = CubicChunks.LOGGER;

	public static final int SPAWN_LOAD_RADIUS = 12; // highest render distance is 32

	private ICubicWorldServer worldServer;
	private CubeIO cubeIO;
	private Queue<CubeCoords> cubesToUnload;
	private Queue<ChunkPos> columnsToUnload;

	private Map<CubeCoords, Cube> cubemap = new HashMap<>();

	private ICubeGenerator   cubeGen;

	public ServerCubeCache(ICubicWorldServer worldServer, ICubeGenerator cubeGen) {
		super((WorldServer) worldServer,
				worldServer.getSaveHandler().getChunkLoader(worldServer.getProvider()), // forge uses this in
				null); // safe to null out IChunkGenerator

		this.cubeGen   = cubeGen;

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
		for(Cube cube : cubemap.values()){
			cubesToUnload.add(cube.getCoords());
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
		return this.loadChunk(columnX, columnZ, null);
	}

	/**
	 * Load chunk asynchronously. Currently CubicChunks only loads synchronously.
	 */
	@Override
	@Nullable
	public Column loadChunk(int columnX, int columnZ, Runnable runnable) {
		Column column = this.getColumn(columnX, columnZ, /*Requirement.LOAD*/Requirement.LIGHT);
		if (runnable == null) {                          // TODO: Set this to LOAD when PlayerCubeMap works
			return column;
		}
		runnable.run();
		return column;
	}

	/**
	 * If this Column is already loaded - returns it.
	 * Loads from disk if possible, otherwise generates new Column.
	 */
	@Override
	public Column provideChunk(int cubeX, int cubeZ) {
		return getColumn(cubeX, cubeZ, Requirement.GENERATE);
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
			it.remove();

			long address = ChunkPos.asLong(pos.chunkXPos, pos.chunkZPos);
			Column column = (Column)id2ChunkMap.get(address);
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

			Cube cube = cubemap.get(coords);

			if (cube != null && cube.unloaded) {
				cube.onUnload();
				cube.getColumn().removeCube(coords.getCubeY());
				cubemap.remove(cube.getCoords());

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
		return cubeGen.getPossibleCreatures(type, pos);
	}

	@Nullable
	public BlockPos getStrongholdGen(@Nonnull World worldIn, @Nonnull String name, @Nonnull BlockPos pos) {
		return cubeGen.getClosestStructure(name, pos);
	}

	@Override
	public int getLoadedChunkCount() {
		return this.id2ChunkMap.size();
	}

	@Override
	public boolean chunkExists(int cubeX, int cubeZ) {
		Chunk column = this.id2ChunkMap.get(ChunkPos.asLong(cubeX, cubeZ));
		return column != null && !column.unloaded;
	}

	//==============================
	//=====CubicChunks methods======
	//==============================

	@Override
	public Cube getCube(int cubeX, int cubeY, int cubeZ) {
		return getCube(new CubeCoords(cubeX, cubeY, cubeZ));
	}

	@Override
	public Cube getCube(CubeCoords coords) {
		return getCube(coords, Requirement.GENERATE);
	}

	@Override
	public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
		return getLoadedCube(new CubeCoords(cubeX, cubeY, cubeZ));
	}

	@Override
	public Cube getLoadedCube(CubeCoords coords) {
		Cube cube = cubemap.get(coords);
		if(cube != null){
			cube.unloaded = false;
		}
		return cube;
	}

	@Override
	@Nullable
	public Cube getCube(CubeCoords coords, Requirement req){
		if(coords.getMinBlockY() < worldServer.getMinHeight()){
			throw new IllegalStateException("Why are you getting a Cube so low? got: " + coords);
		}

		Cube cube = getLoadedCube(coords);
		if(req == Requirement.CACHE || 
				(cube != null && req.compareTo(Requirement.GENERATE) <= 0)){
			return cube;
		}

		// try to get the Column
		Column column = getColumn(coords.getCubeX(), coords.getCubeZ(), req);
		if(column == null){
			return cube; // Column did not reach req, so Cube also does not
		}

		if(cube == null){
			// try to load the Cube
			try {
				worldServer.getProfiler().startSection("cubeIOLoad");
				cube = this.cubeIO.loadCubeAndAddToColumn(column, coords.getCubeY());
			} catch (IOException ex) {
				log.error("Unable to load cube {}", coords, ex);
				return null;
			} finally {
				worldServer.getProfiler().endSection();
			}

			if(cube != null){
				column.addCube(cube);
				cubemap.put(coords, cube); // cache the Cube
				cube.onLoad();             // init the Cube

				if(!column.getLoadedCubes().contains(cube) || !cubemap.containsKey(coords)){
					System.out.println("error");
				}

				if(req.compareTo(Requirement.GENERATE) <= 0){
					return cube;
				}
			}else if(req == Requirement.LOAD){
				return null;
			}
		}

		if(cube == null){
			// generate the Cube
			ICubePrimer primer = cubeGen.generateCube(coords.getCubeX(), coords.getCubeY(), coords.getCubeZ());
			cube = new Cube(column, coords.getCubeY(), primer);

			column.addCube(cube);
			cubemap.put(coords, cube); // cache the Cube
			cube.onLoad();             // init the Cube

			if(req.compareTo(Requirement.GENERATE) <= 0){
				return cube;
			}
		}

		// forced full population of this Cube!
		if(!cube.isFullyPopulated()){
			Vec3i[] bounds = cubeGen.getPopRequirment(cube);
			for(int x = bounds[0].getX();x <= bounds[1].getX();x++){
				for(int y = bounds[0].getY();y <= bounds[1].getY();y++){
					for(int z = bounds[0].getZ();z <= bounds[1].getZ();z++){
						Cube popcube = getCube(new CubeCoords(x + coords.getCubeX(),
															  y + coords.getCubeY(),
															  z + coords.getCubeZ()));
						if(!popcube.isPopulated()){
							cubeGen.populate(popcube);
							popcube.setPopulated(true);
						}
					}
				}
			}
			cube.setFullyPopulated(true);
		}
		if(req == Requirement.POPULATE){
			return cube;
		}

		//TODO: Direct skylight might have changed and even Cubes that have there
		//      initial light done, there might be work to do for a cube that just loaded
		if(!cube.isInitialLightingDone()){
			for(int x = -2;x <= 2;x++){
				for(int y = -2;y <= 2;y++){
					for(int z = -2;z <= 2;z++){
						if(x != 0 || y != 0 || z != 0){
							getCube(coords.add(x, y, z)); // FirstLightProcessor is too soft and fluffy that it can't even ask for Cubes correctly!
						}
					}
				}
			}
			this.worldServer.getFirstLightProcessor().diffuseSkylight(cube);
		}

		return cube;
	}

	@Override
	@Nullable
	public Column getColumn(int columnX, int columnZ, Requirement req){
		Column column = getLoadedChunk(columnX, columnZ);
		if(column != null || req == Requirement.CACHE){
			return column;
		}

		try {
			column = this.cubeIO.loadColumn(columnX, columnZ);
		} catch (IOException ex) {
			log.error("Unable to load column ({},{})", columnX, columnZ, ex);
			return null;
		}
		if(column != null){
			id2ChunkMap.put(ChunkPos.asLong(columnX, columnZ), column);
			column.setLastSaveTime(this.worldServer.getTotalWorldTime()); // the column was just loaded
			column.onChunkLoad();
			return column;
		}else if(req == Requirement.LOAD){
			return null;
		}

		column = new Column(this, worldServer, columnX, columnZ);
		cubeGen.generateColumn(column);

		id2ChunkMap.put(ChunkPos.asLong(columnX, columnZ), column);
		column.setLastSaveTime(this.worldServer.getTotalWorldTime()); // the column was just generated
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
		column.getLoadedCubes().forEach(this::unloadCube);
		//unload that column
		//TODO: columns that have cubes near spawn will never be removed from unload queue
		//there is only a finite amount of them (about 1000) so it's not a big issue
		columnsToUnload.add(column.getChunkCoordIntPair());
		column.unloaded = true;
	}
}
