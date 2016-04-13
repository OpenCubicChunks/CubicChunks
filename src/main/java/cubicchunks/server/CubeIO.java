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
import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.ConcurrentBatchedQueue;
import cubicchunks.util.Coords;
import cubicchunks.world.ChunkSectionHelper;
import cubicchunks.world.IEntityActionListener;
import cubicchunks.world.OpacityIndex;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.IThreadedFileIO;
import net.minecraft.world.storage.ThreadedFileIOBase;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

import static cubicchunks.util.WorldServerAccess.getPendingTickListEntriesHashSet;
import static cubicchunks.util.WorldServerAccess.getPendingTickListEntriesThisTick;

public class CubeIO implements IThreadedFileIO {
	
	private static final Logger LOGGER = CubicChunks.LOGGER;
	
	private static class SaveEntry {
		
		private long address;
		private NBTTagCompound nbt;
		
		public SaveEntry(long address, NBTTagCompound nbt) {
			this.address = address;
			this.nbt = nbt;
		}
	}
	
	private static DB initializeDBConnection(final File saveFile, final WorldProvider dimension) {
		
		// init database connection
		LOGGER.info("Initializing db connection...");
		
		File file = new File(saveFile, String.format("cubes.dim%d.db", dimension.getDimension()));
		
		LOGGER.info("Connected to db at ()", file);
		
		file.getParentFile().mkdirs();
		
		DB db = DBMaker.newFileDB(file).closeOnJvmShutdown()
		// .compressionEnable()
			.make();
		
		return db;
		// NOTE: could set different cache settings
		// the default is a hash map cache with 32768 entries
		// see: http://www.mapdb.org/features.html
	}
	
	private World world;
	
	private DB db;
	private ConcurrentNavigableMap<Long,byte[]> columns;
	private ConcurrentNavigableMap<Long,byte[]> cubes;
	private ConcurrentBatchedQueue<SaveEntry> columnsToSave;
	private ConcurrentBatchedQueue<SaveEntry> cubesToSave;
	
	public CubeIO(World world) {
		
		this.world = world;
		
		this.db = initializeDBConnection(this.world.getSaveHandler().getWorldDirectory(), this.world.provider);
		
		this.columns = this.db.getTreeMap("columns");
		this.cubes = this.db.getTreeMap("chunks");
		
		// init chunk save queue
		this.columnsToSave = new ConcurrentBatchedQueue<SaveEntry>();
		this.cubesToSave = new ConcurrentBatchedQueue<SaveEntry>();
	}
	
	public boolean columnExists(long address) {
		return this.columns.containsKey(address);
	}
	
	public Column loadColumn(int chunkX, int chunkZ) throws IOException {
		// does the database have the column?
		long address = AddressTools.getAddress(chunkX, chunkZ);
		byte[] data = this.columns.get(address);
		if (data == null) {
			// returning null tells the world to generate a new column
			return null;
		}
		
		// read the NBT
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		NBTTagCompound nbt = CompressedStreamTools.readCompressed(in);
		in.close();
		
		// restore the column
		return readColumnFromNBT(chunkX, chunkZ, nbt);
	}
	
	public boolean cubeExists(long address) {
		return this.cubes.containsKey(address);
	}
	
	public Cube loadCubeAndAddToColumn(Column column, long address) throws IOException {
		// does the database have the cube?
		byte[] data = this.cubes.get(address);
		if (data == null) {
			return null;
		}
		
		// read the NBT
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		NBTTagCompound nbt = CompressedStreamTools.readCompressed(in);
		in.close();
		
		// restore the cube
		int cubeX = AddressTools.getX(address);
		int cubeY = AddressTools.getY(address);
		int cubeZ = AddressTools.getZ(address);
		return readCubeFromNbtAndAddToColumn(column, cubeX, cubeY, cubeZ, nbt);
	}
	
	public void saveColumn(Column column) {
		// NOTE: this function blocks the world thread
		// make it as fast as possible by offloading processing to the IO thread
		// except we have to write the NBT in this thread to avoid problems
		// with concurrent access to world data structures
		
		// add the column to the save queue
		this.columnsToSave.add(new SaveEntry(column.getAddress(), IONbtWriter.writeColumnToNbt(column)));
		column.markSaved();
		
		// signal the IO thread to process the save queue
		ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
	}
	
	public void saveCube(Cube cube) {
		// NOTE: this function blocks the world thread, so make it fast
		
		this.cubesToSave.add(new SaveEntry(cube.getAddress(), writeCubeToNbt(cube)));
		cube.markSaved();
		
		// signal the IO thread to process the save queue
		ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
	}
	
