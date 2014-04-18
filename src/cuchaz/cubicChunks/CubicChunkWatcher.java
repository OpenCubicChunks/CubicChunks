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

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.google.common.collect.Maps;

public class CubicChunkWatcher
{
	private CubicChunk m_cubicChunk;
	private TreeMap<Integer,EntityPlayerMP> m_players;
	private long m_previousWorldTime;
	private Set<Integer> m_dirtyBlocks;
	
	public CubicChunkWatcher( CubicChunk cubicChunk )
	{
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
		int localAddress = Bits.packUnsignedToInt( localX, 4, 0 )
			| Bits.packUnsignedToInt( localY, 4, 4 )
			| Bits.packUnsignedToInt( localZ, 4, 8 );
		m_dirtyBlocks.add( localAddress );
	}
}
