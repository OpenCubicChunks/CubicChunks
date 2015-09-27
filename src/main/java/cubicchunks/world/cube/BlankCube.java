/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
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
package cubicchunks.world.cube;

import com.google.common.base.Predicate;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ChunkSection;

/**
 * Cube implementation used clientside used when cube is not loaded. 
 * It does nothing. Contains only Blocks.AIR.
 */
public class BlankCube extends Cube {

	public BlankCube(World world, Column column) {
		super(world, column, 0, 0, 0, false);
	}
	
	@Override
	public boolean isEmpty() {
		return true;
	}
	
	@Override
	public void setEmpty(boolean isEmpty) {}
	
	@Override
	public GeneratorStage getGeneratorStage() {
		//if client has it - it must be LIVE
		return GeneratorStage.LIVE;
	}
	
	@Override
	public void setGeneratorStage(GeneratorStage val) {}
	
	@Override
	public long getAddress() {
		return 0;
	}
	
	@Override
	public boolean containsBlockPos(BlockPos blockPos) {
		return false;
	}
	
	@Override
	public ChunkSection getStorage() {
		return null;
	}
	
	@Override
	public Block getBlockAt(final BlockPos pos) {
		return Blocks.AIR;
	}
	
	@Override
	public Block getBlockAt(final int localX, final int localY, final int localZ) {
		return Blocks.AIR;
	}
	
	@Override
	public IBlockState getBlockState(BlockPos pos) {
		return Blocks.AIR.getDefaultState();
	}
	
	@Override
	public IBlockState getBlockState(int localX, int localY, int localZ) {
		return Blocks.AIR.getDefaultState();
	}
	
	@Override
	public IBlockState setBlockState(BlockPos pos, IBlockState newBlockState) {
		return Blocks.AIR.getDefaultState();
	}
	
	@Override
	public IBlockState setBlockForGeneration(BlockPos pos, IBlockState newBlockState) {
		throw new UnsupportedOperationException("Eighter someone used BlankCube on server or someone generates terrain on client.");
	}
	
	@Override
	public boolean hasBlocks() {
		return false;
	}
	
	@Override
	public void addEntity(Entity entity) {}
	
	@Override
	public boolean removeEntity(Entity entity) {
		return false;
	}
	
	@Override
	public void findEntitiesExcept(Entity excludedEntity, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {}
	
	@Override
	public <T extends Entity> void findEntities(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {}
	
	@Override
	public void getMigratedEntities(List<Entity> out) {}
	
	@Override
	public BlockEntity getBlockEntity(BlockPos pos, Chunk.ChunkEntityCreationType creationType) {
		return null;
	}
	
	@Override
	public void addBlockEntity(BlockPos pos, BlockEntity blockEntity) {}
	
	@Override
	public void removeBlockEntity(BlockPos pos) {}
	
	@Override
	public void onLoad() {}
	
	@Override
	public void onUnload() {}
	
	@Override
	public boolean needsSaving() {
		return false;
	}
	
	@Override
	public void markSaved() {
	}
	
	@Override
	public boolean isUnderground(BlockPos pos) {
		return false;
	}
	
	@Override
	public int getBrightestLight(BlockPos pos, int skyLightDampeningTerm) {
		//TODO: BlankCube.getBrightnessLight - is it correct?
		return 15 - skyLightDampeningTerm;
	}
	
	@Override
	public int getLightValue(LightType lightType, BlockPos pos) {
		//TODO: BlankCube.getLightValue - maybe return 0?
		return 15;
	}
	
	@Override
	public void setLightValue(LightType lightType, BlockPos pos, int light) {}
	
	@Override
	public void doRandomTicks() {}
	
	@Override
	public void markForRenderUpdate() {}
}
