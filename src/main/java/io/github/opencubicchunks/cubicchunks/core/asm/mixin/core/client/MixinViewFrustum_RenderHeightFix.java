/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Replace updateChunkPositions and getRenderChunk with cubic chunks versions
 * that support extended world height.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ViewFrustum.class)
public class MixinViewFrustum_RenderHeightFix {

    @Unique private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newSingleThreadExecutor((runnable) -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.setName("ViewFrustum RenderChunk position updater (CubicChunks)");
        return t;
    });

    @Shadow @Final protected World world;
    @SuppressWarnings("MismatchedReadAndWriteOfArray") @Shadow public RenderChunk[] renderChunks;
    @Shadow protected int countChunksX;
    @Shadow protected int countChunksY;
    @Shadow protected int countChunksZ;

    @Inject(method = "updateChunkPositions", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void updateChunkPositionsInject(double viewEntityX, double viewEntityZ, CallbackInfo cbi) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();

        int viewX = Coords.blockToCube(view.posX);
        int viewY = Coords.blockToCube(view.posY);
        int viewZ = Coords.blockToCube(view.posZ);
        int dx = countChunksX;
        int dy = countChunksY;
        int dz = countChunksZ;
        RenderChunk[] chunks = this.renderChunks;

        BACKGROUND_EXECUTOR.submit(() -> {
            int minX = viewX - (dx >> 1);
            int minY = viewY - (dy >> 1);
            int minZ = viewZ - (dz >> 1);
            int px = MathHelper.intFloorDiv(minX, dx) * dx;
            int py = MathHelper.intFloorDiv(minY, dy) * dy;
            int pz = MathHelper.intFloorDiv(minZ, dz) * dz;

            for (int zIndex = 0; zIndex < this.countChunksZ; zIndex++) {
                int blockZ = pz + zIndex;
                if (blockZ < minZ) {
                    blockZ += dz;
                }
                blockZ <<= 4;
                int idxZ = zIndex * this.countChunksY * this.countChunksX;

                for (int yIndex = 0; yIndex < this.countChunksY; yIndex++) {
                    int blockY = py + yIndex;
                    if (blockY < minY) {
                        blockY += dy;
                    }
                    blockY <<= 4;
                    int idxYZ = idxZ + yIndex * this.countChunksX;
                    for (int xIndex = 0; xIndex < this.countChunksX; xIndex++) {
                        int blockX = px + xIndex;
                        if (blockX < minX) {
                            blockX += dx;
                        }
                        blockX <<= 4;
                        RenderChunk renderer = chunks[idxYZ + xIndex];
                        renderer.setPosition(blockX, blockY, blockZ);
                    }
                }
            }
        });
        cbi.cancel();
    }

    @Inject(method = "getRenderChunk", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void getRenderChunkInject(BlockPos pos, CallbackInfoReturnable<RenderChunk> cbi) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        // treat the y dimension the same as all the rest
        int x = Coords.blockToCube(pos.getX());
        int y = Coords.blockToCube(pos.getY());
        int z = Coords.blockToCube(pos.getZ());
        x %= this.countChunksX;
        if (x < 0) {
            x += this.countChunksX;
        }
        y %= this.countChunksY;
        if (y < 0) {
            y += this.countChunksY;
        }
        z %= this.countChunksZ;
        if (z < 0) {
            z += this.countChunksZ;
        }
        final int index = (z * this.countChunksY + y) * this.countChunksX + x;
        RenderChunk renderChunk = this.renderChunks[index];
        cbi.setReturnValue(renderChunk);
    }
}
