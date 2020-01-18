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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

@Pseudo
@Mixin(targets = "net.optifine.render.ChunkVisibility")
public class MixinChunkVisibility {

    /**
     * Quadrant counter
     */
    @Dynamic @Shadow(remap = false) private static int counter = 0;
    /**
     * Current max Y for quadrants already scanned in this scan.
     */
    @Dynamic @Shadow(remap = false) private static int iMaxStatic = -1;
    /**
     * Max Y after final test of all quadrants
     */
    @Dynamic @Shadow(remap = false) private static int iMaxStaticFinal = Coords.blockToCube(Integer.MAX_VALUE) - 1;

    @Dynamic @Shadow(remap = false) private static World worldLast = null;
    @Dynamic @Shadow(remap = false) private static int pcxLast = -2147483648;
    private static int pcyLast = -2147483648;
    @Dynamic @Shadow(remap = false) private static int pczLast = -2147483648;

    @Dynamic @Inject(method = "getMaxChunkY", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getMaxChunkYCC(World world, Entity viewEntity, int renderDistanceChunks, CallbackInfoReturnable<Integer> cbi) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        cbi.cancel();
        if (true) {
            cbi.setReturnValue(Integer.MAX_VALUE - 1);
            return;
        }
        int pcx = MathHelper.floor(viewEntity.posX) >> 4;
        int pcy = MathHelper.floor(viewEntity.posY) >> 4;
        int pcz = MathHelper.floor(viewEntity.posZ) >> 4;

        Chunk playerChunk = world.getChunk(pcx, pcz);
        int cxStart = pcx - renderDistanceChunks;
        int cxEnd = pcx + renderDistanceChunks;

        int cyStart = pcy - CubicChunksConfig.verticalCubeLoadDistance;
        int cyEnd = pcy + CubicChunksConfig.verticalCubeLoadDistance;

        int czStart = pcz - renderDistanceChunks;
        int czEnd = pcz + renderDistanceChunks;
        if (world != worldLast || pcx != pcxLast || pcy != pcyLast || pcz != pczLast) {
            counter = 0;
            iMaxStaticFinal = Coords.blockToCube(((IMinMaxHeight) world).getMaxHeight());
            worldLast = world;
            pcxLast = pcx;
            pcyLast = pcy;
            pczLast = pcz;
        }

        if (counter == 0) {
            iMaxStatic = Coords.blockToCube(Integer.MIN_VALUE) + 1;
        }

        int iMax = iMaxStatic;
        if ((counter & 1) == 0) {
            cxEnd = pcx;
        } else {
            cxStart = pcx;
        }
        if ((counter & 2) == 0) {
            cyEnd = pcy;
        } else {
            cyStart = pcy;
        }
        if ((counter & 4) == 0) {
            czEnd = pcz;
        } else {
            czStart = pcz;
        }

        for (int cx = cxStart; cx < cxEnd; ++cx) {
            for (int cz = czStart; cz < czEnd; ++cz) {
                Chunk chunk = world.getChunk(cx, cz);
                if (chunk.isEmpty()) {
                    continue;
                }
                Iterable<? extends ICube> cubes = ((IColumn) chunk).getLoadedCubes(cyEnd, cyStart);

                for (ICube cube : cubes) {
                    ExtendedBlockStorage ebs = cube.getStorage();
                    if (ebs != null && !ebs.isEmpty()) {
                        iMax = Math.max(iMax, cube.getY());
                        // it's sorted, in reverse, so can break when the first one is found
                        break;
                    }
                    ClassInheritanceMultiMap<Entity> cimm = cube.getEntitySet();
                    if (!cimm.isEmpty() && (chunk != playerChunk || cimm.size() != 1)) {
                        iMax = Math.max(iMax, cube.getY());
                        break;
                    }
                    Map<BlockPos, TileEntity> mapTileEntities = cube.getTileEntityMap();
                    if (!mapTileEntities.isEmpty()) {
                        Set<BlockPos> keys = mapTileEntities.keySet();

                        for (BlockPos pos : keys) {
                            int i = pos.getY() >> 4;
                            if (i > iMax) {
                                iMax = i;
                            }
                        }
                    }
                }
            }
        }

        if (counter < 7) {
            iMaxStatic = iMax;
            iMax = iMaxStaticFinal;
        } else {
            iMaxStaticFinal = iMax;
            iMaxStatic = -1;
        }

        counter = (counter + 1) % 8;
        cbi.setReturnValue(iMax << 4);
    }
}
