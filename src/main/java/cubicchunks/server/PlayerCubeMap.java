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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ComparisonChain;
import cubicchunks.CubicChunks;
import cubicchunks.util.AddressTools;
import cubicchunks.util.CubeCoords;
import cubicchunks.visibility.CubeSelector;
import cubicchunks.visibility.CuboidalCubeSelector;
import cubicchunks.world.ICubicWorldServer;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static cubicchunks.util.AddressTools.cubeToColumn;
import static cubicchunks.util.AddressTools.getAddress;
import static cubicchunks.util.AddressTools.getX;
import static cubicchunks.util.AddressTools.getY;
import static cubicchunks.util.AddressTools.getZ;
import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.blockToLocal;
import static cubicchunks.util.Coords.getCubeXForEntity;
import static cubicchunks.util.Coords.getCubeYForEntity;
import static cubicchunks.util.Coords.getCubeZForEntity;
import static net.minecraft.util.math.MathHelper.clamp_int;
import static net.minecraft.util.math.MathHelper.floor_double;

/**
 * A cubic chunks implementation of Player Manager.
 * <p>
 * This class manages loading and unloading cubes for players.
 */
public class PlayerCubeMap extends PlayerChunkMap {

	private static final Predicate<EntityPlayerMP> NOT_SPECTATOR = player -> player != null && !player.isSpectator();
	private static final Predicate<EntityPlayerMP> CAN_GENERATE_CHUNKS = player -> player != null &&
			(!player.isSpectator() || player.getServerWorld().getGameRules().getBoolean("spectatorsGenerateChunks"));

	/**
	 * Cube selector is used to find whuch cube positions need to be loaded/unloaded
	 * By default use CuboidalCubeSelector.
	 */
	private final CubeSelector cubeSelector = new CuboidalCubeSelector();

	/**
	 * Mapping if entityId to PlayerCubeMap.PlayerWrapper objects.
	 */
	private final TIntObjectMap<PlayerWrapper> players = new TIntObjectHashMap<>();

	/**
	 * Mapping of Cube addresses to CubeWatchers (Cube equivalent of PlayerManager.PlayerInstance).
	 * Contains cube addresses of all cubes loaded by players.
	 */
	private final TLongObjectMap<PlayerCubeMapEntry> cubeWatchers = new TLongObjectHashMap<>();

	/**
	 * Mapping of Column addresses to ColumnWatchers.
	 * Contains column addresses of all columns loaded by players.
	 * Exists for compatibility with vanilla and to send ColumnLoad/Unload packets to clients.
	 * Columns cannot be managed by client because they have separate data, like heightmap and biome array.
	 */
	private final TLongObjectMap<PlayerCubeMapColumnEntry> columnWatchers = new TLongObjectHashMap<>();

	/**
	 * All cubeWatchers that have pending block updates to send.
	 */
	private final Set<PlayerCubeMapEntry> cubeWatchersToUpdate = new HashSet<>();

	/**
	 * Contains all CubeWatchers that need to be sent to clients,
	 * but these cubes are not fully loaded/generated yet.
	 * <p>
	 * Note that this is not the same as toGenerate list.
	 * Cube can be loaded while not being fully generated yet (not in the last GeneratorPipeline stage).
	 */
	private final List<PlayerCubeMapEntry> toSendToClient = new ArrayList<>();

	/**
	 * Contains all CubeWatchers that still need to be loaded/generated.
	 * CubeWatcher constructor attempts to load cube from disk, but it won't generate it.
	 * Technically it can generate it, especially with GeneratorPipeline
	 * but spectator players can't generate chunks if spectatorsGenerateChunks gamerule is set.
	 */
	private final List<PlayerCubeMapEntry> toGenerate = new ArrayList<>();

	private int viewDistance;

	/**
	 * This is used only to force update of all CubeWatchers every 8000 ticks
	 */
	private long previousWorldTime = 0;

