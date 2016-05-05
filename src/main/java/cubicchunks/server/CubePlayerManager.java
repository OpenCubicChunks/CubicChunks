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

import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ComparisonChain;
import cubicchunks.CubicChunks;
import cubicchunks.network.*;
import cubicchunks.util.AddressTools;
import cubicchunks.visibility.CubeSelector;
import cubicchunks.visibility.CuboidalCubeSelector;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.function.Predicate;

import static cubicchunks.server.ServerCubeCache.LoadType.LOAD_ONLY;
import static cubicchunks.server.ServerCubeCache.LoadType.LOAD_OR_GENERATE;
import static cubicchunks.util.AddressTools.*;
import static cubicchunks.util.Coords.*;
import static cubicchunks.util.ReflectionUtil.getFieldGetterHandle;
import static cubicchunks.util.ReflectionUtil.getFieldSetterHandle;
import static net.minecraft.util.math.MathHelper.clamp_int;
import static net.minecraft.util.math.MathHelper.floor_double;

/**
 * A cubic chunks implementation of Player Manager.
 * <p>
 * This class manages loading and unloading cubes for players.
 */
public class CubePlayerManager extends PlayerManager {

	private static final Predicate<EntityPlayerMP> NOT_SPECTATOR = player -> player != null && !player.isSpectator();
	private static final Predicate<EntityPlayerMP> CAN_GENERATE_CHUNKS = player -> player != null &&
			(!player.isSpectator() || player.getServerWorld().getGameRules().getBoolean("spectatorsGenerateChunks"));

	private final Logger LOGGER = CubicChunks.LOGGER;

	/**
	 * Cube selector is used to find whuch cube positions need to be loaded/unloaded
	 * By default use CuboidalCubeSelector.
	 */
	private final CubeSelector cubeSelector = new CuboidalCubeSelector();

	/**
	 * Mapping if entityId to CubePlayerManager.PlayerWrapper objects.
	 */
	private final TIntObjectMap<PlayerWrapper> players = new TIntObjectHashMap<>();

	/**
	 * Mapping of Cube addresses to CubeWatchers (Cube equivalent of PlayerManager.PlayerInstance).
	 * Contains cube addresses of all cubes loaded by players.
	 */
	private final TLongObjectMap<CubeWatcher> cubeWatchers = new TLongObjectHashMap<>();

	/**
	 * Mapping of Column addresses to ColumnWatchers.
	 * Contains column addresses of all columns loaded by players.
	 * Exists for compatibility with vanilla and to send ColumnLoad/Unload packets to clients.
	 * Columns cannot be managed by client because they have separate data, like heightmap and biome array.
	 */
	private final TLongObjectMap<ColumnWatcher> columnWatchers = new TLongObjectHashMap<>();

	/**
	 * All cubeWatchers that have pending block updates to send.
	 */
	private final Set<CubeWatcher> cubeWatchersToUpdate = new HashSet<>();

	/**
	 * Contains all CubeWatchers that need to be sent to clients,
	 * but these cubes are not fully loaded/generated yet.
	 * <p>
	 * Note that this is not the same as toGenerate list.
	 * Cube can be loaded while not being fully generated yet (not in the last GeneratorPipeline stage).
	 */
	private final List<CubeWatcher> toSendToClient = new ArrayList<>();

	/**
	 * Contains all CubeWatchers that still need to be loaded/generated.
	 * CubeWatcher constructor attempts to load cube from disk, but it won't generate it.
	 * Technically it can generate it, especially with GeneratorPipeline
	 * but spectator players can't generate chunks if spectatorsGenerateChunks gamerule is set.
	 */
	private final List<CubeWatcher> toGenerate = new ArrayList<>();

	private int viewDistance;

	/**
	 * This is used only to force update of all CubeWatchers every 8000 ticks
	 */
	private long previousWorldTime = 0;

	private boolean toGenerateNeedSort = true;
	private boolean toSendToClientNeedSort = true;

	private ServerCubeCache cubeCache;

