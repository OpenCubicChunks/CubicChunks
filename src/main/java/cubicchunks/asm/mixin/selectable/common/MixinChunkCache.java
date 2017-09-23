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

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.type.ICubicWorldType;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ChunkCache.class)
public class MixinChunkCache {

	@Shadow
	public World world;
	@Nonnull
	private Cube[][][] cubes;
	private int originX;
	private int originY;
	private int originZ;
	boolean isCubic = false;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void initChunkCache(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn, CallbackInfo ci) {
		if (worldIn == null || !((ICubicWorld) worldIn).isCubicWorld()
				|| !(worldIn.getWorldType() instanceof ICubicWorldType)) {
			return;
		}
		this.isCubic = true;
		CubePos start = CubePos.fromBlockCoords(posFromIn.add(-subIn, -subIn, -subIn));
		CubePos end = CubePos.fromBlockCoords(posToIn.add(subIn, subIn, subIn));
		int dx = Math.abs(end.getX() - start.getX()) + 1;
		int dy = Math.abs(end.getY() - start.getY()) + 1;
		int dz = Math.abs(end.getZ() - start.getZ()) + 1;
		ICubeProvider prov = (ICubeProvider) worldIn.getChunkProvider();
		this.cubes = new Cube[dx][dy][dz];
		this.originX = Math.min(start.getX(), end.getX());
		this.originY = Math.min(start.getY(), end.getY());
		this.originZ = Math.min(start.getZ(), end.getZ());
		for (int relativeCubeX = 0; relativeCubeX < dx; relativeCubeX++) {
			for (int relativeCubeZ = 0; relativeCubeZ < dz; relativeCubeZ++) {
				for (int relativeCubeY = 0; relativeCubeY < dy; relativeCubeY++) {
					Cube cube = prov.getCube(originX + relativeCubeX, originY + relativeCubeY, originZ + relativeCubeZ);
					this.cubes[relativeCubeX][relativeCubeY][relativeCubeZ] = cube;
				}
			}
		}
	}

	@Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
	public void getBlockState(BlockPos pos, CallbackInfoReturnable<IBlockState> cir) {
		if (!this.isCubic)
			return;
		int worldBlockX = pos.getX();
		int worldBlockY = pos.getY();
		int worldBlockZ = pos.getZ();
		Cube cube = this.getCube(worldBlockX, worldBlockY, worldBlockZ);
		cir.setReturnValue(cube.getBlockState(worldBlockX, worldBlockY, worldBlockZ));
		cir.cancel();
	}

	private Cube getCube(int blockX, int blockY, int blockZ) {
		int cubeX = Coords.blockToCube(blockX);
		int cubeY = Coords.blockToCube(blockY);
		int cubeZ = Coords.blockToCube(blockZ);
		return this.cubes[cubeX - originX][cubeY - originY][cubeZ - originZ];
	}
}
