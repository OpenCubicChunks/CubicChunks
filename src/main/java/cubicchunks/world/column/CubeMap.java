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

import com.google.common.collect.Iterators;
import cubicchunks.world.cube.Cube;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

class CubeMap implements Iterable<Cube> {

	private final TreeMap<Integer, Cube> cubeMap = new TreeMap<>();
	private final Int2ObjectMap<Cube> map = new Int2ObjectOpenHashMap<>();
	
	//TODO: Don't force Cube's to have an ExtendedBlockStorage (empty Cube's eat memory)
	private final ExtendedBlockStorageSet set = new ExtendedBlockStorageSet();

	Cube get(int cubeY) {
		return map.get(cubeY);
	}

	Cube remove(int cubeY) {
		Cube cube = this.map.remove(cubeY);
		if(cube != null) {
			set.remove(cube.getStorage());
		}
		return cubeMap.remove(cubeY);
	}

	boolean put(int cubeY, Cube cube) {
		if (cube == null) {
			throw new NullPointerException();
		}
		if (this.contains(cubeY)) {
			throw new IllegalArgumentException("Cube at " + cubeY + " already exists!");
		}
		this.cubeMap.put(cubeY, cube);
		map.put(cubeY, cube);
		set.add(cube.getStorage()); //TODO: Don't force Cube's to have an ExtendedBlockStorage (empty Cube's eat memory)
		return true;
	}

	//TODO: optimize this... iterating over a map is kinda slow
	Iterable<Cube> cubes(int startY, int endY) {
		if(startY > endY) {
			return this.cubeMap.subMap(endY, true, startY, true).descendingMap().values();
		}
		return this.cubeMap.subMap(startY, true, endY, true).values();
	}

	private boolean contains(int cubeY) {
		return this.map.containsKey(cubeY);
	}

	//TODO: optimize this... iterating over a map is so slow! (and this is used for ticking chunks :/)
	@Override public Iterator<Cube> iterator() {
		return Iterators.unmodifiableIterator(this.map.values().iterator());
	}

	public Collection<Cube> all() {
		return Collections.unmodifiableCollection(this.map.values());
	}

	public boolean isEmpty() {
		return this.cubeMap.isEmpty();
	}

	ExtendedBlockStorage[] getStorageArrays() {
		//TODO: automatically update set if there is a Cube with a new ExtendedBlockStorage
		return set.getArray(); 
	}
}
