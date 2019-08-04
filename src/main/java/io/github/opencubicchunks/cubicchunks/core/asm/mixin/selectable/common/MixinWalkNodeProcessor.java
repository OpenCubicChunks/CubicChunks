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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessor extends NodeProcessor {

    @Redirect(method = "getSafePoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;collidesWithAnyBlock(Lnet/minecraft/util/math/AxisAlignedBB;)Z"), require = 2)
    private boolean collidesWithAnyBlockRedirect(World worldIn, AxisAlignedBB aabb) {
        if (((ICubicWorld) worldIn).isCubicWorld()) {
            List<AxisAlignedBB> aabbList = new ArrayList<AxisAlignedBB>();
            double minX = aabb.minX;
            double minY = aabb.minY;
            double minZ = aabb.minZ;
            double maxX = aabb.maxX;
            double maxY = aabb.maxY;
            double maxZ = aabb.maxZ;
            int x1 = MathHelper.floor(minX) - 1;
            int y1 = MathHelper.floor(minY) - 1;
            int z1 = MathHelper.floor(minZ) - 1;
            int x2 = MathHelper.ceil(maxX) + 1;
            int y2 = MathHelper.ceil(maxY) + 1;
            int z2 = MathHelper.ceil(maxZ) + 1;
            for (MutableBlockPos pos : MutableBlockPos.getAllInBoxMutable(x1, y1, z1, x2, y2, z2)) {
                IBlockState bstate = blockaccess.getBlockState(pos);
                bstate.addCollisionBoxToList(worldIn, pos, aabb, aabbList,
                        entity, false);
                if (!aabbList.isEmpty())
                    return true;
            }
            return false;
        }
        return worldIn.collidesWithAnyBlock(aabb);
    }

    @ModifyVariable(method = "getPathNodeType", at = @At("HEAD"))
    public IBlockAccess getPathNodeTypeFromOwnBlockAccess(IBlockAccess world) {
        return this.blockaccess;
    }
}
