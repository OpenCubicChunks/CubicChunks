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

import com.google.common.collect.Lists;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class CubeMap implements Iterable<Cube> {

	private static final Comparator<Cube> ORDER = (one, two) ->
			one.getY() - two.getY();

	private final List<Cube> cubes = new ArrayList<>();

	private ExtendedBlockStorage[] toBlockTick = new ExtendedBlockStorage[0];

	Cube remove(int cubeY) {
		int index = binarySearch(cubeY);
		return index < cubes.size() && cubes.get(index).getY() == cubeY ? cubes.remove(index) : null;
	}

	void put(Cube cube) {
		if (cube == null) {
			throw new NullPointerException();
		}
		if (this.contains(cube.getY())) {
			throw new IllegalArgumentException("Cube at " + cube.getY() + " already exists!");
		}
		cubes.add(cube);
		cubes.sort(ORDER); // kind of expensive... but puts don't happen much (wish there was a SortedArrayList)
	}

	Iterable<Cube> cubes(int startY, int endY) {
		boolean reverse = false;
		if(startY > endY){
			int i = startY;
			startY = endY;
			endY = i;
			reverse = true;
		}

		int bottom = binarySearch(startY);
		int top = binarySearch(endY + 1); // subList()'s second arg is exclusive so we need to add 1

		if(bottom < cubes.size() && top <= cubes.size()){
			return reverse ? Lists.reverse(cubes.subList(bottom, top)) : cubes.subList(bottom, top);
		}else{
			return Collections.emptyList();
		}
	}

	private boolean contains(int cubeY) {
		int index = binarySearch(cubeY);
		return index < cubes.size() && cubes.get(index).getY() == cubeY;
	}

	@Override public Iterator<Cube> iterator() {
		return cubes.iterator();
	}

	public Collection<Cube> all() {
		return Collections.unmodifiableCollection(cubes);
	}

	public boolean isEmpty() {
		return cubes.isEmpty();
	}

	/**
	 * @return An array of EBS's form Cubes that need ticking... kind of a hack but vanilla needs it
	 */
	ExtendedBlockStorage[] getStoragesToTick() {
		if(!isToTickValid()) {
			int count = 0;
			for(Cube cube : cubes) {
				if (cube.getStorage() != null && cube.getTickets().shouldTick()) {
					count++;
				}
			}

			toBlockTick = new ExtendedBlockStorage[count];
			count = 0;
			for(Cube cube : cubes) {
				if (cube.getStorage() != null && cube.getTickets().shouldTick()) {
					toBlockTick[count++] = cube.getStorage();
				}
			}
		}

		return toBlockTick;
	}

	private boolean isToTickValid(){
		int index = 0;
		for(Cube cube : cubes) {
			if(cube.getStorage() != null && cube.getTickets().shouldTick()) {
				if(index >= toBlockTick.length) {
					return false;
				}
				if(toBlockTick[index++] != cube.getStorage()) {
					return false;
				}
			}
		}
		return index == toBlockTick.length; // did we check everything there was in toBlockTick?
	}

	private int binarySearch(int cubeY) {
		int start = 0;
		int end = cubes.size() - 1;
		int mid = 0;

		while(start <= end) {
			mid = start + end >>> 1;

			int at = cubes.get(mid).getY();
			if(at < cubeY) { // we are below the target;
				start = mid + 1;
			}else if(at > cubeY) {
				end = mid - 1; // we are above the target
			}else{
				return mid;// found target!
			}
		}

		return mid; // not found :(
	}
}