	public CubePlayerManager(ICubicWorldServer worldServer) {
		super((WorldServer) worldServer);
		this.cubeCache = (ServerCubeCache) getWorldServer().getChunkProvider();
		//this.viewDistance = worldServer.getMinecraftServer().getPlayerList().getViewDistance();
		this.setPlayerViewRadius(worldServer.getMinecraftServer().getPlayerList().getViewDistance());
	}

	/**
	 * This method exists only because vanilla needs it. It shouldn't be used anywhere else.
	 */
	@Override
	@Deprecated
	public Iterator<Chunk> getChunkIterator() {
		final Iterator<ColumnWatcher> iterator = this.columnWatchers.valueCollection().iterator();
		return new AbstractIterator<Chunk>() {
			protected Chunk computeNext() {
				while (iterator.hasNext()) {
					ColumnWatcher watcher = iterator.next();
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
	public void updatePlayerInstances() {
		long currentTime = this.getWorldServer().getTotalWorldTime();

		//force update-all every 8000 ticks (400 seconds)
		if (currentTime - this.previousWorldTime > 8000L) {
			this.previousWorldTime = currentTime;

			for (CubeWatcher playerInstance : this.cubeWatchers.valueCollection()) {
				playerInstance.update();
				playerInstance.updateInhabitedTime();
			}
		}

		//process instances to update
		for (CubeWatcher playerInstance : this.cubeWatchersToUpdate) {
			playerInstance.update();
		}
		this.cubeWatchersToUpdate.clear();

		//sort toLoadPending of needed, but at most every 4 ticks
		if (this.toGenerateNeedSort && currentTime % 4L == 0L) {
			this.toGenerateNeedSort = false;
			Collections.sort(this.toGenerate, (watcher1, watcher2) ->
					ComparisonChain.start().compare(
							watcher1.getClosestPlayerDistance(),
							watcher2.getClosestPlayerDistance()
					).result());
		}

		//sort toSendToClient every other 4 ticks
		if (this.toSendToClientNeedSort && currentTime % 4L == 2L) {
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
			Iterator<CubeWatcher> iterator = this.toGenerate.iterator();

			while (iterator.hasNext()) {
				CubeWatcher watcher = iterator.next();

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
			int toSend = 81 * 8;//sending cubes, so  send 8x more at once
			Iterator<CubeWatcher> it = this.toSendToClient.iterator();

			while (it.hasNext() && toSend >= 0) {
				CubeWatcher playerInstance = it.next();

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
	public boolean hasPlayerInstance(int cubeX, int cubeZ) {
		return this.columnWatchers.containsKey(getAddress(cubeX, cubeZ));
	}

	@Override
	public PlayerManager.PlayerInstance getEntry(int cubeX, int cubeZ) {
		return this.columnWatchers.get(getAddress(cubeX, cubeZ));
	}

	/**
	 * Returns existing CubeWatcher or creates new one if it doesn't exist.
	 * Attempts to load the cube and send it to client.
	 * If it can't load it or send it to client - adds it to toGenerate/toSendToClient
	 */
	private CubeWatcher getOrCreateCubeWatcher(long cubeAddress) {
		CubeWatcher cubeWatcher = this.cubeWatchers.get(cubeAddress);

		if (cubeWatcher == null) {
			int cubeX = getX(cubeAddress);
			int cubeY = getY(cubeAddress);
			int cubeZ = getZ(cubeAddress);
			// make a new watcher
			cubeWatcher = new CubeWatcher(cubeX, cubeY, cubeZ);
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
	 * Returns existing ColumnWatcher or creates new one if it doesn't exist.
	 * Always creates the Column.
	 */
	private ColumnWatcher getOrCreateColumnWatcher(long columnAddress) {
		ColumnWatcher columnWatcher = this.columnWatchers.get(columnAddress);
		if (columnWatcher == null) {
			int cubeX = getX(columnAddress);
			int cubeZ = getZ(columnAddress);
			columnWatcher = new ColumnWatcher(cubeX, cubeZ);
			columnWatcher.sentToPlayers();
			this.columnWatchers.put(columnAddress, columnWatcher);
		}
		return columnWatcher;
	}

	@Override
	public void markBlockForUpdate(BlockPos pos) {
		int cubeX = blockToCube(pos.getX());
		int cubeY = blockToCube(pos.getY());
		int cubeZ = blockToCube(pos.getZ());
		CubeWatcher cubeWatcher = this.getCubeWatcher(cubeX, cubeY, cubeZ);

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
			CubeWatcher cubeWatcher = getOrCreateCubeWatcher(currentAddress);
			ColumnWatcher chunkWatcher = getOrCreateColumnWatcher(cubeToColumn(currentAddress));
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
			CubeWatcher watcher = getCubeWatcher(address);
			if (watcher == null) {
				CubicChunks.LOGGER.warn("Found existing cube with no cube watcher that should be watched by a player at address " +
						String.format("0x%16x", address) + ". Possible memory leak. Forcing unload");
				cubeCache.unloadCube(getX(address), getY(address), getZ(address));
				return;
			}

			// remove from the watcher, it also removes the watcher if it becomes empty
			watcher.removePlayer(player);

			// remove column watchers if needed
			address = cubeToColumn(address);
			ColumnWatcher columnWatcher = getColumnWatcher(address);
			if(columnWatcher == null) {
				return;
			}

			if (columnWatcher.containsPlayer(player)) {
				columnWatcher.removePlayer(player);
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

		double distanceSquared = blockDX * blockDX + blockDY * blockDY + blockDZ * blockDZ;
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

		columnsToLoad.forEach(address -> {
			this.getOrCreateColumnWatcher(address).addPlayer(entry.playerEntity);
			return true;
		});
		cubesToLoad.forEach(address -> {
			this.getOrCreateCubeWatcher(address).addPlayer(entry.playerEntity);
			return true;
		});

		cubesToRemove.forEach(address -> {
			CubeWatcher cubeWatcher = this.getCubeWatcher(address);
			if (cubeWatcher != null) {
				cubeWatcher.removePlayer(entry.playerEntity);
			}
			return true;
		});
		columnsToRemove.forEach(address -> {
			ColumnWatcher columnWatcher = this.getColumnWatcher(cubeToColumn(address));
			if (columnWatcher != null) {
				columnWatcher.removePlayer(entry.playerEntity);
			}
			return true;
		});
	}

	@Override
	public boolean isPlayerWatchingChunk(EntityPlayerMP player, int cubeX, int cubeZ) {
		ColumnWatcher columnWatcher = this.getColumnWatcher(getAddress(cubeX, cubeZ));
		return columnWatcher != null &&
				columnWatcher.containsPlayer(player) &&
				columnWatcher.isSentToPlayers();
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
					CubeWatcher cubeWatcher = this.getOrCreateCubeWatcher(address);
					if (!cubeWatcher.containsPlayer(player)) {
						cubeWatcher.addPlayer(player);
					}
					ColumnWatcher columnWatcher = this.getOrCreateColumnWatcher(cubeToColumn(address));
					if (!columnWatcher.containsPlayer(player)) {
						columnWatcher.addPlayer(player);
					}
				});
			} else {
				//if it got smaller...
				TLongSet cubesToUnload = new TLongHashSet();
				TLongSet columnsToUnload = new TLongHashSet();
				this.cubeSelector.findAllUnloadedOnViewDistanceDecrease(playerAddress, oldViewDistance, newViewDistance, cubesToUnload, columnsToUnload);

				cubesToUnload.forEach(address -> {
					CubeWatcher cubeWatcher = this.getCubeWatcher(address);
					if (cubeWatcher != null && cubeWatcher.containsPlayer(player)) {
						cubeWatcher.removePlayer(player);
					}else{CubicChunks.LOGGER.warn("cubeWatcher null or doesn't contain player on render distance change");}
					return true;
				});
				columnsToUnload.forEach(address -> {
					ColumnWatcher columnWatcher = this.getColumnWatcher(cubeToColumn(address));
					if (columnWatcher != null && columnWatcher.containsPlayer(player)) {
						columnWatcher.removePlayer(player);
					}else{CubicChunks.LOGGER.warn("cubeWatcher null or doesn't contain player on render distance change");}
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
	public void addEntry(PlayerManager.PlayerInstance entry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeEntry(PlayerManager.PlayerInstance entry) {
		throw new UnsupportedOperationException();
	}

	private void addToUpdateEntry(CubeWatcher cubeWatcher) {
		this.cubeWatchersToUpdate.add(cubeWatcher);
	}

	private void removeEntry(CubeWatcher cubeWatcher) {
		long address = cubeWatcher.getCubeAddress();
		cubeWatcher.updateInhabitedTime();
		this.cubeWatchers.remove(address);
		this.cubeWatchersToUpdate.remove(cubeWatcher);
		this.toGenerate.remove(cubeWatcher);
		this.toSendToClient.remove(cubeWatcher);
		this.cubeCache.unloadCube(getX(address), getY(address), getZ(address));
	}

	public void removeEntry(CubePlayerManager.ColumnWatcher entry) {
		ChunkCoordIntPair chunkcoordintpair = entry.getPos();
		long address = getAddress(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
		entry.updateChunkInhabitedTime();
		this.columnWatchers.remove(address);
	}

	private CubeWatcher getCubeWatcher(int cubeX, int cubeY, int cubeZ) {
		return getCubeWatcher(AddressTools.getAddress(cubeX, cubeY, cubeZ));
	}

	private CubeWatcher getCubeWatcher(long address) {
		return this.cubeWatchers.get(address);
	}

	private ColumnWatcher getColumnWatcher(long address) {
		return this.columnWatchers.get(address);
	}

	private boolean cubeExists(long address) {
		int cubeX = getX(address);
		int cubeY = getY(address);
		int cubeZ = getZ(address);
		return cubeCache.cubeExists(cubeX, cubeY, cubeZ);
	}

	public class CubeWatcher {

		private Cube cube;
		private TIntObjectMap<WatcherPlayerEntry> players;
		private SortedSet<Integer> dirtyBlocks;
		private long cubeAddress;
		private long previousWorldTime;
		private boolean sentToPlayers;

		public CubeWatcher(int cubeX, int cubeY, int cubeZ) {
			CubePlayerManager.this.cubeCache.loadCube(cubeX, cubeY, cubeZ, LOAD_ONLY);
			this.cube = CubePlayerManager.this.cubeCache.getCube(cubeX, cubeY, cubeZ);
			this.players = new TIntObjectHashMap<>();
			this.previousWorldTime = 0;
			this.dirtyBlocks = new TreeSet<>();
			this.sentToPlayers = false;
			this.cubeAddress = getAddress(cubeX, cubeY, cubeZ);
		}

		public void addPlayer(EntityPlayerMP player) {
			if (this.players.containsKey(player.getEntityId())) {
				CubicChunks.LOGGER.debug("Failed to add player. {} already is in cube at address {}", new Object[]{player, String.format("0x%016x", cubeAddress)});
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
						//TODO: ChunkIOExecutor.dropQueuedChunkLoad: Is it needed? Should it be ported to CubicChunks?
						//net.minecraftforge.common.chunkio.ChunkIOExecutor.dropQueuedChunkLoad(PlayerManager.this.getWorldServer(), this.pos.chunkXPos, this.pos.chunkZPos, this.loadedRunnable);
						CubePlayerManager.this.removeEntry(this);
					}
					return;
				}

				if (this.sentToPlayers) {
					PacketDispatcher.sendTo(new PacketUnloadCube(this.cubeAddress), player);
					//player.playerNetServerHandler.sendPacket(new SPacketUnloadChunk(this.pos.chunkXPos, this.pos.chunkZPos));
				}

				this.players.remove(player.getEntityId());
				//TODO: Cube unwatch event
				//net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkWatchEvent.UnWatch(this.pos, player));

				if (this.players.isEmpty()) {
					CubePlayerManager.this.removeEntry(this);
				}
			}
		}

		public boolean providePlayerCube(boolean canGenerate) {
			if (this.cube != null) {
				return true;
			}
			int cubeX = getX(cubeAddress);
			int cubeY = getY(cubeAddress);
			int cubeZ = getZ(cubeAddress);

			if (canGenerate) {
				CubePlayerManager.this.cubeCache.loadCube(cubeX, cubeY, cubeZ, LOAD_OR_GENERATE);
			} else {
				CubePlayerManager.this.cubeCache.loadCube(cubeX, cubeY, cubeZ, LOAD_ONLY);
			}
			this.cube = CubePlayerManager.this.cubeCache.getCube(cubeX, cubeY, cubeZ);

			return this.cube != null;
		}

		public boolean sendToPlayers() {
			if (this.sentToPlayers) {
				return true;
			}
			if (this.cube == null || !this.cube.getGeneratorStage().isLastStage()) {
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
			PacketDispatcher.sendTo(new PacketCube(this.cube), player);
		}

		public void updateInhabitedTime() {
			final long now = getWorldTime();
			if(this.cube == null) {
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
				CubePlayerManager.this.addToUpdateEntry(this);
			}
			// If the number of changes is above clumpingThreshold
			// we send the whole cube, but to decrease network usage
			// forge sends only TEs that have changed,
			// so we need to know all changed blocks. So add everything
			// it's a set so no need to check for duplicates
			this.dirtyBlocks.add(AddressTools.getLocalAddress(localX, localY, localZ));
		}

		public void update() {

			// are there any updates?
			if (this.dirtyBlocks.isEmpty()) {
				return;
			}

			ICubicWorld world = this.cube.getWorld();

			if (this.dirtyBlocks.size() >= ForgeModContainer.clumpingThreshold) {
				// send whole cube
				sendPacketToAllPlayers(new PacketCubeChange(cube));
			} else {
				// send all the dirty blocks
				sendPacketToAllPlayers(new PacketCubeBlockChange(this.cube, this.dirtyBlocks));
			}

			// send the block entites on those blocks too
			for (int localAddress : this.dirtyBlocks) {
				BlockPos pos = cube.localAddressToBlockPos(localAddress);

				IBlockState state = this.cube.getBlockState(pos);
				if (state.getBlock().hasTileEntity(state)) {
					sendBlockEntityToAllPlayers(world.getTileEntity(pos));
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

		public boolean containsPlayer(EntityPlayerMP player) {
			return this.players.containsKey(player.getEntityId());
		}

		public boolean hasPlayerMatching(Predicate<EntityPlayerMP> predicate) {
			//if any of them is true - stop and return false, then negate the result to get true
			return !this.players.forEachValue(value -> !predicate.test(value.player));
		}

		public boolean hasPlayerMatchingInRange(double range, Predicate<EntityPlayerMP> predicate) {
			//if any of them is true - stop and return false, then negate the result to get true
			return !this.players.forEachValue(v ->
							!(getDistanceSq(cubeAddress, v.player) < range * range && predicate.test(v.player))
			);
		}

		public double getDistanceSq(long cubeAddress, Entity entity) {
			double blockX = localToBlock(getX(cubeAddress), 8);
			double blockY = localToBlock(getY(cubeAddress), 8);
			double blockZ = localToBlock(getZ(cubeAddress), 8);
			double dx = blockX - entity.posX;
			double dy = blockY - entity.posY;
			double dz = blockZ - entity.posZ;
			return dx * dx + dy * dy + dz * dz;
		}

		public boolean isSentToPlayers() {
			return this.sentToPlayers;
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

		public boolean hasPlayers() {
			return !this.players.isEmpty();
		}

		private long getWorldTime() {
			return CubePlayerManager.this.getWorldServer().getWorldTime();
		}

		private void sendPacketToAllPlayers(Packet packet) {
			for (WatcherPlayerEntry entry : this.players.valueCollection()) {
				entry.player.playerNetServerHandler.sendPacket(packet);
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


	public class ColumnWatcher extends PlayerManager.PlayerInstance {

		private MethodHandle getPlayers = getFieldGetterHandle(PlayerManager.PlayerInstance.class, "field_187283_c");
		private MethodHandle setLastUpdateInhabitedTime = getFieldSetterHandle(PlayerManager.PlayerInstance.class, "field_187289_i");
		private MethodHandle setSentToPlayers = getFieldSetterHandle(PlayerManager.PlayerInstance.class, "field_187290_j");

		public ColumnWatcher(int cubeX, int cubeZ) {
			super(cubeX, cubeZ);
			assert this.getColumn() != null;
		}

		public ChunkCoordIntPair getPos() {
			return super.getPos();
		}

		public void addPlayer(EntityPlayerMP player) {
			if (this.getPlayers().contains(player)) {
				LOGGER.debug("Failed to add player. {} already is in chunk {}, {}", new Object[]{player, this.getPos().chunkXPos, this.getPos().chunkZPos});
				return;
			}
			if (this.getPlayers().isEmpty()) {
				this.setLastUpdateInhabitedTime(CubePlayerManager.this.getWorldServer().getTotalWorldTime());
			}

			this.getPlayers().add(player);

			/*
			 * TODO: ChunkWatchEvent.Watch: is it implemented correctly?
			 * TODO: at the moment I'm writing it Forge doesn't have this implemented, I think it should be here:
			 */
			MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(this.getPos(), player));

			//always sent to players
			//if (this.isSentToPlayers()) {
			PacketDispatcher.sendTo(new PacketColumn(this.getColumn()), player);
			CubePlayerManager.this.getWorldServer().getEntityTracker().sendLeashedEntitiesInChunk(player, this.getColumn());
			//}
		}

		public void removePlayer(EntityPlayerMP player) {
			if (!this.getPlayers().contains(player)) {
				return;
			}
			//columns for ColumnWatchers are always loaded with CubicChunks
			assert this.getColumn() != null : "Column not loaded!";

			if (this.isSentToPlayers()) {
				long cubeAddress = getAddress(this.getColumn().xPosition, this.getColumn().zPosition);
				PacketDispatcher.sendTo(new PacketUnloadColumn(cubeAddress), player);
			}

			this.getPlayers().remove(player);

			MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(this.getPos(), player));

			if (this.getPlayers().isEmpty()) {
				CubePlayerManager.this.removeEntry(this);
			}
		}

		private List<EntityPlayerMP> getPlayers() {
			try {
				return (List<EntityPlayerMP>) getPlayers.invoke(this);
			} catch (Throwable throwable) {
				throw Throwables.propagate(throwable);
			}
		}

		private void setLastUpdateInhabitedTime(long time) {
			try {
				setLastUpdateInhabitedTime.invoke(this, time);
			} catch (Throwable throwable) {
				throw Throwables.propagate(throwable);
			}
		}

		//procidePlayerChunk - ok

		@Override
		//actually sendToPlayers
		public boolean sentToPlayers() {
			try {
				this.setSentToPlayers.invoke(this, true);
			} catch (Throwable throwable) {
				throw Throwables.propagate(throwable);
			}
			return true;
		}

		@Override
		@Deprecated
		public void sendNearbySpecialEntities(EntityPlayerMP player) {
			//done by cube watcher
		}

		//updateChunkInhabitedTime - ok

		@Override
		@Deprecated
		public void blockChanged(int x, int y, int z) {
			//TODO: call CubeWatcher equivalent
		}

		@Override
		public void update() {

		}

		//containsPlayer, hasPlayerMatching, hasPlayerMatchingInRange, isAddedToChunkUpdateQueue, getChunk, getClosestPlayerDistance - ok

		public boolean hasPlayers() {
			return false;
		}

		public Column getColumn() {
			return (Column) this.getChunk();
		}
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