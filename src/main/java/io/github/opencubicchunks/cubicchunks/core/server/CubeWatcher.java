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

import com.google.common.base.Predicate;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeUnWatchEvent;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubeBlockChange;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.PacketUnloadCube;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubeWatcher implements ITicket, ICubeWatcher {

    private final Consumer<Cube> consumer;

    private final CubeProviderServer cubeCache;
    private PlayerCubeMap playerCubeMap;
    @Nullable private Cube cube;
    private final ObjectArrayList<EntityPlayerMP> players = ObjectArrayList.wrap(new EntityPlayerMP[0]);
    private final TShortList dirtyBlocks = new TShortArrayList(64);
    private final CubePos cubePos;
    private long previousWorldTime = 0;
    private boolean sentToPlayers = false;
    private boolean loading = true;
    private boolean invalid = false;
    private int lightGenerationAttempts = 0;

    // CHECKED: 1.10.2-12.18.1.2092
    CubeWatcher(PlayerCubeMap playerCubeMap, CubePos cubePos) {
        this.cubePos = cubePos;
        this.playerCubeMap = playerCubeMap;
        this.cubeCache = ((ICubicWorldInternal.Server) playerCubeMap.getWorldServer()).getCubeCache();
        this.consumer = (c) -> {
            if (this.invalid) {
                return;
            }
            this.cube = c;
            this.loading = false;
            if (this.cube != null) {
                this.cube.getTickets().add(this);
            }
        };
        this.cubeCache.asyncGetCube(
                cubePos.getX(), cubePos.getY(), cubePos.getZ(),
                ICubeProviderServer.Requirement.LOAD,
                consumer);
    }

    // CHECKED: 1.10.2-12.18.1.2092
    void addPlayer(EntityPlayerMP player) {
        if (this.players.contains(player)) {
            CubicChunks.LOGGER.debug("Failed to add player. {} already is in cube at {}", player, cubePos);
            return;
        }
        if (this.players.isEmpty()) {
            this.previousWorldTime = this.getWorldTime();
        }
        this.players.add(player);

        if (this.sentToPlayers) {
            this.sendToPlayer(player);
            ((ICubicEntityTracker) playerCubeMap.getWorldServer().getEntityTracker())
                    .sendLeashedEntitiesInCube(player, this.getCube());
        }
    }

    // CHECKED: 1.10.2-12.18.1.2092
    void removePlayer(EntityPlayerMP player) {
        if (!this.players.contains(player)) {
            if (this.players.isEmpty()) {
                playerCubeMap.removeEntry(this);
            }
            return;
        }
        // If we haven't loaded yet don't load the chunk just so we can clean it up
        if (this.cube == null) {
            this.players.remove(player);

            if (this.players.isEmpty()) {
                playerCubeMap.removeEntry(this);
            }
            return;
        }

        if (this.sentToPlayers) {
            PacketDispatcher.sendTo(new PacketUnloadCube(this.cubePos), player);
            playerCubeMap.removeSchedulesSendCubeToPlayer(cube, player);
        }

        this.players.remove(player);
        MinecraftForge.EVENT_BUS.post(new CubeUnWatchEvent(cube, cubePos, this, player));

        if (this.players.isEmpty()) {
            playerCubeMap.removeEntry(this);
        }
    }

    void invalidate() {
        if (loading) {
            AsyncWorldIOExecutor.dropQueuedCubeLoad(this.playerCubeMap.getWorldServer(),
                    cubePos.getX(), cubePos.getY(), cubePos.getZ(),
                    c -> this.cube = c);
        }
        invalid = true;
    }

    // CHECKED: 1.10.2-12.18.1.2092
    boolean providePlayerCube(boolean canGenerate) {
        if (loading) {
            return false;
        }
        if (isWaitingForColumn()) {
            return false;
        }
        if (this.cube != null && (!canGenerate || (!isWaitingForCube() && !isWaitingForLighting()))) {
            return true;
        }
        int cubeX = cubePos.getX();
        int cubeY = cubePos.getY();
        int cubeZ = cubePos.getZ();

        playerCubeMap.getWorldServer().profiler.startSection("getCube");
        if (canGenerate) {
            this.cube = this.cubeCache.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.LIGHT);
            assert this.cube != null;
            if (this.cube instanceof BlankCube) {
                this.cube = null;
                return false;
            }
            if (!this.cube.isFullyPopulated()) {
                return false;
            }
        } else {
            this.cube = this.cubeCache.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.LOAD);
        }
        if (this.cube != null) {
            this.cube.getTickets().add(this);
        }
        playerCubeMap.getWorldServer().profiler.endStartSection("light");
        if (this.cube != null) {
            LightingManager.CubeLightUpdateInfo info = this.cube.getCubeLightUpdateInfo();
            if (info != null) {
                info.tick();
                if (info.hasUpdates()) {
                    lightGenerationAttempts++;
                } else {
                    lightGenerationAttempts = 0;
                }
            }
        }
        playerCubeMap.getWorldServer().profiler.endSection();

        return this.cube != null && !isWaitingForLighting();
    }

    @Override public boolean isSentToPlayers() {
        return sentToPlayers;
    }

    boolean isWaitingForCube() {
        return this.cube == null || !this.cube.isFullyPopulated() || !this.cube.isInitialLightingDone() || !this.cube.isSurfaceTracked();
    }

    boolean isWaitingForLighting() {
        // if lighting couldn't be updated 3 times in a row, give up and consider it done anyway
        return this.cube == null || (this.cube.hasLightUpdates() && lightGenerationAttempts < 3);
    }

    boolean isWaitingForColumn() {
        ColumnWatcher columnEntry = playerCubeMap.getColumnWatcher(this.cubePos.chunkPos());
        return columnEntry == null || !columnEntry.isSentToPlayers();
    }

    // CHECKED: 1.10.2-12.18.1.2092
    SendToPlayersResult sendToPlayers() {
        if (this.sentToPlayers) {
            return SendToPlayersResult.ALREADY_DONE;
        }
        if (isWaitingForCube()) {
            return SendToPlayersResult.WAITING;
        }
        if (isWaitingForLighting()) {
            return SendToPlayersResult.WAITING_LIGHT;
        }
        //can't send cubes before columns
        if (isWaitingForColumn()) {
            return SendToPlayersResult.WAITING;
        }
        if (!playerCubeMap.getColumnWatcher(cubePos.chunkPos()).isSentToPlayers()) {
            return SendToPlayersResult.WAITING;
        }
        this.dirtyBlocks.clear();
        //set to true before adding to queue so that sendToPlayer can actually add it
        this.sentToPlayers = true;

        for (EntityPlayerMP playerEntry : this.players) {
            sendToPlayer(playerEntry);
        }

        return SendToPlayersResult.CUBE_SENT;
    }

    // CHECKED: 1.10.2-12.18.1.2092
    private void sendToPlayer(EntityPlayerMP player) {
        if (!this.sentToPlayers) {
            return;
        }
        assert cube != null;
        playerCubeMap.scheduleSendCubeToPlayer(cube, player);
    }

    // CHECKED: 1.10.2-12.18.1.2092
    void updateInhabitedTime() {
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
    void blockChanged(int localX, int localY, int localZ) {
        //if we are adding the first one, add it to update list
        if (this.dirtyBlocks.isEmpty()) {
            playerCubeMap.addToUpdateEntry(this);
        }
        // If the number of changes is above clumpingThreshold
        // we send the whole cube, but to decrease network usage
        // forge sends only TEs that have changed,
        // so we need to know all changed blocks. So add everything
        // it's a set so no need to check for duplicates
        this.dirtyBlocks.add((short) AddressTools.getLocalAddress(localX, localY, localZ));
    }

    // CHECKED: 1.10.2-12.18.1.2092
    void update() {
        if (!this.sentToPlayers) {
            return;
        }
        assert cube != null;
        // are there any updates?
        if (this.dirtyBlocks.isEmpty()) {
            return;
        }

        World world = this.cube.getWorld();

        if (this.dirtyBlocks.size() >= ForgeModContainer.clumpingThreshold) {
            // send whole cube
            this.players.forEach(entry -> playerCubeMap.scheduleSendCubeToPlayer(cube, entry));
        } else {
            // send all the dirty blocks
            PacketCubeBlockChange packet = null;
            for (EntityPlayerMP player : this.players) {
                if (playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                    if (packet == null) { // create packet lazily
                        packet = new PacketCubeBlockChange(this.cube, this.dirtyBlocks);
                    }
                    PacketDispatcher.sendTo(packet, player);
                } else {
                    playerCubeMap.vanillaNetworkHandler.sendBlockChanges(dirtyBlocks, cube, player);
                }
            }
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

    private void sendBlockEntityToAllPlayers(@Nullable TileEntity blockEntity) {
        if (blockEntity == null) {
            return;
        }
        Packet<?> packet = blockEntity.getUpdatePacket();
        if (packet == null) {
            return;
        }
        sendPacketToAllPlayers(packet);
    }

    boolean containsPlayer(EntityPlayerMP player) {
        return this.players.contains(player);
    }

    boolean hasPlayerMatching(Predicate<EntityPlayerMP> predicate) {
        for (EntityPlayerMP e : players.elements()) {
            if (e == null) {
                break;
            }
            if (predicate.apply(e)) {
                return true;
            }
        }
        return false;
    }
    
    boolean hasPlayerMatchingInRange(Predicate<EntityPlayerMP> predicate, int range) {
        double d = range*range;
        double cx = cubePos.getXCenter();
        double cy = cubePos.getYCenter();
        double cz = cubePos.getZCenter();
        for (EntityPlayerMP e : players.elements()) {
            if (e == null) {
                break;
            }
            if (predicate.apply(e)) {
                double dist = cx - e.posX;
                dist *= dist;
                if (dist > d) {
                    continue;
                }
                double dy = cy - e.posY;
                dist += dy * dy;
                if (dist > d) {
                    continue;
                }
                double dz = cz - e.posZ;
                dist += dz * dz;
                if (dist > d) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private double getDistanceSq(CubePos cubePos, Entity entity) {
        double blockX = cubePos.getXCenter();
        double blockY = cubePos.getYCenter();
        double blockZ = cubePos.getZCenter();
        double dx = blockX - entity.posX;
        double dy = blockY - entity.posY;
        double dz = blockZ - entity.posZ;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override @Nullable public Cube getCube() {
        return this.cube;
    }

    double getClosestPlayerDistance() {
        double min = Double.MAX_VALUE;

        for (EntityPlayerMP entry : this.players.elements()) {
            if (entry == null) {
                break;
            }
            double dist = getDistanceSq(cubePos, entry);

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
        for (EntityPlayerMP entry : this.players) {
            entry.connection.sendPacket(packet);
        }
    }

    @Override public void sendPacketToAllPlayers(IMessage packet) {
        for (EntityPlayerMP entry : this.players) {
            PacketDispatcher.sendTo(packet, entry);
        }
    }

    CubePos getCubePos() {
        return cubePos;
    }

    @Override public int getX() {
        return this.cubePos.getX();
    }

    @Override public int getY() {
        return this.cubePos.getY();
    }

    @Override public int getZ() {
        return this.cubePos.getZ();
    }

    @Override public boolean shouldTick() {
        return true; // Cubes that players can see should tick
    }

    public enum SendToPlayersResult {
        ALREADY_DONE, CUBE_SENT, WAITING, WAITING_LIGHT
    }
}
