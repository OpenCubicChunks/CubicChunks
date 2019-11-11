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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessor_HeightLimit extends NodeProcessor {

    @ModifyConstant(method = "getStart", constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO))
    private int getMinHeight_GetStart(int originalY) {
        return ((ICubicWorld) this.entity.world).getMinHeight() + originalY;
    }

    // redirect getBlockState and check if the block is loaded
    @Redirect(method = "getStart", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/IBlockAccess;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState getLoadedBlockState_getStart(IBlockAccess access, BlockPos pos) {
        if (!entity.world.isBlockLoaded(pos)) {
            return Blocks.BEDROCK.getDefaultState();
        }
        return access.getBlockState(pos);
    }


    @ModifyConstant(
            method = "getSafePoint",
            constant = @Constant(
                    expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO,
                    ordinal = 1
            ))
    private int getMinHeight_GetSafePoint(int originalY) {
        return ((ICubicWorld) this.entity.world).getMinHeight() + originalY;
    }

    @ModifyConstant(
            method = "getPathNodeType(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/pathfinding/PathNodeType;",
            constant = @Constant(
                    intValue = 1, ordinal = 0
            ))
    private int getMinHeight_GetPathNodeType(int originalY, IBlockAccess blockaccessIn, int x, int y, int z) {
        return ((IMinMaxHeight) blockaccessIn).getMinHeight() + originalY;
    }
}
