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
package cubicchunks.world;

public interface IOpacityIndex {

	/**
	 * Returns true if the opacity at the given position != 0 or false otherwise.
	 *
	 * @param localX local block x-coordinate (0..15)
	 * @param blockY global block y-coordinate
	 * @param localZ local block z-coordinate (0..15)
	 *
	 * @return true if the opacity at the given position != 0
	 */
	boolean isOpaque(int localX, int blockY, int localZ);

	/**
	 * Sets the opacity at the given position to the given value.
	 *
	 * @param localX local block x-coordinate (0..15)
	 * @param blockY global block y-coordinate
	 * @param localZ local block z-coordinate (0..15)
	 * @param opacity new opacity (0..255)
	 */
	void onOpacityChange(int localX, int blockY, int localZ, int opacity);

	/**
	 * Returns true if the block at the given position is occluded by a known non-opaque block further up.
	 *
	 * @param localX local block x-coordinate (0..15)
	 * @param blockY global block y-coordinate
	 * @param localZ local block z-coordinate (0..15)
	 * @return true if there exists a known non-opaque block above the given y-coordinate
	 */
	boolean isOccluded(int localX, int blockY, int localZ);

	/**
	 * Returns the y-coordinate of the highest non-transparent block in the specified block-column.
	 *
	 * @param localX local block x-coordinate (0..15)
	 * @param localZ local block z-coordinate (0..15)
	 *
	 * @return y-coordinate of the highest non-transparent block, or null if none exists
	 */
	Integer getTopBlockY(int localX, int localZ);

	/**
	 * Returns the y-coordinate of the highest non-transparent block that is below the given blockY.
	 *
	 * @param localX local block x-coordinate (0..15)
	 * @param localZ local block z-coordinate (0..15)
	 *
	 * @return y-coordinate of the highest non-transparent block below blockY, or null if none exists
	 */
	Integer getTopBlockYBelow(int localX, int localZ, int blockY);

	/**
	 * Returns y-coordinate n of the lowest non-transparent block.
	 *
	 * @param localX local block x-coordinate (0..15)
	 * @param localZ local block z-coordinate(0..15)
	 *
	 * @return y-coordinate of the lowest non-transparent block, or null if none exists
	 */
	Integer getBottomBlockY(int localX, int localZ);

	/**
	 * Out of the highest non-opaque blocks from all block columns in the column, returns the y-coordinate of the lowest
	 * block.
	 */
	int getLowestTopBlockY();

	int[] getHeightmap();
}
