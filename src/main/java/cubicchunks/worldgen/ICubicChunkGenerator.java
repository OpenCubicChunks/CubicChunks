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
package cubicchunks.worldgen;

import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;

public interface ICubicChunkGenerator {

	/**
	 * Generate everything that can be generated without neighbor chunk
	 * or access to world block and entity manipulation.
	 *
	 * This is not intended to be used directly for generating terrain while loading chunks.
	 * @see ICubicChunkGenerator#generateCube
	 */
	void generateTerrain(Cube cube);

	/**
	 * Populate the cube - generate structures where information about neighbor cubes is needed.
	 *
	 * This is not intended to be used directly for generating terrain while loading chunks.
	 * @see ICubicChunkGenerator#generateCube
	 */
	void populateCube(Cube cube);

	/**
	 * Generates and (if possible) populates the cube, populates all neighbor cubes if possible.
	 */
	default void generateCube(ICubeCache cache, Cube cube) {
		this.generateTerrain(cube);
		CubeCoords baseCoords = cube.getCoords();
		for(int dx = 0; dx <= 1; dx++) {
			for (int dy = 0; dy <= 1; dy++) {
				for (int dz = 0; dz <= 1; dz++) {
					Cube currentCube = dx == 0 && dy == 0 && dz == 0 ?
							cube :
							cache.getCube(baseCoords.sub(dx, dy, dz));
					if(currentCube != null && cubesForPopulationExist(cache, currentCube.getCoords())) {
						this.populateCube(currentCube);
						currentCube.setPopulated(true);
					}
				}
			}
		}


	}

	static boolean cubesForPopulationExist(ICubeCache cache, CubeCoords coords) {
		for(int dx = 0; dx <= 1; dx++) {
			for(int dy = 0; dy <= 1; dy++) {
				for(int dz = 0; dz <= 1; dz++) {
					//test only if it's not current cube
					if(dx != 0 && dy != 0 && dz != 0 && !cache.cubeExists(coords.add(dx, dy, dz))) {
						return false;
					}
				}
			}
		}
		return true;
	}
}
