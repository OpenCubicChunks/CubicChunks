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

import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class Column extends Chunk
{
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
						int chunkY = y >> 4;
						CubicChunk cubicChunk = getCubicChunk( chunkY );
						if( cubicChunk == null )
						{
							cubicChunk = new CubicChunk( world, chunkX, chunkY, chunkZ, hasSky );
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
}
