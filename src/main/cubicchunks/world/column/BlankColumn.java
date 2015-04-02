/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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

import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ChunkSection;
import net.minecraft.world.gen.IChunkGenerator;

public class BlankColumn extends Column {
	
	public BlankColumn(World world, int cubeX, int cubeZ) {
		super(world, cubeX, cubeZ);
	}
	
	// column overrides
	
	@Override
	public Cube getOrCreateCube(int cubeY, boolean isModified) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Cube removeCube(int cubeY) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void markSaved() {
	}
	
	// chunk overrides
	
    @Override
	public int getHeightAtCoords(final BlockPos a1) {
		return 0;
	}
	
    @Override
	public int getHeightAtCoords(final int a1, final int a2) {
		return 0;
	}
	
    @Override
	public int getBlockStoreY() {
		return 0;
	}
	
    @Override
	public void generateSkylightMap() {
	}
	
    @Override
	public int getBlockOpacityAt(final BlockPos a1) {
		return 0;
	}
	
    @Override
	public Block getBlockAt(final int a1, final int a2, final int a3) {
		return Blocks.AIR;
	}
	
    @Override
	public Block getBlockAt(final BlockPos a1) {
		return Blocks.AIR;
	}
	
	@Override
	public IBlockState getBlockState(final BlockPos a1) {
		return Blocks.AIR.getDefaultState();
	}
	
	@Override
	public int getBlockMetadata(final BlockPos a1) {
		return 0;
	}
	
    @Override
	public IBlockState setBlockState(final BlockPos a1, final IBlockState a2) {
    	return null;
	}
	
	@Override
	public int getLightAt(final LightType lightType, final BlockPos pos) {
		switch (lightType) {
			case SKY: return 15;
			case BLOCK: return 0;
			default: return 0;
		}
	}
	
	@Override
	public void setLightAt(final LightType a1, final BlockPos a2, final int a3) {
	}
	
	@Override
	public int getBrightestLight(final BlockPos pos, final int skyLightDampeningTerm) {
		if (!this.world.dimension.hasNoSky() && skyLightDampeningTerm < LightType.SKY.defaultValue) {
			return LightType.SKY.defaultValue - skyLightDampeningTerm;
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
	public void removeEntity(final Entity a1, int a2) {
	}
	
	@Override
	public boolean canSeeSky(final BlockPos a1) {
		return true;
	}
	
	@Override
	public BlockEntity getBlockEntityAt(final BlockPos a1, final ChunkEntityCreationType a2) {
		return null;
	}
	
	@Override
	public void setBlockEntity(final BlockEntity a1) {
	}
	
	@Override
	public void setBlockEntityAt(final BlockPos a1, final BlockEntity a2) {
	}
	
	@Override
	public void removeBlockEntityAt(final BlockPos a1) {
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
	public void populateChunk(final IChunkGenerator a1, final IChunkGenerator a2, final int a3, final int a4) {
	}
	
	@Override
	public BlockPos getRainfallHeight(final BlockPos pos) {
		return new BlockPos(pos.getX(), 0, pos.getZ());
	}
	
	@Override
	public void tickChunk(final boolean a1) {
	}
	
	@Override
	public boolean isPopulated() {
		return true;
	}
	
	@Override
	public void setChunkSections(final ChunkSection[] a1) {
	}
	
	@Override
	public void readChunkIn(final byte[] a1, final int a2, final boolean a3) {
	}
	
	@Override
	public void setBiomeMap(final byte[] a1) {
	}
	
	@Override
	public void resetRelightChecks() {
	}
	
	@Override
	public void processRelightChecks() {
	}
	
	@Override
	public void queueRelightChecks() {
	}
	
	@Override
	public boolean isChunkLoaded() {
		return true;
	}
	
	@Override
	public void setChunkLoaded(final boolean a1) {
	}
	
	@Override
	public void setHeightMap(final int[] a1) {
	}
	
	@Override
	public boolean isTerrainPopulated() {
		return true;
	}
	
	@Override
	public void setTerrainPopulated(final boolean a1) {
	}
	
	@Override
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
	public int getHeightMapMinimum() {
		return 0;
	}
	
	@Override
	public long getInhabitedTime() {
		return 0;
	}
	
	@Override
	public void setInhabitedTime(final long a1) {
	}
}
