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

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CubicChunk
{
	private static final Logger log = LogManager.getLogger();
	
	private World m_world;
	private Column m_column;
	private int m_x;
	private int m_y;
	private int m_z;
	private ExtendedBlockStorage m_storage;
	private List<Entity> m_entities;
	private CubicChunkBlockMap<TileEntity> m_tileEntities;
	private boolean m_hasActiveEntities;
	private boolean m_isModified;
	private long m_lastSaveTime;
	
	public CubicChunk( World world, Column column, int x, int y, int z, boolean hasSky )
	{
		if( y < 0 )
		{
			throw new IllegalArgumentException( "y-coord of cubic chunk must be non-negative!" );
		}
		
		m_world = world;
		m_column = column;
		m_x = x;
		m_y = y;
		m_z = z;
		m_storage = new ExtendedBlockStorage( y << 4, hasSky );
		m_entities = new ArrayList<Entity>();
		m_tileEntities = new CubicChunkBlockMap<TileEntity>();
		m_hasActiveEntities = false;
		m_isModified = false;
		m_lastSaveTime = 0;
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
	
	public Block getBlock( int x, int y, int z )
	{
		return m_storage.func_150819_a( x, y, z );
	}
	
	public int getBlockMetadata( int x, int y, int z )
	{
		return m_storage.getMetadataArray().get( x, y, z );
	}
	
	public void setBlockSilently( int x, int y, int z, Block block, int meta )
	{
		m_storage.func_150818_a( x, y, z, block );
		m_storage.setExtBlockMetadata( x, y, z, meta );
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
		
		m_isModified = true;
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
	
	public boolean hasBlocks( )
	{
		return !m_storage.isEmpty();
	}
	
	public Iterable<TileEntity> tileEntities( )
	{
		return m_tileEntities.values();
	}
	
	public boolean hasActiveEntities( )
	{
		return m_hasActiveEntities;
	}
	public void setActiveEntities( boolean val )
	{
		m_hasActiveEntities = val;
	}
	
	public void addEntity( Entity entity )
	{
		// make sure the entity is in this cubic chunk
		int chunkX = Coords.getChunkXForEntity( entity );
		int chunkY = Coords.getChunkYForEntity( entity );
		int chunkZ = Coords.getChunkZForEntity( entity );
		if( chunkX != m_x || chunkY != m_y || chunkZ != m_z )
		{
			log.warn( String.format( "Entity %s in cubic chunk (%d,%d,%d) added to cubic chunk (%d,%d,%d)!",
				entity.getClass().getName(),
				chunkX, chunkY, chunkZ,
				m_x, m_y, m_z
			) );
		}
		
		// tell the entity it's in a cubic chunk
		// NOTE: we have to set the y coord to the chunk the entity is in, not the chunk it's standing on.
		// otherwise, the world will continually re-add the entity to the column
		entity.addedToChunk = true;
		entity.chunkCoordX = m_x;
		entity.chunkCoordY = MathHelper.floor_double( entity.posY/16 );
		entity.chunkCoordZ = m_z;
        
		m_entities.add( entity );
		m_hasActiveEntities = true;
	}
	
	public boolean removeEntity( Entity entity )
	{
		boolean wasRemoved = m_entities.remove( entity );
		if( wasRemoved )
		{
			m_isModified = true;
		}
		return wasRemoved;
	}
	
	public Iterable<Entity> entities( )
	{
		return m_entities;
	}
	
	public void getEntities( List<Entity> out, Class<?> c, AxisAlignedBB queryBox, IEntitySelector selector )
	{
		for( Entity entity : m_entities )
		{
			if( c.isAssignableFrom( entity.getClass() ) && entity.boundingBox.intersectsWith( queryBox ) && ( selector == null || selector.isEntityApplicable( entity ) ) )
			{
				out.add( entity );
			}
		}
	}
	
	public void getEntitiesExcept( List<Entity> out, Entity entityExclusion, AxisAlignedBB queryBox, IEntitySelector selector )
	{
		for( Entity entity : m_entities )
		{
			// handle entity exclusion
			if( entity == entityExclusion )
			{
				continue;
			}
			
			if( entity.boundingBox.intersectsWith( queryBox ) && ( selector == null || selector.isEntityApplicable( entity ) ) )
			{
				out.add( entity );
				
				// also check entity parts
				if( entity.getParts() != null )
				{
					for( Entity part : entity.getParts() )
					{
						if( part != entityExclusion && part.boundingBox.intersectsWith( queryBox ) && ( selector == null || selector.isEntityApplicable( part ) ) )
						{
							out.add( part );
						}
					}
				}
			}
		}
	}
	
	public void getMigratedEntities( List<Entity> out )
	{
		for( Entity entity : m_entities )
		{
			int chunkX = Coords.getChunkXForEntity( entity );
			int chunkY = Coords.getChunkYForEntity( entity );
			int chunkZ = Coords.getChunkZForEntity( entity );
			if( chunkX != m_x || chunkY != m_y || chunkZ != m_z )
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
		for( Entity entity : m_entities )
		{
			entity.onChunkLoad();
		}
		m_world.addLoadedEntities( m_entities );
		
		// tell the world about tile entities
		m_world.func_147448_a( m_tileEntities.values() );
	}
	
	public void onUnload( )
	{
		// tell the world to forget about entities
		m_world.unloadEntities( m_entities );
		
		// tell the world to forget about tile entities
		for( TileEntity tileEntity : m_tileEntities.values() )
		{
			m_world.func_147457_a( tileEntity );
		}
	}
	
	public boolean needsSaving( )
	{
		return ( m_hasActiveEntities && m_world.getTotalWorldTime() >= m_lastSaveTime + 600L ) || m_isModified;
	}
	
	public void markSaved( )
	{
		m_lastSaveTime = m_world.getTotalWorldTime();
		m_isModified = false;
	}
	
	public int getBlockLightValue( int localX, int localY, int localZ, int skylightSubtracted )
	{
		// get sky light
		int skyLight = 0;
		if( !m_world.provider.hasNoSky )
		{
			skyLight = m_storage.getExtSkylightValue( localX, localY, localZ );
		}
		skyLight -= skylightSubtracted;
		
		// get block light
		int blockLight = m_storage.getExtBlocklightValue( localX, localY, localZ );
		
		// FIGHT!!!
		if( blockLight > skyLight )
		{
			return blockLight;
		}
		return skyLight;
	}
	
	public int getLightValue( EnumSkyBlock lightType, int x, int y, int z )
	{
		if( lightType == EnumSkyBlock.Sky )
		{
			if( !m_world.provider.hasNoSky )
			{
				return m_storage.getExtSkylightValue( x, y, z );
			}
			else
			{
				return 0;
			}
		}
		else if( lightType == EnumSkyBlock.Block )
		{
			return m_storage.getExtBlocklightValue( x, y, z );
		}
		else
		{
			return lightType.defaultLightValue;
		}
	}
	
	public void setLightValue( EnumSkyBlock lightType, int x, int y, int z, int light )
	{
		if( lightType == EnumSkyBlock.Sky )
		{
			if( !m_world.provider.hasNoSky )
			{
				m_storage.setExtSkylightValue( x, y, z, light );
			}
		}
		else if( lightType == EnumSkyBlock.Block )
		{
			m_storage.setExtBlocklightValue( x, y, z, light );
		}
	}
}
