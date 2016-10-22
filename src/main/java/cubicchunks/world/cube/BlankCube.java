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
package cubicchunks.world.cube;

import com.google.common.base.Predicate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.List;

import cubicchunks.world.column.Column;

/**
 * Cube implementation used clientside used when cube is not loaded.
 * It does nothing. Contains only Blocks.AIR.
 */
public class BlankCube extends Cube {

	public BlankCube(Column column) {
		super(column, 0);
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public long getAddress() {
		return 0;
	}

	@Override
	public boolean containsBlockPos(BlockPos blockPos) {
		return false;
	}

	@Override
	public ExtendedBlockStorage getStorage() {
		return null;
	}

	@Override
	public IBlockState getBlockState(BlockPos pos) {
		return Blocks.AIR.getDefaultState();
	}

	@Override
	public IBlockState getBlockState(int blockX, int blockY, int blockZ) {
		return Blocks.AIR.getDefaultState();
	}

	@Override
	public IBlockState setBlockStateDirect(BlockPos pos, IBlockState newstate) {
		return null;
	}

	@Override
	public void addEntity(Entity entity) {
	}

	@Override
	public boolean removeEntity(Entity entity) {
		return false;
	}

	@Override
	public void getEntitiesWithinAABBForEntity(Entity excludedEntity, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
	}

	@Override
	public <T extends Entity> void getEntitiesOfTypeWithinAAAB(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
	}

	@Override
	public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType creationType) {
		return null;
	}

	@Override
	public void addTileEntity(BlockPos pos, TileEntity blockEntity) {
	}

	@Override
	public void removeTileEntity(BlockPos pos) {
	}

	@Override
	public void onLoad() {
	}

	@Override
	public void onUnload() {
	}

	@Override
	public boolean needsSaving() {
		return false;
	}

	@Override
	public void markSaved() {
	}

	@Override
	public int getLightSubtracted(BlockPos pos, int skyLightDampeningTerm) {
		return 15 - skyLightDampeningTerm;
	}

	@Override
	public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
		return lightType.defaultLightValue;
	}

	@Override
	public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
	}

	@Override
	public void markForRenderUpdate() {
	}
}
