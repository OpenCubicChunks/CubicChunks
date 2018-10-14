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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ChunkCache.class)
public class MixinChunkCache_Cubic {

    @Shadow public World world;
    @Nonnull private Cube[][][] cubes;
    private int originX;
    private int originY;
    private int originZ;
    boolean isCubic = false;
    private int dx;
    private int dy;
    private int dz;

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    public void getBiome(BlockPos pos, CallbackInfoReturnable<Biome> cir) {
        if (!this.isCubic)
            return;
        int blockX = pos.getX();
        int blockY = pos.getY();
        int blockZ = pos.getZ();
        int cubeX = Coords.blockToCube(blockX) - originX;
        int cubeY = Coords.blockToCube(blockY) - originY;
        int cubeZ = Coords.blockToCube(blockZ) - originZ;
        if (cubeX < 0 || cubeX >= dx || cubeY < 0 || cubeY >= dy || cubeZ < 0 || cubeZ >= dz) {
            return;
        }
        Cube cube = this.cubes[cubeX][cubeY][cubeZ];
        cir.setReturnValue(cube.getBiome(pos, this.world.getBiomeProvider()));
        cir.cancel();
    }
}
