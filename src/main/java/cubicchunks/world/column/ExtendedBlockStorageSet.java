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
package cubicchunks.world.column;


import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

//just a simple set for ExtendedBlockStorage that exposes the internal ExtendedBlockStorage[] array
//TODO: make it faster?
class ExtendedBlockStorageSet {
	private static final int SIZE_INCREMENT = 32;
	private ExtendedBlockStorage[] array = new ExtendedBlockStorage[SIZE_INCREMENT];
	private int size = 0;

	public ExtendedBlockStorage[] getArray() {
		return array;
	}

	public void add(ExtendedBlockStorage ebs) {
		assert size <= array.length;
		if(size == array.length) {
			ExtendedBlockStorage[] newArray = new ExtendedBlockStorage[array.length + SIZE_INCREMENT];
			System.arraycopy(array, 0, newArray, SIZE_INCREMENT - 1, array.length);
			array = newArray;
		}

		for(int index = 0; index < array.length; index++) {
			if(array[index] == null) {
				array[index] = ebs;
				size++;
				return;
			}
		}
		throw new AssertionError();
	}

	public void remove(ExtendedBlockStorage ebs) {
		for(int i = 0; i < array.length; i++) {
			if(array[i] == ebs) {
				array[i] = null;
				size--;
				assert size >= 0;
				if(size < array.length/2) {
					decreaseSize();
				}
				return;
			}
		}
	}

	private void decreaseSize() {
		ExtendedBlockStorage[] newArray = new ExtendedBlockStorage[array.length/2];
		int j = 0;
		for(int i = 0; i < array.length; i++) {
			if(array[i] != null) {
				newArray[j] = array[i];
				j++;
			}
		}
		array = newArray;
	}
}
