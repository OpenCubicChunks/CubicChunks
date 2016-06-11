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

import cubicchunks.world.cube.Cube;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCubeMap {

	//this test class has been made when attempting to optimize CubeMap for 3 last used cubeY positions.
	//this feature may need to be readded, so keeping it there
	@Test
	public void testSequentialGet() {
		CubeMap map = new CubeMap();
		Cube[] cubes = new Cube[32];
		for (int i = 0; i < cubes.length; i++) {
			cubes[i] = new Cube(null, null, 0, 0, 0, false);
			map.put(i, cubes[i]);
		}
		for (int i = -10; i < 32 + 10; i++) {
			Cube expected = i < 0 || i >= cubes.length ? null : cubes[i];
			assertEquals(expected, map.get(i));
		}
	}

	@Test
	public void testRepeatedSequentialGet() {
		CubeMap map = new CubeMap();
		Cube[] cubes = new Cube[32];
		for (int i = 0; i < cubes.length; i++) {
			cubes[i] = new Cube(null, null, 0, 0, 0, false);
			map.put(i, cubes[i]);
		}
		for (int i = 0; i < 3; i++) {
			for (int j = -10; j < 32 + 10; j++) {
				Cube expected = j < 0 || j >= cubes.length ? null : cubes[j];
				assertEquals(expected, map.get(j));
			}
		}
	}

	@Test
	public void testSequentialGetMultipleTimes() {
		CubeMap map = new CubeMap();
		Cube[] cubes = new Cube[32];
		for (int i = 0; i < cubes.length; i++) {
			cubes[i] = new Cube(null, null, 0, 0, 0, false);
			map.put(i, cubes[i]);
		}
		for (int i = -10; i < 32 + 10; i++) {
			Cube expected = i < 0 || i >= cubes.length ? null : cubes[i];
			for(int j = 0; j < 3; j++) {
				assertEquals(expected, map.get(i));
			}
		}
	}
}
