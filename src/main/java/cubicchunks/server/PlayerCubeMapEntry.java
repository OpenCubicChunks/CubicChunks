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
import cubicchunks.server.chunkio.async.forge.AsyncWorldIOExecutor;
import cubicchunks.util.AddressTools;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IProviderExtras;
import cubicchunks.world.cube.Cube;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PlayerCubeMapEntry {

	private final Consumer<Cube> consumer = (c) -> {
		this.cube = c;
		this.loading = false;
	};
	private final ServerCubeCache cubeCache;
	private PlayerCubeMap playerCubeMap;
	private Cube cube;
	private final TIntObjectMap<WatcherPlayerEntry> players = new TIntObjectHashMap<>();
	private final TShortList dirtyBlocks = new TShortArrayList(64);
	private final CubeCoords cubePos;
	private long previousWorldTime = 0;
	private boolean sentToPlayers = false;
	private boolean loading = true;

	// CHECKED: 1.10.2-12.18.1.2092
	public PlayerCubeMapEntry(PlayerCubeMap playerCubeMap, CubeCoords cubePos) {
		this.playerCubeMap = playerCubeMap;
		this.cubeCache = playerCubeMap.getWorld().getCubeCache();
		this.cubeCache.asyncGetCube(
				cubePos.getCubeX(), cubePos.getCubeY(), cubePos.getCubeZ(),
				IProviderExtras.Requirement.LOAD,
				consumer);
		this.cubePos = cubePos;
	}

	// CHECKED: 1.10.2-12.18.1.2092
	public void addPlayer(EntityPlayerMP player) {
		if (this.players.containsKey(player.getEntityId())) {
			CubicChunks.LOGGER.debug("Failed to add player. {} already is in cube at {}", player, cubePos);
			return;
		}
		if (this.players.isEmpty()) {
			this.previousWorldTime = this.getWorldTime();
		}
		this.players.put(player.getEntityId(), new WatcherPlayerEntry(player));

		if (this.sentToPlayers) {
			this.sendToPlayer(player);
			//TODO: cube watch event?
		}
	}

	// CHECKED: 1.10.2-12.18.1.2092
	public void removePlayer(EntityPlayerMP player) {
		if (!this.players.containsValue(player)) {
			return;
		}
		// If we haven't loaded yet don't load the chunk just so we can clean it up
		if (this.cube == null) {
			this.players.remove(player.getEntityId());

			if (this.players.isEmpty()) {
				if(loading) {
					AsyncWorldIOExecutor.dropQueuedCubeLoad(this.playerCubeMap.getWorld(),
							cubePos.getCubeX(), cubePos.getCubeY(), cubePos.getCubeZ(),
							c -> this.cube = c);
				}
				playerCubeMap.removeEntry(this);
			}
			return;
		}

		if (this.sentToPlayers) {
			PacketDispatcher.sendTo(new PacketUnloadCube(this.cubePos), player);
		}

		this.players.remove(player.getEntityId());
		//TODO: Cube unwatch event
		//net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkWatchEvent.UnWatch(this.pos, player));

		if (this.players.isEmpty()) {
			playerCubeMap.removeEntry(this);
		}
	}

	// CHECKED: 1.10.2-12.18.1.2092
	public boolean providePlayerCube(boolean canGenerate) {
		if(loading) {
			return false;
		}
		if(this.cube != null) {
			return true;
		}
		int cubeX = cubePos.getCubeX();
		int cubeY = cubePos.getCubeY();
		int cubeZ = cubePos.getCubeZ();

		playerCubeMap.getWorld().getProfiler().startSection("getCube");
		if (canGenerate) {
			this.cube = this.cubeCache.getCube(cubeX, cubeY, cubeZ, IProviderExtras.Requirement.LIGHT);
		} else {
			this.cube = this.cubeCache.getCube(cubeX, cubeY, cubeZ, IProviderExtras.Requirement.LOAD);
		}
		playerCubeMap.getWorld().getProfiler().endSection();

		return this.cube != null;
	}

	public boolean isSentToPlayers() {
		return sentToPlayers;
	}

	// CHECKED: 1.10.2-12.18.1.2092
	public boolean sendToPlayers() {
		if (this.sentToPlayers) {
			return true;
		}
		if (this.cube == null || !this.cube.isPopulated() || !this.cube.isInitialLightingDone()) {
			return false;
		}
		PlayerCubeMapColumnEntry columnEntry = playerCubeMap.getColumnWatcher(this.cubePos.chunkPos());
		//can't send cubes before columns
		if(columnEntry == null || columnEntry.getColumn() == null) {
			return false;
		}
		this.dirtyBlocks.clear();
		//set to true before adding to queue so that sendToPlayer can actually add it
		this.sentToPlayers = true;

		for (WatcherPlayerEntry playerEntry : this.players.valueCollection()) {
			//don't send entities here, Column sends them.
			//TODO: send entities per cube? Sending all entities from column may be bad on multiplayer
			sendToPlayer(playerEntry.player);
		}

		return true;
	}

	// CHECKED: 1.10.2-12.18.1.2092
	public void sendToPlayer(EntityPlayerMP player) {
		if (!this.sentToPlayers) {
			return;
		}
		PacketDispatcher.sendTo(new PacketCube(this.cube, PacketCube.Type.NEW_CUBE), player);
	}

	// CHECKED: 1.10.2-12.18.1.2092
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

	// CHECKED: 1.10.2-12.18.1.2092
	public void blockChanged(int localX, int localY, int localZ) {
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

	// CHECKED: 1.10.2-12.18.1.2092
	public void update() {
		if (!this.sentToPlayers) {
			return;
		}
		assert cube != null;
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
			this.dirtyBlocks.forEach(localAddress -> {
				BlockPos pos = cube.localAddressToBlockPos(localAddress);

				IBlockState state = this.cube.getBlockState(pos);
				if (state.getBlock().hasTileEntity(state)) {
					sendBlockEntityToAllPlayers(world.getTileEntity(pos));
				}
				return true;
			});
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

	public double getDistanceSq(CubeCoords cubePos, Entity entity) {
		double blockX = cubePos.getXCenter();
		double blockY = cubePos.getYCenter();
		double blockZ = cubePos.getZCenter();
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
			double dist = getDistanceSq(cubePos, entry.player);

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

	public CubeCoords getCubePos() {
		return cubePos;
	}
}
