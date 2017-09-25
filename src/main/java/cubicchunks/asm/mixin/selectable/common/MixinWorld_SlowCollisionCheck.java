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

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import cubicchunks.world.ICubicWorld;
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

    @Shadow
    public abstract boolean isInsideWorldBorder(Entity p_191503_1_);

    @Overwrite(constraints = "MC_FORGE(23)")
    private boolean getCollisionBoxes(@Nullable Entity entity, AxisAlignedBB aabb, boolean flagArg, @Nullable List<AxisAlignedBB> aabbList) {
        int i = MathHelper.floor(aabb.minX) - 1;
        int j = MathHelper.ceil(aabb.maxX) + 1;
        int k = MathHelper.floor(aabb.minY) - 1;
        int l = MathHelper.ceil(aabb.maxY) + 1;
        int i1 = MathHelper.floor(aabb.minZ) - 1;
        int j1 = MathHelper.ceil(aabb.maxZ) + 1;
        WorldBorder worldborder = this.getWorldBorder();
        boolean flag = entity != null && entity.isOutsideBorder();
        boolean flag1 = entity != null && this.isInsideWorldBorder(entity);
        IBlockState iblockstate = Blocks.STONE.getDefaultState();
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();

        try {
            for (int k1 = i; k1 < j; ++k1) {
                for (int l1 = i1; l1 < j1; ++l1) {
                    boolean flag2 = k1 == i || k1 == j - 1;
                    boolean flag3 = l1 == i1 || l1 == j1 - 1;

                    // CubicChunks: change isBlockLoaded to isBlockColumnLoaded
                    if ((!flag2 || !flag3) && this.isBlockColumnLoaded(pos.setPos(k1, 64, l1))) {
                        for (int i2 = k; i2 < l; ++i2) {
                            // CubicChunks: add  && isBlockLoaded(pos.setPos(k1, i2, l1))
                            if ((!flag2 && !flag3 || i2 != l - 1) && isBlockLoaded(pos.setPos(k1, i2, l1))) {
                                if (flagArg) {
                                    if (k1 < -30000000 || k1 >= 30000000 || l1 < -30000000 || l1 >= 30000000) {
                                        boolean lvt_21_1_ = true;
                                        return lvt_21_1_;
                                    }
                                } else if (entity != null && flag == flag1) {
                                    entity.setOutsideBorder(!flag1);
                                }

                                pos.setPos(k1, i2, l1);
                                IBlockState iblockstate1;

                                if (!flagArg && !worldborder.contains(pos) && flag1) {
                                    iblockstate1 = iblockstate;
                                } else {
                                    iblockstate1 = this.getBlockState(pos);
                                }

                                iblockstate1
                                        .addCollisionBoxToList((World) (Object) this, pos, aabb, aabbList, entity, false);
                                net.minecraftforge.common.MinecraftForge.EVENT_BUS
                                        .post(new net.minecraftforge.event.world.GetCollisionBoxesEvent((World) (Object) this, null, aabb, aabbList));

                                if (flagArg && !aabbList.isEmpty()) {
                                    boolean flag5 = true;
                                    return flag5;
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