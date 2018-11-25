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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessor_HeightLimit extends NodeProcessor {

    @Shadow
    abstract PathNodeType getPathNodeType(EntityLiving entitylivingIn, int x, int y, int z);

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


    @Inject(
            method = "getSafePoint",
            at = @At(value = "JUMP", opcode = Opcodes.IF_ACMPNE, ordinal = 4), cancellable = true)
    private void getMinHeight_GetSafePoint(int x, int y, int z, int range, double distance, EnumFacing facing,
            CallbackInfoReturnable<PathPoint> cir) {
        if (y > 0)
            return;
        PathNodeType pathnodetype = PathNodeType.OPEN;
        PathPoint pathpoint = this.openPoint(x, y, z);
        int maxFallHeight = this.entity.getMaxFallHeight();
        while (pathnodetype == PathNodeType.OPEN) {
            if (maxFallHeight-- <= 0)
                return;
            --y;
            pathnodetype = this.getPathNodeType(this.entity, x, y, z);
            float f = this.entity.getPathPriority(pathnodetype);

            if (pathnodetype != PathNodeType.OPEN && f >= 0.0F) {
                pathpoint = this.openPoint(x, y, z);
                pathpoint.nodeType = pathnodetype;
                pathpoint.costMalus = Math.max(pathpoint.costMalus, f);
                cir.setReturnValue(pathpoint);
                cir.cancel();
                return;
            }
            if (f < 0.0F) {
                return;
            }
        }
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
