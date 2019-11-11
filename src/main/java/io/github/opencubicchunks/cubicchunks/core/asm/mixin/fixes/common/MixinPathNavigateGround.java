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

import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(PathNavigateGround.class)
public abstract class MixinPathNavigateGround extends PathNavigate {

    public MixinPathNavigateGround(EntityLiving entitylivingIn, World worldIn) {
        super(entitylivingIn, worldIn);
    }

    /**
     * @author Barteks2x
     * @reason original function not only uses {@code > 0} check, but also does not check if area is loaded.
     */
    @Overwrite
    public Path getPathToPos(BlockPos posIn) {
        BlockPos posOriginal = posIn;
        if (world.getBlockState(posIn).getMaterial() == Material.AIR) {
            BlockPos pos = posIn.down();

            // CubicChunks: instead of going down until it reaches zero, go down until unloaded block is reached
            while (pos.getY() > ((IMinMaxHeight) world).getMinHeight()
                    && world.isBlockLoaded(pos)
                    && world.getBlockState(pos).getMaterial() == Material.AIR) {
                pos = pos.down();
            }

            // CubicChunks check for min world height and block loaded check
            if (pos.getY() > ((IMinMaxHeight) world).getMinHeight() && world.isBlockLoaded(pos)) {
                return super.getPathToPos(pos.up());
            }

            // CubicChunks do isBlockLoaded check. If we ended up here, it's not loaded at the beginning so go 1 block up first
            pos = pos.up();
            while (pos.getY() < ((IMinMaxHeight) world).getMaxHeight()
                    && world.isBlockLoaded(pos)
                    && world.getBlockState(pos).getMaterial() == Material.AIR) {
                pos = pos.up();
            }

            posIn = pos;
        }

        if (!world.getBlockState(posIn).getMaterial().isSolid()) {
            return super.getPathToPos(posIn);
        } else {
            BlockPos pos = posIn.up();

            // found solid block... so now find block that isn't solid?
            // CubicChunks: add is block loaded check
            while (pos.getY() < ((IMinMaxHeight) world).getMaxHeight()
                    && world.isBlockLoaded(pos)
                    && this.world.getBlockState(pos).getMaterial().isSolid()) {
                pos = pos.up();
            }

            // CubicChunks: check if actually found something, if hit unloaded area - go to where we are already
            if(pos.getY() >= ((IMinMaxHeight) world).getMaxHeight() || !world.isBlockLoaded(pos)) {
                return super.getPathToPos(posOriginal);
            }

            return super.getPathToPos(pos);
        }
    }

}
