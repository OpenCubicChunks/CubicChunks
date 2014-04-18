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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.google.common.collect.Maps;

public class CubicChunkPlayerManager extends PlayerManager
{
	private static class PlayerInfo
	{
		public EntityPlayerMP player;
		public Set<Long> watchedAddresses;
		public LinkedList<CubicChunk> outgoingCubicChunks;
		public int lastBlockX;
		public int lastBlockY;
		public int lastBlockZ;
		
		public PlayerInfo( EntityPlayerMP player )
		{
			this.player = player;
			this.watchedAddresses = new TreeSet<Long>();
			this.outgoingCubicChunks = new LinkedList<CubicChunk>();
			this.lastBlockX = MathHelper.floor_double( player.posZ );
			this.lastBlockY = MathHelper.floor_double( player.posY );
			this.lastBlockZ = MathHelper.floor_double( player.posZ );
		}
		
		public void sortOutgoingCubicChunks( )
		{
			// get the player chunk position
			final int chunkX = Coords.blockToChunk( MathHelper.floor_double( player.posX ) );
			final int chunkY = Coords.blockToChunk( MathHelper.floor_double( player.posY ) );
			final int chunkZ = Coords.blockToChunk( MathHelper.floor_double( player.posZ ) );
			
			// sort cubic chunks so they load radially away from the player
			Collections.sort( outgoingCubicChunks, new Comparator<CubicChunk>( )
			{
				@Override
				public int compare( CubicChunk a, CubicChunk b )
				{
					return getManhattanDist( a ) - getManhattanDist( b );
				}
				
				private int getManhattanDist( CubicChunk cubicChunk )
				{
					int dx = cubicChunk.getX() - chunkX;
					int dy = cubicChunk.getY() - chunkY;
					int dz = cubicChunk.getZ() - chunkZ;
					return dx + dy + dz;
				}
			} );
		}
	}
	
	private WorldServer m_worldServer;
	private int m_viewDistance;
	private TreeMap<Long,CubicChunkWatcher> m_watchers;
	private TreeMap<Integer,PlayerInfo> m_players;
	
	public CubicChunkPlayerManager( WorldServer worldServer, int viewDistance )
	{
		super( worldServer, viewDistance );
		
		m_worldServer = worldServer;
		m_viewDistance = viewDistance;
		m_watchers = Maps.newTreeMap();
		m_players = Maps.newTreeMap();
	}
	
	public void addPlayer( EntityPlayerMP player )
	{
		int chunkX = Coords.blockToChunk( MathHelper.floor_double( player.posX ) );
		int chunkY = Coords.blockToChunk( MathHelper.floor_double( player.posY ) );
		int chunkZ = Coords.blockToChunk( MathHelper.floor_double( player.posZ ) );
		
		// make new player info
		PlayerInfo info = new PlayerInfo( player );
		m_players.put( player.getEntityId(), info );
		
		// add player to watchers and collect the cubic chunks to send over
		List<Long> addresses = new ArrayList<Long>();
		EllipsoidalCubicChunkSelector.getAddresses( addresses, chunkX, chunkY, chunkZ, m_viewDistance );
		for( long address : addresses )
		{
			CubicChunkWatcher watcher = getOrCreateWatcher( address );
			watcher.addPlayer( player );
			info.watchedAddresses.add( address );
			info.outgoingCubicChunks.add( watcher.getCubicChunk() );
		}
	}
	
	public void removePlayer( EntityPlayerMP player )
	{
		int chunkX = Coords.blockToChunk( MathHelper.floor_double( player.posX ) );
		int chunkY = Coords.blockToChunk( MathHelper.floor_double( player.posY ) );
		int chunkZ = Coords.blockToChunk( MathHelper.floor_double( player.posZ ) );
		
		// remove player from all its cubic chunks
		List<Long> addresses = new ArrayList<Long>();
		EllipsoidalCubicChunkSelector.getAddresses( addresses, chunkX, chunkY, chunkZ, m_viewDistance );
		for( long address : addresses )
		{
			CubicChunkWatcher watcher = getWatcher( address );
			if( watcher != null )
			{
				watcher.removePlayer( player );
			}
			
			// cleanup empty watchers and cubic chunks
			if( !watcher.hasPlayers() )
			{
				m_watchers.remove( address );
				getCubicChunkProvider().unloadCubicChunkIfNotNearSpawn( watcher.getCubicChunk() );
			}
		}
		
		// remove the info
		m_players.remove( player.getEntityId() );
	}
	
