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
package cubicchunks.asm.mixin.core;

import cubicchunks.lighting.LightingManager;
import cubicchunks.util.AddressTools;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.NotCubicChunksWorldException;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;
import java.util.function.Predicate;

import static cubicchunks.util.Coords.blockToCube;

@Mixin(World.class)
@Implements(@Interface(iface = ICubicWorld.class, prefix = "world$"))
public abstract class MixinWorld implements ICubicWorld {

	@Shadow protected IChunkProvider chunkProvider;
	@Shadow @Final public WorldProvider provider;
	@Shadow @Final public Random rand;
	@Shadow @Final public boolean isRemote;
	@Shadow @Final @Mutable protected ISaveHandler saveHandler;

	protected LightingManager lightingManager;
	protected boolean isCubicWorld;
	protected int minHeight = 0, maxHeight = 256;
	private boolean wgenFullRelight;

	protected void setSaveHandler(ISaveHandler newSaveHandler) {
		this.saveHandler = newSaveHandler;
	}

	@Shadow public abstract WorldType getWorldType();

	@Shadow public abstract void loadEntities(Collection<Entity> entities);

	@Shadow public abstract void addTileEntities(Collection<TileEntity> values);

	@Shadow public abstract void unloadEntities(Collection<Entity> entities);

	@Shadow public abstract void removeTileEntity(BlockPos pos);

	@Shadow public abstract long getTotalWorldTime();

	@Shadow public abstract void setTileEntity(BlockPos blockpos, TileEntity tileentity);

