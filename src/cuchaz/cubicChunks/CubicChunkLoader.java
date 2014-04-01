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
import java.util.Iterator;
import java.util.List;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class CubicChunkLoader implements IChunkLoader
{
	private static final Logger log = LogManager.getLogger();
	
	private SqlJetDb m_db;
	private int m_numChunksSaved;
	
	public CubicChunkLoader( ISaveHandler saveHandler )
	{
		// identify the world
		String worldName = saveHandler.getWorldDirectoryName();
		
		// init database connection
		File file = new File( String.format( "%s.db", worldName ) );
		try
		{
			if( file.exists() )
			{
				m_db = loadExistingDb( file );
			}
			else
			{
				m_db = createNewDb( file );
			}
		}
		catch( SqlJetException ex )
		{
			// there's probably no recovering from this
			throw new Error( ex );
		}
		
		// make sure the DB gets closed
		Runtime.getRuntime().addShutdownHook( new Thread( )
		{
			@Override
			public void run( )
			{
				if( m_db != null )
				{
					try
					{
						m_db.close();
					}
					catch( SqlJetException ex )
					{
						throw new Error( ex );
					}
				}
			}
		} );
		
		// init defaults
		m_numChunksSaved = 0;
	}
	
	private static SqlJetDb createNewDb( File file )
	throws SqlJetException
	{
		SqlJetDb db = SqlJetDb.open( file, true );
		
		// set first-time options before any transactions
		db.getOptions().setAutovacuum( true );
		
		initConnection( db );
		
		// create tables
		db.beginTransaction( SqlJetTransactionMode.WRITE );
        try
        {
        	db.createTable( "CREATE TABLE chunks ( dimension INTEGER, x INTEGER, y INTEGER, z INTEGER, data BLOB NOT NULL, PRIMARY KEY( dimension, x, y, z ) ) WITHOUT ROWID" );
        }
        finally
        {
        	db.commit();
        }
		
		return db;
	}

	private static SqlJetDb loadExistingDb( File file )
	throws SqlJetException
	{
		SqlJetDb db = SqlJetDb.open( file, true );
		initConnection( db );
		return db;
	}
	
	private static void initConnection( SqlJetDb db )
	throws SqlJetException
	{
		// apparently we need to set the user version
		// I don't actually know what this does or why it's useful, but what's the harm in keeping it? =P
		db.beginTransaction( SqlJetTransactionMode.WRITE );
		try
		{
			db.getOptions().setUserVersion( 1 );
		}
		finally
		{
			db.commit();
		}
	}

	@Override
	public void chunkTick( )
	{
		// not used
	}
	
	@Override
	public Chunk loadChunk( World world, int x, int z ) throws IOException
	{
		// NOTE: returning null tells the world to generate a new chunk
		try
		{
			// does the database have a chunk?
			byte[] data = loadChunk( world.provider.dimensionId, x, z );
			if( data == null )
			{
				// TEMP
				log.info( "\tnot available" );
				
				return null;
			}
			
			// render back to NBT
			DataInputStream in = new DataInputStream( new ByteArrayInputStream( data ) );
			NBTTagCompound nbt = CompressedStreamTools.readCompressed( in );
			in.close();
			
			// TEMP
			log.info( String.format( "Loaded chunk (%3d,%3d)", x, z ) );
			
			// restore the chunk
			return readChunkFromNBT( world, nbt );
		}
		catch( Exception ex )
		{
			log.error( String.format( "Unable to load chunk %d,%d! A new one will be generated", x, z ), ex );
			return null;
		}
	}
	
	private byte[] loadChunk( int dimension, int x, int z )
	throws SqlJetException
	{
		// TEMP: method for vanilla chunks
		ISqlJetCursor cursor = null;
		m_db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
		try
		{
			cursor = m_db.getTable( "chunks" ).lookup( null, dimension, x, 0, z );
			if( cursor.eof() )
			{
				return null;
			}
			return cursor.getBlobAsArray( "data" );
		}
		finally
		{
			if( cursor != null )
			{
				cursor.close();
			}
			m_db.commit();
		}
	}
	
	@Override
	public void saveChunk( World world, Chunk chunk )
	throws MinecraftException, IOException
	{
		// NEXTTIME: could speed up using ThreadedFileIOBase and a pending queue, like AnvilChunkLoader.saveChunk()
		try
		{
			// write the chunk to NBT
			NBTTagCompound nbt = new NBTTagCompound();
			writeChunkToNBT( chunk, world, nbt );
			
			// render the NBT to a byte buffer
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream( buf );
			CompressedStreamTools.writeCompressed( nbt, out );
			out.close();
			byte[] data = buf.toByteArray();
			
			saveChunk( world.provider.dimensionId, chunk.xPosition, chunk.zPosition, data );
			m_numChunksSaved++;
		}
		catch( SqlJetException ex )
		{
			log.error( String.format( "Unable to save chunk %d,%d!", chunk.xPosition, chunk.zPosition ), ex );
		}
	}
	
	public void startChunkSave( )
	throws SqlJetException
	{
		m_numChunksSaved = 0;
		m_db.beginTransaction( SqlJetTransactionMode.WRITE );
	}
	
	public int stopChunkSave( )
	throws SqlJetException
	{
		m_db.commit();
		return m_numChunksSaved;
	}
	
	private void saveChunk( int dimension, int x, int z, byte[] data )
	throws SqlJetException
	{
		// TEMP: method for vanilla chunks
		
		// is this chunk already saved?
		ISqlJetTable table = m_db.getTable( "chunks" );
		Object[] key = { dimension, x, 0, z };
		ISqlJetCursor cursor = table.scope( null, key, key );
		if( cursor.eof() )
		{
			// insert
			m_db.getTable( "chunks" ).insert( dimension, x, 0, z, data );
		}
		else
		{
			// update
			cursor.update( dimension, x, 0, z, data );
		}
	}
	
	@Override
	public void saveExtraChunkData( World world, Chunk chunk )
	{
		// UNDONE: do nothing for now
	}
	
	@Override
	public void saveExtraData( )
	{
		// UNDONE: do nothing for now
	}
	
	private void writeChunkToNBT( Chunk chunk, World world, NBTTagCompound nbt )
	{
		// chunk properties
		nbt.setByte( "V", (byte)1 );
		nbt.setInteger( "xPos", chunk.xPosition );
		nbt.setInteger( "zPos", chunk.zPosition );
		nbt.setLong( "LastUpdate", world.getTotalWorldTime() );
		nbt.setIntArray( "HeightMap", chunk.heightMap );
		nbt.setBoolean( "TerrainPopulated", chunk.isTerrainPopulated );
		nbt.setBoolean( "LightPopulated", chunk.isLightPopulated );
		nbt.setLong( "InhabitedTime", chunk.inhabitedTime );
		
		// block data
		int numSegments = chunk.getBlockStorageArray().length;
		NBTTagList var5 = new NBTTagList();
		for( int i=0; i<numSegments; i++ )
		{
			ExtendedBlockStorage chunkSegment = chunk.getBlockStorageArray()[i];
			if( chunkSegment != null )
			{
				NBTTagCompound var11 = new NBTTagCompound();
				var11.setByte( "Y", (byte)( chunkSegment.getYLocation() >> 4 & 255 ) );
				var11.setByteArray( "Blocks", chunkSegment.getBlockLSBArray() );
				
				if( chunkSegment.getBlockMSBArray() != null )
				{
					var11.setByteArray( "Add", chunkSegment.getBlockMSBArray().data );
				}
				
				var11.setByteArray( "Data", chunkSegment.getMetadataArray().data );
				var11.setByteArray( "BlockLight", chunkSegment.getBlocklightArray().data );
				
				if( !world.provider.hasNoSky )
				{
					var11.setByteArray( "SkyLight", chunkSegment.getSkylightArray().data );
				}
				else
				{
					var11.setByteArray( "SkyLight", new byte[chunkSegment.getBlocklightArray().data.length] );
				}
				
				var5.appendTag( var11 );
			}
		}
		
		nbt.setTag( "Sections", var5 );
		nbt.setByteArray( "Biomes", chunk.getBiomeArray() );
		chunk.hasEntities = false;
		NBTTagList var16 = new NBTTagList();
		Iterator var18;
		
		for( int i=0; i<chunk.entityLists.length; i++ )
		{
			var18 = chunk.entityLists[i].iterator();
			
			while( var18.hasNext() )
			{
				Entity var20 = (Entity)var18.next();
				NBTTagCompound var11 = new NBTTagCompound();
				
				if( var20.writeToNBTOptional( var11 ) )
				{
					chunk.hasEntities = true;
					var16.appendTag( var11 );
				}
			}
		}
		
		nbt.setTag( "Entities", var16 );
		NBTTagList var17 = new NBTTagList();
		var18 = chunk.chunkTileEntityMap.values().iterator();
		
		while( var18.hasNext() )
		{
			TileEntity var21 = (TileEntity)var18.next();
			NBTTagCompound var11 = new NBTTagCompound();
			var21.writeToNBT( var11 );
			var17.appendTag( var11 );
		}
		
		nbt.setTag( "TileEntities", var17 );
		List var19 = world.getPendingBlockUpdates( chunk, false );
		
		if( var19 != null )
		{
			long var22 = world.getTotalWorldTime();
			NBTTagList var12 = new NBTTagList();
			Iterator var13 = var19.iterator();
			
			while( var13.hasNext() )
			{
				NextTickListEntry var14 = (NextTickListEntry)var13.next();
				NBTTagCompound var15 = new NBTTagCompound();
				var15.setInteger( "i", Block.getIdFromBlock( var14.func_151351_a() ) );
				var15.setInteger( "x", var14.xCoord );
				var15.setInteger( "y", var14.yCoord );
				var15.setInteger( "z", var14.zCoord );
				var15.setInteger( "t", (int)( var14.scheduledTime - var22 ) );
				var15.setInteger( "p", var14.priority );
				var12.appendTag( var15 );
			}
			
			nbt.setTag( "TileTicks", var12 );
		}
	}
	
	private Chunk readChunkFromNBT( World par1World, NBTTagCompound par2NBTTagCompound )
	{
		int var3 = par2NBTTagCompound.getInteger( "xPos" );
		int var4 = par2NBTTagCompound.getInteger( "zPos" );
		Chunk var5 = new Chunk( par1World, var3, var4 );
		var5.heightMap = par2NBTTagCompound.getIntArray( "HeightMap" );
		var5.isTerrainPopulated = par2NBTTagCompound.getBoolean( "TerrainPopulated" );
		var5.isLightPopulated = par2NBTTagCompound.getBoolean( "LightPopulated" );
		var5.inhabitedTime = par2NBTTagCompound.getLong( "InhabitedTime" );
		NBTTagList var6 = par2NBTTagCompound.getTagList( "Sections", 10 );
		byte var7 = 16;
		ExtendedBlockStorage[] var8 = new ExtendedBlockStorage[var7];
		boolean var9 = !par1World.provider.hasNoSky;
		
		for( int var10 = 0; var10 < var6.tagCount(); ++var10 )
		{
			NBTTagCompound var11 = var6.getCompoundTagAt( var10 );
			byte var12 = var11.getByte( "Y" );
			ExtendedBlockStorage var13 = new ExtendedBlockStorage( var12 << 4, var9 );
			var13.setBlockLSBArray( var11.getByteArray( "Blocks" ) );
			
			if( var11.func_150297_b( "Add", 7 ) )
			{
				var13.setBlockMSBArray( new NibbleArray( var11.getByteArray( "Add" ), 4 ) );
			}
			
			var13.setBlockMetadataArray( new NibbleArray( var11.getByteArray( "Data" ), 4 ) );
			var13.setBlocklightArray( new NibbleArray( var11.getByteArray( "BlockLight" ), 4 ) );
			
			if( var9 )
			{
				var13.setSkylightArray( new NibbleArray( var11.getByteArray( "SkyLight" ), 4 ) );
			}
			
			var13.removeInvalidBlocks();
			var8[var12] = var13;
		}
		
		var5.setStorageArrays( var8 );
		
		if( par2NBTTagCompound.func_150297_b( "Biomes", 7 ) )
		{
			var5.setBiomeArray( par2NBTTagCompound.getByteArray( "Biomes" ) );
		}
		
		NBTTagList var17 = par2NBTTagCompound.getTagList( "Entities", 10 );
		
		if( var17 != null )
		{
			for( int var18 = 0; var18 < var17.tagCount(); ++var18 )
			{
				NBTTagCompound var20 = var17.getCompoundTagAt( var18 );
				Entity var22 = EntityList.createEntityFromNBT( var20, par1World );
				var5.hasEntities = true;
				
				if( var22 != null )
				{
					var5.addEntity( var22 );
					Entity var14 = var22;
					
					for( NBTTagCompound var15 = var20; var15.func_150297_b( "Riding", 10 ); var15 = var15.getCompoundTag( "Riding" ) )
					{
						Entity var16 = EntityList.createEntityFromNBT( var15.getCompoundTag( "Riding" ), par1World );
						
						if( var16 != null )
						{
							var5.addEntity( var16 );
							var14.mountEntity( var16 );
						}
						
						var14 = var16;
					}
				}
			}
		}
		
		NBTTagList var19 = par2NBTTagCompound.getTagList( "TileEntities", 10 );
		
		if( var19 != null )
		{
			for( int var21 = 0; var21 < var19.tagCount(); ++var21 )
			{
				NBTTagCompound var24 = var19.getCompoundTagAt( var21 );
				TileEntity var26 = TileEntity.createAndLoadEntity( var24 );
				
				if( var26 != null )
				{
					var5.func_150813_a( var26 );
				}
			}
		}
		
		if( par2NBTTagCompound.func_150297_b( "TileTicks", 9 ) )
		{
			NBTTagList var23 = par2NBTTagCompound.getTagList( "TileTicks", 10 );
			
			if( var23 != null )
			{
				for( int var25 = 0; var25 < var23.tagCount(); ++var25 )
				{
					NBTTagCompound var27 = var23.getCompoundTagAt( var25 );
					par1World.func_147446_b( var27.getInteger( "x" ), var27.getInteger( "y" ), var27.getInteger( "z" ), Block.getBlockById( var27.getInteger( "i" ) ), var27.getInteger( "t" ),
							var27.getInteger( "p" ) );
				}
			}
		}
		
		return var5;
	}
}
