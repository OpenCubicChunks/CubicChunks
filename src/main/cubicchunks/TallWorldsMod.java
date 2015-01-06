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
package cubicchunks;

import java.io.IOException;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cubicchunks.client.CubeWorldClient;
import cubicchunks.generator.GeneratorPipeline;
import cubicchunks.server.CubePlayerManager;
import cubicchunks.server.CubeWorldServer;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.world.Column;
import cuchaz.magicMojoModLoader.api.Mod;
import cuchaz.magicMojoModLoader.api.ModMetadata;
import cuchaz.magicMojoModLoader.api.Version;
import cuchaz.magicMojoModLoader.api.events.BuildSizeEvent;
import cuchaz.magicMojoModLoader.api.events.CheckChunksExistForEntityEvent;
import cuchaz.magicMojoModLoader.api.events.ClassOverrideEvent;
import cuchaz.magicMojoModLoader.api.events.EncodeChunkEvent;
import cuchaz.magicMojoModLoader.api.events.EntityPlayerMPUpdateEvent;
import cuchaz.magicMojoModLoader.api.events.InitialChunkLoadEvent;
import cuchaz.magicMojoModLoader.api.events.RandomChunkBlockYEvent;
import cuchaz.magicMojoModLoader.api.events.UpdateRenderPositionEvent;
import cuchaz.magicMojoModLoader.api.events.VoidFogRangeEvent;
import cuchaz.magicMojoModLoader.api.events.WorldProviderEvent;
import cuchaz.magicMojoModLoader.util.Util;

public class TallWorldsMod implements Mod
{
	private static final Logger log = LogManager.getLogger();
	
	// define one instance of the metadata
	private static final ModMetadata m_meta;
	static
	{
		m_meta = new ModMetadata();
		m_meta.setId( "tall-worlds" );
		m_meta.setVersion( new Version( "0.1 beta" ) );
		m_meta.setName( "Tall Worlds" );
	}
	
	@Override
	public ModMetadata getMetadata( )
	{
		return m_meta;
	}
	
	public void handleEvent( ClassOverrideEvent event )
	{
		if( event.getOldClassName().equals( "net.minecraft.client.multiplayer.WorldClient" ) )
		{
			event.setNewClassName( CubeWorldClient.class.getName() );
		}
		else if( event.getOldClassName().equals( "net.minecraft.world.WorldServer" ) )
		{
			event.setNewClassName( CubeWorldServer.class.getName() );
		}
	}
	
	public void handleEvent( WorldProviderEvent event )
	{
		if( event.getDimension() == 0 ) // surface world
		{
			event.setCustomWorldProvider( new CubeWorldProviderSurface() );
		}
	}
	
	public void handleEvent( EncodeChunkEvent event )
	{
		// check for our chunk instance
		if( event.getChunk() instanceof Column )
		{
			Column column = (Column)event.getChunk();
			
			// encode the column
			try
			{
				byte[] data = column.encode( event.isFirstTime() );
				event.setData( data );
			}
			catch( IOException ex )
			{
				log.error( String.format( "Unable to encode data for column (%d,%d)", column.xPosition, column.zPosition ), ex );
			}
		}
	}
	
	public void handleEvent( BuildSizeEvent event )
	{
		event.setCustomBuildHeight( Coords.cubeToMaxBlock( AddressTools.MaxY ) );
		event.setCustomBuildDepth( Coords.cubeToMinBlock( AddressTools.MinY ) );
		
		log.info( String.format( "Set build height to [%d,%d]", event.getCustomBuildDepth(), event.getCustomBuildHeight() ) );
	}
	
	public void handleEvent( InitialChunkLoadEvent event )
	{
		// get the surface world
		CubeWorldServer worldServer = (CubeWorldServer)event.worldServers().get( 0 );
		
		// load the cubes around the spawn point
		log.info( "Loading cubes for spawn..." );
		final int Distance = 12;
		ChunkCoordinates spawnPoint = worldServer.getSpawnPoint();
		int spawnCubeX = Coords.blockToCube( spawnPoint.posX );
		int spawnCubeY = Coords.blockToCube( spawnPoint.posY );
		int spawnCubeZ = Coords.blockToCube( spawnPoint.posZ );
		for( int cubeX=spawnCubeX-Distance; cubeX<=spawnCubeX+Distance; cubeX++ )
		{
			for( int cubeY=spawnCubeY-Distance; cubeY<=spawnCubeY+Distance; cubeY++ )
			{
				for( int cubeZ=spawnCubeZ-Distance; cubeZ<=spawnCubeZ+Distance; cubeZ++ )
				{
					worldServer.getCubeProvider().loadCubeAndNeighbors( cubeX, cubeY, cubeZ );
				}
			}
		}
		
		// wait for the cubes to be loaded
		GeneratorPipeline pipeline = worldServer.getGeneratorPipeline();
		int numCubesTotal = pipeline.getNumCubes();
		if( numCubesTotal > 0 )
		{
			long timeStart = System.currentTimeMillis();
			log.info( String.format( "Generating %d cubes for spawn at block (%d,%d,%d) cube (%d,%d,%d)...",
				numCubesTotal,
				spawnPoint.posX, spawnPoint.posY, spawnPoint.posZ,
				spawnCubeX, spawnCubeY, spawnCubeZ
			) );
			pipeline.generateAll();
			long timeDiff = System.currentTimeMillis() - timeStart;
			log.info( String.format( "Done in %d ms", timeDiff ) );
		}
	}
	