	private boolean toGenerateNeedSort = true;
	private boolean toSendToClientNeedSort = true;

	private ServerCubeCache cubeCache;

	public PlayerCubeMap(ICubicWorldServer worldServer) {
		super((WorldServer) worldServer);
		this.cubeCache = (ServerCubeCache) getWorldServer().getChunkProvider();
		this.setPlayerViewRadius(worldServer.getMinecraftServer().getPlayerList().getViewDistance());
	}

	/**
	 * This method exists only because vanilla needs it. It shouldn't be used anywhere else.
	 */
	@Override
	@Deprecated
	public Iterator<Chunk> getChunkIterator() {
		final Iterator<PlayerCubeMapColumnEntry> iterator = this.columnWatchers.valueCollection().iterator();
		return new AbstractIterator<Chunk>() {
			protected Chunk computeNext() {
				while (iterator.hasNext()) {
					PlayerCubeMapColumnEntry watcher = iterator.next();
					Chunk column = watcher.getChunk();

					if (column == null) {
						continue;
					}
					//columns that don't have light calculated or haven't been ticked
					//have higher priority
					if (!column.isLightPopulated() && column.isTerrainPopulated()) {
						return column;
					}
					if (!column.isChunkTicked()) {
						return column;
					}
					//is there any non-spectator player within 128 blocks distance?
					if (watcher.hasPlayerMatchingInRange(128.0D, NOT_SPECTATOR::test)) {
						return column;
					}
				}
				return this.endOfData();
			}
		};
	}

	/**
	 * Updates all CubeWatchers and ColumnWatchers.
	 * Also sends packets to clients.
	 */
	@Override
	public void tick() {
		long currentTime = this.getWorldServer().getTotalWorldTime();

		//force update-all every 8000 ticks (400 seconds)
		if (currentTime - this.previousWorldTime > 8000L) {
			this.previousWorldTime = currentTime;

			for (PlayerCubeMapEntry playerInstance : this.cubeWatchers.valueCollection()) {
				playerInstance.update();
				playerInstance.updateInhabitedTime();
			}
		}

		//process instances to update
		for (PlayerCubeMapEntry playerInstance : this.cubeWatchersToUpdate) {
			playerInstance.update();
		}
		this.cubeWatchersToUpdate.clear();

		//sort toLoadPending of needed, but at most every 4 ticks
		if (this.toGenerateNeedSort && currentTime%4L == 0L) {
			this.toGenerateNeedSort = false;
			Collections.sort(this.toGenerate, (watcher1, watcher2) ->
					ComparisonChain.start().compare(
							watcher1.getClosestPlayerDistance(),
							watcher2.getClosestPlayerDistance()
					).result());
		}

		//sort toSendToClient every other 4 ticks
		if (this.toSendToClientNeedSort && currentTime%4L == 2L) {
			this.toSendToClientNeedSort = false;
			Collections.sort(this.toSendToClient, (watcher1, watcher2) ->
					ComparisonChain.start().compare(
							watcher1.getClosestPlayerDistance(),
							watcher2.getClosestPlayerDistance()
					).result());
		}

		if (!this.toGenerate.isEmpty()) {
			long stopTime = System.nanoTime() + 50000000L;
			int maxChunksToLoad = 49;
			Iterator<PlayerCubeMapEntry> iterator = this.toGenerate.iterator();

			while (iterator.hasNext()) {
				PlayerCubeMapEntry watcher = iterator.next();

				if (watcher.getCube() == null) {
					boolean flag = watcher.hasPlayerMatching(CAN_GENERATE_CHUNKS);

					if (watcher.providePlayerCube(flag)) {
						iterator.remove();

						if (watcher.sendToPlayers()) {
							this.toSendToClient.remove(watcher);
						}

						--maxChunksToLoad;

						if (maxChunksToLoad < 0 || System.nanoTime() > stopTime) {
							break;
						}
					}
				}
			}
		}

		//actually this will only add then to a queue
		if (!this.toSendToClient.isEmpty()) {
			int toSend = 81*8;//sending cubes, so  send 8x more at once
			Iterator<PlayerCubeMapEntry> it = this.toSendToClient.iterator();

			while (it.hasNext() && toSend >= 0) {
				PlayerCubeMapEntry playerInstance = it.next();

				if (playerInstance.sendToPlayers()) {
					it.remove();
					--toSend;
				}
			}
		}

		//if there are no players - unload everything
		if (this.players.isEmpty()) {
			WorldProvider worldprovider = this.getWorldServer().provider;

			if (!worldprovider.canRespawnHere()) {
				this.getWorldServer().getChunkProvider().unloadAllChunks();
			}
		}
	}

