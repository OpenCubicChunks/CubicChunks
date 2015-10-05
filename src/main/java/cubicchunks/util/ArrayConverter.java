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
package cubicchunks.util;

public class ArrayConverter {

	public static final byte[] toByteArray(char[] arr) {
		byte[] b = new byte[arr.length * 2];
		int i = 0;
		for(char c : arr) {
			b[i++] = (byte) (c&0xFF);
			b[i++] = (byte) (c >>> 8);
		}
		return b;
	}

	public static final char[] toCharArray(byte[] b) {
		if((b.length&1) != 0) {
			throw new IllegalArgumentException("Byte array length must be even number, but it's: " + b.length);
		}

		char[] arr = new char[b.length/2];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = (char) (b[i*2] | (b[i*2+1] << 8));
		}
		return arr;
	}
}
