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

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Teleporter.class)
public class MixinTeleporter {
    
    @Shadow
    @Final
    protected WorldServer world;
    @Shadow
    @Final
    protected Random random;

    
    // placeInExistingPortal fixes

    @Redirect(method = "placeInExistingPortal",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"),
              slice = @Slice(
                      from = @At(value = "NEW", target = "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;"),
                      to = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;down()Lnet/minecraft/util/math/BlockPos;")
              ))
    private BlockPos makeTopStartPos(BlockPos orig, int dx, int dy, int dz, Entity entity, float rotationYaw) {
        if (((ICubicWorld) world).isCubicWorld())
            return orig.add(dx, 128, dz);
        return orig.add(dx, dy, dz);
    }

    @ModifyConstant(method = "placeInExistingPortal",
                    constant = @Constant(
                            intValue = 0,
                            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
                            ordinal = 1))
    private int getScanBottomY(int zero, Entity entity, float rotationYaw) {
        if (((ICubicWorld) world).isCubicWorld())
            return MathHelper.floor(entity.posY - 128);
        return zero;
    }
    
    /**
     * @param entityIn entity to make portal for
     * @return true
     * @author Foghrye4
     * @reason Original function contain so much height limitations in a weird
     *         ways that fixing it with redirections is a way too complex task
     **/
    @Overwrite
    public boolean makePortal(Entity entityIn) {
        double distanceToPortal = -1.0D;
        int x = MathHelper.floor(entityIn.posX);
        int y = MathHelper.floor(entityIn.posY);
        int z = MathHelper.floor(entityIn.posZ);
        int x1 = x;
        int y1 = y;
        int z1 = z;
        int searchFromY = 70;
        int searchToY = world.getActualHeight() - 1;
        if (((ICubicWorld) world).isCubicWorld()) {
            searchFromY = y1 - 128;
            searchToY = y1 + 128;
        }
        int verticalPlane = 0;
        int random = this.random.nextInt(4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int ix = x - 16; ix <= x + 16; ++ix) {
            double dX = (double) ix + 0.5D - entityIn.posX;
            for (int iz = z - 16; iz <= z + 16; ++iz) {
                double dZ = (double) iz + 0.5D - entityIn.posZ;
                for (int minDepth = 3; minDepth >= 1; minDepth -= 2) {
                    nextY: for (int iy = searchToY; iy >= searchFromY; --iy) {
                        if (this.world.isAirBlock(pos.setPos(ix, iy, iz))) {
                            while (this.world.isAirBlock(pos)) {
                                pos.setPos(ix, --iy - 1, iz);
                                if (world.isOutsideBuildHeight(pos))
                                    break;
                            }

                            for (int verticalPlaneSelector = random; verticalPlaneSelector < random + 4; ++verticalPlaneSelector) {
                                int xPlane = verticalPlaneSelector % 2;
                                int zPlane = 1 - xPlane;

                                if (verticalPlaneSelector % 4 >= 2) {
                                    xPlane = -xPlane;
                                    zPlane = -zPlane;
                                }

                                for (int depth = 0; depth < minDepth; ++depth) {
                                    for (int width = 0; width < 4; ++width) {
                                        for (int height = -1; height < 4; ++height) {
                                            int ix1 = ix + (width - 1) * xPlane + depth * zPlane;
                                            int iy1 = iy + height;
                                            int iz1 = iz + (width - 1) * zPlane - depth * xPlane;
                                            pos.setPos(ix1, iy1, iz1);

                                            if (height < 0 && !this.world.getBlockState(pos).getMaterial().isSolid()
                                                    || height >= 0 && !this.world.isAirBlock(pos)) {
                                                continue nextY;
                                            }
                                        }
                                    }
                                }

                                double dY = (double) iy + 0.5D - entityIn.posY;
                                double distanceToPortal1 = dX * dX + dY * dY + dZ * dZ;

                                if (distanceToPortal < 0.0D || distanceToPortal1 < distanceToPortal) {
                                    distanceToPortal = distanceToPortal1;
                                    x1 = ix;
                                    y1 = iy;
                                    z1 = iz;
                                    verticalPlane = verticalPlaneSelector % 4;
                                }
                            }
                        }
                    }
                }
            }
        }

        int xPlane = verticalPlane % 2;
        int zPlane = 1 - xPlane;

        if (verticalPlane % 4 >= 2) {
            xPlane = -xPlane;
            zPlane = -zPlane;
        }

        // No empty space enough to place portal
        if (distanceToPortal < 0.0D) {
            for (int depth = -1; depth <= 1; ++depth) {
                for (int width = 1; width < 3; ++width) {
                    for (int height = -1; height < 3; ++height) {
                        int ix = x1 + (width - 1) * xPlane + depth * zPlane;
                        int iy = y1 + height;
                        int iz = z1 + (width - 1) * zPlane - depth * xPlane;
                        boolean isBase = height < 0;
                        this.world.setBlockState(new BlockPos(ix, iy, iz), isBase ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        IBlockState portal = Blocks.PORTAL.getDefaultState().withProperty(BlockPortal.AXIS, xPlane == 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);

        for (int width = 0; width < 4; ++width) {
            for (int height = -1; height < 4; ++height) {
                int ix = x1 + (width - 1) * xPlane;
                int iy = y1 + height;
                int iz = z1 + (width - 1) * zPlane;
                boolean isFrame = width == 0 || width == 3 || height == -1 || height == 3;
                this.world.setBlockState(new BlockPos(ix, iy, iz), isFrame ? Blocks.OBSIDIAN.getDefaultState() : portal, 2);
            }
        }

        for (int width = 0; width < 4; ++width) {
            for (int height = -1; height < 4; ++height) {
                int ix = x1 + (width - 1) * xPlane;
                int iy = y1 + height;
                int iz = z1 + (width - 1) * zPlane;
                BlockPos blockpos = new BlockPos(ix, iy, iz);
                this.world.notifyNeighborsOfStateChange(blockpos, this.world.getBlockState(blockpos).getBlock(), false);
            }
        }
        return true;
    }
}
