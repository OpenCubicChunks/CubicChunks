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

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.packet.clientbound.PacketMapChunkBulk;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;

import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.visibility.CubeSelector;
import cubicchunks.visibility.CuboidalCubeSelector;
import cubicchunks.world.ColumnView;
import cubicchunks.world.Cube;

public class CubePlayerManager extends PlayerManager {
	
	private static final Logger log = LogManager.getLogger();
	
	private static class PlayerInfo {
		
		public Set<Long> watchedAddresses;
		public List<Cube> outgoingCubes;
		public CubeSelector cubeSelector;
		public int blockX;
		public int blockY;
		public int blockZ;
		public long address;
		
		public PlayerInfo() {
			this.watchedAddresses = new TreeSet<Long>();
			this.outgoingCubes = new LinkedList<Cube>();
			this.cubeSelector = new CuboidalCubeSelector();
			this.blockX = 0;
			this.blockY = 0;
			this.blockZ = 0;
			this.address = 0;
		}
		
		public void sortOutgoingCubes() {
			// get the player chunk position
			final int cubeX = AddressTools.getX(this.address);
			final int cubeY = AddressTools.getY(this.address);
			final int cubeZ = AddressTools.getZ(this.address);
			
			// sort cubes so they load radially away from the player
			Collections.sort(this.outgoingCubes, new Comparator<Cube>() {
				
				@Override
				public int compare(Cube a, Cube b) {
					return getManhattanDist(a) - getManhattanDist(b);
				}
				
				private int getManhattanDist(Cube cube) {
					int dx = Math.abs(cube.getX() - cubeX);
					int dy = Math.abs(cube.getY() - cubeY);
					int dz = Math.abs(cube.getZ() - cubeZ);
					return dx + dy + dz;
				}
			});
		}
		
		public void removeOutOfRangeOutgoingCubes() {
			Iterator<Cube> iter = this.outgoingCubes.iterator();
			while (iter.hasNext()) {
				Cube cube = iter.next();
				if (!this.cubeSelector.isVisible(cube.getAddress())) {
					iter.remove();
				}
			}
		}
	}
	
	private WorldServer m_worldServer;
	private int m_viewDistance;
	private TreeMap<Long,CubeWatcher> m_watchers;
	private TreeMap<Integer,PlayerInfo> m_players;
	
	public CubePlayerManager(WorldServer worldServer, int viewDistance) {
		super(worldServer);
		
		this.m_worldServer = worldServer;
		this.m_viewDistance = viewDistance;
		this.m_watchers = Maps.newTreeMap();
		this.m_players = Maps.newTreeMap();
	}
	
	@Override
	public void addPlayer(EntityPlayerMP player) {
		// make new player info
		PlayerInfo info = new PlayerInfo();
		this.m_players.put(player.getEntityId(), info);
		
		// set initial player position
		info.blockX = MathHelper.floorDoubleToInt(player.xPos);
		info.blockY = MathHelper.floorDoubleToInt(player.yPos);
		info.blockZ = MathHelper.floorDoubleToInt(player.zPos);
		int cubeX = Coords.blockToCube(info.blockX);
		int cubeY = Coords.blockToCube(info.blockY);
		int cubeZ = Coords.blockToCube(info.blockZ);
		info.address = AddressTools.getAddress(cubeX, cubeY, cubeZ);
		
		// compute initial visibility
		info.cubeSelector.setPlayerPosition(info.address, this.m_viewDistance);
		
		// add player to watchers and collect the cubes to send over
		for (long address : info.cubeSelector.getVisibleCubes()) {
			CubeWatcher watcher = getOrCreateWatcher(address);
			watcher.addPlayer(player);
			info.watchedAddresses.add(address);
			info.outgoingCubes.add(watcher.getCube());
		}
	}
	
	@Override
	public void removePlayer(EntityPlayerMP player) {
		// get the player info
		PlayerInfo info = this.m_players.get(player.getEntityId());
		if (info == null) {
			return;
		}
		
		// remove player from all its cubes
		for (long address : info.watchedAddresses) {
			// skip non-existent cubes
			if (!cubeExists(address)) {
				continue;
			}
			
			// get the watcher
			CubeWatcher watcher = getWatcher(address);
			if (watcher == null) {
				continue;
			}
			
			// remove from the watcher
			watcher.removePlayer(player);
			
			// cleanup empty watchers and cubes
			if (!watcher.hasPlayers()) {
				this.m_watchers.remove(address);
				getCubeProvider().unloadCube(watcher.getCube());
			}
		}
		
		// remove the info
		this.m_players.remove(player.getEntityId());
	}
	
