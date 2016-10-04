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
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ComparisonChain;
import cubicchunks.CubicChunks;
import cubicchunks.IConfigUpdateListener;
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
public class PlayerCubeMap extends PlayerChunkMap implements IConfigUpdateListener {

	private static final Predicate<EntityPlayerMP> NOT_SPECTATOR = player -> player != null && !player.isSpectator();
	private static final Predicate<EntityPlayerMP> CAN_GENERATE_CHUNKS = player -> player != null &&
			(!player.isSpectator() || player.getServerWorld().getGameRules().getBoolean("spectatorsGenerateChunks"));

	/**
	 * Cube selector is used to find which cube positions need to be loaded/unloaded
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
	 * Note that this is not the same as cubesToGenerate list.
	 * Cube can be loaded while not being fully generated yet (not in the last GeneratorStageRegistry stage).
	 */
	private final List<PlayerCubeMapEntry> cubesToSendToClients = new ArrayList<>();

	/**
	 * Contains all CubeWatchers that still need to be loaded/generated.
	 * CubeWatcher constructor attempts to load cube from disk, but it won't generate it.
	 * Technically it can generate it, using the world's IGeneratorPipeline,
	 * but spectator players can't generate chunks if spectatorsGenerateChunks gamerule is set.
	 */
	private final List<PlayerCubeMapEntry> cubesToGenerate = new ArrayList<>();

	/**
	 * Contains all ColumnWatchers that need to be sent to clients,
	 * but these cubes are not fully loaded/generated yet.
	 * <p>
	 * Note that this is not the same as columnsToGenerate list.
	 * Columns can be loaded while not being fully generated yet
	 */
	private final List<PlayerCubeMapColumnEntry> columnsToSendToClients = new ArrayList<>();

	/**
	 * Contains all ColumnWatchers that still need to be loaded/generated.
	 * ColumnWatcher constructor attempts to load column from disk, but it won't generate it.
	 */
	private final List<PlayerCubeMapColumnEntry> columnsToGenerate = new ArrayList<>();

	private int horizontalViewDistance;
	private int verticalViewDistance;
	private volatile int updatedVerticalViewDistance;

	/**
	 * This is used only to force update of all CubeWatchers every 8000 ticks
	 */
	private long previousWorldTime = 0;

	private boolean toGenerateNeedSort = true;
	private boolean toSendToClientNeedSort = true;

	private ServerCubeCache cubeCache;

	private volatile int maxGeneratedCubesPerTick = CubicChunks.Config.DEFAULT_MAX_GENERATED_CUBES_PER_TICK;

	public PlayerCubeMap(ICubicWorldServer worldServer) {
		super((WorldServer) worldServer);
		this.cubeCache = (ServerCubeCache) getWorldServer().getChunkProvider();
		this.setPlayerViewDistance(worldServer.getMinecraftServer().getPlayerList().getViewDistance(), CubicChunks.Config.DEFAULT_VERTICAL_CUBE_LOAD_DISTANCE);
		CubicChunks.addConfigChangeListener(this);
	}

	@Override
	public void onConfigUpdate(CubicChunks.Config config) {
		if(config.getVerticalCubeLoadDistance() != this.verticalViewDistance) {
			this.updatedVerticalViewDistance = config.getVerticalCubeLoadDistance();
		}
		this.maxGeneratedCubesPerTick = config.getMaxGeneratedCubesPerTick();
	}

