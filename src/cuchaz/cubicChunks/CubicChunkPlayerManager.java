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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
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
		
		public PlayerInfo( EntityPlayerMP player )
		{
			this.player = player;
			this.watchedAddresses = new TreeSet<Long>();
			this.outgoingCubicChunks = new LinkedList<CubicChunk>();
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
		
		// save the last checked position for the player
		player.managedPosX = player.posX;
		player.managedPosZ = player.posZ;
		
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
			// UNDONE: send (or collect?) cubic chunk updates
			
			watcher.tick();
		}
		
		// did all the players leave an alternate dimension?
		if( m_players.isEmpty() && !m_worldServer.provider.canRespawnHere() )
		{
			// unload everything
			getCubicChunkProvider().unloadAllChunks();
		}
		
		// NOTE: these things are done in sub-method calls
		
        // send single block updates
        var1 = this.chunkLocation.chunkXPos * 16 + (this.field_151254_d[0] >> 12 & 15);
        var2 = this.field_151254_d[0] & 255;
        var3 = this.chunkLocation.chunkZPos * 16 + (this.field_151254_d[0] >> 8 & 15);
        this.func_151251_a(new S23PacketBlockChange(var1, var2, var3, PlayerManager.this.theWorldServer));
        if (PlayerManager.this.theWorldServer.getBlock(var1, var2, var3).hasTileEntity())
        {
            this.func_151252_a(PlayerManager.this.theWorldServer.getTileEntity(var1, var2, var3));
        }
        
        // send multi-block updates
        this.func_151251_a(new S22PacketMultiBlockChange(this.numberOfTilesToUpdate, this.field_151254_d, PlayerManager.this.theWorldServer.getChunkFromChunkCoords(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos)));
        for (var1 = 0; var1 < this.numberOfTilesToUpdate; ++var1)
        {
            var2 = this.chunkLocation.chunkXPos * 16 + (this.field_151254_d[var1] >> 12 & 15);
            var3 = this.field_151254_d[var1] & 255;
            var4 = this.chunkLocation.chunkZPos * 16 + (this.field_151254_d[var1] >> 8 & 15);

            if (PlayerManager.this.theWorldServer.getBlock(var2, var3, var4).hasTileEntity())
            {
                this.func_151252_a(PlayerManager.this.theWorldServer.getTileEntity(var2, var3, var4));
            }
        }
        
        // send whole chunk updates
        var1 = this.chunkLocation.chunkXPos * 16;
        var2 = this.chunkLocation.chunkZPos * 16;
        this.func_151251_a(new S21PacketChunkData(PlayerManager.this.theWorldServer.getChunkFromChunkCoords(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos), false, this.flagsYAreasToUpdate));
        for (var3 = 0; var3 < 16; ++var3)
        {
            if ((this.flagsYAreasToUpdate & 1 << var3) != 0)
            {
                var4 = var3 << 4;
                List var5 = PlayerManager.this.theWorldServer.func_147486_a(var1, var4, var2, var1 + 16, var4 + 16, var2 + 16);

                for (int var6 = 0; var6 < var5.size(); ++var6)
                {
                    this.func_151252_a((TileEntity)var5.get(var6));
                }
            }
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
		
		int playerChunkX = (int)player.posX >> 4;
		int playerChunkZ = (int)player.posZ >> 4;
		double dx = player.managedPosX - player.posX;
		double dz = player.managedPosZ - player.posZ;
		double distMovedSquared = dx * dx + dz * dz;
		
		if( distMovedSquared >= 64.0D ) // 8 blocks
		{
			int lastChunkX = (int)player.managedPosX >> 4;
			int lastChunkZ = (int)player.managedPosZ >> 4;
			int radius = this.playerViewRadius;
			int chunkDx = playerChunkX - lastChunkX;
			int chunkDz = playerChunkZ - lastChunkZ;
			
			if( chunkDx != 0 || chunkDz != 0 )
			{
				for( int x = playerChunkX - radius; x <= playerChunkX + radius; ++x )
				{
					for( int z = playerChunkZ - radius; z <= playerChunkZ + radius; ++z )
					{
						// add player to new chunk
						if( !this.overlaps( x, z, lastChunkX, lastChunkZ, radius ) )
						{
							PlayerInstance watcher = this.getOrCreateChunkWatcher( x, z, true );
							watcher.addPlayer( player );
						}
						
						// remove player from old chunk
						if( !this.overlaps( x - chunkDx, z - chunkDz, playerChunkX, playerChunkZ, radius ) )
						{
							PlayerManager.PlayerInstance var17 = this.getOrCreateChunkWatcher( x - chunkDx, z - chunkDz, false );
							
							if( var17 != null )
							{
								var17.removePlayer( player );
							}
						}
					}
				}
				
				this.filterChunkLoadQueue( player );
				player.managedPosX = player.posX;
				player.managedPosZ = player.posZ;
			}
		}
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
		
		// UNDONE: pull off enough cubic chunks from the queue to fit in a packet
		
		// copied from EntityPlayerMP.onUpdate()
		if( !this.loadedChunks.isEmpty() )
		{
			// drain the "loadedChunks" list into a buffer for later
			// transmission
			ArrayList chunks = new ArrayList();
			Iterator iterChunk = this.loadedChunks.iterator();
			ArrayList tileEntities = new ArrayList();
			Chunk chunk;
			
			while( iterChunk.hasNext() && chunks.size() < S26PacketMapChunkBulk.func_149258_c() )
			{
				ChunkCoordIntPair chunkCoords = (ChunkCoordIntPair)iterChunk.next();
				
				if( chunkCoords != null )
				{
					if( this.worldObj.blockExists( chunkCoords.chunkXPos << 4, 0, chunkCoords.chunkZPos << 4 ) )
					{
						chunk = this.worldObj.getChunkFromChunkCoords( chunkCoords.chunkXPos, chunkCoords.chunkZPos );
						
						if( chunk.func_150802_k() )
						{
							chunks.add( chunk );
							tileEntities.addAll( ( (WorldServer)this.worldObj ).func_147486_a( chunkCoords.chunkXPos * 16, 0, chunkCoords.chunkZPos * 16, chunkCoords.chunkXPos * 16 + 16, 256,
									chunkCoords.chunkZPos * 16 + 16 ) );
							iterChunk.remove();
						}
					}
				}
				else
				{
					iterChunk.remove();
				}
			}
			
			if( !chunks.isEmpty() )
			{
				// send the selected chunks
				this.playerNetServerHandler.sendPacket( new S26PacketMapChunkBulk( chunks ) );
				
				// send tile entities in those chunks
				Iterator var10 = tileEntities.iterator();
				while( var10.hasNext() )
				{
					TileEntity var11 = (TileEntity)var10.next();
					this.func_147097_b( var11 );
				}
				
				// something about watching chunks
				var10 = chunks.iterator();
				while( var10.hasNext() )
				{
					chunk = (Chunk)var10.next();
					this.getServerForPlayer().getEntityTracker().func_85172_a( this, chunk );
				}
			}
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
