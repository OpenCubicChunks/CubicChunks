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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Column extends Chunk
{
	private static final Logger log = LogManager.getLogger();
	
	private TreeMap<Integer,CubicChunk> m_cubicChunks;
	private ExtendedBlockStorage[] m_legacySegments;
	
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
		
		// for each block...
		for( int localX=0; localX<16; localX++ )
		{
			for( int localZ=0; localZ<16; localZ++ )
			{
				for( int blockY=0; blockY<maxY; blockY++ )
				{
					int blockIndex = localX*maxY*16 | localZ*maxY | blockY;
					Block block = blocks[blockIndex];
					if( block != null && block != Blocks.air )
					{
						// get the cubic chunk
						int chunkY = Coords.blockToChunk( blockY );
						CubicChunk cubicChunk = getCubicChunk( chunkY );
						if( cubicChunk == null )
						{
							cubicChunk = new CubicChunk( world, this, chunkX, chunkY, chunkZ, hasSky );
							m_cubicChunks.put( chunkY, cubicChunk );
						}
						
						// save the block
						int localY = Coords.blockToLocal( blockY );
						
						// NOTE: don't call CubicChunk.setBlock() during chunk loading!
						// it will send block events and cause bad things to happen
						cubicChunk.setBlockSilently( localX, localY, localZ, block, meta[blockIndex] );
					}
				}
			}
		}
    }
	
	private void init( )
	{
		m_cubicChunks = new TreeMap<Integer,CubicChunk>();
		m_legacySegments = null;
		
		// make sure no one's using the Minecraft segments
		setStorageArrays( null );
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
	
	public Iterable<CubicChunk> getCubicChunks( int minY, int maxY )
	{
		return m_cubicChunks.subMap( minY, true, maxY, true ).values();
	}
	
	public void addCubicChunk( CubicChunk cubicChunk )
	{
		m_cubicChunks.put( cubicChunk.getY(), cubicChunk );
		m_legacySegments = null;
	}
	
	private CubicChunk addEmptyCubicChunk( int chunkY )
	{
		// is there already a chunk here?
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			log.warn( String.format( "Column (%d,%d) already has cubic chunk at %d!", xPosition, zPosition, chunkY ) );
			return cubicChunk;
		}
		
		// make a new empty chunk
		cubicChunk = new CubicChunk( worldObj, this, xPosition, chunkY, zPosition, !worldObj.provider.hasNoSky );
		addCubicChunk( cubicChunk );
		return cubicChunk;
	}
	
	public List<RangeInt> getCubicChunkYRanges( )
	{
		// compute a kind of run-length encoding on the cubic chunk y-values
		List<RangeInt> ranges = new ArrayList<RangeInt>();
		Integer start = null;
		Integer stop = null;
		for( int chunkY : m_cubicChunks.keySet() )
		{
			if( start == null )
			{
				// start a new range
				start = chunkY;
				stop = chunkY;
			}
			else if( chunkY == stop + 1 )
			{
				// extend the range
				stop = chunkY;
			}
			else
			{
				// end the range
				ranges.add( new RangeInt( start, stop ) );
				
				// start a new range
				start = chunkY;
				stop = chunkY;
			}
		}
		
		if( start != null )
		{
			// finish the last range
			ranges.add( new RangeInt( start, stop ) );
		}
		
		return ranges;
	}
	
	@Override
	public boolean needsSaving( boolean alwaysTrue )
	{
		for( CubicChunk cubicChunk : m_cubicChunks.values() )
		{
			if( cubicChunk.needsSaving() )
			{
				return true;
			}
		}
		return false;
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
		return Blocks.air;
	}
	
	@Override //        setBlock
	public boolean func_150807_a( int localX, int blockY, int localZ, Block block, int meta )
	{
		// is there a chunk for this block?
		int chunkY = Coords.blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk == null )
		{
			if( block == Blocks.air )
			{
				return false;
			}
			
			// make a new chunk for the block
			cubicChunk = addEmptyCubicChunk( chunkY );
		}
		
		// pass off to chunk
		int localY = Coords.blockToLocal( blockY );
		boolean changed = cubicChunk.setBlock( localX, localY, localZ, block, meta );
		if( !changed )
		{
			return false;
		}
		
		/* UNDONE: update height data structures
		int xzCoord = localZ << 4 | localX;
		int highestY = heightMap[xzCoord];
		boolean newHighestBlock = blockY >= highestY;
		*/
		
		/* UNDONE: redo lighting
		if( newHighestBlock )
		{
			this.generateSkylightMap();
		}
		else
		{
			int var14 = block.getLightOpacity();
			int var15 = oldBlock.getLightOpacity();
			
			if( var14 > 0 )
			{
				if( blockY >= highestY )
				{
					this.relightBlock( localX, blockY + 1, localZ );
				}
			}
			else if( blockY == highestY - 1 )
			{
				this.relightBlock( localX, blockY, localZ );
			}
			
			if( var14 != var15
					&& ( var14 < var15 || this.getSavedLightValue( EnumSkyBlock.Sky, localX, blockY, localZ ) > 0 || this.getSavedLightValue( EnumSkyBlock.Block, localX, blockY, localZ ) > 0 ) )
			{
				this.propagateSkylightOcclusion( localX, localZ );
			}
		}
		*/
		
		return true;
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
		// pass off to the cubic chunk
		int chunkY = Coords.blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cubicChunk.setBlockMetadata( localX, localY, localZ, meta );
		}
		return false;
	}
	
	@Override
	public ExtendedBlockStorage[] getBlockStorageArray( )
	{
		if( m_legacySegments == null )
		{
			// build the segments index
			if( m_cubicChunks.isEmpty() )
			{
				m_legacySegments = new ExtendedBlockStorage[0];
			}
			else
			{
				m_legacySegments = new ExtendedBlockStorage[m_cubicChunks.lastKey()+1];
				for( CubicChunk cubicChunk : m_cubicChunks.values() )
				{
					m_legacySegments[cubicChunk.getY()] = cubicChunk.getStorage();
				}
			}
		}
		return m_legacySegments;
	}
	
	@Override
	public int getTopFilledSegment()
    {
		for( CubicChunk cubicChunk : m_cubicChunks.descendingMap().values() )
		{
			if( cubicChunk.hasBlocks() )
			{
				return Coords.chunkToMinBlock( cubicChunk.getY() );
			}
		}
		return 0;
    }
	
	@Override
	public boolean getAreLevelsEmpty( int minBlockY, int maxBlockY )
	{
		int minChunkY = Coords.blockToChunk( minBlockY );
		int maxChunkY = Coords.blockToChunk( maxBlockY );
		for( int chunkY=minChunkY; chunkY<=maxChunkY; chunkY++ )
		{
			CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
			if( cubicChunk != null && cubicChunk.hasBlocks() )
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void addEntity( Entity entity )
    {
		// make sure the y-coord is sane
		int chunkY = Coords.getChunkYForEntity( entity );
		if( chunkY < 0 )
		{
			return;
		}
		
		// pass off to the cubic chunk
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk == null )
		{
			// make a new chunk for the entity
			cubicChunk = addEmptyCubicChunk( chunkY );
		}
		
		cubicChunk.addEntity( entity );
    }
	
	@Override
	public void removeEntity( Entity entity )
	{
		removeEntityAtIndex( entity, entity.chunkCoordY );
	}
	
	@Override
	public void removeEntityAtIndex( Entity entity, int chunkY )
	{
		// pass off to the cubic chunk
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			cubicChunk.removeEntity( entity );
		}
	}
	
	@Override
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public void getEntitiesOfTypeWithinAAAB( Class c, AxisAlignedBB queryBox, List out, IEntitySelector selector )
	{
		// get a y-range that 2 blocks wider than the box for safety
		int minChunkY = Coords.blockToChunk( MathHelper.floor_double( queryBox.minY - 2 ) );
		int maxChunkY = Coords.blockToChunk( MathHelper.floor_double( queryBox.maxY + 2 ) );
		for( CubicChunk cubicChunk : getCubicChunks( minChunkY, maxChunkY ) )
		{
			cubicChunk.getEntities( (List<Entity>)out, c, queryBox, selector );
		}
	}
	
	@Override
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public void getEntitiesWithinAABBForEntity( Entity entity, AxisAlignedBB queryBox, List out, IEntitySelector selector )
	{
		// get a y-range that 2 blocks wider than the box for safety
		int minChunkY = Coords.blockToChunk( MathHelper.floor_double( queryBox.minY - 2 ) );
		int maxChunkY = Coords.blockToChunk( MathHelper.floor_double( queryBox.maxY + 2 ) );
		for( CubicChunk cubicChunk : getCubicChunks( minChunkY, maxChunkY ) )
		{
			cubicChunk.getEntitiesExcept( (List<Entity>)out, entity, queryBox, selector );
		}
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
	@SuppressWarnings( "unchecked" )
	public void addTileEntity( TileEntity tileEntity )
	{
		// NOTE: this is called only by the chunk loader
		
		int blockX = tileEntity.field_145851_c;
		int blockY = tileEntity.field_145848_d;
		int blockZ = tileEntity.field_145849_e;
		
		// pass off to the cubic chunk
		int chunkY = Coords.blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			int localX = Coords.blockToLocal( blockX );
			int localY = Coords.blockToLocal( blockY );
			int localZ = Coords.blockToLocal( blockZ );
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
		
		// pass off to the cubic chunk
		int chunkY = Coords.blockToChunk( blockY );
		CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
		if( cubicChunk != null )
		{
			int localY = Coords.blockToLocal( blockY );
			cubicChunk.addTileEntity( localX, localY, localZ, tileEntity );
		}
		else
		{
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
			int chunkY = Coords.blockToChunk( blockY );
			CubicChunk cubicChunk = m_cubicChunks.get( chunkY );
			if( cubicChunk != null )
			{
				int localY = Coords.blockToLocal( blockY );
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
	
	@Override
	public void generateHeightMap()
	{
		// UNDONE: implement this for realsies
		heightMapMinimum = 30;
		for( int i=0; i<heightMap.length; i++ )
		{
			heightMap[i] = heightMapMinimum;
		}
	}
	
	@Override
	public void generateSkylightMap()
    {
		// UNDONE: implement lighting
		generateHeightMap();
    }
	
	@Override
	public int getBlockLightValue( int localX, int blockY, int localZ, int skylightSubtracted )
	{
		// return 0-15
		return 15;
	}
	
	@Override
	public int getSavedLightValue( EnumSkyBlock skyBlock, int localX, int blockY, int localZ )
	{
		// return 0-15
		return 15;
	}
	
	@Override
	public void setLightValue( EnumSkyBlock skyBlock, int localX, int blockY, int localZ, int light )
	{
		// ignore
	}
	
	@Override //         tick
	public void func_150804_b( boolean tryToTickFaster )
	{
		super.func_150804_b( tryToTickFaster );
		
		// migrate moved entities to new cubic chunks
		// UNDONE: optimize out the new
		List<Entity> entities = new ArrayList<Entity>();
		for( CubicChunk cubicChunk : m_cubicChunks.values() )
		{
			cubicChunk.getMigratedEntities( entities );
			for( Entity entity : entities )
			{
				int chunkX = Coords.getChunkXForEntity( entity );
				int chunkY = Coords.getChunkYForEntity( entity );
				int chunkZ = Coords.getChunkZForEntity( entity );
				
				if( chunkX != xPosition || chunkZ != zPosition )
				{
					// Unfortunately, entities get updated after chunk ticks
					// that means entities might appear to be in the wrong column this tick,
					// but they'll be corrected before the next tick during column migration
					// so we can safely ignore them
					continue;
				}
				
				// try to find the new cubic chunk for this entity
				CubicChunk newCubicChunk = m_cubicChunks.get( chunkY );
				if( newCubicChunk == null )
				{
					log.warn( String.format( "Entity %s migrated from cubic chunk (%d,%d,%d) to unloaded cubic chunk (%d,%d,%d). Why hasn't this chunk loaded?",
						entity.getClass().getName(),
						cubicChunk.getX(), cubicChunk.getY(), cubicChunk.getZ(),
						xPosition, chunkY, zPosition
					) );
					continue;
				}
				
				// move the entity to the new cubic chunk
				cubicChunk.removeEntity( entity );
				newCubicChunk.addEntity( entity );
			}
		}
	}
}
