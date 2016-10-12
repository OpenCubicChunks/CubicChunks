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
package cubicchunks.world.column;

import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Collection;
import java.util.Collections;

public class BlankColumn extends Column {

	private final Cube blankCube;

	public BlankColumn(ICubeCache provider, ICubicWorld world, int cubeX, int cubeZ) {
		super(provider, world, cubeX, cubeZ);
		blankCube = new BlankCube(this);
	}

	// column overrides

	@Override
	public Cube getCube(int cubeY) {
		return blankCube;
	}

	@Override
	public Cube removeCube(int cubeY) {
		return blankCube;
	}

	public void addCube(Cube cube) {
	}

	@Override
	public void markSaved() {
	}

	// chunk overrides

	@Override
	public int getHeight(final BlockPos a1) {
		return 0;
	}

	@Override
	public int getHeightValue(final int a1, final int a2) {
		return 0;
	}

	@Override
	@Deprecated
	public int getTopFilledSegment() {
		return 0;
	}

	@Override
	@Deprecated
	public void generateSkylightMap() {
	}

	@Override
	public int getBlockLightOpacity(final BlockPos a1) {
		return 0;
	}

	@Override
	public IBlockState getBlockState(final BlockPos a1) {
		return Blocks.AIR.getDefaultState();
	}

	@Override
	public IBlockState setBlockState(final BlockPos a1, final IBlockState a2) {
		return null;
	}

	@Override
	public int getLightFor(final EnumSkyBlock lightType, final BlockPos pos) {
		return lightType.defaultLightValue;
	}

	@Override
	public void setLightFor(final EnumSkyBlock a1, final BlockPos a2, final int a3) {
	}

	//getBroghtestLight. It actually returns light value... 
	@Override
	public int getLightSubtracted(BlockPos pos, int skyLightDampeningTerm) {
		if (!this.getWorld().provider.getHasNoSky() && skyLightDampeningTerm < EnumSkyBlock.SKY.defaultLightValue) {
			return EnumSkyBlock.SKY.defaultLightValue - skyLightDampeningTerm;
		}
		return 0;
	}

	@Override
	public void addEntity(final Entity a1) {
	}

	@Override
	public void removeEntity(final Entity a1) {
	}

	@Override
	public void removeEntityAtIndex(final Entity a1, int a2) {
	}

	@Override
	public boolean canSeeSky(final BlockPos a1) {
		return true;
	}

	@Override
	public TileEntity getTileEntity(final BlockPos a1, final Chunk.EnumCreateEntityType a2) {
		return null;
	}

	@Override
	public void addTileEntity(final TileEntity tileEntity) {
	}

	@Override
	public void addTileEntity(final BlockPos a1, final TileEntity a2) {
	}

	@Override
	public void removeTileEntity(final BlockPos a1) {
	}

	@Override
	public void onChunkLoad() {
	}

	@Override
	public void onChunkUnload() {
	}

	@Override
	public void setChunkModified() {
	}

	@Override
	public boolean needsSaving(final boolean a1) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public BlockPos getPrecipitationHeight(final BlockPos pos) {
		return new BlockPos(pos.getX(), 0, pos.getZ());
	}

	@Override
	public void onTick(final boolean a1) {
	}

	@Override
	@Deprecated
	public boolean isPopulated() {
		return true;
	}

	@Override
	public void setStorageArrays(final ExtendedBlockStorage[] a1) {
	}

	@Override
	public void setBiomeArray(final byte[] a1) {
	}

	@Override
	@Deprecated
	public void resetRelightChecks() {
	}

	@Override
	@Deprecated
	public void enqueueRelightChecks() {
	}

	@Override
	public boolean isLoaded() {
		return true;
	}

	@Override
	public void setChunkLoaded(final boolean a1) {
	}

	@Override
	public void setHeightMap(final int[] a1) {
	}

	@Override
	@Deprecated
	public boolean isTerrainPopulated() {
		return true;
	}

	@Override
	public void setTerrainPopulated(final boolean a1) {
	}

	@Override
	@Deprecated
	public boolean isLightPopulated() {
		return true;
	}

	@Override
	public void setLightPopulated(final boolean a1) {
	}

	@Override
	public void setModified(final boolean a1) {
	}

	@Override
	public void setHasEntities(final boolean a1) {
	}

	@Override
	public void setLastSaveTime(final long a1) {
	}

	@Override
	public int getLowestHeight() {
		return 0;
	}

	@Override
	public long getInhabitedTime() {
		return 0;
	}

	@Override
	public void setInhabitedTime(final long a1) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Cube> getLoadedCubes() {
		return Collections.EMPTY_SET;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<Cube> getLoadedCubes(int startY, int endY) {
		return Collections.EMPTY_SET;
	}
}
