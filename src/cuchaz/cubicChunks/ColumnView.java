/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks;

import java.util.List;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;


public class ColumnView extends Column
{
	private Column m_column;
	private TreeMap<Integer,CubicChunk> m_cubicChunks;
	
	public ColumnView( Column column ) 
	{
		super( column.getWorld(), column.xPosition, column.zPosition );
		
		m_column = column;
		m_cubicChunks = new TreeMap<Integer,CubicChunk>();
	}
	
	public LightIndex getLightIndex( )
	{
		return m_column.getLightIndex();
	}
	
	public Iterable<CubicChunk> cubicChunks( )
	{
		return m_cubicChunks.values();
	}
	
	public void addCubicChunkToView( CubicChunk cubicChunk )
	{
		m_cubicChunks.put( cubicChunk.getY(), cubicChunk );
	}
	
	public CubicChunk getCubicChunk( int y )
	{
		return m_cubicChunks.get( y );
	}
	
	public CubicChunk getOrCreateCubicChunk( int y )
	{
		throw new UnsupportedOperationException();
	}
	
	public Iterable<CubicChunk> getCubicChunks( int minY, int maxY )
	{
		return m_cubicChunks.subMap( minY, true, maxY, true ).values();
	}
	
	public void addCubicChunk( CubicChunk cubicChunk )
	{
		throw new UnsupportedOperationException();
	}
	
	public List<RangeInt> getCubicChunkYRanges( )
	{
		return getRanges( m_cubicChunks.keySet() );
	}
	
	@Override
	public boolean needsSaving( boolean alwaysTrue )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override //      getBlock
	public Block func_150810_a( final int localX, final int blockY, final int localZ )
	{
		// pass off to the cubic chunk
		int chunkY = Coords.blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cubicChunk.getBlock( localX, localY, localZ );
		}
		
		// this cubic chunk isn't loaded, but there's something non-transparent there, return a block proxy
		int opacity = m_column.getLightIndex().getOpacity( localX, blockY, localZ );
		if( opacity > 0 )
		{
			return LightIndexBlockProxy.get( opacity );
		}
		
		return Blocks.air;
	}
	
	@Override //        setBlock
	public boolean func_150807_a( int localX, int blockY, int localZ, Block block, int meta )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getBlockMetadata( int localX, int blockY, int localZ )
	{
		// pass off to the cubic chunk
		int chunkY = Coords.blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cubicChunk.getBlockMetadata( localX, localY, localZ );
		}
		return 0;
	}
	
	@Override
	public boolean setBlockMetadata( int localX, int blockY, int localZ, int meta )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ExtendedBlockStorage[] getBlockStorageArray( )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getTopFilledSegment()
    {
		throw new UnsupportedOperationException();
    }
	
	@Override
	public boolean getAreLevelsEmpty( int minBlockY, int maxBlockY )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean canBlockSeeTheSky( int localX, int blockY, int localZ )
	{
		return m_column.canBlockSeeTheSky( localX, blockY, localZ );
	}
	
	@Override
	public int getHeightValue( int localX, int localZ )
	{
		return m_column.getHeightValue( localX, localZ );
	}
	
	@Override //  getOpacity
	public int func_150808_b( int localX, int blockY, int localZ )
	{
		return m_column.func_150808_b( localX, blockY, localZ );
	}
	
	@Override
	public void addEntity( Entity entity )
    {
		throw new UnsupportedOperationException();
    }
	
	@Override
	public void removeEntity( Entity entity )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeEntityAtIndex( Entity entity, int chunkY )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override //      getTileEntity
	public TileEntity func_150806_e( int localX, int blockY, int localZ )
	{
		// pass off to the cubic chunk
		int chunkY = Coords.blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cubicChunk.getTileEntity( localX, localY, localZ );
		}
		return null;
	}
	
	@Override
	public void addTileEntity( TileEntity tileEntity )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override // addTileEntity
	public void func_150812_a( int localX, int blockY, int localZ, TileEntity tileEntity )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeTileEntity( int localX, int blockY, int localZ )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void onChunkLoad( )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void onChunkUnload( )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void fillChunk( byte[] data, int segmentsToCopyBitFlags, int blockMSBToCopyBitFlags, boolean isFirstTime )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override //         tick
	public void func_150804_b( boolean tryToTickFaster )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void generateSkylightMap()
    {
		throw new UnsupportedOperationException();
    }
	
	@Override
	public int getPrecipitationHeight( int localX, int localZ )
	{
		return m_column.getPrecipitationHeight( localX, localZ );
	}
	
	@Override
	public int getBlockLightValue( int localX, int blockY, int localZ, int skylightSubtracted )
	{
		return m_column.getBlockLightValue( localX, blockY, localZ, skylightSubtracted );
	}
	
	@Override
	public int getSavedLightValue( EnumSkyBlock lightType, int localX, int blockY, int localZ )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setLightValue( EnumSkyBlock lightType, int localX, int blockY, int localZ, int light )
	{
		throw new UnsupportedOperationException();
	}
}
