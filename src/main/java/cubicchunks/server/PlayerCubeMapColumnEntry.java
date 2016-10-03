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
import cubicchunks.CubicChunks;
import cubicchunks.network.PacketColumn;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.network.PacketUnloadColumn;
import cubicchunks.world.column.Column;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;

import static cubicchunks.util.AddressTools.getAddress;
import static cubicchunks.util.ReflectionUtil.getFieldGetterHandle;
import static cubicchunks.util.ReflectionUtil.getFieldSetterHandle;

public class PlayerCubeMapColumnEntry extends PlayerChunkMapEntry {

	private PlayerCubeMap playerCubeMap;
	private MethodHandle getPlayers = getFieldGetterHandle(PlayerChunkMapEntry.class, "field_187283_c");
	private MethodHandle setLastUpdateInhabitedTime = getFieldSetterHandle(PlayerChunkMapEntry.class, "field_187289_i");
	private MethodHandle setSentToPlayers = getFieldSetterHandle(PlayerChunkMapEntry.class, "field_187290_j");

	public PlayerCubeMapColumnEntry(PlayerCubeMap playerCubeMap, int cubeX, int cubeZ) {
		super(playerCubeMap, cubeX, cubeZ);
		this.playerCubeMap = playerCubeMap;
	}

	public ChunkPos getPos() {
		return super.getPos();
	}

	public void addPlayer(@Nonnull EntityPlayerMP player) {
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

		//TODO: ChunkWatchEvent.Watch: is it implemented correctly? at the moment I'm writing it Forge doesn't have this implemented, I think it should be here:
		MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(this.getPos(), player));
	}

	public void removePlayer(@Nonnull EntityPlayerMP player) {
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

	@Override
	//actually sendToPlayers
	public boolean sentToPlayers() {
		if (getColumn() == null) {
			return false;
		}

		try {
			PacketColumn message = new PacketColumn(this.getColumn());
			for (EntityPlayerMP player: this.getPlayers()) {
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
	public void sendNearbySpecialEntities(@Nonnull EntityPlayerMP player) {
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

	@Nullable
	public Column getColumn() {
		return (Column) this.getChunk();
	}
}