	@Override
	public boolean contains(int cubeX, int cubeZ) {
		return this.columnWatchers.containsKey(getAddress(cubeX, cubeZ));
	}

	@Override
	public PlayerChunkMapEntry getEntry(int cubeX, int cubeZ) {
		return this.columnWatchers.get(getAddress(cubeX, cubeZ));
	}

	/**
	 * Returns existing CubeWatcher or creates new one if it doesn't exist.
	 * Attempts to load the cube and send it to client.
	 * If it can't load it or send it to client - adds it to toGenerate/toSendToClient
	 */
	private PlayerCubeMapEntry getOrCreateCubeWatcher(long cubeAddress) {
		PlayerCubeMapEntry cubeWatcher = this.cubeWatchers.get(cubeAddress);

		if (cubeWatcher == null) {
			int cubeX = getX(cubeAddress);
			int cubeY = getY(cubeAddress);
			int cubeZ = getZ(cubeAddress);
			// make a new watcher
			cubeWatcher = new PlayerCubeMapEntry(this, cubeX, cubeY, cubeZ);
			this.cubeWatchers.put(cubeAddress, cubeWatcher);
			if (cubeWatcher.getCube() == null) {
				this.toGenerate.add(cubeWatcher);
			}
			if (!cubeWatcher.sendToPlayers()) {
				this.toSendToClient.add(cubeWatcher);
			}
		}
		return cubeWatcher;
	}

	/**
	 * Returns existing PlayerCubeMapColumnEntry or creates new one if it doesn't exist.
	 * Always creates the Column.
	 */
	private PlayerCubeMapColumnEntry getOrCreateColumnWatcher(long columnAddress) {
		PlayerCubeMapColumnEntry playerCubeMapColumnEntry = this.columnWatchers.get(columnAddress);
		if (playerCubeMapColumnEntry == null) {
			int cubeX = getX(columnAddress);
			int cubeZ = getZ(columnAddress);
			playerCubeMapColumnEntry = new PlayerCubeMapColumnEntry(this, cubeX, cubeZ);
			playerCubeMapColumnEntry.sentToPlayers();
			this.columnWatchers.put(columnAddress, playerCubeMapColumnEntry);
		}
		return playerCubeMapColumnEntry;
	}

	@Override
	public void markBlockForUpdate(BlockPos pos) {
		int cubeX = blockToCube(pos.getX());
		int cubeY = blockToCube(pos.getY());
		int cubeZ = blockToCube(pos.getZ());
		PlayerCubeMapEntry cubeWatcher = this.getCubeWatcher(cubeX, cubeY, cubeZ);

		if (cubeWatcher != null) {
			int localX = blockToLocal(pos.getX());
			int localY = blockToLocal(pos.getY());
			int localZ = blockToLocal(pos.getZ());
			cubeWatcher.setDirtyBlock(localX, localY, localZ);
		}
	}

