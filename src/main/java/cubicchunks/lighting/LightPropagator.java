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
import static net.minecraft.crash.CrashReportCategory.getCoordinateInfo;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

import java.util.Collection;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Handles propagating light changes from blocks.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightPropagator {

    public static final Consumer<BlockPos> NULL_CALLBACK = p -> {
    };

    @Nonnull private LightUpdateQueue internalRelightQueue = new LightUpdateQueue();

    /**
     * Updates light at all BlockPos in given iterable.
     * <p>
     * For each block in the volume, if light source is brighter than the light value there was before - it will spread
     * light. If the light source is less bright than the current light value - the method will undo light spreading for
     * these blocks, and spread inside from the edges.
     * <p>
     * Additionally, light sources with light value higher or equal than the result of spreading out light will be ignored and left as is.
     * This behavior allows to reasonably efficiently update large volumes of terrain from da
     * This is intended to make initial light propagation more efficient in common cases when starting from complete darkness
     * without the half-broken initial MC-indev-like lightmap generation.
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
        internalRelightQueue.begin(centerPos);
        try {
            unspreadDecreased(coords, blocks, type, setLightCallback);
            spreadIncreased(coords, blocks, type, setLightCallback);
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

    private void unspreadDecreased(Iterable<BlockPos> coords, ILightBlockAccess blocks, EnumSkyBlock type, Consumer<BlockPos> setLightCallback) {
        // first add all decreased light values to the queue
        coords.forEach(pos -> {
            int emitted = blocks.getEmittedLight(pos, type);
            if (blocks.getLightFor(type, pos) > emitted) {
                //add the emitted value even if it's not used here - it will be used when relighting that area
                internalRelightQueue.put(pos, emitted, MAX_DISTANCE);
            }
        });
        // follow decreasing light values
        // setting each encountered value to 0 for easy spreading
        while (internalRelightQueue.next()) {
            int distance = internalRelightQueue.getDistance();
            assert distance >= 0;
            BlockPos pos = internalRelightQueue.getPos();

            int currentValue = blocks.getLightFor(type, pos);
            blocks.setLightFor(type, pos, 0);
            setLightCallback.accept(pos);

            for (EnumFacing direction : EnumFacing.values()) {
                BlockPos offsetPos = pos.offset(direction);
                int existingOffsetLight = blocks.getLightFor(type, offsetPos);
                if (existingOffsetLight == 0) {
                    continue;
                }
                int expectedSpread = getLightSpreadInto(blocks, offsetPos, currentValue);
                if (existingOffsetLight <= expectedSpread) {
                    // add the emitted value even if it's not used here - it will be used when relighting that area
                    // TODO: use 2 separate queues, places from where light continues spreading should be added there without all the others
                    // the distance set here will be reused by spreading code - unspreading follows decreasing light values and spreading
                    // decreases as it goes, so it can only ever decrease 16 times before dropping to 0
                    internalRelightQueue.put(offsetPos, blocks.getEmittedLight(offsetPos, type), distance - 1);
                }
            }
        }
    }

    private void spreadIncreased(Iterable<BlockPos> coords, ILightBlockAccess blocks, EnumSkyBlock type, Consumer<BlockPos> setLightCallback) {
        internalRelightQueue.resetIndex();
        // then handle everything
        coords.forEach(pos -> {
            int emitted = getExpectedLight(blocks, type, pos);//blocks.getEmittedLight(pos, type);
            // blocks where light decreased are already added (previous run over the queue)
            if (emitted > blocks.getLightFor(type, pos)) {
                internalRelightQueue.put(pos, emitted, MAX_DISTANCE);
                blocks.setLightFor(type, pos, emitted);
                setLightCallback.accept(pos);
            }
        });
        // spread out light values
        while (internalRelightQueue.next()) {
            int light = internalRelightQueue.getValue();

            int distance = internalRelightQueue.getDistance();
            assert distance >= 0;

            BlockPos pos = internalRelightQueue.getPos();

            for (EnumFacing direction : EnumFacing.values()) {
                BlockPos nextPos = pos.offset(direction);
                int newLight = getLightSpreadInto(blocks, nextPos, light);
                if (newLight <= blocks.getLightFor(type, nextPos) || newLight <= blocks.getEmittedLight(nextPos, type)) {
                    // can't go further, the next block already has the same or higher light value
                    continue;
                }
                blocks.setLightFor(type, nextPos, newLight);
                setLightCallback.accept(nextPos);

                if (newLight > 1) {
                    internalRelightQueue.put(nextPos, newLight, distance - 1);
                }
            }
        }
    }

    private int getExpectedLight(ILightBlockAccess blocks, EnumSkyBlock type, BlockPos pos) {
        return Math.max(blocks.getEmittedLight(pos, type), blocks.getLightFromNeighbors(type, pos));
    }

    private int getLightSpreadInto(ILightBlockAccess blocks, BlockPos pos, int currentValue) {
        return Math.max(0, currentValue - Math.max(1, blocks.getBlockLightOpacity(pos)));
    }
}
