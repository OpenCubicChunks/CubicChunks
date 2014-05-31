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
package cuchaz.cubicChunks.world;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.generator.GeneratorStage;
import cuchaz.cubicChunks.util.CubeAddress;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeBlockMap;

public class Cube
{
	private static final Logger log = LogManager.getLogger();
	
	private World m_world;
	private Column m_column;
	private int m_x;
	private int m_y;
	private int m_z;
	private boolean m_isModified;
	private ExtendedBlockStorage m_storage;
	private EntityContainer m_entities;
	private CubeBlockMap<TileEntity> m_tileEntities;
	private GeneratorStage m_generatorStage;
	
	public Cube( World world, Column column, int x, int y, int z, boolean isModified )
	{
		m_world = world;
		m_column = column;
		m_x = x;
		m_y = y;
		m_z = z;
		m_isModified = isModified;
		
		m_storage = null;
		m_entities = new EntityContainer();
		m_tileEntities = new CubeBlockMap<TileEntity>();
		m_generatorStage = null;
	}
	
	public boolean isEmpty( )
	{
		return m_storage == null;
	}
	
	public void setEmpty( boolean isEmpty )
	{
		if( isEmpty )
		{
			m_storage = null;
		}
		else
		{
			m_storage = new ExtendedBlockStorage( m_y << 4, !m_world.provider.hasNoSky );
		}
	}
	
	public GeneratorStage getGeneratorStage( )
	{
		return m_generatorStage;
	}
	public void setGeneratorStage( GeneratorStage val )
	{
		m_generatorStage = val;
	}
	
