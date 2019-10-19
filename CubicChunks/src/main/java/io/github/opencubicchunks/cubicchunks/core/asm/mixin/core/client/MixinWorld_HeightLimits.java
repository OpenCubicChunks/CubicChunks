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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(World.class)
public abstract class MixinWorld_HeightLimits implements ICubicWorld {

    @Final @Shadow public WorldProvider provider;

    @Shadow public abstract boolean isValid(BlockPos pos);

    @Shadow public abstract boolean isBlockLoaded(BlockPos pos);

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow public abstract int getLightFor(EnumSkyBlock type, BlockPos pos);

    // I don't know why but this is what the code is transformed into:
    /*
             if (pos.getY() < this.constant$getLightFromNeighborsFor_getMinHeight$zbd000(0)) {
                int var10002 = pos.getX();
                int var10003 = this.constant$getLightFromNeighborsFor_getMinHeight$zbd000(0);
                int var9 = pos.getZ();
                int var8 = var10003;
                pos = new BlockPos(var10002, this.modify$getLightFromNeighborsForGetMinHeight$zbd000(var8), var9);
            }
    */
    // it somehow finds 3 places to modify the constant zero when there are only 2 originally
    @Group(name = "getLightFromNeighborsFor", min = 2, max = 3)
    @ModifyConstant(
            method = "getLightFromNeighborsFor",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.LESS_THAN_ZERO),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getY()I"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getZ()I")
            ))
    private int getLightFromNeighborsFor_getMinHeight(int zero) {
        return getMinHeight();
    }

    @Group(name = "getLightFromNeighborsFor")
    @ModifyArg(method = "getLightFromNeighborsFor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;<init>(III)V"),
            index = 1,
            require = 1)
    private int getLightFromNeighborsForGetMinHeight(int origY) {
        return this.getMinHeight();
    }
}
