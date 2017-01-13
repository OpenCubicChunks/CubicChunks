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
package cubicchunks.asm.mixin.fixes.common;

import static cubicchunks.asm.JvmNames.BLOCK_POS_GETY;
import static cubicchunks.asm.JvmNames.BLOCK_FALLING_CAN_FALL_THROUGH;
import static cubicchunks.asm.JvmNames.ENTITY_FALLING_BLOCK_CONSTRUCT;
import static cubicchunks.asm.JvmNames.WORLD_IS_AIR_BLOCK;
import static cubicchunks.asm.JvmNames.WORLD;
import static cubicchunks.asm.JvmNames.IBLOCK_STATE;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import cubicchunks.asm.MixinUtils;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(BlockFalling.class)
public abstract class MixinBlockFalling_HeightLimits extends Block {
    
    public MixinBlockFalling_HeightLimits(Material materialIn) {
        super(materialIn);
    }
    // First call - checking if BlockPos of block is > 0 to continue.
    @Redirect(method = "checkFallable", at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal=0), require = 1)
    private int checkHeightYReplace0(BlockPos pos1, World worldIn, BlockPos pos) {
        return MixinUtils.getReplacementY(worldIn, pos1);
    }

    // Second call - creating entity on block position. Skipped.
    // Third call - if area is not loaded call in cycle to attempt to find spot to land.
    @Redirect(method = "checkFallable", at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal=2), require = 1)
    private int checkHeightYReplace2(BlockPos pos1, World worldIn, BlockPos pos) {
        return MixinUtils.getReplacementY(worldIn, pos1);
    }

    // Forth call - check again if founded in previous cycle spot is above 0.
    @Redirect(method = "checkFallable", at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal=3), require = 1)
    private int checkHeightYReplace3(BlockPos pos1, World worldIn, BlockPos pos) {
        return MixinUtils.getReplacementY(worldIn, pos1);
    }

    
    @Redirect(method = "checkFallable", at = @At(value = "INVOKE", target = BLOCK_FALLING_CAN_FALL_THROUGH), require = 2)
    private boolean checkCanFallThrough(IBlockState blockState, World worldIn, BlockPos pos) {
        if(!((ICubicWorld)worldIn).isCubicWorld() || ((ICubicWorld)worldIn).getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos.down()))!=null) {
            return BlockFalling.canFallThrough(blockState);
        }
        return false;
    }
    
    @Redirect(method = "checkFallable", at = @At(value = "INVOKE", target = WORLD_IS_AIR_BLOCK), require = 2)
    private boolean checkIsAirBlock(World worldIn, BlockPos pos) {
        if(!((ICubicWorld)worldIn).isCubicWorld() || ((ICubicWorld)worldIn).getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos))!=null) {
            return worldIn.isAirBlock(pos);
        }
        return false;
    }
}