	public long getAddress( )
	{
		return CubeAddress.getAddress( m_x, m_y, m_z );
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
	
	public EntityContainer getEntityContainer( )
	{
		return m_entities;
	}
	
	public ExtendedBlockStorage getStorage( )
	{
		return m_storage;
	}
	
	public Block getBlock( int x, int y, int z )
	{
		if( isEmpty() )
		{
			return Blocks.air;
		}
		
		return m_storage.func_150819_a( x, y, z );
	}
	
	public int getBlockMetadata( int x, int y, int z )
	{
		if( isEmpty() )
		{
			return 0;
		}
		
		return m_storage.getMetadataArray().get( x, y, z );
	}
	
	public boolean setBlock( int x, int y, int z, Block block, int meta )
	{
		// did anything actually change?
		Block oldBlock = getBlock( x, y, z );
		int oldMeta = getBlockMetadata( x, y, z );
		if( oldBlock == block && oldMeta == meta )
		{
			return false;
		}
		
		// make sure we're not empty
		if( isEmpty() )
		{
			setEmpty( false );
		}
		
		int blockX = Coords.localToBlock( m_x, x );
		int blockY = Coords.localToBlock( m_y, y );
		int blockZ = Coords.localToBlock( m_z, z );
		
		if( !m_world.isClient )
		{
			// on the server, tell the block it's about to be broken
			oldBlock.onBlockPreDestroy( m_world, blockX, blockY, blockZ, oldMeta );
		}
		
		// set the block
		m_storage.func_150818_a( x, y, z, block );
		
		if( !m_world.isClient )
		{
			// on the server, break the old block
			oldBlock.breakBlock( m_world, blockX, blockY, blockZ, oldBlock, oldMeta );
		}
		else if( oldBlock instanceof ITileEntityProvider && oldBlock != block )
		{
			// on the client, remove the tile entity
			m_world.removeTileEntity( blockX, blockY, blockZ );
		}
		
		// did the block change work correctly?
		if( m_storage.func_150819_a( x, y, z ) != block )
		{
			return false;
		}
		m_isModified = true;
		
		// set the meta
		m_storage.setExtBlockMetadata( x, y, z, meta );
		
		if( oldBlock instanceof ITileEntityProvider )
		{
			// update tile entity
			TileEntity tileEntity = getTileEntity( x, y, z );
			if( tileEntity != null )
			{
				tileEntity.updateContainingBlockInfo();
			}
		}
		
		if( !m_world.isClient )
		{
			// on the server, tell the block it was added
			block.onBlockAdded( m_world, blockX, blockY, blockZ );
		}
		
		if( block instanceof ITileEntityProvider )
		{
			// make sure the tile entity is good
			TileEntity tileEntity = getTileEntity( x, y, z );
			if( tileEntity == null )
			{
				tileEntity = ( (ITileEntityProvider)block ).createNewTileEntity( m_world, meta );
				m_world.setTileEntity( blockX, blockY, blockZ, tileEntity );
			}
			if( tileEntity != null )
			{
				tileEntity.updateContainingBlockInfo();
			}
		}
		
		return true;
	}
	
	public boolean setBlockMetadata( int x, int y, int z, int meta )
	{
		// did anything even change
		int oldMeta = getBlockMetadata( x, y, z );
		if( meta == oldMeta )
		{
			return false;
		}
		
		// make sure we're not empty
		if( isEmpty() )
		{
			setEmpty( false );
		}
		
		// change the metadata
		m_storage.setExtBlockMetadata( x, y, z, meta );
		m_isModified = true;
		
		// notify any tile entities of the change
		if( getBlock( x, y, z ) instanceof ITileEntityProvider )
		{
			TileEntity tileEntity = getTileEntity( x, y, z );
			if( tileEntity != null )
			{
				tileEntity.updateContainingBlockInfo();
				tileEntity.blockMetadata = meta;
			}
		}
		
		return true;
	}
	
	public boolean setBlockForGeneration( int x, int y, int z, Block block )
	{
		return setBlockForGeneration( x, y, z, block, 0 );
	}
	
	public boolean setBlockForGeneration( int x, int y, int z, Block block, int meta )
	{
		// null blocks are really air
		if( block == null )
		{
			block = Blocks.air;
		}
		
		// did anything actually change?
		Block oldBlock = getBlock( x, y, z );
		int oldMeta = getBlockMetadata( x, y, z );
		if( oldBlock == block && oldMeta == meta )
		{
			return false;
		}
		
		// make sure we're not empty
		if( isEmpty() )
		{
			setEmpty( false );
		}
		
		// set the block
		m_storage.func_150818_a( x, y, z, block );
		
		// did the block change work correctly?
		if( m_storage.func_150819_a( x, y, z ) != block )
		{
			return false;
		}
		m_isModified = true;
		
		// set the meta
		m_storage.setExtBlockMetadata( x, y, z, meta );
		
		// update the column light index
		int blockY = Coords.localToBlock( m_y, y );
		m_column.getLightIndex().setOpacity( x, blockY, z, block.getLightOpacity() );
		
		return true;
	}
	
	public boolean hasBlocks( )
	{
		if( isEmpty() )
		{
			return false;
		}
		
		return !m_storage.isEmpty();
	}
	
	public Iterable<TileEntity> tileEntities( )
	{
		return m_tileEntities.values();
	}
	
	public void addEntity( Entity entity )
	{
		// make sure the entity is in this cube
		int cubeX = Coords.getCubeXForEntity( entity );
		int cubeY = Coords.getCubeYForEntity( entity );
		int cubeZ = Coords.getCubeZForEntity( entity );
		if( cubeX != m_x || cubeY != m_y || cubeZ != m_z )
		{
			log.warn( String.format( "Entity %s in cube (%d,%d,%d) added to cube (%d,%d,%d)!",
				entity.getClass().getName(),
				cubeX, cubeY, cubeZ,
				m_x, m_y, m_z
			) );
		}
		
		// tell the entity it's in this cube
		entity.addedToChunk = true;
		entity.chunkCoordX = m_x;
		entity.chunkCoordY = m_y;
		entity.chunkCoordZ = m_z;
        
		m_entities.add( entity );
		m_isModified = true;
	}
	
	public boolean removeEntity( Entity entity )
	{
		boolean wasRemoved = m_entities.remove( entity );
		if( wasRemoved )
		{
			entity.addedToChunk = false;
			m_isModified = true;
		}
		else
		{
			log.warn( String.format( "%s Tried to remove entity %s from cube (%d,%d,%d), but it was not there. Entity thinks it's in cube (%d,%d,%d)",
				m_world.isClient? "CLIENT" : "SERVER",
				entity.getClass().getName(),
				m_x, m_y, m_z,
				entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ
			) );
		}
		return wasRemoved;
	}
	
	public Iterable<Entity> entities( )
	{
		return m_entities.entities();
	}
	
	public void getEntities( List<Entity> out, Class<?> c, AxisAlignedBB queryBox, IEntitySelector selector )
	{
		m_entities.getEntities( out, c, queryBox, selector );
	}
	
	public void getEntitiesExcept( List<Entity> out, Entity excludedEntity, AxisAlignedBB queryBox, IEntitySelector selector )
	{
		m_entities.getEntitiesExcept( out, excludedEntity, queryBox, selector );
	}
	
	public void getMigratedEntities( List<Entity> out )
	{
		for( Entity entity : m_entities.entities() )
		{
			int cubeX = Coords.getCubeXForEntity( entity );
			int cubeY = Coords.getCubeYForEntity( entity );
			int cubeZ = Coords.getCubeZForEntity( entity );
			if( cubeX != m_x || cubeY != m_y || cubeZ != m_z )
			{
				out.add( entity );
			}
		}
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
			int blockX = Coords.localToBlock( m_x, x );
			int blockY = Coords.localToBlock( m_y, y );
			int blockZ = Coords.localToBlock( m_z, z );
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
		int blockX = Coords.localToBlock( m_x, x );
		int blockY = Coords.localToBlock( m_y, y );
		int blockZ = Coords.localToBlock( m_z, z );
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
			m_isModified = true;
		}
	}
	