	@Override
	public void addPlayer(EntityPlayerMP player) {
		PlayerWrapper playerWrapper = new PlayerWrapper(player);

		this.players.put(player.getEntityId(), playerWrapper);
		playerWrapper.updateManagedPos();

		int blockX = floor_double(player.posX);
		int blockY = floor_double(player.posY);
		int blockZ = floor_double(player.posZ);
		int cubeX = blockToCube(blockX);
		int cubeY = blockToCube(blockY);
		int cubeZ = blockToCube(blockZ);
		long address = AddressTools.getAddress(cubeX, cubeY, cubeZ);

		this.cubeSelector.forAllVisibleFrom(address, viewDistance, (currentAddress) -> {
			//create cubeWatcher and chunkWatcher
			//order is important
			PlayerCubeMapColumnEntry chunkWatcher = getOrCreateColumnWatcher(cubeToColumn(currentAddress));
			PlayerCubeMapEntry cubeWatcher = getOrCreateCubeWatcher(currentAddress);

			//and add the player to them
			if (!chunkWatcher.containsPlayer(player)) {
				chunkWatcher.addPlayer(player);
			}
			assert !cubeWatcher.containsPlayer(player);
			cubeWatcher.addPlayer(player);
		});
	}

	@Override
	public void removePlayer(EntityPlayerMP player) {
		PlayerWrapper playerWrapper = this.players.get(player.getEntityId());

		int cubeX = blockToCube(playerWrapper.getManagedPosX());
		int cubeY = blockToCube(playerWrapper.getManagedPosY());
		int cubeZ = blockToCube(playerWrapper.getManagedPosZ());

		long cubeAddress = getAddress(cubeX, cubeY, cubeZ);

		this.cubeSelector.forAllVisibleFrom(cubeAddress, viewDistance, (address) -> {
			// skip non-existent cubes
			if (!cubeExists(address)) {
				return;
			}

			// get the watcher
			PlayerCubeMapEntry watcher = getCubeWatcher(address);
			if (watcher == null) {
				CubicChunks.LOGGER.warn(
						"Found existing cube with no cube watcher that should be watched by a player at " + new CubeCoords(address));
				return;
			}

			// remove from the watcher, it also removes the watcher if it becomes empty
			watcher.removePlayer(player);

			// remove column watchers if needed
			address = cubeToColumn(address);
			PlayerCubeMapColumnEntry playerCubeMapColumnEntry = getColumnWatcher(address);
			if (playerCubeMapColumnEntry == null) {
				return;
			}

			if (playerCubeMapColumnEntry.containsPlayer(player)) {
				playerCubeMapColumnEntry.removePlayer(player);
			}
		});
		this.players.remove(player.getEntityId());
		this.setNeedSort();
	}

	@Override
	public void updateMountedMovingPlayer(EntityPlayerMP player) {
		// the player moved
		// if the player moved into a new chunk, update which chunks the player needs to know about
		// then update the list of chunks that need to be sent to the client

		// get the player info
		PlayerWrapper playerWrapper = this.players.get(player.getEntityId());

		// did the player move far enough to matter?
		double blockDX = player.posX - playerWrapper.getManagedPosX();
		double blockDY = player.posY - playerWrapper.getManagedPosY();
		double blockDZ = player.posZ - playerWrapper.getManagedPosZ();

		double distanceSquared = blockDX*blockDX + blockDY*blockDY + blockDZ*blockDZ;
		if (distanceSquared < 64.0D) {
			return;
		}

		// did the player move into a new cube?
		int newCubeX = getCubeXForEntity(player);
		int newCubeY = getCubeYForEntity(player);
		int newCubeZ = getCubeZForEntity(player);

		long newAddress = getAddress(newCubeX, newCubeY, newCubeZ);

		int oldCubeX = blockToCube(playerWrapper.getManagedPosX());
		int oldCubeY = blockToCube(playerWrapper.getManagedPosY());
		int oldCubeZ = blockToCube(playerWrapper.getManagedPosZ());

		long oldAddress = getAddress(oldCubeX, oldCubeY, oldCubeZ);

		if (newAddress == oldAddress) {
			return;
		}

		this.updatePlayer(playerWrapper, oldAddress, newAddress);
		playerWrapper.updateManagedPos();
		this.setNeedSort();
	}

