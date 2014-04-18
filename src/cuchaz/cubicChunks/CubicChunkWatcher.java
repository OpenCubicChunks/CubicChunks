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

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;

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
	
	public void sendUpdates( )
	{
		// NEXTTIME: finish this
		// NOTE: use sendPacketToAllPlayers()
		
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
	
	private void sendPacketToAllPlayers( Packet packet )
	{
		for( EntityPlayerMP player : m_players.values() )
		{
			player.playerNetServerHandler.sendPacket( packet );
		}
	}
}
