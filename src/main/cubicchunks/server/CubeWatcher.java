/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.server;

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

import cubicchunks.util.Bits;
import cubicchunks.util.Coords;
import cubicchunks.world.ColumnView;
import cubicchunks.world.Cube;

public class CubeWatcher {
	
	private static final int MaxBlocksPerUpdate = 64;
	
	private static class PlayerEntry {
		
		EntityPlayerMP player;
		boolean sawCube;
		
		public PlayerEntry(EntityPlayerMP player) {
			this.player = player;
			this.sawCube = false;
		}
	}
	
	private Cube m_cube;
	private TreeMap<Integer,PlayerEntry> m_players;
	private long m_previousWorldTime;
	private TreeSet<Integer> m_dirtyBlocks;
	
	public CubeWatcher(Cube cube) {
		if (cube == null) {
			throw new IllegalArgumentException("cube cannot be null!");
		}
		
		m_cube = cube;
		m_players = Maps.newTreeMap();
		m_previousWorldTime = 0;
		m_dirtyBlocks = new TreeSet<Integer>();
	}
	
	public Cube getCube() {
		return m_cube;
	}
	
	public void addPlayer(EntityPlayerMP player) {
		m_players.put(player.getEntityId(), new PlayerEntry(player));
		m_previousWorldTime = getWorldTime();
	}
	
	public void removePlayer(EntityPlayerMP player) {
		m_players.remove(player.getEntityId());
		updateInhabitedTime();
	}
	
	public boolean hasPlayers() {
		return !m_players.isEmpty();
	}
	
	public void setPlayerSawCube(EntityPlayerMP player) {
		PlayerEntry entry = m_players.get(player.getEntityId());
		if (entry != null) {
			entry.sawCube = true;
		}
	}
	
	public void tick() {
		updateInhabitedTime();
	}
	
	private long getWorldTime() {
		return m_cube.getWorld().getTotalWorldTime();
	}
	
	private void updateInhabitedTime() {
		long now = getWorldTime();
		m_cube.getColumn().inhabitedTime += now - m_previousWorldTime;
		m_previousWorldTime = now;
	}
	
	public void setDirtyBlock(int localX, int localY, int localZ) {
		// save up to some number of individual block updates
		// once that threshold is passed, the whole cube is sent during an update,
		// so there's no need to save more per-block updates
		if (m_dirtyBlocks.size() < MaxBlocksPerUpdate) {
			m_dirtyBlocks.add(packAddress(localX, localY, localZ));
		}
	}
	
	public void sendUpdates() {
		// are there any updates?
		if (m_dirtyBlocks.isEmpty()) {
			return;
		}
		
		World world = m_cube.getWorld();
		
		// how many?
		if (m_dirtyBlocks.size() == 1) {
			// get the block coords
			int address = m_dirtyBlocks.first();
			int localX = unpackLocalX(address);
			int localY = unpackLocalY(address);
			int localZ = unpackLocalZ(address);
			int blockX = Coords.localToBlock(m_cube.getX(), localX);
			int blockY = Coords.localToBlock(m_cube.getY(), localY);
			int blockZ = Coords.localToBlock(m_cube.getZ(), localZ);
			
			// send single block updates
			sendPacketToAllPlayers(new S23PacketBlockChange(blockX, blockY, blockZ, world));
			if (world.getBlock(blockX, blockY, blockZ).hasTileEntity()) {
				sendTileEntityToAllPlayers(world.getTileEntity(blockX, blockY, blockZ));
			}
		} else if (m_dirtyBlocks.size() == MaxBlocksPerUpdate) {
			// send whole cube (wrapped in a column view)
			ColumnView view = new ColumnView(m_cube.getColumn());
			view.addCubeToView(m_cube);
			sendPacketToAllPlayers(new S21PacketChunkData(view, false, 0));
			for (TileEntity tileEntity : m_cube.tileEntities()) {
				sendTileEntityToAllPlayers(tileEntity);
			}
		} else {
			// encode the update coords
			short[] coords = new short[m_dirtyBlocks.size()];
			int i = 0;
			for (int address : m_dirtyBlocks) {
				int localX = unpackLocalX(address);
				int localY = unpackLocalY(address);
				int localZ = unpackLocalZ(address);
				int blockY = Coords.localToBlock(m_cube.getY(), localY);
				coords[i++] = (short) ( (localX & 0xf) << 12 | (localZ & 0xf) << 8 | (blockY & 0xff));
			}
			
			// send multi-block updates
			sendPacketToAllPlayers(new S22PacketMultiBlockChange(coords.length, coords, m_cube.getColumn()));
			for (int address : m_dirtyBlocks) {
				int localX = unpackLocalX(address);
				int localY = unpackLocalY(address);
				int localZ = unpackLocalZ(address);
				sendTileEntityToAllPlayers(m_cube.getTileEntity(localX, localY, localZ));
			}
		}
		
		m_dirtyBlocks.clear();
	}
	
	private void sendTileEntityToAllPlayers(TileEntity tileEntity) {
		if (tileEntity == null) {
			return;
		}
		
		Packet packet = tileEntity.getDescriptionPacket();
		if (packet == null) {
			return;
		}
		
		sendPacketToAllPlayers(packet);
	}
	
	private void sendPacketToAllPlayers(Packet packet) {
		for (PlayerEntry entry : m_players.values()) {
			// has this player seen this cube before?
			if (entry.sawCube) {
				entry.player.playerNetServerHandler.sendPacket(packet);
			}
		}
	}
	
	private int packAddress(int localX, int localY, int localZ) {
		return Bits.packUnsignedToInt(localX, 4, 0) | Bits.packUnsignedToInt(localY, 4, 4) | Bits.packUnsignedToInt(localZ, 4, 8);
	}
	
	private int unpackLocalX(int packed) {
		return Bits.unpackUnsigned(packed, 4, 0);
	}
	
	private int unpackLocalY(int packed) {
		return Bits.unpackUnsigned(packed, 4, 4);
	}
	
	private int unpackLocalZ(int packed) {
		return Bits.unpackUnsigned(packed, 4, 8);
	}
}
