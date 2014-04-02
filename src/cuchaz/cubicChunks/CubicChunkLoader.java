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
import java.util.Iterator;
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
	
	private static class ChunkToSave
	{
		private long address;
		private NBTTagCompound nbt;
		
		public ChunkToSave( long address, NBTTagCompound nbt )
		{
			this.address = address;
			this.nbt = nbt;
		}
	}
	
	private DB m_db;
	private ConcurrentNavigableMap<Long,byte[]> m_chunks;
	private List<ChunkToSave> m_chunksToSave;
	
	public CubicChunkLoader( ISaveHandler saveHandler )
	{
		// identify the world
		String worldName = saveHandler.getWorldDirectoryName();
		
		// init database connection
		File file = new File( String.format( "%s.db", worldName ) );
        m_db = DBMaker.newFileDB( file )
            .closeOnJvmShutdown()
            .compressionEnable()
            .make();
        m_chunks = m_db.getTreeMap( "chunks" );
        
        // init chunk save queue
        m_chunksToSave = new ArrayList<ChunkToSave>();
	}
	
	@Override
	public Chunk loadChunk( World world, int x, int z )
	throws IOException
	{
		// does the database have the chunk?
		long address = AddressTools.toAddress( world.provider.dimensionId, x, 0, z );
		byte[] data = m_chunks.get( address );
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
		return readChunkFromNBT( world, nbt );
	}
	
	@Override
	public void saveChunk( World world, Chunk chunk )
	throws MinecraftException, IOException
	{
		// NOTE: this function blocks the world thread
		// make it as fast as possible by offloading processing to the IO thread
		// except we have to write the NBT in this thread to avoid problems with world ticks
		
		// write the chunk to NBT
		NBTTagCompound nbt = new NBTTagCompound();
		writeChunkToNBT( chunk, world, nbt );
		
		// add the chunk to the save queue
		long address = AddressTools.toAddress( chunk );
		ChunkToSave chunkToSave = new ChunkToSave( address, nbt );
		synchronized( m_chunksToSave )
		{
			m_chunksToSave.add( chunkToSave );
		}
		ThreadedFileIOBase.threadedIOInstance.queueIO( this );
	}
	
	@Override
	public boolean writeNextIO( )
	{
		// NOTE: return true to redo-this call
		
		int numChunksSaved = 0;
		int numBytesSaved = 0;
		long start = System.currentTimeMillis();
		
		synchronized( m_chunksToSave )
		{
			for( ChunkToSave chunkToSave: m_chunksToSave )
			{
				try
				{
					// render the NBT to a byte buffer
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					DataOutputStream out = new DataOutputStream( buf );
					CompressedStreamTools.writeCompressed( chunkToSave.nbt, out );
					out.close();
					byte[] data = buf.toByteArray();
					
					// save the chunk
					m_chunks.put( chunkToSave.address, data );
					
					numChunksSaved++;
					numBytesSaved += data.length;
				}
				catch( IOException ex )
				{
					log.error( String.format( "Unable to write chunk %d,%d",
						AddressTools.getX( chunkToSave.address ),
						AddressTools.getZ( chunkToSave.address )
					), ex );
				}
			}
			m_chunksToSave.clear();
		}
		
		// flush changes to disk
		m_db.commit();
		
		long diff = System.currentTimeMillis() - start;
		log.info( String.format( "Wrote %d chunks with %dk in %d ms", numChunksSaved, numBytesSaved/1024, diff ) );
		
		return false;
	}
	
	@Override
	public void saveExtraData( )
	{
		// UNDONE: do nothing for now
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
