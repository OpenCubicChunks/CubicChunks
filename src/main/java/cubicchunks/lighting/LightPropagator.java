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
package cubicchunks.lighting;

import static cubicchunks.lighting.LightUpdateQueue.MAX_DISTANCE;
import static cubicchunks.lighting.LightUpdateQueue.MIN_DISTANCE;
import static net.minecraft.crash.CrashReportCategory.getCoordinateInfo;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Handles propagating light changes from blocks.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightPropagator {

    @Nonnull private LightUpdateQueue internalRelightQueue = new LightUpdateQueue();

    /**
     * Updates light at all BlockPos in given iterable.
     * <p>
     * For each block in the volume, if light source is brighter than the light value there was before - it will spread
     * light. If the light source is less bright than the current light value - the method will redo light spreading for
     * these blocks.
     * <p>
     * If updating lighting starting at these positions would never end, the algorithm will stop after walking {@link
     * LightUpdateQueue#MAX_DISTANCE} blocks
     * <p>
     * All coords to update must be between {@code centerPos.getX/Y/Z + }{@link LightUpdateQueue#MIN_POS} and {@code
     * centerPos.getX/Y/Z + }{@link LightUpdateQueue#MAX_POS} of centerPos (inclusive) with {@code
     * LightUpdateQueue#MAX_DISTANCE + 1} buffer radius.
     * <p>
     * WARNING: You probably shouldn't use this method directly and use
     * {@link LightingManager#relightMultiBlock(BlockPos, BlockPos, EnumSkyBlock, Consumer)} instead
     *
     * @param centerPos position relative to which calculations are done. usually average position.
     * @param coords contains all coords that need updating
     * @param blocks block access object. Must contain all blocks within radius of 17 blocks from all coords
     * @param type light type to update
     * @param setLightCallback this will be called for each position where light value is changed
     */
     public void propagateLight(BlockPos centerPos, Iterable<BlockPos> coords, ILightBlockAccess blocks, EnumSkyBlock type,
            Consumer<BlockPos> setLightCallback) {
        if (type == EnumSkyBlock.SKY && LightingManager.NO_SUNLIGHT_PROPAGATION) {
            return;
        }
        internalRelightQueue.begin(centerPos);
        try {
            // first add all decreased light values to the queue
            coords.forEach(pos -> {
                int emitted = blocks.getEmittedLight(pos, type);
                if (blocks.getLightFor(type, pos) > emitted) {
                    //add the emitted value even if it's not used here - it will be used when relighting that area
                    internalRelightQueue.put(pos, emitted, MAX_DISTANCE);
                }
            });
            // follow decreasing light values until it stops decreasing,
            // setting each encountered value to 0 for easy spreading
            while (internalRelightQueue.next()) {
                BlockPos pos = internalRelightQueue.getPos();
                int distance = internalRelightQueue.getDistance();

                int currentValue = blocks.getLightFor(type, pos);
                // note: min value is 0
                int lightFromNeighbors = getExpectedLight(blocks, type, pos);
                // if this is true, this blocks currently spreads light out, and has no light coming in from neighbors
                // lightFromNeighbors == currentValue-1 means that some neighbor has the same light value, or that
                // currentValue == 1 and all surrounding blocks have light 0
                // neither of these 2 cases can be ignored.
                // note that there is no need to handle case when some surrounding block has value one higher -
                // this would mean that the current block is in the light area from other block, no need to update that
                if (lightFromNeighbors <= currentValue - 1) {
                    // set it to 0 and add neighbors to the queue
                    if (!blocks.setLightFor(type, pos, 0)) {
                        this.markNeighborEdgeNeedLightUpdate(pos, blocks, type);
                        continue;
                    }
                    setLightCallback.accept(pos);
                    // if no distance left - stop spreading, so that it won't run into problems when updating too much
                    if (distance <= MIN_DISTANCE) {
                        continue;
                    }
                    // add all neighbors even those already checked - the check above will fail for them
                    // because currentValue-1 == -1 (already checked are set to 0)
                    // and min. possible lightFromNeighbors is 0
                    for (EnumFacing direction : EnumFacing.values()) {
                        BlockPos offset = pos.offset(direction);
                        //add the emitted value even if it's not used here - it will be used when relighting that area
                        internalRelightQueue.put(offset, blocks.getEmittedLight(offset, type), distance - 1);
                    }
                }
            }

            internalRelightQueue.resetIndex();

            // then handle everything
            coords.forEach(pos -> {
                int emitted = blocks.getEmittedLight(pos, type);
                // blocks where light decreased are already added (previous run over the queue)
                if (emitted > blocks.getLightFor(type, pos)) {
                    internalRelightQueue.put(pos, emitted, MAX_DISTANCE);
                    // do it here so that the loop below only needs to check if the light from this block can go into
                    // any neighbor. This simplifies logic for decreasing light value. Current code wouldn't work when
                    // decreasing sunlight below a block, because sunlight couldn't spread "into" any block made dark
                    // by light un-spreading code above
                    if (blocks.setLightFor(type, pos, emitted)) {
                        setLightCallback.accept(pos);
                    } else {
                        this.markNeighborEdgeNeedLightUpdate(pos, blocks, type);
                    }
                }
            });
            // spread out light values
            while (internalRelightQueue.next()) {
                BlockPos pos = internalRelightQueue.getPos();
                int distance = internalRelightQueue.isBeforeReset() ? MAX_DISTANCE : internalRelightQueue.getDistance();

                for (EnumFacing direction : EnumFacing.values()) {
                    BlockPos nextPos = pos.offset(direction);
                    int newLight = getExpectedLight(blocks, type, nextPos);
                    if (newLight <= blocks.getLightFor(type, nextPos)) {
                        // can't go further, the next block already has the same or higher light value
                        continue;
                    }
                    if (blocks.setLightFor(type, nextPos, newLight)) {
                        setLightCallback.accept(nextPos);
                    } else {
                        // If cube is not loaded we will notify neighbors so cube will update light when it loads.
                        blocks.markEdgeNeedLightUpdate(pos, type);
                        continue;
                    }

                    // if no distance left - stop spreading, so that it won't run into problems when updating too much
                    if (distance - 1 <= MIN_DISTANCE) {
                        continue;
                    }
                    internalRelightQueue.put(nextPos, newLight, distance - 1);
                }
            }
        } catch (Throwable t) {
            CrashReport report = CrashReport.makeCrashReport(t, "Updating skylight");
            CrashReportCategory category = report.makeCategory("Skylight update");
            category.addDetail("CenterLocation", () -> getCoordinateInfo(centerPos));
            int i = 0;
            for (BlockPos pos : coords) {
                category.addDetail("UpdateLocation" + i, () -> getCoordinateInfo(pos));
                i++;
            }
            throw new ReportedException(report);
        } finally {
            internalRelightQueue.end();
        }
    }

    private int getExpectedLight(ILightBlockAccess blocks, EnumSkyBlock type, BlockPos pos) {
        return Math.max(blocks.getEmittedLight(pos, type), blocks.getLightFromNeighbors(type, pos));
    }
    
    private void markNeighborEdgeNeedLightUpdate(BlockPos pos, ILightBlockAccess blocks, EnumSkyBlock type) {
        // If cube is not loaded we will notify neighbors so cube will update light when it loads.
        for (EnumFacing direction : EnumFacing.values()) {
            BlockPos offset = pos.offset(direction);
            blocks.markEdgeNeedLightUpdate(offset, type);
        }
    }
}
