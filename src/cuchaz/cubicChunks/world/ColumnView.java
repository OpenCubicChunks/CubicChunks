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
package cuchaz.cubicChunks.world;

import java.util.List;
import java.util.TreeMap;

import cuchaz.cubicChunks.util.CubeCoordinate;
import cuchaz.cubicChunks.util.RangeInt;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;


public class ColumnView extends Column
{
	private Column m_column;
	private TreeMap<Integer,Cube> m_cubes;
	
	public ColumnView( Column column ) 
	{
		super( column.getWorld(), column.xPosition, column.zPosition );
		
		m_column = column;
		m_cubes = new TreeMap<Integer,Cube>();
	}
	
	public LightIndex getLightIndex( )
	{
		return m_column.getLightIndex();
	}
	
	public Iterable<Cube> cubes( )
	{
		return m_cubes.values();
	}
	
	public void addCubeToView( Cube cube )
	{
		m_cubes.put( cube.getY(), cube );
	}
	
	public Cube getCube( int y )
	{
		return m_cubes.get( y );
	}
	
	public Cube getOrCreateCube( int y )
	{
		throw new UnsupportedOperationException();
	}
	
	public Iterable<Cube> getCubes( int minY, int maxY )
	{
		return m_cubes.subMap( minY, true, maxY, true ).values();
	}
	
	public void addCube( Cube cube )
	{
		throw new UnsupportedOperationException();
	}
	
	public List<RangeInt> getCubeYRanges( )
	{
		return getRanges( m_cubes.keySet() );
	}
	
	@Override
	public boolean needsSaving( boolean alwaysTrue )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override //      getBlock
	public Block func_150810_a( final int localX, final int blockY, final int localZ )
	{
		// pass off to the cube
		int cubeY = CubeCoordinate.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = CubeCoordinate.blockToLocal( blockY );
			return cube.getBlock( localX, localY, localZ );
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
		// pass off to the cube
		int cubeY = CubeCoordinate.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = CubeCoordinate.blockToLocal( blockY );
			return cube.getBlockMetadata( localX, localY, localZ );
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
	public void removeEntityAtIndex( Entity entity, int cubeY )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override //      getTileEntity
	public TileEntity func_150806_e( int localX, int blockY, int localZ )
	{
		// pass off to the cube
		int cubeY = CubeCoordinate.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = CubeCoordinate.blockToLocal( blockY );
			return cube.getTileEntity( localX, localY, localZ );
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
