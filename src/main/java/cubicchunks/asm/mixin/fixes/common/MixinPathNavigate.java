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
package cubicchunks.asm.mixin.fixes.common;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(PathNavigate.class)
public abstract class MixinPathNavigate {
	@Shadow
	protected EntityLiving entity;
	private ChunkCache chunkCache = null;
	private int lastCubePosX = 0;
	private int lastCubePosY = 0;
	private int lastCubePosZ = 0;

	@Redirect(method = "getPathToPos", at = @At(value = "NEW", target = "net/minecraft/world/ChunkCache"))
	private ChunkCache newChunkCacheToPosRedirect(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn) {
		this.updateChunkCache(worldIn, posFromIn, posToIn, subIn);
		return chunkCache;
	}
	
	@Redirect(method = "getPathToEntityLiving", at = @At(value = "NEW", target = "net/minecraft/world/ChunkCache"))
	private ChunkCache newChunkCacheToLivingRedirect(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn) {
		this.updateChunkCache(worldIn, posFromIn, posToIn, subIn);
		return chunkCache;
	}
	
	
	private void updateChunkCache(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn){
		if (chunkCache == null || lastCubePosX != entity.chunkCoordX || lastCubePosY != entity.chunkCoordY
				|| lastCubePosZ != entity.chunkCoordZ) {
			chunkCache = new ChunkCache(worldIn, posFromIn, posToIn, subIn);
			lastCubePosX = entity.chunkCoordX;
			lastCubePosY = entity.chunkCoordY;
			lastCubePosZ = entity.chunkCoordZ;
		}
	}
}
