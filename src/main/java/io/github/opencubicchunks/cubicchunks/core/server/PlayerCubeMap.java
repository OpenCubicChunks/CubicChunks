/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
import io.github.opencubicchunks.cubicchunks.api.world.CubeWatchEvent;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubes;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.util.WatchersSortingList;
import io.github.opencubicchunks.cubicchunks.core.visibility.CubeSelector;
import io.github.opencubicchunks.cubicchunks.core.visibility.CuboidalCubeSelector;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
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
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    final XYZMap<CubeWatcher> cubeWatchers = new XYZMap<>(0.7f, 25 * 25 * 25);

    /**
     * Mapping of Column positions to ColumnWatchers.
     * Contains column positions of all columns loaded by players.
     * Exists for compatibility with vanilla and to send ColumnLoad/Unload packets to clients.
     * Columns cannot be managed by client because they have separate data, like heightmap and biome array.
     */
    final XZMap<ColumnWatcher> columnWatchers = new XZMap<>(0.7f, 25 * 25);

    /**
     * All cubeWatchers that have pending block updates to send.
     */
    private final Set<CubeWatcher> cubeWatchersToUpdate = new HashSet<>();

    /**
     * All columnWatchers that have pending height updates to send.
     */
    private final Set<ColumnWatcher> columnWatchersToUpdate = new HashSet<>();

    /**
     * A queue of cubes to add a player to, this limits the amount of cubes sent to a player per tick to the set limit
     * even when joining an area with already existing cube watchers
     */
    private final Map<EntityPlayerMP, WatchersSortingList<CubeWatcher>> cubesToAddPlayerTo = new IdentityHashMap<>();

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

    private final TickableChunkContainer tickableChunksCubesToReturn = new TickableChunkContainer();

    // see comment in updateMovingPlayer() for explnation why it's in this class
    private final ChunkGc chunkGc;

    final VanillaNetworkHandler vanillaNetworkHandler;

    public PlayerCubeMap(WorldServer worldServer) {
        super(worldServer);
        this.cubeCache = ((ICubicWorldInternal.Server) worldServer).getCubeCache();
        this.setPlayerViewDistance(worldServer.getMinecraftServer().getPlayerList().getViewDistance(),
                ((ICubicPlayerList) worldServer.getMinecraftServer().getPlayerList()).getVerticalViewDistance());
        ((ICubicWorldInternal) worldServer).getLightingManager().registerHeightChangeListener(this);
        this.chunkGc = new ChunkGc(((ICubicWorldInternal.Server) worldServer).getCubeCache());
        this.vanillaNetworkHandler = ((ICubicWorldInternal.Server) worldServer).getVanillaNetworkHandler();
    }

    /**
     * This method exists only because vanilla needs it. It shouldn't be used anywhere else.
     */
    @Override
    @Deprecated // Warning: Hacks! For vanilla use only! (WorldServer.updateBlocks())
    public Iterator<Chunk> getChunkIterator() {
        // CubicChunks.bigWarning("Usage of PlayerCubeMap#getChunkIterator detected in a cubic chunks world! "
        //        + "This is likely to work incorrectly. This is not supported.");
        // TODO: throw UnsupportedOperationException?
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

    public TickableChunkContainer getTickableChunks() {
        TickableChunkContainer tickableChunksCubes = this.tickableChunksCubesToReturn;
        tickableChunksCubes.clear();
        addTickableColumns(tickableChunksCubes);
        addTickableCubes(tickableChunksCubes);
        addForcedColumns(tickableChunksCubes);
        addForcedCubes(tickableChunksCubes);
        return tickableChunksCubes;
    }

    private void addForcedColumns(TickableChunkContainer tickableChunksCubes) {
        for(IColumn columns : ((ICubicWorldInternal.Server) getWorldServer()).getForcedColumns()) {
            tickableChunksCubes.addColumn((Chunk) columns);
        }
    }

    private void addForcedCubes(TickableChunkContainer tickableChunksCubes) {
        tickableChunksCubes.forcedCubes = ((ICubicWorldInternal.Server) getWorldServer()).getForcedCubes();
    }

    private void addTickableCubes(TickableChunkContainer tickableChunksCubes) {
        for (CubeWatcher watcher : cubeWatchers) {
            ICube cube = watcher.getCube();
            if (cube == null || !watcher.hasPlayerMatchingInRange(NOT_SPECTATOR, 128)) {
                continue;
            }
            tickableChunksCubes.addCube(cube);
        }
    }

    private void addTickableColumns(TickableChunkContainer tickableChunksCubes) {
        for (ColumnWatcher watcher : columnWatchers) {
            Chunk chunk = watcher.getChunk();
            if (chunk == null || !watcher.hasPlayerMatchingInRange(128.0D, NOT_SPECTATOR)) {
                continue;
            }
            tickableChunksCubes.addColumn(chunk);
        }
    }

    /**
     * Updates all CubeWatchers and ColumnWatchers.
     * Also sends packets to clients.
     */
    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void tick() {
        getWorldServer().profiler.startSection("playerCubeMapTick");
        long currentTime = this.getWorldServer().getTotalWorldTime();

        getWorldServer().profiler.startSection("addPendingPlayers");
        if (!pendingPlayerAdd.isEmpty()) {
            // copy in case player still isn't in world
            Set<EntityPlayerMP> players = pendingPlayerAdd;
            pendingPlayerAdd = new HashSet<>();
            for (EntityPlayerMP player : players) {
                addPlayer(player);
            }
        }
        getWorldServer().profiler.endStartSection("tickEntries");
        //force update-all every 8000 ticks (400 seconds)
        if (currentTime - this.previousWorldTime > 8000L) {
            this.previousWorldTime = currentTime;

            for (CubeWatcher playerInstance : this.cubeWatchers) {
                playerInstance.update();
                playerInstance.updateInhabitedTime();
            }
        }


        //process instances to update
        if (!cubeWatchersToUpdate.isEmpty()) {
            this.cubeWatchersToUpdate.forEach(CubeWatcher::update);
            this.cubeWatchersToUpdate.clear();
        }

        if (!columnWatchersToUpdate.isEmpty()) {
            this.columnWatchersToUpdate.forEach(ColumnWatcher::update);
            this.columnWatchersToUpdate.clear();
        }

        getWorldServer().profiler.endStartSection("sortToGenerate");
        //sort toLoadPending if needed, but at most every 4 ticks
        if (this.toGenerateNeedSort && currentTime % 4L == 0L) {
            this.toGenerateNeedSort = false;
            this.cubesToGenerate.sort();
            this.columnsToGenerate.sort();
        }
        getWorldServer().profiler.endStartSection("sortToSend");
        //sort cubesToSendToClients every other 4 ticks
        if (this.toSendToClientNeedSort && currentTime % 4L == 2L) {
            this.toSendToClientNeedSort = false;
            this.cubesToSendToClients.sort();
            this.columnsToSendToClients.sort();
            this.cubesToAddPlayerTo.forEach((p, set) -> set.sort());
        }

        getWorldServer().profiler.endStartSection("generate");
        if (!this.columnsToGenerate.isEmpty()) {
            getWorldServer().profiler.startSection("columns");
            Iterator<ColumnWatcher> iter = this.columnsToGenerate.iterator();
            while (iter.hasNext()) {
                ColumnWatcher entry = iter.next();

                boolean success = entry.getChunk() != null;
                if (!success) {
                    boolean canGenerate = entry.hasPlayerMatching(CAN_GENERATE_CHUNKS);
                    getWorldServer().profiler.startSection("generate");
                    success = entry.providePlayerChunk(canGenerate);
                    getWorldServer().profiler.endSection(); // generate
                }

                if (success) {
                    iter.remove();

                    if (entry.sendToPlayers()) {
                        this.columnsToSendToClients.remove(entry);
                    }
                }
            }

            getWorldServer().profiler.endSection(); // columns
        }
        if (!this.cubesToGenerate.isEmpty()) {
            getWorldServer().profiler.startSection("cubes");

            long stopTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CubicChunksConfig.maxCubeGenerationTimeMillis);
            int chunksToGenerate = CubicChunksConfig.maxGeneratedCubesPerTick;
            Iterator<CubeWatcher> iterator = this.cubesToGenerate.iterator();

            while (iterator.hasNext() && chunksToGenerate >= 0 && System.nanoTime() < stopTime) {
                CubeWatcher watcher = iterator.next();
                if (watcher.isWaitingForColumn()) {
                    continue;
                }
                boolean success = !watcher.isWaitingForCube() && !watcher.isWaitingForLighting();
                boolean alreadyLoaded = success;
                if (!success) {
                    boolean canGenerate = watcher.hasPlayerMatching(CAN_GENERATE_CHUNKS);
                    getWorldServer().profiler.startSection("generate");
                    success = watcher.providePlayerCube(canGenerate);
                    getWorldServer().profiler.endSection();
                }

                if (success) {
                    CubeWatcher.SendToPlayersResult state = watcher.sendToPlayers();
                    if (state == CubeWatcher.SendToPlayersResult.WAITING || state == CubeWatcher.SendToPlayersResult.CUBE_SENT
                            || state == CubeWatcher.SendToPlayersResult.ALREADY_DONE) {
                        iterator.remove();
                        this.cubesToSendToClients.remove(watcher);
                    }
                    if (!alreadyLoaded) {
                        --chunksToGenerate;
                    }
                }
            }

            getWorldServer().profiler.endSection(); // chunks
        }
        getWorldServer().profiler.endStartSection("send");
        if (!this.columnsToSendToClients.isEmpty()) {
            getWorldServer().profiler.startSection("columns");

            Iterator<ColumnWatcher> it = this.columnsToSendToClients.iterator();

            while (it.hasNext()) {
                ColumnWatcher playerInstance = it.next();

                if (playerInstance.sendToPlayers()) {
                    it.remove();
                } else if (!columnsToGenerate.contains(playerInstance)) {
                    columnsToGenerate.appendToStart(playerInstance);
                }
            }
            this.columnsToSendToClients.removeIf(ColumnWatcher::sendToPlayers);
            getWorldServer().profiler.endSection(); // columns
        }
        if (!this.cubesToSendToClients.isEmpty()) {
            getWorldServer().profiler.startSection("cubes");
            int toSend = CubicChunksConfig.cubesToSendPerTick;
            Iterator<CubeWatcher> it = this.cubesToSendToClients.iterator();

            while (it.hasNext() && toSend > 0) {
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
            getWorldServer().profiler.endSection(); // cubes
        }

        if (!cubesToAddPlayerTo.isEmpty()) {
            for (Iterator<EntityPlayerMP> iterator = cubesToAddPlayerTo.keySet().iterator(); iterator.hasNext(); ) {
                EntityPlayerMP entityPlayerMP = iterator.next();
                WatchersSortingList<CubeWatcher> watchers = cubesToAddPlayerTo.get(entityPlayerMP);
                int toSend = CubicChunksConfig.cubesToSendPerTick;
                Iterator<CubeWatcher> iter;
                for (iter = watchers.iterator(); toSend > 0 && iter.hasNext(); ) {
                    CubeWatcher watcher = iter.next();
                    watcher.addPlayer(entityPlayerMP);
                    CubeWatcher.SendToPlayersResult state = watcher.sendToPlayers();
                    if (state == CubeWatcher.SendToPlayersResult.WAITING_LIGHT || state == CubeWatcher.SendToPlayersResult.WAITING) {
                        if (!cubesToGenerate.contains(watcher)) {
                            cubesToGenerate.appendToStart(watcher);
                        }
                    }
                    if (state != CubeWatcher.SendToPlayersResult.ALREADY_DONE) {
                        toSend--;
                    }
                    iter.remove();
                }
                if (!iter.hasNext()) {
                    iterator.remove();
                }
            }
        }
        getWorldServer().profiler.endStartSection("unload");
        //if there are no players - unload everything
        if (this.players.isEmpty()) {
            WorldProvider worldprovider = this.getWorldServer().provider;

            if (!worldprovider.canRespawnHere()) {
                this.getWorldServer().getChunkProvider().queueUnloadAll();
            }
        }
        getWorldServer().profiler.endStartSection("sendCubes");//unload
        if (!cubesToSend.isEmpty()) {
            for (EntityPlayerMP player : cubesToSend.keySet()) {
                Collection<Cube> cubes = cubesToSend.get(player);
                if (vanillaNetworkHandler.hasCubicChunks(player)) {
                    PacketCubes packet = new PacketCubes(new ArrayList<>(cubes));
                    PacketDispatcher.sendTo(packet, player);
                } else {
                    vanillaNetworkHandler.sendCubeLoadPackets(cubes, player);
                }
                //Sending entities per cube.
                for (Cube cube : cubes) {
                    ((ICubicEntityTracker) getWorldServer().getEntityTracker()).sendLeashedEntitiesInCube(player, cube);
                    CubeWatcher watcher = getCubeWatcher(cube.getCoords());
                    assert watcher != null;
                    MinecraftForge.EVENT_BUS.post(new CubeWatchEvent(cube, cube.getCoords(), watcher, player));
                }
            }
            cubesToSend.clear();
        }
        getWorldServer().profiler.endSection();//sendCubes
        getWorldServer().profiler.endSection();//playerCubeMapTick
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


            if (cubeWatcher.isWaitingForColumn() || cubeWatcher.isWaitingForCube() || cubeWatcher.isWaitingForLighting()) {
                this.cubesToGenerate.appendToEnd(cubeWatcher);
            }
            // vanilla has the below check, which causes the cubes to be sent to client too early and sometimes in too big amounts
            // if they are sent too early, client won't have the right player position and renderer positions are wrong
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
        ColumnWatcher columnWatcher = this.columnWatchers.get(chunkPos.x, chunkPos.z);
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

        if (!vanillaNetworkHandler.hasCubicChunks(player)) {
            vanillaNetworkHandler.updatePlayerPosition(this, player, playerWrapper.getManagedCubePos());
        }

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

            scheduleAddPlayerToWatcher(cubeWatcher, player);
        });
        this.players.put(player.getEntityId(), playerWrapper);
        this.setNeedSort();
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void removePlayer(EntityPlayerMP player) {
        PlayerWrapper playerWrapper = this.players.get(player.getEntityId());
        if (playerWrapper == null) {
            return;
        }
        // Minecraft does something evil there: this method is called *after* changing the player's position
        // so we need to use managerPosition there
        CubePos playerCubePos = CubePos.fromEntityCoords(player.managedPosX, playerWrapper.managedPosY, player.managedPosZ);

        // send unload columns later so that they get unloaded after their corresponding cubes
        ObjectSet<ColumnWatcher> toSendUnload = new ObjectOpenHashSet<>((horizontalViewDistance*2+1) * (horizontalViewDistance*2+1) * 6);
        this.cubeSelector.forAllVisibleFrom(playerCubePos, horizontalViewDistance, verticalViewDistance, (cubePos) -> {

            // get the watcher
            CubeWatcher watcher = getCubeWatcher(cubePos);
            if (watcher != null) {
                // remove from the watcher, it also removes the watcher if it becomes empty
                removePlayerFromCubeWatcher(watcher, player);
            }

            // remove column watchers if needed
            ColumnWatcher columnWatcher = getColumnWatcher(cubePos.chunkPos());
            if (columnWatcher == null) {
                return;
            }

            toSendUnload.add(columnWatcher);
        });
        toSendUnload.stream()
                .filter(watcher->watcher.containsPlayer(player))
                .forEach(watcher->watcher.removePlayer(player));
        this.players.remove(player.getEntityId());
        this.setNeedSort();
        vanillaNetworkHandler.removePlayer(player);
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

        if (!vanillaNetworkHandler.hasCubicChunks(player)) {
            vanillaNetworkHandler.updatePlayerPosition(this, player, playerWrapper.getManagedCubePos());
        }
        // With ChunkGc being separate from PlayerCubeMap, there are 2 issues:
        // Problem 0: Sometimes, a chunk can be generated after CubeWatcher's chunk load callback returns with a null
        // but before ChunkGC call. This means that the cube will get unloaded, even when ChunkWatcher is waiting for it.
        // Problem 1: When chunkGc call is not in this method, sometimes, when a player teleports far away and is
        // unlucky, and ChunkGc runs in the same tick the teleport appears to happen after PlayerCubeMap call, but
        // before ChunkGc call. This means that PlayerCubeMap won't yet have a CubeWatcher for the player cubes at all,
        // so even directly checking for CubeWatchers before unload attempt won't work.
        //
        // While normally not an issue as it will be reloaded soon anyway, it breaks a lot of things if that cube
        // contains the player. Which is not unlikely if the player is what caused generating this cube in the first place
        // for problem #0.
        // So we put ChunkGc here so that we can be sure it has consistent data about player location, and that no chunks are
        // loaded while we aren't looking.
        this.chunkGc.tick();
    }

    private void updatePlayer(PlayerWrapper entry, CubePos oldPos, CubePos newPos) {
        getWorldServer().profiler.startSection("updateMovedPlayer");
        Set<CubePos> cubesToRemove = new HashSet<>();
        Set<CubePos> cubesToLoad = new HashSet<>();
        Set<ChunkPos> columnsToRemove = new HashSet<>();
        Set<ChunkPos> columnsToLoad = new HashSet<>();

        getWorldServer().profiler.startSection("findChanges");
        // calculate new visibility
        this.cubeSelector.findChanged(oldPos, newPos, horizontalViewDistance, verticalViewDistance, cubesToRemove, cubesToLoad, columnsToRemove,
                columnsToLoad);

        getWorldServer().profiler.endStartSection("createColumns");
        //order is important, columns first
        columnsToLoad.forEach(pos -> {
            ColumnWatcher columnWatcher = this.getOrCreateColumnWatcher(pos);
            assert columnWatcher.getPos().equals(pos);
            columnWatcher.addPlayer(entry.playerEntity);
        });
        getWorldServer().profiler.endStartSection("createCubes");
        cubesToLoad.forEach(pos -> {
            CubeWatcher cubeWatcher = this.getOrCreateCubeWatcher(pos);
            assert cubeWatcher.getCubePos().equals(pos);
            scheduleAddPlayerToWatcher(cubeWatcher, entry.playerEntity);
        });
        getWorldServer().profiler.endStartSection("removeCubes");
        cubesToRemove.forEach(pos -> {
            CubeWatcher cubeWatcher = this.getCubeWatcher(pos);
            if (cubeWatcher != null) {
                assert cubeWatcher.getCubePos().equals(pos);
                removePlayerFromCubeWatcher(cubeWatcher, entry.playerEntity);
            }
        });
        getWorldServer().profiler.endStartSection("removeColumns");
        columnsToRemove.forEach(pos -> {
            ColumnWatcher columnWatcher = this.getColumnWatcher(pos);
            if (columnWatcher != null) {
                assert columnWatcher.getPos().equals(pos);
                columnWatcher.removePlayer(entry.playerEntity);
            }
        });
        getWorldServer().profiler.endSection();//removeColumns
        getWorldServer().profiler.endSection();//updateMovedPlayer
        setNeedSort();
    }

    private void removePlayerFromCubeWatcher(CubeWatcher cubeWatcher, EntityPlayerMP playerEntity) {
        if (!cubeWatcher.containsPlayer(playerEntity)) {
            WatchersSortingList<CubeWatcher> cubeWatchers = cubesToAddPlayerTo.get(playerEntity);
            if (cubeWatchers != null) {
                cubeWatchers.remove(cubeWatcher);
            }
        }
        cubeWatcher.removePlayer(playerEntity);
    }

    private void scheduleAddPlayerToWatcher(CubeWatcher cubeWatcher, EntityPlayerMP playerEntity) {
        cubesToAddPlayerTo.computeIfAbsent(playerEntity, p -> new WatchersSortingList<>(Comparator.comparingDouble(w -> {
            double dx = w.getCubePos().getXCenter() - playerEntity.posX;
            double dy = w.getCubePos().getYCenter() - playerEntity.posY;
            double dz = w.getCubePos().getZCenter() - playerEntity.posZ;
            return dx*dx + dy*dy + dz*dz;
        }))).appendToEnd(cubeWatcher);
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

        newHorizontalViewDistance = clamp(newHorizontalViewDistance, 3, CubicChunks.hasOptifine() ? 64 : 32);
        newVerticalViewDistance = clamp(newVerticalViewDistance, 3, CubicChunks.hasOptifine() ? 64 : 32);

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
                        scheduleAddPlayerToWatcher(cubeWatcher, player);
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
                    if (cubeWatcher != null) {
                        removePlayerFromCubeWatcher(cubeWatcher, player);
                    } else {
                        CubicChunks.LOGGER.warn("cubeWatcher null on render distance change");
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
        if (!cubesToAddPlayerTo.isEmpty()) {
            for (WatchersSortingList<CubeWatcher> value : cubesToAddPlayerTo.values()) {
                if (value.contains(cubeWatcher)) {
                    return;
                }
            }
        }
        cubeWatcher.invalidate();
        CubePos cubePos = cubeWatcher.getCubePos();
        cubeWatcher.updateInhabitedTime();
        CubeWatcher removed = this.cubeWatchers.remove(cubePos.getX(), cubePos.getY(), cubePos.getZ());
        assert removed == cubeWatcher : "Removed unexpected cube watcher";
        this.cubeWatchersToUpdate.remove(cubeWatcher);
        this.cubesToGenerate.remove(cubeWatcher);
        this.cubesToSendToClients.remove(cubeWatcher);
        if (cubeWatcher.getCube() != null) {
            cubeWatcher.getCube().getTickets().remove(cubeWatcher); // remove the ticket, so this Cube can unload
        }
        if (!cubesToAddPlayerTo.isEmpty()) {
            for (Iterator<WatchersSortingList<CubeWatcher>> iterator = cubesToAddPlayerTo.values().iterator(); iterator.hasNext(); ) {
                WatchersSortingList<CubeWatcher> value = iterator.next();
                value.remove(cubeWatcher);
                if (value.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        //don't unload, ChunkGc unloads chunks
    }

    public void removeEntry(ColumnWatcher entry) {
        ChunkPos pos = entry.getPos();
        entry.updateChunkInhabitedTime();
        this.columnWatchers.remove(pos.x, pos.z);
        this.columnsToGenerate.remove(entry);
        this.columnsToSendToClients.remove(entry);
        this.columnWatchersToUpdate.remove(entry);
    }

    public void scheduleSendCubeToPlayer(Cube cube, EntityPlayerMP player) {
        cubesToSend.put(player, cube);
    }

    public void removeSchedulesSendCubeToPlayer(Cube cube, EntityPlayerMP player) {
        cubesToSend.remove(player, cube);
    }

    @Nullable public CubeWatcher getCubeWatcher(CubePos pos) {
        return this.cubeWatchers.get(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable public ColumnWatcher getColumnWatcher(ChunkPos pos) {
        return this.columnWatchers.get(pos.x, pos.z);
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
     * @return cube watcher iterator
     */
    public Iterator<CubeWatcher> getRandomWrappedCubeWatcherIterator(int seed) {
        return this.cubeWatchers.randomWrappedIterator(seed);
    }
    
    public Iterator<Cube> getCubeIterator() {
        WorldServer world = this.getWorldServer();
        final Iterator<CubeWatcher> iterator = this.cubeWatchers.iterator();
        ImmutableSetMultimap<ChunkPos, Ticket> persistentChunksFor = ForgeChunkManager.getPersistentChunksFor(world);
        world.profiler.startSection("forcedChunkLoading");
        @SuppressWarnings("unchecked")
        final Iterator<Cube> persistentCubesIterator = persistentChunksFor.keys().stream()
                .filter(Objects::nonNull)
                .map(input -> (Collection<Cube>) ((IColumn) world.getChunk(input.x, input.z)).getLoadedCubes())
                .collect(ArrayList<Cube>::new, ArrayList::addAll, ArrayList::addAll)
                .iterator();
        world.profiler.endSection();
        
        return new AbstractIterator<Cube>() {

            Iterator<Cube> persistentCubes = persistentCubesIterator;
            
            boolean shouldSkip(Cube cube){
                if (cube == null) 
                    return true;
                if (cube.isEmpty())
                    return true;
                if (!cube.isFullyPopulated())
                    return true;
                return false;
            }

            @Override protected Cube computeNext() {
                while(persistentCubes != null && persistentCubes.hasNext()){
                    Cube cube = persistentCubes.next();
                    if (!persistentCubes.hasNext()) {
                        persistentCubes = null;
                    }
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

    public class TickableChunkContainer {

        private final ObjectArrayList<ICube> cubes = ObjectArrayList.wrap(new ICube[64*1024]);
        private XYZMap<ICube> forcedCubes;
        private final Set<Chunk> columns = Collections.newSetFromMap(new IdentityHashMap<>());

        private void clear() {
            this.cubes.clear();
            this.columns.clear();
        }

        private void addCube(ICube cube) {
            cubes.add(cube);
        }

        public void addColumn(Chunk column) {
            columns.add(column);
        }

        public Iterable<ICube> forcedCubes() {
            return forcedCubes;
        }

        public ICube[] playerTickableCubes() {
            return cubes.elements();
        }

        public Iterable<Chunk> columns() {
            return columns;
        }
    }
}