	public void updatePlayerInstances( ) // aka tick()
	{
		// responsibilities:
		//    update chunk properties
		//    send chunk updates to players
		
		for( CubicChunkWatcher watcher : m_watchers.values() )
		{
			watcher.sendUpdates();
			watcher.tick();
		}
		
		// did all the players leave an alternate dimension?
		if( m_players.isEmpty() && !m_worldServer.provider.canRespawnHere() )
		{
			// unload everything
			getCubicChunkProvider().unloadAllChunks();
		}
	}
	
	//     markBlockForUpdate
	public void func_151250_a( int blockX, int blockY, int blockZ )
	{
		// get the watcher
		int chunkX = Coords.blockToChunk( blockX );
		int chunkY = Coords.blockToChunk( blockY );
		int chunkZ = Coords.blockToChunk( blockZ );
		CubicChunkWatcher watcher = getWatcher( chunkX, chunkY, chunkZ );
		if( watcher == null )
		{
			return;
		}
		
		// pass off to watcher
		int localX = Coords.blockToLocal( blockX );
		int localY = Coords.blockToLocal( blockY );
		int localZ = Coords.blockToLocal( blockZ );
		watcher.setDirtyBlock( localX, localY, localZ );
	}
	
	public void updateMountedMovingPlayer( EntityPlayerMP player )
	{
		// the player moved
		// if the player moved into a new chunk, update which chunks the player needs to know about
		// then update the list of chunks that need to be sent to the client
		
		// get the player info
		PlayerInfo info = m_players.get( player.getEntityId() );
		if( info == null )
		{
			return;
		}
		
		// how far did the player move?
		int newBlockX = MathHelper.floor_double( player.posX );
		int newBlockY = MathHelper.floor_double( player.posY );
		int newBlockZ = MathHelper.floor_double( player.posZ );
		int manhattanDistance = Math.abs( newBlockX - info.lastBlockX )
				+ Math.abs( newBlockY - info.lastBlockY )
				+ Math.abs( newBlockZ - info.lastBlockZ );
		if( manhattanDistance < 8 )
		{
			return;
		}
		
		// did the player move into a new cubic chunk?
		int newChunkX = Coords.blockToChunk( newBlockX );
		int newChunkY = Coords.blockToChunk( newBlockY );
		int newChunkZ = Coords.blockToChunk( newBlockZ );
		int oldChunkX = Coords.blockToChunk( info.lastBlockX );
		int oldChunkY = Coords.blockToChunk( info.lastBlockY );
		int oldChunkZ = Coords.blockToChunk( info.lastBlockZ );
		if( newChunkX == oldChunkX && newChunkY == oldChunkY && newChunkZ == oldChunkZ )
		{
			return;
		}
		
		// find out which cubic chunks have been added to view, and which ones have been removed
		Set<Long> oldAddresses = new TreeSet<Long>();
		EllipsoidalCubicChunkSelector.getAddresses( oldAddresses, oldChunkX, oldChunkY, oldChunkZ, m_viewDistance );
		Set<Long> newAddresses = new TreeSet<Long>();
		EllipsoidalCubicChunkSelector.getAddresses( newAddresses, newChunkX, newChunkY, newChunkZ, m_viewDistance );
		Set<Long> intersection = new TreeSet<Long>( newAddresses );
		intersection.retainAll( oldAddresses );
		oldAddresses.removeAll( intersection );
		newAddresses.removeAll( intersection );
		
		// add to new watchers
		for( long address : newAddresses )
		{
			CubicChunkWatcher watcher = getOrCreateWatcher( address );
			watcher.addPlayer( player );
			info.outgoingCubicChunks.add( watcher.getCubicChunk() );
		}
		
		// remove from old watchers
		for( long address : oldAddresses )
		{
			CubicChunkWatcher watcher = getWatcher( address );
			if( watcher != null )
			{
				watcher.removePlayer( player );
			}
		}
		
		// update player info
		info.lastBlockX = newBlockX;
		info.lastBlockY = newBlockY;
		info.lastBlockZ = newBlockZ;
	}
	
