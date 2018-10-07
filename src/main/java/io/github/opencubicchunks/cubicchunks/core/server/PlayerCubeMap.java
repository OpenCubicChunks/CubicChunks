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
package io.github.opencubicchunks.cubicchunks.core.server;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;
import static net.minecraft.util.math.MathHelper.clamp;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.util.XZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.entity.CubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubes;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.util.WatchersSortingList;
import io.github.opencubicchunks.cubicchunks.core.visibility.CubeSelector;
import io.github.opencubicchunks.cubicchunks.core.visibility.CuboidalCubeSelector;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A cubic chunks implementation of Player Manager.
 * <p>
 * This class manages loading and unloading cubes for players.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PlayerCubeMap extends PlayerChunkMap implements LightingManager.IHeightChangeListener {

    private static final Predicate<EntityPlayerMP> NOT_SPECTATOR = player -> player != null && !player.isSpectator();
    private static final Predicate<EntityPlayerMP> CAN_GENERATE_CHUNKS = player -> player != null &&
            (!player.isSpectator() || player.getServerWorld().getGameRules().getBoolean("spectatorsGenerateChunks"));

    /**
     * Comparator that specifies order in which cubes will be generated and sent to clients
     */
    private static final Comparator<CubeWatcher> CUBE_ORDER = (watcher1, watcher2) ->
            ComparisonChain.start().compare(
                    watcher1.getClosestPlayerDistance(),
                    watcher2.getClosestPlayerDistance()
            ).result();

    /**
     * Comparator that specifies order in which columns will be generated and sent to clients
     */
    private static final Comparator<ColumnWatcher> COLUMN_ORDER = (watcher1, watcher2) ->
            ComparisonChain.start().compare(
                    watcher1.getClosestPlayerDistance(),
                    watcher2.getClosestPlayerDistance()
            ).result();

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
     * Mapping of Cube positions to CubeWatchers (Cube equivalent of PlayerManager.PlayerInstance).
     * Contains cube positions of all cubes loaded by players.
     */
    private final XYZMap<CubeWatcher> cubeWatchers = new XYZMap<>(0.7f, 25 * 25 * 25);

    /**
     * Mapping of Column positions to ColumnWatchers.
     * Contains column positions of all columns loaded by players.
     * Exists for compatibility with vanilla and to send ColumnLoad/Unload packets to clients.
     * Columns cannot be managed by client because they have separate data, like heightmap and biome array.
     */
    private final XZMap<ColumnWatcher> columnWatchers = new XZMap<>(0.7f, 25 * 25);

    /**
     * All cubeWatchers that have pending block updates to send.
     */
    private final Set<CubeWatcher> cubeWatchersToUpdate = new HashSet<>();

    /**
     * All columnWatchers that have pending height updates to send.
     */
    private final Set<ColumnWatcher> columnWatchersToUpdate = new HashSet<>();

    /**
     * Contains all CubeWatchers that need to be sent to clients,
     * but these cubes are not fully loaded/generated yet.
     * <p>
     * Note that this is not the same as cubesToGenerate list.
     * Cube can be loaded while not being fully generated yet (not in the last GeneratorStageRegistry stage).
     */
    private final WatchersSortingList<CubeWatcher> cubesToSendToClients = new WatchersSortingList<CubeWatcher>(CUBE_ORDER);

    /**
     * Contains all CubeWatchers that still need to be loaded/generated.
     * CubeWatcher constructor attempts to load cube from disk, but it won't generate it.
     * Technically it can generate it, using the world's IGeneratorPipeline,
     * but spectator players can't generate chunks if spectatorsGenerateChunks gamerule is set.
     */
    private final WatchersSortingList<CubeWatcher> cubesToGenerate = new WatchersSortingList<CubeWatcher>(CUBE_ORDER);

    /**
     * Contains all ColumnWatchers that need to be sent to clients,
     * but these cubes are not fully loaded/generated yet.
     * <p>
     * Note that this is not the same as columnsToGenerate list.
     * Columns can be loaded while not being fully generated yet
     */
    private final WatchersSortingList<ColumnWatcher> columnsToSendToClients = new WatchersSortingList<ColumnWatcher>(COLUMN_ORDER);

    /**
     * Contains all ColumnWatchers that still need to be loaded/generated.
     * ColumnWatcher constructor attempts to load column from disk, but it won't generate it.
     */
    private final WatchersSortingList<ColumnWatcher> columnsToGenerate = new WatchersSortingList<ColumnWatcher>(COLUMN_ORDER);

    private int horizontalViewDistance;
    private int verticalViewDistance;

    /**
     * This is used only to force update of all CubeWatchers every 8000 ticks
     */
    private long previousWorldTime = 0;

    private boolean toGenerateNeedSort = true;
    private boolean toSendToClientNeedSort = true;

    private final CubeProviderServer cubeCache;

    private final Multimap<EntityPlayerMP, Cube> cubesToSend = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);

    // these player adds will be processed on the next tick
    // this exists as temporary workaround to player respawn code calling addPlayer() before spawning
    // the player in world as it's spawning player in world that triggers sending cubic chunks world
    // information to client, this causes the server to send columns to the client before the client
    // knows it's a cubic chunks world delaying addPlayer() by one tick fixes it.
    // this should be fixed by hooking into the code in a different place to send the cubic chunks world information
    // (player respawn packet?)
    private Set<EntityPlayerMP> pendingPlayerAdd = new HashSet<>();

    public PlayerCubeMap(WorldServer worldServer) {
        super((WorldServer) worldServer);
        this.cubeCache = ((ICubicWorldInternal.Server) worldServer).getCubeCache();
        this.setPlayerViewDistance(worldServer.getMinecraftServer().getPlayerList().getViewDistance(),
                ((ICubicPlayerList) worldServer.getMinecraftServer().getPlayerList()).getVerticalViewDistance());
        ((ICubicWorldInternal) worldServer).getLightingManager().registerHeightChangeListener(this);
    }

    /**
     * This method exists only because vanilla needs it. It shouldn't be used anywhere else.
     */
    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    @Deprecated // Warning: Hacks! For vanilla use only! (WorldServer.updateBlocks())
    public Iterator<Chunk> getChunkIterator() {
        // GIVE TICKET SYSTEM FULL CONTROL
        Iterator<Chunk> chunkIt = this.cubeCache.getLoadedChunks().iterator();
        return new AbstractIterator<Chunk>() {
            @Override protected Chunk computeNext() {
                while (chunkIt.hasNext()) {
                    IColumn column = (IColumn) chunkIt.next();
                    if (column.shouldTick()) { // shouldTick is true when there Cubes with tickets the request to be ticked
                        return (Chunk) column;
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
    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void tick() {
        getWorldServer().theProfiler.startSection("playerCubeMapTick");
        long currentTime = this.getWorldServer().getTotalWorldTime();

        getWorldServer().theProfiler.startSection("addPendingPlayers");
        if (!pendingPlayerAdd.isEmpty()) {
            // copy in case player still isn't in world
            Set<EntityPlayerMP> players = pendingPlayerAdd;
            pendingPlayerAdd = new HashSet<>();
            for (EntityPlayerMP player : players) {
                addPlayer(player);
            }
        }
        getWorldServer().theProfiler.endStartSection("tickEntries");
        //force update-all every 8000 ticks (400 seconds)
        if (currentTime - this.previousWorldTime > 8000L) {
            this.previousWorldTime = currentTime;

            for (CubeWatcher playerInstance : this.cubeWatchers) {
                playerInstance.update();
                playerInstance.updateInhabitedTime();
            }
        }


        //process instances to update
        this.cubeWatchersToUpdate.forEach(CubeWatcher::update);
        this.cubeWatchersToUpdate.clear();

        this.columnWatchersToUpdate.forEach(ColumnWatcher::update);
        this.columnWatchersToUpdate.clear();

        getWorldServer().theProfiler.endStartSection("sortToGenerate");
        //sort toLoadPending if needed, but at most every 4 ticks
        if (this.toGenerateNeedSort && currentTime % 4L == 0L) {
            this.toGenerateNeedSort = false;
            this.cubesToGenerate.sort();
            this.columnsToGenerate.sort();
        }
        getWorldServer().theProfiler.endStartSection("sortToSend");
        //sort cubesToSendToClients every other 4 ticks
        if (this.toSendToClientNeedSort && currentTime % 4L == 2L) {
            this.toSendToClientNeedSort = false;
            this.cubesToSendToClients.sort();
            this.columnsToSendToClients.sort();
        }

        getWorldServer().theProfiler.endStartSection("generate");
        if (!this.columnsToGenerate.isEmpty()) {
            getWorldServer().theProfiler.startSection("columns");
            Iterator<ColumnWatcher> iter = this.columnsToGenerate.iterator();
            while (iter.hasNext()) {
                ColumnWatcher entry = iter.next();

                getWorldServer().theProfiler.startSection("column[" + entry.getPos().chunkXPos + "," + entry.getPos().chunkZPos + "]");
                boolean success = entry.getChunk() != null;
                if (!success) {
                    boolean canGenerate = entry.hasPlayerMatching(CAN_GENERATE_CHUNKS);
                    getWorldServer().theProfiler.startSection("generate");
                    success = entry.providePlayerChunk(canGenerate);
                    getWorldServer().theProfiler.endSection(); // generate
                }

                if (success) {
                    iter.remove();

                    if (entry.sendToPlayers()) {
                        this.columnsToSendToClients.remove(entry);
                    }
                }

                getWorldServer().theProfiler.endSection(); // column[x,z]
            }

            getWorldServer().theProfiler.endSection(); // columns
        }
        if (!this.cubesToGenerate.isEmpty()) {
            getWorldServer().theProfiler.startSection("cubes");

            long stopTime = System.nanoTime() + 50000000L;
            int chunksToGenerate = CubicChunksConfig.maxGeneratedCubesPerTick;
            Iterator<CubeWatcher> iterator = this.cubesToGenerate.iterator();

            while (iterator.hasNext() && chunksToGenerate >= 0 && System.nanoTime() < stopTime) {
                CubeWatcher watcher = iterator.next();
                CubePos pos = watcher.getCubePos();

                getWorldServer().theProfiler.startSection("chunk=" + pos);

                boolean success = watcher.getCube() != null && watcher.getCube().isFullyPopulated() && watcher.getCube().isInitialLightingDone() &&
                        !watcher.getCube().hasLightUpdates();
                if (!success) {
                    boolean canGenerate = watcher.hasPlayerMatching(CAN_GENERATE_CHUNKS);
                    getWorldServer().theProfiler.startSection("generate");
                    success = watcher.providePlayerCube(canGenerate);
                    getWorldServer().theProfiler.endSection();
                }

                if (success) {
                    CubeWatcher.SendToPlayersResult state = watcher.sendToPlayers();
                    if (state == CubeWatcher.SendToPlayersResult.WAITING || state == CubeWatcher.SendToPlayersResult.CUBE_SENT
                            || state == CubeWatcher.SendToPlayersResult.ALREADY_DONE) {
                        iterator.remove();
                        this.cubesToSendToClients.remove(watcher);
                    }

                    --chunksToGenerate;
                }

                getWorldServer().theProfiler.endSection();//chunk[x, y, z]
            }

            getWorldServer().theProfiler.endSection(); // chunks
        }
        getWorldServer().theProfiler.endStartSection("send");
        if (!this.columnsToSendToClients.isEmpty()) {
            getWorldServer().theProfiler.startSection("columns");

            this.columnsToSendToClients.removeIf(ColumnWatcher::sendToPlayers);
            getWorldServer().theProfiler.endSection(); // columns
        }
        if (!this.cubesToSendToClients.isEmpty()) {
            getWorldServer().theProfiler.startSection("cubes");
            int toSend = 81 * 8;//sending cubes, so send 8x more at once
            Iterator<CubeWatcher> it = this.cubesToSendToClients.iterator();

            while (it.hasNext() && toSend >= 0) {
                CubeWatcher playerInstance = it.next();

                CubeWatcher.SendToPlayersResult state = playerInstance.sendToPlayers();
                if (state == CubeWatcher.SendToPlayersResult.ALREADY_DONE || state == CubeWatcher.SendToPlayersResult.CUBE_SENT) {
                    it.remove();
                    --toSend;
                } else if (state == CubeWatcher.SendToPlayersResult.WAITING_LIGHT) {
                    if (!cubesToGenerate.contains(playerInstance)) {
                        cubesToGenerate.appendToStart(playerInstance);
                    }
                }
            }
            getWorldServer().theProfiler.endSection(); // cubes
        }

        getWorldServer().theProfiler.endStartSection("unload");
        //if there are no players - unload everything
        if (this.players.isEmpty()) {
            WorldProvider worldprovider = this.getWorldServer().provider;

            if (!worldprovider.canRespawnHere()) {
                this.getWorldServer().getChunkProvider().unloadAllChunks();
            }
        }
        getWorldServer().theProfiler.endStartSection("sendCubes");//unload
        for (EntityPlayerMP player : cubesToSend.keySet()) {
            List<Cube> cubes = new ArrayList<>(cubesToSend.get(player));
            // split into packets of 64 cubes because in 1.10.2 packet splitting is broken
            for (int i = 0; i < cubes.size(); i += 64) {
                int max = Math.min(i + 64, cubes.size());
                PacketCubes packet = new PacketCubes(cubes.subList(i, max));
                PacketDispatcher.sendTo(packet, player);
            }
            //Sending entities per cube.
            for (Cube cube : cubes) {
                ((CubicEntityTracker) getWorldServer().getEntityTracker())
                        .sendLeashedEntitiesInCube(player, cube);
            }

        }
        cubesToSend.clear();
        getWorldServer().theProfiler.endSection();//sendCubes
        getWorldServer().theProfiler.endSection();//playerCubeMapTick
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public boolean contains(int cubeX, int cubeZ) {
        return this.columnWatchers.get(cubeX, cubeZ) != null;
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public PlayerChunkMapEntry getEntry(int cubeX, int cubeZ) {
        return this.columnWatchers.get(cubeX, cubeZ);
    }

    /**
     * Returns existing CubeWatcher or creates new one if it doesn't exist.
     * Attempts to load the cube and send it to client.
     * If it can't load it or send it to client - adds it to cubesToGenerate/cubesToSendToClients
     */
    private CubeWatcher getOrCreateCubeWatcher(@Nonnull CubePos cubePos) {
        CubeWatcher cubeWatcher = this.cubeWatchers.get(cubePos.getX(), cubePos.getY(), cubePos.getZ());

        if (cubeWatcher == null) {
            // make a new watcher
            cubeWatcher = new CubeWatcher(this, cubePos);
            this.cubeWatchers.put(cubeWatcher);


            if (cubeWatcher.getCube() == null ||
                    !cubeWatcher.getCube().isFullyPopulated() ||
                    !cubeWatcher.getCube().isInitialLightingDone()) {
                this.cubesToGenerate.appendToEnd(cubeWatcher);
            }
            // vanilla has the below check, which causes the cubes to be sent to client too early and sometimes in too big amounts
            // if they are sent too earlu, client won't have the right player position and renderer positions are wrong
            // which cause some cubes to not be rendered
            // DO NOT make it the same as vanilla until it's confirmed that Mojang fixed MC-120079
            //if (!cubeWatcher.sendToPlayers()) {
                this.cubesToSendToClients.appendToEnd(cubeWatcher);
            //}
        }
        return cubeWatcher;
    }

    /**
     * Returns existing ColumnWatcher or creates new one if it doesn't exist.
     * Always creates the Column.
     */
    private ColumnWatcher getOrCreateColumnWatcher(ChunkPos chunkPos) {
        ColumnWatcher columnWatcher = this.columnWatchers.get(chunkPos.chunkXPos, chunkPos.chunkZPos);
        if (columnWatcher == null) {
            columnWatcher = new ColumnWatcher(this, chunkPos);
            this.columnWatchers.put(columnWatcher);
            if (columnWatcher.getChunk() == null) {
                this.columnsToGenerate.appendToEnd(columnWatcher);
            }
            if (!columnWatcher.sendToPlayers()) {
                this.columnsToSendToClients.appendToEnd(columnWatcher);
            }
        }
        return columnWatcher;
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void markBlockForUpdate(BlockPos pos) {
        CubeWatcher cubeWatcher = this.getCubeWatcher(CubePos.fromBlockCoords(pos));

        if (cubeWatcher != null) {
            int localX = blockToLocal(pos.getX());
            int localY = blockToLocal(pos.getY());
            int localZ = blockToLocal(pos.getZ());
            cubeWatcher.blockChanged(localX, localY, localZ);
        }
    }

    @Override
    public void heightUpdated(int blockX, int blockZ) {
        ColumnWatcher columnWatcher = this.columnWatchers.get(blockToCube(blockX), blockToCube(blockZ));
        if (columnWatcher != null) {
            int localX = blockToLocal(blockX);
            int localZ = blockToLocal(blockZ);
            columnWatcher.heightChanged(localX, localZ);
        }
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void addPlayer(EntityPlayerMP player) {
        if (player.world != this.getWorldServer()) {
            CubicChunks.bigWarning("Player world not the same ad PlayerCubeMap world! Adding anyway. This is very likely to cause issues! Player "
                            + "world dimension ID: %d, PlayerCubeMap dimension ID: %d", player.world.provider.getDimension(),
                    getWorldServer().provider.getDimension());
        } else if (!player.world.playerEntities.contains(player)) {
            CubicChunks.LOGGER.debug("PlayerCubeMap (dimension {}): Adding player to pending to add list", getWorldServer().provider.getDimension());
            pendingPlayerAdd.add(player);
            return;
        }

        PlayerWrapper playerWrapper = new PlayerWrapper(player);
        playerWrapper.updateManagedPos();

        CubePos playerCubePos = CubePos.fromEntity(player);

        this.cubeSelector.forAllVisibleFrom(playerCubePos, horizontalViewDistance, verticalViewDistance, (currentPos) -> {
            //create cubeWatcher and chunkWatcher
            //order is important
            ColumnWatcher chunkWatcher = getOrCreateColumnWatcher(currentPos.chunkPos());
            //and add the player to them
            if (!chunkWatcher.containsPlayer(player)) {
                chunkWatcher.addPlayer(player);
            }
            CubeWatcher cubeWatcher = getOrCreateCubeWatcher(currentPos);

            cubeWatcher.addPlayer(player);
        });
        this.players.put(player.getEntityId(), playerWrapper);
        this.setNeedSort();
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void removePlayer(EntityPlayerMP player) {
        PlayerWrapper playerWrapper = this.players.get(player.getEntityId());
        if (playerWrapper == null) {
            CubicChunks.bigWarning("PlayerCubeMap#removePlayer got called when there is no player in this world! Things may break!");
            return;
        }
        // Minecraft does something evil there: this method is called *after* changing the player's position
        // so we need to use managerPosition there
        CubePos playerCubePos = CubePos.fromEntityCoords(player.managedPosX, playerWrapper.managedPosY, player.managedPosZ);

        this.cubeSelector.forAllVisibleFrom(playerCubePos, horizontalViewDistance, verticalViewDistance, (cubePos) -> {

            // get the watcher
            CubeWatcher watcher = getCubeWatcher(cubePos);
            if (watcher != null) {
                // remove from the watcher, it also removes the watcher if it becomes empty
                watcher.removePlayer(player);
            }

            // remove column watchers if needed
            ColumnWatcher columnWatcher = getColumnWatcher(cubePos.chunkPos());
            if (columnWatcher == null) {
                return;
            }

            if (columnWatcher.containsPlayer(player)) {
                columnWatcher.removePlayer(player);
            }
        });
        this.players.remove(player.getEntityId());
        this.setNeedSort();
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void updateMovingPlayer(EntityPlayerMP player) {
        // the player moved
        // if the player moved into a new chunk, update which chunks the player needs to know about
        // then update the list of chunks that need to be sent to the client

        // get the player info
        PlayerWrapper playerWrapper = this.players.get(player.getEntityId());

        if (playerWrapper == null) {
            // vanilla sometimes does it, this is normal
            return;
        }
        // did the player move into new cube?
        if (!playerWrapper.cubePosChanged()) {
            return;
        }

        this.updatePlayer(playerWrapper, playerWrapper.getManagedCubePos(), CubePos.fromEntity(player));
        playerWrapper.updateManagedPos();
        this.setNeedSort();
    }

    private void updatePlayer(PlayerWrapper entry, CubePos oldPos, CubePos newPos) {
        getWorldServer().theProfiler.startSection("updateMovedPlayer");
        Set<CubePos> cubesToRemove = new HashSet<>();
        Set<CubePos> cubesToLoad = new HashSet<>();
        Set<ChunkPos> columnsToRemove = new HashSet<>();
        Set<ChunkPos> columnsToLoad = new HashSet<>();

        getWorldServer().theProfiler.startSection("findChanges");
        // calculate new visibility
        this.cubeSelector.findChanged(oldPos, newPos, horizontalViewDistance, verticalViewDistance, cubesToRemove, cubesToLoad, columnsToRemove,
                columnsToLoad);

        getWorldServer().theProfiler.endStartSection("createColumns");
        //order is important, columns first
        columnsToLoad.forEach(pos -> {
            ColumnWatcher columnWatcher = this.getOrCreateColumnWatcher(pos);
            assert columnWatcher.getPos().equals(pos);
            columnWatcher.addPlayer(entry.playerEntity);
        });
        getWorldServer().theProfiler.endStartSection("createCubes");
        cubesToLoad.forEach(pos -> {
            CubeWatcher cubeWatcher = this.getOrCreateCubeWatcher(pos);
            assert cubeWatcher.getCubePos().equals(pos);
            cubeWatcher.addPlayer(entry.playerEntity);
        });
        getWorldServer().theProfiler.endStartSection("removeCubes");
        cubesToRemove.forEach(pos -> {
            CubeWatcher cubeWatcher = this.getCubeWatcher(pos);
            if (cubeWatcher != null) {
                assert cubeWatcher.getCubePos().equals(pos);
                cubeWatcher.removePlayer(entry.playerEntity);
            }
        });
        getWorldServer().theProfiler.endStartSection("removeColumns");
        columnsToRemove.forEach(pos -> {
            ColumnWatcher columnWatcher = this.getColumnWatcher(pos);
            if (columnWatcher != null) {
                assert columnWatcher.getPos().equals(pos);
                columnWatcher.removePlayer(entry.playerEntity);
            }
        });
        getWorldServer().theProfiler.endSection();//removeColumns
        getWorldServer().theProfiler.endSection();//updateMovedPlayer
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public boolean isPlayerWatchingChunk(EntityPlayerMP player, int cubeX, int cubeZ) {
        ColumnWatcher columnWatcher = this.getColumnWatcher(new ChunkPos(cubeX, cubeZ));
        return columnWatcher != null &&
                columnWatcher.containsPlayer(player) &&
                columnWatcher.isSentToPlayers();
    }

    public boolean isPlayerWatchingCube(EntityPlayerMP player, int cubeX, int cubeY, int cubeZ) {
        CubeWatcher watcher = this.getCubeWatcher(new CubePos(cubeX, cubeY, cubeZ));
        return watcher != null &&
                watcher.containsPlayer(player) &&
                watcher.isSentToPlayers();
    }

    // CHECKED: 1.10.2-12.18.1.2092
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
        newHorizontalViewDistance = clamp(newHorizontalViewDistance, 3, 32);
        newVerticalViewDistance = clamp(newVerticalViewDistance, 3, 32);

        if (newHorizontalViewDistance == this.horizontalViewDistance && newVerticalViewDistance == this.verticalViewDistance) {
            return;
        }
        int oldHorizontalViewDistance = this.horizontalViewDistance;
        int oldVerticalViewDistance = this.verticalViewDistance;

        // Somehow the view distances went in opposite directions
        if ((newHorizontalViewDistance < oldHorizontalViewDistance && newVerticalViewDistance > oldVerticalViewDistance) ||
                (newHorizontalViewDistance > oldHorizontalViewDistance && newVerticalViewDistance < oldVerticalViewDistance)) {
            // Adjust the values separately to avoid imploding
            setPlayerViewDistance(newHorizontalViewDistance, oldVerticalViewDistance);
            setPlayerViewDistance(newHorizontalViewDistance, newVerticalViewDistance);
            return;
        }

        for (PlayerWrapper playerWrapper : this.players.valueCollection()) {

            EntityPlayerMP player = playerWrapper.playerEntity;
            CubePos playerPos = playerWrapper.getManagedCubePos();

            if (newHorizontalViewDistance > oldHorizontalViewDistance || newVerticalViewDistance > oldVerticalViewDistance) {
                //if newRadius is bigger, we only need to load new cubes
                this.cubeSelector.forAllVisibleFrom(playerPos, newHorizontalViewDistance, newVerticalViewDistance, pos -> {
                    //order is important
                    ColumnWatcher columnWatcher = this.getOrCreateColumnWatcher(pos.chunkPos());
                    if (!columnWatcher.containsPlayer(player)) {
                        columnWatcher.addPlayer(player);
                    }
                    CubeWatcher cubeWatcher = this.getOrCreateCubeWatcher(pos);
                    if (!cubeWatcher.containsPlayer(player)) {
                        cubeWatcher.addPlayer(player);
                    }
                });
                // either both got smaller or only one of them changed
            } else {
                //if it got smaller...
                Set<CubePos> cubesToUnload = new HashSet<>();
                Set<ChunkPos> columnsToUnload = new HashSet<>();
                this.cubeSelector.findAllUnloadedOnViewDistanceDecrease(playerPos,
                        oldHorizontalViewDistance, newHorizontalViewDistance,
                        oldVerticalViewDistance, newVerticalViewDistance, cubesToUnload, columnsToUnload);

                cubesToUnload.forEach(pos -> {
                    CubeWatcher cubeWatcher = this.getCubeWatcher(pos);
                    if (cubeWatcher != null && cubeWatcher.containsPlayer(player)) {
                        cubeWatcher.removePlayer(player);
                    } else {
                        CubicChunks.LOGGER.warn("cubeWatcher null or doesn't contain player on render distance change");
                    }
                });
                columnsToUnload.forEach(pos -> {
                    ColumnWatcher columnWatcher = this.getColumnWatcher(pos);
                    if (columnWatcher != null && columnWatcher.containsPlayer(player)) {
                        columnWatcher.removePlayer(player);
                    } else {
                        CubicChunks.LOGGER.warn("cubeWatcher null or doesn't contain player on render distance change");
                    }
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
    public void entryChanged(PlayerChunkMapEntry entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEntry(PlayerChunkMapEntry entry) {
        throw new UnsupportedOperationException();
    }

    void addToUpdateEntry(CubeWatcher cubeWatcher) {
        this.cubeWatchersToUpdate.add(cubeWatcher);
    }

    void addToUpdateEntry(ColumnWatcher columnWatcher) {
        this.columnWatchersToUpdate.add(columnWatcher);
    }

    // CHECKED: 1.10.2-12.18.1.2092
    void removeEntry(CubeWatcher cubeWatcher) {
        CubePos cubePos = cubeWatcher.getCubePos();
        cubeWatcher.updateInhabitedTime();
        this.cubeWatchers.remove(cubePos.getX(), cubePos.getY(), cubePos.getZ());
        this.cubeWatchersToUpdate.remove(cubeWatcher);
        this.cubesToGenerate.remove(cubeWatcher);
        this.cubesToSendToClients.remove(cubeWatcher);
        if (cubeWatcher.getCube() != null) {
            cubeWatcher.getCube().getTickets().remove(cubeWatcher); // remove the ticket, so this Cube can unload
        }
        //don't unload, ChunkGc unloads chunks
    }

    // CHECKED: 1.10.2-12.18.1.2092
    public void removeEntry(ColumnWatcher entry) {
        ChunkPos pos = entry.getPos();
        entry.updateChunkInhabitedTime();
        this.columnWatchers.remove(pos.chunkXPos, pos.chunkZPos);
        this.columnsToGenerate.remove(entry);
        this.columnsToSendToClients.remove(entry);
    }

    public void scheduleSendCubeToPlayer(Cube cube, EntityPlayerMP player) {
        cubesToSend.put(player, cube);
    }

    @Nullable public CubeWatcher getCubeWatcher(CubePos pos) {
        return this.cubeWatchers.get(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable public ColumnWatcher getColumnWatcher(ChunkPos pos) {
        return this.columnWatchers.get(pos.chunkXPos, pos.chunkZPos);
    }

    public boolean contains(CubePos coords) {
        return this.cubeWatchers.get(coords.getX(), coords.getY(), coords.getZ()) != null;
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

        int getManagedCubePosX() {
            return blockToCube(this.playerEntity.managedPosX);
        }

        int getManagedCubePosY() {
            return blockToCube(this.managedPosY);
        }

        int getManagedCubePosZ() {
            return blockToCube(this.playerEntity.managedPosZ);
        }


        CubePos getManagedCubePos() {
            return new CubePos(getManagedCubePosX(), getManagedCubePosY(), getManagedCubePosZ());
        }

        boolean cubePosChanged() {
            // did the player move far enough to matter?
            return blockToCube(playerEntity.posX) != this.getManagedCubePosX()
                    || blockToCube(playerEntity.posY) != this.getManagedCubePosY()
                    || blockToCube(playerEntity.posZ) != this.getManagedCubePosZ();
        }
    }

    /**
     * Return iterator over 'CubeWatchers' of all cubes loaded
     * by players. Iterator first element defined by seed.
     * 
     * @param seed seed for random iterator
     */
    public Iterator<CubeWatcher> getRandomWrappedCubeWatcherIterator(int seed) {
        return this.cubeWatchers.randomWrappedIterator(seed);
    }

    public Iterator<Cube> getCubeIterator() {
        WorldServer world = this.getWorldServer();
        final Iterator<CubeWatcher> iterator = this.cubeWatchers.iterator();
        ImmutableSetMultimap<ChunkPos, Ticket> persistentChunksFor = ForgeChunkManager.getPersistentChunksFor(world);
        world.theProfiler.startSection("forcedChunkLoading");
        final Iterator<Cube> persistentCubesIterator = persistentChunksFor.keys().stream()
                .filter(Objects::nonNull)
                .map(input -> (Collection<Cube>) ((IColumn) world.getChunkFromChunkCoords(input.chunkXPos, input.chunkZPos)).getLoadedCubes())
                .collect(ArrayList<Cube>::new, (list, cubeCollection) -> ((ArrayList<Cube>) list).addAll(cubeCollection),
                        (list, cubeList) -> ((ArrayList<Cube>) list).addAll(cubeList))
                .iterator();
        world.theProfiler.endSection();

        return new AbstractIterator<Cube>() {

            boolean shouldSkip(Cube cube){
                if (cube == null)
                    return true;
                if (cube.isEmpty())
                    return true;
                if (!cube.isFullyPopulated())
                    return true;
                return false;
            }

            protected Cube computeNext() {
                while(persistentCubesIterator.hasNext()){
                    Cube cube = persistentCubesIterator.next();
                    if(shouldSkip(cube))
                        continue;
                    return cube;
                }

                while (iterator.hasNext()) {
                    CubeWatcher watcher = iterator.next();
                    Cube cube = watcher.getCube();
                    if(shouldSkip(cube))
                        continue;
                    if(!watcher.hasPlayerMatchingInRange(NOT_SPECTATOR, 128))
                        continue;
                    return cube;
                }
                return this.endOfData();
            }
        };
    }
}
