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

import static io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil.getFieldGetterHandle;
import static io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil.getFieldSetterHandle;

import com.google.common.base.Throwables;
import io.github.opencubicchunks.cubicchunks.core.network.PacketColumn;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.PacketHeightMapUpdate;
import io.github.opencubicchunks.cubicchunks.core.network.PacketUnloadColumn;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.network.PacketColumn;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.PacketHeightMapUpdate;
import io.github.opencubicchunks.cubicchunks.core.network.PacketUnloadColumn;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XZAddressable;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

import java.lang.invoke.MethodHandle;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class ColumnWatcher extends PlayerChunkMapEntry implements XZAddressable {

    @Nonnull private PlayerCubeMap playerCubeMap;
    private static MethodHandle getPlayers = getFieldGetterHandle(PlayerChunkMapEntry.class, "field_187283_c");
    private static MethodHandle setLastUpdateInhabitedTime = getFieldSetterHandle(PlayerChunkMapEntry.class, "field_187289_i");
    private static MethodHandle setSentToPlayers = getFieldSetterHandle(PlayerChunkMapEntry.class, "field_187290_j");
    private static MethodHandle isLoading = getFieldGetterHandle(PlayerChunkMapEntry.class, "loading");//forge field, no srg name
    private static MethodHandle getLoadedRunnable = getFieldGetterHandle(PlayerChunkMapEntry.class, "loadedRunnable");//forge field, no srg name
    @Nonnull private final Runnable loadedRunnable;

    @Nonnull private final TByteList dirtyColumns = new TByteArrayList(64);

    ColumnWatcher(PlayerCubeMap playerCubeMap, ChunkPos pos) {
        super(playerCubeMap, pos.chunkXPos, pos.chunkZPos);
        this.playerCubeMap = playerCubeMap;
        try {
            this.loadedRunnable = (Runnable) getLoadedRunnable.invoke(this);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    // CHECKED: 1.10.2-12.18.1.2092
    public void addPlayer(EntityPlayerMP player) {
        if (this.getPlayers().contains(player)) {
            CubicChunks.LOGGER.debug("Failed to add player. {} already is in chunk {}, {}", player,
                    this.getPos().chunkXPos,
                    this.getPos().chunkZPos);
            return;
        }
        if (this.getPlayers().isEmpty()) {
            this.setLastUpdateInhabitedTime(playerCubeMap.getWorldServer().getTotalWorldTime());
        }

        this.getPlayers().add(player);

        //always sent to players, no need to check it

        if (this.isSentToPlayers()) {
            PacketColumn message = new PacketColumn(this.getChunk());
            PacketDispatcher.sendTo(message, player);
            //this.sendNearbySpecialEntities - done by cube entry
            MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(this.getChunk().getChunkCoordIntPair(), player));
        }
    }

    // CHECKED: 1.10.2-12.18.1.2092//TODO: remove it, the only different line is sending packet
    public void removePlayer(EntityPlayerMP player) {
        if (!this.getPlayers().contains(player)) {
            return;
        }
        if (this.getChunk() == null) {
            this.getPlayers().remove(player);
            if (this.getPlayers().isEmpty()) {
                if (isLoading()) {
                    AsyncWorldIOExecutor.dropQueuedColumnLoad(
                            playerCubeMap.getWorldServer(), getPos().chunkXPos, getPos().chunkZPos, (c) -> loadedRunnable.run());
                }
                this.playerCubeMap.removeEntry(this);
            }
            return;
        }

        if (this.isSentToPlayers()) {
            PacketDispatcher.sendTo(new PacketUnloadColumn(getPos()), player);
        }

        this.getPlayers().remove(player);

        MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(this.getPos(), player));

        if (this.getPlayers().isEmpty()) {
            playerCubeMap.removeEntry(this);
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

        try {
            PacketColumn message = new PacketColumn(this.getChunk());
            for (EntityPlayerMP player : this.getPlayers()) {
                PacketDispatcher.sendTo(message, player);
            }
            setSentToPlayers.invoke(this, true);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
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
        CubeWatcher watcher = playerCubeMap.getCubeWatcher(CubePos.fromBlockCoords(x, y, z));
        if (watcher != null) {
            watcher.blockChanged(x, y, z);
        }
    }

    @Override
    public void update() {
        if (!this.isSentToPlayers() || this.dirtyColumns.isEmpty()) {
            return;
        }
        assert getChunk() != null;
        for (EntityPlayerMP player : this.getPlayers()) {
            PacketDispatcher.sendTo(new PacketHeightMapUpdate(getPos(), dirtyColumns, ((IColumn) getChunk()).getOpacityIndex()), player);
        }
        this.dirtyColumns.clear();
    }

    //containsPlayer, hasPlayerMatching, hasPlayerMatchingInRange, isAddedToChunkUpdateQueue, getChunk, getClosestPlayerDistance - ok

    private boolean isLoading() {
        try {
            return (boolean) isLoading.invoke(this);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Override public int getX() {
        return this.getPos().chunkXPos;
    }

    @Override public int getZ() {
        return this.getPos().chunkZPos;
    }

    void heightChanged(int localX, int localZ) {
        if (!isSentToPlayers()) {
            return;
        }
        if (this.dirtyColumns.isEmpty()) {
            playerCubeMap.addToUpdateEntry(this);
        }
        this.dirtyColumns.add((byte) AddressTools.getLocalAddress(localX, localZ));
    }
}