	@Override
	public void updatePlayerInstances() // aka tick()
	{
		// responsibilities:
		// update chunk properties
		// send chunk updates to players
		
		for (CubeWatcher watcher : this.m_watchers.values()) {
			watcher.sendUpdates();
			watcher.tick();
		}
		
		// did all the players leave an alternate dimension?
		if (this.m_players.isEmpty() && !this.m_worldServer.dimension.canRespawnHere()) {
			// unload everything
			getCubeProvider().unloadAllChunks();
		}
	}
	
	// markBlockForUpdate
	public void func_151250_a(int blockX, int blockY, int blockZ) {
		// get the watcher
		int cubeX = Coords.blockToCube(blockX);
		int cubeY = Coords.blockToCube(blockY);
		int cubeZ = Coords.blockToCube(blockZ);
		CubeWatcher watcher = getWatcher(cubeX, cubeY, cubeZ);
		if (watcher == null) {
			return;
		}
		
		// pass off to watcher
		int localX = Coords.blockToLocal(blockX);
		int localY = Coords.blockToLocal(blockY);
		int localZ = Coords.blockToLocal(blockZ);
		watcher.setDirtyBlock(localX, localY, localZ);
	}
	
	@Override
	public void updateMountedMovingPlayer(EntityPlayerMP player) {
		// the player moved
		// if the player moved into a new chunk, update which chunks the player needs to know about
		// then update the list of chunks that need to be sent to the client
		
		// get the player info
		PlayerInfo info = this.m_players.get(player.getEntityId());
		if (info == null) {
			return;
		}
		
		// did the player move far enough to matter?
		int newBlockX = MathHelper.floorDoubleToInt(player.xPos);
		int newBlockY = MathHelper.floorDoubleToInt(player.yPos);
		int newBlockZ = MathHelper.floorDoubleToInt(player.zPos);
		int manhattanDistance = Math.abs(newBlockX - info.blockX) + Math.abs(newBlockY - info.blockY) + Math.abs(newBlockZ - info.blockZ);
		if (manhattanDistance < 8) {
			return;
		}
		
		// did the player move into a new cube?
		int newCubeX = Coords.blockToCube(newBlockX);
		int newCubeY = Coords.blockToCube(newBlockY);
		int newCubeZ = Coords.blockToCube(newBlockZ);
		long newAddress = AddressTools.getAddress(newCubeX, newCubeY, newCubeZ);
		if (newAddress == info.address) {
			return;
		}
		
		// update player info
		info.blockX = newBlockX;
		info.blockY = newBlockY;
		info.blockZ = newBlockZ;
		info.address = newAddress;
		
		// calculate new visibility
		info.cubeSelector.setPlayerPosition(newAddress, this.m_viewDistance);
		
		// add to new watchers
		for (long address : info.cubeSelector.getNewlyVisibleCubes()) {
			CubeWatcher watcher = getOrCreateWatcher(address);
			watcher.addPlayer(player);
			info.outgoingCubes.add(watcher.getCube());
		}
		
		// remove from old watchers
		for (long address : info.cubeSelector.getNewlyHiddenCubes()) {
			CubeWatcher watcher = getWatcher(address);
			if (watcher == null) {
				continue;
			}
			
			watcher.removePlayer(player);
			
			// cleanup empty watchers and cubes
			if (!watcher.hasPlayers()) {
				this.m_watchers.remove(address);
				getCubeProvider().unloadCube(watcher.getCube());
			}
		}
	}
	
	@Override
	public boolean isPlayerWatchingChunk(EntityPlayerMP player, int cubeX, int cubeZ) {
		// get the info
		PlayerInfo info = this.m_players.get(player.getEntityId());
		if (info == null) {
			return false;
		}
		
		// check the player's watched addresses
		for (long address : info.watchedAddresses) {
			int x = AddressTools.getX(address);
			int z = AddressTools.getZ(address);
			if (x == cubeX && z == cubeZ) {
				return true;
			}
		}
		return false;
	}
	
