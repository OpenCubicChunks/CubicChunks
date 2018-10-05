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

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Teleporter.class)
public class MixinTeleporter {

    // placeInExistingPortal fixes

    @Redirect(method = "placeInExistingPortal",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"),
              slice = @Slice(
                      from = @At(value = "NEW", target = "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;"),
                      to = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;down()Lnet/minecraft/util/math/BlockPos;")
              ))
    private BlockPos makeTopStartPos(BlockPos orig, int dx, int dy, int dz, Entity entity, float rotationYaw) {
        return orig.add(dx, 128, dz);
    }

    @ModifyConstant(method = "placeInExistingPortal",
                    constant = @Constant(
                            intValue = 0,
                            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
                            ordinal = 1))
    private int getScanBottomY(int zero, Entity entity, float rotationYaw) {
        return MathHelper.floor(entity.posY - 128);
    }

    // makePortal fixes

    @Redirect(method = "makePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;getActualHeight()I"))
    private int makePortalScanTopY(WorldServer world, Entity entity) {
        return MathHelper.floor(entity.posY + 128);
    }

    @ModifyConstant(method = "makePortal",
                    constant = @Constant(
                            intValue = 0,
                            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
                            ordinal = 1))
    private int makePortalScanBottomY1(int zero, Entity entity) {
        return MathHelper.floor(entity.posY - 128);
    }

    @ModifyConstant(method = "makePortal",
                    constant = @Constant(
                            intValue = 0,
                            expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO,
                            ordinal = 1)) // also 1 because different expandZeroConditions
    private int makePortalScanBottomY2(int zero, Entity entity) {
        return MathHelper.floor(entity.posY - 128);
    }

    @ModifyConstant(method = "makePortal",
                    constant = @Constant(
                            intValue = 0,
                            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
                            ordinal = 9)) // no clue why it has to be 9... I can't count that many zeros before that place
    private int makePortalScanBottomY3(int zero, Entity entity) {
        return MathHelper.floor(entity.posY - 128);
    }

    @ModifyConstant(method = "makePortal",
                    constant = @Constant(
                            intValue = 0,
                            expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO,
                            ordinal = 4))
    private int makePortalScanBottomY4(int zero, Entity entity) {
        return MathHelper.floor(entity.posY - 128);
    }

}
