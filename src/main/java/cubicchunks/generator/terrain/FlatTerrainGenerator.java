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
package cubicchunks.generator.terrain;

import cubicchunks.api.generators.ITerrainGenerator;
import cubicchunks.world.cube.Cube;

import static cubicchunks.util.Coords.CUBE_SIZE;
import static cubicchunks.util.TerrainGeneratorUtils.applyHeightGradient;
import static cubicchunks.util.TerrainGeneratorUtils.getNewCubeSizedArray;

public class FlatTerrainGenerator implements ITerrainGenerator {

	private final double[][][] rawDensity;

	public FlatTerrainGenerator(final long seed) {
		this.rawDensity = getNewCubeSizedArray();
	}

	@Override
	public double[][][] generate(final Cube cube) {
		generateTerrainArray(cube);

		return applyHeightGradient(cube, this.rawDensity);
	}

	private void generateTerrainArray(final Cube cube) {
		for (int x = 0; x < CUBE_SIZE; x++) {
			for (int z = 0; z < CUBE_SIZE; z++) {
				for (int y = 0; y < CUBE_SIZE; y++) {
					this.rawDensity[x][y][z] = 0;
				}
			}
		}
	}
}
