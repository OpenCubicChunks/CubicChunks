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

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

class CubeMap implements Collection<Cube> {
	private final TreeMap<Integer, Cube> cubeMap = new TreeMap<>();

	/*
	 * Most of the time it's the same 2 cubes that are being accessed over and over again
	 * This class caches 3 values, when already cached cube is accessed - it goes up in the list,
	 * When uncached cube is accessed - it appears at the end of the list.
	 *
	 * With 3 cubes cached - accessing a single uncached cube won't affect 2 most frequently used cached cubes.
	 */
	private Cube cached1 = null, cached2 = null, cached3 = null;
	private int cached1Y = Integer.MIN_VALUE, cached2Y = Integer.MIN_VALUE, cached3Y = Integer.MIN_VALUE;

	private static long total = 0, uncached = 0;
	Cube get(int cubeY) {
		if(cubeY == cached1Y) {
			return cached1;
		}
		if(cubeY == cached2Y) {
			Cube t = cached1;
			cached1 = cached2;
			cached2 = t;
			cached2Y = cached1Y;
			cached1Y = cubeY;
			return cached1;
		}
		if(cubeY == cached3Y) {
			Cube t = cached2;
			cached2 = cached3;
			cached3 = t;
			cached3Y = cached2Y;
			cached2Y = cubeY;
			return cached2;
		}
		//it's not cached, find it and set as cached3
		Cube cube = cubeMap.get(cubeY);
		cached3 = cube;
		cached3Y = cubeY;
		return cube;
	}

	Cube remove(int cubeY) {
		this.invalidateCache();
		return cubeMap.remove(cubeY);
	}

	boolean put(int cubeY, Cube cube) {
		if(cube == null) {
			throw new NullPointerException();
		}
		if(this.cubeMapContains(cubeY)) {
			throw new IllegalArgumentException("Cube at " + cubeY + " already exists!");
		}
		this.cubeMap.put(cubeY, cube);
		this.invalidateCache();
		return true;
	}

	Iterable<Cube> cubes(int minCubeY, int maxCubeY) {
		return this.cubeMap.subMap(minCubeY, true, maxCubeY, true).values();
	}

	//contains method that doesn't update cached cubes
	private boolean cubeMapContains(int cubeY) {
		return this.cubeMap.containsKey(cubeY);
	}

	@Override
	public int size() {
		return this.cubeMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.cubeMap.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.cubeMap.containsValue(o);
	}

	@Override
	public Iterator<Cube> iterator() {
		return this.cubeMap.values().iterator();
	}

	@Override
	public Object[] toArray() {
		return this.cubeMap.values().toArray();
	}

	@Override
	public <T> T[] toArray(T[] ts) {
		return  this.cubeMap.values().toArray(ts);
	}

	@Override
	public boolean add(Cube cube) {
		return this.put(cube.getY(), cube);
	}

	@Override
	public boolean remove(Object o) {
		if(!(o instanceof Cube)) {
			return false;
		}
		return this.remove(((Cube)o).getY()) != null;
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return this.cubeMap.values().containsAll(collection);
	}

	@Override
	public boolean addAll(Collection<? extends Cube> collection) {
		collection.forEach(c -> this.put(c.getY(), c));
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		this.invalidateCache();
		return this.cubeMap.values().removeAll(collection);
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		this.invalidateCache();
		return this.cubeMap.values().retainAll(collection);
	}

	@Override
	public void clear() {
		this.cubeMap.clear();
		this.invalidateCache();
	}

	private void invalidateCache() {
		cached1 = cached2 = cached3 = null;
		cached1Y = cached2Y = cached3Y = Integer.MIN_VALUE;
	}
}
