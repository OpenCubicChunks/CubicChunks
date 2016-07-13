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
package cubicchunks.server.chunkio;

import cubicchunks.CubicChunks;
import cubicchunks.util.AddressTools;
import cubicchunks.util.ConcurrentBatchedQueue;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.IThreadedFileIO;
import net.minecraft.world.storage.ThreadedFileIOBase;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

import static cubicchunks.util.AddressTools.getX;
import static cubicchunks.util.AddressTools.getY;
import static cubicchunks.util.AddressTools.getZ;

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

		LOGGER.info("Connected to db at {}", file);

		file.getParentFile().mkdirs();

		DB db = DBMaker.fileDB(file).transactionEnable().make();
		return db;
		// NOTE: could set different cache settings
		// the default is a hash map cache with 32768 entries
		// see: http://www.mapdb.org/features.html
	}

	private ICubicWorldServer world;

	private final DB db;
	private ConcurrentNavigableMap<Long, byte[]> columns;
	private ConcurrentNavigableMap<Long, byte[]> cubes;
	private ConcurrentBatchedQueue<SaveEntry> columnsToSave;
	private ConcurrentBatchedQueue<SaveEntry> cubesToSave;

	private final Thread theShutdownHook;

	public CubeIO(ICubicWorldServer world) {
		this.world = world;

		this.db = initializeDBConnection(this.world.getSaveHandler().getWorldDirectory(), this.world.getProvider());
		//we can't use closeOnJvmShutdown() because Minecraft saves all unsaved things on shutdown
		//so the DB will be closed while we are still saving.
		//also we need to save the thread into field because in client environment we need to remove the shutdown hook
		Runtime.getRuntime().addShutdownHook(theShutdownHook = new Thread() {
			public void run() {
				try {
					ThreadedFileIOBase.getThreadedIOInstance().waitForFinish();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					if (!CubeIO.this.db.isClosed()) {
						CubeIO.this.db.close();
					}
				}

			}
		});
		this.columns = this.db.treeMap("columns", Serializer.LONG, Serializer.BYTE_ARRAY).createOrOpen();
		this.cubes = this.db.treeMap("chunks", Serializer.LONG, Serializer.BYTE_ARRAY).createOrOpen();

		// init chunk save queue
		this.columnsToSave = new ConcurrentBatchedQueue<>();
		this.cubesToSave = new ConcurrentBatchedQueue<>();
	}

	public void flush() {
		if (columnsToSave.size() != 0 || cubesToSave.size() != 0) {
			err("Attempt to flush() CubeIO when there are remaining cubes to save! Saving remaining cubes to avoid corruption");
			while (this.writeNextIO()) ;
		}
		if (!Runtime.getRuntime().removeShutdownHook(theShutdownHook)) {
			err("WARNING!!!");
			err("Shutdown hook removing failed!");
			err("This may cause memory leak and/or crash");
		}
		if (!this.db.isClosed()) {
			this.db.close();
		} else {
			err("DB already closed!");
		}
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
		NBTTagCompound nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(data));

		// restore the column
		return IONbtReader.readColumn(world, chunkX, chunkZ, nbt);
	}

	public Cube loadCubeAndAddToColumn(Column column, long address) throws IOException {
		// does the database have the cube?
		byte[] data = this.cubes.get(address);
		if (data == null) {
			return null;
		}

		NBTTagCompound nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(data));

		// restore the cube
		int cubeX = getX(address);
		int cubeY = getY(address);
		int cubeZ = getZ(address);
		return IONbtReader.readCube(column, cubeX, cubeY, cubeZ, nbt);
	}

	public void saveColumn(Column column) {
		// NOTE: this function blocks the world thread
		// make it as fast as possible by offloading processing to the IO thread
		// except we have to write the NBT in this thread to avoid problems
		// with concurrent access to world data structures

		// add the column to the save queue
		this.columnsToSave.add(new SaveEntry(column.getAddress(), IONbtWriter.write(column)));
		column.markSaved();

		// signal the IO thread to process the save queue
		ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
	}

	public void saveCube(Cube cube) {
		// NOTE: this function blocks the world thread, so make it fast

		this.cubesToSave.add(new SaveEntry(cube.getAddress(), IONbtWriter.write(cube)));
		cube.markSaved();

		// signal the IO thread to process the save queue
		ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
	}

	@Override
	public boolean writeNextIO() {
		try {
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
					err(String.format("Unable to write column (%d, %d)", getX(entry.address), getZ(entry.address)), t);
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
					err(String.format("Unable to write cube %d, %d, %d", getX(entry.address), getY(entry.address), getZ(entry.address)), t);
				}
			}
			entries.clear();

			numColumnsRemaining = this.columnsToSave.size();
			numCubesRemaining = this.cubesToSave.size();

			// flush changes to disk
			this.db.commit();

			long diff = System.currentTimeMillis() - start;
			LOGGER.debug("Wrote {} columns ({} remaining) ({}k) and {} cubes ({} remaining) ({}k) in {} ms",
					numColumnsSaved, numColumnsRemaining, numColumnBytesSaved/1024,
					numCubesSaved, numCubesRemaining, numCubeBytesSaved/1024, diff
			);

			return hasMoreColumns || hasMoreCubes;
		} catch (Throwable t) {
			err("Exception occurred when saving cubes", t);
			return cubesToSave.size() != 0 || columnsToSave.size() != 0;
		}
	}


	/**
	 * Method that prints error message even when shutting down (ie. LOGGER is disabled)
	 */
	private static void err(String message) {
		if (!LOGGER.isErrorEnabled()) {
			System.err.println(message);
		} else {
			LOGGER.error(message);
		}
	}

	/**
	 * Method that prints error message even when shutting down (ie. LOGGER is disabled)
	 */
	private static void err(String message, Throwable t) {
		if (!LOGGER.isErrorEnabled()) {
			System.err.println(message);
			t.printStackTrace();
		} else {
			LOGGER.error(message, t);
		}
	}
}
