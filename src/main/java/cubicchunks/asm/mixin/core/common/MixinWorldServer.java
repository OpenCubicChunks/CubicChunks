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
package cubicchunks.asm.mixin.core.common;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.server.ChunkGc;
import cubicchunks.server.CubeProviderServer;
import cubicchunks.server.PlayerCubeMap;
import cubicchunks.world.CubeWorldEntitySpawner;
import cubicchunks.world.CubicSaveHandler;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.NotCubicChunksWorldException;
import cubicchunks.world.provider.ICubicWorldProvider;
import mcp.MethodsReturnNonnullByDefault;

/**
 * Implementation of {@link ICubicWorldServer} interface.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(WorldServer.class)
@Implements(@Interface(iface = ICubicWorldServer.class, prefix = "world$"))
public abstract class MixinWorldServer extends MixinWorld implements ICubicWorldServer {
	@Shadow @Mutable @Final private PlayerChunkMap playerChunkMap;
	@Shadow @Mutable @Final private WorldEntitySpawner entitySpawner;
	@Shadow public boolean disableLevelSaving;

	@Nullable private ChunkGc chunkGc;
	@Nullable private FirstLightProcessor firstLightProcessor;

	@Override public void initCubicWorld() {
		super.initCubicWorld();
		this.isCubicWorld = true;

		this.entitySpawner = new CubeWorldEntitySpawner();

		this.chunkProvider = new CubeProviderServer(this,
			((ICubicWorldProvider) this.provider).createCubeGenerator());

		this.playerChunkMap = new PlayerCubeMap(this);
		this.chunkGc = new ChunkGc(getCubeCache());

		this.saveHandler = new CubicSaveHandler(this, this.getSaveHandler());

		this.firstLightProcessor = new FirstLightProcessor(this);
	}

	@Override public void tickCubicWorld() {
		if (!this.isCubicWorld()) {
			throw new NotCubicChunksWorldException();
		}
		assert chunkGc != null;
		this.chunkGc.tick();
	}

	@Override public CubeProviderServer getCubeCache() {
		if (!this.isCubicWorld()) {
			throw new NotCubicChunksWorldException();
		}
		return (CubeProviderServer) this.chunkProvider;
	}

	@Override public FirstLightProcessor getFirstLightProcessor() {
		if (!this.isCubicWorld()) {
			throw new NotCubicChunksWorldException();
		}
		assert this.firstLightProcessor != null;
		return this.firstLightProcessor;
	}
	//vanilla field accessors

	@Override public boolean getDisableLevelSaving() {
		return this.disableLevelSaving;
	}

	@Override public PlayerCubeMap getPlayerCubeMap() {
		if (!this.isCubicWorld()) {
			throw new NotCubicChunksWorldException();
		}
		return (PlayerCubeMap) this.playerChunkMap;
	}

	//vanilla methods
	//==============================================
	@Nullable @Shadow
	public abstract Biome.SpawnListEntry getSpawnListEntryForTypeAt(EnumCreatureType type, BlockPos pos);

	@Nullable @Intrinsic
	public Biome.SpawnListEntry world$getSpawnListEntryForTypeAt(EnumCreatureType type, BlockPos pos) {
		return this.getSpawnListEntryForTypeAt(type, pos);
	}

	//==============================================
	@Shadow
	public abstract boolean canCreatureTypeSpawnHere(EnumCreatureType type, Biome.SpawnListEntry entry, BlockPos pos);

	@Intrinsic
	public boolean world$canCreatureTypeSpawnHere(EnumCreatureType type, Biome.SpawnListEntry entry, BlockPos pos) {
		return this.canCreatureTypeSpawnHere(type, entry, pos);
	}
	//==============================================
}