	private void updatePlayer(PlayerWrapper entry, long oldAddress, long newAddress) {
		TLongSet cubesToRemove = new TLongHashSet();
		TLongSet cubesToLoad = new TLongHashSet();
		TLongSet columnsToRemove = new TLongHashSet();
		TLongSet columnsToLoad = new TLongHashSet();
		// calculate new visibility
		this.cubeSelector.findChanged(oldAddress, newAddress, viewDistance, cubesToRemove, cubesToLoad, columnsToRemove, columnsToLoad);

		//order is important, columns first
		columnsToLoad.forEach(address -> {
			this.getOrCreateColumnWatcher(address).addPlayer(entry.playerEntity);
			return true;
		});
		cubesToLoad.forEach(address -> {
			this.getOrCreateCubeWatcher(address).addPlayer(entry.playerEntity);
			return true;
		});

		cubesToRemove.forEach(address -> {
			PlayerCubeMapEntry cubeWatcher = this.getCubeWatcher(address);
			if (cubeWatcher != null) {
				cubeWatcher.removePlayer(entry.playerEntity);
			}
			return true;
		});
		columnsToRemove.forEach(address -> {
			PlayerCubeMapColumnEntry playerCubeMapColumnEntry = this.getColumnWatcher(cubeToColumn(address));
			if (playerCubeMapColumnEntry != null) {
				playerCubeMapColumnEntry.removePlayer(entry.playerEntity);
			}
			return true;
		});
	}

	@Override
	public boolean isPlayerWatchingChunk(@Nonnull EntityPlayerMP player, int cubeX, int cubeZ) {
		PlayerCubeMapColumnEntry playerCubeMapColumnEntry = this.getColumnWatcher(getAddress(cubeX, cubeZ));
		return playerCubeMapColumnEntry != null &&
				playerCubeMapColumnEntry.containsPlayer(player) &&
				playerCubeMapColumnEntry.isSentToPlayers();
	}

	@Override
	public final void setPlayerViewRadius(int newViewDistance) {
		//this method is called by vanilla before these fields are initialized.
		//and it doesn't really need to be called because in this case
		//it reduces to setting the viewRadius field
		if (this.players == null) {
			return;
		}
		newViewDistance = clamp_int(newViewDistance, 3, 32);

		if (newViewDistance == this.viewDistance) {
			return;
		}
		int oldViewDistance = this.viewDistance;

		for (PlayerWrapper playerWrapper : this.players.valueCollection()) {
			EntityPlayerMP player = playerWrapper.playerEntity;

			int playerCubeX = blockToCube(playerWrapper.getManagedPosX());
			int playerCubeY = blockToCube(playerWrapper.getManagedPosY());
			int playerCubeZ = blockToCube(playerWrapper.getManagedPosZ());

			long playerAddress = getAddress(playerCubeX, playerCubeY, playerCubeZ);

			if (newViewDistance > oldViewDistance) {
				//if newRadius is bigger, we only need to load new cubes
				this.cubeSelector.forAllVisibleFrom(playerAddress, newViewDistance, address -> {
					//order is important
					PlayerCubeMapColumnEntry playerCubeMapColumnEntry = this.getOrCreateColumnWatcher(cubeToColumn(address));
					if (!playerCubeMapColumnEntry.containsPlayer(player)) {
						playerCubeMapColumnEntry.addPlayer(player);
					}
					PlayerCubeMapEntry cubeWatcher = this.getOrCreateCubeWatcher(address);
					if (!cubeWatcher.containsPlayer(player)) {
						cubeWatcher.addPlayer(player);
					}
				});
			} else {
				//if it got smaller...
				TLongSet cubesToUnload = new TLongHashSet();
				TLongSet columnsToUnload = new TLongHashSet();
				this.cubeSelector.findAllUnloadedOnViewDistanceDecrease(playerAddress, oldViewDistance, newViewDistance, cubesToUnload, columnsToUnload);

				cubesToUnload.forEach(address -> {
					PlayerCubeMapEntry cubeWatcher = this.getCubeWatcher(address);
					if (cubeWatcher != null && cubeWatcher.containsPlayer(player)) {
						cubeWatcher.removePlayer(player);
					} else {
						CubicChunks.LOGGER.warn("cubeWatcher null or doesn't contain player on render distance change");
					}
					return true;
				});
				columnsToUnload.forEach(address -> {
					PlayerCubeMapColumnEntry playerCubeMapColumnEntry = this.getColumnWatcher(cubeToColumn(address));
					if (playerCubeMapColumnEntry != null && playerCubeMapColumnEntry.containsPlayer(player)) {
						playerCubeMapColumnEntry.removePlayer(player);
					} else {
						CubicChunks.LOGGER.warn("cubeWatcher null or doesn't contain player on render distance change");
					}
					return true;
				});
			}
		}

		this.viewDistance = newViewDistance;
		this.setNeedSort();
	}

