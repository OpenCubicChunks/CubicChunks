/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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

import com.google.common.base.Predicate;
import cubicchunks.CubicChunks;
import cubicchunks.network.PacketCube;
import cubicchunks.network.PacketCubeBlockChange;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.network.PacketUnloadCube;
import cubicchunks.util.AddressTools;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IProviderExtras;
import cubicchunks.world.cube.Cube;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.SortedSet;
import java.util.TreeSet;

import static cubicchunks.util.AddressTools.getAddress;
import static cubicchunks.util.AddressTools.getX;
import static cubicchunks.util.AddressTools.getY;
import static cubicchunks.util.AddressTools.getZ;
import static cubicchunks.util.Coords.localToBlock;

public class PlayerCubeMapEntry {

	private final ServerCubeCache cubeCache;
	private PlayerCubeMap playerCubeMap;
	private Cube cube;
	private TIntObjectMap<WatcherPlayerEntry> players;
	private SortedSet<Integer> dirtyBlocks;
	private long cubeAddress;
	private long previousWorldTime;
	private boolean sentToPlayers;

	public PlayerCubeMapEntry(PlayerCubeMap playerCubeMap, int cubeX, int cubeY, int cubeZ) {
		this.playerCubeMap = playerCubeMap;
		this.cubeCache = playerCubeMap.getWorld().getCubeCache();
		this.cube = this.cubeCache.getCube(
				new CubeCoords(cubeX, cubeY, cubeZ),
				IProviderExtras.Requirement.LOAD);//TODO: async loading
		this.players = new TIntObjectHashMap<>();
		this.previousWorldTime = 0;
		this.dirtyBlocks = new TreeSet<>();
		this.sentToPlayers = false;
		this.cubeAddress = getAddress(cubeX, cubeY, cubeZ);
	}

	public void addPlayer(EntityPlayerMP player) {
		if (this.players.containsKey(player.getEntityId())) {
			CubicChunks.LOGGER.debug("Failed to add player. {} already is in cube at address {}", player,
					String.format("0x%016x", cubeAddress));
			return;
		}
		if (this.players.isEmpty()) {
			this.previousWorldTime = this.getWorldTime();
		}
		this.players.put(player.getEntityId(), new WatcherPlayerEntry(player));

		if (this.sentToPlayers) {
			this.sendToPlayer(player);
		}
	}

	public void removePlayer(EntityPlayerMP player) {
		if (this.players.containsKey(player.getEntityId())) {
			// If we haven't loaded yet don't load the chunk just so we can clean it up
			if (this.cube == null) {
				this.players.remove(player.getEntityId());

				if (this.players.isEmpty()) {
					//TODO: Implement threaded chunk loading
					//TODO: Port dropQueuedChunk it to cubic chunks?
					//net.minecraftforge.common.chunkio.ChunkIOExecutor.dropQueuedChunkLoad(PlayerManager.this.getWorldServer(), this.pos.chunkXPos, this.pos.chunkZPos, this.loadedRunnable);
					playerCubeMap.removeEntry(this);
				}
				return;
			}

			if (this.sentToPlayers) {
				PacketDispatcher.sendTo(new PacketUnloadCube(this.cubeAddress), player);
			}

			this.players.remove(player.getEntityId());
			//TODO: Cube unwatch event
			//net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkWatchEvent.UnWatch(this.pos, player));

			if (this.players.isEmpty()) {
				playerCubeMap.removeEntry(this);
			}
		}
	}

	public boolean providePlayerCube(boolean canGenerate) {
		int cubeX = getX(cubeAddress);
		int cubeY = getY(cubeAddress);
		int cubeZ = getZ(cubeAddress);

		playerCubeMap.getWorld().getProfiler().startSection("getCube");
		if (canGenerate) {
			this.cube = this.cubeCache.getCube(new CubeCoords(cubeX, cubeY, cubeZ), IProviderExtras.Requirement.LIGHT);
		} else {
			this.cube = this.cubeCache.getCube(new CubeCoords(cubeX, cubeY, cubeZ), IProviderExtras.Requirement.LOAD);
		}
		playerCubeMap.getWorld().getProfiler().endSection();

		return this.cube != null;
	}

	public boolean isSentToPlayers() {
		return sentToPlayers;
	}

