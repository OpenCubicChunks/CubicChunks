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
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentNavigableMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
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
	private ConcurrentNavigableMap<Long,byte[]> m_chunks;
	private ConcurrentLinkedQueue<SaveEntry> m_columnsToSave;
	private ConcurrentLinkedQueue<SaveEntry> m_chunksToSave;
	
	public CubicChunkLoader( ISaveHandler saveHandler )
	{
		// identify the world
		String worldName = saveHandler.getWorldDirectoryName();
		
		// init database connection
		File file = new File( String.format( "%s/chunks.db", worldName ) );
        m_db = DBMaker.newFileDB( file )
            .closeOnJvmShutdown()
            //.compressionEnable()
            .make();
        m_columns = m_db.getTreeMap( "columns" );
        m_chunks = m_db.getTreeMap( "chunks" );
        
        // init chunk save queue
        m_columnsToSave = new ConcurrentLinkedQueue<SaveEntry>();
        m_chunksToSave = new ConcurrentLinkedQueue<SaveEntry>();
	}
	
	@Override
	public Chunk loadChunk( World world, int x, int z )
	throws IOException
	{
		// does the database have the chunk?
		long address = AddressTools.getAddress( world.provider.dimensionId, x, 0, z );
		byte[] data = m_columns.get( address );
		if( data == null )
		{
			// returning null tells the world to generate a new chunk
			return null;
		}
		
		// render back to NBT
		DataInputStream in = new DataInputStream( new ByteArrayInputStream( data ) );
		NBTTagCompound nbt = CompressedStreamTools.readCompressed( in );
		in.close();
		
		// restore the chunk
		return readColumnFromNBT( world, nbt );
	}
	
	@Override
	public void saveChunk( World world, Chunk mcChunk )
	throws MinecraftException, IOException
	{
		// NOTE: this function blocks the world thread
		// make it as fast as possible by offloading processing to the IO thread
		// except we have to write the NBT in this thread to avoid problems with world ticks
		
		// add the column to the save queue
		Column column = castColumn( mcChunk );
		m_columnsToSave.offer( new SaveEntry( column.getAddress(), writeColumnToNbt( column ) ) );
		
		// NOTE: in a MC chunk, we can probably set the ExtendedBlockStorage instances to point to the instances in the cubic chunks
		
		ThreadedFileIOBase.threadedIOInstance.queueIO( this );
	}
	
	@Override
	public boolean writeNextIO( )
	{
		// NOTE: return true to redo-this call
		
		int numChunksSaved = 0;
		int numBytesSaved = 0;
		long start = System.currentTimeMillis();
		
		// save columns
		SaveEntry entry = null;
		while( ( entry = m_columnsToSave.poll() ) != null )
		{
			try
			{
				// save the chunk
				byte[] data = writeNbtBytes( entry.nbt );
				m_columns.put( entry.address, data );
				
				numChunksSaved++;
				numBytesSaved += data.length;
			}
			catch( IOException ex )
			{
				log.error( String.format( "Unable to write chunk %d,%d",
					AddressTools.getX( entry.address ),
					AddressTools.getZ( entry.address )
				), ex );
			}
		}
		
		// flush changes to disk
		m_db.commit();
		
		long diff = System.currentTimeMillis() - start;
		log.info( String.format( "Wrote %d chunks with %dk in %d ms", numChunksSaved, numBytesSaved/1024, diff ) );
		
		return false;
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
		nbt.setByte( "V", (byte)1 );
		nbt.setInteger( "xPos", column.xPosition );
		nbt.setInteger( "zPos", column.zPosition );
		nbt.setBoolean( "TerrainPopulated", column.isTerrainPopulated );
		nbt.setBoolean( "LightPopulated", column.isLightPopulated );
		nbt.setLong( "InhabitedTime", column.inhabitedTime );
		
		// 16x16 array of highest y-value in chunk
		// COLUMN
		nbt.setIntArray( "HeightMap", column.heightMap );
		
		// UNDONE: might need to store more detailed data structure for lighting/rain calculations
		
		// block sections
		// CUBIC CHUNK
		NBTTagList nbtSections = new NBTTagList();
		nbt.setTag( "Sections", nbtSections );
		for( int i=0; i<column.getBlockStorageArray().length; i++ )
		{
			ExtendedBlockStorage section = column.getBlockStorageArray()[i];
			if( section != null )
			{
				NBTTagCompound nbtSection = new NBTTagCompound();
				nbtSections.appendTag( nbtSection );
				
				nbtSection.setByte( "Y", (byte)( section.getYLocation() >> 4 & 255 ) );
				nbtSection.setByteArray( "Blocks", section.getBlockLSBArray() );
				
				if( section.getBlockMSBArray() != null )
				{
					nbtSection.setByteArray( "Add", section.getBlockMSBArray().data );
				}
				
				nbtSection.setByteArray( "Data", section.getMetadataArray().data );
				nbtSection.setByteArray( "BlockLight", section.getBlocklightArray().data );
				
				if( !column.worldObj.provider.hasNoSky )
				{
					nbtSection.setByteArray( "SkyLight", section.getSkylightArray().data );
				}
				else
				{
					nbtSection.setByteArray( "SkyLight", new byte[section.getBlocklightArray().data.length] );
				}
			}
		}
		
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
			List<Entity> entities = (List<Entity>)column.entityLists[i];
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
		List<TileEntity> tileEntities = (List<TileEntity>)column.chunkTileEntityMap.values();
		for( TileEntity tileEntity : tileEntities )
		{
			NBTTagCompound nbtTileEntity = new NBTTagCompound();
			tileEntity.writeToNBT( nbtTileEntity );
			nbtTileEntities.appendTag( nbtTileEntity );
		}
		
		// schedule block ticks
		// CUBIC CHUNK
		@SuppressWarnings( "unchecked" )
		List<NextTickListEntry> scheduledTicks = (List<NextTickListEntry>)column.worldObj.getPendingBlockUpdates( column, false );
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
	
	private Column readColumnFromNBT( World world, NBTTagCompound nbt )
	{
		// NBT types:
		// 0      1       2        3      4       5        6         7         8         9       10          11
		// "END", "BYTE", "SHORT", "INT", "LONG", "FLOAT", "DOUBLE", "BYTE[]", "STRING", "LIST", "COMPOUND", "INT[]"
		
		// create the column
		int x = nbt.getInteger( "xPos" );
		int z = nbt.getInteger( "zPos" );
		Column column = new Column( world, x, z );
		
		// check the version number
		byte version = nbt.getByte( "V" );
		if( version != 1 )
		{
			throw new IllegalArgumentException( "Column has wrong version! " + version );
		}
		
		// read the rest of the column properties
		column.isTerrainPopulated = nbt.getBoolean( "TerrainPopulated" );
		column.isLightPopulated = nbt.getBoolean( "LightPopulated" );
		column.inhabitedTime = nbt.getLong( "InhabitedTime" );
		
		// height map		
		column.heightMap = nbt.getIntArray( "HeightMap" );

		// block sections
		NBTTagList nbtSections = nbt.getTagList( "Sections", 10 );
		ExtendedBlockStorage[] segments = new ExtendedBlockStorage[16];
		column.setStorageArrays( segments );
		boolean hasSky = !world.provider.hasNoSky;
		for( int i=0; i<nbtSections.tagCount(); i++ )
		{
			NBTTagCompound nbtSection = nbtSections.getCompoundTagAt( i );
			
			byte y = nbtSection.getByte( "Y" );
			ExtendedBlockStorage section = new ExtendedBlockStorage( y << 4, hasSky );
			segments[y] = section;
			
			section.setBlockLSBArray( nbtSection.getByteArray( "Blocks" ) );
			if( nbtSection.func_150297_b( "Add", 7 ) )
			{
				section.setBlockMSBArray( new NibbleArray( nbtSection.getByteArray( "Add" ), 4 ) );
			}
			section.setBlockMetadataArray( new NibbleArray( nbtSection.getByteArray( "Data" ), 4 ) );
			section.setBlocklightArray( new NibbleArray( nbtSection.getByteArray( "BlockLight" ), 4 ) );
			if( hasSky )
			{
				section.setSkylightArray( new NibbleArray( nbtSection.getByteArray( "SkyLight" ), 4 ) );
			}
			section.removeInvalidBlocks();
		}
		
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
}
