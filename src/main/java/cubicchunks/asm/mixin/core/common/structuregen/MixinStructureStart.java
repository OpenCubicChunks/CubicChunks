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
package cubicchunks.asm.mixin.core.common.structuregen;

import static cubicchunks.util.Coords.localToBlock;

import com.google.common.base.Preconditions;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.generator.custom.structure.feature.ICubicStructureStart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart implements ICubicStructureStart {

    @Shadow public abstract int getChunkPosX();

    @Shadow public abstract int getChunkPosZ();

    @Shadow protected StructureBoundingBox boundingBox;
    @Shadow protected List<StructureComponent> components;

    private int cubeY;
    private boolean isCubic = false;
    private CustomGeneratorSettings conf;

    // for storing arguments of methods that need to be redone on cubic init
    private boolean markAvailableHeightDone = false;
    private int minDepth;
    private int randY;

    @Override public int getChunkPosY() {
        return this.cubeY;
    }

    @Override public void initCubic(World world, CustomGeneratorSettings conf, int cubeY) {
        if (this.isCubic) {
            throw new IllegalStateException("Already initialized!");
        }
        this.cubeY = cubeY;
        this.isCubic = true;
        this.conf = conf;
        if (markAvailableHeightDone) {
            markAvailableHeightCubic(world, null, minDepth, null);
        }
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

    @Inject(method = "markAvailableHeight", at = @At("HEAD"), cancellable = true)
    private void markAvailableHeightCubic(World worldIn, @Nullable Random rand, int minDepth, CallbackInfo cbi) {
        if (!this.isCubic) {
            if (((ICubicWorld) worldIn).isCubicWorld()) {
                Preconditions.checkNotNull(rand);
                this.markAvailableHeightDone = true;
                this.minDepth = minDepth;
                this.randY = rand.nextInt(Cube.SIZE);
            }
            return;
        }
        int maxY = conf.getAverageHeight() - minDepth;
        int originalY = this.boundingBox.minY;
        int newY = localToBlock(getChunkPosY(), rand == null ? this.randY : rand.nextInt(Cube.SIZE));

        int newYMaxBound = boundingBox.getYSize() + newY;
        if (newYMaxBound > maxY) {
            newY -= newYMaxBound - maxY;
        }
        int offset = newY - originalY;
        // scrollOffset down by originalY, and then up to cube pos
        this.boundingBox.offset(0, offset, 0);

        for (StructureComponent component : this.components) {
            component.offset(0, offset, 0);
        }
        if (cbi != null) {
            cbi.cancel();
        }
    }

    @Inject(method = "setRandomHeight", at = @At("HEAD"), cancellable = true)
    private void setRandomHeightCubic(World worldIn, Random rand, int minY, int maxY, CallbackInfo cbi) {
        if (!this.isCubic) {
            return;
        }
        throw new UnsupportedOperationException("Not yet implemented");
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
}
