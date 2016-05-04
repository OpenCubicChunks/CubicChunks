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
package cubicchunks;

import cubicchunks.util.AddressTools;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestAddressTools {
	
	@Test
	public void testY() {
		assertEquals(-524288, AddressTools.MIN_CUBE_Y);
		assertEquals(524287, AddressTools.MAX_CUBE_Y);
		for (int i = AddressTools.MIN_CUBE_Y; i <= AddressTools.MAX_CUBE_Y; i++) {
			assertEquals(i, AddressTools.getY(AddressTools.getAddress(0, i, 0)));
		}
	}
	
	@Test
	public void testX() {
		assertEquals(-2097152, AddressTools.MIN_CUBE_X);
		assertEquals(2097151, AddressTools.MAX_CUBE_X);
		for (int i = AddressTools.MIN_CUBE_X; i <= AddressTools.MAX_CUBE_X; i++) {
			assertEquals(i, AddressTools.getX(AddressTools.getAddress(i, 0, 0)));
		}
	}
	
	@Test
	public void testZ() {
		assertEquals(-2097152, AddressTools.MIN_CUBE_Z);
		assertEquals(2097151, AddressTools.MAX_CUBE_Z);
		for (int i = AddressTools.MIN_CUBE_Z; i <= AddressTools.MAX_CUBE_Z; i++) {
			assertEquals(i, AddressTools.getZ(AddressTools.getAddress(0, 0, i)));
		}
	}
	
	@Test
	public void testAddresses() {
		for (int x = -32; x <= 32; x++) {
			for (int y = -32; y <= 32; y++) {
				for (int z = -32; z <= 32; z++) {
					long address = AddressTools.getAddress(x, y, z);
					assertEquals(x, AddressTools.getX(address));
					assertEquals(y, AddressTools.getY(address));
					assertEquals(z, AddressTools.getZ(address));
				}
			}
		}
	}
	
	@Test
	public void testCollisions() {
		HashSet<Long> addresses = new HashSet<Long>();
		for (int x = -32; x <= 32; x++) {
			for (int y = -32; y <= 32; y++) {
				for (int z = -32; z <= 32; z++) {
					long address = AddressTools.getAddress(x, y, z);
					assertFalse(addresses.contains(address));
					addresses.add(address);
				}
			}
		}
	}
}