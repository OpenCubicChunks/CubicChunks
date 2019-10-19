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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.api.util.Bits;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class LightUpdateQueue {

    /**
     * Enables additional error checks. Can be disable if performance becomes an issue.
     */
    private static final boolean DEBUG = true;

    // there is some redundant arithmetic, but it's there so the pattern is easily visible
    private static final int QUEUE_PART_SIZE = 64 * 1024;
    private static final int POS_BITS = 8;
    private static final int POS_X_OFFSET = POS_BITS * 0;
    private static final int POS_Y_OFFSET = POS_BITS * 1;
    private static final int POS_Z_OFFSET = POS_BITS * 2;

    private static final int VALUE_BITS = 4;
    private static final int DISTANCE_BITS = 4;
    private static final int VALUE_OFFSET = POS_BITS * 3 + VALUE_BITS * 0;
    private static final int DISTANCE_OFFSET = POS_BITS * 3 + VALUE_BITS * 1 + DISTANCE_BITS * 0;
    //note: position is signed
    /**
     * Minimum allowed block position relative to center position
     */
    static final int MIN_POS = Bits.getMinSigned(POS_BITS);
    /**
     * Maximum allowed block position relative to center position
     */
    static final int MAX_POS = Bits.getMaxSigned(POS_BITS);
    //note: value is unsigned
    static final int MIN_VALUE = 0;
    static final int MAX_VALUE = Bits.getMaxUnsigned(VALUE_BITS);
    static final int MIN_DISTANCE = 0;
    static final int MAX_DISTANCE = Bits.getMaxUnsigned(DISTANCE_BITS);

    @Nonnull private final ArrayQueueSegment start = new ArrayQueueSegment(QUEUE_PART_SIZE);
    @Nullable private ArrayQueueSegment currentReadQueue;
    @Nullable private ArrayQueueSegment currentWriteQueue;
    /**
     * Index of the previously read entry from current queue array
     */
    private int currentReadIndex;
    /**
     * Index of the next entry to write to in current queue array
     */
    private int nextWriteIndex;
    /**
     * Absolute index of the previously read entry from current queue array
     * (doesn't reset after next segment)
     */
    private int absoluteIndexRead;
    /**
     * Absolute index of the previously read entry from current queue array
     * (doesn't reset after next segment)
     */
    private int absoluteIndexWrite;
    /**
     * Write index at which last reset occurred
     */
    private int lastWrittenAbsoluteIndexBeforeReset;

    private int centerX;
    private int centerY;
    private int centerZ;

    //the read data:
    private int readValue;
    private int readDistance;
    private int readX;
    private int readY;
    private int readZ;
    private boolean isBeforeReset;

    void begin(BlockPos pos) {
        begin(pos.getX(), pos.getY(), pos.getZ());
    }

    void begin(int centerX, int centerY, int centerZ) {
        if (currentReadQueue != null) {
            throw new IllegalStateException("Called begin() in unclean state! Did you forget to call end()?");
        }
        this.currentWriteQueue = start;
        this.nextWriteIndex = 0;
        this.absoluteIndexWrite = 0;
        resetIndex();
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }

    /**
     * Resets queue index. All previously processed entries since last begin() call will appear again.
     */
    void resetIndex() {
        this.currentReadQueue = start;
        this.currentReadIndex = -1;//start at -1 so that next read is at 0
        this.lastWrittenAbsoluteIndexBeforeReset = this.absoluteIndexWrite - 1;
        this.absoluteIndexRead = -1;
    }

    void put(BlockPos pos, int value, int distance) {
        put(pos.getX(), pos.getY(), pos.getZ(), value, distance);
    }

    void put(int x, int y, int z, int value, int distance) {
        x -= this.centerX;
        y -= this.centerY;
        z -= this.centerZ;
        if (DEBUG) {
            if (x < MIN_POS || x > MAX_POS || y < MIN_POS || y > MAX_POS || z < MIN_POS || z > MAX_POS) {
                throw new IndexOutOfBoundsException("Position is out of bounds: (" +
                        (x + centerX) + ", " + (y + centerY) + ", " + (z + centerZ) + "), minPos is: (" +
                        (centerX + MIN_POS) + ", " + (centerY + MIN_POS) + ", " + (centerZ + MIN_POS) + "), maxPos is: (" +
                        (centerX + MAX_POS) + ", " + (centerY + MAX_POS) + ", " + (centerZ + MAX_POS) + ")");
            }
            if (value < MIN_VALUE || value > MAX_VALUE) {
                throw new RuntimeException("Value is out of bounds: " + value +
                        ", minValue is: " + MIN_VALUE + ", maxValue is: " + MAX_VALUE);
            }
        }
        int packed = Bits.packSignedToInt(x, POS_BITS, POS_X_OFFSET) |
                Bits.packSignedToInt(y, POS_BITS, POS_Y_OFFSET) |
                Bits.packSignedToInt(z, POS_BITS, POS_Z_OFFSET) |
                Bits.packSignedToInt(value, VALUE_BITS, VALUE_OFFSET) |
                Bits.packSignedToInt(distance, DISTANCE_BITS, DISTANCE_OFFSET);
        this.putPacked(packed);
    }

    private void putPacked(int packedValue) {
        currentWriteQueue.data[nextWriteIndex] = packedValue;
        nextWriteIndex++;
        absoluteIndexWrite++;
        //switch to next queue array?
        if (nextWriteIndex >= QUEUE_PART_SIZE) {
            nextWriteIndex = 0;
            if (currentWriteQueue.next == null) {
                currentWriteQueue.next = new ArrayQueueSegment(QUEUE_PART_SIZE);
                CubicChunks.LOGGER.debug("Adding LightUpdateQueue segment to " + this);
            }
            currentWriteQueue = currentWriteQueue.next;
        }
    }

    int getValue() {
        return readValue;
    }

    int getDistance() {
        return readDistance;
    }

    int getX() {
        return readX;
    }

    int getY() {
        return readY;
    }

    int getZ() {
        return readZ;
    }

    BlockPos getPos() {
        return new BlockPos(readX, readY, readZ);
    }

    boolean isBeforeReset() {
        return isBeforeReset;
    }

    /**
     * This should be called before reading set of values using getNext*
     *
     * @return true if there is next value, false otherwise
     */
    public boolean next() {
        currentReadIndex++;
        absoluteIndexRead++;
        if (currentReadIndex >= QUEUE_PART_SIZE) {
            if (currentReadQueue.next == null) {
                return false;
            }
            currentReadQueue = currentReadQueue.next;
            currentReadIndex = 0;
        }
        if (currentReadQueue == currentWriteQueue && currentReadIndex >= nextWriteIndex) {
            return false;
        }
        int packed = currentReadQueue.data[currentReadIndex];

        readX = centerX + Bits.unpackSigned(packed, POS_BITS, POS_X_OFFSET);
        readY = centerY + Bits.unpackSigned(packed, POS_BITS, POS_Y_OFFSET);
        readZ = centerZ + Bits.unpackSigned(packed, POS_BITS, POS_Z_OFFSET);
        readValue = Bits.unpackUnsigned(packed, VALUE_BITS, VALUE_OFFSET);
        readDistance = Bits.unpackUnsigned(packed, DISTANCE_BITS, DISTANCE_OFFSET);
        isBeforeReset = absoluteIndexRead <= lastWrittenAbsoluteIndexBeforeReset;
        return true;
    }

    void end() {
        if (currentReadQueue == null) {
            throw new IllegalStateException("Called end() without corresponding begin()!");
        }
        this.currentReadQueue = null;
        this.currentWriteQueue = null;
        this.currentReadIndex = 0;
        this.nextWriteIndex = 0;
        this.centerX = Integer.MAX_VALUE;
        this.centerY = Integer.MAX_VALUE;
        this.centerZ = Integer.MAX_VALUE;
    }

    private static class ArrayQueueSegment {

        private int[] data;
        @Nullable private ArrayQueueSegment next;

        ArrayQueueSegment(int initSize) {
            data = new int[initSize];
        }
    }
}
