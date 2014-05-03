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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.gen.CubeBlocks;
import cuchaz.cubicChunks.util.AddressTools;
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
	private ExtendedBlockStorage m_storage;
	private EntityContainer m_entities;
	private CubeBlockMap<TileEntity> m_tileEntities;
	private boolean m_isModified;
	
	public Cube( World world, Column column, int x, int y, int z, boolean hasSky )
	{
		if( y < 0 )
		{
			throw new IllegalArgumentException( "y-coord of cube must be non-negative!" );
		}
		
		m_world = world;
		m_column = column;
		m_x = x;
		m_y = y;
		m_z = z;
		m_storage = new ExtendedBlockStorage( y << 4, hasSky );
		m_entities = new EntityContainer();
		m_tileEntities = new CubeBlockMap<TileEntity>();
		m_isModified = false;
	}
	
	public static Cube generateCubeAndAddToColumn( World world, Column column, int cubeX, int cubeY, int cubeZ, boolean hasSky, CubeBlocks blocks )
	{
		Cube cube = new Cube( world, column, cubeX, cubeY, cubeZ, hasSky );
		
		// copy over the block data
		Block block = null;
		for( int x=0; x<16; x++ )
		{
			for( int y=0; y<16; y++ )
			{
				for( int z=0; z<16; z++ )
				{
					// save the block data
					block = blocks.getBlock( x, y, z );
					if( block == null )
					{
						continue;
					}
					
					cube.m_storage.func_150818_a( x, y, z, block );
					cube.m_storage.setExtBlockMetadata( x, y, z, blocks.getMeta( x, y, z ) );
					
					// update the column light index
					column.getLightIndex().setOpacity( x, y, z, block.getLightOpacity() );
				}
			}
		}
		
		column.addCube( cube );
		
		cube.m_isModified = true;
		
		return cube;
	}
	
	public long getAddress( )
	{
		return AddressTools.getAddress( m_x, m_y, m_z );
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
		return m_storage.func_150819_a( x, y, z );
	}
	
	public int getBlockMetadata( int x, int y, int z )
	{
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
		
		// tell the entity it's in a cube
		// NOTE: we have to set the y coord to the chunk the entity is in, not the chunk it's standing on.
		// otherwise, the world will continually re-add the entity to the column
		entity.addedToChunk = true;
		entity.chunkCoordX = m_x;
		entity.chunkCoordY = MathHelper.floor_double( entity.posY/16 );
		entity.chunkCoordZ = m_z;
        
		m_entities.add( entity );
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
		m_isModified = true;
	}
	
	public void populateLight( )
	{
		// UNDONE: do me
		// look at chunkPopulateLighting()
	}
	
	/* for reference

    public void chunkPopulateLighting()
    {
        this.isTerrainPopulated = true;
        this.isLightPopulated = true;
        
        if (!this.worldObj.provider.hasNoSky)
        {
            if ( this.worldObj.checkChunksExist(this.xPosition * 16 - 1, 0, this.zPosition * 16 - 1, this.xPosition * 16 + 1, 63, this.zPosition * 16 + 1))
            {
                for (int var1 = 0; var1 < 16; ++var1)
                {
                    for (int var2 = 0; var2 < 16; ++var2)
                    {
                        if (!this.chunkPopulateBlockColumnLighting(var1, var2))
                        {
                            this.isLightPopulated = false;
                            break;
                        }
                    }
                }

                if (this.isLightPopulated)
                {
                    Chunk var3 = this.worldObj.getChunkFromBlockCoords(this.xPosition * 16 - 1, this.zPosition * 16);
                    var3.func_150801_a(3);
                    var3 = this.worldObj.getChunkFromBlockCoords(this.xPosition * 16 + 16, this.zPosition * 16);
                    var3.func_150801_a(1);
                    var3 = this.worldObj.getChunkFromBlockCoords(this.xPosition * 16, this.zPosition * 16 - 1);
                    var3.func_150801_a(0);
                    var3 = this.worldObj.getChunkFromBlockCoords(this.xPosition * 16, this.zPosition * 16 + 16);
                    var3.func_150801_a(2);
                }
            }
            else
            {
                this.isLightPopulated = false;
            }
        }
    }

    private void chunkPopulateEdgeLighting(int edge) // 0,1,2, or 3
    {
        if (this.isTerrainPopulated)
        {
            int var2;

            if (edge == 3)
            {
                for (var2 = 0; var2 < 16; ++var2)
                {
                    this.chunkPopulateBlockColumnLighting(15, var2);
                }
            }
            else if (edge == 1)
            {
                for (var2 = 0; var2 < 16; ++var2)
                {
                    this.chunkPopulateBlockColumnLighting(0, var2);
                }
            }
            else if (edge == 0)
            {
                for (var2 = 0; var2 < 16; ++var2)
                {
                    this.chunkPopulateBlockColumnLighting(var2, 15);
                }
            }
            else if (edge == 2)
            {
                for (var2 = 0; var2 < 16; ++var2)
                {
                    this.chunkPopulateBlockColumnLighting(var2, 0);
                }
            }
        }
    }

    private boolean chunkPopulateBlockColumnLighting(int localX, int localZ)
    {
        int topSegmentBottomBlockY = this.getTopFilledSegment();
        boolean foundNonTransparentBlock = false;
        boolean foundOpaqueBlockBelowSeaLevel = false;
        int blockY;

        // start at the top of the top segment
        // keep dropping while we're above sea level
        // or keep dropping until we hit an opaque block below sea level
        for (blockY = topSegmentBottomBlockY + 16 - 1; blockY > 63 || blockY > 0 && !foundOpaqueBlockBelowSeaLevel; --blockY)
        {
            int lightOpacity = this.func_150808_b(localX, blockY, localZ);

            // if we hit an opaque block below sea level
            if (lightOpacity == 255 && blockY < 63)
            {
                foundOpaqueBlockBelowSeaLevel = true;
            }
            
            // check the blocks above the opaque-below-sea-level block for something...
            if (!foundNonTransparentBlock && lightOpacity > 0)
            {
                foundNonTransparentBlock = true;
            }
            // if we're below a non-transparent block and we're a clear block, then update lights for this block
            // func_147451_t (updateLights) only returns false when nearby chunks are missing (and hence we can't do full lighting)
            else if (foundNonTransparentBlock && lightOpacity == 0 && !this.worldObj.func_147451_t(this.xPosition * 16 + localX, blockY, this.zPosition * 16 + localZ))
            {
                return false;
            }
        }

        // update lights for light source blocks under and including this block
        for (; blockY > 0; --blockY)
        {
            if (this.func_150810_a(localX, blockY, localZ).getLightValue() > 0)
            {
                this.worldObj.func_147451_t(this.xPosition * 16 + localX, blockY, this.zPosition * 16 + localZ);
            }
        }

        return true;
    }
    */
}
