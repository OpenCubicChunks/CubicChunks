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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature.ICubicFeatureStart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart implements ICubicFeatureStart {

    @Shadow public abstract int getChunkPosX();

    @Shadow public abstract int getChunkPosZ();

    private int cubeY;
    private boolean isCubic = false;

    @Override public int getChunkPosY() {
        return this.cubeY;
    }

    @Override public void initCubic(World world, int cubeY) {
        if (this.isCubic) {
            throw new IllegalStateException("Already initialized!");
        }
        this.cubeY = cubeY;
        this.isCubic = true;
    }

    @Override public CubePos getCubePos() {
        return new CubePos(getX(), getY(), getZ());
    }

    @Inject(method = "writeStructureComponentsToNBT",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NBTTagCompound;setInteger(Ljava/lang/String;I)V", ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void writeYToNbt(int chunkX, int chunkZ, CallbackInfoReturnable<NBTTagCompound> cir, NBTTagCompound tag) {
        tag.setInteger("ChunkY", this.cubeY);
    }

    @Inject(method = "readStructureComponentsFromNBT", at = @At("HEAD"))
    private void readYFromNBT(World world, NBTTagCompound tag, CallbackInfo cbi) {
        if (tag.hasKey("ChunkY")) {
            this.isCubic = true;
            this.cubeY = tag.getInteger("ChunkY");
        }
    }

    @Override public int getX() {
        return getChunkPosX();
    }

    @Override public int getY() {
        return getChunkPosY();
    }

    @Override public int getZ() {
        return getChunkPosZ();
    }
    
    @Override public boolean isCubic() {
        return this.isCubic;
    }
}