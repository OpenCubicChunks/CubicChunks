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

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(BlockFalling.class)
public abstract class MixinBlockFalling_HeightLimits extends Block {
    
    public MixinBlockFalling_HeightLimits(Material materialIn) {
        super(materialIn);
    }

    // this would work without slices but adding them in case the code changes in the future

    // First call - checking if BlockPos of block is > 0 to continue.
    @Group(name = "checkFallable_getMinY1", min = 1, max = 1)
    @ModifyConstant(
            method = "checkFallable",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO),
            slice = @Slice(
                    from = @At(value = "INVOKE",
                            target = "Lnet/minecraft/block/BlockFalling;canFallThrough(Lnet/minecraft/block/state/IBlockState;)Z"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/block/BlockFalling;fallInstantly:Z")
            ), expect = 1)
    private int checkFallable_getMinY1(int orig, World worldIn, BlockPos pos) {
        return ((ICubicWorld) worldIn).getMinHeight();
    }

    // Second call - creating entity on block position. Skipped.
    // Third call - if area is not loaded call in cycle to attempt to find spot to land.
    // Forth call - check again if founded in previous cycle spot is above 0.
    @Group(name = "checkFallable_getMinY2", min = 2, max = 2)
    @ModifyConstant(
            method = "checkFallable",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO),
            slice = @Slice(
                    from = @At(value = "INVOKE:LAST",
                            target = "Lnet/minecraft/block/BlockFalling;canFallThrough(Lnet/minecraft/block/state/IBlockState;)Z"),
                    to = @At(value = "INVOKE:ONE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/block/state/IBlockState;)Z")
            ))
    private int checkFallable_getMinY2(int orig, World worldIn, BlockPos pos) {
        return ((ICubicWorld) worldIn).getMinHeight();
    }

    @Redirect(method = "checkFallable",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
            ),
            // skip the first getBlockState as failing this check won't reschedule the tick, and won't create an entity
            slice = @Slice(
                    from = @At(
                            value = "CONSTANT",
                            args = "intValue=32"
                    ),
                    to = @At(value = "TAIL")
            ),
            require = 2)
    private IBlockState checkCanFallThroughGetBlockState(World world, BlockPos pos, World worldIn, BlockPos origPos) {
        if (pos == origPos) {
            // this means a getBlockState that is actually used to do something
            return world.getBlockState(pos);
        }
        if (!((ICubicWorld) worldIn).isCubicWorld() || world.isBlockLoaded(pos.down(), false)) {
            return world.getBlockState(pos);
        }
        // a state that nothing can fall through
        return Blocks.BEDROCK.getDefaultState();
    }

    @Redirect(method = "checkFallable",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAirBlock(Lnet/minecraft/util/math/BlockPos;)Z"), require = 2)
    private boolean checkIsAirBlock(World worldIn, BlockPos pos) {
        if (!((ICubicWorld) worldIn).isCubicWorld() || worldIn.isBlockLoaded(pos.down(), false)) {
            return worldIn.isAirBlock(pos);
        }
        return false;
    }
}
