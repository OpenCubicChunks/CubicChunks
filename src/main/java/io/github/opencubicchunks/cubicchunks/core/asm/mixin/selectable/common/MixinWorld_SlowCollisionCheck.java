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

import com.google.common.collect.Lists;
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

    @Shadow public abstract boolean isInsideBorder(WorldBorder worldBorderIn, Entity entityIn);

    @Shadow public abstract List<Entity> getEntitiesWithinAABBExcludingEntity(@Nullable Entity entityIn, AxisAlignedBB bb);

    @Shadow public abstract WorldBorder getWorldBorder();

    @Shadow public abstract boolean isBlockLoaded(BlockPos p_isBlockLoaded_1_);

    @Shadow public abstract IBlockState getBlockState(BlockPos p_getBlockState_1_);

    /**
     * Gets a list of bounding boxes that intersect with the provided AABB.
     */
    @Overwrite
    public List<AxisAlignedBB> getCollisionBoxes(@Nullable Entity entityIn, AxisAlignedBB aabb) {
        List<AxisAlignedBB> list = Lists.<AxisAlignedBB>newArrayList();
        int i = MathHelper.floor(aabb.minX) - 1;
        int j = MathHelper.ceil(aabb.maxX) + 1;
        int k = MathHelper.floor(aabb.minY) - 1;
        int l = MathHelper.ceil(aabb.maxY) + 1;
        int i1 = MathHelper.floor(aabb.minZ) - 1;
        int j1 = MathHelper.ceil(aabb.maxZ) + 1;
        WorldBorder worldborder = this.getWorldBorder();
        boolean flag = entityIn != null && entityIn.isOutsideBorder();
        boolean flag1 = entityIn != null && this.isInsideBorder(worldborder, entityIn);
        IBlockState iblockstate = Blocks.STONE.getDefaultState();
        BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                int i2 = (k1 != i && k1 != j - 1 ? 0 : 1) + (l1 != i1 && l1 != j1 - 1 ? 0 : 1);

                // CubicChunks: isBlockLoaded->isBlockColumnLoaded
                if (i2 != 2 && this.isBlockColumnLoaded(blockpos$pooledmutableblockpos.setPos(k1, 64, l1))) {

                    for (int j2 = k; j2 < l; ++j2) {
                        if (i2 <= 0 || j2 != k && j2 != l - 1) {
                            blockpos$pooledmutableblockpos.setPos(k1, j2, l1);
                            // CubicChunks: isBlockLoaded check
                            if (!isBlockLoaded(blockpos$pooledmutableblockpos)) {
                                continue;
                            }
                            if (entityIn != null) {
                                if (flag && flag1) {
                                    entityIn.setOutsideBorder(false);
                                } else if (!flag && !flag1) {
                                    entityIn.setOutsideBorder(true);
                                }
                            }

                            IBlockState iblockstate1 = iblockstate;

                            if (worldborder.contains(blockpos$pooledmutableblockpos) || !flag1) {
                                iblockstate1 = this.getBlockState(blockpos$pooledmutableblockpos);
                            }

                            iblockstate1.addCollisionBoxToList((World) (Object) this, blockpos$pooledmutableblockpos, aabb, list, entityIn);
                        }
                    }
                }
            }
        }

        blockpos$pooledmutableblockpos.release();

        if (entityIn != null) {
            List<Entity> list1 = this.getEntitiesWithinAABBExcludingEntity(entityIn, aabb.expandXyz(0.25D));

            for (int k2 = 0; k2 < list1.size(); ++k2) {
                Entity entity = (Entity) list1.get(k2);

                if (!entityIn.isRidingSameEntity(entity)) {
                    AxisAlignedBB axisalignedbb = entity.getCollisionBoundingBox();

                    if (axisalignedbb != null && axisalignedbb.intersectsWith(aabb)) {
                        list.add(axisalignedbb);
                    }

                    axisalignedbb = entityIn.getCollisionBox(entity);

                    if (axisalignedbb != null && axisalignedbb.intersectsWith(aabb)) {
                        list.add(axisalignedbb);
                    }
                }
            }
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
                .post(new net.minecraftforge.event.world.GetCollisionBoxesEvent((World) (Object) this, entityIn, aabb, list));
        return list;
    }

    @Overwrite
    public List<AxisAlignedBB> getCollisionBoxes(AxisAlignedBB bb) {
        List<AxisAlignedBB> list = Lists.<AxisAlignedBB>newArrayList();
        int i = MathHelper.floor(bb.minX) - 1;
        int j = MathHelper.ceil(bb.maxX) + 1;
        int k = MathHelper.floor(bb.minY) - 1;
        int l = MathHelper.ceil(bb.maxY) + 1;
        int i1 = MathHelper.floor(bb.minZ) - 1;
        int j1 = MathHelper.ceil(bb.maxZ) + 1;
        BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                int i2 = (k1 != i && k1 != j - 1 ? 0 : 1) + (l1 != i1 && l1 != j1 - 1 ? 0 : 1);

                // cubic chunks: isBlockLoaded -> isBlockColumnLoaded
                if (i2 != 2 && this.isBlockColumnLoaded(blockpos$pooledmutableblockpos.setPos(k1, 64, l1))) {
                    for (int j2 = k; j2 < l; ++j2) {
                        if (i2 <= 0 || j2 != k && j2 != l - 1) {
                            // CubicChunks: isBlockLoaded check
                            blockpos$pooledmutableblockpos.setPos(k1, j2, l1);
                            if (!isBlockLoaded(blockpos$pooledmutableblockpos)) {
                                continue;
                            }
                            IBlockState iblockstate;

                            if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000) {
                                iblockstate = this.getBlockState(blockpos$pooledmutableblockpos);
                            } else {
                                iblockstate = Blocks.BEDROCK.getDefaultState();
                            }

                            iblockstate.addCollisionBoxToList((World) (Object) this, blockpos$pooledmutableblockpos, bb, list, (Entity) null);
                        }
                    }
                }
            }
        }

        blockpos$pooledmutableblockpos.release();
        return list;
    }
}