	@Override
	public boolean writeNextIO() {
		
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
		boolean hasMoreColumns = this.columnsToSave.getBatch(entries, ColumnsBatchSize);
		for (SaveEntry entry : entries) {
			try {
				// save the column
				byte[] data = IONbtWriter.writeNbtBytes(entry.nbt);
				this.columns.put(entry.address, data);
				
				numColumnsSaved++;
				numColumnBytesSaved += data.length;
			} catch (Throwable t) {
				LOGGER.error("Unable to write column {},{}",
					AddressTools.getX(entry.address),
					AddressTools.getZ(entry.address),
					t
				);
			}
		}
		entries.clear();
		
		// save a batch of cubes
		boolean hasMoreCubes = this.cubesToSave.getBatch(entries, CubesBatchSize);
		for (SaveEntry entry : entries) {
			try {
				// save the cube
				byte[] data = IONbtWriter.writeNbtBytes(entry.nbt);
				this.cubes.put(entry.address, data);
				
				numCubesSaved++;
				numCubeBytesSaved += data.length;
			} catch (Throwable t) {
				LOGGER.error("Unable to write cube {},{},{}",
					AddressTools.getX(entry.address),
					AddressTools.getY(entry.address),
					AddressTools.getZ(entry.address),
					t
				);
			}
		}
		entries.clear();
		
		numColumnsRemaining = this.columnsToSave.size();
		numCubesRemaining = this.cubesToSave.size();
		
		// flush changes to disk
		this.db.commit();
		
		long diff = System.currentTimeMillis() - start;
		LOGGER.debug("Wrote {} columns ({} remaining) ({}k) and {} cubes ({} remaining) ({}k) in {} ms",
			numColumnsSaved, numColumnsRemaining, numColumnBytesSaved / 1024,
			numCubesSaved, numCubesRemaining, numCubeBytesSaved / 1024, diff
		);
		
		return hasMoreColumns || hasMoreCubes;
	}
	
	private Column readColumnFromNBT(final int x, final int z, NBTTagCompound nbt) {
		
		// check the version number
		byte version = nbt.getByte("v");
		if (version != 1) {
			LOGGER.warn(String.format("Column has wrong version: %d. Column will be regenerated.", version));
			return null;
		}
		
		// check the coords
		int xCheck = nbt.getInteger("x");
		int zCheck = nbt.getInteger("z");
		if (xCheck != x || zCheck != z) {
			LOGGER.warn(String.format("Column is corrupted! Expected (%d,%d) but got (%d,%d). Column will be regenerated.", x, z, xCheck, zCheck));
			return null;
		}
		
		// create the column
		Column column = new Column(this.world, x, z);
		
		// read the rest of the column properties
		column.setTerrainPopulated(nbt.getBoolean("TerrainPopulated"));
		column.setInhabitedTime(nbt.getLong("InhabitedTime"));
		
		// biomes
		column.setBiomeArray(nbt.getByteArray("Biomes"));
		
		// read light index
		((OpacityIndex)column.getOpacityIndex()).readData(nbt.getByteArray("OpacityIndex"));
		
		// entities
		column.getEntityContainer().readFromNbt(nbt, "Entities", this.world, entity -> {
			entity.addedToChunk = true;
			entity.chunkCoordX = x;
			entity.chunkCoordY = Coords.getCubeYForEntity(entity);
			entity.chunkCoordZ = z;
		});
		
		return column;
	}
	