	public void onPlayerUpdate(EntityPlayerMP player) {
		// this method flushes outgoing chunks to the player
		
		// get the outgoing cubes
		PlayerInfo info = this.m_players.get(player.getEntityId());
		if (info == null || info.outgoingCubes.isEmpty()) {
			return;
		}
		info.removeOutOfRangeOutgoingCubes();
		info.sortOutgoingCubes();
		
		// pull off enough cubes from the queue to fit in a packet
		final int MaxCubesToSend = 100;
		List<Cube> cubesToSend = new ArrayList<Cube>();
		List<BlockEntity> blockEntitiesToSend = new ArrayList<BlockEntity>();
		Iterator<Cube> iter = info.outgoingCubes.iterator();
		while (iter.hasNext() && cubesToSend.size() < MaxCubesToSend) {
			Cube cube = iter.next();
			
			// wait for the cube to be live before sending this cube
			// or any cube in the order after it
			if (!cube.getGeneratorStage().isLastStage()) {
				break;
			}
			
			// add this cube to the send buffer
			cubesToSend.add(cube);
			iter.remove();
			
			// add tile entities too
			for (BlockEntity blockEntity : cube.getBlockEntities()) {
				blockEntitiesToSend.add(blockEntity);
			}
		}
		
		if (cubesToSend.isEmpty()) {
			return;
		}
		
		// group the cubes into column views
		Map<Long,ColumnView> views = new TreeMap<Long,ColumnView>();
		for (Cube cube : cubesToSend) {
			// is there a column view for this cube?
			long columnAddress = AddressTools.getAddress(cube.getX(), cube.getZ());
			ColumnView view = views.get(columnAddress);
			if (view == null) {
				view = new ColumnView(cube.getColumn());
				views.put(columnAddress, view);
			}
			
			view.addCubeToView(cube);
		}
		List<Chunk> columnsToSend = new ArrayList<Chunk>(views.values());
		
		// send the cube data with the first time flag set
		player.netServerHandler.send(new PacketMapChunkBulk(columnsToSend));
		log.info(String.format("Server sent %d cubes to player, %d remaining", cubesToSend.size(), info.outgoingCubes.size()));
		
		// tell the cube watchers which cubes were sent for this player
		for (Cube cube : cubesToSend) {
			// get the watcher
			CubeWatcher watcher = getWatcher(cube.getAddress());
			if (watcher == null) {
				continue;
			}
			
			watcher.setPlayerSawCube(player);
		}
		
		// send tile entity data
		for (BlockEntity blockEntity : blockEntitiesToSend) {
			IPacket<?> packet = blockEntity.getDescriptionPacket();
			if (packet != null) {
				player.netServerHandler.send(packet);
			}
		}
		
		// watch entities on the chunks we just sent
		for (Chunk chunk : columnsToSend) {
			this.m_worldServer.getEntityTracker().a(player, chunk);
		}
	}
	
	public Iterable<Long> getVisibleCubeAddresses(EntityPlayerMP player) {
		// get the info
		PlayerInfo info = this.m_players.get(player.getEntityId());
		if (info == null) {
			return null;
		}
		
		return info.cubeSelector.getVisibleCubes();
	}
	
	private ServerCubeCache getCubeProvider() {
		return (ServerCubeCache)this.m_worldServer.serverChunkCache;
	}
	
	private CubeWatcher getWatcher(int cubeX, int cubeY, int cubeZ) {
		return getWatcher(AddressTools.getAddress(cubeX, cubeY, cubeZ));
	}
	
	private CubeWatcher getWatcher(long address) {
		return this.m_watchers.get(address);
	}
	
	private boolean cubeExists(long address) {
		int cubeX = AddressTools.getX(address);
		int cubeY = AddressTools.getY(address);
		int cubeZ = AddressTools.getZ(address);
		return getCubeProvider().cubeExists(cubeX, cubeY, cubeZ);
	}
	
	private CubeWatcher getOrCreateWatcher(long address) {
		CubeWatcher watcher = this.m_watchers.get(address);
		if (watcher == null) {
			// get the cube
			int cubeX = AddressTools.getX(address);
			int cubeY = AddressTools.getY(address);
			int cubeZ = AddressTools.getZ(address);
			getCubeProvider().loadCubeAndNeighbors(cubeX, cubeY, cubeZ);
			
			// make a new watcher
			watcher = new CubeWatcher(getCubeProvider().getCube(cubeX, cubeY, cubeZ));
			this.m_watchers.put(address, watcher);
		}
		return watcher;
	}
}
