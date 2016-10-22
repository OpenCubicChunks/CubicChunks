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

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

@SuppressWarnings("deprecation") // Block.BLOCK_STATE_IDS
public class CubePrimer implements ICubePrimer {
	private static final IBlockState DEFAULT_STATE = Blocks.AIR.getDefaultState();
	private final char[] data = new char[4096];

	public IBlockState getBlockState(int x, int y, int z) {
		IBlockState iblockstate = Block.BLOCK_STATE_IDS.getByValue(this.data[getBlockIndex(x, y, z)]);
		return iblockstate == null ? DEFAULT_STATE : iblockstate;
	}

	public void setBlockState(int x, int y, int z, IBlockState state) {
		this.data[getBlockIndex(x, y, z)] = (char) Block.BLOCK_STATE_IDS.get(state);
	}

	private static int getBlockIndex(int x, int y, int z) {
		return x << 8 | z << 4 | y;
	}

	public int findGroundHeight(int x, int z) {
		int i = (x << 8 | z << 4) + 15;

		for (int j = 15; j >= 0; --j) {
			IBlockState iblockstate = Block.BLOCK_STATE_IDS.getByValue(this.data[i + j]);

			if (iblockstate != null && iblockstate != DEFAULT_STATE) {
				return j;
			}
		}

		return -1; // no non-air block found
	}
}