	private Cube readCubeFromNbtAndAddToColumn(Column column, final int cubeX, final int cubeY, final int cubeZ, NBTTagCompound nbt) {
		// NBT types:
		// 0 1 2 3 4 5 6 7 8 9 10 11
		// "END", "BYTE", "SHORT", "INT", "LONG", "FLOAT", "DOUBLE", "BYTE[]", "STRING", "LIST", "COMPOUND", "INT[]"
		
		// check the version number
		byte version = nbt.getByte("v");
		if (version != 1) {
			throw new IllegalArgumentException("Cube has wrong version! " + version);
		}
		
		// check the coordinates
		int xCheck = nbt.getInteger("x");
		int yCheck = nbt.getInteger("y");
		int zCheck = nbt.getInteger("z");
		if (xCheck != cubeX || yCheck != cubeY || zCheck != cubeZ) {
			throw new Error(String.format("Cube is corrupted! Expected (%d,%d,%d) but got (%d,%d,%d)", cubeX, cubeY, cubeZ, xCheck, yCheck, zCheck));
		}
		
		// check against column
		if (cubeX != column.xPosition || cubeZ != column.zPosition) {
			throw new Error(String.format("Cube is corrupted! Cube (%d,%d,%d) does not match column (%d,%d)", cubeX, cubeY, cubeZ, column.xPosition, column.zPosition));
		}
		
		// build the cube
		boolean hasSky = !this.world.provider.getHasNoSky();
		final Cube cube = column.getOrCreateCube(cubeY, false);
		
		// get the generator stage
		cube.setGeneratorStage(GeneratorStage.values()[nbt.getByte("GeneratorStage")]);
		
		// is this an empty cube?
		boolean isEmpty = !nbt.hasKey("Blocks");
		cube.setEmpty(isEmpty);
		if (!isEmpty) {
			ExtendedBlockStorage storage = cube.getStorage();
			
			// block ids and metadata (ie block states)
			byte[] blockIdLsbs = nbt.getByteArray("Blocks");
			NibbleArray blockIdMsbs = null;
			if (nbt.hasKey("Add")) {
				blockIdMsbs = new NibbleArray(nbt.getByteArray("Add"));
			}
			NibbleArray blockMetadata = new NibbleArray(nbt.getByteArray("Data"));
			ChunkSectionHelper.setBlockStates(storage, blockIdLsbs, blockIdMsbs, blockMetadata);
			
			// lights
			storage.setBlocklightArray(new NibbleArray(nbt.getByteArray("BlockLight")));
			if (hasSky) {
				storage.setSkylightArray(new NibbleArray(nbt.getByteArray("SkyLight")));
			}
			storage.removeInvalidBlocks();
		}
		
		// entities
		cube.getEntityContainer().readFromNbt(nbt, "Entities", this.world, new IEntityActionListener() {
			
			@Override
			public void onEntity(Entity entity) {
				// make sure this entity is really in the chunk
				int entityCubeX = Coords.getCubeXForEntity(entity);
				int entityCubeY = Coords.getCubeYForEntity(entity);
				int entityCubeZ = Coords.getCubeZForEntity(entity);
				if (entityCubeX != cube.getX() || entityCubeY != cube.getY() || entityCubeZ != cube.getZ()) {
					LOGGER.warn(String.format("Loaded entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)!", entity.getClass().getName(), entityCubeX, entityCubeY, entityCubeZ, cube.getX(), cube.getY(), cube.getZ()));
				}
				
				entity.addedToChunk = true;
				entity.chunkCoordX = cubeX;
				entity.chunkCoordY = cubeY;
				entity.chunkCoordZ = cubeZ;
			}
		});
		
		// tile entities
		NBTTagList nbtTileEntities = nbt.getTagList("TileEntities", 10);
		if (nbtTileEntities != null) {
			for (int i = 0; i < nbtTileEntities.tagCount(); i++) {
				NBTTagCompound nbtTileEntity = nbtTileEntities.getCompoundTagAt(i);
				TileEntity blockEntity = TileEntity.createTileEntity(world.getMinecraftServer(), nbtTileEntity);
				if (blockEntity != null) {
					column.addTileEntity(blockEntity);
				}
			}
		}
		
		// scheduled block ticks
		NBTTagList nbtScheduledTicks = nbt.getTagList("TileTicks", 10);
		if (nbtScheduledTicks != null) {
			for (int i = 0; i < nbtScheduledTicks.tagCount(); i++) {
				NBTTagCompound nbtScheduledTick = nbtScheduledTicks.getCompoundTagAt(i);
				this.world.addBlockEvent(
					new BlockPos(
						nbtScheduledTick.getInteger("x"),
						nbtScheduledTick.getInteger("y"),
						nbtScheduledTick.getInteger("z")
					),
					Block.getBlockById(nbtScheduledTick.getInteger("i")),
					nbtScheduledTick.getInteger("t"),
					nbtScheduledTick.getInteger("p")
				);
			}
		}
		
		// check to see if the cube needs to be relit
		if (nbt.hasKey("OpacityIndex")) {
			int savedHash = nbt.getInteger("OpacityIndex");
			int currentHash = column.getOpacityIndex().hashCode();
			cube.setNeedsRelightAfterLoad(savedHash != currentHash);
		} else {
			cube.setNeedsRelightAfterLoad(true);
		}
		
		return cube;
	}
	
	private static class IONbtWriter {
		private static byte[] writeNbtBytes(NBTTagCompound nbt) throws IOException {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(buf);
			CompressedStreamTools.writeCompressed(nbt, out);
			out.close();
			return buf.toByteArray();
		}
		
		private static NBTTagCompound writeColumnToNbt(Column column) {
			NBTTagCompound nbt = new NBTTagCompound();
			
			// coords
			nbt.setInteger("x", column.xPosition);
			nbt.setInteger("z", column.zPosition);
			
			// column properties
			nbt.setByte("v", (byte)1);
			nbt.setBoolean("TerrainPopulated", column.isTerrainPopulated());
			nbt.setLong("InhabitedTime", column.getInhabitedTime());
			
			// biome mappings
			nbt.setByteArray("Biomes", column.getBiomeArray());
			
			// light index
			nbt.setByteArray("OpacityIndex", ((OpacityIndex)column.getOpacityIndex()).getData());
			
			// entities
			column.getEntityContainer().writeToNbt(nbt, "Entities");
			
			return nbt;
		}
	}
	
