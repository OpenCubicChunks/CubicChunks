/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.packet.clientbound.PacketBlockChange;
import net.minecraft.network.play.packet.clientbound.PacketChunkData;
import net.minecraft.network.play.packet.clientbound.PacketMultiBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk.ChunkEntityCreationType;

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
		return this.cube.getWorld().getGameTime();
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
			this.dirtyBlocks.add(packAddress(localX, localY, localZ));
		}
	}
	
	public void sendUpdates() {
		// are there any updates?
		if (this.dirtyBlocks.isEmpty()) {
			return;
		}
		
		World world = this.cube.getWorld();
		
		// how many?
		if (this.dirtyBlocks.size() == 1) {
			// get the block coords
			int address = this.dirtyBlocks.first();
			
			BlockPos pos = addressToBlockPos(address);
			
			// send single block updates
			sendPacketToAllPlayers(new PacketBlockChange(world, pos));
			if (world.getBlockEntityAt(pos) != null) {
				sendBlockEntityToAllPlayers(world.getBlockEntityAt(pos));
			}
		} else if (this.dirtyBlocks.size() == MaxBlocksPerUpdate) {
			// send whole cube (wrapped in a column view)
			ColumnView view = new ColumnView(this.cube.getColumn());
			view.addCubeToView(this.cube);
			sendPacketToAllPlayers(new PacketChunkData(view, false, 0));
			for (BlockEntity blockEntity : this.cube.getBlockEntities()) {
				sendBlockEntityToAllPlayers(blockEntity);
			}
		} else {
			// encode the update coords
			short[] coords = new short[this.dirtyBlocks.size()];
			int i = 0;
			for (int address : this.dirtyBlocks) {
				int localX = unpackLocalX(address);
				int localY = unpackLocalY(address);
				int localZ = unpackLocalZ(address);
				int blockY = Coords.localToBlock(this.cube.getY(), localY);
				coords[i++] = (short) ( (localX & 0xf) << 12 | (localZ & 0xf) << 8 | (blockY & 0xff));
			}
			
			// send multi-block updates
			sendPacketToAllPlayers(new PacketMultiBlockChange(coords.length, coords, this.cube.getColumn()));
			for (int address : this.dirtyBlocks) {
				int localX = unpackLocalX(address);
				int localY = unpackLocalY(address);
				int localZ = unpackLocalZ(address);
				int blockX = Coords.localToBlock(this.cube.getX(), localX);
				int blockY = Coords.localToBlock(this.cube.getY(), localY);
				int blockZ = Coords.localToBlock(this.cube.getZ(), localZ);
				
				BlockPos pos = new BlockPos(blockX, blockY, blockZ);
				sendBlockEntityToAllPlayers(this.cube.getBlockEntity(pos, ChunkEntityCreationType.QUEUED));
			}
		}
		
		this.dirtyBlocks.clear();
	}
	
	private void sendBlockEntityToAllPlayers(BlockEntity blockEntity) {
		if (blockEntity == null) {
			return;
		}
		
		IPacket<?> packet = blockEntity.getDescriptionPacket();
		if (packet == null) {
			return;
		}
		
		sendPacketToAllPlayers(packet);
	}
	
	private void sendPacketToAllPlayers(IPacket packet) {
		for (PlayerEntry entry : this.players.values()) {
			// has this player seen this cube before?
			if (entry.sawCube) {
				entry.player.netServerHandler.send(packet);
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
	
	private BlockPos addressToBlockPos(int address) {
		int localX = unpackLocalX(address);
		int localY = unpackLocalY(address);
		int localZ = unpackLocalZ(address);
		int blockX = Coords.localToBlock(this.cube.getX(), localX);
		int blockY = Coords.localToBlock(this.cube.getY(), localY);
		int blockZ = Coords.localToBlock(this.cube.getZ(), localZ);
		
		return new BlockPos(blockX, blockY, blockZ);	
	}
}