	/**
	 * This method exists only because vanilla needs it. It shouldn't be used anywhere else.
	 */
	@Override
	@Deprecated // BUG: no vertical filtering! TODO: do filtering and return a sort of 'ColumnView'
	public Iterator<Chunk> getChunkIterator() {
		final Iterator<PlayerCubeMapColumnEntry> iterator = this.columnWatchers.valueCollection().iterator();
		return new AbstractIterator<Chunk>() {
			protected Chunk computeNext() {
				while (iterator.hasNext()) {
					PlayerCubeMapColumnEntry watcher = iterator.next();
					Chunk column = watcher.getChunk();

					// TODO: test Cubes, not Column
					
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
					if (watcher.hasPlayerMatchingInRange(128.0D, NOT_SPECTATOR)) {
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
		if(this.updatedVerticalViewDistance != this.verticalViewDistance) {
			this.setPlayerViewDistance(getWorld().getMinecraftServer().getPlayerList().getViewDistance(), this.updatedVerticalViewDistance);
		}
		getWorld().getProfiler().startSection("playerCubeMapTick");
		long currentTime = this.getWorldServer().getTotalWorldTime();

		getWorld().getProfiler().startSection("tickEntries");
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

		getWorld().getProfiler().endStartSection("sortToGenerate");
		//sort toLoadPending if needed, but at most every 4 ticks
		if (this.toGenerateNeedSort && currentTime%4L == 0L) {
			this.toGenerateNeedSort = false;
			Collections.sort(this.cubesToGenerate, (watcher1, watcher2) ->
					ComparisonChain.start().compare(
							watcher1.getClosestPlayerDistance(),
							watcher2.getClosestPlayerDistance()
					).result());
			// We don't sort columns because we generate all columns
		}
		getWorld().getProfiler().endStartSection("sortToSend");
		//sort cubesToSendToClients every other 4 ticks
		if (this.toSendToClientNeedSort && currentTime%4L == 2L) {
			this.toSendToClientNeedSort = false;
			Collections.sort(this.cubesToSendToClients, (watcher1, watcher2) ->
					ComparisonChain.start().compare(
							watcher1.getClosestPlayerDistance(),
							watcher2.getClosestPlayerDistance()
					).result());
			// We don't sort columns because we send all columns
		}

		getWorld().getProfiler().endStartSection("generate");
		CubicChunks.LOGGER.debug(this.columnsToGenerate.size() + " columns, " + this.cubesToGenerate.size() + " cubes remain to be generated");
		if (!this.columnsToGenerate.isEmpty()) {
			getWorld().getProfiler().startSection("columns");
			Iterator<PlayerCubeMapColumnEntry> iter = this.columnsToGenerate.iterator();
			while (iter.hasNext()) {
				PlayerCubeMapColumnEntry next = iter.next();
				CubicChunks.LOGGER.debug("col[" + next.getPos().chunkXPos + "," + next.getPos().chunkZPos + "]");
				CubicChunks.LOGGER.debug("\tcol state: " + next.getColumn());

				getWorld().getProfiler().startSection("column[" + next.getPos().chunkXPos + "," + next.getPos().chunkZPos + "]");
				boolean success = next.getColumn() != null;
				if (!success) {
					boolean canGenerate = next.hasPlayerMatching(CAN_GENERATE_CHUNKS);
					CubicChunks.LOGGER.debug("\tcan generate: " + canGenerate);
					getWorld().getProfiler().startSection("generate");
					success = next.providePlayerChunk(canGenerate);
					getWorld().getProfiler().endSection(); // generate
				}

				CubicChunks.LOGGER.debug("\tsuccess: " + success);
				if (success) {
					iter.remove();

					if (next.sentToPlayers()) {
						this.columnsToSendToClients.remove(next);
					}
				}

				getWorld().getProfiler().endSection(); // column[x,z]
			}

			getWorld().getProfiler().endSection(); // columns
		}
		if (!this.cubesToGenerate.isEmpty()) {
			getWorld().getProfiler().startSection("cubes");

			long stopTime = System.nanoTime() + 50000000L;
			int chunksToGenerate = maxGeneratedCubesPerTick;
			Iterator<PlayerCubeMapEntry> iterator = this.cubesToGenerate.iterator();

			while (iterator.hasNext() && chunksToGenerate >= 0 && System.nanoTime() < stopTime) {
				PlayerCubeMapEntry watcher = iterator.next();
				long address = watcher.getCubeAddress();
				getWorld().getProfiler()
						.startSection("chunk[" + getX(address) + "," + getY(address) + "," + getZ(address) + "]");

				boolean success = watcher.getCube() != null;
				if (success) {
					boolean canGenerate = watcher.hasPlayerMatching(CAN_GENERATE_CHUNKS);
					getWorld().getProfiler().startSection("generate");
					success = watcher.providePlayerCube(canGenerate);
					getWorld().getProfiler().endSection();
				}

				if (success) {
					iterator.remove();

					if (watcher.sendToPlayers()) {
						this.cubesToSendToClients.remove(watcher);
					}

					--chunksToGenerate;
				}

				getWorld().getProfiler().endSection();//chunk[x, y, z]
			}

			getWorld().getProfiler().endSection(); // chunks
		}
		getWorld().getProfiler().endStartSection("send");
		CubicChunks.LOGGER.debug(this.columnsToSendToClients.size() + " columns, " + this.cubesToSendToClients.size() + " cubes remain to be sent");
		if (!this.columnsToSendToClients.isEmpty()) {
			getWorld().getProfiler().startSection("columns");
			Iterator<PlayerCubeMapColumnEntry> iter = this.columnsToSendToClients.iterator();

			while (iter.hasNext()) {
				PlayerCubeMapColumnEntry next = iter.next();
				if (next.sentToPlayers()) {
					iter.remove();
				}
			}
			getWorld().getProfiler().endSection(); // columns
		}
		if (!this.cubesToSendToClients.isEmpty()) {
			getWorld().getProfiler().startSection("cubes");
			int toSend = 81*8;//sending cubes, so send 8x more at once
			Iterator<PlayerCubeMapEntry> it = this.cubesToSendToClients.iterator();

			while (it.hasNext() && toSend >= 0) {
				PlayerCubeMapEntry playerInstance = it.next();

				if (playerInstance.sendToPlayers()) {
					it.remove();
					--toSend;
				}
			}
			getWorld().getProfiler().endSection(); // cubes
		}

		getWorld().getProfiler().endStartSection("unload");
		//if there are no players - unload everything
		if (this.players.isEmpty()) {
			WorldProvider worldprovider = this.getWorldServer().provider;

			if (!worldprovider.canRespawnHere()) {
				this.getWorldServer().getChunkProvider().unloadAllChunks();
			}
		}
		getWorld().getProfiler().endSection();//unload
		getWorld().getProfiler().endSection();//playerCubeMapTick
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
	 * If it can't load it or send it to client - adds it to cubesToGenerate/cubesToSendToClients
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
				this.cubesToGenerate.add(cubeWatcher);
			}
			if (!cubeWatcher.isSentToPlayers()) {
				this.cubesToSendToClients.add(cubeWatcher);
			}
		}
		return cubeWatcher;
	}

	/**
	 * Returns existing PlayerCubeMapColumnEntry or creates new one if it doesn't exist.
	 * Always creates the Column.
	 */
	private PlayerCubeMapColumnEntry getOrCreateColumnWatcher(long columnAddress) {
		PlayerCubeMapColumnEntry columnWatcher = this.columnWatchers.get(columnAddress);
		if (columnWatcher == null) {
			int cubeX = getX(columnAddress);
			int cubeZ = getZ(columnAddress);
			columnWatcher = new PlayerCubeMapColumnEntry(this, cubeX, cubeZ);
			this.columnWatchers.put(columnAddress, columnWatcher);
			if (columnWatcher.getColumn() == null) {
				this.columnsToGenerate.add(columnWatcher);
			}
			if (!columnWatcher.sentToPlayers()) {
				this.columnsToSendToClients.add(columnWatcher);
			}
		}
		return columnWatcher;
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

		this.cubeSelector.forAllVisibleFrom(address, horizontalViewDistance, verticalViewDistance, (currentAddress) -> {
			//create cubeWatcher and chunkWatcher
			//order is important
			PlayerCubeMapColumnEntry chunkWatcher = getOrCreateColumnWatcher(cubeToColumn(currentAddress));
			//and add the player to them
			if (!chunkWatcher.containsPlayer(player)) {
				chunkWatcher.addPlayer(player);
			}
			PlayerCubeMapEntry cubeWatcher = getOrCreateCubeWatcher(currentAddress);

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

		this.cubeSelector.forAllVisibleFrom(cubeAddress, horizontalViewDistance, verticalViewDistance, (address) -> {
			// skip non-existent cubes
			if (!cubeExists(address)) {
				return;
			}

			// get the watcher
			PlayerCubeMapEntry watcher = getCubeWatcher(address);
			if (watcher == null) {
				CubicChunks.LOGGER.warn(
						"Found existing cube with no cube watcher that should be watched by a player at " +
								new CubeCoords(address));
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
		getWorld().getProfiler().startSection("updateMovedPlayer");
		TLongSet cubesToRemove = new TLongHashSet();
		TLongSet cubesToLoad = new TLongHashSet();
		TLongSet columnsToRemove = new TLongHashSet();
		TLongSet columnsToLoad = new TLongHashSet();

		getWorld().getProfiler().startSection("findChanges");
		// calculate new visibility
		this.cubeSelector.findChanged(oldAddress, newAddress, horizontalViewDistance, verticalViewDistance, cubesToRemove, cubesToLoad, columnsToRemove, columnsToLoad);

		getWorld().getProfiler().endStartSection("createColumns");
		//order is important, columns first
		columnsToLoad.forEach(address -> {
			this.getOrCreateColumnWatcher(address).addPlayer(entry.playerEntity);
			return true;
		});
		getWorld().getProfiler().endStartSection("createCubes");
		cubesToLoad.forEach(address -> {
			this.getOrCreateCubeWatcher(address).addPlayer(entry.playerEntity);
			return true;
		});
		getWorld().getProfiler().endStartSection("removeCubes");
		cubesToRemove.forEach(address -> {
			PlayerCubeMapEntry cubeWatcher = this.getCubeWatcher(address);
			if (cubeWatcher != null) {
				cubeWatcher.removePlayer(entry.playerEntity);
			}
			return true;
		});
		getWorld().getProfiler().endStartSection("removeColumns");
		columnsToRemove.forEach(address -> {
			PlayerCubeMapColumnEntry playerCubeMapColumnEntry = this.getColumnWatcher(cubeToColumn(address));
			if (playerCubeMapColumnEntry != null) {
				playerCubeMapColumnEntry.removePlayer(entry.playerEntity);
			}
			return true;
		});
		getWorld().getProfiler().endSection();//removeColumns
		getWorld().getProfiler().endSection();//updateMovedPlayer
	}

	@Override
	public boolean isPlayerWatchingChunk(@Nonnull EntityPlayerMP player, int cubeX, int cubeZ) {
		PlayerCubeMapColumnEntry playerCubeMapColumnEntry = this.getColumnWatcher(getAddress(cubeX, cubeZ));
		return playerCubeMapColumnEntry != null &&
				playerCubeMapColumnEntry.containsPlayer(player) &&
				playerCubeMapColumnEntry.isSentToPlayers();
	}

	@Override
	@Deprecated
	public final void setPlayerViewRadius(int newHorizontalViewDistance) {
		this.setPlayerViewDistance(newHorizontalViewDistance, verticalViewDistance);
	}

	public final void setPlayerViewDistance(int newHorizontalViewDistance, int newVerticalViewDistance) {
		//this method is called by vanilla before these fields are initialized.
		//and it doesn't really need to be called because in this case
		//it reduces to setting the viewRadius field
		if (this.players == null) {
			return;
		}
		newHorizontalViewDistance = clamp_int(newHorizontalViewDistance, 3, 32);
		newVerticalViewDistance = clamp_int(newVerticalViewDistance, 3, 32);

		if (newHorizontalViewDistance == this.horizontalViewDistance && newVerticalViewDistance == this.verticalViewDistance) {
			return;
		}
		int oldHorizontalViewDistance = this.horizontalViewDistance;
		int oldVerticalViewDistance = this.verticalViewDistance;

		for (PlayerWrapper playerWrapper : this.players.valueCollection()) {
			EntityPlayerMP player = playerWrapper.playerEntity;

			int playerCubeX = blockToCube(playerWrapper.getManagedPosX());
			int playerCubeY = blockToCube(playerWrapper.getManagedPosY());
			int playerCubeZ = blockToCube(playerWrapper.getManagedPosZ());

			long playerAddress = getAddress(playerCubeX, playerCubeY, playerCubeZ);

			// Somehow the view distances went in opposite directions
			if ((newHorizontalViewDistance - oldHorizontalViewDistance < 0 && newHorizontalViewDistance - oldVerticalViewDistance > 0) ||
					(newHorizontalViewDistance - oldHorizontalViewDistance > 0 && newHorizontalViewDistance - oldVerticalViewDistance < 0)) {
				// Adjust the values separately to avoid imploding
				setPlayerViewDistance(newHorizontalViewDistance, oldVerticalViewDistance);
				setPlayerViewDistance(newHorizontalViewDistance, newVerticalViewDistance);
				return;
			} else if (newHorizontalViewDistance > oldHorizontalViewDistance || newVerticalViewDistance > oldVerticalViewDistance) {
				//if newRadius is bigger, we only need to load new cubes
				this.cubeSelector.forAllVisibleFrom(playerAddress, newHorizontalViewDistance, newVerticalViewDistance, address -> {
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
			// Both view distances got smaller
			} else {
				//if it got smaller...
				TLongSet cubesToUnload = new TLongHashSet();
				TLongSet columnsToUnload = new TLongHashSet();
				this.cubeSelector.findAllUnloadedOnViewDistanceDecrease(playerAddress, oldHorizontalViewDistance, newHorizontalViewDistance, oldVerticalViewDistance, newVerticalViewDistance, cubesToUnload, columnsToUnload);

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

		this.horizontalViewDistance = newHorizontalViewDistance;
		this.verticalViewDistance = newVerticalViewDistance;
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
		this.cubesToGenerate.remove(cubeWatcher);
		this.cubesToSendToClients.remove(cubeWatcher);
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