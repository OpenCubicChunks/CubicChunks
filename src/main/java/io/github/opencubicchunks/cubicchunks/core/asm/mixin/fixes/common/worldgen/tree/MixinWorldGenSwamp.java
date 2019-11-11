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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen.tree;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenSwamp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(WorldGenSwamp.class)
public class MixinWorldGenSwamp {

    private int minPos;

    // a bad hack because vanilla has no height check ans assumes it will always reach non-water block
    // while it's "technically mostly true" for cubic chunks, it could go way out of the current cube
    // so cubic chunks needs to make it go down to min. population area instead.
    // Checking for material is the only loop termination condition mixin can intercept here

    // first we need to get the original position to store the minPos because it's the position argument being modified
    // as MC goes down
    @Inject(method = "generate", at = @At("HEAD"))
    private void onGenerate(World worldIn, Random rand, BlockPos position, CallbackInfoReturnable<Boolean> cbi) {
        this.minPos = Coords.getMinCubePopulationPos(position.getY());
    }

    // and then redirect Material.WATER access with ordinal zero,
    // return null to make sure it never succeeds for any block when below minPos
    @Redirect(method = "generate",
            at = @At(value = "FIELD", target = "Lnet/minecraft/block/material/Material;WATER:Lnet/minecraft/block/material/Material;", ordinal = 0))
    @Nullable private Material getReplaceMaterial_HeightCheckHack(World worldIn, Random rand, BlockPos position) {
        if (((ICubicWorld) worldIn).isCubicWorld() && position.getY() < minPos) {
            return null;
        }
        return Material.WATER;
    }

    // normal height check replacement:

    // the ordinal=0 is boolean flag = true
    @ModifyConstant(method = "generate", constant = @Constant(intValue = 1, ordinal = 1))
    private int getMinGenHeight(int orig, World worldIn, Random rand, BlockPos position) {
        return ((ICubicWorld) worldIn).getMinHeight() + 1;
    }

    @ModifyConstant(method = "generate",
                    constant = @Constant(intValue = 0, ordinal = 1,
                                         expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int getMinGenHeightCompareZero(int orig, World worldIn, Random rand, BlockPos position) {
        return ((ICubicWorld) worldIn).getMinHeight();
    }

    @ModifyConstant(method = "generate", constant = @Constant(intValue = 256))
    private int getMaxGenHeight(int orig, World worldIn, Random rand, BlockPos position) {
        return ((ICubicWorld) worldIn).getMaxHeight();
    }
}