	public void removeTileEntity( int x, int y, int z )
	{
		TileEntity tileEntity = m_tileEntities.remove( x, y, z );
		if( tileEntity != null )
		{
			tileEntity.invalidate();
			m_isModified = true;
		}
	}
	
	public void onLoad( )
	{
		// tell the world about entities
		for( Entity entity : m_entities.entities() )
		{
			entity.onChunkLoad();
		}
		m_world.addLoadedEntities( m_entities.entities() );
		
		// tell the world about tile entities
		m_world.func_147448_a( m_tileEntities.values() );
	}
	
	public void onUnload( )
	{
		// tell the world to forget about entities
		m_world.unloadEntities( m_entities.entities() );
		
		// tell the world to forget about tile entities
		for( TileEntity tileEntity : m_tileEntities.values() )
		{
			m_world.func_147457_a( tileEntity );
		}
	}
	
	public boolean needsSaving( )
	{
		return m_entities.needsSaving( m_world.getTotalWorldTime() ) || m_isModified;
	}
	
	public void markSaved( )
	{
		m_entities.markSaved( m_world.getTotalWorldTime() );
		m_isModified = false;
	}
	
	public boolean isUnderground( int localX, int localY, int localZ )
	{
		return m_column.getLightIndex().getTopNonTransparentBlock( localX, localZ ) >= Coords.localToBlock( m_y, localY );
	}
	
	public int getBlockLightValue( int localX, int localY, int localZ, int skylightSubtracted )
	{
		// get sky light
		int skyLight = getLightValue( EnumSkyBlock.Sky, localX, localY, localZ );
		skyLight -= skylightSubtracted;
		
		// get block light
		int blockLight = getLightValue( EnumSkyBlock.Block, localX, localY, localZ );
		
		// FIGHT!!!
		if( blockLight > skyLight )
		{
			return blockLight;
		}
		return skyLight;
	}
	
	public int getLightValue( EnumSkyBlock lightType, int localX, int localY, int localZ )
	{
		switch( lightType )
		{
			case Sky:
				if( !m_world.provider.hasNoSky )
				{
					if( isEmpty() )
					{
						if( isUnderground( localX, localY, localZ ) )
						{
							return 0;
						}
						else
						{
							return 15;
						}
					}
					
					return m_storage.getExtSkylightValue( localX, localY, localZ );
				}
				else
				{
					return 0;
				}
				
			case Block:
				if( isEmpty() )
				{
					return 0;
				}
				
				return m_storage.getExtBlocklightValue( localX, localY, localZ );
				
			default:
				return lightType.defaultLightValue;
		}
	}
	
	public void setLightValue( EnumSkyBlock lightType, int x, int y, int z, int light )
	{
		// make sure we're not empty
		if( isEmpty() )
		{
			setEmpty( false );
		}
		
		switch( lightType )
		{
			case Sky:
				if( !m_world.provider.hasNoSky )
				{
					m_storage.setExtSkylightValue( x, y, z, light );
					m_isModified = true;
				}
			break;
			
			case Block:
				m_storage.setExtBlocklightValue( x, y, z, light );
				m_isModified = true;
			break;
		}
	}
	
	public void doRandomTicks( )
	{
		if( isEmpty() || !m_storage.getNeedsRandomTick() )
		{
			return;
		}
		
		// do three random ticks
		for( int i=0; i<3; i++ )
		{
			// get a random block
			int index = m_world.rand.nextInt();
			int localX = index & 15;
			int localY = index >> 8 & 15;
			int localZ = index >> 16 & 15;
			Block block = m_storage.func_150819_a( localX, localY, localZ );
			
			if( block.getTickRandomly() )
			{
				// tick it
				int blockX = Coords.localToBlock( m_x, localX );
				int blockY = Coords.localToBlock( m_y, localY );
				int blockZ = Coords.localToBlock( m_z, localZ );
				block.updateTick( m_world, blockX, blockY, blockZ, m_world.rand );
			}
		}
	}
	
	public void markForRenderUpdate( )
	{
		m_world.markBlockRangeForRenderUpdate(
			Coords.cubeToMinBlock( m_x ), Coords.cubeToMinBlock( m_y ), Coords.cubeToMinBlock( m_z ),
			Coords.cubeToMaxBlock( m_x ), Coords.cubeToMaxBlock( m_y ), Coords.cubeToMaxBlock( m_z )
		);
	}
}
