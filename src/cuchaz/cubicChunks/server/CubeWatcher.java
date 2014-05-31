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
package cuchaz.cubicChunks.server;

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

import cuchaz.cubicChunks.util.Bits;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.ColumnView;
import cuchaz.cubicChunks.world.Cube;

public class CubeWatcher
{
	private Cube m_cube;
	private TreeMap<Integer,EntityPlayerMP> m_players;
	private long m_previousWorldTime;
	private TreeSet<Integer> m_dirtyBlocks;
	
	public CubeWatcher( Cube cube )
	{
		if( cube == null )
		{
			throw new IllegalArgumentException( "cube cannot be null!" );
		}
		
		m_cube = cube;
		m_players = Maps.newTreeMap();
		m_previousWorldTime = 0;
		m_dirtyBlocks = new TreeSet<Integer>();
	}
	
	public Cube getCube( )
	{
		return m_cube;
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
		return m_cube.getWorld().getTotalWorldTime();
	}
	
	private void updateInhabitedTime( )
	{
		long now = getWorldTime();
		m_cube.getColumn().inhabitedTime += now - m_previousWorldTime;
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
		
		World world = m_cube.getWorld();
		
		// how many?
		if( m_dirtyBlocks.size() == 1 )
		{
			// get the block coords
			int address = m_dirtyBlocks.first();
			int localX = unpackLocalX( address );
			int localY = unpackLocalY( address );
			int localZ = unpackLocalZ( address );
			int blockX = Coords.localToBlock( m_cube.getX(), localX );
			int blockY = Coords.localToBlock( m_cube.getY(), localY );
			int blockZ = Coords.localToBlock( m_cube.getZ(), localZ );
			
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
			ColumnView view = new ColumnView( m_cube.getColumn() );
			view.addCubeToView( m_cube );
			
			// send whole chunk update
			sendPacketToAllPlayers( new S21PacketChunkData( view, false, 0 ) );
			for( TileEntity tileEntity : m_cube.tileEntities() )
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
				int blockY = Coords.localToBlock( m_cube.getY(), localY );
				coords[i++] = (short)( ( localX & 0xf ) << 12 | ( localZ & 0xf ) << 8 | ( blockY & 0xff ) );
			}
			
			// send multi-block updates
			sendPacketToAllPlayers( new S22PacketMultiBlockChange( coords.length, coords, m_cube.getColumn() ) );
			for( int address : m_dirtyBlocks )
			{
				int localX = unpackLocalX( address );
				int localY = unpackLocalY( address );
				int localZ = unpackLocalZ( address );
				sendTileEntityToAllPlayers( m_cube.getTileEntity( localX, localY, localZ ) );
			}
		}
		
		m_dirtyBlocks.clear();
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
