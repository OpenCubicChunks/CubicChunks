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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.CubeWorldProvider;
import cuchaz.cubicChunks.client.CubeProviderClient;
import cuchaz.cubicChunks.generator.GeneratorStage;
import cuchaz.cubicChunks.generator.biome.WorldColumnManager;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.Bits;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.RangeInt;

public class Column extends Chunk
{
	private static final Logger log = LogManager.getLogger();

	private static final ExtendedBlockStorage[] m_emptyStorageArray = new ExtendedBlockStorage[0];

	private TreeMap<Integer, Cube> m_cubes;
	private LightIndex m_lightIndex;
	private int m_roundRobinLightUpdatePointer;
	private List<Cube> m_roundRobinCubes;
	private EntityContainer m_entities;
	private byte[] columnBlockBiomeArray;

	public Column( World world, int x, int z )
	{
		// NOTE: this constructor is called by the chunk loader
		super( world, x, z );

		init();
	}

	public Column( World world, int cubeX, int cubeZ, CubeBiomeGenBase[] biomes )
	{
		// NOTE: this constructor is called by the cube generator
		this( world, cubeX, cubeZ );
		
		// save the biome data
		byte[] biomeArray = getBiomeArray();
		assert biomes.length == biomeArray.length;
		for( int i = 0; i < biomeArray.length; i++ )
		{
			biomeArray[i] = (byte)biomes[i].biomeID;
		}

		isModified = true;
	}

	private void init()
	{
		m_cubes = new TreeMap<Integer, Cube>();
		m_lightIndex = new LightIndex( getWorldProvider().getSeaLevel() );
		m_roundRobinLightUpdatePointer = 0;
		m_roundRobinCubes = new ArrayList<Cube>();
		m_entities = new EntityContainer();
		columnBlockBiomeArray = new byte[256];

		// make sure no one's using data structures that have been replaced
		// also saves memory
		setStorageArrays( null );
		heightMap = null;
		updateSkylightColumns = null;
		super.setBiomeArray( null );

		Arrays.fill( this.columnBlockBiomeArray, (byte)- 1 );
	}

	public long getAddress()
	{
		return AddressTools.getAddress( xPosition, zPosition );
	}

	public World getWorld()
	{
		return worldObj;
	}

	private CubeWorldProvider getWorldProvider()
	{
		return (CubeWorldProvider)worldObj.provider;
	}

	public int getX()
	{
		return xPosition;
	}

	public int getZ()
	{
		return zPosition;
	}

	public EntityContainer getEntityContainer()
	{
		return m_entities;
	}

	public LightIndex getLightIndex()
	{
		return m_lightIndex;
	}

	@Override
	public void generateHeightMap()
	{
		// override this so no height map is generated
	}

	public Iterable<Cube> cubes()
	{
		return m_cubes.values();
	}

	public boolean hasCubes()
	{
		return !m_cubes.isEmpty();
	}

	public Cube getCube( int y )
	{
		return m_cubes.get( y );
	}

	public Cube getOrCreateCube( int cubeY, boolean isModified )
	{
		Cube cube = m_cubes.get( cubeY );
		if( cube == null )
		{
			cube = new Cube( worldObj, this, xPosition, cubeY, zPosition, isModified );
			m_cubes.put( cubeY, cube );
		}
		return cube;
	}

	public Iterable<Cube> getCubes( int minY, int maxY )
	{
		return m_cubes.subMap( minY, true, maxY, true ).values();
	}
	
	public int loadedCubes()
	{
		return m_cubes.size();	
	}

	public Cube removeCube( int cubeY )
	{
		return m_cubes.remove( cubeY );
	}

	public List<RangeInt> getCubeYRanges()
	{
		return getRanges( m_cubes.keySet() );
	}

	@Override
	public boolean needsSaving( boolean alwaysTrue )
	{
		return m_entities.needsSaving( worldObj.getTotalWorldTime() ) || isModified;
	}

	public void markSaved()
	{
		m_entities.markSaved( worldObj.getTotalWorldTime() );
		isModified = false;
	}

	@Override //      getBlock
	public Block func_150810_a( final int localX, final int blockY, final int localZ )
	{
		// pass off to the cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cube.getBlock( localX, localY, localZ );
		}

