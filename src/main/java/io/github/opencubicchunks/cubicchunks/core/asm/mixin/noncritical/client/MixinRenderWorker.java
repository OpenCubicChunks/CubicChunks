
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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinRenderWorker {

    @Redirect(method = "processTask", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderWorker;isChunkExisting(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;)Z",
            ordinal = 0))
    private boolean onIsChunkExisting(ChunkRenderWorker chunkRenderWorker, BlockPos pos, World world) {
        BlockPos.MutableBlockPos p = (BlockPos.MutableBlockPos) pos;
        if (((ICubicWorld) world).isCubicWorld()) {
            if (!this.isChunkExisting(p.move(EnumFacing.EAST, 16).move(EnumFacing.DOWN, 16), world)) {
                return false;
            }
            if (!this.isChunkExisting(p.move(EnumFacing.UP, 32), world)) {
                return false;
            }
            p.move(EnumFacing.DOWN, 16).move(EnumFacing.WEST, 16);
        }
        return this.isChunkExisting(p, world);
    }

    /**
     * @author Barteks2x
     * @reason use cubes in cubic chunks world
     */
    @Overwrite
    private boolean isChunkExisting(BlockPos pos, World worldIn) {
        if (((ICubicWorld) worldIn).isCubicWorld()) {
            return ((ICubicWorld) worldIn).getCubeCache()
                    .getLoadedCube(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4) != null;
        } else {
            return !worldIn.getChunk(pos.getX() >> 4, pos.getZ() >> 4).isEmpty();
        }
    }
}
