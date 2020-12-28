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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XZAddressable;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IPlayerChunkMapEntry;
import io.github.opencubicchunks.cubicchunks.core.network.PacketColumn;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.PacketHeightMapUpdate;
import io.github.opencubicchunks.cubicchunks.core.network.PacketUnloadColumn;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ColumnWatcher extends PlayerChunkMapEntry implements XZAddressable {

    @Nonnull private final PlayerCubeMap playerCubeMap;
    @Nonnull private final TByteList dirtyColumns = new TByteArrayList(64);

    ColumnWatcher(PlayerCubeMap playerCubeMap, ChunkPos pos) {
        super(playerCubeMap, pos.x, pos.z);
        this.playerCubeMap = playerCubeMap;
    }

    private IPlayerChunkMapEntry self() {
        return (IPlayerChunkMapEntry) this;
    }

    @Override
    public boolean providePlayerChunk(boolean canGenerate) {
        if (self().isLoading()) {
            return false;
        }
        if (self().getChunk() != null) {
            return true;
        }
        if (canGenerate) {
            Chunk chunk = this.playerCubeMap.getWorldServer().getChunkProvider().provideChunk(self().getPos().x, self().getPos().z);
            if (chunk.isEmpty()) {
                return false;
            }
            self().setChunk(chunk);
        } else {
            self().setChunk(this.playerCubeMap.getWorldServer().getChunkProvider().loadChunk(self().getPos().x, self().getPos().z));
        }

        return self().getChunk() != null;
    }

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public void addPlayer(EntityPlayerMP player) {
        assert this.getChunk() == null || this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        if (self().getPlayerList().contains(player)) {
            CubicChunks.LOGGER.debug("Failed to expand player. {} already is in chunk {}, {}", player,
                    this.getPos().x,
                    this.getPos().z);
            return;
        }
        if (self().getPlayerList().isEmpty()) {
            self().setLastUpdateInhabitedTime(playerCubeMap.getWorldServer().getTotalWorldTime());
        }

        self().getPlayerList().add(player);

        //always sent to players, no need to check it

        if (this.isSentToPlayers()) {
            if (playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                PacketColumn message = new PacketColumn(this.getChunk());
                PacketDispatcher.sendTo(message, player);
            } else {
                playerCubeMap.vanillaNetworkHandler.sendColumnLoadPacket(this.getChunk(), player);
            }
            //this.sendNearbySpecialEntities - done by cube entry
            MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(this.getChunk(), player));
        }
    }

    // CHECKED: 1.10.2-12.18.1.2092//TODO: remove it, the only different line is sending packet
    @Override
    public void removePlayer(EntityPlayerMP player) {
        assert this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        if (!self().getPlayerList().contains(player)) {
            return;
        }
        if (this.getChunk() == null) {
            self().getPlayerList().remove(player);
            if (self().getPlayerList().isEmpty()) {
                if (self().isLoading()) {
                    AsyncWorldIOExecutor.dropQueuedColumnLoad(
                            playerCubeMap.getWorldServer(), getPos().x, getPos().z, (c) -> self().getLoadedRunnable().run());
                }
                this.playerCubeMap.removeEntry(this);
            }
            return;
        }

        if (this.isSentToPlayers()) {
            if (playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                PacketDispatcher.sendTo(new PacketUnloadColumn(getPos()), player);
            } else {
                playerCubeMap.vanillaNetworkHandler.sendColumnUnloadPacket(getPos(), player);
            }
        }

        self().getPlayerList().remove(player);

        MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(this.getChunk(), player));

        if (self().getPlayerList().isEmpty()) {
            playerCubeMap.removeEntry(this);
        }
    }

    //providePlayerChunk - ok

    // CHECKED: 1.10.2-12.18.1.2092
    @Override
    public boolean sendToPlayers() {
        if (this.isSentToPlayers()) {
            return true;
        }
        if (getChunk() == null) {
            return false;
        }
        assert this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        try {

            PacketColumn message = new PacketColumn(this.getChunk());
            for (EntityPlayerMP player : self().getPlayerList()) {
                if (playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                    PacketDispatcher.sendTo(message, player);
                } else {
                    playerCubeMap.vanillaNetworkHandler.sendColumnLoadPacket(this.getChunk(), player);
                }
            }
            self().setSentToPlayers(true);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        return true;
    }

    @Override
    @Deprecated
    public void sendToPlayer(EntityPlayerMP player) {
        assert this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        //done by cube watcher
    }

    //updateChunkInhabitedTime - ok

    @Override
    @Deprecated
    public void blockChanged(int x, int y, int z) {
        assert this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        CubeWatcher watcher = playerCubeMap.getCubeWatcher(CubePos.fromBlockCoords(x, y, z));
        if (watcher != null) {
            watcher.blockChanged(x, y, z);
        }
    }

    @Override
    public void update() {
        if (!this.isSentToPlayers()) {
            return;
        }
        assert this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ()) :
            "Column watcher " + this + " at " + getPos() + " contains column " + getChunk() + " but loaded column is " +
                    playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        if (this.dirtyColumns.isEmpty()) {
            return;
        }
        assert getChunk() != null;
        for (EntityPlayerMP player : self().getPlayerList()) {
            if (playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                PacketDispatcher.sendTo(new PacketHeightMapUpdate(getPos(), dirtyColumns, ((IColumn) getChunk()).getOpacityIndex()), player);
            }
        }
        this.dirtyColumns.clear();
    }

    //containsPlayer, hasPlayerMatching, hasPlayerMatchingInRange, isAddedToChunkUpdateQueue, getChunk, getClosestPlayerDistance - ok

    @Override public int getX() {
        return this.getPos().x;
    }

    @Override public int getZ() {
        return this.getPos().z;
    }

    void heightChanged(int localX, int localZ) {
        if (!isSentToPlayers()) {
            return;
        }
        assert this.getChunk() == playerCubeMap.getWorldServer().getChunkProvider().getLoadedChunk(getX(), getZ());
        if (this.dirtyColumns.isEmpty()) {
            playerCubeMap.addToUpdateEntry(this);
        }
        this.dirtyColumns.add((byte) AddressTools.getLocalAddress(localX, localZ));
    }

    // BetterPortals: keep compatibility
    @Deprecated private List<EntityPlayerMP> getPlayers() {
        return self().getPlayerList();
    }
}
