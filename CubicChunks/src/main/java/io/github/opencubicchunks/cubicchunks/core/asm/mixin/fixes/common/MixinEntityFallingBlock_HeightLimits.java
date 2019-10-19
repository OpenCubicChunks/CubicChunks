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
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(EntityFallingBlock.class)
public abstract class MixinEntityFallingBlock_HeightLimits extends Entity {

    //to make javac happy
    public MixinEntityFallingBlock_HeightLimits(World worldIn) {
        super(worldIn);
    }

    /**
     * Fixes the following code:
     * <p>
     * else if (this.fallTime > 100 && !this.worldObj.isRemote && (blockpos1.getY() < 1 || blockpos1.getY() > 256) ||
     * this.fallTime > 600)
     */
    @Group(name = "onUpdateGetMinHeight", min = 1, max = 1)
    @ModifyConstant(
            method = "onUpdate",
            constant = @Constant(intValue = 1),
            slice = @Slice(
                    from = @At(value = "CONSTANT:ONE", args = "intValue=100"), // between this.fallTime > 100
                    to = @At(value = "CONSTANT:FIRST", args = "stringValue=doEntityDrops") // and this.world.getGameRules().getBoolean
                    // ("doEntityDrops")
            ))
    private int onUpdateGetMinHeight(int orig) {
        return ((ICubicWorld) world).getMinHeight();
    }

    @Group(name = "onUpdateGetMaxHeight", min = 1, max = 1)
    @ModifyConstant(
            method = "onUpdate",
            constant = @Constant(intValue = 256),
            slice = @Slice(
                    from = @At(value = "CONSTANT:ONE", args = "intValue=100"), // between this.fallTime > 100
                    to = @At(value = "CONSTANT:LAST", args = "stringValue=doEntityDrops") // and this.world.getGameRules().getBoolean("doEntityDrops")
            ))
    private int onUpdateGetMaxHeight(int orig) {
        return ((ICubicWorld) world).getMaxHeight();
    }
}
