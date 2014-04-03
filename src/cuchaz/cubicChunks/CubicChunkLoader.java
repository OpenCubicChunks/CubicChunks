/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.IThreadedFileIO;
import net.minecraft.world.storage.ThreadedFileIOBase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class CubicChunkLoader implements IChunkLoader, IThreadedFileIO
{
	private static final Logger log = LogManager.getLogger();
	
	private static class SaveEntry
	{
		private long address;
		private NBTTagCompound nbt;
		
		public SaveEntry( long address, NBTTagCompound nbt )
		{
			this.address = address;
			this.nbt = nbt;
		}
	}
	
	private DB m_db;
	private ConcurrentNavigableMap<Long,byte[]> m_columns;
	private ConcurrentNavigableMap<Long,byte[]> m_cubicChunks;
	private ConcurrentBatchedQueue<SaveEntry> m_columnsToSave;
	private ConcurrentBatchedQueue<SaveEntry> m_cubicChunksToSave;
	
	public CubicChunkLoader( ISaveHandler saveHandler )
	{
		// identify the world
		String worldName = saveHandler.getWorldDirectoryName();
		
		// init database connection
		// NEXTTIME: this is fine for the server
		// on the client, this needs to be in the saves folder
		File file = new File( String.format( "%s/chunks.db", worldName ) );
		file.getParentFile().mkdirs();
        m_db = DBMaker.newFileDB( file )
            .closeOnJvmShutdown()
            //.compressionEnable()
            .make();
        /*
        Caused by: java.io.FileNotFoundException: Cubic Chunks/chunks.db (Is a directory)
    	at java.io.RandomAccessFile.open(Native Method) ~[?:1.6.0_30]
    	at java.io.RandomAccessFile.<init>(RandomAccessFile.java:236) ~[?:1.6.0_30]
    	at org.mapdb.Volume$FileChannelVol.<init>(Volume.java:637) ~[Volume$FileChannelVol.class:?]
    	at org.mapdb.Volume.volumeForFile(Volume.java:181) ~[Volume.class:?]
    	at org.mapdb.Volume$1.createIndexVolume(Volume.java:202) ~[Volume$1.class:?]
    	at org.mapdb.StoreDirect.<init>(StoreDirect.java:202) ~[StoreDirect.class:?]
    	at org.mapdb.StoreWAL.<init>(StoreWAL.java:57) ~[StoreWAL.class:?]
    	at org.mapdb.DBMaker.extendStoreWAL(DBMaker.java:907) ~[DBMaker.class:?]
    	at org.mapdb.DBMaker.makeEngine(DBMaker.java:703) ~[DBMaker.class:?]
    	at org.mapdb.DBMaker.make(DBMaker.java:654) ~[DBMaker.class:?]
    	at cuchaz.cubicChunks.CubicChunkLoader.<init>(CubicChunkLoader.java:79) ~[CubicChunkLoader.class:?]
    	at cuchaz.cubicChunks.CubicChunkProviderServer.<init>(CubicChunkProviderServer.java:22) ~[CubicChunkProviderServer.class:?]
    	at cuchaz.cubicChunks.CubicChunksMod.handleEvent(CubicChunksMod.java:38) ~[CubicChunksMod.class:?]
    	*/
    	
        m_columns = m_db.getTreeMap( "columns" );
        m_cubicChunks = m_db.getTreeMap( "chunks" );
        
        // init chunk save queue
        m_columnsToSave = new ConcurrentBatchedQueue<SaveEntry>();
        m_cubicChunksToSave = new ConcurrentBatchedQueue<SaveEntry>();
	}
	
	@Override
	public Column loadChunk( World world, int x, int z )
	throws IOException
	{
		// TEMP
		long start = System.currentTimeMillis();
		
		// does the database have the column?
		Column column = loadColumn( world, AddressTools.getAddress( world.provider.dimensionId, x, 0, z ) );
		if( column == null )
		{
			// returning null tells the world to generate a new column
			return null;
		}
		
		// restore the cubic chunks
		// TEMP: restore chunks 0-15
		ExtendedBlockStorage[] segments = new ExtendedBlockStorage[16];
		for( int y=0; y<15; y++ )
		{
			CubicChunk cubicChunk = loadCubicChunk( world, AddressTools.getAddress( world.provider.dimensionId, x, y, z ) );
			if( cubicChunk == null )
			{
				continue;
			}
			
			column.addCubicChunk( cubicChunk );
			
			// save the storage reference in the Minecraft chunk
			segments[y] = cubicChunk.getStorage();
		}
		column.setStorageArrays( segments );
		
		// TEMP
		long diff = System.currentTimeMillis() - start;
		if( diff > 20 )
		{
			log.warn( String.format( "Loaded column %d,%d in %d ms", x, z, diff ) );
		}
		
		return column;
	}
	
	private Column loadColumn( World world, long address )
	throws IOException
	{
		// does the database have the column?
		byte[] data = m_columns.get( address );
		if( data == null )
		{
			// returning null tells the world to generate a new column
			return null;
		}
		
		// read the NBT
		DataInputStream in = new DataInputStream( new ByteArrayInputStream( data ) );
		NBTTagCompound nbt = CompressedStreamTools.readCompressed( in );
		in.close();
		
		// restore the column
		int x = AddressTools.getX( address );
		int z = AddressTools.getZ( address );
		return readColumnFromNBT( world, x, z, nbt );
	}
	
	private CubicChunk loadCubicChunk( World world, long address )
	throws IOException
	{
		// does the database have the cubic chunk?
		byte[] data = m_cubicChunks.get( address );
		if( data == null )
		{
			return null;
		}
		
		// read the NBT
		DataInputStream in = new DataInputStream( new ByteArrayInputStream( data ) );
		NBTTagCompound nbt = CompressedStreamTools.readCompressed( in );
		in.close();
		
		// restore the cubic chunk
		int x = AddressTools.getX( address );
		int y = AddressTools.getY( address );
		int z = AddressTools.getZ( address );
		return readCubicChunkFromNbt( world, x, y, z, nbt );
	}
	
	@Override
	public void saveChunk( World world, Chunk mcChunk )
	throws MinecraftException, IOException
	{
		// NOTE: this function blocks the world thread
		// make it as fast as possible by offloading processing to the IO thread
		// except we have to write the NBT in this thread to avoid problems
		// with concurrent access to world data structures
		
		// add the column to the save queue
		Column column = castColumn( mcChunk );
		m_columnsToSave.add( new SaveEntry( column.getAddress(), writeColumnToNbt( column ) ) );
		
		// add the cubic chunks to the save queue
		List<SaveEntry> entries = new ArrayList<SaveEntry>();
		for( CubicChunk cubicChunk : column.cubicChunks() )
		{
			entries.add( new SaveEntry( cubicChunk.getAddress(), writeCubicChunkToNbt( cubicChunk ) ) );
		}
		m_cubicChunksToSave.addAll( entries );
		
		// signal the IO thread to process the save queue
		ThreadedFileIOBase.threadedIOInstance.queueIO( this );
	}
	
	@Override
	public boolean writeNextIO( )
	{
		// NOTE: return true to redo this call (used for batching)
		
		final int ColumnsBatchSize = 26;
		final int CubicChunksBatchSize = ColumnsBatchSize*6;
		
		int numColumnsSaved = 0;
		int numColumnBytesSaved = 0;
		int numCubicChunksSaved = 0;
		int numCubicChunkBytesSaved = 0;
		long start = System.currentTimeMillis();
		
		List<SaveEntry> entries = new ArrayList<SaveEntry>( Math.max( ColumnsBatchSize, CubicChunksBatchSize ) );
		
		// save a batch of columns
		boolean hasMoreColumns = m_columnsToSave.getBatch( entries, ColumnsBatchSize );
		for( SaveEntry entry : entries )
		{
			try
			{
				// save the column
				byte[] data = writeNbtBytes( entry.nbt );
				m_columns.put( entry.address, data );
				
				numColumnsSaved++;
				numColumnBytesSaved += data.length;
			}
			catch( IOException ex )
			{
				log.error( String.format( "Unable to write column %d,%d",
					AddressTools.getX( entry.address ),
					AddressTools.getZ( entry.address )
				), ex );
			}
		}
		entries.clear();
		
		// save a batch of cubic chunks
		boolean hasMoreCubicChunks = m_cubicChunksToSave.getBatch( entries, CubicChunksBatchSize );
		for( SaveEntry entry : entries )
		{
			try
			{
				// save the cubic chunk
				byte[] data = writeNbtBytes( entry.nbt );
				m_cubicChunks.put( entry.address, data );
				
				numCubicChunksSaved++;
				numCubicChunkBytesSaved += data.length;
			}
			catch( IOException ex )
			{
				log.error( String.format( "Unable to write cubic chunk %d,%d,%d",
					AddressTools.getX( entry.address ),
					AddressTools.getY( entry.address ),
					AddressTools.getZ( entry.address )
				), ex );
			}
		}
		entries.clear();
		
		// flush changes to disk
		m_db.commit();
		
		long diff = System.currentTimeMillis() - start;
		log.info( String.format( "Wrote %d columns (%dk) and %d cubic chunks (%dk) in %d ms",
			numColumnsSaved, numColumnBytesSaved/1024,
			numCubicChunksSaved, numCubicChunkBytesSaved/1024,
			diff
		) );
		
		return hasMoreColumns || hasMoreCubicChunks;
	}
	
	private byte[] writeNbtBytes( NBTTagCompound nbt )
	throws IOException
	{
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( buf );
		CompressedStreamTools.writeCompressed( nbt, out );
		out.close();
		return buf.toByteArray();
	}

	@Override
	public void saveExtraData( )
	{
		// not used
	}
	
	@Override
	public void chunkTick( )
	{
		// not used
	}
	
	@Override
	public void saveExtraChunkData( World world, Chunk chunk )
	{
		// not used
	}
	
	private Column castColumn( Chunk mcChunk )
	{
		if( mcChunk instanceof Column )
		{
			return (Column)mcChunk;
		}
		throw new IllegalArgumentException( "Chunk is a vanilla chunk! Exepected column of cubic chunks!" );
	}
	
	private NBTTagCompound writeColumnToNbt( Column column )
	{
		NBTTagCompound nbt = new NBTTagCompound();
		
		// chunk properties
		// COLUMN
		nbt.setByte( "v", (byte)1 );
		nbt.setBoolean( "TerrainPopulated", column.isTerrainPopulated );
		nbt.setBoolean( "LightPopulated", column.isLightPopulated );
		nbt.setLong( "InhabitedTime", column.inhabitedTime );
		
		// 16x16 array of highest y-value in chunk
		// COLUMN
		nbt.setIntArray( "HeightMap", column.heightMap );
		
		// UNDONE: might need to store more detailed data structure for lighting/rain calculations
		
		// biome mappings
		// COLUMN
		nbt.setByteArray( "Biomes", column.getBiomeArray() );
		
		// entities
		// CUBIC CHUNK
		column.hasEntities = false;
		NBTTagList nbtEntities = new NBTTagList();
		nbt.setTag( "Entities", nbtEntities );
		for( int i=0; i<column.entityLists.length; i++ )
		{
			@SuppressWarnings( "unchecked" )
			Iterable<Entity> entities = (Iterable<Entity>)column.entityLists[i];
			for( Entity entity : entities )
			{
				NBTTagCompound nbtEntity = new NBTTagCompound();
				if( entity.writeToNBTOptional( nbtEntity ) )
				{
					column.hasEntities = true;
					nbtEntities.appendTag( nbtEntity );
				}
			}
		}
		
		// tile entities
		// CUBIC CHUNK
		NBTTagList nbtTileEntities = new NBTTagList();
		nbt.setTag( "TileEntities", nbtTileEntities );
		@SuppressWarnings( "unchecked" )
		Iterable<TileEntity> tileEntities = (Iterable<TileEntity>)column.chunkTileEntityMap.values();
		for( TileEntity tileEntity : tileEntities )
		{
			NBTTagCompound nbtTileEntity = new NBTTagCompound();
			tileEntity.writeToNBT( nbtTileEntity );
			nbtTileEntities.appendTag( nbtTileEntity );
		}
		
		// schedule block ticks
		// CUBIC CHUNK
		@SuppressWarnings( "unchecked" )
		Iterable<NextTickListEntry> scheduledTicks = (Iterable<NextTickListEntry>)column.worldObj.getPendingBlockUpdates( column, false );
		if( scheduledTicks != null )
		{
			long time = column.worldObj.getTotalWorldTime();
			
			NBTTagList nbtTicks = new NBTTagList();
			nbt.setTag( "TileTicks", nbtTicks );
			for( NextTickListEntry scheduledTick : scheduledTicks )
			{
				NBTTagCompound nbtScheduledTick = new NBTTagCompound();
				nbtScheduledTick.setInteger( "i", Block.getIdFromBlock( scheduledTick.func_151351_a() ) );
				nbtScheduledTick.setInteger( "x", scheduledTick.xCoord );
				nbtScheduledTick.setInteger( "y", scheduledTick.yCoord );
				nbtScheduledTick.setInteger( "z", scheduledTick.zCoord );
				nbtScheduledTick.setInteger( "t", (int)( scheduledTick.scheduledTime - time ) );
				nbtScheduledTick.setInteger( "p", scheduledTick.priority );
				nbtTicks.appendTag( nbtScheduledTick );
			}
		}
		
		return nbt;
	}
	
	private Column readColumnFromNBT( World world, int x, int z, NBTTagCompound nbt )
	{
		// NBT types:
		// 0      1       2        3      4       5        6         7         8         9       10          11
		// "END", "BYTE", "SHORT", "INT", "LONG", "FLOAT", "DOUBLE", "BYTE[]", "STRING", "LIST", "COMPOUND", "INT[]"
		
		// check the version number
		byte version = nbt.getByte( "v" );
		if( version != 1 )
		{
			throw new IllegalArgumentException( "Column has wrong version! " + version );
		}
		
		// create the column
		Column column = new Column( world, x, z );
		
		// read the rest of the column properties
		column.isTerrainPopulated = nbt.getBoolean( "TerrainPopulated" );
		column.isLightPopulated = nbt.getBoolean( "LightPopulated" );
		column.inhabitedTime = nbt.getLong( "InhabitedTime" );
		
		// height map		
		column.heightMap = nbt.getIntArray( "HeightMap" );
		
		// biomes
		column.setBiomeArray( nbt.getByteArray( "Biomes" ) );
		
		// entities
		NBTTagList nbtEntities = nbt.getTagList( "Entities", 10 );
		if( nbtEntities != null )
		{
			for( int i=0; i<nbtEntities.tagCount(); i++ )
			{
				NBTTagCompound nbtEntity = nbtEntities.getCompoundTagAt( i );
				Entity entity = EntityList.createEntityFromNBT( nbtEntity, world );
				column.hasEntities = true;
				if( entity != null )
				{
					column.addEntity( entity );
					
					// deal with riding
					Entity topEntity = entity;
					for( NBTTagCompound nbtRiddenEntity = nbtEntity; nbtRiddenEntity.func_150297_b( "Riding", 10 ); nbtRiddenEntity = nbtRiddenEntity.getCompoundTag( "Riding" ) )
					{
						Entity riddenEntity = EntityList.createEntityFromNBT( nbtRiddenEntity.getCompoundTag( "Riding" ), world );
						if( riddenEntity != null )
						{
							column.addEntity( riddenEntity );
							topEntity.mountEntity( riddenEntity );
						}
						topEntity = riddenEntity;
					}
				}
			}
		}
		
		// tile entities
		NBTTagList nbtTileEntities = nbt.getTagList( "TileEntities", 10 );
		if( nbtTileEntities != null )
		{
			for( int i=0; i<nbtTileEntities.tagCount(); i++ )
			{
				NBTTagCompound nbtTileEntity = nbtTileEntities.getCompoundTagAt( i );
				TileEntity tileEntity = TileEntity.createAndLoadEntity( nbtTileEntity );
				if( tileEntity != null )
				{
					column.func_150813_a( tileEntity );
				}
			}
		}
		
		// scheduled ticks
		NBTTagList nbtScheduledTicks = nbt.getTagList( "TileTicks", 10 );
		if( nbtScheduledTicks != null )
		{
			for( int i=0; i<nbtScheduledTicks.tagCount(); i++ )
			{
				NBTTagCompound nbtScheduledTick = nbtScheduledTicks.getCompoundTagAt( i );
				world.func_147446_b(
					nbtScheduledTick.getInteger( "x" ),
					nbtScheduledTick.getInteger( "y" ),
					nbtScheduledTick.getInteger( "z" ),
					Block.getBlockById( nbtScheduledTick.getInteger( "i" ) ),
					nbtScheduledTick.getInteger( "t" ),
					nbtScheduledTick.getInteger( "p" )
				);
			}
		}
		
		return column;
	}
	
	private NBTTagCompound writeCubicChunkToNbt( CubicChunk cubicChunk )
	{
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte( "v", (byte)1 );
		
		// blocks
		ExtendedBlockStorage storage = cubicChunk.getStorage();
		nbt.setByteArray( "Blocks", storage.getBlockLSBArray() );
		if( storage.getBlockMSBArray() != null )
		{
			nbt.setByteArray( "Add", storage.getBlockMSBArray().data );
		}
		
		// metadata
		nbt.setByteArray( "Data", storage.getMetadataArray().data );
		
		// light
		nbt.setByteArray( "BlockLight", storage.getBlocklightArray().data );
		if( storage.getSkylightArray() != null )
		{
			nbt.setByteArray( "SkyLight", storage.getSkylightArray().data );
		}
		
		return nbt;
	}
	
	private CubicChunk readCubicChunkFromNbt( World world, int x, int y, int z, NBTTagCompound nbt )
	{
		// check the version number
		byte version = nbt.getByte( "v" );
		if( version != 1 )
		{
			throw new IllegalArgumentException( "Cubic chunk has wrong version! " + version );
		}
		
		// build the cubic chunk
		boolean hasSky = !world.provider.hasNoSky;
		CubicChunk cubicChunk = new CubicChunk( world, x, y, z, hasSky );
		
		ExtendedBlockStorage storage = cubicChunk.getStorage();
		
		// blocks
		storage.setBlockLSBArray( nbt.getByteArray( "Blocks" ) );
		if( nbt.func_150297_b( "Add", 7 ) )
		{
			storage.setBlockMSBArray( new NibbleArray( nbt.getByteArray( "Add" ), 4 ) );
		}
		
		// metadata
		storage.setBlockMetadataArray( new NibbleArray( nbt.getByteArray( "Data" ), 4 ) );
		
		// lights
		storage.setBlocklightArray( new NibbleArray( nbt.getByteArray( "BlockLight" ), 4 ) );
		if( hasSky )
		{
			storage.setSkylightArray( new NibbleArray( nbt.getByteArray( "SkyLight" ), 4 ) );
		}
		storage.removeInvalidBlocks();
		
		return cubicChunk;
	}
}
