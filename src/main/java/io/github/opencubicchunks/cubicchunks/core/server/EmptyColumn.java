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
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class EmptyColumn extends Chunk
{
	public EmptyColumn(World worldIn, int x, int z)
	{
		super(worldIn, x, z);
	}

	/**
	 * Checks whether the chunk is at the X/Z location specified
	 */
	@Override public boolean isAtLocation(int x, int z)
	{
		return x == this.x && z == this.z;
	}

	/**
	 * Returns the value in the height map at this x, z coordinate in the chunk
	 */
	@Override public int getHeightValue(int x, int z)
	{
		return 0;
	}

	/**
	 * Generates the height map for a chunk from scratch
	 */
	@Override public void generateHeightMap()
	{
	}

	/**
	 * Generates the initial skylight map for the chunk upon generation or load.
	 */
	@Override public void generateSkylightMap()
	{
	}

	@Override public IBlockState getBlockState(BlockPos pos)
	{
		return Blocks.AIR.getDefaultState();
	}

	@Override public int getBlockLightOpacity(BlockPos pos)
	{
		return 255;
	}

	@Override public int getLightFor(EnumSkyBlock type, BlockPos pos)
	{
		return type.defaultLightValue;
	}

	@Override public void setLightFor(EnumSkyBlock type, BlockPos pos, int value)
	{
	}

	@Override public int getLightSubtracted(BlockPos pos, int amount)
	{
		return 0;
	}

	/**
	 * Adds an entity to the chunk.
	 */
	@Override public void addEntity(Entity entityIn)
	{
	}

	/**
	 * removes entity using its y chunk coordinate as its index
	 */
	@Override public void removeEntity(Entity entityIn)
	{
	}

	/**
	 * Removes entity at the specified index from the entity array.
	 */
	@Override public void removeEntityAtIndex(Entity entityIn, int index)
	{
	}

	@Override public boolean canSeeSky(BlockPos pos)
	{
		return false;
	}

	@Nullable
	@Override public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType creationMode)
	{
		return null;
	}

	@Override public void addTileEntity(TileEntity tileEntityIn)
	{
	}

	@Override public void addTileEntity(BlockPos pos, TileEntity tileEntityIn)
	{
	}

	@Override public void removeTileEntity(BlockPos pos)
	{
	}

	/**
	 * Called when this Chunk is loaded by the ChunkProvider
	 */
	@Override public void onLoad()
	{
	}

	/**
	 * Called when this Chunk is unloaded by the ChunkProvider
	 */
	@Override public void onUnload()
	{
	}

	/**
	 * Sets the isModified flag for this Chunk
	 */
	@Override public void markDirty()
	{
	}

	/**
	 * Fills the given list of all entities that intersect within the given bounding box that aren't the passed entity.
	 */
	@Override public void getEntitiesWithinAABBForEntity(@Nullable Entity entityIn, AxisAlignedBB aabb, List<Entity> listToFill, Predicate<? super Entity > filter)
	{
	}

	/**
	 * Gets all entities that can be assigned to the specified class.
	 */
	@Override public <T extends Entity> void getEntitiesOfTypeWithinAABB(Class <? extends T > entityClass, AxisAlignedBB aabb, List<T> listToFill, Predicate <? super T > filter)
	{
	}

	/**
	 * Returns true if this Chunk needs to be saved
	 */
	@Override public boolean needsSaving(boolean p_76601_1_)
	{
		return false;
	}

	@Override public Random getRandomWithSeed(long seed)
	{
		return new Random(this.getWorld().getSeed() + (long)(this.x * this.x * 4987142) + (long)(this.x * 5947611) + (long)(this.z * this.z) * 4392871L + (long)(this.z * 389711) ^ seed);
	}

	@Override public boolean isEmpty()
	{
		return true;
	}

	/**
	 * Returns whether the ExtendedBlockStorages containing levels (in blocks) from arg 1 to arg 2 are fully empty
	 * (true) or not (false).
	 */
	@Override public boolean isEmptyBetween(int startY, int endY)
	{
		return true;
	}
}