		return Blocks.air;
	}

	@Override //        setBlock
	public boolean func_150807_a( int localX, int blockY, int localZ, Block block, int meta )
	{
		// is there a chunk for this block?
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube == null )
		{
			return false;
		}

		// did anything change?
		int localY = Coords.blockToLocal( blockY );
		Block oldBlock = cube.getBlock( localX, localY, localZ );
		boolean changed = cube.setBlock( localX, localY, localZ, block, meta );
		if( !changed )
		{
			return false;
		}

		// update rain map
		// NOTE: precipitationHeightMap[xzCoord] is he lowest block that will contain rain
		// so precipitationHeightMap[xzCoord] - 1 is the block that is being rained on
		int xzCoord = localZ << 4 | localX;
		if( blockY >= precipitationHeightMap[xzCoord] - 1 )
		{
			// invalidate the rain height map value
			precipitationHeightMap[xzCoord] = -999;
		}

		int newOpacity = block.getLightOpacity();
		int oldOpacity = oldBlock.getLightOpacity();

		int blockX = Coords.localToBlock( xPosition, localX );
		int blockZ = Coords.localToBlock( zPosition, localZ );

		CubeWorld cubeWorld = (CubeWorld)worldObj;

		// did the top non-transparent block change?
		Integer oldSkylightY = getSkylightBlockY( localX, localZ );
		getLightIndex().setOpacity( localX, blockY, localZ, newOpacity );
		Integer newSkylightY = getSkylightBlockY( localX, localZ );
		if( oldSkylightY != null && newSkylightY != null && oldSkylightY != newSkylightY )
		{
			// sort the y-values
			int minBlockY = oldSkylightY;
			int maxBlockY = newSkylightY;
			if( minBlockY > maxBlockY )
			{
				minBlockY = newSkylightY;
				maxBlockY = oldSkylightY;
			}
			assert (minBlockY <= maxBlockY): "Values not sorted! " + minBlockY + ", " + maxBlockY;

			// update light and signal render update
			cubeWorld.getLightingManager().computeSkyLightUpdate( this, localX, localZ, minBlockY, maxBlockY );
			worldObj.markBlockRangeForRenderUpdate( blockX, minBlockY, blockZ, blockX, maxBlockY, blockZ );
		}

		// if opacity changed and ( opacity decreased or block now has any light )
		int skyLight = getSavedLightValue( EnumSkyBlock.Sky, localX, blockY, localZ );
		int blockLight = getSavedLightValue( EnumSkyBlock.Block, localX, blockY, localZ );
		if( newOpacity != oldOpacity && (newOpacity < oldOpacity || skyLight > 0 || blockLight > 0) )
		{
			cubeWorld.getLightingManager().queueSkyLightOcclusionCalculation( blockX, blockZ );
		}

		// update lighting index
		getLightIndex().setOpacity( localX, blockY, localZ, block.getLightOpacity() );

		isModified = true;

		// NOTE: after this method, the World calls updateLights on the source block which changes light values again
		return true;
	}

	@Override
	public int getBlockMetadata( int localX, int blockY, int localZ )
	{
		// pass off to the cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cube.getBlockMetadata( localX, localY, localZ );
		}
		return 0;
	}

	@Override
	public boolean setBlockMetadata( int localX, int blockY, int localZ, int meta )
	{
		// pass off to the cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cube.setBlockMetadata( localX, localY, localZ, meta );
		}
		return false;
	}

	@Override
	public ExtendedBlockStorage[] getBlockStorageArray()
	{
		// don't use this anymore
		return m_emptyStorageArray;
	}

	public int getTopCubeY()
	{
		return m_cubes.lastKey();
	}

	public int getBottomCubeY()
	{
		return m_cubes.firstKey();
	}

	public Integer getTopFilledCubeY()
	{
		Integer blockY = getLightIndex().getTopNonTransparentBlockY();
		if( blockY == null )
		{
			return null;
		}
		return Coords.blockToCube( blockY );
	}

	@Override
	@Deprecated // don't use this! It's only here because vanilla needs it, but we need to be hacky about it
	public int getTopFilledSegment()
	{
		Integer cubeY = getTopFilledCubeY();
		if( cubeY != null )
		{
			return Coords.cubeToMinBlock( cubeY );
		}
		else
		{
			// PANIC!
			// this column doesn't have any blocks in it that aren't air!
			// but we can't return null here because vanilla code expects there to be a surface down there somewhere
			// we don't actually know where the surface is yet, because maybe it hasn't been generated
			// but we do know that the surface has to be at least at sea level,
			// so let's go with that for now and hope for the best
			return getWorldProvider().getSeaLevel();
		}
	}

	@Override
	public boolean getAreLevelsEmpty( int minBlockY, int maxBlockY )
	{
		int minCubeY = Coords.blockToCube( minBlockY );
		int maxCubeY = Coords.blockToCube( maxBlockY );
		for( int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++ )
		{
			Cube cube = m_cubes.get( cubeY );
			if( cube != null && cube.hasBlocks() )
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean canBlockSeeTheSky( int localX, int blockY, int localZ )
	{
		Integer skylightBlockY = getSkylightBlockY( localX, localZ );
		if( skylightBlockY == null )
		{
			return true;
		}
		return blockY >= skylightBlockY;
	}

	public Integer getSkylightBlockY( int localX, int localZ )
	{
		// NOTE: a "skylight" block is the transparent block that is directly one block above the top non-transparent block
		Integer topBlockY = getLightIndex().getTopNonTransparentBlockY( localX, localZ );
		if( topBlockY != null )
		{
			return topBlockY + 1;
		}
		return null;
	}

	@Override
	@Deprecated // don't use this! It's only here because vanilla needs it, but we need to be hacky about it
	public int getHeightValue( int localX, int localZ )
	{
		// NOTE: the "height value" here is the height of the transparent block on top of the highest non-transparent block

		Integer skylightBlockY = getSkylightBlockY( localX, localZ );
		if( skylightBlockY == null )
		{
			// PANIC!
			// this column doesn't have any blocks in it that aren't air!
			// but we can't return null here because vanilla code expects there to be a surface down there somewhere
			// we don't actually know where the surface is yet, because maybe it hasn't been generated
			// but we do know that the surface has to be at least at sea level,
			// so let's go with that for now and hope for the best
			skylightBlockY = getWorldProvider().getSeaLevel() + 1;
		}
		return skylightBlockY;
	}

	@Override //  getOpacity
	public int func_150808_b( int localX, int blockY, int localZ )
	{
		return getLightIndex().getOpacity( localX, blockY, localZ );
	}

	public Iterable<Entity> entities()
	{
		return m_entities.entities();
	}

	@Override
	public void addEntity( Entity entity )
	{
		//check whether client has loaded column
		/*if (this.isEmpty() && this.worldObj.isClient)
		{
			return;
		}*/
		int cubeY = Coords.getCubeYForEntity( entity );
		// pass off to the cube
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			cube.addEntity( entity );
		}
		else
		{
			// entities don't have to be in chunks, just add it directly to the column
			entity.addedToChunk = true;
			entity.chunkCoordX = xPosition;
			entity.chunkCoordY = cubeY;
			entity.chunkCoordZ = zPosition;

			m_entities.add( entity );
			isModified = true;
		}
	}

	@Override
	public void removeEntity( Entity entity )
	{
		removeEntityAtIndex( entity, entity.chunkCoordY );
	}

	@Override
	public void removeEntityAtIndex( Entity entity, int cubeY )
	{
		if( !entity.addedToChunk )
		{
			return;
		}
		boolean wasRemoved = false;
		// pass off to the cube
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			wasRemoved = cube.removeEntity( entity );
		}
		if(!wasRemoved)
		{
			if (m_entities.remove( entity ))
			{
				entity.addedToChunk = false;
				isModified = true;
				wasRemoved = true;
			}
		}
		if (!wasRemoved && cube != null && !this.isEmpty())
		{
			entity.addedToChunk = false;
			log.warn( String.format( "%s Tried to remove entity %s from column (%d,%d), but it was not there. Entity thinks it's in cube (%d,%d,%d)",
				worldObj.isClient ? "CLIENT" : "SERVER",
				entity.getClass().getName(),
				xPosition, zPosition,
				entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ
			) );
		}
	}

	@Override
	@SuppressWarnings(
	{
		"unchecked", "rawtypes"
	})
	public void getEntitiesOfTypeWithinAAAB( Class c, AxisAlignedBB queryBox, List out, IEntitySelector selector )
	{
		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube( MathHelper.floor_double( queryBox.minY - 2 ) );
		int maxCubeY = Coords.blockToCube( MathHelper.floor_double( queryBox.maxY + 2 ) );
		for( Cube cube: getCubes( minCubeY, maxCubeY ) )
		{
			cube.getEntities( (List<Entity>)out, c, queryBox, selector );
		}

		// check the column too
		m_entities.getEntities( out, c, queryBox, selector );
	}

	@Override
	@SuppressWarnings(
	{
		"unchecked", "rawtypes"
	})
	public void getEntitiesWithinAABBForEntity( Entity excludedEntity, AxisAlignedBB queryBox, List out, IEntitySelector selector )
	{
		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube( MathHelper.floor_double( queryBox.minY - 2 ) );
		int maxCubeY = Coords.blockToCube( MathHelper.floor_double( queryBox.maxY + 2 ) );
		for( Cube cube: getCubes( minCubeY, maxCubeY ) )
		{
			cube.getEntitiesExcept( (List<Entity>)out, excludedEntity, queryBox, selector );
		}

		// check the column too
		m_entities.getEntitiesExcept( out, excludedEntity, queryBox, selector );
	}

	@Override //      getTileEntity
	public TileEntity func_150806_e( int localX, int blockY, int localZ )
	{
		// pass off to the cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cube.getTileEntity( localX, localY, localZ );
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addTileEntity( TileEntity tileEntity )
	{
		// NOTE: this is called only by the chunk loader

		int blockX = tileEntity.field_145851_c;
		int blockY = tileEntity.field_145848_d;
		int blockZ = tileEntity.field_145849_e;

		// pass off to the cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localX = Coords.blockToLocal( blockX );
			int localY = Coords.blockToLocal( blockY );
			int localZ = Coords.blockToLocal( blockZ );
			cube.addTileEntity( localX, localY, localZ, tileEntity );
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

		// pass off to the cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			cube.addTileEntity( localX, localY, localZ, tileEntity );
		}
		else
		{
			log.warn( String.format( "No cube at (%d,%d,%d) to add tile entity (block %d,%d,%d)!",
				xPosition, cubeY, zPosition,
				tileEntity.field_145851_c, blockY, tileEntity.field_145849_e
			) );
		}
	}

	@Override
	public void removeTileEntity( int localX, int blockY, int localZ )
	{
		if( isChunkLoaded )
		{
			// pass off to the cube
			int cubeY = Coords.blockToCube( blockY );
			Cube cube = m_cubes.get( cubeY );
			if( cube != null )
			{
				int localY = Coords.blockToLocal( blockY );
				cube.removeTileEntity( localX, localY, localZ );
			}
		}
	}

	@Override
	public void onChunkLoad()
	{
		// tell the world about entities
		for( Entity entity : m_entities.entities() )
		{
			entity.onChunkLoad();
		}
		worldObj.addLoadedEntities( m_entities.entities() );

		isChunkLoaded = true;
	}

	@Override
	public void onChunkUnload()
	{
		isChunkLoaded = false;

		for( Cube cube: m_cubes.values() )
		{
			for( TileEntity tileEntity: cube.tileEntities() )
			{
				this.worldObj.func_147457_a( tileEntity );
			}
			this.worldObj.unloadEntities(cube.getEntityContainer().entities());
		}

        this.worldObj.unloadEntities(this.m_entities.entities());
	}

	public byte[] encode( boolean isFirstTime )
		throws IOException
	{
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( buf );
		// NOTE: there's no need to do compression here. This output is compressed later

		// how many cubes are we sending?
		int numCubes = 0;
		for( @SuppressWarnings("unused") Cube cube: cubes() )
		{
			numCubes++;
		}
		out.writeShort( numCubes );

		// send the actual cube data
		for( Cube cube: cubes() )
		{
			// signal we're sending this cube
			out.writeInt( cube.getY() );

			out.writeBoolean( cube.isEmpty() );
			if( !cube.isEmpty() )
			{
				ExtendedBlockStorage storage = cube.getStorage();

				// 1. block IDs, low bits
				out.write( storage.getBlockLSBArray() );

				// 2. block IDs, high bits
				if( storage.getBlockMSBArray() != null )
				{
					out.writeByte( 1 );
					out.write( storage.getBlockMSBArray().data );
				}
				else
				{
					// signal we're not sending this data
					out.writeByte( 0 );
				}

				// 3. metadata
				out.write( storage.getMetadataArray().data );

				// 4. block light
				out.write( storage.getBlocklightArray().data );

				if( !worldObj.provider.hasNoSky )
				{
					// 5. sky light
					out.write( storage.getSkylightArray().data );
				}
			}
		}

		if( isFirstTime )
		{
			// 6. biomes
			out.write( getBiomeArray() );
		}

		// 7. light index
		getLightIndex().writeData( out );

		out.close();
		return buf.toByteArray();
	}

	@Override
	public void fillChunk( byte[] data, int segmentsToCopyBitFlags, int blockMSBToCopyBitFlags, boolean isFirstTime )
	{
		// NOTE: this is called on the client when it receives chunk data from the server
		ByteArrayInputStream buf = new ByteArrayInputStream( data );
		DataInputStream in = new DataInputStream( buf );

		try
		{
			// how many cubes are we reading?
			int numCubes = in.readUnsignedShort();
			for( int i = 0; i < numCubes; i++ )
			{
				int cubeY = in.readInt();
				boolean isEmpty = in.readBoolean();
				Cube cube = getOrCreateCube( cubeY, false );

				// if the cube came from the server, it must be live
				cube.setGeneratorStage( GeneratorStage.getLastStage() );

				// is the cube empty?
				cube.setEmpty( isEmpty );

				if( !isEmpty )
				{
					ExtendedBlockStorage storage = cube.getStorage();

					// 1. block IDs, low bits
					in.read( storage.getBlockLSBArray() );

					// 2. block IDs, high bits
					boolean isHighBitsAttached = in.readByte() != 0;
					if( isHighBitsAttached )
					{
						if( storage.getBlockMSBArray() == null )
						{
							storage.createBlockMSBArray();
						}
						in.read( storage.getBlockMSBArray().data );
					}

					// 3. metadata
					in.read( storage.getMetadataArray().data );

					// 4. block light
					in.read( storage.getBlocklightArray().data );

					if( !worldObj.provider.hasNoSky )
					{
						// 5. sky light
						in.read( storage.getSkylightArray().data );
					}

					// clean up invalid blocks
					storage.removeInvalidBlocks();
				}

				// flag cube for render update
				cube.markForRenderUpdate();
			}

			if( isFirstTime )
			{
				// 6. biomes
				in.read( getBiomeArray() );
			}

			// 7. light index
			getLightIndex().readData( in );

			in.close();
			
			// 8. clean up column if we have just emptied all its cubes
			if ( worldObj.isClient){
				boolean unload = true;
				Iterator<Cube> c = cubes().iterator();
				while (c.hasNext()){
					if (!c.next().isEmpty()){
						unload = false;
						break;
					}
				}
				if (unload){
					CubeProviderClient provider = (CubeProviderClient) worldObj.getChunkProvider();
					provider.unloadChunk(this.getX(), this.getZ());
				}
			}
		}
		catch( IOException ex )
		{
			log.error( String.format( "Unable to read data for column (%d,%d)", xPosition, zPosition ), ex );
		}

		// update lighting flags
		isTerrainPopulated = true;

		// update tile entities in each chunk
		for( Cube cube: m_cubes.values() )
		{
			for( TileEntity tileEntity: cube.tileEntities() )
			{
				tileEntity.updateContainingBlockInfo();
			}
		}
	}

	/**
	 * This method retrieves the biome at a set of coordinates
	 */
	@Override
	public CubeBiomeGenBase getBiomeGenForWorldCoords( int xRel, int zRel, WorldChunkManager worldChunkManager ){
		int biomeID = this.getBiomeArray()[zRel << 4 | xRel] & 255;

		WorldColumnManager worldColumnManager = (WorldColumnManager)worldChunkManager;

		if( biomeID == 255 )
		{
			CubeBiomeGenBase var5 = worldColumnManager.getBiomeGenAt( (this.xPosition << 4) + xRel, (this.zPosition << 4) + zRel );
			biomeID = var5.biomeID;
			this.getBiomeArray()[zRel << 4 | xRel] = (byte)(biomeID & 255);
		}

		return (CubeBiomeGenBase)(CubeBiomeGenBase.func_150568_d( biomeID ) == null ? CubeBiomeGenBase.plains : CubeBiomeGenBase.func_150568_d( biomeID ));	
	}
	
	/**
	 * Returns an array containing a 16x16 mapping on the X/Z of block positions in this Chunk to biome IDs.
	 */
	@Override
	public byte[] getBiomeArray()
	{
		return this.columnBlockBiomeArray;
	}

	/**
	 * Accepts a 256-entry array that contains a 16x16 mapping on the X/Z plane of block positions in this Chunk to
	 * biome IDs.
	 */
	@Override
	public void setBiomeArray( byte[] par1ArrayOfByte )
	{
		this.columnBlockBiomeArray = par1ArrayOfByte;
	}

	@Override //        isActive
	public boolean func_150802_k()
	{
		boolean isAnyCubeLive = false;
		for( Cube cube: m_cubes.values() )
		{
			isAnyCubeLive |= cube.getGeneratorStage().isLastStage();
		}

		return field_150815_m && isTerrainPopulated && isAnyCubeLive;
	}

	@Override //         tick
	public void func_150804_b( boolean tryToTickFaster )
	{
		// isTicked
		field_150815_m = true;

		// don't need to do anything else here
	}

	@Override
	public void generateSkylightMap()
	{
		// don't call this, use the lighting manager
		throw new UnsupportedOperationException();
	}

	public void resetPrecipitationHeight()
	{
		// init the rain map to -999, which is a kind of null value
		// this array is actually a cache
		// values will be calculated by the getter
		for( int localX = 0; localX < 16; localX++ )
		{
			for( int localZ = 0; localZ < 16; localZ++ )
			{
				int xzCoord = localX | localZ << 4;
				precipitationHeightMap[xzCoord] = -999;
			}
		}
	}

	@Override
	public int getPrecipitationHeight( int localX, int localZ )
	{
		// UNDONE: update this calculation to use better data structures
		int xzCoord = localZ << 4 | localX;
		int height = this.precipitationHeightMap[xzCoord];
		if( height == -999 )
		{
			// UNDONE: compute a new rain height
			/* look over the blocks in the top filled cube (if one exists) and do this:
			 int maxBlockY = getTopFilledSegment() + 15;
			 int minBlockY = Coords.cubeToMinBlock( getBottomCubeY() );
			 Block block = cube.getBlock( ... );
			 Material material = block.getMaterial();
			 if( material.blocksMovement() || material.isLiquid() )
			 {
			 height = maxBlockY + 1;
			 }
			 */

			// TEMP: just rain down to the sea
			precipitationHeightMap[xzCoord] = getWorldProvider().getSeaLevel();
			height = getWorldProvider().getSeaLevel();
		}
		
		return height;
	}

	@Override
	public int getBlockLightValue( int localX, int blockY, int localZ, int skylightSubtracted )
	{
		// NOTE: this is called by WorldRenderers

		// pass off to cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			int light = cube.getBlockLightValue( localX, localY, localZ, skylightSubtracted );

			if( light > 0 )
			{
				isLit = true;
			}

			return light;
		}

		// defaults
		if( !worldObj.provider.hasNoSky && skylightSubtracted < EnumSkyBlock.Sky.defaultLightValue )
		{
			return EnumSkyBlock.Sky.defaultLightValue - skylightSubtracted;
		}
		return 0;
	}

	@Override
	public int getSavedLightValue( EnumSkyBlock lightType, int localX, int blockY, int localZ )
	{
		// NOTE: this is the light function that is called by the rendering code on client

		// pass off to cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			return cube.getLightValue( lightType, localX, localY, localZ );
		}

		// there's no cube, rely on defaults
		if( canBlockSeeTheSky( localX, blockY, localZ ) )
		{
			return lightType.defaultLightValue;
		}
		else
		{
			return 0;
		}
	}

	@Override
	public void setLightValue( EnumSkyBlock lightType, int localX, int blockY, int localZ, int light )
	{
		// pass off to cube
		int cubeY = Coords.blockToCube( blockY );
		Cube cube = m_cubes.get( cubeY );
		if( cube != null )
		{
			int localY = Coords.blockToLocal( blockY );
			cube.setLightValue( lightType, localX, localY, localZ, light );

			isModified = true;
		}
	}

	protected List<RangeInt> getRanges( Iterable<Integer> yValues )
	{
		// compute a kind of run-length encoding on the cube y-values
		List<RangeInt> ranges = new ArrayList<RangeInt>();
		Integer start = null;
		Integer stop = null;
		for( int cubeY: yValues )
		{
			if( start == null )
			{
				// start a new range
				start = cubeY;
				stop = cubeY;
			}
			else if( cubeY == stop + 1 )
			{
				// extend the range
				stop = cubeY;
			}
			else
			{
				// end the range
				ranges.add( new RangeInt( start, stop ) );

				// start a new range
				start = cubeY;
				stop = cubeY;
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
	public void resetRelightChecks()
	{
		m_roundRobinLightUpdatePointer = 0;
		m_roundRobinCubes.clear();
		m_roundRobinCubes.addAll( m_cubes.values() );
	}

	@Override // doSomeRoundRobinLightUpdates
	public void enqueueRelightChecks()
	{
		worldObj.theProfiler.startSection( "roundRobinRelight" );

		if( m_roundRobinCubes.isEmpty() )
		{
			resetRelightChecks();
		}

		// we get just a few updates this time
		for( int i = 0; i < 2; i++ )
		{
			// once we've checked all the blocks, stop checking
			int maxPointer = 16 * 16 * m_roundRobinCubes.size();
			if( m_roundRobinLightUpdatePointer >= maxPointer )
			{
				break;
			}

			// get this update's arguments
			int cubeIndex = Bits.unpackUnsigned( m_roundRobinLightUpdatePointer, 4, 8 );
			int localX = Bits.unpackUnsigned( m_roundRobinLightUpdatePointer, 4, 4 );
			int localZ = Bits.unpackUnsigned( m_roundRobinLightUpdatePointer, 4, 0 );

			// advance to the next block
			// this pointer advances over segment block columns
			// starting from the block columns in the bottom segment and moving upwards
			m_roundRobinLightUpdatePointer++;

			// get the cube that was pointed to
			Cube cube = m_roundRobinCubes.get( cubeIndex );

			int blockX = Coords.localToBlock( xPosition, localX );
			int blockZ = Coords.localToBlock( zPosition, localZ );

			// for each block in this segment block column...
			for( int localY = 0; localY < 16; ++localY )
			{
				if( cube.getBlock( localX, localY, localZ ).getMaterial() == Material.air )
				{
					int blockY = Coords.localToBlock( cubeIndex, localY );

					// if there's a light source next to this block, update the light source
					if( worldObj.getBlock( blockX, blockY - 1, blockZ ).getLightValue() > 0 )
					{
						worldObj.func_147451_t( blockX, blockY - 1, blockZ );
					}
					if( worldObj.getBlock( blockX, blockY + 1, blockZ ).getLightValue() > 0 )
					{
						worldObj.func_147451_t( blockX, blockY + 1, blockZ );
					}
					if( worldObj.getBlock( blockX - 1, blockY, blockZ ).getLightValue() > 0 )
					{
						worldObj.func_147451_t( blockX - 1, blockY, blockZ );
					}
					if( worldObj.getBlock( blockX + 1, blockY, blockZ ).getLightValue() > 0 )
					{
						worldObj.func_147451_t( blockX + 1, blockY, blockZ );
					}
					if( worldObj.getBlock( blockX, blockY, blockZ - 1 ).getLightValue() > 0 )
					{
						worldObj.func_147451_t( blockX, blockY, blockZ - 1 );
					}
					if( worldObj.getBlock( blockX, blockY, blockZ + 1 ).getLightValue() > 0 )
					{
						worldObj.func_147451_t( blockX, blockY, blockZ + 1 );
					}

					// then update this block
					worldObj.func_147451_t( blockX, blockY, blockZ );
				}
			}
		}

		worldObj.theProfiler.endSection();
	}

	@Override // populateLighting
	public void func_150809_p()
	{
		// don't use this, use the new lighting manager
		throw new UnsupportedOperationException();
	}

	public void doRandomTicks()
	{
		if( isEmpty() )
		{
			return;
		}

		for( Cube cube: m_cubes.values() )
		{
			cube.doRandomTicks();
		}
	}
}
