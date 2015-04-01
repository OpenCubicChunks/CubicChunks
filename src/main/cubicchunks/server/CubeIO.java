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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.Dimension;
import net.minecraft.world.ScheduledBlockTick;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ChunkSection;
import net.minecraft.world.storage.FileIOWorker;
import net.minecraft.world.storage.IThreadedFileIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.ConcurrentBatchedQueue;
import cubicchunks.util.Coords;
import cubicchunks.world.ChunkSectionHelper;
import cubicchunks.world.Column;
import cubicchunks.world.Cube;
import cubicchunks.world.EntityActionListener;

public class CubeIO implements IThreadedFileIO {
	
	private static final Logger log = LogManager.getLogger();
	
	private static class SaveEntry {
		
		private long address;
		private NbtTagCompound nbt;
		
		public SaveEntry(long address, NbtTagCompound nbt) {
			this.address = address;
			this.nbt = nbt;
		}
	}
	
	private DB m_db;
	private ConcurrentNavigableMap<Long,byte[]> m_columns;
	private ConcurrentNavigableMap<Long,byte[]> m_cubes;
	private ConcurrentBatchedQueue<SaveEntry> m_columnsToSave;
	private ConcurrentBatchedQueue<SaveEntry> m_cubesToSave;
	
	public CubeIO(File saveFile, Dimension dimension) {
		
		// init database connection
		File file = new File(saveFile, String.format("cubes.dim%d.db", dimension.getId()));
		
		file.getParentFile().mkdirs();
		m_db = DBMaker.newFileDB(file).closeOnJvmShutdown()
		// .compressionEnable()
			.make();
		
		// NOTE: could set different cache settings
		// the default is a hash map cache with 32768 entries
		// see: http://www.mapdb.org/features.html
		
		m_columns = m_db.getTreeMap("columns");
		m_cubes = m_db.getTreeMap("chunks");
		
		// init chunk save queue
		m_columnsToSave = new ConcurrentBatchedQueue<SaveEntry>();
		m_cubesToSave = new ConcurrentBatchedQueue<SaveEntry>();
	}
	
	public boolean columnExists(long address) {
		return m_columns.containsKey(address);
	}
	
	public Column loadColumn(World world, int cubeX, int cubeZ) throws IOException {
		// does the database have the column?
		long address = AddressTools.getAddress(cubeX, cubeZ);
		byte[] data = m_columns.get(address);
		if (data == null) {
			// returning null tells the world to generate a new column
			return null;
		}
		
		// read the NBT
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		NbtTagCompound nbt = CompressedStreamTools.readCompressedInputStream(in);
		in.close();
		
		// restore the column
		return readColumnFromNBT(world, cubeX, cubeZ, nbt);
	}
	
	public boolean cubeExists(long address) {
		return m_cubes.containsKey(address);
	}
	
	public Cube loadCubeAndAddToColumn(World world, Column column, long address) throws IOException {
		// does the database have the cube?
		byte[] data = m_cubes.get(address);
		if (data == null) {
			return null;
		}
		
		// read the NBT
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		NbtTagCompound nbt = CompressedStreamTools.readCompressedInputStream(in);
		in.close();
		
		// restore the cube
		int x = AddressTools.getX(address);
		int y = AddressTools.getY(address);
		int z = AddressTools.getZ(address);
		return readCubeFromNbtAndAddToColumn(world, column, x, y, z, nbt);
	}
	
	public void saveColumn(Column column) {
		// NOTE: this function blocks the world thread
		// make it as fast as possible by offloading processing to the IO thread
		// except we have to write the NBT in this thread to avoid problems
		// with concurrent access to world data structures
		
		// add the column to the save queue
		m_columnsToSave.add(new SaveEntry(column.getAddress(), writeColumnToNbt(column)));
		column.markSaved();
		
		// signal the IO thread to process the save queue
		FileIOWorker.getThread().queueIO(this);
	}
	
	public void saveCube(Cube cube) {
		// NOTE: this function blocks the world thread, so make it fast
		
		m_cubesToSave.add(new SaveEntry(cube.getAddress(), writeCubeToNbt(cube)));
		cube.markSaved();
		
		// signal the IO thread to process the save queue
		FileIOWorker.getThread().queueIO(this);
	}
	
