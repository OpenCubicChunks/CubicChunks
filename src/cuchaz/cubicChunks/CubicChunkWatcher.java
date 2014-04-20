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

import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.google.common.collect.Maps;

public class CubicChunkWatcher
{
	private CubicChunk m_cubicChunk;
	private TreeMap<Integer,EntityPlayerMP> m_players;
	private long m_previousWorldTime;
	private TreeSet<Integer> m_dirtyBlocks;
	
	public CubicChunkWatcher( CubicChunk cubicChunk )
	{
		if( cubicChunk == null )
		{
			throw new IllegalArgumentException( "cubicChunk cannot be null!" );
		}
		
		m_cubicChunk = cubicChunk;
		m_players = Maps.newTreeMap();
		m_previousWorldTime = 0;
		m_dirtyBlocks = new TreeSet<Integer>();
	}
	
	public CubicChunk getCubicChunk( )
	{
		return m_cubicChunk;
	}
	
	public void addPlayer( EntityPlayerMP player )
	{
		m_players.put( player.getEntityId(), player );
		m_previousWorldTime = getWorldTime();
	}
	
	public void removePlayer( EntityPlayerMP player )
	{
		m_players.remove( player.getEntityId() );
		updateInhabitedTime();
	}
	
	public boolean hasPlayers( )
	{
		return !m_players.isEmpty();
	}
	
	public void tick( )
	{
		updateInhabitedTime();
	}
	
	private long getWorldTime( )
	{
		return m_cubicChunk.getWorld().getTotalWorldTime();
	}
	
	private void updateInhabitedTime( )
	{
		long now = getWorldTime();
		m_cubicChunk.getColumn().inhabitedTime += now - m_previousWorldTime;
		m_previousWorldTime = now;
	}
	
	public void setDirtyBlock( int localX, int localY, int localZ )
	{
		m_dirtyBlocks.add( packAddress( localX, localY, localZ ) );
	}
	
	public void sendUpdates( )
	{
		// are there any updates?
		if( m_dirtyBlocks.isEmpty() )
		{
			return;
		}
		
		World world = m_cubicChunk.getWorld();
		
		// how many?
		if( m_dirtyBlocks.size() == 1 )
		{
			// get the block coords
			int address = m_dirtyBlocks.first();
			int localX = unpackLocalX( address );
			int localY = unpackLocalY( address );
			int localZ = unpackLocalZ( address );
			int blockX = Coords.localToBlock( m_cubicChunk.getX(), localX );
			int blockY = Coords.localToBlock( m_cubicChunk.getY(), localY );
			int blockZ = Coords.localToBlock( m_cubicChunk.getZ(), localZ );
			
			// send single block updates
			sendPacketToAllPlayers( new S23PacketBlockChange( blockX, blockY, blockZ, world ) );
			if( world.getBlock( blockX, blockY, blockZ ).hasTileEntity() )
			{
				sendTileEntityToAllPlayers( world.getTileEntity( blockX, blockY, blockZ ) );
			}
		}
		else if( m_dirtyBlocks.size() == 64 )
		{
			// make a column view
			ColumnView view = new ColumnView( m_cubicChunk.getColumn() );
			view.addCubicChunkToView( m_cubicChunk );
			
			// send whole chunk update
			sendPacketToAllPlayers( new S21PacketChunkData( view, false, 0 ) );
			for( TileEntity tileEntity : m_cubicChunk.tileEntities() )
			{
				sendTileEntityToAllPlayers( tileEntity );
			}
		}
		else
		{
			// encode the update coords
			short[] coords = new short[m_dirtyBlocks.size()];
			int i=0;
			for( int address : m_dirtyBlocks )
			{
				int localX = unpackLocalX( address );
				int localY = unpackLocalY( address );
				int localZ = unpackLocalZ( address );
				int blockY = Coords.localToBlock( m_cubicChunk.getY(), localY );
				coords[i++] = (short)( ( localX & 0xf ) << 12 | ( localZ & 0xf ) << 8 | ( blockY & 0xff ) );
			}
			
			// send multi-block updates
			sendPacketToAllPlayers( new S22PacketMultiBlockChange( coords.length, coords, m_cubicChunk.getColumn() ) );
			for( int address : m_dirtyBlocks )
			{
				int localX = unpackLocalX( address );
				int localY = unpackLocalY( address );
				int localZ = unpackLocalZ( address );
				sendTileEntityToAllPlayers( m_cubicChunk.getTileEntity( localX, localY, localZ ) );
			}
		}
	}
	
	private void sendTileEntityToAllPlayers( TileEntity tileEntity )
	{
		if( tileEntity == null )
		{
			return;
		}
		
		Packet packet = tileEntity.getDescriptionPacket();
		if( packet == null )
		{
			return;
		}
		
		sendPacketToAllPlayers( packet );
	}

	private void sendPacketToAllPlayers( Packet packet )
	{
		for( EntityPlayerMP player : m_players.values() )
		{
			player.playerNetServerHandler.sendPacket( packet );
		}
	}
	
	private int packAddress( int localX, int localY, int localZ )
	{
		return Bits.packUnsignedToInt( localX, 4, 0 ) | Bits.packUnsignedToInt( localY, 4, 4 ) | Bits.packUnsignedToInt( localZ, 4, 8 );
	}
	
	private int unpackLocalX( int packed )
	{
		return Bits.unpackUnsigned( packed, 4, 0 );
	}
	
	private int unpackLocalY( int packed )
	{
		return Bits.unpackUnsigned( packed, 4, 4 );
	}
	
	private int unpackLocalZ( int packed )
	{
		return Bits.unpackUnsigned( packed, 4, 8 );
	}
}
