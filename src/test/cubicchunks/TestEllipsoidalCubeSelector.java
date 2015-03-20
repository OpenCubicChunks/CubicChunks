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
package cubicchunks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.TreeSet;

import org.junit.Test;

import cubicchunks.util.AddressTools;
import cubicchunks.visibility.EllipsoidalCubeSelector;

public class TestEllipsoidalCubeSelector {
	
	@Test
	public void small() {
		EllipsoidalCubeSelector selector = new EllipsoidalCubeSelector();
		selector.setPlayerPosition(AddressTools.getAddress(5, 5, 5), 1);
		
		TreeSet<Long> addresses = (TreeSet<Long>)selector.getVisibleCubes();
		assertTrue(addresses.contains(AddressTools.getAddress(4, 5, 5)));
		assertTrue(addresses.contains(AddressTools.getAddress(5, 5, 4)));
		assertTrue(addresses.contains(AddressTools.getAddress(5, 3, 5)));
		assertTrue(addresses.contains(AddressTools.getAddress(5, 4, 5)));
		assertTrue(addresses.contains(AddressTools.getAddress(5, 5, 5)));
		assertTrue(addresses.contains(AddressTools.getAddress(5, 6, 5)));
		assertTrue(addresses.contains(AddressTools.getAddress(5, 7, 5)));
		assertTrue(addresses.contains(AddressTools.getAddress(5, 5, 6)));
		assertTrue(addresses.contains(AddressTools.getAddress(6, 5, 5)));
		assertEquals(9, addresses.size());
	}
}