	@Override
	public boolean tryWrite() {
		
		// NOTE: return true to redo this call (used for batching)
		
		final int ColumnsBatchSize = 25;
		final int CubesBatchSize = 250;
		
		int numColumnsSaved = 0;
		int numColumnsRemaining = 0;
		int numColumnBytesSaved = 0;
		int numCubesSaved = 0;
		int numCubesRemaining = 0;
		int numCubeBytesSaved = 0;
		long start = System.currentTimeMillis();
		
		List<SaveEntry> entries = new ArrayList<SaveEntry>(Math.max(ColumnsBatchSize, CubesBatchSize));
		
		// save a batch of columns
		boolean hasMoreColumns = m_columnsToSave.getBatch(entries, ColumnsBatchSize);
		for (SaveEntry entry : entries) {
			try {
				// save the column
				byte[] data = writeNbtBytes(entry.nbt);
				m_columns.put(entry.address, data);
				
				numColumnsSaved++;
				numColumnBytesSaved += data.length;
			} catch (Throwable t) {
				log.error("Unable to write column {},{}",
					AddressTools.getX(entry.address),
					AddressTools.getZ(entry.address),
					t
				);
			}
		}
		entries.clear();
		
		// save a batch of cubes
		boolean hasMoreCubes = m_cubesToSave.getBatch(entries, CubesBatchSize);
		for (SaveEntry entry : entries) {
			try {
				// save the cube
				byte[] data = writeNbtBytes(entry.nbt);
				m_cubes.put(entry.address, data);
				
				numCubesSaved++;
				numCubeBytesSaved += data.length;
			} catch (Throwable t) {
				log.error("Unable to write cube {},{},{}",
					AddressTools.getX(entry.address),
					AddressTools.getY(entry.address),
					AddressTools.getZ(entry.address),
					t
				);
			}
		}
		entries.clear();
		
		numColumnsRemaining = m_columnsToSave.size();
		numCubesRemaining = m_cubesToSave.size();
		
		// flush changes to disk
		m_db.commit();
		
		long diff = System.currentTimeMillis() - start;
		log.info("Wrote {} columns ({} remaining) ({}k) and {} cubes ({} remaining) ({}k) in {} ms",
			numColumnsSaved, numColumnsRemaining, numColumnBytesSaved / 1024,
			numCubesSaved, numCubesRemaining, numCubeBytesSaved / 1024, diff
		);
		
		return hasMoreColumns || hasMoreCubes;
	}
	
