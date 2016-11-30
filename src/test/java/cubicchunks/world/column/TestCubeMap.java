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

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;

import static it.ozimov.cirneco.hamcrest.java7.collect.IsIterableWithDistinctElements.hasDistinctElements;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestCubeMap {

	@Test
	public void addCubesInOrder() {
		CubeMap testedCubeMap = new CubeMap();
		for (int y = -30; y < 30; y++) {
			testedCubeMap.put(createCube(y));
		}

		checkCubeMap(testedCubeMap, 60);

		for (int y = -30; y < 30; y++) {
			try {
				testedCubeMap.put(createCube(y));
				fail("Was able to add a Cube to the CubeMap at the same y coordinate as another Cube");
			} catch (IllegalArgumentException e) {
				// this should always happen
			}
		}
	}

	@Test
	public void addCubesInReverse() {
		CubeMap testedCubeMap = new CubeMap();
		for (int y = 29; y >= -30; y--) {
			testedCubeMap.put(createCube(y));
		}

		checkCubeMap(testedCubeMap, 60);

		for (int y = -30; y < 30; y++) {
			try {
				testedCubeMap.put(createCube(y));
				fail("Was able to add a Cube to the CubeMap at the same y coordinate as another Cube");
			} catch (IllegalArgumentException e) {
				// this should always happen
			}
		}
	}

	@Test
	public void findRangeOfCubes() {
		CubeMap testedCubeMap = new CubeMap();
		for (int y = -30; y < 30; y++) {
			testedCubeMap.put(createCube(y));
		}

		checkRange(testedCubeMap, 3, 3);

		checkRange(testedCubeMap, -5, 5);
		checkRange(testedCubeMap, -5, 500);
		checkRange(testedCubeMap, -500, 5);
		checkRange(testedCubeMap, -500, 500);

		checkRange(testedCubeMap, 500, 500);
		checkRange(testedCubeMap, -500, -500);

		Random rang = new Random(12345);
		testedCubeMap = new CubeMap();
		for (int y = -40; y < 40; y++) {
			if (rang.nextBoolean()) {
				testedCubeMap.put(createCube(y));
			}
		}

		checkRange(testedCubeMap, 3, 3);

		checkRange(testedCubeMap, -5, 5);
		checkRange(testedCubeMap, -5, 500);
		checkRange(testedCubeMap, -500, 5);
		checkRange(testedCubeMap, -500, 500);

		checkRange(testedCubeMap, 500, 500);
		checkRange(testedCubeMap, -500, -500);
	}

	private void checkRange(CubeMap cubeMap, int lowerBound, int upperBound) {
		Iterable<Cube> range = cubeMap.cubes(lowerBound, upperBound);
		Set<Cube> rangeSet = new HashSet<>();
		range.forEach(rangeSet::add);

		assertThat(range, hasDistinctElements());

		// validate contents
		cubeMap.all().forEach(cube -> {
			if (cube.getY() >= lowerBound && cube.getY() <= upperBound) {
				assertThat(rangeSet, hasItem(cube));
			} else {
				assertThat(rangeSet, not(hasItem(cube)));
			}
		});

		// check the order
		int alwaysLower = Integer.MIN_VALUE;
		for (Cube cube : range) {
			assertThat(cube.getY(), is(greaterThan(alwaysLower)));
			alwaysLower = cube.getY();
		}
	}

	private void checkCubeMap(CubeMap cubeMap, int size) {
		assertEquals(size, cubeMap.all().size());

		// check order (should be lowest y to highest y)
		int alwaysLower = Integer.MIN_VALUE;
		for (Cube cube : cubeMap.all()) {
			assertThat(cube.getY(), is(greaterThan(alwaysLower)));
			alwaysLower = cube.getY();
		}
	}

	private Cube createCube(int cubeY) {
		Cube cube = mock(Cube.class);
		when(cube.getY()).thenReturn(cubeY);
		return cube;
	}
}
