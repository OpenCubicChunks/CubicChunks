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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static cubicchunks.util.Coords.*;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Mixin(value = World.class, priority = 1001)
public abstract class MixinWorld_CollisionCheck implements ICubicWorld {

    @Inject(method = "getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;ZLjava/util/List;)Z",
            at = @At("HEAD"), cancellable = true)
    private void addBlocksCollisionBoundingBoxesToList(@Nullable Entity entity, AxisAlignedBB aabb, boolean breakOnWorldBorder,
            @Nullable List<AxisAlignedBB> aabbList, CallbackInfoReturnable<Boolean> ci) {
        if (this.isCubicWorld()) {
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
            for (int cx = blockToCube(x1); cx <= blockToCube(x2); cx++)
                for (int cy = blockToCube(y1); cy <= blockToCube(y2); cy++)
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
                            for (int x = minBlockX; x <= maxBlockX; x++)
                                for (int y = minBlockY; y <= maxBlockY; y++)
                                    for (int z = minBlockZ; z <= maxBlockZ; z++) {
                                        pooledmutableblockpos.setPos(x, y, z);
                                        IBlockState bstate = loadedCube.getStorage().get(blockToLocal(x),blockToLocal(y), blockToLocal(z));
                                        bstate.addCollisionBoxToList((World) (Object) this, pooledmutableblockpos, aabb, aabbList, entity, false);
                                        net.minecraftforge.common.MinecraftForge.EVENT_BUS
                                                .post(new net.minecraftforge.event.world.GetCollisionBoxesEvent((World) (Object) this, null, aabb,
                                                        aabbList));
                                    }
                        }
                    }
            pooledmutableblockpos.release();
            ci.setReturnValue(!aabbList.isEmpty());
            ci.cancel();
        }
    }
}
