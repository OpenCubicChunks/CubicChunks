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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicChunkCache;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ChunkCache.class)
public class MixinChunkCache implements ICubicChunkCache {

    @Shadow public World world;
    @Nonnull private Cube[][][] cubes;
    private int originX;
    private int originY;
    private int originZ;
    boolean isCubic = false;
    private int dx;
    private int dy;
    private int dz;
    private IBlockState air = Blocks.AIR.getDefaultState();

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initChunkCache(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn, CallbackInfo ci) {
        if (worldIn == null || !((ICubicWorld) worldIn).isCubicWorld()
                || !(worldIn.getWorldType() instanceof ICubicWorldType)) {
            return;
        }
        this.isCubic = true;
        CubePos start = CubePos.fromBlockCoords(posFromIn.add(-subIn, -subIn, -subIn));
        CubePos end = CubePos.fromBlockCoords(posToIn.add(subIn, subIn, subIn));
        dx = Math.abs(end.getX() - start.getX()) + 1;
        dy = Math.abs(end.getY() - start.getY()) + 1;
        dz = Math.abs(end.getZ() - start.getZ()) + 1;
        ICubeProviderInternal prov = (ICubeProviderInternal) worldIn.getChunkProvider();
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
        Cube cube = this.getCube(pos);
        if (cube == null) {
            cir.setReturnValue(air);
            cir.cancel();
            return;
        }
        cir.setReturnValue(cube.getBlockState(pos));
        cir.cancel();
    }
    
    @Override
    @Nullable
    public Cube getCube(BlockPos pos) {
        if (!this.isCubic)
            return null;
        int blockX = pos.getX();
        int blockY = pos.getY();
        int blockZ = pos.getZ();
        int cubeX = Coords.blockToCube(blockX) - originX;
        int cubeY = Coords.blockToCube(blockY) - originY;
        int cubeZ = Coords.blockToCube(blockZ) - originZ;
        if (cubeX < 0 || cubeX >= dx || cubeY < 0 || cubeY >= dy || cubeZ < 0 || cubeZ >= dz) {
            return null;
        }
        return this.cubes[cubeX][cubeY][cubeZ];
    }
    
    @Override
    public boolean isCubic() {
        return this.isCubic;
    }
}
