/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.server;

import com.google.common.collect.Maps;
import cubicchunks.network.PacketCubeBlockChange;
import cubicchunks.network.PacketCubeChange;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.util.AddressTools;
import cubicchunks.world.cube.Cube;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
	
	private Cube cube;
	private Map<Integer,PlayerEntry> players;
	private long previousWorldTime;
	private SortedSet<Integer> dirtyBlocks;
	
	public CubeWatcher(Cube cube) {
		if (cube == null) {
			throw new IllegalArgumentException("cube cannot be null!");
		}
		
		this.cube = cube;
		this.players = Maps.newTreeMap();
		this.previousWorldTime = 0;
		this.dirtyBlocks = new TreeSet<Integer>();
	}
	
	public Cube getCube() {
		return this.cube;
	}
	
	public void addPlayer(EntityPlayerMP player) {
		this.players.put(player.getEntityId(), new PlayerEntry(player));
		this.previousWorldTime = getWorldTime();
	}
	
	public void removePlayer(EntityPlayerMP player) {
		this.players.remove(player.getEntityId());
		updateInhabitedTime();
	}
	
	public boolean hasPlayers() {
		return !this.players.isEmpty();
	}
	
	public void setPlayerSawCube(EntityPlayerMP player) {
		PlayerEntry entry = this.players.get(player.getEntityId());
		if (entry != null) {
			entry.sawCube = true;
		}
	}
	
	public void tick() {
		updateInhabitedTime();
	}
	
	private long getWorldTime() {
		return this.cube.getWorld().getWorldTime();
	}
	
	private void updateInhabitedTime() {
		final long now = getWorldTime();
		
		long inhabitedTime = this.cube.getColumn().getInhabitedTime();
		inhabitedTime += now - this.previousWorldTime;
		
		this.cube.getColumn().setInhabitedTime(inhabitedTime);
		this.previousWorldTime = now;
	}
	
	public void setDirtyBlock(int localX, int localY, int localZ) {
		// save up to some number of individual block updates
		// once that threshold is passed, the whole cube is sent during an update,
		// so there's no need to save more per-block updates
		if (this.dirtyBlocks.size() < MaxBlocksPerUpdate) {
			this.dirtyBlocks.add(AddressTools.getLocalAddress(localX, localY, localZ));
		}
	}
	
	public void sendUpdates() {
		
		// are there any updates?
		if (this.dirtyBlocks.isEmpty()) {
			return;
		}
		
		World world = this.cube.getWorld();
		
		if (this.dirtyBlocks.size() == MaxBlocksPerUpdate) {
			
			// send whole cube
			sendPacketToAllPlayers(new PacketCubeChange(cube));
			for (TileEntity blockEntity : this.cube.getBlockEntities()) {
				sendBlockEntityToAllPlayers(blockEntity);
			}
			
		} else {
			
			// send all the dirty blocks
			sendPacketToAllPlayers(new PacketCubeBlockChange(this.cube, this.dirtyBlocks));
			
			// send the block entites on those blocks too
			for (int address : this.dirtyBlocks) {
				BlockPos pos = cube.localAddressToBlockPos(address);
				if (world.getTileEntity(pos) != null) {
					sendBlockEntityToAllPlayers(world.getTileEntity(pos));
				}
			}
		}
		
		this.dirtyBlocks.clear();
	}
	
	private void sendBlockEntityToAllPlayers(TileEntity blockEntity) {
		if (blockEntity == null) {
			return;
		}
		Packet packet = blockEntity.getDescriptionPacket();
		if (packet == null) {
			return;
		}
		sendPacketToAllPlayers(packet);
	}

	//Java really needs templates...
	private void sendPacketToAllPlayers(Packet packet) {
		for (PlayerEntry entry : this.players.values()) {
			// has this player seen this cube before?
			if (entry.sawCube) {
				entry.player.playerNetServerHandler.sendPacket(packet);
			}
		}
	}

	private void sendPacketToAllPlayers(IMessage packet) {
		for (PlayerEntry entry : this.players.values()) {
			// has this player seen this cube before?
			if (entry.sawCube) {
				PacketDispatcher.sendTo(packet, entry.player);
			}
		}
	}
}
