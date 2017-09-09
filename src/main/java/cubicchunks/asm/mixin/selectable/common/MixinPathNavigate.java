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
package cubicchunks.asm.mixin.selectable.common;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import cubicchunks.util.CubePos;
import cubicchunks.util.FastCubeBlockAccess;
import cubicchunks.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(PathNavigate.class)
public abstract class MixinPathNavigate {

	@Shadow
	@Nullable
	protected Path currentPath;

	@Shadow
	private BlockPos targetPos;

	@Shadow
	protected World world;

	@Shadow
	@Final
	private PathFinder pathFinder;

	@Shadow
	protected EntityLiving entity;

	@Shadow
	protected abstract boolean canNavigate();

	@Shadow
	public abstract float getPathSearchRange();

	/**
	 * @reason This method is awfully slow in cubic worlds
	 */
	@Nullable
	@Overwrite
	public Path getPathToPos(BlockPos pos) {
		if (!this.canNavigate())
			return null;

		if (this.currentPath != null && !this.currentPath.isFinished() && pos.equals(this.targetPos))
			return this.currentPath;

		this.targetPos = pos;
		float f = this.getPathSearchRange();
		BlockPos blockpos = new BlockPos(this.entity);
		int i = (int) (f + 8.0F);
		IBlockAccess blockcache;
		ICubicWorld cubicWorld = ((ICubicWorld) this.world);
		if (cubicWorld.isCubicWorld()) {
			CubePos from = CubePos.fromBlockCoords(blockpos.getX() - i, blockpos.getY() - i, blockpos.getZ() - i);
			CubePos to = CubePos.fromBlockCoords(blockpos.getX() + i, blockpos.getY() + i, blockpos.getZ() + i);
			if (!cubicWorld.testForCubes(from, to, Objects::nonNull))
				return null;
			blockcache = new FastCubeBlockAccess(cubicWorld, cubicWorld.getCubeCache(), from, to);
		} else {
			blockcache = new ChunkCache(this.world, blockpos.add(-i, -i, -i), blockpos.add(i, i, i), 0);
		}
		this.world.profiler.startSection("pathfind");
		Path path = this.pathFinder.findPath(blockcache, this.entity, this.targetPos, f);
		this.world.profiler.endSection();
		return path;
	}

	/**
	 * @reason This method is awfully slow in cubic worlds
	 */
	@Nullable
	@Overwrite
	public Path getPathToEntityLiving(Entity entityIn) {
		if (!this.canNavigate())
			return null;

		BlockPos blockpos = new BlockPos(entityIn);
		if (this.currentPath != null && !this.currentPath.isFinished() && blockpos.equals(this.targetPos))
			return this.currentPath;

		this.targetPos = blockpos;
		float f = this.getPathSearchRange();
		BlockPos blockpos1 = (new BlockPos(this.entity)).up();
		int i = (int) (f + 16.0F);
		IBlockAccess blockcache;
		ICubicWorld cubicWorld = ((ICubicWorld) this.world);
		if (cubicWorld.isCubicWorld()) {
			CubePos from = CubePos.fromBlockCoords(blockpos1.getX() - i, blockpos1.getY() - i, blockpos1.getZ() - i);
			CubePos to = CubePos.fromBlockCoords(blockpos1.getX() + i, blockpos1.getY() + i, blockpos1.getZ() + i);
			if (!cubicWorld.testForCubes(from, to, Objects::nonNull))
				return null;
			blockcache = new FastCubeBlockAccess(cubicWorld, cubicWorld.getCubeCache(), from, to);
		} else {
			blockcache = new ChunkCache(this.world, blockpos1.add(-i, -i, -i), blockpos1.add(i, i, i), 0);
		}
		this.world.profiler.startSection("pathfind");
		Path path = this.pathFinder.findPath(blockcache, this.entity, entityIn, f);
		this.world.profiler.endSection();
		return path;
	}
}
