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

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

@Mixin(World.class)
public abstract class MixinWorld_SlowCollisionCheck implements ICubicWorld {

    @Shadow public abstract boolean isInsideWorldBorder(Entity p_191503_1_);

    @Shadow public abstract WorldBorder getWorldBorder();

    @Shadow public abstract boolean isBlockLoaded(BlockPos pos);

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    /**
     * @author Barteks2x
     * @reason Original of that function use constant 64 to check if chunk loaded
     **/
    @Overwrite(constraints = "MC_FORGE(23)")
    private boolean getCollisionBoxes(@Nullable Entity entity, AxisAlignedBB aabb, boolean flagArg, @Nullable List<AxisAlignedBB> aabbList) {
        int minX = MathHelper.floor(aabb.minX) - 1;
        int maxX = MathHelper.ceil(aabb.maxX) + 1;
        int minY = MathHelper.floor(aabb.minY) - 1;
        int maxY = MathHelper.ceil(aabb.maxY) + 1;
        int minZ = MathHelper.floor(aabb.minZ) - 1;
        int maxZ = MathHelper.ceil(aabb.maxZ) + 1;
        WorldBorder worldborder = this.getWorldBorder();
        boolean entityOutsideOfBorder = entity != null && entity.isOutsideBorder();
        boolean entityInsideOfBorder = entity != null && this.isInsideWorldBorder(entity);
        IBlockState iblockstate = Blocks.STONE.getDefaultState();
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();

        try {
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    boolean isXboundary = x == minX || x == maxX - 1;
                    boolean isZBoundary = z == minZ || z == maxZ - 1;

                    // CubicChunks: change isBlockLoaded to isBlockColumnLoaded
                    if ((!isXboundary || !isZBoundary) && this.isBlockColumnLoaded(pos.setPos(x, 64, z))) {
                        for (int y = minY; y < maxY; ++y) {
                            // CubicChunks: add  && isBlockLoaded(pos.setPos(k1, i2, l1))
                            if ((!isXboundary && !isZBoundary || y != maxY - 1) && isBlockLoaded(pos.setPos(x, y, z))) {
                                if (flagArg) {
                                    if (x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) {
                                        return true;
                                    }
                                } else if (entity != null && entityOutsideOfBorder == entityInsideOfBorder) {
                                    entity.setOutsideBorder(!entityInsideOfBorder);
                                }

                                pos.setPos(x, y, z);
                                IBlockState iblockstate1;

                                if (!flagArg && !worldborder.contains(pos) && entityInsideOfBorder) {
                                    iblockstate1 = iblockstate;
                                } else {
                                    iblockstate1 = this.getBlockState(pos);
                                }

                                iblockstate1
                                        .addCollisionBoxToList((World) (Object) this, pos, aabb, aabbList, entity, false);
                                net.minecraftforge.common.MinecraftForge.EVENT_BUS
                                        .post(new net.minecraftforge.event.world.GetCollisionBoxesEvent((World) (Object) this, null, aabb, aabbList));

                                if (flagArg && !aabbList.isEmpty()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            pos.release();
        }

        return !aabbList.isEmpty();
    }
}
