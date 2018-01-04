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
package cubicchunks.asm.mixin.selectable.client;

import static cubicchunks.client.RenderConstants.RENDER_CHUNK_SIZE;
import static cubicchunks.client.RenderConstants.RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Replace updateChunkPositions and getRenderChunk with cubic chunks versions
 * that support extended world height.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(value = ViewFrustum.class, priority = 1001)
public class MixinViewFrustum_BiggerRenderChunk {

    @Shadow @Final protected World world;
    @SuppressWarnings("MismatchedReadAndWriteOfArray") @Shadow public RenderChunk[] renderChunks;
    @Shadow protected int countChunksX;
    @Shadow protected int countChunksY;
    @Shadow protected int countChunksZ;

    @Overwrite
    private int getBaseCoordinate(int playerWorldAxisPos, int sizeInBlocks, int index) {
        int relativeAxisPos = index * RENDER_CHUNK_SIZE;
        int j = relativeAxisPos - playerWorldAxisPos + sizeInBlocks / 2;
        if (j < 0) {
            j -= sizeInBlocks - 1;
        }
        return relativeAxisPos - j / sizeInBlocks * sizeInBlocks;
    }
    
    private int frustumUpdatePosRenderChunkX = Integer.MIN_VALUE;
    private int frustumUpdatePosRenderChunkY = Integer.MIN_VALUE;
    private int frustumUpdatePosRenderChunkZ = Integer.MIN_VALUE;

    @Inject(method = "updateChunkPositions", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void updateChunkPositionsInject(double viewEntityX, double viewEntityZ, CallbackInfo cbi) {
        cbi.cancel();
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (this.frustumUpdatePosRenderChunkX == view.chunkCoordX >> RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS
                && this.frustumUpdatePosRenderChunkY == view.chunkCoordY >> RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS
                && this.frustumUpdatePosRenderChunkZ == view.chunkCoordZ >> RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS)
            return;
        this.frustumUpdatePosRenderChunkX = view.chunkCoordX >> RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS;
        this.frustumUpdatePosRenderChunkY = view.chunkCoordY >> RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS;
        this.frustumUpdatePosRenderChunkZ = view.chunkCoordZ >> RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS;

        double x = view.posX;
        double y = view.posY;
        double z = view.posZ;

        // treat the y dimension the same as all the rest
        int viewX = MathHelper.floor(x) - RENDER_CHUNK_SIZE / 2;
        int viewY = MathHelper.floor(y) - RENDER_CHUNK_SIZE / 2;
        int viewZ = MathHelper.floor(z) - RENDER_CHUNK_SIZE / 2;

        int xSizeInBlocks = this.countChunksX * RENDER_CHUNK_SIZE;
        int ySizeInBlocks = this.countChunksY * RENDER_CHUNK_SIZE;
        int zSizeInBlocks = this.countChunksZ * RENDER_CHUNK_SIZE;

        for (int xIndex = 0; xIndex < this.countChunksX; xIndex++) {
            //getRendererBlockCoord
            int blockX = this.getBaseCoordinate(viewX, xSizeInBlocks, xIndex);

            for (int yIndex = 0; yIndex < this.countChunksY; yIndex++) {
                int blockY = this.getBaseCoordinate(viewY, ySizeInBlocks, yIndex);

                for (int zIndex = 0; zIndex < this.countChunksZ; zIndex++) {
                    int blockZ = this.getBaseCoordinate(viewZ, zSizeInBlocks, zIndex);

                    // get the renderer
                    int rendererIndex = (zIndex * this.countChunksY + yIndex) * this.countChunksX + xIndex;
                    RenderChunk renderer = this.renderChunks[rendererIndex];

                    // update the position if needed
                    BlockPos oldPos = renderer.getPosition();
                    if (oldPos.getX() != blockX || oldPos.getY() != blockY || oldPos.getZ() != blockZ) {
                        renderer.setPosition(blockX, blockY, blockZ);
                    }
                }
            }
        }
    }

    @Inject(method = "getRenderChunk", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void getRenderChunkInject(BlockPos pos, CallbackInfoReturnable<RenderChunk> cbi) {
        // treat the y dimension the same as all the rest
        int x = MathHelper.intFloorDiv(pos.getX(), RENDER_CHUNK_SIZE);
        int y = MathHelper.intFloorDiv(pos.getY(), RENDER_CHUNK_SIZE);
        int z = MathHelper.intFloorDiv(pos.getZ(), RENDER_CHUNK_SIZE);
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
        cbi.cancel();
        cbi.setReturnValue(renderChunk);
    }
    
    @ModifyConstant(method = "createRenderChunks", constant = @Constant(intValue = 16))
    public int onCreateRenderChunks(int oldValue){
        return RENDER_CHUNK_SIZE;
    }
    
    @ModifyConstant(method = "markBlocksForUpdate", constant = @Constant(intValue = 16))
    public int onMarkBlocksForUpdate(int oldValue) {
        return RENDER_CHUNK_SIZE;
    }
}
