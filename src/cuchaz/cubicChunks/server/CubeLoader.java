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
package cuchaz.cubicChunks.server;

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
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.IThreadedFileIO;
import net.minecraft.world.storage.ThreadedFileIOBase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import cuchaz.cubicChunks.accessors.WorldServerAccessor;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.ConcurrentBatchedQueue;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;
import cuchaz.cubicChunks.world.EntityActionListener;

public class CubeLoader implements IThreadedFileIO
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
	private ConcurrentNavigableMap<Long,byte[]> m_cubes;
	private ConcurrentBatchedQueue<SaveEntry> m_columnsToSave;
	private ConcurrentBatchedQueue<SaveEntry> m_cubesToSave;
	
	public CubeLoader( ISaveHandler saveHandler )
	{
		// init database connection
		File file = new File( saveHandler.getWorldDirectory(), "chunks.db" );
		file.getParentFile().mkdirs();
		m_db = DBMaker.newFileDB( file )
			.closeOnJvmShutdown()
			//.compressionEnable()
			.make();
		
		// NOTE: could set different cache settings
		// the default is a hash map cache with 32768 entries
		// see: http://www.mapdb.org/features.html
		
		m_columns = m_db.getTreeMap( "columns" );
		m_cubes = m_db.getTreeMap( "chunks" );
        
        // init chunk save queue
        m_columnsToSave = new ConcurrentBatchedQueue<SaveEntry>();
        m_cubesToSave = new ConcurrentBatchedQueue<SaveEntry>();
	}
	
	public boolean columnExists( long address )
	{
		return m_columns.containsKey( address );
	}
	
	public Column loadColumn( World world, int chunkX, int chunkZ )
	throws IOException
	{
		// does the database have the column?
		long address = AddressTools.getAddress( chunkX, chunkZ );
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
		return readColumnFromNBT( world, chunkX, chunkZ, nbt );
	}
	
	public boolean cubeExists( long address )
	{
		return m_cubes.containsKey( address );
	}
	
	public Cube loadCubeAndAddToColumn( World world, Column column, long address )
	throws IOException
	{
		// does the database have the cubic chunk?
		byte[] data = m_cubes.get( address );
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
		return readCubeFromNbtAndAddToColumn( world, column, x, y, z, nbt );
	}
	
	public void saveColumn( Column column )
	{
		// NOTE: this function blocks the world thread
		// make it as fast as possible by offloading processing to the IO thread
		// except we have to write the NBT in this thread to avoid problems
		// with concurrent access to world data structures
		
		// add the column to the save queue
		m_columnsToSave.add( new SaveEntry( column.getAddress(), writeColumnToNbt( column ) ) );
		column.markSaved();
		
		// signal the IO thread to process the save queue
		ThreadedFileIOBase.threadedIOInstance.queueIO( this );
	}
	
	public void saveCube( Cube cube )
	{
		// NOTE: this function blocks the world thread, so make it fast
		
		m_cubesToSave.add( new SaveEntry( cube.getAddress(), writeCubeToNbt( cube ) ) );
		cube.markSaved();
		
		// signal the IO thread to process the save queue
		ThreadedFileIOBase.threadedIOInstance.queueIO( this );
	}
	
	@Override
	public boolean writeNextIO( )
	{
		// NOTE: return true to redo this call (used for batching)
		
		final int ColumnsBatchSize = 26;
		final int CubesBatchSize = ColumnsBatchSize*6;
		
		int numColumnsSaved = 0;
		int numColumnBytesSaved = 0;
		int numCubesSaved = 0;
		int numCubeBytesSaved = 0;
		long start = System.currentTimeMillis();
		
		List<SaveEntry> entries = new ArrayList<SaveEntry>( Math.max( ColumnsBatchSize, CubesBatchSize ) );
		
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
		boolean hasMoreCubes = m_cubesToSave.getBatch( entries, CubesBatchSize );
		for( SaveEntry entry : entries )
		{
			try
			{
				// save the cubic chunk
				byte[] data = writeNbtBytes( entry.nbt );
				m_cubes.put( entry.address, data );
				
				numCubesSaved++;
				numCubeBytesSaved += data.length;
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
			numCubesSaved, numCubeBytesSaved/1024,
			diff
		) );
		
		return hasMoreColumns || hasMoreCubes;
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
	
	private NBTTagCompound writeColumnToNbt( Column column )
	{
		NBTTagCompound nbt = new NBTTagCompound();
		
		// coords
		nbt.setInteger( "x", column.xPosition );
		nbt.setInteger( "z", column.zPosition );
		
		// column properties
		nbt.setByte( "v", (byte)1 );
		nbt.setBoolean( "TerrainPopulated", column.isTerrainPopulated );
		nbt.setBoolean( "LightPopulated", column.isLightPopulated );
		nbt.setLong( "InhabitedTime", column.inhabitedTime );
		
		// biome mappings
		nbt.setByteArray( "Biomes", column.getBiomeArray() );
		
		// light index
		nbt.setByteArray( "LightIndex", column.getLightIndex().getData() );
		
		// entities
		column.getEntityContainer().writeToNbt( nbt, "Entities" );
		
		return nbt;
	}
	
	private Column readColumnFromNBT( World world, int x, int z, NBTTagCompound nbt )
	{
		// check the version number
		byte version = nbt.getByte( "v" );
		if( version != 1 )
		{
			log.warn( String.format( "Column has wrong version: %d. Column will be regenerated.", version ) );
			return null;
		}
		
		// check the coords
		int xCheck = nbt.getInteger( "x" );
		int zCheck = nbt.getInteger( "z" );
		if( xCheck != x || zCheck != z )
		{
			log.warn( String.format( "Column is corrupted! Expected (%d,%d) but got (%d,%d). Column will be regenerated.", x, z, xCheck, zCheck ) );
			return null;
		}
		
		// create the column
		Column column = new Column( world, x, z );
		
		// read the rest of the column properties
		column.isTerrainPopulated = nbt.getBoolean( "TerrainPopulated" );
		column.isLightPopulated = nbt.getBoolean( "LightPopulated" );
		column.inhabitedTime = nbt.getLong( "InhabitedTime" );
		
		// biomes
		column.setBiomeArray( nbt.getByteArray( "Biomes" ) );
		
		// read light index
		column.getLightIndex().readData( nbt.getByteArray( "LightIndex" ) );
		
		// entities
		column.getEntityContainer().readFromNbt( nbt, "Entities", world );
		
		return column;
	}
	
	private NBTTagCompound writeCubeToNbt( final Cube cube )
	{
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte( "v", (byte)1 );
		
		// coords
		nbt.setInteger( "x", cube.getX() );
		nbt.setInteger( "y", cube.getY() );
		nbt.setInteger( "z", cube.getZ() );
		
		// blocks
		ExtendedBlockStorage storage = cube.getStorage();
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
		
		// entities
		cube.getEntityContainer().writeToNbt( nbt, "Entities", new EntityActionListener( )
		{
			@Override
			public void onEntity( Entity entity )
			{
				// make sure this entity is really in the chunk
				int chunkX = Coords.getChunkXForEntity( entity );
				int chunkY = Coords.getChunkYForEntity( entity );
				int chunkZ = Coords.getChunkZForEntity( entity );
				if( chunkX != cube.getX() || chunkY != cube.getY() || chunkZ != cube.getZ() )
				{
					log.warn( String.format( "Saved entity %s in cubic chunk (%d,%d,%d) to cubic chunk (%d,%d,%d)! Entity thinks its in (%d,%d,%d)",
						entity.getClass().getName(),
						chunkX, chunkY, chunkZ,
						cube.getX(), cube.getY(), cube.getZ(),
						entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ
					) );
				}
			}
		} );
		
		// tile entities
		NBTTagList nbtTileEntities = new NBTTagList();
		nbt.setTag( "TileEntities", nbtTileEntities );
		for( TileEntity tileEntity : cube.tileEntities() )
		{
			NBTTagCompound nbtTileEntity = new NBTTagCompound();
			tileEntity.writeToNBT( nbtTileEntity );
			nbtTileEntities.appendTag( nbtTileEntity );
		}
		
		// scheduled block ticks
		Iterable<NextTickListEntry> scheduledTicks = getScheduledTicks( cube );
		if( scheduledTicks != null )
		{
			long time = cube.getWorld().getTotalWorldTime();
			
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
	
	private Cube readCubeFromNbtAndAddToColumn( World world, Column column, int x, int y, int z, NBTTagCompound nbt )
	{
		// NBT types:
		// 0      1       2        3      4       5        6         7         8         9       10          11
		// "END", "BYTE", "SHORT", "INT", "LONG", "FLOAT", "DOUBLE", "BYTE[]", "STRING", "LIST", "COMPOUND", "INT[]"
		
		// TEMP
		int targetChunkX = Coords.blockToChunk( 221 );
		int targetChunkY = Coords.blockToChunk( 131 );
		int targetChunkZ = Coords.blockToChunk( -121 );
		if( x == targetChunkX && y == targetChunkY && z == targetChunkZ )
		{
			System.out.println( "Loading target chunk from NBT!" );
		}
		
		// check the version number
		byte version = nbt.getByte( "v" );
		if( version != 1 )
		{
			throw new IllegalArgumentException( "Cubic chunk has wrong version! " + version );
		}
		
		// check the coordinates
		int xCheck = nbt.getInteger( "x" );
		int yCheck = nbt.getInteger( "y" );
		int zCheck = nbt.getInteger( "z" );
		if( xCheck != x || yCheck != y || zCheck != z )
		{
			throw new Error( String.format( "Cubic chunk is corrupted! Expected (%d,%d,%d) but got (%d,%d,%d)", x, y, z, xCheck, yCheck, zCheck ) );
		}
		
		// check against column
		if( x != column.xPosition || z != column.zPosition )
		{
			throw new Error( String.format( "Cubic chunk is corrupted! Cubic chunk (%d,%d,%d) does not match column (%d,%d)", x, y, z, column.xPosition, column.zPosition ) );
		}
		
		// build the cubic chunk
		boolean hasSky = !world.provider.hasNoSky;
		final Cube cube = new Cube( world, column, x, y, z, hasSky );
		column.addCube( cube );
		
		ExtendedBlockStorage storage = cube.getStorage();
		
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
		
		// entities
		cube.getEntityContainer().readFromNbt( nbt, "Entities", world, new EntityActionListener( )
		{
			@Override
			public void onEntity( Entity entity )
			{
				// make sure this entity is really in the chunk
				int chunkX = Coords.getChunkXForEntity( entity );
				int chunkY = Coords.getChunkYForEntity( entity );
				int chunkZ = Coords.getChunkZForEntity( entity );
				if( chunkX != cube.getX() || chunkY != cube.getY() || chunkZ != cube.getZ() )
				{
					log.warn( String.format( "Loaded entity %s in cubic chunk (%d,%d,%d) to cubic chunk (%d,%d,%d)!",
						entity.getClass().getName(),
						chunkX, chunkY, chunkZ,
						cube.getX(), cube.getY(), cube.getZ()
					) );
				}
			}
		} );
		
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
					column.addTileEntity( tileEntity );
				}
			}
		}
		
		// scheduled block ticks
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
		
		return cube;
	}
	
	private List<NextTickListEntry> getScheduledTicks( Cube cube )
	{
		ArrayList<NextTickListEntry> out = new ArrayList<NextTickListEntry>();
		
		// make sure this is a server
		if( !( cube.getWorld() instanceof WorldServer ) )
		{
			throw new Error( "Column is not on the server!" );
		}
		WorldServer worldServer = (WorldServer)cube.getWorld();
		
		// copy the ticks for this cubic chunk
		copyScheduledTicks( out, WorldServerAccessor.getScheduledTicksTreeSet( worldServer ), cube );
		copyScheduledTicks( out, WorldServerAccessor.getScheduledTicksThisTick( worldServer ), cube );
		
		return out;
	}
	
	private void copyScheduledTicks( ArrayList<NextTickListEntry> out, Collection<NextTickListEntry> scheduledTicks, Cube cube )
	{
		// WorldServer.getPendingBlockUpdates() has extra -2 offsets on the min bounds, but maybe we don't actually need them?
		int minX = cube.getX() << 4;
		int maxX = ( cube.getX() + 1 ) << 4;
		int minY = cube.getY() << 4;
		int maxY = ( cube.getY() + 1 ) << 4;
		int minZ = cube.getZ() << 4;
		int maxZ = ( cube.getZ() + 1 ) << 4;
		
		for( NextTickListEntry scheduleTick : scheduledTicks )
		{
			if( scheduleTick.xCoord >= minX && scheduleTick.xCoord < maxX
				&& scheduleTick.yCoord >= minY && scheduleTick.yCoord < maxY
				&& scheduleTick.zCoord >= minZ && scheduleTick.zCoord < maxZ )
			{
				out.add( scheduleTick );
			}
		}
	}
}