	private static NBTTagCompound writeCubeToNbt(final Cube cube) {
		
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("v", (byte)1);
		
		// coords
		nbt.setInteger("x", cube.getX());
		nbt.setInteger("y", cube.getY());
		nbt.setInteger("z", cube.getZ());
		
		nbt.setByte("GeneratorStage", (byte)cube.getGeneratorStage().ordinal());
		
		if (!cube.isEmpty()) {
			
			// blocks
			ExtendedBlockStorage storage = cube.getStorage();
			byte[] idLsb = new byte[4096];
			byte[] idMsb = new byte[2048];
			byte[] meta = new byte[2048];
			int flags = ChunkSectionHelper.getBlockDataArray(storage, idLsb, idMsb, meta);
			nbt.setByteArray("Blocks", idLsb);

			if ((flags & ChunkSectionHelper.HAS_MSB) != 0) {
				nbt.setByteArray("Add", idMsb);
			}
			
			// metadata
			if((flags & ChunkSectionHelper.HAS_META) != 0) {
				nbt.setByteArray("Data", meta);
			}
			// light
			nbt.setByteArray("BlockLight", storage.getBlocklightArray().getData());
			if (storage.getSkylightArray() != null) {
				nbt.setByteArray("SkyLight", storage.getSkylightArray().getData());
			}
		}
		
		// entities
		cube.getEntityContainer().writeToNbt(nbt, "Entities", new IEntityActionListener() {
			
			@Override
			public void onEntity(Entity entity) {
				// make sure this entity is really in the chunk
				int cubeX = Coords.getCubeXForEntity(entity);
				int cubeY = Coords.getCubeYForEntity(entity);
				int cubeZ = Coords.getCubeZForEntity(entity);
				if (cubeX != cube.getX() || cubeY != cube.getY() || cubeZ != cube.getZ()) {
					LOGGER.warn(String.format("Saved entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)! Entity thinks its in (%d,%d,%d)",
						entity.getClass().getName(),
						cubeX, cubeY, cubeZ,
						cube.getX(), cube.getY(), cube.getZ(),
						entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ
					));
				}
			}
		});
		
		// tile entities
		NBTTagList nbtTileEntities = new NBTTagList();
		nbt.setTag("TileEntities", nbtTileEntities);
		for (TileEntity blockEntity : cube.getTileEntityMap()) {
			NBTTagCompound nbtTileEntity = new NBTTagCompound();
			blockEntity.writeToNBT(nbtTileEntity);
			nbtTileEntities.appendTag(nbtTileEntity);
		}
		
		// scheduled block ticks
		Iterable<NextTickListEntry> scheduledTicks = getScheduledTicks(cube);
		if (scheduledTicks != null) {
			long time = cube.getWorld().getTotalWorldTime();
			
			NBTTagList nbtTicks = new NBTTagList();
			nbt.setTag("TileTicks", nbtTicks);
			for (NextTickListEntry scheduledTick : scheduledTicks) {
				NBTTagCompound nbtScheduledTick = new NBTTagCompound();
				nbtScheduledTick.setInteger("i", Block.getIdFromBlock(scheduledTick.getBlock()));
				nbtScheduledTick.setInteger("x", scheduledTick.position.getX());
				nbtScheduledTick.setInteger("y", scheduledTick.position.getY());
				nbtScheduledTick.setInteger("z", scheduledTick.position.getZ());
				nbtScheduledTick.setInteger("t", (int)(scheduledTick.scheduledTime - time));
				nbtScheduledTick.setInteger("p", scheduledTick.priority);
				nbtTicks.appendTag(nbtScheduledTick);
			}
		}
		
		// opacity index hash
		nbt.setInteger("OpacityIndex", cube.getColumn().getOpacityIndex().hashCode());
		
		return nbt;
	}
	
	private static List<NextTickListEntry> getScheduledTicks(Cube cube) {
		ArrayList<NextTickListEntry> out = new ArrayList<>();
		
		// make sure this is a server
		if (!(cube.getWorld() instanceof WorldServer)) {
			throw new Error("Column is not on the server!");
		}
		WorldServer worldServer = (WorldServer)cube.getWorld();
		
		// copy the ticks for this cube
		copyScheduledTicks(out, getPendingTickListEntriesHashSet(worldServer), cube);
		copyScheduledTicks(out, getPendingTickListEntriesThisTick(worldServer), cube);
		
		return out;
	}
	
	private static void copyScheduledTicks(ArrayList<NextTickListEntry> out, Collection<NextTickListEntry> scheduledTicks, Cube cube) {
		for (NextTickListEntry scheduledTick : scheduledTicks) {
			if (cube.containsBlockPos(scheduledTick.position)) {
				out.add(scheduledTick);
			}
		}
	}
}
