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

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class CubicChunk
{
	private World m_world;
	private Column m_column;
	private int m_x;
	private int m_y;
	private int m_z;
	private ExtendedBlockStorage m_storage;
	private CubicChunkBlockMap<TileEntity> m_tileEntities;
	
	public CubicChunk( World world, Column column, int x, int y, int z, boolean hasSky )
	{
		m_world = world;
		m_column = column;
		m_x = x;
		m_y = y;
		m_z = z;
		m_storage = new ExtendedBlockStorage( y << 4, hasSky );
		m_tileEntities = new CubicChunkBlockMap<TileEntity>();
	}
	
	public long getAddress( )
	{
		return AddressTools.getAddress( m_world.provider.dimensionId, m_x, m_y, m_z );
	}
	
	public World getWorld( )
	{
		return m_world;
	}
	
	public Column getColumn( )
	{
		return m_column;
	}

	public int getX( )
	{
		return m_x;
	}

	public int getY( )
	{
		return m_y;
	}

	public int getZ( )
	{
		return m_z;
	}
	
	public ExtendedBlockStorage getStorage( )
	{
		return m_storage;
	}
	
	public Iterable<TileEntity> tileEntities( )
	{
		return m_tileEntities.values();
	}
	
	public Block getBlock( int x, int y, int z )
	{
		return m_storage.func_150819_a( x, y, z );
	}
	
	public int getBlockMetadata( int x, int y, int z )
	{
		return m_storage.getMetadataArray().get( x, y, z );
	}
	
	public TileEntity getTileEntity( int x, int y, int z )
	{
		TileEntity tileEntity = m_tileEntities.get( x, y, z );
		
		if( tileEntity == null )
		{
			// is this block not supposed to have a tile entity?
			Block block = getBlock( x, y, z );
			if( !block.hasTileEntity() )
			{
				return null;
			}
			
			// make a new tile entity for the block
			tileEntity = ((ITileEntityProvider)block).createNewTileEntity( m_world, getBlockMetadata( x, y, z ) );
			int blockX = localToBlock( m_x, x );
			int blockY = localToBlock( m_y, y );
			int blockZ = localToBlock( m_z, z );
			m_world.setTileEntity( blockX, blockY, blockZ, tileEntity );
		}
		
		if( tileEntity != null && tileEntity.isInvalid() )
		{
			// remove the tile entity
			m_tileEntities.remove( x, y, z );
			return null;
		}
		else
		{
			return tileEntity;
		}
	}
	
	public void addTileEntity( int x, int y, int z, TileEntity tileEntity )
	{
		// update the tile entity
		int blockX = localToBlock( m_x, x );
		int blockY = localToBlock( m_y, y );
		int blockZ = localToBlock( m_z, z );
		tileEntity.setWorldObj( m_world );
		tileEntity.field_145851_c = blockX;
		tileEntity.field_145848_d = blockY;
		tileEntity.field_145849_e = blockZ;
		
		// is this block supposed to have a tile entity?
		if( getBlock( x, y, z ) instanceof ITileEntityProvider )
		{
			// cleanup the old tile entity
			TileEntity oldTileEntity = m_tileEntities.get( x, y, z );
			if( oldTileEntity != null )
			{
				oldTileEntity.invalidate();
			}
			
			// install the new tile entity
			tileEntity.validate();
			m_tileEntities.put( x, y, z, tileEntity );
		}
	}
	
	public void removeTileEntity( int x, int y, int z )
	{
		TileEntity tileEntity = m_tileEntities.remove( x, y, z );
		if( tileEntity != null )
		{
			tileEntity.invalidate();
		}
	}
	
	public void onLoad( )
	{
		// tell the world about tile entities
		m_world.func_147448_a( m_tileEntities.values() );
	}

	public void onUnload( )
	{
		// tell the world to forget about tile entities
		for( TileEntity tileEntity : m_tileEntities.values() )
		{
			m_world.func_147457_a( tileEntity );
		}
	}
	
	private int localToBlock( int chunkX, int localX )
	{
		return ( chunkX << 4 ) + localX;
	}
}
