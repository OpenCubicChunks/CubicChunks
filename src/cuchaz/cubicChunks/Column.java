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

import java.util.List;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Column extends Chunk
{
	private static final Logger log = LogManager.getLogger();
	
	private TreeMap<Integer,CubicChunk> m_cubicChunks;
	
	public Column( World world, int x, int z )
	{
		// NOTE: this constructor is called by the chunk loader
		super( world, x, z );
		
		init();
	}
	
	public Column( World world, Block[] blocks, byte[] meta, int chunkX, int chunkZ )
    {
		// NOTE: this constructor is called by the chunk generator
		this( world, chunkX, chunkZ );
		
		init();
		
		int maxY = blocks.length/256; // 256 blocks per y-layer
		boolean hasSky = !world.provider.hasNoSky;
		
		ExtendedBlockStorage[] segments = new ExtendedBlockStorage[maxY];
		
		// for each block...
		for( int x=0; x<16; x++ )
		{
			for( int z=0; z<16; z++ )
			{
				for( int y=0; y<maxY; y++ )
				{
					int blockIndex = x*maxY*16 | z*maxY | y;
					Block block = blocks[blockIndex];
					if( block != null && block != Blocks.air )
					{
						// get the cubic chunk
						int chunkY = blockToChunk( y );
						CubicChunk cubicChunk = getCubicChunk( chunkY );
						if( cubicChunk == null )
						{
							cubicChunk = new CubicChunk( world, this, chunkX, chunkY, chunkZ, hasSky );
							m_cubicChunks.put( chunkY, cubicChunk );
							
							// save the reference for the Minecraft chunk
							segments[chunkY] = cubicChunk.getStorage();
						}
						
						// save the block
						cubicChunk.getStorage().func_150818_a( x, y & 15, z, block );
						cubicChunk.getStorage().setExtBlockMetadata( x, y & 15, z, meta[blockIndex] );
					}
				}
			}
		}
		
		// save the segments for the Minecraft chunk
		setStorageArrays( segments );
    }
	
	private void init( )
	{
		m_cubicChunks = new TreeMap<Integer,CubicChunk>();
	}
	
	public long getAddress( )
	{
		return AddressTools.getAddress( worldObj.provider.dimensionId, xPosition, 0, zPosition );
	}
	
	public World getWorld( )
	{
		return worldObj;
	}

	public int getX( )
	{
		return xPosition;
	}

	public int getZ( )
	{
		return zPosition;
	}
	
	public Iterable<CubicChunk> cubicChunks( )
	{
		return m_cubicChunks.values();
	}
	
	public CubicChunk getCubicChunk( int y )
	{
		return m_cubicChunks.get( y );
	}
	
	public void addCubicChunk( CubicChunk cubicChunk )
	{
		m_cubicChunks.put( cubicChunk.getY(), cubicChunk );
	}
	
	// public boolean func_150807_a(int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_, int p_150807_5_)
	// this function is the only one that resets segment references
	// it's setBlock( ... )
	// of course, we'll have to override that one
	// but it will only cause problems when we place a block outside of a generated segment
	
	// getBlock( ... )
	// public Block func_150810_a(final int p_150810_1_, final int p_150810_2_, final int p_150810_3_)
	// we'll eventually need to override this one too
	
	@Override //      getTileEntity
	public TileEntity func_150806_e( int localX, int blockY, int localZ )
	{
		// pass off to the cubic chunk
		CubicChunk cubicChunk = m_cubicChunks.get( blockToChunk( blockY ) );
		if( cubicChunk != null )
		{
			int localY = blockToLocal( blockY );
			return cubicChunk.getTileEntity( localX, localY, localZ );
		}
		return null;
	}
	
	@Override
	public void addTileEntity( TileEntity tileEntity )
	{
		// NOTE: this is called only by the chunk loader
		
		int blockX = tileEntity.field_145851_c;
		int blockY = tileEntity.field_145848_d;
		int blockZ = tileEntity.field_145849_e;
		
		// pass off to the cubic chunk
		CubicChunk cubicChunk = m_cubicChunks.get( blockToChunk( blockY ) );
		if( cubicChunk != null )
		{
			int localX = blockToLocal( blockX );
			int localY = blockToLocal( blockY );
			int localZ = blockToLocal( blockZ );
			cubicChunk.addTileEntity( localX, localY, localZ, tileEntity );
		}
		
		if( isChunkLoaded )
		{
			// was the tile entity actually added?
			if( tileEntity.hasWorldObj() )
			{
				// tell the world
				worldObj.field_147482_g.add( tileEntity );
			}
		}
	}
	
	@Override // addTileEntity
	public void func_150812_a( int localX, int blockY, int localZ, TileEntity tileEntity )
	{
		// NOTE: this is called when the world sets this block
		
		log.info( String.format( "addTileEntity(%d,%d,%d) to cubic chunk (%d,%d,%d)",
			localX, blockY, localZ,
			xPosition, blockToChunk( blockY ), zPosition
		) );
		
		// pass off to the cubic chunk
		int chunkY = blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			int localY = blockToLocal( blockY );
			cubicChunk.addTileEntity( localX, localY, localZ, tileEntity );
		}
		else
		{
			// TEMP: this eventually won't happen when we're in full cubic chunks land
			// but for now, we need to handle this case
			log.warn( String.format( "No cubic chunk at (%d,%d,%d) to add tile entity (block %d,%d,%d)!",
				xPosition, chunkY, zPosition,
				tileEntity.field_145851_c, blockY, tileEntity.field_145849_e
			) );
		}
	}
	
	@Override
	public void removeTileEntity( int localX, int blockY, int localZ )
	{
		if( isChunkLoaded )
		{
			// pass off to the cubic chunk
			CubicChunk cubicChunk = m_cubicChunks.get( blockToChunk( blockY ) );
			if( cubicChunk != null )
			{
				int localY = blockToLocal( blockY );
				cubicChunk.removeTileEntity( localX, localY, localZ );
			}
		}
	}
	
	@Override
	public void onChunkLoad( )
	{
		isChunkLoaded = true;
		
		// pass off to cubic chunks
		for( CubicChunk cubicChunk : m_cubicChunks.values() )
		{
			cubicChunk.onLoad();
		}
		
		// tell the world about entities
		// UNDONE: move these to cubic chunks
		for( List list : entityLists )
		{
			for( Entity entity : (List<Entity>)list )
			{
				entity.onChunkLoad();
			}
			worldObj.addLoadedEntities( list );
		}
	}
	
	@Override
	public void onChunkUnload( )
	{
		this.isChunkLoaded = false;
		
		// pass off to cubic chunks
		for( CubicChunk cubicChunk : m_cubicChunks.values() )
		{
			cubicChunk.onUnload();
		}
		
		// tell the world to forget about entities
		// UNDONE: move these to cubic chunks
		for( List list : entityLists )
		{
			worldObj.unloadEntities( list );
		}
	}
	
	@Override
	public void fillChunk( byte[] data, int segmentsToCopyBitFlags, int blockMSBToCopyBitFlags, boolean deleteUnflaggedSegmentsAndCopyBiomeData )
	{
		// called on the client after chunk data is sent from the server
		// NOTE: the bit flags impose a 32-chunk limit for the y-dimension
		super.fillChunk( data, segmentsToCopyBitFlags, blockMSBToCopyBitFlags, deleteUnflaggedSegmentsAndCopyBiomeData );
		
		/* SUPER does this:
		for( TileEntity tileEntity : chunkTileEntityMap.values() )
		{
            tileEntity.updateContainingBlockInfo();
        }
        but the tile entity map will be empty, so we need to do that here
		*/
		
		// update tile entities in each chunk
		for( CubicChunk cubicChunk : m_cubicChunks.values() )
		{
			for( TileEntity tileEntity : cubicChunk.tileEntities() )
			{
				tileEntity.updateContainingBlockInfo();
			}
		}
	}
	
	private int blockToLocal( int val )
	{
		return val & 0xf;
	}
	
	private int blockToChunk( int val )
	{
		return val >> 4;
	}
}
