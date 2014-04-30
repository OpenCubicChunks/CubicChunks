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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;

import cuchaz.cubicChunks.client.CubeWorldClient;
import cuchaz.cubicChunks.server.CubePlayerManager;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.world.Column;
import cuchaz.magicMojoModLoader.api.Mod;
import cuchaz.magicMojoModLoader.api.ModMetadata;
import cuchaz.magicMojoModLoader.api.Version;
import cuchaz.magicMojoModLoader.api.events.ClassOverrideEvent;
import cuchaz.magicMojoModLoader.api.events.EncodeChunkEvent;
import cuchaz.magicMojoModLoader.api.events.EntityPlayerMPUpdateEvent;

public class TallWorldsMod implements Mod
{
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
				LogManager.getLogger().error( String.format( "Unable to encode data for column (%d,%d)", column.xPosition, column.zPosition ), ex );
			}
		}
	}
	
	public void handleEvent( EntityPlayerMPUpdateEvent event )
	{
		EntityPlayerMP player = event.getPlayer();
		WorldServer world = (WorldServer)player.theItemInWorldManager.theWorld;
		CubePlayerManager playerManager = (CubePlayerManager)world.getPlayerManager();
		playerManager.onPlayerUpdate( player );
	}
}
