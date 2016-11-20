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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

import java.lang.invoke.MethodHandle;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.CubicChunks;
import cubicchunks.network.PacketColumn;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.network.PacketUnloadColumn;
import cubicchunks.server.chunkio.async.forge.AsyncWorldIOExecutor;
import cubicchunks.util.CubePos;
import cubicchunks.util.XZAddressable;
import cubicchunks.world.column.Column;
import mcp.MethodsReturnNonnullByDefault;

import static cubicchunks.util.ReflectionUtil.getFieldGetterHandle;
import static cubicchunks.util.ReflectionUtil.getFieldSetterHandle;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ColumnWatcher extends PlayerChunkMapEntry implements XZAddressable {

	private PlayerCubeMap playerCubeMap;
	private static MethodHandle getPlayers = getFieldGetterHandle(PlayerChunkMapEntry.class, "field_187283_c");
	private static MethodHandle setLastUpdateInhabitedTime = getFieldSetterHandle(PlayerChunkMapEntry.class, "field_187289_i");
	private static MethodHandle setSentToPlayers = getFieldSetterHandle(PlayerChunkMapEntry.class, "field_187290_j");
	private static MethodHandle isLoading = getFieldGetterHandle(PlayerChunkMapEntry.class, "loading");//forge field, no srg name
	private static MethodHandle getLoadedRunnable = getFieldGetterHandle(PlayerChunkMapEntry.class, "loadedRunnable");//forge field, no srg name
	private final Runnable loadedRunnable;

	public ColumnWatcher(PlayerCubeMap playerCubeMap, ChunkPos pos) {
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
			//this.sendNearbySpecialEntities - done by cube entry
			MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(this.getPos(), player));
		}
	}

	// CHECKED: 1.10.2-12.18.1.2092//TODO: remove it, the only different line is sending packet
	public void removePlayer(EntityPlayerMP player) {
		if (!this.getPlayers().contains(player)) {
			return;
		}
		if (this.getColumn() == null) {
			this.getPlayers().remove(player);
			if (this.getPlayers().isEmpty()) {
				if (isLoading()) {
					AsyncWorldIOExecutor.dropQueuedColumnLoad(
						playerCubeMap.getWorld(), getPos().chunkXPos, getPos().chunkZPos, (c) -> loadedRunnable.run());
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
		if (getColumn() == null) {
			return false;
		}

		try {
			PacketColumn message = new PacketColumn(this.getColumn());
			for (EntityPlayerMP player : this.getPlayers()) {
				PacketDispatcher.sendTo(message, player);
				playerCubeMap.getWorldServer()
					.getEntityTracker()
					.sendLeashedEntitiesInChunk(player, this.getColumn());
			}
			this.setSentToPlayers.invoke(this, true);
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
		this.playerCubeMap.getCubeWatcher(CubePos.fromBlockCoords(x, y, z)).blockChanged(x, y, z);
	}

	@Override
	public void update() {
		//no-op, handles by cube entries
	}

	//containsPlayer, hasPlayerMatching, hasPlayerMatchingInRange, isAddedToChunkUpdateQueue, getChunk, getClosestPlayerDistance - ok

	@Nullable
	public Column getColumn() {
		return (Column) this.getChunk();
	}

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
}
