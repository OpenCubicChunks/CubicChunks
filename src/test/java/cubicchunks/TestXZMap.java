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

import com.sun.jndi.cosnaming.IiopUrl;
import cubicchunks.util.XYZAddressable;
import cubicchunks.util.XYZMap;
import cubicchunks.util.XZAddressable;
import cubicchunks.util.XZMap;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestXZMap {
	@Test
	public void testSimpleGetEqual() {
		XZAddressable value = new Addressable(0, 0, "1");
		XZMap<XZAddressable> map = new XZMap<>(0.75f, 10);
		map.put(value);
		assertEquals(value, map.get(0, 0));
	}

	@Test
	public void testGetEqualRandomPositions() {
		XZMap<XZAddressable> map = new XZMap<>(0.75f, 10);
		//set seed so that tests are predictable
		Random rand = new Random(42);
		int maxPuts = 500;
		Addressable[] values = new Addressable[maxPuts];
		testPutRandom(map, rand, maxPuts, values);
	}

	@Test
	public void testGetEqualRandomPositionsReplace() {
		XZMap<XZAddressable> map = new XZMap<>(0.75f, 10);
		//set seed so that tests are predictable
		Random rand = new Random(42);
		int maxPuts = 500;
		Addressable[] values = new Addressable[maxPuts];
		testPutRandom(map, rand, maxPuts, values);
		rand = new Random(42);
		//test that replacing works
		testPutRandom(map, rand, maxPuts, values);
		assertEquals(maxPuts, map.getSize());
	}

	private void testPutRandom(XZMap<XZAddressable> map, Random rand, int maxPuts, Addressable[] values) {
		for (int i = 0; i < maxPuts; i++) {
			values[i] = new Addressable(rand.nextInt(), rand.nextInt(), String.valueOf(i));
			map.put(values[i]);
			//test all previous values
			for (int j = 0; j <= i; j++) {
				Addressable exp = values[j];
				assertEquals(
						"added=" + values[i] + ", wrongValue=" + exp, exp, map.get(exp.getX(), exp.getZ()));
			}
		}
	}

	@Test
	public void testGetUnique() {
		XZAddressable value = new Addressable(0, 0, "1");
		XZMap<XZAddressable> map = new XZMap<>(0.75f, 10);
		map.put(value);
		for(int x = -20; x < 20; x++) {
			for(int z = -20; z < 20; z++) {
				if(x != 0 || z != 0) {
					assertNull(map.get(x, z));
				}
			}
		}
	}

	@Test
	public void testContains() {
		XZMap<XZAddressable> map = new XZMap<>(0.75f, 10);
		Addressable[] values = new Addressable[500];

		Random rand = new Random(42);
		for (int i = 0; i < values.length; ++i) {
			values[i] = new Addressable(rand.nextInt(), rand.nextInt(), String.valueOf(i));
			map.put(values[i]);

			for (int j = 0; j <= i; ++j) {
				assertTrue(map.contains(values[i].getX(), values[i].getZ()));
			}
		}
	}

	@Test
	public void testContainsNot() {
		XZAddressable value = new Addressable(0, 0, "1");
		XZMap<XZAddressable> map = new XZMap<>(0.75f, 10);
		map.put(value);
		for(int x = -20; x < 20; x++) {
			for(int z = -20; z < 20; z++) {
				if(x != 0 || z != 0) {
					assertTrue(!map.contains(x, z));
				}
			}
		}
	}

	@Test
	public void testIterator() {
		XZMap<XZAddressable> map = new XZMap<>(0.75f, 10);
		Set<XZAddressable> allElements = new HashSet<>();
		Random rand = new Random(42);
		int maxPut = 500;
		for(int i = 0; i < maxPut; i++) {
			Addressable newElement = new Addressable(rand.nextInt(), rand.nextInt(), String.valueOf(i));
			map.put(newElement);
			allElements.add(newElement);
		}
		Iterator<XZAddressable> it = map.iterator();
		while(it.hasNext()) {
			XZAddressable element = it.next();
			assertThat(allElements, hasItem(element));
			allElements.remove(element);
		}
		assertThat(allElements, empty());
	}

	/**
	 * Simple implementation of Addressable for testing, equal only if id of them is equal
	 */
	private static class Addressable implements XZAddressable {

		private final int x;
		private final int z;
		private Object id;

		public Addressable(int x, int z, @Nonnull Object id) {
			this.x = x;
			this.z = z;
			this.id = id;
		}

		@Override public int getX() {
			return x;
		}

		@Override public int getZ() {
			return z;
		}

		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Addressable that = (Addressable) o;

			return this.id.equals(that.id);

		}

		@Override public int hashCode() {
			return id.hashCode();
		}
	}
}