	@Shadow public abstract void markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1);

	@Shadow public abstract boolean addTileEntity(TileEntity tileEntity);

	@Shadow
	public abstract void markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ);

	@Shadow public abstract long getSeed();

	@Shadow public abstract boolean checkLightFor(EnumSkyBlock sky, BlockPos pos);

	@Shadow public abstract ISaveHandler getSaveHandler();

	@Shadow public abstract MinecraftServer getMinecraftServer();

	@Shadow public abstract void addBlockEvent(BlockPos blockPos, Block i, int t, int p);

	@Shadow public abstract GameRules getGameRules();

	@Shadow public abstract WorldInfo getWorldInfo();

	@Shadow public abstract TileEntity getTileEntity(BlockPos pos);

	@Shadow public abstract boolean setBlockState(BlockPos blockPos, IBlockState blockState, int i);

	@Shadow public abstract IBlockState getBlockState(BlockPos pos);

	@Shadow public abstract boolean isAirBlock(BlockPos randomPos);

	@Shadow public abstract Biome getBiome(BlockPos cubeCenter);

	@Shadow public abstract BiomeProvider getBiomeProvider();

	@Shadow public abstract BlockPos getSpawnPoint();

	@Override public boolean isCubicWorld() {
		return this.isCubicWorld;
	}

	@Override public int getMinHeight() {
		return this.minHeight;
	}

	@Override public int getMaxHeight() {
		return this.maxHeight;
	}

	@Override public ICubeCache getCubeCache() {
		if(!this.isCubicWorld()) {
			throw new NotCubicChunksWorldException();
		}
		return (ICubeCache) this.chunkProvider;
	}

	@Override public LightingManager getLightingManager() {
		if(!this.isCubicWorld()) {
			throw new NotCubicChunksWorldException();
		}
		return this.lightingManager;
	}

	@Override
	public boolean testForCubes(CubeCoords start, CubeCoords end, @Nullable Predicate<Cube> cubeAllowed) {
		if(wgenFullRelight) {
			return true;
		}
		// convert block bounds to chunk bounds
		int minCubeX = start.getCubeX();
		int minCubeY = start.getCubeY();
		int minCubeZ = start.getCubeZ();
		int maxCubeX = end.getCubeX();
		int maxCubeY = end.getCubeY();
		int maxCubeZ = end.getCubeZ();

		for (int cubeX = minCubeX; cubeX <= maxCubeX; cubeX++) {
			for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
				for (int cubeZ = minCubeZ; cubeZ <= maxCubeZ; cubeZ++) {
					Cube cube = this.getCubeCache().getCube(cubeX, cubeY, cubeZ);
					if (cube == null || (cubeAllowed != null && !cubeAllowed.test(cube))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override public Cube getCubeForAddress(long address) {
		int x = AddressTools.getX(address);
		int y = AddressTools.getY(address);
		int z = AddressTools.getZ(address);

		return this.getCubeCache().getCube(x, y, z);
	}

	@Override public Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ) {
		return this.getCubeCache().getCube(cubeX, cubeY, cubeZ);
	}

	@Override public Cube getCubeFromBlockCoords(BlockPos pos) {
		return this.getCubeFromCubeCoords(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()));
	}

	//vanilla field accessors

	@Override public WorldProvider getProvider() {
		return this.provider;
	}

	@Override public Random getRand() {
		return this.rand;
	}

	@Override public boolean isRemote() {
		return this.isRemote;
	}

	//vanilla methods

	@Intrinsic public void world$loadEntities(Collection<Entity> entities) {
		this.loadEntities(entities);
	}

	@Intrinsic public void world$addTileEntities(Collection<TileEntity> values) {
		this.addTileEntities(values);
	}

	@Intrinsic public void world$unloadEntities(Collection<Entity> entities) {
		this.unloadEntities(entities);
	}

	@Intrinsic public void world$removeTileEntity(BlockPos pos) {
		this.removeTileEntity(pos);
	}

	@Intrinsic public long world$getTotalWorldTime() {
		return this.getTotalWorldTime();
	}

	@Intrinsic public void world$setTileEntity(BlockPos blockpos, TileEntity tileentity) {
		this.setTileEntity(blockpos, tileentity);
	}

	@Intrinsic public void world$markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1) {
		this.markBlockRangeForRenderUpdate(blockpos, blockpos1);
	}

	@Intrinsic public boolean world$addTileEntity(TileEntity tileEntity) {
		return this.addTileEntity(tileEntity);
	}

	@Intrinsic
	public void world$markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {
		this.markBlockRangeForRenderUpdate(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
	}

	@Intrinsic public long world$getSeed() {
		return this.getSeed();
	}

	@Intrinsic public boolean world$checkLightFor(EnumSkyBlock type, BlockPos pos) {
		return this.checkLightFor(type, pos);
	}

	@Intrinsic public ISaveHandler world$getSaveHandler() {
		return this.getSaveHandler();
	}

	@Intrinsic public MinecraftServer world$getMinecraftServer() {
		return this.getMinecraftServer();
	}

	@Intrinsic public void world$addBlockEvent(BlockPos blockPos, Block i, int t, int p) {
		this.addBlockEvent(blockPos, i, t, p);
	}

	@Intrinsic public GameRules world$getGameRules() {
		return this.getGameRules();
	}

	@Intrinsic public WorldInfo world$getWorldInfo() {
		return this.getWorldInfo();
	}

	@Intrinsic public TileEntity world$getTileEntity(BlockPos pos) {
		return this.getTileEntity(pos);
	}

	@Intrinsic public boolean world$setBlockState(BlockPos blockPos, IBlockState blockState, int i) {
		return this.setBlockState(blockPos, blockState, i);
	}

	@Intrinsic public IBlockState world$getBlockState(BlockPos pos) {
		return this.getBlockState(pos);
	}

	@Intrinsic public boolean world$isAirBlock(BlockPos pos) {
		return this.isAirBlock(pos);
	}

	@Intrinsic public Biome world$getBiome(BlockPos pos) {
		return this.getBiome(pos);
	}

	@Intrinsic public BiomeProvider world$getBiomeProvider() {
		return this.getBiomeProvider();
	}

	@Intrinsic public BlockPos world$getSpawnPoint() {
		return this.getSpawnPoint();
	}

	@Override
	public void setGeneratingWorld(boolean generating) {
		this.wgenFullRelight = generating;
	}
}
