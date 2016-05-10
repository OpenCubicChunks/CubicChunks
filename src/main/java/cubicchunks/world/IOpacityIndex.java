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
	 * Returns true if opacity at given position != 0, or false if it is 0
	 *
	 * @param localX local block X position (0..15).
	 * @param blockY block Y position (any integer from CubicChunks mod height range).
	 * @param localZ local block Z position (0..15)
	 *
	 * @return True is opacity != 0
	 */
	boolean isOpaque(int localX, int blockY, int localZ);

	/**
	 * Set opacity at given position.
	 *
	 * @param localX local block X position (0..15).
	 * @param blockY block Y position (any integer from CubicChunks mod height range).
	 * @param localZ local block Z position (0..15)
	 * @param opacity new opacity (0..255)
	 */
	void onOpacityChange(int localX, int blockY, int localZ, int opacity);

	/**
	 * Returns Y position of the top non-transparent block.
	 *
	 * @param localX local block X position (0..15).
	 * @param localZ local block Z position (0..15)
	 *
	 * @return Y position of the top non-transparent block, or null if one doesn't exist
	 */
	Integer getTopBlockY(int localX, int localZ);

	/**
	 * Returns Y position of the top non-transparent block that is below given blockY.
	 *
	 * @param localX local block X position (0..15).
	 * @param localZ local block Z position (0..15)
	 *
	 * @return Y position of the top non-transparent block below blockY, or null if one doesn't exist.
	 */
	Integer getTopBlockYBelow(int localX, int localZ, int blockY);

	/**
	 * Returns Y position of the bottom non-transparent block.
	 *
	 * @param localX local block X position (0..15).
	 * @param localZ local block Z position (0..15)
	 *
	 * @return Y position of the bottom non-transparent block, or null if one doesn't exist
	 */
	Integer getBottomBlockY(int localX, int localZ);

	/**
	 * Returns the lowest value that could be returned by getTopBlockY (for any localX and localZ values).
	 */
	int getLowestTopBlockY();
}
