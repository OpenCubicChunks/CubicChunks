/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.util;

import java.util.TreeMap;

import net.minecraft.util.BlockPos;

public class CubeBlockMap<T> extends TreeMap<Integer,T> {
	
	private static final long serialVersionUID = -356507892710221222L;
	
	// each coordinate is only 4 bits since a chunk is 16x16x16
	private static final int XSize = 4;
	private static final int YSize = 4;
	private static final int ZSize = 4;
	
	private static final int ZOffset = 0;
	private static final int YOffset = ZOffset + ZSize;
	private static final int XOffset = YOffset + YSize;
	
	public T put(BlockPos pos, T val) {
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		return put(x, y, z, val);
	}
	
	public T put(int x, int y, int z, T val) {
		return put(getKey(x, y, z), val);
	}
	
	public T get(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		return get(x, y, z);
	}
	
	public T get(int x, int y, int z) {
		return get(getKey(x, y, z));
	}
	
	public T remove(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		return remove(x, y, z);
	}
	
	public T remove(int x, int y, int z) {
		return remove(getKey(x, y, z));
	}
	
	private int getKey(int x, int y, int z) {
		return Bits.packSignedToInt(x, XSize, XOffset) | Bits.packSignedToInt(y, YSize, YOffset) | Bits.packSignedToInt(z, ZSize, ZOffset);
	}
	
	public int getKeyX(int key) {
		return Bits.unpackSigned(key, XSize, XOffset);
	}
	
	public int getKeyY(int key) {
		return Bits.unpackSigned(key, YSize, YOffset);
	}
	
	public int getKeyZ(int key) {
		return Bits.unpackSigned(key, ZSize, ZOffset);
	}
}