	private void setNeedSort() {
		this.toGenerateNeedSort = true;
		this.toSendToClientNeedSort = true;
	}

	@Override
	public void addEntry(@Nonnull PlayerChunkMapEntry entry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeEntry(PlayerChunkMapEntry entry) {
		throw new UnsupportedOperationException();
	}

	void addToUpdateEntry(PlayerCubeMapEntry cubeWatcher) {
		this.cubeWatchersToUpdate.add(cubeWatcher);
	}

	void removeEntry(PlayerCubeMapEntry cubeWatcher) {
		long address = cubeWatcher.getCubeAddress();
		cubeWatcher.updateInhabitedTime();
		this.cubeWatchers.remove(address);
		this.cubeWatchersToUpdate.remove(cubeWatcher);
		this.toGenerate.remove(cubeWatcher);
		this.toSendToClient.remove(cubeWatcher);
		//don't unload, ChunkGc unloads chunks
	}

	public void removeEntry(PlayerCubeMapColumnEntry entry) {
		ChunkPos pos = entry.getPos();
		long address = getAddress(pos.chunkXPos, pos.chunkZPos);
		entry.updateChunkInhabitedTime();
		this.columnWatchers.remove(address);
	}

	public PlayerCubeMapEntry getCubeWatcher(CubeCoords pos) {
		return getCubeWatcher(pos.getAddress());
	}

	public PlayerCubeMapEntry getCubeWatcher(int cubeX, int cubeY, int cubeZ) {
		return getCubeWatcher(AddressTools.getAddress(cubeX, cubeY, cubeZ));
	}

	private PlayerCubeMapEntry getCubeWatcher(long address) {
		return this.cubeWatchers.get(address);
	}

	public PlayerCubeMapColumnEntry getColumnWatcher(long address) {
		return this.columnWatchers.get(address);
	}

	private boolean cubeExists(long address) {
		int cubeX = getX(address);
		int cubeY = getY(address);
		int cubeZ = getZ(address);
		return cubeCache.cubeExists(cubeX, cubeY, cubeZ);
	}

	public ICubicWorldServer getWorld() {
		return (ICubicWorldServer) this.getWorldServer();
	}

	public boolean contains(CubeCoords coords) {
		return this.cubeWatchers.containsKey(coords.getAddress());
	}

	private static final class PlayerWrapper {
		final EntityPlayerMP playerEntity;
		private double managedPosY;

		PlayerWrapper(EntityPlayerMP player) {
			this.playerEntity = player;
		}

		void updateManagedPos() {
			this.playerEntity.managedPosX = playerEntity.posX;
			this.managedPosY = playerEntity.posY;
			this.playerEntity.managedPosZ = playerEntity.posZ;
		}

		double getManagedPosX() {
			return this.playerEntity.managedPosX;
		}

		double getManagedPosY() {
			return this.managedPosY;
		}

		double getManagedPosZ() {
			return this.playerEntity.managedPosZ;
		}
	}
}