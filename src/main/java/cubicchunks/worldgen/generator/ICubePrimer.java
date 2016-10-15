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
package cubicchunks.worldgen.generator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public interface ICubePrimer {

	IBlockState DEFAULT_STATE = Blocks.AIR.getDefaultState();

	/**
	 * Gets a block state at the given location
	 * 
	 * @param x cube relative x
	 * @param y cube relative y
	 * @param z cube relative z
	 * @return the block state
	 */
	IBlockState getBlockState(int x, int y, int z);

	/**
	 * Sets a block state at the given location
	 * 
	 * @param x cube local x
	 * @param y cube local y
	 * @param z cube local z
	 * @param state the block state
	 */
	void setBlockState(int x, int y, int z, IBlockState state);

	/**
	 * Counting down from the highest block in the cube, find the first non-air
	 * block for the given location.<br>
	 * <br>
	 * NOTE: This will return -1 if there where no blocks under that location!<br>
	 * WARNING: It does not know if there are blocks over this cube!<br>
	 * 
	 * @param x cube relative x
	 * @param z cube relative x
	 * @return the height of the top non-air block at x, z or -1 if there was no block found
	 */
	int findGroundHeight(int x, int z);
}
