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

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

    public MixinEntityLivingBase(World worldIn) {
        super(worldIn);
    }

    @ModifyArg(
            method = "travel",
            index = 1,
            at = @At(
                    target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;setPos(DDD)"
                            + "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;",
                    value = "INVOKE", ordinal = 1)
    )
    public double moveEntityWithHeading_getReplacedY(double y) {
        return this.posY;
    }

    @ModifyConstant(method = "attemptTeleport", constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO))
    private int getMinHeight(int orig) {
        return ((ICubicWorld) world).getMinHeight();
    }

    @Redirect(method = "attemptTeleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getY()I"))
    private int isBlockLoadedTeleportCheck(BlockPos blockPos) {
        // in this check:
        // while (!flag1 && blockpos.getY() > 0)
        // return very high Y if the block isn't loaded to avoid potentially infinite loop
        return world.isBlockLoaded(blockPos.down()) ? blockPos.getY() : (Integer.MIN_VALUE + 1);
    }
}
