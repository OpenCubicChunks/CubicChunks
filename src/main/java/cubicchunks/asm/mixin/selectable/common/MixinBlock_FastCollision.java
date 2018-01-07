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
package cubicchunks.asm.mixin.selectable.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import cubicchunks.block.state.BlockStairsFieldBasedBlockStateContainer;
import cubicchunks.block.state.FullBlockBlockStateContainer;
import cubicchunks.block.state.NonCollidingBlockStateContainer;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

@Mixin(value = Block.class, priority = 1001)
public abstract class MixinBlock_FastCollision {

    /**
     * Make state.addCollisionBoxToList() faster by replacing calls of block state to direct adding of FULL_BLOCK_AABB for full blocks.
     *
     * @author Foghrye4
     **/
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;createBlockState()Lnet/minecraft/block/state/BlockStateContainer;"))
    public BlockStateContainer alterBlockStateCollection(Block block) {
        BlockStateContainer oldBlockStateContainer = this.createBlockState();
        // If someone already add a custom implementation, we should not do that.
        if (oldBlockStateContainer.getClass() != BlockStateContainer.class)
            return oldBlockStateContainer;
        if (block instanceof BlockStairs) {
            return new BlockStairsFieldBasedBlockStateContainer(block,
                    new IProperty[] {BlockStairs.FACING, BlockStairs.HALF, BlockStairs.SHAPE});
        }
        Collection<IProperty<?>> properties = oldBlockStateContainer.getProperties();
        boolean isFullBlock = true;
        for (IBlockState state : oldBlockStateContainer.getValidStates()) {
            if (!state.isFullCube())
                isFullBlock = false;
        }
        if (isFullBlock)
            return new FullBlockBlockStateContainer(block, properties.toArray(new IProperty<?>[0]));
        boolean isNonCollideableBlock = false;
        if (block instanceof BlockBush ||
                block instanceof BlockAir ||
                block instanceof BlockButton ||
                block instanceof BlockLiquid ||
                block instanceof BlockFire) {
            isNonCollideableBlock = true;
            List<AxisAlignedBB> collidingBoxes = new ArrayList<AxisAlignedBB>();
            try {
                for (IBlockState state : oldBlockStateContainer.getValidStates())
                    state.addCollisionBoxToList(null, BlockPos.ORIGIN, Block.FULL_BLOCK_AABB, collidingBoxes, null, false);
                if (!collidingBoxes.isEmpty())
                    isNonCollideableBlock = false;
            }
            // Catch all cases of modded extended classes accessing world here
            catch (Exception e) {
                isNonCollideableBlock = false;
            }
        }
        if (isNonCollideableBlock)
            return new NonCollidingBlockStateContainer(block, properties.toArray(new IProperty<?>[0]));
        return oldBlockStateContainer;
    }

    @Shadow
    abstract protected BlockStateContainer createBlockState();
}
