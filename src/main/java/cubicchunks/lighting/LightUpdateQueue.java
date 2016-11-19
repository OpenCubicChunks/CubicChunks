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

import net.minecraft.util.math.BlockPos;

import cubicchunks.CubicChunks;
import cubicchunks.util.Bits;

class LightUpdateQueue {
	/**
	 * Enables additional error checks. Can be disable if performance becomes an issue.
	 */
	private static final boolean DEBUG = true;

	private static final int QUEUE_PART_SIZE = 64*1024;
	private static final int POS_BITS = 8;
	private static final int POS_X_OFFSET = POS_BITS*0;
	private static final int POS_Y_OFFSET = POS_BITS*1;
	private static final int POS_Z_OFFSET = POS_BITS*2;

	private static final int VALUE_BITS = 8;
	private static final int VALUE_OFFSET = POS_BITS*3;
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

	private ArrayQueueSegment start = new ArrayQueueSegment(QUEUE_PART_SIZE);
	private ArrayQueueSegment currentReadQueue;
	private ArrayQueueSegment currentWriteQueue;
	/**
	 * Index of the previously read entry from current queue array
	 */
	private int currentReadIndex;
	/**
	 * Index of the next entry to write to in current queue array
	 */
	private int nextWriteIndex;
	private int centerX;
	private int centerY;
	private int centerZ;

	//the read data:
	private int readValue;
	private int readX;
	private int readY;
	private int readZ;

	void begin(BlockPos pos) {
		begin(pos.getX(), pos.getY(), pos.getZ());
	}

	void begin(int centerX, int centerY, int centerZ) {
		if (currentReadQueue != null) {
			throw new IllegalStateException("Called begin() in unclean state! Did you forget to call end()?");
		}
		this.currentWriteQueue = start;
		this.nextWriteIndex = 0;
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
	}

	void put(BlockPos pos, int value) {
		put(pos.getX(), pos.getY(), pos.getZ(), value);
	}

	void put(int x, int y, int z, int value) {
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
			Bits.packSignedToInt(value, VALUE_BITS, VALUE_OFFSET);
		this.putPacked(packed);
	}

	private void putPacked(int packedValue) {
		currentWriteQueue.data[nextWriteIndex] = packedValue;
		nextWriteIndex++;
		//switch to next queue array?
		if (nextWriteIndex >= QUEUE_PART_SIZE) {
			nextWriteIndex = 0;
			if (currentWriteQueue.next == null) {
				currentWriteQueue.next = new ArrayQueueSegment(QUEUE_PART_SIZE);
			}
			currentWriteQueue = currentWriteQueue.next;
			CubicChunks.LOGGER.debug("Adding LightUpdateQueue segment to " + this);
		}
	}

	int getValue() {
		return readValue;
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

	/**
	 * This should be called before reading set of values using getNext*
	 *
	 * @return true if there is next value, false otherwise
	 */
	public boolean next() {
		currentReadIndex++;
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
		private ArrayQueueSegment next;

		ArrayQueueSegment(int initSize) {
			data = new int[initSize];
		}
	}
}
