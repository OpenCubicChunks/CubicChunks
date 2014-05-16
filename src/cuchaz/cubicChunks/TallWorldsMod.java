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

import java.io.IOException;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.client.CubeWorldClient;
import cuchaz.cubicChunks.server.CubePlayerManager;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.biome.WorldColumnManager;
import cuchaz.magicMojoModLoader.api.Mod;
import cuchaz.magicMojoModLoader.api.ModMetadata;
import cuchaz.magicMojoModLoader.api.Version;
import cuchaz.magicMojoModLoader.api.events.BuildSizeEvent;
import cuchaz.magicMojoModLoader.api.events.ClassOverrideEvent;
import cuchaz.magicMojoModLoader.api.events.EncodeChunkEvent;
import cuchaz.magicMojoModLoader.api.events.EntityPlayerMPUpdateEvent;
import cuchaz.magicMojoModLoader.api.events.UpdateRenderPositionEvent;

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
}
