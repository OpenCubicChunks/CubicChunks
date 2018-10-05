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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import javax.annotation.Nullable;

@Mixin(value = World.class, priority = 1001)
public abstract class MixinWorld_CollisionCheck implements ICubicWorldInternal {


    @Shadow public abstract boolean isInsideBorder(WorldBorder worldBorderIn, Entity entityIn);

    @Shadow public abstract List<Entity> getEntitiesWithinAABBExcludingEntity(@Nullable Entity entityIn, AxisAlignedBB bb);

    @Shadow public abstract WorldBorder getWorldBorder();

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    /**
     * Gets a list of bounding boxes that intersect with the provided AABB.
     */
    @Inject(method = "getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    public void getCollisionBoxes(@Nullable Entity entityIn, AxisAlignedBB aabb, CallbackInfoReturnable<List<AxisAlignedBB>> cir) {
        if (!isCubicWorld()) {
            return;
        }
        List<AxisAlignedBB> list = Lists.<AxisAlignedBB>newArrayList();

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

        WorldBorder worldborder = this.getWorldBorder();
        boolean flag = entityIn != null && entityIn.isOutsideBorder();
        boolean flag1 = entityIn != null && this.isInsideBorder(worldborder, entityIn);

        IBlockState iblockstate = Blocks.STONE.getDefaultState();

        BlockPos.PooledMutableBlockPos pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();
        for (int cx = blockToCube(x1); cx <= blockToCube(x2); cx++) {
            for (int cy = blockToCube(y1); cy <= blockToCube(y2); cy++) {
                for (int cz = blockToCube(z1); cz <= blockToCube(z2); cz++) {
                    CubePos coords = new CubePos(cx, cy, cz);
                    int minBlockX = coords.getMinBlockX();
                    int minBlockY = coords.getMinBlockY();
                    int minBlockZ = coords.getMinBlockZ();
                    int maxBlockX = coords.getMaxBlockX();
                    int maxBlockY = coords.getMaxBlockY();
                    int maxBlockZ = coords.getMaxBlockZ();
                    Cube loadedCube = this.getCubeCache().getLoadedCube(coords);
                    if (loadedCube != null && loadedCube.getStorage() != null) {
                        minBlockX = minBlockX > x1 ? minBlockX : x1;
                        minBlockY = minBlockY > y1 ? minBlockY : y1;
                        minBlockZ = minBlockZ > z1 ? minBlockZ : z1;
                        maxBlockX = maxBlockX < x2 ? maxBlockX : x2;
                        maxBlockY = maxBlockY < y2 ? maxBlockY : y2;
                        maxBlockZ = maxBlockZ < z2 ? maxBlockZ : z2;
                        for (int x = minBlockX; x <= maxBlockX; x++) {
                            for (int y = minBlockY; y <= maxBlockY; y++) {
                                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                                    pooledmutableblockpos.setPos(x, y, z);
                                    if (entityIn != null) {
                                        if (flag && flag1) {
                                            entityIn.setOutsideBorder(false);
                                        } else if (!flag && !flag1) {
                                            entityIn.setOutsideBorder(true);
                                        }
                                    }

                                    IBlockState iblockstate1 = iblockstate;

                                    if (worldborder.contains(pooledmutableblockpos) || !flag1) {
                                        iblockstate1 = this.getBlockState(pooledmutableblockpos);
                                    }

                                    iblockstate1.addCollisionBoxToList((World) (Object) this, pooledmutableblockpos, aabb, list, entityIn);
                                }
                            }
                        }
                    }
                }
            }
        }

        pooledmutableblockpos.release();

        if (entityIn != null) {
            List<Entity> list1 = this.getEntitiesWithinAABBExcludingEntity(entityIn, aabb.expandXyz(0.25D));

            for (Entity entity : list1) {

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
        cir.setReturnValue(list);
    }

    @Inject(method = "getCollisionBoxes(Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    public void getCollisionBoxes(AxisAlignedBB aabb, CallbackInfoReturnable<List<AxisAlignedBB>> cir) {
        if (!isCubicWorld()) {
            return;
        }
        List<AxisAlignedBB> list = Lists.<AxisAlignedBB>newArrayList();
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
        BlockPos.PooledMutableBlockPos pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();
        for (int cx = blockToCube(x1); cx <= blockToCube(x2); cx++) {
            for (int cy = blockToCube(y1); cy <= blockToCube(y2); cy++) {
                for (int cz = blockToCube(z1); cz <= blockToCube(z2); cz++) {
                    CubePos coords = new CubePos(cx, cy, cz);
                    int minBlockX = coords.getMinBlockX();
                    int minBlockY = coords.getMinBlockY();
                    int minBlockZ = coords.getMinBlockZ();
                    int maxBlockX = coords.getMaxBlockX();
                    int maxBlockY = coords.getMaxBlockY();
                    int maxBlockZ = coords.getMaxBlockZ();
                    Cube loadedCube = this.getCubeCache().getLoadedCube(coords);
                    if (loadedCube != null && loadedCube.getStorage() != null) {
                        minBlockX = minBlockX > x1 ? minBlockX : x1;
                        minBlockY = minBlockY > y1 ? minBlockY : y1;
                        minBlockZ = minBlockZ > z1 ? minBlockZ : z1;
                        maxBlockX = maxBlockX < x2 ? maxBlockX : x2;
                        maxBlockY = maxBlockY < y2 ? maxBlockY : y2;
                        maxBlockZ = maxBlockZ < z2 ? maxBlockZ : z2;
                        for (int x = minBlockX; x <= maxBlockX; x++) {
                            for (int y = minBlockY; y <= maxBlockY; y++) {
                                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                                    IBlockState iblockstate;

                                    if (minBlockX >= -30000000 && maxBlockX < 30000000 && minBlockZ >= -30000000 && maxBlockZ < 30000000) {
                                        iblockstate = this.getBlockState(pooledmutableblockpos);
                                    } else {
                                        iblockstate = Blocks.BEDROCK.getDefaultState();
                                    }

                                    iblockstate.addCollisionBoxToList((World) (Object) this, pooledmutableblockpos, aabb, list, (Entity) null);
                                }
                            }
                        }
                    }
                }
            }
        }

        pooledmutableblockpos.release();
        cir.setReturnValue(list);
    }
}