	public boolean isPlayerWatchingChunk( EntityPlayerMP player, int chunkX, int chunkZ )
	{
		// get the info
		PlayerInfo info = m_players.get( player.getEntityId() );
		if( info == null )
		{
			return false;
		}
		
		// check the player's watched addresses
		for( long address : info.watchedAddresses )
		{
			int x = AddressTools.getX( address );
			int z = AddressTools.getZ( address );
			if( x == chunkX && z == chunkZ )
			{
				return true;
			}
		}
		return false;
	}
	
	public void onPlayerUpdate( EntityPlayerMP player )
	{
		// this method flushes outgoing chunks to the player
		
		// get the outgoing chunks
		PlayerInfo info = m_players.get( player.getEntityId() );
		if( info == null || info.outgoingCubicChunks.isEmpty() )
		{
			return;
		}
		info.sortOutgoingCubicChunks();
		
		// pull off enough cubic chunks from the queue to fit in a packet
		final int MaxCubicChunksToSend = 20;
		List<CubicChunk> cubicChunksToSend = new ArrayList<CubicChunk>();
		List<TileEntity> tileEntitiesToSend = new ArrayList<TileEntity>();
		Iterator<CubicChunk> iter = info.outgoingCubicChunks.iterator();
		while( iter.hasNext() && cubicChunksToSend.size() < MaxCubicChunksToSend )
		{
			CubicChunk cubicChunk = iter.next();
			if( cubicChunk.getColumn().func_150802_k() )
			{
				// add this cubic chunk to the send buffer
				cubicChunksToSend.add( cubicChunk );
				iter.remove();
				
				// add tile entities too
				for( TileEntity tileEntity : cubicChunk.tileEntities() )
				{
					tileEntitiesToSend.add( tileEntity );
				}
			}
		}
		
		if( cubicChunksToSend.isEmpty() )
		{
			return;
		}
		
		// group the cubic chunks into column views
		Map<Long,ColumnView> views = new TreeMap<Long,ColumnView>();
		for( CubicChunk cubicChunk : cubicChunksToSend )
		{
			// is there a column view for this cubic chunk?
			long columnAddress = AddressTools.getAddress( 0, cubicChunk.getX(), 0, cubicChunk.getY() );
			ColumnView view = views.get( columnAddress );
			if( view == null )
			{
				view = new ColumnView( cubicChunk.getColumn() );
				views.put( columnAddress, view );
			}
			
			view.addCubicChunkToView( cubicChunk );
		}
		List<Chunk> columnsToSend = new ArrayList<Chunk>( views.values() );
		
		// send the cubic chunk data
		player.playerNetServerHandler.sendPacket( new S26PacketMapChunkBulk( columnsToSend ) );
		
		// send tile entity data
		for( TileEntity tileEntity : tileEntitiesToSend )
		{
			Packet packet = tileEntity.getDescriptionPacket();
			if( packet != null )
			{
				player.playerNetServerHandler.sendPacket( packet );
			}
		}
		
		// watch entities on the chunks we just sent
		for( Chunk chunk : columnsToSend )
		{
			m_worldServer.getEntityTracker().func_85172_a( player, chunk );
		}
	}
	
	private CubicChunkProviderServer getCubicChunkProvider( )
	{
		return (CubicChunkProviderServer)m_worldServer.theChunkProviderServer;
	}
	
	private CubicChunkWatcher getWatcher( int chunkX, int chunkY, int chunkZ )
	{
		return getWatcher( AddressTools.getAddress( 0, chunkX, chunkY, chunkZ ) );
	}
	
	private CubicChunkWatcher getWatcher( long address )
	{
		return m_watchers.get( address );
	}
	
	private CubicChunkWatcher getOrCreateWatcher( int chunkX, int chunkY, int chunkZ )
	{
		return getOrCreateWatcher( AddressTools.getAddress( 0, chunkX, chunkY, chunkZ ) );
	}
	
	private CubicChunkWatcher getOrCreateWatcher( long address )
	{
		CubicChunkWatcher watcher = m_watchers.get( address );
		if( watcher == null )
		{
			// get the cubic chunk
			int chunkX = AddressTools.getX( address );
			int chunkY = AddressTools.getY( address );
			int chunkZ = AddressTools.getZ( address );
			CubicChunk cubicChunk = getCubicChunkProvider().loadCubicChunk( chunkX, chunkY, chunkZ );
			
			// make a new watcher
			watcher = new CubicChunkWatcher( cubicChunk );
			m_watchers.put( address, watcher );
		}
		return watcher;
	}
}
