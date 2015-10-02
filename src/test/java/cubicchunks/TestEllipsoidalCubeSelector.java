/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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
		
		/*	
		 * 					0	-2	 0
		 * 
		 * 	-1	-1	-1		0	-1	-1		1	-1	-1
		 * 	-1	-1	 0		0	-1	 0		1	-1	 0
		 * 	-1	-1	 1		0	-1	 1		1	-1	 1
		 * 
		 * 	-1	 0	-1		0	 0	-1		1	 0	-1
		 * 	-1	 0	 0		0	 0	 0		1	 0	 0
		 * 	-1	 0	 1		0	 0	 1		1	 0	 1
		 * 
		 * 	-1	 1	-1		0	 1	-1		1	 1	-1
		 * 	-1	 1	 0		0	 1	 0		1	 1	 0
		 * 	-1	 1	 1		0	 1	 1		1	 1	 1
		 * 
		 * 					0	 2	 0
		 */
		
		TreeSet<Long> addresses = (TreeSet<Long>)selector.getVisibleCubes();
		assertTrue(addresses.contains(AddressTools.getAddress(5, 2, 5)));	//	 0	-3	 0
		assertTrue(addresses.contains(AddressTools.getAddress(5, 3, 5)));	//	 0	-2	 0
		assertTrue(addresses.contains(AddressTools.getAddress(5, 4, 5)));	//	 0	-1	 0
		assertTrue(addresses.contains(AddressTools.getAddress(4, 5, 5)));	//	-1	 0	 0
		assertTrue(addresses.contains(AddressTools.getAddress(5, 5, 4)));	//	 0	 0	-1
		assertTrue(addresses.contains(AddressTools.getAddress(5, 5, 5)));	//	 0	 0	 0
		assertTrue(addresses.contains(AddressTools.getAddress(6, 5, 5)));	//	 1	 0	 0
		assertTrue(addresses.contains(AddressTools.getAddress(5, 5, 6)));	//	 0	 0	 1
		assertTrue(addresses.contains(AddressTools.getAddress(5, 6, 5)));	//	 0	 1	 0
		assertTrue(addresses.contains(AddressTools.getAddress(5, 7, 5)));	//	 0	 2	 0
		assertTrue(addresses.contains(AddressTools.getAddress(5, 8, 5)));	//	 0	 3	 0
		assertEquals(11, addresses.size());
	}
}