	private byte[] writeNbtBytes(NbtTagCompound nbt) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buf);
		CompressedStreamTools.writeNbtMapToOutputStream(nbt, out);
		out.close();
		return buf.toByteArray();
	}
	
	private NbtTagCompound writeColumnToNbt(Column column) {
		NbtTagCompound nbt = new NbtTagCompound();
		
		// coords
		nbt.put("x", column.chunkX);
		nbt.put("z", column.chunkZ);
		
		// column properties
		nbt.put("v", (byte)1);
		nbt.setBoolean("TerrainPopulated", column.isTerrainPopulated());
		nbt.put("InhabitedTime", column.getInhabitedTime());
		
		// biome mappings
		nbt.put("Biomes", column.getBiomeMap());
		
		// light index
		nbt.put("LightIndex", column.getLightIndex().getData());
		
		// entities
		column.getEntityContainer().writeToNbt(nbt, "Entities");
		
		return nbt;
	}
	
	private Column readColumnFromNBT(World world, final int x, final int z, NbtTagCompound nbt) {
		
		// check the version number
		byte version = nbt.getAsByte("v");
		if (version != 1) {
			log.warn(String.format("Column has wrong version: %d. Column will be regenerated.", version));
			return null;
		}
		
		// check the coords
		int xCheck = nbt.getAsInt("x");
		int zCheck = nbt.getAsInt("z");
		if (xCheck != x || zCheck != z) {
			log.warn(String.format("Column is corrupted! Expected (%d,%d) but got (%d,%d). Column will be regenerated.", x, z, xCheck, zCheck));
			return null;
		}
		
		// create the column
		Column column = new Column(world, x, z);
		
		// read the rest of the column properties
		column.setTerrainPopulated(nbt.getAsBoolean("TerrainPopulated"));
		column.setInhabitedTime(nbt.getAsLong("InhabitedTime"));
		
		// biomes
		column.setBiomeMap(nbt.getAsByteArray("Biomes"));
		
		// read light index
		column.getLightIndex().readData(nbt.getAsByteArray("LightIndex"));
		
		// entities
		column.getEntityContainer().readFromNbt(nbt, "Entities", world, new EntityActionListener() {
			
			@Override
			public void onEntity(Entity entity) {
				entity.addedToChunk = true;
				entity.chunkX = x;
				entity.chunkY = Coords.getCubeYForEntity(entity);
				entity.chunkZ = z;
			}
		});
		
		return column;
	}
	
	private NbtTagCompound writeCubeToNbt(final Cube cube) {
		
		NbtTagCompound nbt = new NbtTagCompound();
		nbt.put("v", (byte)1);
		
		// coords
		nbt.put("x", cube.getX());
		nbt.put("y", cube.getY());
		nbt.put("z", cube.getZ());
		
		nbt.put("GeneratorStage", (byte)cube.getGeneratorStage().ordinal());
		
		if (!cube.isEmpty()) {
			
			// blocks
			ChunkSection storage = cube.getStorage();
			nbt.put("Blocks", ChunkSectionHelper.getBlockLSBArray(storage));
			NibbleArray msbArray = ChunkSectionHelper.getBlockMSBArray(storage);
			if (msbArray != null) {
				nbt.put("Add", msbArray.get());
			}
			
			// metadata
			nbt.put("Data", ChunkSectionHelper.getBlockMetaArray(storage).get());
			
			// light
			nbt.put("BlockLight", storage.getBlockLightArray().get());
			if (storage.getSkyLightArray() != null) {
				nbt.put("SkyLight", storage.getSkyLightArray().get());
			}
		}
		
		// entities
		cube.getEntityContainer().writeToNbt(nbt, "Entities", new EntityActionListener() {
			
			@Override
			public void onEntity(Entity entity) {
				// make sure this entity is really in the chunk
				int cubeX = Coords.getCubeXForEntity(entity);
				int cubeY = Coords.getCubeYForEntity(entity);
				int cubeZ = Coords.getCubeZForEntity(entity);
				if (cubeX != cube.getX() || cubeY != cube.getY() || cubeZ != cube.getZ()) {
					log.warn(String.format("Saved entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)! Entity thinks its in (%d,%d,%d)",
						entity.getClass().getName(),
						cubeX, cubeY, cubeZ,
						cube.getX(), cube.getY(), cube.getZ(),
						entity.chunkX, entity.chunkY, entity.chunkZ
					));
				}
			}
		});
		
		// tile entities
		NbtList nbtTileEntities = new NbtList();
		nbt.put("TileEntities", nbtTileEntities);
		for (BlockEntity blockEntity : cube.getBlockEntities()) {
			NbtTagCompound nbtTileEntity = new NbtTagCompound();
			blockEntity.save(nbtTileEntity);
			nbtTileEntities.add(nbtTileEntity);
		}
		
		// scheduled block ticks
		Iterable<ScheduledBlockTick> scheduledTicks = getScheduledTicks(cube);
		if (scheduledTicks != null) {
			long time = cube.getWorld().getGameTime();
			
			NbtList nbtTicks = new NbtList();
			nbt.put("TileTicks", nbtTicks);
			for (ScheduledBlockTick scheduledTick : scheduledTicks) {
				NbtTagCompound nbtScheduledTick = new NbtTagCompound();
				nbtScheduledTick.put("i", Block.getBlockIndex(scheduledTick.getBlock()));
				nbtScheduledTick.put("x", scheduledTick.blockPos.getX());
				nbtScheduledTick.put("y", scheduledTick.blockPos.getY());
				nbtScheduledTick.put("z", scheduledTick.blockPos.getZ());
				nbtScheduledTick.put("t", (int)(scheduledTick.scheduledTime - time));
				nbtScheduledTick.put("p", scheduledTick.priority);
				nbtTicks.add(nbtScheduledTick);
			}
		}
		
		return nbt;
	}
	
	private Cube readCubeFromNbtAndAddToColumn(World world, Column column, final int x, final int y, final int z, NbtTagCompound nbt) {
		// NBT types:
		// 0 1 2 3 4 5 6 7 8 9 10 11
		// "END", "BYTE", "SHORT", "INT", "LONG", "FLOAT", "DOUBLE", "BYTE[]", "STRING", "LIST", "COMPOUND", "INT[]"
		
		// check the version number
		byte version = nbt.getAsByte("v");
		if (version != 1) {
			throw new IllegalArgumentException("Cube has wrong version! " + version);
		}
		
		// check the coordinates
		int xCheck = nbt.getAsInt("x");
		int yCheck = nbt.getAsInt("y");
		int zCheck = nbt.getAsInt("z");
		if (xCheck != x || yCheck != y || zCheck != z) {
			throw new Error(String.format("Cube is corrupted! Expected (%d,%d,%d) but got (%d,%d,%d)", x, y, z, xCheck, yCheck, zCheck));
		}
		
		// check against column
		if (x != column.chunkX || z != column.chunkZ) {
			throw new Error(String.format("Cube is corrupted! Cube (%d,%d,%d) does not match column (%d,%d)", x, y, z, column.chunkX, column.chunkZ));
		}
		
		// build the cube
		boolean hasSky = !world.dimension.hasNoSky();
		final Cube cube = column.getOrCreateCube(y, false);
		
		// get the generator stage
		cube.setGeneratorStage(GeneratorStage.values()[nbt.getAsByte("GeneratorStage")]);
		
		// is this an empty cube?
		boolean isEmpty = !nbt.containsKey("Blocks");
		cube.setEmpty(isEmpty);
		if (!isEmpty) {
			ChunkSection storage = cube.getStorage();
			
			// block ids and metadata (ie block states)
			byte[] blockIdLsbs = nbt.getAsByteArray("Blocks");
			NibbleArray blockIdMsbs = null;
			if (nbt.containsKey("Add")) {
				blockIdMsbs = new NibbleArray(nbt.getAsByteArray("Add"));
			}
			NibbleArray blockMetadata = new NibbleArray(nbt.getAsByteArray("Data"));
			ChunkSectionHelper.setBlockStates(storage, blockIdLsbs, blockIdMsbs, blockMetadata);
			
			// lights
			storage.setBlockLightArray(new NibbleArray(nbt.getAsByteArray("BlockLight")));
			if (hasSky) {
				storage.setSkyLightArray(new NibbleArray(nbt.getAsByteArray("SkyLight")));
			}
			storage.countBlocksInSection();
		}
		
		// entities
		cube.getEntityContainer().readFromNbt(nbt, "Entities", world, new EntityActionListener() {
			
			@Override
			public void onEntity(Entity entity) {
				// make sure this entity is really in the chunk
				int cubeX = Coords.getCubeXForEntity(entity);
				int cubeY = Coords.getCubeYForEntity(entity);
				int cubeZ = Coords.getCubeZForEntity(entity);
				if (cubeX != cube.getX() || cubeY != cube.getY() || cubeZ != cube.getZ()) {
					log.warn(String.format("Loaded entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)!", entity.getClass().getName(), cubeX, cubeY, cubeZ, cube.getX(), cube.getY(), cube.getZ()));
				}
				
				entity.addedToChunk = true;
				entity.chunkX = x;
				entity.chunkY = y;
				entity.chunkZ = z;
			}
		});
		
		// tile entities
		NbtList nbtTileEntities = nbt.getAsNbtList("TileEntities", 10);
		if (nbtTileEntities != null) {
			for (int i = 0; i < nbtTileEntities.getSize(); i++) {
				NbtTagCompound nbtTileEntity = nbtTileEntities.getAsNbtMap(i);
				BlockEntity blockEntity = BlockEntity.createAndLoadEntity(nbtTileEntity);
				if (blockEntity != null) {
					column.setBlockEntity(blockEntity);
				}
			}
		}
		
		// scheduled block ticks
		NbtList nbtScheduledTicks = nbt.getAsNbtList("TileTicks", 10);
		if (nbtScheduledTicks != null) {
			for (int i = 0; i < nbtScheduledTicks.getSize(); i++) {
				NbtTagCompound nbtScheduledTick = nbtScheduledTicks.getAsNbtMap(i);
				world.scheduleBlockTickForced(
					new BlockPos(
						nbtScheduledTick.getAsInt("x"),
						nbtScheduledTick.getAsInt("y"),
						nbtScheduledTick.getAsInt("z")
					),
					Block.getBlockFromIndex(nbtScheduledTick.getAsInt("i")),
					nbtScheduledTick.getAsInt("t"),
					nbtScheduledTick.getAsInt("p")
				);
			}
		}
		
		return cube;
	}
	
	private List<ScheduledBlockTick> getScheduledTicks(Cube cube) {
		ArrayList<ScheduledBlockTick> out = new ArrayList<ScheduledBlockTick>();
		
		// make sure this is a server
		if (!(cube.getWorld() instanceof WorldServer)) {
			throw new Error("Column is not on the server!");
		}
		WorldServer worldServer = (WorldServer)cube.getWorld();
		
		// copy the ticks for this cube
		copyScheduledTicks(out, worldServer.lastSyncedTickNextTick, cube);
		copyScheduledTicks(out, worldServer.tickNextTick, cube);
		
		return out;
	}
	
	private void copyScheduledTicks(ArrayList<ScheduledBlockTick> out, Collection<ScheduledBlockTick> scheduledTicks, Cube cube) {
		for (ScheduledBlockTick scheduledTick : scheduledTicks) {
			if (cube.containsBlockPos(scheduledTick.blockPos)) {
				out.add(scheduledTick);
			}
		}
	}
}