	public void handleEvent( EntityPlayerMPUpdateEvent event )
	{
		EntityPlayerMP player = event.getPlayer();
		WorldServer world = (WorldServer)player.theItemInWorldManager.theWorld;
		CubePlayerManager playerManager = (CubePlayerManager)world.getPlayerManager();
		playerManager.onPlayerUpdate( player );
	}
	
	public void handleEvent( UpdateRenderPositionEvent event )
	{
		// block position is the position of the viewpoint entity
		int blockX = event.getBlockX();
		int blockY = event.getBlockY();
		int blockZ = event.getBlockZ();
		
		// move back 8 blocks?? (why?)
		blockX -= 8;
		blockY -= 8;
		blockZ -= 8;
		
		int minBlockX = Integer.MAX_VALUE;
		int minBlockY = Integer.MAX_VALUE;
		int minBlockZ = Integer.MAX_VALUE;
		int maxBlockX = Integer.MIN_VALUE;
		int maxBlockY = Integer.MIN_VALUE;
		int maxBlockZ = Integer.MIN_VALUE;
		
		// get view dimensions
		int blockViewDx = event.getRenderCubeDx()*16;
		int blockViewHdx = blockViewDx/2;
		int blockViewDy = event.getRenderCubeDy()*16;
		int blockViewHdy = blockViewDy/2;
		int blockViewDz = event.getRenderCubeDz()*16;
		int blockViewHdz = blockViewDz/2;
		
		for( int renderX=0; renderX<event.getRenderCubeDx(); renderX++ )
		{
			int posBlockX = renderX*16;
			
			// compute parameter of coordinate transformation
			int blockWidthsX = posBlockX + blockViewHdx - blockX;
			if( blockWidthsX < 0 )
			{
				blockWidthsX -= blockViewDx - 1;
			}
			blockWidthsX /= blockViewDx;
			
			// translate by player position
			posBlockX -= blockWidthsX*blockViewDx;
			
			// update bounds
			if( posBlockX < minBlockX )
			{
				minBlockX = posBlockX;
			}
			if( posBlockX > maxBlockX )
			{
				maxBlockX = posBlockX;
			}
			
			for( int renderZ=0; renderZ<event.getRenderCubeDz(); renderZ++ )
			{
				int posBlockZ = renderZ*16;
				
				// compute parameter of coordinate transformation
				int blockWidthsZ = posBlockZ + blockViewHdz - blockZ;
				if( blockWidthsZ < 0 )
				{
					blockWidthsZ -= blockViewDz - 1;
				}
				blockWidthsZ /= blockViewDz;
				
				// translate by player position
				posBlockZ -= blockWidthsZ*blockViewDz;
				
				// update bounds
				if( posBlockZ < minBlockZ )
				{
					minBlockZ = posBlockZ;
				}
				if( posBlockZ > maxBlockZ )
				{
					maxBlockZ = posBlockZ;
				}
				
				for( int renderY=0; renderY<event.getRenderCubeDy(); renderY++ )
				{
					int posBlockY = renderY*16;
					
					// compute parameter of coordinate transformation
					int blockHeightsY = posBlockY + blockViewHdy - blockY;
					if( blockHeightsY < 0 )
					{
						blockHeightsY -= blockViewDy - 1;
					}
					blockHeightsY /= blockViewDy;
					
					// translate by player position
					posBlockY -= blockHeightsY*blockViewDy;
					
					// update bounds
					if( posBlockY < minBlockY )
					{
						minBlockY = posBlockY;
					}
					if( posBlockY > maxBlockY )
					{
						maxBlockY = posBlockY;
					}
					
					// update renderer
					WorldRenderer renderer = event.getRenderer( renderX, renderY, renderZ );
					boolean neededUpdate = renderer.needsUpdate;
					renderer.setPosition( posBlockX, posBlockY, posBlockZ );
					if( !neededUpdate && renderer.needsUpdate )
					{
						event.updateRenderer( renderer );
					}
				}
			}
		}
		
		// save the bounds to the event
		event.setMinBlockX( minBlockX );
		event.setMinBlockY( minBlockY );
		event.setMinBlockZ( minBlockZ );
		event.setMaxBlockX( maxBlockX );
		event.setMaxBlockY( maxBlockY );
		event.setMaxBlockZ( maxBlockZ );
		
		event.setHandled();
	}
	
	public void handleEvent( VoidFogRangeEvent event )
	{
		int min = Coords.cubeToMinBlock( AddressTools.MinY );
		event.setCustomRange( min, min + 1024 );
	}
	
	public void handleEvent( RandomChunkBlockYEvent event )
	{
		if( event.getChunk() instanceof Column )
		{
			Column column = (Column)event.getChunk();
			event.setBlockY( Util.randRange(
				event.getRand(),
				Coords.cubeToMinBlock( column.getBottomCubeY() ),
				Coords.cubeToMaxBlock( column.getTopCubeY() )
			) );
		}
	}
	
	public void handleEvent( CheckChunksExistForEntityEvent event )
	{
		Entity entity = event.getEntity();
		int entityX = MathHelper.floor_double( entity.posX );
        int entityY = MathHelper.floor_double( entity.posY );
		int entityZ = MathHelper.floor_double( entity.posZ );
		
    	event.setChunksExist( entity.worldObj.checkChunksExist(
    		entityX - 32, entityY - 32, entityZ - 32,
    		entityX + 32, entityY + 32, entityZ + 32
    	) );
	}
}