	public boolean sendToPlayers() {
		if (this.sentToPlayers) {
			return true;
		}
		if (this.cube == null || !this.cube.isPopulated() || !this.cube.isInitialLightingDone()) {
			return false;
		}
		this.dirtyBlocks.clear();
		//set to true before adding to queue so that sendToPlayer can actually add it
		this.sentToPlayers = true;

		for (WatcherPlayerEntry entry : this.players.valueCollection()) {
			sendToPlayer(entry.player);
		}

		return true;
	}

	public void sendToPlayer(EntityPlayerMP player) {
		if (!this.sentToPlayers) {
			return;
		}
		PacketDispatcher.sendTo(new PacketCube(this.cube, PacketCube.Type.NEW_CUBE), player);
	}

	public void updateInhabitedTime() {
		final long now = getWorldTime();
		if (this.cube == null) {
			this.previousWorldTime = now;
			return;
		}

		long inhabitedTime = this.cube.getColumn().getInhabitedTime();
		inhabitedTime += now - this.previousWorldTime;

		this.cube.getColumn().setInhabitedTime(inhabitedTime);
		this.previousWorldTime = now;
	}

	public void setDirtyBlock(int localX, int localY, int localZ) {
		//if we are adding the first one, add it to update list
		if (this.dirtyBlocks.isEmpty()) {
			playerCubeMap.addToUpdateEntry(this);
		}
		// If the number of changes is above clumpingThreshold
		// we send the whole cube, but to decrease network usage
		// forge sends only TEs that have changed,
		// so we need to know all changed blocks. So add everything
		// it's a set so no need to check for duplicates
		this.dirtyBlocks.add(AddressTools.getLocalAddress(localX, localY, localZ));
	}

	public void update() {
		if (!this.sentToPlayers) {
			return;
		}
		// are there any updates?
		if (this.dirtyBlocks.isEmpty()) {
			return;
		}

		ICubicWorld world = this.cube.getWorld();

		if (this.dirtyBlocks.size() >= ForgeModContainer.clumpingThreshold) {
			// send whole cube
			sendPacketToAllPlayers(new PacketCube(cube, PacketCube.Type.UPDATE));
		} else {
			// send all the dirty blocks
			sendPacketToAllPlayers(new PacketCubeBlockChange(this.cube, this.dirtyBlocks));
			// send the block entites on those blocks too
			for (int localAddress : this.dirtyBlocks) {
				BlockPos pos = cube.localAddressToBlockPos(localAddress);

				IBlockState state = this.cube.getBlockState(pos);
				if (state.getBlock().hasTileEntity(state)) {
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
		Packet packet = blockEntity.getUpdatePacket();
		if (packet == null) {
			return;
		}
		sendPacketToAllPlayers(packet);
	}

	public boolean containsPlayer(EntityPlayerMP player) {
		return this.players.containsKey(player.getEntityId());
	}

	public boolean hasPlayerMatching(Predicate<EntityPlayerMP> predicate) {
		//if any of them is true - stop and return false, then negate the result to get true
		return !this.players.forEachValue(value -> !predicate.apply(value.player));
	}

	public boolean hasPlayers() {
		return !this.players.isEmpty();
	}

	public double getDistanceSq(long cubeAddress, Entity entity) {
		double blockX = localToBlock(getX(cubeAddress), 8);
		double blockY = localToBlock(getY(cubeAddress), 8);
		double blockZ = localToBlock(getZ(cubeAddress), 8);
		double dx = blockX - entity.posX;
		double dy = blockY - entity.posY;
		double dz = blockZ - entity.posZ;
		return dx*dx + dy*dy + dz*dz;
	}

	public Cube getCube() {
		return this.cube;
	}

	public double getClosestPlayerDistance() {
		double min = Double.MAX_VALUE;

		for (WatcherPlayerEntry entry : this.players.valueCollection()) {
			double dist = getDistanceSq(cubeAddress, entry.player);

			if (dist < min) {
				min = dist;
			}
		}

		return min;
	}

	private long getWorldTime() {
		return playerCubeMap.getWorldServer().getWorldTime();
	}

	private void sendPacketToAllPlayers(Packet<?> packet) {
		for (WatcherPlayerEntry entry : this.players.valueCollection()) {
			entry.player.connection.sendPacket(packet);
		}
	}

	private void sendPacketToAllPlayers(IMessage packet) {
		for (WatcherPlayerEntry entry : this.players.valueCollection()) {
			PacketDispatcher.sendTo(packet, entry.player);
		}
	}

	public long getCubeAddress() {
		return cubeAddress;
	}
}
