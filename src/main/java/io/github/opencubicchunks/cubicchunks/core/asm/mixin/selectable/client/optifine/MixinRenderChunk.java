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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineRenderChunk;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("target")
@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk implements IOptifineRenderChunk {

    @Shadow @Final private BlockPos.MutableBlockPos position;
    @Shadow private World world;
    @Dynamic @Shadow(remap = false) private RenderChunk[] renderChunkNeighboursValid;
    @Dynamic @Shadow(remap = false) private RenderChunk[] renderChunkNeighbours;
    private int regionY;
    @Dynamic @Shadow(remap = false) private int regionX;

    @Shadow public abstract BlockPos getPosition();

    private ICube cube;
    private boolean isCubic;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(World worldIn, RenderGlobal renderGlobalIn, int indexIn, CallbackInfo cbi) {
        this.isCubic = ((ICubicWorld) worldIn).isCubicWorld();
    }

    @Dynamic @Inject(method = "setPosition",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;chunk:Lnet/minecraft/world/chunk/Chunk;",
                    remap = false),
            remap = true)
    private void onSetChunk(int x, int y, int z, CallbackInfo cbi) {
        this.cube = null;
        this.isCubic = ((ICubicWorld) world).isCubicWorld();
        this.regionY = y & ~255;
    }

    @Dynamic @Inject(method = "updateRenderChunkNeighboursValid()V", at = @At("HEAD"), remap = false)
    private void onUpdateNeighbors(CallbackInfo cbi) {
        if (!isCubic) {
            return;
        }
        int y = this.getPosition().getY();
        int up = EnumFacing.UP.ordinal();
        int down = EnumFacing.DOWN.ordinal();
        this.renderChunkNeighboursValid[up] = this.renderChunkNeighbours[up].getPosition().getY() == y + 16 ?
                this.renderChunkNeighbours[up] : null;
        this.renderChunkNeighboursValid[down] = this.renderChunkNeighbours[down].getPosition().getY() == y - 16 ?
                this.renderChunkNeighbours[down] : null;
    }

    @Dynamic @ModifyArg(method = "preRenderBlocks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;setTranslation(DDD)V", ordinal = 0),
            index = 1
    )
    private double getRegionY(double dy) {
        return dy;
    }

    @Override public ICube getCube() {
        return this.getCube(this.position);
    }

    @Override public boolean isCubic() {
        return isCubic;
    }

    @Override public int getRegionY() {
        return this.regionY;
    }

    @Override public int getRegionX() {
        return this.regionX;
    }

    private ICube getCube(BlockPos posIn) {
        ICube cubeLocal = this.cube;
        if (cubeLocal != null && cubeLocal.isCubeLoaded()) {
            return cubeLocal;
        } else {
            cubeLocal = ((ICubicWorld) this.world).getCubeFromBlockCoords(posIn);
            this.cube = cubeLocal;
            return cubeLocal;
        }
    }

}
